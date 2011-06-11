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

package javapayload.cli;

import javapayload.Module;
import javapayload.Parameter;
import javapayload.builder.Injector;
import javapayload.handler.stager.StagerHandler;

public class InjectorCommand extends Command {
	public InjectorCommand() {
		super("Start an injector",
				"This command can be used to start an injector.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] { new Parameter("INJECTOR", false, Command.TYPE_INJECTOR, "injector to run"), };
	}

	public void execute(String[] parameters) throws Exception {
		Injector injector = (Injector) Module.load(Injector.class, parameters[0] + "Injector");
		final String[] injectorArgs = new String[injector.getParameters().length];
		System.arraycopy(parameters, 1, injectorArgs, 0, injectorArgs.length);
		final String[] stagerArgs = shiftArray(parameters, injectorArgs.length + 1);
		StagerHandler.Loader loader = new StagerHandler.Loader(stagerArgs);
		initIO(loader);
		injector.inject(injectorArgs, loader, stagerArgs);
		finishIO(loader);
	}
}
