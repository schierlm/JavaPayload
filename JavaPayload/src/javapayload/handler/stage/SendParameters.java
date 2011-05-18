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

import java.io.DataOutputStream;
import java.io.InputStream;

public class SendParameters extends FilterStageHandler {

	public SendParameters() {
		super("Send/Overwrite parameters", 
				"This filter stage will send its parameters over the staging connection, so\r\n" +
				"that parameters can be overwritten dynamically by the attacker.");
	}
	
	protected void customUpload(DataOutputStream out, String[] parameters) throws Exception {
		StageHandler realStageHandler = findRealStageHandler(parameters);
		int cnt = parameters.length - realStageOffset;
		out.writeShort(cnt);
		for (int i = 0; i < cnt; i++) {
			out.writeUTF(parameters[i + realStageOffset]);
		}
		realStageHandler.customUpload(out, realParameters);
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
		findRealStageHandler(parameters).handleStreams(out, in, realParameters);
	}

	public Class[] getNeededClasses(String[] parameters) throws Exception {
		StageHandler realStageHandler = findRealStageHandler(parameters);
		Class[] baseClasses = realStageHandler.getNeededClasses(realParameters);
		Class[] result = new Class[baseClasses.length + 1];
		System.arraycopy(baseClasses, 0, result, 0, baseClasses.length);
		result[baseClasses.length] = javapayload.stage.SendParameters.class;
		return result;
	}

	public Class[] getNeededClasses() {
		throw new IllegalStateException("Parameters needed to determine classes");
	}

	protected StageHandler createClone() {
		return new SendParameters();
	}
}