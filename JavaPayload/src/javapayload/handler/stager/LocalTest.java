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
package javapayload.handler.stager;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import javapayload.Parameter;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.dynstager.LocalStage;
import javapayload.handler.dynstager.Spawn;
import javapayload.handler.stage.StageHandler;
import javapayload.loader.DynLoader;
import javapayload.stager.Stager;

public class LocalTest extends StagerHandler implements Runnable {

	public LocalTest() {
		super("Test a stage locally", true, false, "Test stager that runs a stage in the same JVM as the stager.");
	};
	
	public Parameter[] getParameters() {
		return new Parameter[0];
	}
	
	public boolean isStagerUsableWith(DynStagerHandler[] dynstagers) {
		for (int i = 0; i < dynstagers.length; i++) {
			if (dynstagers[i] instanceof Spawn)
				return false;
			if (dynstagers[i] instanceof LocalStage && i < dynstagers.length-1)
				return false;
		}
		return true;
	}
	
	private InputStream in;
	private OutputStream out;
	private PrintStream errorStream;

	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (readyHandler != null) readyHandler.notifyReady();
		this.errorStream = errorStream;
		final PipedInputStream localIn = new PipedInputStream();
		final PipedOutputStream localOut = new PipedOutputStream();
		final WrappedPipedOutputStream wrappedLocalOut = new WrappedPipedOutputStream(localOut);
		out = new WrappedPipedOutputStream(new PipedOutputStream(localIn), wrappedLocalOut);
		in = new PipedInputStream(localOut);
		new Thread(this).start();
		stageHandler.handle(wrappedLocalOut, localIn, parameters);
	}

	public void run() {
		try {
			try {
				if (!originalParameters[0].equals("LocalTest")) {
					((Stager)DynLoader.loadStager(originalParameters[0], originalParameters, 0).getConstructor(new Class[] {InputStream.class, OutputStream.class}).newInstance(new Object[] {in, out})).bootstrap(originalParameters, false);
					return;
				}
			} catch (Throwable t) {
				// fall through
			}
			new javapayload.stager.LocalTest(in, out).bootstrap(originalParameters, false);
		} catch (final Exception ex) {
			ex.printStackTrace(errorStream);
		}
	}
	
	protected boolean needHandleBeforeStart() {
		throw new RuntimeException("LocalTest cannot be used as a standalone stager!");
	}
	
	protected String getTestArguments() {
		return null;
	}
}
