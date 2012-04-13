/*
 * JpMsfBridge.
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
package jpmsfbridge;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;

/**
 * Utility class for creating log files in case of an error in jpmsfbridge.
 */
public class Logger {
	private static ByteArrayOutputStream out, err;
	private static PrintStream origOut, origErr;
	
	public static void startLogging(boolean redirectOut) throws IOException {
		if (out != null || err != null) {
			logEntry("startLogging: Streams already initialized.");
			return;
		}
		out = new ByteArrayOutputStream();
		err = new ByteArrayOutputStream();
		origOut = System.out;
		if (redirectOut)
			System.setOut(new PrintStream(out));
		origErr = System.err;
		System.setErr(new PrintStream(err));
	}
	
	public static void stopLogging(String[] args) throws IOException {
		if (out == null || err == null) {
			logEntry("stopLogging: Streams not initialized.");
			return;
		}
		System.setOut(origOut);
		System.setErr(origErr);
		if (out.size() > 0 || err.size() > 0) {
			logEntry("Arguments: \r\n"+Arrays.toString(args));
		}
		if (out.size() > 0) {
			logEntry("Output:\r\n"+new String(out.toByteArray()));
		}
		if (err.size() > 0) {
			logEntry("Error output:\r\n"+new String(err.toByteArray()));
		}
	}	
	
	public static void logEntry(String entry) throws IOException {
		File logFile = new File(System.getProperty("user.home"), ".msf4/logs/jpmsfbridge.log");
		BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
		bw.write("==================== "+new Date()+" ====================");
		bw.newLine();
		bw.write(entry);
		bw.newLine();
		bw.write("---------------------------------------------------------");
		bw.newLine();
		bw.close();
	}
}
