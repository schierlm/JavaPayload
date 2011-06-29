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

import java.io.IOException;
import java.io.PrintStream;

import javapayload.IOEnabledModule;
import javapayload.handler.stager.StagerHandler.Loader;

public abstract class Command extends IOEnabledModule {

	// additional parameter types for commands
	public static final int TYPE_COMMAND = 80;
	public static final int TYPE_MODULETYPE = 81;
	public static final int TYPE_MODULE_BY_TYPE = 82;
	public static final int TYPE_STAGE_2DASHES = 83;
	public static final int TYPE_DISCOVERY = 84;
	public static final int TYPE_INJECTOR = 85;
	public static final int TYPE_BUILDER = 86;
	public static final int TYPE_STAGER_HANDLER = 87;
	public static final int TYPE_STAGER_AND_HANDLER = 88;

	public Main mainClass = null;

	public Command(String summary, String description) {
		super("Command", Command.class, summary, description);
	}

	public String getName() {
		return super.getName().toLowerCase();
	}
	
	public void initIO(Loader loader) {
		loader.stageHandler.consoleIn = new LocalCloseInputStream(consoleIn);		
		loader.stageHandler.consoleOut = new PrintStream(new LocalCloseOutputStream(consoleOut));
		loader.stageHandler.consoleErr = new PrintStream(new LocalCloseOutputStream(consoleErr));
	}
	
	public void finishIO(Loader loader) throws IOException {
		LocalCloseInputStream lcis = (LocalCloseInputStream) loader.stageHandler.consoleIn;
		lcis.closeAsync(consoleOut);
	}
	
	public static String getModuleName(String command) {
		return command.length() == 0 ? "" : command.substring(0,1).toUpperCase()+command.substring(1).toLowerCase() + "Command";
	}
	
	public abstract void execute(String[] parameters) throws Exception;
}
