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
package javapayload.handler.stager;

import java.io.PrintStream;

import javapayload.Parameter;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.dynstager.Integrated;
import javapayload.handler.dynstager.LocalStage;
import javapayload.handler.stage.StageHandler;

public class Console extends StagerHandler {

	public Console() {
		super("Connect to the victim via stdin/stdout", false, false,
				"Connect to the victim via standard input and output streams. This stager can\r\n" +
				"only sensibly be used with integrated dynstagers.");
	}
	
	protected Console(String summary, boolean handlerUsable, boolean stagerUsable, String description) {
		super(summary, handlerUsable, stagerUsable, description);
	}
	
	public boolean isStagerUsableWith(DynStagerHandler[] dynstagers) {
		return dynstagers.length == 1 && (dynstagers[0] instanceof Integrated || dynstagers[0] instanceof LocalStage);
	}
	
	public Parameter[] getParameters() {
		return new Parameter[0];
	}
	
	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		throw new RuntimeException("Console cannot be used as a handler");	
	}
	
	protected boolean needHandleBeforeStart() {
		throw new RuntimeException("Console cannot be used as a standalone stager!");
	}
	
	protected String getTestArguments() {
		return null;
	}
}
