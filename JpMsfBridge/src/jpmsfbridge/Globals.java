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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import javapayload.Module;

public class Globals {
	public static final Globals instance = new Globals();

	private String classPath = null;
	private String javaExecutable = null;

	public String getClassPath() throws Exception {
		if (classPath != null)
			return classPath;
		StringBuffer sb = new StringBuffer(".");
		URL[] urls = new URL[0];
		ClassLoader ucl = Module.class.getClassLoader();
		if (ucl instanceof URLClassLoader)
			urls = ((URLClassLoader) ucl).getURLs();
		for (int i = 0; i < urls.length; i++) {
			File f = Module.urlToFile(urls[i]);
			sb.append(File.pathSeparatorChar).append(f.getAbsolutePath());
		}
		return classPath = sb.toString();
	}

	public String getJavaExecutable() {
		return javaExecutable;
	}

	protected void setJavaExecutable(String javaExecutable) {
		this.javaExecutable = javaExecutable;
	}

	public String toLowerUnderscores(String input) {
		return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}

	public String indent(String input, int tabCount) {
		return input.replaceAll("\n", "\n\t\t\t\t\t\t\t\t\t\t".substring(0, tabCount + 1));
	}
}
