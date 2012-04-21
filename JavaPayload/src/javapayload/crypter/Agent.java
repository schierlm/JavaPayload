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

package javapayload.crypter;

import java.lang.instrument.Instrumentation;
import java.util.jar.Manifest;

import javapayload.Parameter;

public class Agent extends TemplateBasedJarLayout {

	public Agent() {
		super("Agent Jar", Template.class,
				"Use this jar layout with Agent Jar files.");
	}

	public Parameter[] getParameters() {
		return new Parameter[0];
	}

	public void init(String[] parameters, Manifest manifest) throws Exception {
		targetClassName = manifest.getMainAttributes().getValue("Agent-Class");
		manifest.getMainAttributes().putValue("Agent-Class", stubClassName);
		manifest.getMainAttributes().putValue("Premain-Class", stubClassName);
	}

	public static class Template {
		public static Class target;

		public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
			TemplateBasedJarLayout.cryptedMain(new String[] { "TARGET_CLASS_NAME", "STUB_CLASS_NAME", "target" });
			target.getMethod("agentmain", new Class[] { Class.forName("java.lang.String"), Class.forName("java.lang.instrument.Instrumentation") }).invoke(null, new Object[] { agentArgs, inst });
		}

		public static void premain(String agentArgs, Instrumentation inst) throws Exception {
			TemplateBasedJarLayout.cryptedMain(new String[] { "TARGET_CLASS_NAME", "STUB_CLASS_NAME", "target" });
			target.getMethod("premain", new Class[] { Class.forName("java.lang.String"), Class.forName("java.lang.instrument.Instrumentation") }).invoke(null, new Object[] { agentArgs, inst });
		}
	}
}
