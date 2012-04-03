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

package javapayload.handler.dynstager;

import javapayload.Parameter;


public class Spawn extends DynStagerHandler {

	public Spawn() {
		super("Run stager in a subprocess", true, true,
				"Spawn a subprocess to run the stager in");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[0];
	}
	
	public Parameter getExtraArg() {
		return null;
	}
	
	public boolean isDynstagerUsableWith(DynStagerHandler[] dynstagers) {
		for (int i = 0; i < dynstagers.length; i++) {
			if (!(dynstagers[i] instanceof Spawn))
				return false;
		}
		return true;
	}
	
	protected boolean prepare(String[] parametersToPrepare) throws Exception {
		boolean fCutSpawn = parametersToPrepare.length >= 1 && parametersToPrepare[0].startsWith("Spawn_");
		if (fCutSpawn)
			parametersToPrepare[0] = parametersToPrepare[0].substring(6);
		try {
			return super.prepare(parametersToPrepare);
		} finally {
			if (fCutSpawn)
				parametersToPrepare[0] = "Spawn_" + parametersToPrepare[0];
		}
	}
	
	public String getTestExtraArg() {
		return null;
	}
}
