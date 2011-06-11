/*
 * Java Payloads.
 * 
 * Copyright (c) 2011 Michael 'mihi' Schierl
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

import java.rmi.Remote;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;

import javapayload.Parameter;

public class RMIDiscovery extends Discovery {

	public RMIDiscovery() {
		super("List remote objects in a RMI registry", "");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("RHOST", false, Parameter.TYPE_HOST, "Host to connect to"),
				new Parameter("RPORT", false, Parameter.TYPE_PORT, "Port to connect to"),
		};
	}

	public void discover(String[] parameters) throws Exception {
		Registry registry = LocateRegistry.getRegistry(parameters[0], Integer.parseInt(parameters[1]));
		String[] names = registry.list();
		for (int i = 0; i < names.length; i++) {
			consoleOut.println(names[i]);
			Remote obj;
			try {
				obj = registry.lookup(names[i]);
				if (obj == null)
					consoleOut.println("\tObject: null");
			} catch (UnmarshalException ex) {
				consoleOut.println("\tUnmarshal exception: " + ex.getCause().toString());
				obj = null;
			}
			if (obj != null) {
				consoleOut.println("\tObject class: " + obj.getClass().getName());
				consoleOut.println("\tObject: " + obj.toString());
				if (obj instanceof RemoteObject) {
					consoleOut.println("\tRemote reference: " + ((RemoteObject) obj).getRef().remoteToString());
				}
			}
		}
	}
}
