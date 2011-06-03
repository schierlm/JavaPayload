/*
 * Java Payloads.
 * 
 * Copyright (c) 2011 Michael 'mihi' Schierl
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javapayload.builder.ClassBuilder;
import javapayload.builder.ClassBuilder.ClassBuilderTemplate;
import javapayload.builder.SpawnTemplate;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.stage.StageHandler;
import javapayload.stage.StreamForwarder;

public class SpawnConsole extends Console {

	public SpawnConsole() {
		super("Spawn a new process that communicates via stdin/stdout", true, false,
				"This handler will spawn a new Java process and connect to it via" +
				"stdin/stdout.");
	}
	
	public boolean isStagerUsableWith(DynStagerHandler[] dynstagers) {
		return false;
	}
	
	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (readyHandler != null) readyHandler.notifyReady();
		File tempFile = File.createTempFile("~console", ".tmp");
		tempFile.delete();
		File tempDir = new File(tempFile.getAbsolutePath()+".dir");
		tempDir.mkdir();
		tempFile = new File(tempDir, "ConsoleClass.class");
		FileOutputStream fos = new FileOutputStream(tempFile);
		fos.write(ClassBuilder.buildClassBytes("ConsoleClass", "Console", ClassBuilderTemplate.class, null, null));
		fos.close();
		Process proc = SpawnTemplate.launch("ConsoleClass", tempDir.getAbsolutePath(), parameters);
		new StreamForwarder(proc.getErrorStream(), stageHandler.consoleErr, null, false).start();
		stageHandler.handle(proc.getOutputStream(), proc.getInputStream(), parameters);
		proc.waitFor();
		tempFile.delete();
		tempDir.delete();		
	}
}
