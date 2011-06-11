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

import javapayload.Parameter;
import javapayload.stage.StreamForwarder;

public class ExecCommand extends Command {
	public ExecCommand() {
		super("Execute a native command",
				"Execute a native command using Runtime.exec().");
	}

	public Parameter[] getParameters() {
		return new Parameter[] { 
				new Parameter("EXECUTABLE", false, Parameter.TYPE_ANY, "Executable name"),
				new Parameter("ARGS", true, Parameter.TYPE_REST, "Arguments for the executable"),
		};
	}

	public void execute(String[] parameters) throws Exception {
		Process proc = Runtime.getRuntime().exec(parameters);
		LocalCloseInputStream lcis = new LocalCloseInputStream(consoleIn);
		new StreamForwarder(proc.getInputStream(), consoleOut, consoleErr, false).start();
		new StreamForwarder(proc.getErrorStream(), consoleErr, consoleErr, false).start();
		new StreamForwarder(lcis, proc.getOutputStream(), consoleErr).start();
		proc.waitFor();
		lcis.closeAsync(consoleOut);
	}
}
