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

package javapayload.handler.stage;

import javapayload.Parameter;
import javapayload.loader.DynLoader;

public class LaunchStager extends StageHandler {

	public LaunchStager() {
		super("Launch another stager to be connected to via a different channel", true, true, 
				"Launch a stager given by the parameters. It will not be tunneled via the\r\n" +
				"current stage, but act independently from it. Useful to have multiple\r\n" +
				"concurrent stages running from a MultiStage stage.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("STAGER", false, Parameter.TYPE_STAGER, "Stager to run"),
				new Parameter("STAGE", false, Parameter.TYPE_STAGE_3DASHES, "Stage to run")
		};
	}
	
	public Class[] getNeededClasses(String[] parameters) throws Exception {
		String stager = null;
		int firstArg = -1;
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				firstArg = i + 2;
				break;
			}
		}
		if (firstArg == -1) throw new IllegalStateException("No stager given");
		stager = parameters[firstArg];
		
		return new Class[] {
				javapayload.stage.Stage.class,
				javapayload.stager.Stager.class,
				DynLoader.loadStager(stager, parameters, firstArg),
				javapayload.stage.LaunchStager.class
		};
	}

	public Class[] getNeededClasses() {
		throw new IllegalStateException("Parameters needed to determine classes");
	}

	protected StageHandler createClone() {
		return new LaunchStager();
	}
}