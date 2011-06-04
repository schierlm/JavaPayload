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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class LocalStageMenu extends StageMenu {

	protected Object[] parseLine(String line, List stages) {
		if (line.indexOf(' ') != -1) 
			return new Object[] {"Custom", splitArgs("StageMenu -- "+line), new byte[0]};
		try {
			int number = Integer.parseInt(line);
			if (number > 0 && number <= stages.size()) {
				Object[] stage = (Object[])stages.get(number-1);
				return stage;
			}
		} catch (NumberFormatException ex) {
		}
		return null;
	}
	
	protected String getExtraOption() {
		return " or enter your own stage";
	}
	
	protected byte[] loadStageBytes(DataInputStream in) throws IOException {
		return new byte[0];
	}
		
	protected void bootstrap(InputStream rawIn, OutputStream out, String[] parameters) {
		try {
			final DataInputStream in = new DataInputStream(rawIn);
			Class clazz = null;
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i].equals("--")) {
					clazz = Class.forName("javapayload.stage."+parameters[i+1]);
					break;
				}
			}
			final Object stage = clazz.newInstance();
			clazz.getMethod("start", new Class[] { DataInputStream.class, OutputStream.class, String[].class }).invoke(stage, new Object[] { in, out, parameters });
		} catch (final Throwable t) {
			t.printStackTrace(new PrintStream(out, true));
		}
	}
}