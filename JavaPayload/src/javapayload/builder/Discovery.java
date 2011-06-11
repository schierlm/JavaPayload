/*
 * Java Payloads.
 * 
 * Copyright (c) 2011 Michael 'mihi' Schierl
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

import javapayload.Module;
import javapayload.IOEnabledModule;
import javapayload.Parameter;

public abstract class Discovery extends IOEnabledModule {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.Discovery <module> <arguments>");
			System.out.println();
			System.out.println("Supported discovery modules:");
			Module.list(System.out, Discovery.class);
			return;
		}
		Discovery discoveryModule = (Discovery) Module.load(Discovery.class, args[0] + "Discovery");
		Parameter[] params = discoveryModule.getParameters();
		if (args.length < params.length + 3) {
			System.out.println("Usage: java javapayload.builder.Discovery " + discoveryModule.getNameAndParameters());
			System.out.println();
			System.out.println(discoveryModule.getSummary());
			System.out.println();
			System.out.println(discoveryModule.getDescription());
			System.out.println();
			discoveryModule.printParameterDescription(System.out);
			return;
		}
		discoveryModule.discover(shiftArray(args, 1));
	}

	protected Discovery(String summary, String description) {
		super("Discovery", Discovery.class, summary, description);
	}

	public abstract void discover(String[] parameters) throws Exception;
}
