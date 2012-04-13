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
package j2eepayload.servlet;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;

import jtcpfwd.util.PollingHandler;
import jtcpfwd.util.http.HTTPTunnelClient;

public class DeadConnectClientHelper {

	public static Object[] go(String baseURL, String token, String timeout) throws Exception {
		PrintStream nullStream = new PrintStream(new ByteArrayOutputStream());
		return go(baseURL, token, timeout, nullStream, nullStream);
	}
	
	public static Object[] go(String baseURL, String token, String timeout, PrintStream outputStream, PrintStream errorStream) throws Exception {
		final int timeoutValue;
		if (timeout.equals("--"))
			timeoutValue = 0;
		else
			timeoutValue=Integer.parseInt(timeout);
		String createParam = "create=" + URLEncoder.encode(token,"UTF-8");
		
		PipedInputStream localIn = new PipedInputStream();
		final PollingHandler pth = new PollingHandler(new PipedOutputStream(localIn), 1048576);
		HTTPTunnelClient.handle(baseURL, timeoutValue, createParam, pth, outputStream, errorStream);
		return new Object[] {localIn, pth};
	}
}
