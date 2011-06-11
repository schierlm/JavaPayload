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

package javapayload.stage;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.rmi.Remote;
import java.rmi.activation.ActivationDesc;
import java.rmi.activation.ActivationGroupID;
import java.rmi.activation.ActivationID;
import java.rmi.dgc.DGC;
import java.rmi.dgc.VMID;
import java.rmi.registry.Registry;
import java.rmi.server.ObjID;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sun.rmi.transport.ObjectTable;
import sun.rmi.transport.Target;
import sun.rmi.transport.Transport;
import sun.rmi.transport.tcp.TCPTransport;

public class RMIInfo implements Stage {

	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		PrintStream pout = new PrintStream(out, true);
		List transports = new ArrayList();
		List activations = new ArrayList();
		List targets = new ArrayList(((Map) getField(ObjectTable.class, null, "objTable")).values());

		pout.println("Exported objects:");
		for (int i = 0; i < targets.size(); i++) {
			Target target = (Target) targets.get(i);
			ObjID id = (ObjID) getField(Target.class, target, "id");
			Object impl = ((WeakReference) getField(Target.class, target, "weakImpl")).get();
			Transport exportedTransport = (Transport) getField(Target.class, target, "exportedTransport");
			pout.println("\t" + id.toString());
			pout.println("\t\tTransport: " + exportedTransport);
			if (exportedTransport != null && !transports.contains(exportedTransport))
				transports.add(exportedTransport);
			pout.println("\t\tObject class: " + (impl == null ? "null" : impl.getClass().getName()));
			pout.println("\t\tObject: " + impl);
			if (impl instanceof RemoteObject) {
				pout.println("\t\tRemote reference: " + ((RemoteObject) impl).getRef().remoteToString());
			}
			if (impl instanceof Registry) {
				Registry r = (Registry) impl;
				String[] names = r.list();
				pout.println("\t\tObjects in this registry: " + names.length);
				for (int j = 0; j < names.length; j++) {
					pout.println("\t\t\t" + names[j]);
					Remote obj = r.lookup(names[j]);
					pout.println("\t\t\t\tObject class: " + (obj == null ? "null" : obj.getClass().getName()));
					pout.println("\t\t\t\tObject: " + obj.toString());
					if (obj instanceof RemoteObject) {
						pout.println("\t\t\t\tRemote reference: " + ((RemoteObject) obj).getRef().remoteToString());
					}
				}
			} else if (impl instanceof DGC) {
				List leaseTable = new ArrayList(((Map) getField(null, impl, "leaseTable")).values());
				pout.println("\t\tDGC Leases:");
				for (int j = 0; j < leaseTable.size(); j++) {
					Object leaseInfo = leaseTable.get(j);
					VMID vmid = (VMID) getField(null, leaseInfo, "vmid");
					pout.println("\t\t\t" + vmid);
					Date expiration = new Date(((Long) getField(null, leaseInfo, "expiration")).longValue());
					pout.println("\t\t\t\tExpires: " + expiration);
					List notifySet = new ArrayList((Set) getField(null, leaseInfo, "notifySet"));
					for (int k = 0; k < notifySet.size(); k++) {
						Target t = (Target) notifySet.get(k);
						notifySet.set(k, getField(Target.class, t, "id"));
					}
					pout.println("\t\t\t\tNotify set: " + notifySet);
				}
			} else if (impl.getClass().getName().startsWith("sun.rmi.server.Activation$")) {
				try {
					Field f = impl.getClass().getDeclaredField("this$0");
					f.setAccessible(true);
					Object activation = f.get(impl);
					if (activation != null) {
						pout.println("\t\tActivation object: "+activation);
						if (!activations.contains(activation))
							activations.add(activation);
					}
				} catch (NoSuchFieldException ex) {
					// ignore
				}
			}
		}
		pout.println();
		
		for (int i = 0; i < activations.size(); i++) {
			Object activation = activations.get(i);
			pout.println("Activation groups of "+activation+":");
			List groupValues = new ArrayList(((Map) getField(null, activation, "groupTable")).values());
			for (int j = 0; j < groupValues.size(); j++) {
				Object groupEntry = groupValues.get(j);
				ActivationGroupID groupid = (ActivationGroupID) getField(null, groupEntry, "groupID");
				pout.println("\t"+getField(ActivationGroupID.class, groupid, "uid"));
				ArrayList objects = new ArrayList(((Map) getField(null, groupEntry, "objects")).entrySet());
				for (int k = 0; k < objects.size(); k++) {
					Map.Entry object = (Map.Entry) objects.get(k);
					ActivationID activationId = (ActivationID)object.getKey();
					ActivationDesc desc = (ActivationDesc) getField(null, object.getValue(), "desc");
					pout.println("\t\t" + getField(null, activationId, "uid"));
					pout.println("\t\t\tClass: "+desc.getClassName());
					pout.println("\t\t\tLocation: "+desc.getLocation());
				}
			}
			pout.println();
		}
		
		pout.println("Transports:");
		for (int i = 0; i < transports.size(); i++) {
			Transport transport = (Transport) transports.get(i);
			pout.println("\t" + transport);
			pout.println("\t\tObject class: " + transport.getClass().getName());
			if (transport instanceof TCPTransport) {
				TCPTransport tcpTransport = (TCPTransport) transport;
				ServerSocket server = (ServerSocket) getField(TCPTransport.class, tcpTransport, "server");
				pout.println("\t\tSocket: " + server);
				List epList = (List) getField(TCPTransport.class, tcpTransport, "epList");
				pout.println("\t\tEndpoints: " + epList);
				List channelTable = new ArrayList(((Map) getField(TCPTransport.class, tcpTransport, "channelTable")).keySet());
				pout.println("\t\tChannels open to: " + channelTable);
			}
		}
		pout.println();
		pout.close();
	}
	
	private static Object getField(Class clazz, Object target, String field) throws Exception {
		if (clazz == null) clazz = target.getClass();
		Field f = clazz.getDeclaredField(field);
		f.setAccessible(true);
		return f.get(target);
	}
}
