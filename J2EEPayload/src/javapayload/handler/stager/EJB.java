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

import j2eepayload.ejb.JavaPayload;
import j2eepayload.ejb.JavaPayloadHome;

import java.io.PrintStream;

import javapayload.Parameter;
import javapayload.handler.stage.StageHandler;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

public class EJB extends StagerHandler {

	// TODO make this an Injector?
	
	public EJB() {
		super("Run a stager on an EJB Server", true, false, 
				"This stager can launch another stager via EJB, and is therefore more like an\r\n" +
				"injector. The EJB communication is taken from jndi.properties file.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("STAGER", false, Parameter.TYPE_STAGER, "Stager to run")
		};
	}
	
	protected void handle(final StageHandler stageHandler, String[] parameters, final PrintStream errorStream, final Object extraArg, final StagerHandler readyHandler) throws Exception {
		final String[] realParameters = new String[parameters.length-1];
		System.arraycopy(parameters, 1, realParameters, 0, realParameters.length);
		final StagerHandler realHandler = StagerHandler.getStagerHandler(realParameters[0]);
		Thread beforeThread = null;
		if (realHandler.needHandleBeforeStart()) {
			realHandler.prepare(realParameters); // may modify realParameters
			beforeThread = new Thread(new Runnable() {
				public void run() {
					try {
						realHandler.handle(stageHandler, realParameters, errorStream, extraArg, readyHandler);
					} catch (final Exception ex) {
						ex.printStackTrace();
					}
				}
			});
			beforeThread.start();
		}  
		Context ctx = new InitialContext();
		JavaPayloadHome homeObject = (JavaPayloadHome)PortableRemoteObject.narrow(ctx.lookup(JavaPayloadHome.JNDI_NAME),JavaPayloadHome.class);
		JavaPayload remoteObject = homeObject.create();
		remoteObject.runPayload(realParameters);
		if (!realHandler.needHandleBeforeStart())
			realHandler.handle(stageHandler, realParameters, errorStream, extraArg, readyHandler);
		if (beforeThread != null)
			beforeThread.join();
	}

	protected boolean needHandleBeforeStart() {
		throw new IllegalStateException("No extra handler needed");
	}
	
	protected String getTestArguments() {
		return null;
	}
}
