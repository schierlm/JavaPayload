/*
 * Java Payloads.
 * 
 * Copyright (c) 2012 Michael 'mihi' Schierl
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javapayload.Parameter;

public class Background extends StageHandler {

	public Background() {
		super("Run multiple stages in the background", true, true,
				"Runs multiple stages in the background and interacts with the last one.\r\n" +
				"Useful in combination with LaunchStager to have several stagers available.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("STAGES", false, Parameter.TYPE_STAGE, "Stages"),
				new Parameter("STAGES", true, Parameter.TYPE_STAGELIST_3DASHES, "Stages")
		};
	}

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
				currentParameters.add(0, "--");
				currentParameters.add(0, "Background");
				String[] params = (String[]) currentParameters.toArray(new String[currentParameters.size()]);
				currentParameters.clear();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				StageHandler realStageHandler = (StageHandler) Class.forName("javapayload.handler.stage." + params[2]).newInstance();
				realStageHandler.consoleIn = consoleIn;
				realStageHandler.consoleOut = consoleOut;
				realStageHandler.consoleErr = consoleErr;
				realStageHandler.handleBootstrap(params, dos);
				dos.close();
				out.writeInt(baos.size());
				out.write(baos.toByteArray());
			} else if (parameters[i].startsWith("---")) {
				currentParameters.add(parameters[i].substring(1));
			} else {
				currentParameters.add(parameters[i]);
			}
		}
		currentParameters.add(0, "--");
		currentParameters.add(0, "Background");
		String[] params = (String[]) currentParameters.toArray(new String[currentParameters.size()]);
		StageHandler realStageHandler = (StageHandler) Class.forName("javapayload.handler.stage." + params[2]).newInstance();
		realStageHandler.consoleIn = consoleIn;
		realStageHandler.consoleOut = consoleOut;
		realStageHandler.consoleErr = consoleErr;
		realStageHandler.handleBootstrap(params, out);
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
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
				currentParameters.clear();
			} else if (parameters[i].startsWith("---")) {
				currentParameters.add(parameters[i].substring(1));
			} else {
				currentParameters.add(parameters[i]);
			}
		}
		currentParameters.add(0, "--");
		currentParameters.add(0, "Background");
		String[] params = (String[]) currentParameters.toArray(new String[currentParameters.size()]);
		StageHandler realStageHandler = (StageHandler) Class.forName("javapayload.handler.stage." + params[2]).newInstance();
		realStageHandler.consoleIn = consoleIn;
		realStageHandler.consoleOut = consoleOut;
		realStageHandler.consoleErr = consoleErr;
		realStageHandler.handleStreams(out, in, params);
	}

	public Class[] getNeededClasses() {
		return new Class[] { javapayload.stage.Stage.class, javapayload.stage.MultiStageClassLoader.class, javapayload.stage.Background.class };
	}

	protected StageHandler createClone() {
		return new Background();
	}
}