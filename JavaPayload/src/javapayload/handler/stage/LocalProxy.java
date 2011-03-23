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
import java.net.SocketException;

import javapayload.stage.StreamForwarder;
import javapayload.stager.Stager;

public class LocalProxy extends StageHandler {

	public Class[] getNeededClasses() {
		throw new IllegalStateException("Not used");
	}

	public void handle(final OutputStream rawOut, final InputStream in, String[] parameters) throws Exception {
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				String[] args = new String[parameters.length - (i + 2)];
				System.arraycopy(parameters, i + 2, args, 0, args.length);
				final Stager stager = (Stager) Class.forName("javapayload.stager." + args[0]).newInstance();
				stager.targetStager = new Stager() {
					public void bootstrap(String[] parameters) throws Exception {
						throw new IllegalStateException("Not used");
					}

					protected void bootstrap(final InputStream stagerIn, final OutputStream stagerOut, String[] parameters) throws Exception {
						consoleOut.println("Successfully started local proxy.");
						Thread t = new Thread() {
							public void run() {
								try {
									StreamForwarder.forward(stagerIn, rawOut);
								} catch (SocketException ex) {
									// ignore
								} catch (IOException ex) {
									ex.printStackTrace(consoleErr);
								}
							};
						};
						t.setDaemon(true);
						t.start();
						try {
							StreamForwarder.forward(in, stagerOut);
						} catch (SocketException ex) {
							// ignore
						}
						consoleOut.println("Shutting down local proxy.");
					};
				};
				stager.bootstrap(args);
				break;
			}
		}
	}

	protected StageHandler createClone() {
		return new LocalProxy();
	}
}