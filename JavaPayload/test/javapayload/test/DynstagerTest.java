/*
 * Java Payloads.
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
package javapayload.test;

import java.util.ArrayList;
import java.util.List;

import javapayload.Module;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.stager.LocalTest;
import javapayload.handler.stager.ReverseTCP;
import javapayload.handler.stager.StagerHandler;
import javapayload.stage.StageMenu;

public class DynstagerTest {

	public static void main(String[] args) throws Exception {
		Module[] modules = Module.loadAll(DynStagerHandler.class);
		List dynstagerList = new ArrayList();
		for (int i = 0; i < modules.length; i++) {
			DynStagerHandler d = (DynStagerHandler) modules[i];
			try {
				d.getTestExtraArg();
				dynstagerList.add(d);
			} catch (UnsupportedOperationException ex) {
			}
		}
		DynStagerHandler[] dynstagers = (DynStagerHandler[]) dynstagerList.toArray(new DynStagerHandler[dynstagerList.size()]);
		for (int i = 0; i < dynstagers.length; i++) {
			DynStagerHandler d1 = dynstagers[i];
			testDynstager(new DynStagerHandler[] { d1 }, false);
		}
		for (int i = 0; i < dynstagers.length; i++) {
			DynStagerHandler d1 = dynstagers[i];
			for (int j = 0; j < dynstagers.length; j++) {
				DynStagerHandler d2 = dynstagers[j];
				if (d2.isDynstagerUsableWith(new DynStagerHandler[] { d1 })) {
					testDynstager(new DynStagerHandler[] { d1, d2 }, true);
				}
			}
		}
		for (int i = 0; i < dynstagers.length; i++) {
			DynStagerHandler d1 = dynstagers[i];
			for (int j = 0; j < dynstagers.length; j++) {
				DynStagerHandler d2 = dynstagers[j];
				if (d2.isDynstagerUsableWith(new DynStagerHandler[] { d1 })) {
					for (int k = 0; k < dynstagers.length; k++) {
						DynStagerHandler d3 = dynstagers[k];
						if (d3.isDynstagerUsableWith(new DynStagerHandler[] { d1, d2 })) {
							testDynstager(new DynStagerHandler[] { d1, d2, d3 }, true);
						}
					}
				}
			}
		}
	}

	private static void testDynstager(DynStagerHandler[] dynStagerHandlers, boolean fast) throws Exception {
		StagerHandler localTest = new LocalTest();
		if (localTest.isStagerUsableWith(dynStagerHandlers)) {
			String name = buildDynStagerName(dynStagerHandlers, "LocalTest" + (fast ? " Fast" : ""));
			System.out.println("\t" + name);
			final StagerHandler.Loader loader = new StagerHandler.Loader(StageMenu.splitArgs(name + " # # # # # -- TestStub"));
			loader.handle(System.err, null);
		}
		StagerHandler reverseTCP = new ReverseTCP();
		if (reverseTCP.isStagerUsableWith(dynStagerHandlers)) {
			StagerTest.testStager(buildDynStagerName(dynStagerHandlers, "ReverseTCP"), fast);
		}
	}

	private static String buildDynStagerName(DynStagerHandler[] dynStagerHandlers, String stagerName) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < dynStagerHandlers.length; i++) {
			sb.append(dynStagerHandlers[i].getName());
			if (dynStagerHandlers[i].getExtraArg() != null) {
				String testExtraArg = dynStagerHandlers[i].getTestExtraArg();
				if (testExtraArg == null)
					throw new IllegalStateException("No text extra arg!");
				sb.append('$').append(testExtraArg);
			}
			sb.append('_');
		}
		return sb.append(stagerName).toString();
	}
}
