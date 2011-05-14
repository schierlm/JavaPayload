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
import javapayload.Parameter;
import javapayload.handler.stager.StagerHandler;

public abstract class Injector extends Module {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.Injector <injector> [<arguments> <stager> [stageroptions] -- <stage> [stageoptions]]");
			System.out.println();
			System.out.println("Supported injectors:");
			Module.list(System.out, Injector.class);
			return;
		}
		Injector injector = (Injector) Module.load(Injector.class, args[0] + "Injector");
		Parameter[] params = injector.getParameters();
		if (args.length < params.length + 3) {
			StringBuffer injectorParams = new StringBuffer();
			for (int i = 0; i < params.length; i++) {
				injectorParams.append(" <").append(params[i].getName()).append(">");
			}
			System.out.println("Usage: java javapayload.builder.Injector " + injector.getName() + injectorParams.toString() + " <stager> [stageroptions] -- <stage> [stageoptions]");
			System.out.println();
			System.out.println(injector.getSummary());
			System.out.println();
			System.out.println(injector.getDescription());
			System.out.println();
			injector.printParameterDescription(System.out);
			return;
		}
		String[] injectorArgs = new String[args.length - 1];
		System.arraycopy(args, 1, injectorArgs, 0, injectorArgs.length);
		injector.inject(injectorArgs);
	}

	protected Injector(String summary, String description) {
		super("Injector", Injector.class, summary, description);
	}

	public void inject(String[] args) throws Exception {
		final String[] parameters = new String[getParameters().length];
		System.arraycopy(args, 0, parameters, 0, parameters.length);
		final String[] stagerArgs = new String[args.length - parameters.length];
		System.arraycopy(args, parameters.length, stagerArgs, 0, stagerArgs.length);
		StagerHandler.Loader loader = new StagerHandler.Loader(stagerArgs);
		inject(parameters, loader, stagerArgs);
	}

	public abstract void inject(String[] parameters, StagerHandler.Loader loader, String[] stagerArgs) throws Exception;
}
