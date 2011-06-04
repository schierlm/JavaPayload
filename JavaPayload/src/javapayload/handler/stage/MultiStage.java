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
import javapayload.stage.MultiStageOutputStream;

public class MultiStage extends StageHandler {

	public MultiStage() {
		super("Stage multiplexer with job control", true, true,
				"Multiplexes between multiple stages over the same connection.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[0];
	}
	
	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
		DataInputStream dis = new DataInputStream(consoleIn);
		List/* <MultiStagerStage> */runningStages = new ArrayList();
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
				consoleOut.println("Type a new stager with args to run it.");
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
				out.writeInt(number - 1);
				stage = (Stage) runningStages.get(number - 1);
			} catch (NumberFormatException ex) {
				try {
					stage = new Stage(line);
				} catch (Throwable t) {
					t.printStackTrace(consoleOut);
					continue;
				}
				out.writeInt(runningStages.size());
				stage.bootstrap(in, out);
				runningStages.add(stage);
			}
			stage.forward();
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
		return new Class[] { javapayload.stage.Stage.class, javapayload.stage.MultiStageOutputStream.class, javapayload.stage.MultiStageClassLoader.class, javapayload.stage.MultiStage.class };
	}

	protected StageHandler createClone() {
		return new MultiStage();
	}

	private class Stage {
		private final String commandLine;
		private final String[] arguments;
		private final StageHandler handler;
		private MultiStageOutputStream msOut;
		private DecodeForwarder df;
		private PipedOutputStream console;
		private ConsoleWrapper consoleWrapper;
		private volatile boolean alive = true;

		public Stage(String commandLine) throws Exception {
			this.commandLine = commandLine;
			StringTokenizer st = new StringTokenizer("MultiStage -- "+commandLine, " ");
			arguments = new String[st.countTokens()];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = st.nextToken();
			}
			handler = (StageHandler) Class.forName("javapayload.handler.stage." + arguments[2]).newInstance();
		}

		public boolean isAlive() {
			return alive;
		}

		public void bootstrap(InputStream in, DataOutputStream out) throws Exception {
			out.writeShort(arguments.length);
			for (int i = 0; i < arguments.length; i++) {
				out.writeUTF(arguments[i]);
			}
			msOut = new MultiStageOutputStream(out);
			final PipedOutputStream pipedOut = new PipedOutputStream();
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
			new Thread(df = new DecodeForwarder(in, pipedOut)).start();
		}

		public void forward() throws Exception {
			msOut.start();
			consoleWrapper.start();
			df.start();
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
			msOut.stop();
			consoleWrapper.stop();
			df.waitFor();
		}

		public String getCommandLine() {
			return commandLine;
		}

		public void shutdown() throws IOException {
			df.shutdown();
			// make sure all pending data/exceptions can be sent
			msOut.start();
			consoleWrapper.start();
		}

		private class DecodeForwarder implements Runnable {

			private boolean active = false, shutdown = false;
			private final InputStream in;
			private final BufferedOutputStream out;

			public DecodeForwarder(InputStream in, OutputStream out) {
				this.in = in;
				this.out = new BufferedOutputStream(out);
			}

			public synchronized void shutdown() {
				if (active)
					throw new IllegalStateException();
				shutdown = true;
				notifyAll();
			}

			public void run() {
				try {
					while (true) {
						synchronized (this) {
							while (!active && !shutdown)
								wait();
							if (shutdown)
								return;
						}
						MultiStageOutputStream.decodeForward(in, out);
						out.flush();
						synchronized (this) {
							active = false;
							notifyAll();
						}
					}
				} catch (Throwable t) {
					t.printStackTrace(consoleErr);
				}
			}

			public synchronized void start() {
				active = true;
				notifyAll();
			}

			public synchronized void waitFor() throws InterruptedException {
				while (active) {
					wait();
				}
			}
		}

		public class ConsoleWrapper extends OutputStream {

			private final OutputStream out;
			private boolean active = false;

			public ConsoleWrapper(OutputStream out) {
				this.out = out;
			}

			public void write(byte[] b, int off, int len) throws IOException {
				writeInternal(b, off, len);
			}

			public void write(int b) throws IOException {
				write(new byte[] { (byte) b });
			}

			private synchronized void writeInternal(byte[] bs, int off, int len) throws IOException {
				while (!active) {
					try {
						wait();
					} catch (InterruptedException ex) {
					}
				}
				out.write(bs, off, len);
			}

			public synchronized void flush() throws IOException {
				while (!active) {
					try {
						wait();
					} catch (InterruptedException ex) {
					}
				}
				out.flush();
			}

			public synchronized void start() throws IOException {
				if (active)
					throw new IllegalStateException();
				active = true;
				notifyAll();
			}

			public synchronized void stop() throws IOException {
				if (!active)
					throw new IllegalStateException();
				active = false;
			}
		}
	}
}