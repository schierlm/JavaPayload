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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class StageMenu extends StageHandler {
		
	protected void customUpload(DataOutputStream out, String[] parameters) throws Exception {		
		boolean go = false;
		List currentParameters = new ArrayList();
		for (int i = 0; i < parameters.length; i++) {
			if (!go) {
				if (parameters[i].equals("--")) {
					go = true;
					i++;
				}
				continue;
			}
			if (parameters[i].equals("---")) {
				uploadStage(out, currentParameters);
			} else if (parameters[i].startsWith("---")) {
				currentParameters.add(parameters[i].substring(1));
			} else {
				currentParameters.add(parameters[i]);
			}
		}
		uploadStage(out, currentParameters);
	}
	
	private void uploadStage(DataOutputStream out, List currentParameters) throws Exception {
		currentParameters.add(0, "--");
		currentParameters.add(0, "StageMenu");
		String[] parameters = (String[]) currentParameters.toArray(new String[currentParameters.size()]);
		currentParameters.clear();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		StageHandler realStageHandler = (StageHandler) Class.forName("javapayload.handler.stage." + parameters[2]).newInstance();
		realStageHandler.consoleIn = consoleIn;
		realStageHandler.consoleOut = consoleOut;
		realStageHandler.consoleErr = consoleErr;
		realStageHandler.handleBootstrap(parameters, dos);
		dos.close();
		out.writeInt(baos.size());
		out.write(baos.toByteArray());
	}
	
	public Class[] getNeededClasses() {
		return new Class[] { javapayload.stage.Stage.class, javapayload.stage.StageMenu.class };
	}
	
	protected StageHandler createClone() {
		return new StageMenu();
	}
}