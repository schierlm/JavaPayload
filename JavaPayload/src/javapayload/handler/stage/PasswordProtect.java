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

package javapayload.handler.stage;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javapayload.Parameter;

public class PasswordProtect extends StageHandler {

	public PasswordProtect() {
		super("Ask for a password before running a stage", true, true,
				"Usually used from Integrated dynstagers. The user will have to enter the \r\n" +
						"password before the stager runs. The password can be either given in plain\r\n" +
						"text, or in the form /<HASHALG>:<salt>,<hash>. Add a /remember:<arg>, to the\r\n" +
						"start of the password, to remember the password once entered correctly, for all\r\n" +
						"connections where the <arg>th parameter is the same (Useful with the RemoteIP\r\n" +
						"feature of the BindMultiTCP stager). To generate salted hashes, use the\r\n" +
						"/printhash:<HASHALG>,<plain> syntax which will make the stage handler print\r\n" +
						"the hashed password before connecting.\r\n" +
						"Note that if you want to use this as a stage handler, you'll have to give the\r\n" +
						"plain text password.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("PASSWORD", false, Parameter.TYPE_ANY, "Password and optional switches"),
				new Parameter("STAGE", false, Parameter.TYPE_STAGE, "Stage to load")
		};
	}

	protected String[] realParameters = null;

	protected StageHandler findRealStageHandler(String[] parameters) throws Exception {
		String realStage = null;
		realParameters = (String[]) parameters.clone();
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				realParameters[i] = "";
				realParameters[i + 1] = "";
				realParameters[i + 2] = "--";
				realStage = parameters[i + 3];
				break;
			}
		}
		if (realStage == null)
			throw new IllegalArgumentException("Cannot find stage");
		StageHandler realStageHandler = (StageHandler) Class.forName("javapayload.handler.stage." + realStage).newInstance();
		realStageHandler.consoleIn = consoleIn;
		realStageHandler.consoleOut = consoleOut;
		realStageHandler.consoleErr = consoleErr;
		return realStageHandler;
	}

	protected void customUpload(DataOutputStream out, String[] parameters) throws Exception {
		StageHandler realStageHandler = findRealStageHandler(parameters);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		realStageHandler.customUpload(new DataOutputStream(baos), realParameters);
		out.writeInt(baos.size());
		baos.writeTo(out);
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
		String password = null;
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				password = parameters[i + 2];
				break;
			}
		}
		if (password == null)
			throw new IllegalArgumentException("Cannot find stage");
		String hashalg = null;
		while (true) {
			if (password.startsWith("/remember:")) {
				// ignore
			} else if (password.startsWith("/printhash:")) {
				hashalg = password.substring("/printhash:".length(), password.indexOf(','));
			} else if (password.startsWith("/")) {
				throw new IllegalArgumentException("Hashed passwords are not supported in the stage handler");
			} else {
				break;
			}
			password = password.substring(password.indexOf(',') + 1);
		}
		if (hashalg != null) {
			MessageDigest md = MessageDigest.getInstance(hashalg);
			byte[] salt = new byte[8];
			new SecureRandom().nextBytes(salt);
			System.out.println("Hashed: /" + hashalg + ":" + javapayload.stage.PasswordProtect.hexify(salt) + "," + javapayload.stage.PasswordProtect.hexify(md.digest((password + javapayload.stage.PasswordProtect.hexify(salt)).getBytes("UTF-8"))));
		}
		DataInputStream dis = new DataInputStream(in);
		byte[] prompt = new byte[javapayload.stage.PasswordProtect.PROMPT.length()];
		dis.readFully(prompt);
		if (!new String(prompt, "UTF-8").equals(javapayload.stage.PasswordProtect.PROMPT))
			throw new IOException("Invalid password prompt received");
		out.write((password + "\n").getBytes("UTF-8"));
		out.flush();
		findRealStageHandler(parameters).handleStreams(out, dis, realParameters);
	}

	public Class[] getNeededClasses(String[] parameters) throws Exception {
		StageHandler realStageHandler = findRealStageHandler(parameters);
		Class[] baseClasses = realStageHandler.getNeededClasses(realParameters);
		Class[] result = new Class[baseClasses.length + 1];
		System.arraycopy(baseClasses, 0, result, 0, baseClasses.length);
		result[baseClasses.length] = javapayload.stage.PasswordProtect.class;
		return result;
	}

	public Class[] getNeededClasses() {
		throw new IllegalStateException("Parameters needed to determine classes");
	}

	protected StageHandler createClone() {
		return new PasswordProtect();
	}
}