/*
 * Java Payloads.
 * 
 * Copyright (c) 2011 Michael 'mihi' Schierl
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

public class CrudeScript implements Stage {
	
	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		PrintStream pout = new PrintStream(out, true);
		Object[] values = new Object[1024];
		int maxUsed=0, maxPrinted = -1;
		pout.println("Welcome to CrudeScript.");
		pout.println(" \"str        create String literal");
		pout.println(" 1            print methods of Object/class 1, members of array 1");
		pout.println(" 1/foo        print methods of Object/class 1 starting with foo");
		pout.println(" 1 2 3 4      invoke methods 2 on object 1 with params 3 4");
		pout.println(" 1 [ 2 3      create Array of class 1 with elements 2 3");
		pout.println(" exit         get out of here");
		pout.println();
		while(true) {
			while (maxPrinted < maxUsed) {
				pout.println(++maxPrinted+":\t"+values[maxPrinted]);
			}
			pout.print("$> ");
			String line = in.readLine();
			if (line.equals("exit")) break;
			try {
				if (line.startsWith("\"")) {
					values[++maxUsed] = line.substring(1);
				} else {
					if (line.indexOf(' ')== -1 && line.indexOf('[') == -1 && line.indexOf('/') == -1) 
						line += "/";
					int pos = line.indexOf('/');
					if (pos != -1) {
						Object obj = values[Integer.parseInt(line.substring(0, pos).trim())];
						if (obj instanceof Object[] && pos == line.length()-1) {
							Object[] arr = (Object[]) obj;
							for (int i = 0; i < arr.length; i++) {
								values[++maxUsed] = arr[i];
							}
						} else {
							Class clazz = obj instanceof Class ? (Class) obj: obj.getClass();
							Method[] mthds = clazz.getMethods();
							for (int i = 0; i < mthds.length; i++) {
								if(mthds[i].getName().startsWith(line.substring(pos+1))) {
									values[++maxUsed] = mthds[i];
								}
							}
						}
					} else {
						StringTokenizer st = new StringTokenizer(line);
						Object obj = values[Integer.parseInt(st.nextToken())];
						String mthIdx = st.nextToken();
						Object[] args = new Object[st.countTokens()];
						for (int i = 0; i < args.length; i++) {
							args[i] = values[Integer.parseInt(st.nextToken())];
						}
						if (mthIdx.equals("[")) {
							Object[] result = (Object[]) Array.newInstance((Class)obj, args.length);
							System.arraycopy(args, 0, result, 0, args.length);
							values[++maxUsed] = result;
						} else {
							Method mth = (Method) values[Integer.parseInt(mthIdx)];
							values[++maxUsed] = mth.invoke(obj, args);
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace(pout);
			}
		}
		pout.close();
	}
}
