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

package javapayload.builder;

import java.util.StringTokenizer;

import javapayload.stager.Stager;

public class EmbeddedClassBuilder extends Stager {

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Usage: java javapayload.builder.EmbeddedClassBuilder <classname> <stager> [stageroptions] -- <stage> [stageoptions]");
			return;
		}
		ClassBuilder.buildClass(args[0], args[1], EmbeddedClassBuilder.class, buildEmbeddedArgs(args), args);
	}

	public static String buildEmbeddedArgs(String[] args) {
		final StringBuffer embeddedArgs = new StringBuffer();
		for (int i = 1; i < args.length; i++) {
			if (i != 1) {
				embeddedArgs.append("\n");
			}
			embeddedArgs.append("$").append(args[i]);
		}
		return embeddedArgs.toString();
	}

	public static void mainToEmbed(String[] args) throws Exception {
		EmbeddedClassBuilder cb = new EmbeddedClassBuilder();
		boolean needWait = false;
		if (args.length == 1 && args[0].equals("+")) {
			args[0] = args[0].substring(1);
			needWait = true;
			byte[] clazz = "WAITER_THREAD".getBytes("ISO-8859-1");
			Thread waiterThread = (Thread)cb.defineClass(clazz, 0, clazz.length).getConstructors()[0].newInstance(new Object[] {cb});
			waiterThread.start();
		}
		final StringTokenizer tokenizer = new StringTokenizer("TO_BE_REPLACED", "\n");
		args = new String[tokenizer.countTokens()];
		for (int i = 0; i < args.length; i++) {
			args[i] = tokenizer.nextToken().substring(1);
		}
		cb.bootstrap(args, needWait);
	}
	
	public void bootstrap(String[] parameters, boolean needWait) throws Exception {
		throw new Exception("Never used!");
	}
	
	public void waitReady() {
		throw new RuntimeException("Never used!");
	}
}