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

package javapayload.stage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.security.MessageDigest;

public class PasswordProtect implements Stage {

	public static final String PROMPT = "0-\" ";
	private static final String PROPERTY_PREFIX = "PasswordProtect.Whitelist.";

	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		byte[] stageUpload = new byte[in.readInt()];
		in.readFully(stageUpload);
		String password = null, stage = null;
		;
		String[] newParameters = (String[]) parameters.clone();
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				password = parameters[i + 2];
				stage = parameters[i + 3];
				newParameters[i] = "";
				newParameters[i + 1] = "";
				newParameters[i + 2] = "--";
			}
		}
		if (password == null)
			throw new IllegalArgumentException("Parameters not found");
		String rememberKey = null, salt = "";
		MessageDigest digest = null;
		while (true) {
			int pos = password.indexOf(',');
			if (password.startsWith("/remember:")) {
				rememberKey = parameters[Integer.parseInt(password.substring(10, pos))];
			} else if (password.startsWith("/printhash:")) {
				// ignore
			} else if (password.startsWith("/")) {
				String hashalg = password.substring(1, pos);
				int pos2 = hashalg.indexOf(':');
				if (pos2 != -1) {
					salt = hashalg.substring(pos2 + 1);
					hashalg = hashalg.substring(0, pos2);
				}
				digest = MessageDigest.getInstance(hashalg);
			} else {
				break;
			}
			password = password.substring(pos + 1);
		}
		if (rememberKey != null) {
			if (System.getProperty(PROPERTY_PREFIX + rememberKey, "").equals("1")) {
				password = null;
			}
		}
		while (password != null) {
			out.write(PROMPT.getBytes("UTF-8"));
			String value = in.readLine();
			if (value == null)
				return;
			boolean ok = false;
			if (digest != null)
				ok = hexify(digest.digest((value + salt).getBytes("UTF-8"))).equals(password);
			else
				ok = value.equals(password);
			if (ok)
				password = null;
		}
		if (rememberKey != null) {
			System.setProperty(PROPERTY_PREFIX + rememberKey, "1");
		}
		Stage realStage = (Stage) Class.forName("javapayload.stage." + stage).newInstance();
		realStage.start(new DataInputStream(new SequenceInputStream(new ByteArrayInputStream(stageUpload), in)), out, newParameters);
		parameters[0] = newParameters[0];
	}

	public static String hexify(byte[] data) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int b = data[i] & 0xff;
			sb.append(b < 0x10 ? "0" : "").append(Integer.toHexString(b));
		}
		return sb.toString();
	}
}
