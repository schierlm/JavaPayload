/*
 * Java Payloads.
 * 
 * Copyright (c) 2012 Michael 'mihi' Schierl
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class GrowSize extends WrappingDynStagerBuilder {

	public void bootstrapWrap(String[] parameters, boolean needWait) throws Exception {
		bootstrap();
		bootstrapOrig(parameters, needWait);
	}

	protected void bootstrapWrap(InputStream rawIn, OutputStream out, String[] parameters) {
		bootstrap();
		bootstrapOrig(rawIn, out, parameters);
	}

	protected void handleCustomMethods(String bootstrapName, ClassWriter cw, String stagerName, Class baseStagerClass, String extraArg, String[] args) throws Exception {
		int count = Integer.parseInt(extraArg);
		Random rnd = new Random();
		char[] buffer = new char[50000];
		// create the bootstrap method
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, bootstrapName, "()Ljava/lang/String;", null, null);
		mv.visitCode();
		mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuffer");
		mv.visitInsn(Opcodes.DUP);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V");
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < buffer.length; j++) {
				buffer[j] = (char) (rnd.nextInt(95) + ' ');
			}
			mv.visitLdcInsn(new String(buffer));
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
		}
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}

	private String bootstrap() {
		throw new IllegalStateException("This method is replaced in the final stager");
		// StringBuffer sb = new StringBuffer();
		// sb.append("PART");
		// return sb.toString();
	}
}
