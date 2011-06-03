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

package javapayload.handler.dynstager;

import java.io.PrintStream;

import javapayload.Parameter;
import javapayload.handler.stage.InternalIntegratedStageHandler;
import javapayload.handler.stage.StageHandler;
import javapayload.handler.stager.StagerHandler;

public class Integrated extends DynStagerHandler {

	public Integrated() {
		super("Include bootstrap data of a stage into stager", true, true, 
				"In some scenarios it is helpful if you can reduce the amount of data\r\n" +
				"transferred, or if you can connect to an exploit/payload without having\r\n" +
				"JavaPayload available. The Integrated dynstager can be used to build a\r\n" +
				"stager that integrates all the classes of a particular stage.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[0];
	}
	
	public Parameter getExtraArg() {
		return new Parameter("TOKEN", false, Parameter.TYPE_ANY, "Token to distinguish this Integrated dynstager from others");
	}
	
	protected void handleDyn(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		super.handleDyn(new InternalIntegratedStageHandler(stageHandler), parameters, errorStream, extraArg, readyHandler);
	}
}
