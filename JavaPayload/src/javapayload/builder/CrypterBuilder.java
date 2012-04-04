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

package javapayload.builder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javapayload.Module;
import javapayload.crypter.Crypter;
import javapayload.stage.StreamForwarder;

public class CrypterBuilder extends Builder {

	public static final String CRYPTER_PROPERTY = "javapayload.crypter";

	public CrypterBuilder() {
		super("Crypt a standalone Class file", "");
	}

	public String getParameterSyntax() {
		return "<inputClass> <outputClass> <crypter> | --set [<crypter>]";
	}

	public void build(String[] args) throws Exception {
		if (args[0].equals("--set")) {
			System.setProperty(CRYPTER_PROPERTY, args.length == 1 ? "" : args[1]);
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamForwarder.forward(new FileInputStream(args[0] + ".class"), baos);
			FileOutputStream out = new FileOutputStream(args[1] + ".class");
			Crypter crypter = (Crypter) Module.load(Crypter.class, args[2]);
			out.write(crypter.crypt(args[1], baos.toByteArray()));
			out.close();
		}
	}
}
