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

package javapayload.builder.dynstager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.SequenceInputStream;

import javapayload.handler.stage.StageHandler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class Integrated extends WrappingDynStagerBuilder {

	public void bootstrapWrap(String[] parameters) throws Exception {
		bootstrapOrig(parameters);
	}

	protected void bootstrapWrap(InputStream rawIn, OutputStream out, String[] parameters) {
		try {
			rawIn = new SequenceInputStream(new ByteArrayInputStream(bootstrap().getBytes("ISO-8859-1")), rawIn);
			bootstrapOrig(rawIn, out, parameters);
		} catch (final IOException ex) {
			ex.printStackTrace(new PrintStream(out, true));
		}
	}

	protected void handleCustomMethods(String bootstrapName, ClassWriter cw, String stagerName, Class baseStagerClass, String extraArg, String[] args) throws Exception {
		if (extraArg == null || args == null)
			throw new IllegalArgumentException("Need arguments for building integrated stager");
		String stage = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--"))
				stage = args[i+1];
		}
		if (stage == null)
			throw new IllegalArgumentException("Stage argument missing");		
		StageHandler stageHandler = (StageHandler) Class.forName("javapayload.handler.stage." + stage).newInstance();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		stageHandler.handleBootstrap(args, dos);
		String prependDataString = new String(baos.toByteArray(), "ISO-8859-1");
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, bootstrapName, "()Ljava/lang/String;", null, null);
		mv.visitCode();
		visitStringConstant(mv, prependDataString);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(3, 1);
		mv.visitEnd();
	}

	private String bootstrap() {
		return "TO_BE_REPLACED";
	}
}
