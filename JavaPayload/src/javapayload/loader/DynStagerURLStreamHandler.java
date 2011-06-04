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

package javapayload.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

public class DynStagerURLStreamHandler extends URLStreamHandler {

	private static DynStagerURLStreamHandler instance;

	public static synchronized DynStagerURLStreamHandler getInstance() throws Exception {
		if (instance == null) {
			instance = new DynStagerURLStreamHandler();
		}
		return instance;
	}

	final URLClassLoader dynClassLoader;
	Map classes = new HashMap();

	private DynStagerURLStreamHandler() throws Exception {
		URL myURL = new URL("dynstager", null, 0, "/", this);
		dynClassLoader = new URLClassLoader(new URL[] { myURL });
	}

	public URLClassLoader getDynClassLoader() {
		return dynClassLoader;
	}

	public synchronized void addStager(String stagerName, byte[] classBytes) {
		addClass("javapayload.stager." + stagerName, classBytes);
	}
	
	public synchronized void addClass(String className, byte[] classBytes) {
		classes.put("/"+className.replace('.', '/') + ".class", classBytes);
	}

	protected synchronized URLConnection openConnection(URL u) throws IOException {
		String path = u.getFile();
		final byte[] content = (byte[]) classes.get(path);
		if (content == null)
			throw new IOException("Class not found");
		return new URLConnection(u) {

			public void connect() throws IOException {
			}

			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(content);
			}

			public int getContentLength() {
				return content.length;
			}

			public String getContentType() {
				return "application/octet-stream";
			}
		};
	}
}