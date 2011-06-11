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

package javapayload.cli;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * A {@link FilterInputStream} that stops writing on close, but does not close
 * the underlying output stream.
 */
public class LocalCloseInputStream extends FilterInputStream {

	volatile boolean closed = false;
	boolean reading = false;

	public LocalCloseInputStream(InputStream in) {
		super(in);
	}

	public void close() throws IOException {
		closed = true;
	}

	public synchronized void closeAsync(PrintStream consoleOut) throws IOException {
		close();
		if (reading) {
			consoleOut.println("[Command still reading from console. Hit Enter to return to the prompt.]");
			try {
			while(reading) 
				wait();
			} catch (InterruptedException ex) {
				ex.printStackTrace(consoleOut);
			}
		}
	}

	private synchronized void startReading() throws IOException {
		if (closed)
			throw new IOException("Stream closed");
		reading = true;
	}

	private synchronized void stopReading() {
		reading = false;
		notifyAll();
	}

	public int available() throws IOException {
		if (closed) 
			return 0;
		startReading();
		try {
			return super.available();
		} finally {
			stopReading();
		}
	}

	public int read() throws IOException {
		if (closed)
			return -1;
		startReading();
		try {
			return super.read();
		} finally {
			stopReading();
		}
	}

	public int read(byte[] b) throws IOException {
		if (closed)
			return -1;
		startReading();
		try {
			return super.read(b);
		} finally {
			stopReading();
		}
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if (closed)
			return -1;
		startReading();
		try {
			return super.read(b, off, len);
		} finally {
			stopReading();
		}
	}

	public long skip(long n) throws IOException {
		if (closed)
			return 0;
		startReading();
		try {
			return super.skip(n);
		} finally {
			stopReading();
		}
	}
}
