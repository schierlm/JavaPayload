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

package javapayload.crypter;

import java.security.SecureRandom;

import javapayload.builder.ClassBuilder;
import javapayload.builder.dynstager.DynStagerBuilder;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public abstract class TemplateBasedCrypter extends Crypter {

	public TemplateBasedCrypter(String summary, String description) {
		super(summary, description);
	}

	public byte[] crypt(final String className, byte[] innerClassBytes) throws Exception {
		final long seed = new SecureRandom().nextLong();
		final String replaceDataString = new String(generateReplaceData(className, innerClassBytes, seed), "ISO-8859-1");
		final ClassWriter cw = new ClassWriter(0);
		final ClassVisitor templateVisitor = new ClassAdapter(cw) {

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, className, null, "java/lang/Object", null);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.equals("templateMain")) {
					return new MethodAdapter(super.visitMethod(access, "main", desc, signature, exceptions)) {
						public void visitLdcInsn(Object cst) {
							if ("TO_BE_REPLACED".equals(cst))
								DynStagerBuilder.visitStringConstant(mv, replaceDataString);
							else if (cst instanceof Long && ((Long) cst).longValue() == 4242)
								super.visitLdcInsn(new Long(seed));
							else
								super.visitLdcInsn(cst);
						};
					};
				}
				return null;
			}

			public FieldVisitor visitField(int arg0, String arg1, String arg2, String arg3, Object arg4) {
				return null;
			}
		};

		ClassBuilder.visitClass(getClass(), templateVisitor, cw);
		return cw.toByteArray();
	}

	protected abstract byte[] generateReplaceData(String className, byte[] innerClassBytes, long seed) throws Exception;

	// subclasses must implement a method like this:
	public static void templateMain(String[] args) throws Exception {
	}
}
