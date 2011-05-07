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

package javapayload.handler.stager;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import javapayload.handler.stage.StageHandler;

public class PollingTunnel extends StagerHandler implements Runnable {

	private WrappedPipedOutputStream pipedOut;
	private PipedInputStream pipedIn;
	private PrintStream errorStream;
	private CommunicationInterface communicationInterface;
	private byte[][] readBuffer = new byte[1][];

	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (readyHandler != null) readyHandler.notifyReady();
		this.errorStream = errorStream;
		if (extraArg == null) {
			extraArg = new LocalCommunicationInterface(parameters);
		} else if (!(extraArg instanceof CommunicationInterface)) {
			throw new IllegalArgumentException("No Communication interface found");
		}
		communicationInterface = (CommunicationInterface) extraArg;
		pipedIn = new PipedInputStream(4096);
		WrappedPipedOutputStream stagerOut = new WrappedPipedOutputStream(new PipedOutputStream(pipedIn));
		PipedOutputStream pos = new PipedOutputStream();
		pipedOut = new WrappedPipedOutputStream(pos, stagerOut);
		new Thread(this).start();
		new Thread(new Runnable() {
			public void run() {
				runReader();
			}
		}).start();
		stageHandler.handle(stagerOut, new PipedInputStream(pos), parameters);
	}

	public void runReader() {
		try {
			javapayload.stager.PollingTunnel.runReaderThread(readBuffer, pipedIn);
		} catch (Throwable t) {
			t.printStackTrace(errorStream);
		}
	}

	public void run() {
		try {
			int pollTimeout = 1;
			boolean done = false;
			while (!done) {
				byte[] read;
				synchronized (readBuffer) {
					if (readBuffer[0] == null)
						readBuffer.wait(pollTimeout);
					pollTimeout *= 2;
					if (pollTimeout > 1000)
						pollTimeout = 1000;
					read = readBuffer[0];
					if (readBuffer[0] != null) {
						pollTimeout = 1;
						readBuffer[0] = null;
						readBuffer.notifyAll();
					}
				}
				String request = (read == null) ? "0" : ("1" + javapayload.stager.PollingTunnel.encodeASCII85(read));
				String response = communicationInterface.sendData(request);
				if (response.startsWith("1")) {
					pollTimeout = 1;
					byte[] respData = javapayload.stager.PollingTunnel.decodeASCII85(response.substring(1));
					pipedOut.write(respData);
					pipedOut.flush();
					if (respData.length == 0) {
						pipedOut.close();
						pipedIn.close();
						done = true;
					}
				} else if (!response.equals("0")) {
					throw new IOException("Unsupported response: " + response);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace(errorStream);
		}
	}

	protected boolean needHandleBeforeStart() {
		return false;
	}

	protected boolean canHandleExtraArg(Class argType) {
		return argType.equals(CommunicationInterface.class);
	}

	protected String getTestArguments() {
		return null;
	}

	public static interface CommunicationInterface {
		public String sendData(String request) throws Exception;
	}

	private class LocalCommunicationInterface implements CommunicationInterface {
		private javapayload.stager.PollingTunnel stager;

		public LocalCommunicationInterface(String[] parameters) throws Exception {
			stager = new javapayload.stager.PollingTunnel();
			stager.bootstrap(parameters, false);
		}

		public String sendData(String request) throws Exception {
			String response = stager.sendData(request);
			return response;
		}
	}
}
