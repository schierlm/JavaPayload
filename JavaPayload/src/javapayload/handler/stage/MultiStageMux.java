/*
 * Java Payloads.
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

package javapayload.handler.stage;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javapayload.Parameter;
import javapayload.handler.stage.MultiStage.ConsoleWrapper;
import javapayload.stage.MultiStageMuxOutputStream;

public class MultiStageMux extends StageHandler {

	public MultiStageMux() {
		super("Stage multiplexer with background streams and job control", true, true,
				"Multiplexes between multiple stages over the same connection. Background\r\n" +
				"stages continue sending data, only the output is buffered.");
	}

	public Parameter[] getParameters() {
		return new Parameter[0];
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
		DataInputStream dis = new DataInputStream(consoleIn);
		List/* <MultiStage.Stage> */runningStages = new ArrayList();
		new Thread(new DecodeForwarder(new DataInputStream(in), runningStages)).start();
		while (true) {
			consoleOut.println();
			for (int i = 0; i < runningStages.size(); i++) {
				Stage stage = (Stage) runningStages.get(i);
				consoleOut.println(i + 1 + ": " + (stage.isAlive() ? stage.getCommandLine() : "[" + stage.getCommandLine() + "]"));
			}
			consoleOut.print("]> ");
			// yes I know this is deprecated. but BufferedReader is way too
			// bloated for what we need here
			String line = dis.readLine().trim();
			if (line.length() == 0)
				continue;
			if (line.equals("exit")) {
				boolean mayExit = true;
				for (int i = 0; i < runningStages.size(); i++) {
					Stage stage = (Stage) runningStages.get(i);
					if (stage.isAlive()) {
						consoleOut.println("There are still running stages.");
						mayExit = false;
						break;
					}
				}
				if (mayExit)
					break;
				else
					continue;
			}
			if (line.equals("help")) {
				consoleOut.println("Type a number to switch to that stage.");
				consoleOut.println("Type a new stage with args to run it.");
				consoleOut.println("While connected, type ]> to get back to this prompt.");
				continue;
			}
			Stage stage;
			try {
				int number = Integer.parseInt(line);
				if (number <= 0 || number > runningStages.size()) {
					consoleOut.println("Invalid stage index.");
					continue;
				}
				stage = (Stage) runningStages.get(number - 1);
			} catch (NumberFormatException ex) {
				try {
					stage = new Stage(line);
				} catch (Throwable t) {
					t.printStackTrace(consoleOut);
					continue;
				}
				stage.bootstrap(runningStages.size(), out);
				runningStages.add(stage);
			}
			stage.interact();
		}
		out.writeInt(-1);
		for (int i = 0; i < runningStages.size(); i++) {
			((Stage) runningStages.get(i)).shutdown();
		}
		out.close();
		consoleIn.close();
		consoleOut.close();
	}

	public Class[] getNeededClasses() {
		return new Class[] { javapayload.stage.Stage.class, javapayload.stage.MultiStageClassLoader.class, javapayload.stage.MultiStageMuxOutputStream.class, javapayload.stage.MultiStageMux.class };
	}

	protected StageHandler createClone() {
		return new MultiStageMux();
	}

	private class Stage {
		private final String commandLine;
		private final String[] arguments;
		private final StageHandler handler;
		private MultiStageMuxOutputStream msOut;
		private PipedOutputStream console;
		private BufferedOutputStream buffer;
		private ConsoleWrapper consoleWrapper;
		private volatile boolean alive = true;

		public Stage(String commandLine) throws Exception {
			this.commandLine = commandLine;
			StringTokenizer st = new StringTokenizer("MultiStage -- " + commandLine, " ");
			arguments = new String[st.countTokens()];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = st.nextToken();
			}
			handler = (StageHandler) Class.forName("javapayload.handler.stage." + arguments[2]).newInstance();
		}

		public boolean isAlive() {
			return alive;
		}

		public BufferedOutputStream getBuffer() {
			return buffer;
		}

		public void bootstrap(int index, DataOutputStream out) throws Exception {
			final PipedOutputStream pipedOut = new PipedOutputStream();
			buffer = new BufferedOutputStream(pipedOut);
			synchronized (out) {
				out.writeInt(index);
				out.writeShort(arguments.length);
				for (int i = 0; i < arguments.length; i++) {
					out.writeUTF(arguments[i]);
				}
			}
			msOut = new MultiStageMuxOutputStream(index, out);
			console = new PipedOutputStream();
			handler.consoleIn = new PipedInputStream(console);
			handler.consoleOut = new PrintStream(consoleWrapper = new ConsoleWrapper(consoleOut), true);
			handler.consoleErr = consoleErr;
			new Thread(new Runnable() {
				public void run() {
					try {
						PipedInputStream in = new PipedInputStream(pipedOut);
						handler.handle(msOut, in, arguments);
						consoleOut.println("Stage terminated.");
						alive = false;
						in.close();
					} catch (Exception ex) {
						ex.printStackTrace(consoleErr);
					}
				}
			}).start();
		}

		public void interact() throws Exception {
			consoleWrapper.start();
			boolean escape = false;
			while (true) {
				int b = consoleIn.read();
				if (escape) {
					if (b == '>') {
						break;
					} else if (b != ']') {
						console.write(']');
					}
					console.write(b);
					escape = false;
				} else if (b == ']') {
					escape = true;
				} else {
					console.write(b);
				}
			}
			consoleWrapper.stop();
		}

		public String getCommandLine() {
			return commandLine;
		}

		public void shutdown() throws IOException {
			// make sure all pending data/exceptions can be sent
			consoleWrapper.start();
		}
	}

	private class DecodeForwarder implements Runnable {
		private final DataInputStream in;
		private final List/* <MultiStage.Stage> */runningStages;

		public DecodeForwarder(DataInputStream in, List/* <MultiStage.Stage> */runningStages) {
			this.in = in;
			this.runningStages = runningStages;
		}

		public void run() {
			try {
				while (true) {
					int index = in.readInt();
					if (index == -1)
						break;
					if (index == -2) {
						int b;
						while ((b = in.read()) != -1)
							consoleOut.write(b);
						break;
					}
					if (index < 0 || index >= runningStages.size()) {
						throw new RuntimeException("Invalid stage index: " + index + " (stages size = " + runningStages.size() + ")");
					}
					OutputStream outBuf = ((Stage) runningStages.get(index)).getBuffer();
					byte[] buf = new byte[in.readInt()];
					if (buf.length == 0) {
						outBuf.close();
					} else {
						in.readFully(buf);
						outBuf.write(buf);
						outBuf.flush();
					}
				}
				while (in.read() != -1)
					;
			} catch (Throwable t) {
				t.printStackTrace(consoleErr);
			}
		}
	}
}