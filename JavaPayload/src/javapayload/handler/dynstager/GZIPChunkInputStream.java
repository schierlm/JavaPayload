/*
 * Java Payloads.
 * 
 * Copyright (c) 2012 Michael 'mihi' Schierl.
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

package javapayload.handler.dynstager;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GZIPChunkInputStream extends InputStream {

	InputStream in;
	GZIPInputStream gzis;

	public GZIPChunkInputStream(InputStream in) throws IOException {
		this.in = in;
		gzis = new GZIPInputStream(in, 1);
	}

	public int read() throws IOException {
		int b = gzis.read();
		while (b == -1) {
			if (!reopen())
				return -1;
			b = gzis.read();
		}
		return b;
	};

	public int read(byte[] b, int off, int len) throws IOException {
		int result = gzis.read(b, off, len);
		while (result == -1) {
			if (!reopen())
				return -1;
			result = gzis.read(b, off, len);
		}
		return result;
	}

	private boolean reopen() throws IOException {
		try {
			gzis = new GZIPInputStream(in, 1);
			return true;
		} catch (EOFException ex) {
			return false;
		}
	};
}
