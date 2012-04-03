/*
 * J2EE Payloads.
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

package j2eepayload.dynstager;

import java.util.List;

import javapayload.loader.DynStagerURLStreamHandler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DynstagerSupportBuilder {
	public static Class buildSupport(String prefix, List args) throws Exception {
		DynStagerURLStreamHandler ush = DynStagerURLStreamHandler.getInstance();
		String freeClassName = null;
		for (int i = 1; i < 10; i++) {
			String className = DynstagerSupport.class.getName() + i;
			try {
				ush.getDynClassLoader().loadClass(className);
			} catch (ClassNotFoundException ex2) {
				freeClassName = className;
				break;
			}
		}
		if (freeClassName == null)
			throw new IllegalStateException("No builder slots available, please restart the builder.");
		byte[] bytecode = dump(freeClassName, "javapayload/stager/" + prefix + "LocalTest", args);
		ush.addClass(freeClassName, bytecode);
		return ush.getDynClassLoader().loadClass(freeClassName);
	}

	public static byte[] dump(String supportClassName, String stagerClassName, List args) throws Exception {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_1, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, supportClassName.replace('.', '/'), null, "j2eepayload/dynstager/DynstagerSupport", null);

		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "j2eepayload/dynstager/DynstagerSupport", "<init>", "()V");
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "getExtraArgs", "()[Ljava/lang/String;", null, null);
		mv.visitCode();
		mv.visitIntInsn(Opcodes.BIPUSH, args.size());
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");
		for (int i = 0; i < args.size(); i++) {
			mv.visitInsn(Opcodes.DUP);
			mv.visitIntInsn(Opcodes.BIPUSH, i);
			mv.visitLdcInsn(args.get(i));
			mv.visitInsn(Opcodes.AASTORE);
		}
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(4, 1);
		mv.visitEnd();

		mv = cw.visitMethod(Opcodes.ACC_PROTECTED, "createStager", "(Ljava/io/InputStream;Ljava/io/OutputStream;)Ljavapayload/stager/Stager;", null, new String[] { "java/lang/Exception" });
		mv.visitCode();
		mv.visitTypeInsn(Opcodes.NEW, stagerClassName);
		mv.visitInsn(Opcodes.DUP);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, stagerClassName, "<init>", "(Ljava/io/InputStream;Ljava/io/OutputStream;)V");
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(4, 3);
		mv.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}
}
