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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MultiStageOutputStream extends OutputStream {

	private final OutputStream out;
	private boolean active = false;
	private boolean closed = false;

	public MultiStageOutputStream(OutputStream out) {
		this.out = out;
	}

	public void write(byte[] b, int off, int len) throws IOException {
		synchronized (this) {
			if (closed)
				throw new IOException("Stream closed");
		}
		int start = off;
		for (int i = off; i < off + len; i++) {
			if (b[i] == 1) {
				writeInternal(b, start, i - start + 1, true);
				start = i + 1;
			}
		}
		writeInternal(b, start, len - start + off, false);
	}

	public void write(int b) throws IOException {
		write(new byte[] { (byte) b });
	}

	public synchronized void close() throws IOException {
		if (closed)
			return;
		writeInternal(new byte[] { 1, 3 }, 0, 2, false);
		closed = true;
	}

	private synchronized void writeInternal(byte[] bs, int off, int len, boolean addOne) throws IOException {
		while (!active) {
			try {
				wait();
			} catch (InterruptedException ex) {
			}
		}
		out.write(bs, off, len);
		if (addOne)
			out.write(1);
	}

	public synchronized void flush() throws IOException {
		while (!active) {
			try {
				wait();
			} catch (InterruptedException ex) {
			}
		}
		out.flush();
	}

	public synchronized void start() throws IOException {
		if (active)
			throw new IllegalStateException();
		out.write(new byte[] { 1, 0 });
		out.flush();
		active = true;
		notifyAll();
	}

	public synchronized void stop() throws IOException {
		if (!active)
			throw new IllegalStateException();
		out.write(new byte[] { 1, 2 });
		out.flush();
		active = false;
	}

	public static void decodeForward(InputStream in, OutputStream out) throws IOException {
		if (in.read() != 1 || in.read() != 0)
			throw new IOException("Invalid stream start marker");
		int b;
		boolean escape = false;
		while ((b = in.read()) != -1) {
			if (escape) {
				switch (b) {
				case 1:
					out.write(b);
					break;
				case 2:
					return;
				case 3:
					out.close();
					break;
				default:
					throw new IOException("Invalid escape character: " + (b & 0xff));
				}
				escape = false;
			} else if (b == 1) {
				escape = true;
			} else {
				out.write(b);
			}
			try {
				if (in.available() == 0)
					out.flush();
			} catch (IOException ex) {
				// ignore
			}
		}
		throw new IOException("Premature end of stream");
	}
}