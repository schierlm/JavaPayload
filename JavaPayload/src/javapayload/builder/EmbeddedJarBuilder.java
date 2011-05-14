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

package javapayload.builder;

import java.util.jar.Manifest;

import javapayload.loader.DynLoader;

public class EmbeddedJarBuilder extends Builder {
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Usage: java javapayload.builder.EmbeddedJarBuilder "+new EmbeddedJarBuilder().getParameterSyntax());
			return;
		}
		new EmbeddedJarBuilder().build(args);
	}
	
	public EmbeddedJarBuilder() {
		super("Build a Jar that has its command line built in", "");
	}
	
	protected int getMinParameterCount() {
		return 4;
	}
	
	public String getParameterSyntax() {
		return "[--strip] [<filename>.jar] <stager> [stageroptions] -- <stage> [stageoptions]";
	}
	
	public void build(String[] args) throws Exception {
		boolean stripDebugInfo = false;
		if (args[0].equals("--strip")) {
			stripDebugInfo = true;
			String[] oldArgs = args;
			args = new String[oldArgs.length-1];
			System.arraycopy(oldArgs, 1, args, 0, args.length);
		}
		String jarName = "embedded.jar";
		if (args[0].endsWith(".jar")) {
			jarName = args[0];
			String[] oldArgs = args;
			args = new String[oldArgs.length-1];
			System.arraycopy(oldArgs, 1, args, 0, args.length);
		}
		final String stager = args[0];
		final Class[] classes = new Class[] { javapayload.loader.EmbeddedJarLoader.class, javapayload.stager.Stager.class, DynLoader.loadStager(stager, args, 0) };
		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Main-Class", "javapayload.loader.EmbeddedJarLoader");
		manifest.getMainAttributes().putValue("Argument-Count", "" + args.length);
		for (int i = 0; i < args.length; i++) {
			manifest.getMainAttributes().putValue("Argument-" + i, args[i]);
		}
		JarBuilder.buildJar(jarName, classes, stripDebugInfo, manifest, null, null);
	}
}
