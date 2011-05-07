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

package javapayload.stage;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import javapayload.stager.Stager;

public class LaunchStager implements Stage, Runnable {

	private String[] args = null;
	private Throwable stagerException = null;
	private Stager stager;

	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				args = new String[parameters.length - (i + 2)];
				for (int j = 0; j < args.length; j++) {
					args[j] = parameters[i + 2 + j];
					if (args[j].startsWith("---"))
						args[j] = args[j].substring(1);
				}
			}
		}
		stager = (Stager) Class.forName("javapayload.stager." + args[0]).newInstance();
		new Thread(this).start();
		stager.waitReady();
		PrintStream pout = new PrintStream(out);
		if (stagerException != null) {
			pout.println("Stager start failed: ");
			stagerException.printStackTrace(pout);
		} else {
			pout.println("Stager started.");
		}
		pout.close();
	}

	public void run() {
		try {
			stager.bootstrap(args, true);
		} catch (Throwable t) {
			stagerException = t;
		}
	};
}
