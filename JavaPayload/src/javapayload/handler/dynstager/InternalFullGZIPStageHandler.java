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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import javapayload.Parameter;
import javapayload.handler.dynstager.GZIPChunkInputStream;
import javapayload.handler.dynstager.GZIPChunkOutputStream;
import javapayload.handler.stage.StageHandler;

public class InternalFullGZIPStageHandler extends StageHandler {

	private final StageHandler handler;

	public InternalFullGZIPStageHandler(StageHandler handler) {
		super(null, false, false, null);
		this.handler = handler;
	}

	public Parameter[] getParameters() {
		throw new UnsupportedOperationException();
	}

	public Class[] getNeededClasses() {
		return new Class[0];
	}

	protected void handleStreams(DataOutputStream out, InputStream in, String[] parameters) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(baos));
		new InternalFullGZIPStageHandler(null) {
			public Class[] getNeededClasses() {
				return new Class[] {
						GZIPChunkInputStream.class,
						GZIPChunkOutputStream.class
				};
			};
		}.handleBootstrap(parameters, dos);
		dos.close();
		out.writeInt(baos.size());
		baos.writeTo(out);
		handler.handle(new BufferedOutputStream(new GZIPChunkOutputStream(out)), (new GZIPChunkInputStream(in)), parameters);
	}

	protected StageHandler createClone() {
		return new InternalFullGZIPStageHandler(handler);
	}
}
