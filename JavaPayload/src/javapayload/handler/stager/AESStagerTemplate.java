/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, 2011 Michael 'mihi' Schierl.
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

import javapayload.handler.stage.StageHandler;

public class AESStagerTemplate extends StagerHandler {

	StagerHandler handler = new LocalTest();

	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg) throws Exception {
		String[] newParameters = new String[parameters.length - 1];
		System.arraycopy(parameters, 2, newParameters, 1, newParameters.length - 1);
		newParameters[0] = parameters[0];
		handler.handle(new AESStageHandler(parameters[1], stageHandler), newParameters, errorStream, extraArg);
	}

	protected boolean prepare(String[] parametersToPrepare) throws Exception {
		String[] temp = new String[parametersToPrepare.length - 1];
		System.arraycopy(parametersToPrepare, 2, temp, 1, temp.length - 1);
		temp[0] = parametersToPrepare[0].substring(3);
		boolean changed = handler.prepare(temp);
		if (changed)
			System.arraycopy(temp, 1, parametersToPrepare, 2, temp.length - 1);
		if (parametersToPrepare[1].equals("#")) {
			parametersToPrepare[1] = AESStageHandler.generatePassword();
			changed = true;
		}
		return changed;
	}

	protected boolean needHandleBeforeStart() {
		return handler.needHandleBeforeStart();
	}

	protected boolean canHandleExtraArg(Class argType) {
		return handler.canHandleExtraArg(argType);
	}

	protected String getTestArguments() {
		String handlerArgs = handler.getTestArguments();
		return handlerArgs == null ? null : "# " + handlerArgs;
	}
}
