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

package javapayload.builder.dynstager;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketException;

import javapayload.handler.stage.LocalProxy;
import javapayload.stage.StreamForwarder;

// used by LocalProxy stage handler
public class InternalLocalProxy extends WrappingDynStagerBuilder {

	public PrintStream consoleOut;
	public PrintStream consoleErr;
	public OutputStream rawOut;
	public InputStream in;
	
	public void bootstrapWrap(String[] parameters, boolean needWait) throws Exception {
		bootstrapOrig(parameters, needWait);
	}

	protected void bootstrapWrap(final InputStream stagerIn, final OutputStream stagerOut, String[] parameters) {
		try {
			consoleOut.println("Successfully started local proxy.");
			Thread t = new LocalProxy.ForwardThread(stagerIn, rawOut, consoleErr);
			t.setDaemon(true);
			t.start();
			try {
				StreamForwarder.forward(in, stagerOut);
			} catch (SocketException ex) {
				// ignore
			}
			consoleOut.println("Shutting down local proxy.");
		} catch (Throwable t) {
			t.printStackTrace(consoleErr);
		}
	}
}
