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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javapayload.Parameter;
import javapayload.builder.CryptedJarBuilder;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class War extends TemplateBasedJarLayout {

	private String webXML = null;

	public War() {
		super("War file with one or more servlets", Template.class, "");
	}

	public Parameter[] getParameters() {
		return new Parameter[0];
	}

	public void init(String[] parameters, Manifest manifest) throws Exception {
		stubClassName = null;
	}

	public boolean shouldInclude(JarEntry je, JarInputStream jis) throws Exception {
		if (!je.getName().equals("WEB-INF/web.xml"))
			return true;

		int length;
		byte[] buf = new byte[4096];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while ((length = jis.read(buf)) != -1) {
			out.write(buf, 0, length);
		}
		webXML = new String(out.toByteArray(), "UTF-8");
		return false;
	}

	public void addStubs(JarOutputStream jos, String cryptedLoaderClassName) throws Exception {
		if (webXML == null)
			throw new IllegalStateException("No WEB-INF/web.xml found in war file");

		// Parsing XML with regex. Kids, don't try this at home, better
		// depend on Java 1.5 and use XPath...
		StringBuffer sb = new StringBuffer();
		Matcher matcher = Pattern.compile("<servlet-class>([^<>]+)</servlet-class>").matcher(webXML);
		Map/* <String,String> */servletStubs = new HashMap();
		while (matcher.find()) {
			targetClassName = matcher.group(1);
			if (!servletStubs.containsKey(targetClassName)) {
				stubClassName = CryptedJarBuilder.createRandomClassName();
				super.addStubs(jos, cryptedLoaderClassName);
				servletStubs.put(targetClassName, stubClassName);
			}
			matcher.appendReplacement(sb, "<servlet-class>" + (String) servletStubs.get(targetClassName) + "</servlet-class>");
		}
		if (servletStubs.size() == 0)
			throw new IllegalStateException("No servlets found in war file");
		matcher.appendTail(sb);
		webXML = sb.toString();
		jos.putNextEntry(new JarEntry("WEB-INF/web.xml"));
		jos.write(webXML.getBytes("UTF-8"));
	}

	public String getPrefix() {
		return "WEB-INF/classes/";
	}

	public static class Template implements Servlet {

		public static Class target;

		static {
			if (!"TARGET_CLASS_NAME".endsWith("_NAME"))
				TemplateBasedJarLayout.cryptedMain(new String[] { "TARGET_CLASS_NAME", "STUB_CLASS_NAME", "target" });
		}

		private Servlet realServlet;

		public Template() {
			try {
				realServlet = (Servlet) target.newInstance();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public void init(ServletConfig c) throws ServletException {
			realServlet.init(c);
		}

		public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			realServlet.service(req, resp);
		}

		public void destroy() {
			realServlet.destroy();
		}

		public ServletConfig getServletConfig() {
			return realServlet.getServletConfig();
		}

		public String getServletInfo() {
			return realServlet.getServletInfo();
		}

		public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {
			realServlet.service(req, resp);
		}
	}
}
