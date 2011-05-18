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
package javapayload.handler.dynstager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javapayload.Parameter;
import javapayload.handler.stage.StageHandler;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESStageHandler extends StageHandler {

	private final StageHandler handler;
	private final String key;

	public AESStageHandler(String key, StageHandler handler) {
		super(null, false, false, null);
		this.handler = handler;
		this.key = key;
	}
	
	public Parameter[] getParameters() {
		throw new UnsupportedOperationException();
	}

	public Class[] getNeededClasses() {
		return new Class[0];
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
		DataInputStream din = new DataInputStream(in);
		SecureRandom sr = new SecureRandom();
		byte[] outIV = new byte[16];
		sr.nextBytes(outIV);
		out.write(outIV);
		out.flush();
		byte[] inIV = new byte[16];
		din.readFully(inIV);
		byte[] keyBytes = MessageDigest.getInstance("MD5").digest(key.getBytes());
		Cipher co = Cipher.getInstance("AES/CFB8/NoPadding");
		co.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(outIV), sr);
		Cipher ci = Cipher.getInstance("AES/CFB8/NoPadding");
		ci.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(inIV), sr);
		handler.handle(new SynchronizedOutputStream(new CipherOutputStream(out, co)), new CipherInputStream(din, ci), parameters);
	}

	protected StageHandler createClone() {
		return new AESStageHandler(key, handler);
	}

	StageHandler getHandler() {
		return handler;
	}

	protected static String generatePassword() {
		SecureRandom rnd = new SecureRandom();
		char[] pwd = new char[rnd.nextInt(16) + 16];
		for (int i = 0; i < pwd.length; i++) {
			int idx = rnd.nextInt(26 + 26 + 10);
			if (idx >= 36) {
				pwd[i] = (char) (idx - 36 + 'a');
			} else if (idx >= 10) {
				pwd[i] = (char) (idx - 10 + 'A');
			} else {
				pwd[i] = (char) (idx + '0');
			}
		}
		return new String(pwd);
	}
}
