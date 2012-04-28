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

import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javapayload.builder.ClassBuilder;
import javapayload.builder.CryptedJarBuilder;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public abstract class TemplateBasedJarLayout extends JarLayout {

	protected String stubClassName;
	protected final Class templateClass;
	protected String targetClassName = null;

	public TemplateBasedJarLayout(String summary, Class templateClass, String description) {
		super(summary, description);
		this.templateClass = templateClass;
		stubClassName = CryptedJarBuilder.createRandomClassName();
	}

	public void addStubs(JarOutputStream jos, final String cryptedLoaderClassName) throws Exception {
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor templateAdapter = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, stubClassName, signature, superName, interfaces);
			}

			public void visitInnerClass(String name, String outerName, String innerName, int access) {
			}

			public void visitOuterClass(String owner, String name, String desc) {
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {

					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						if (name.equals("cryptedMain") && owner.equals(TemplateBasedJarLayout.class.getName().replace('.', '/'))) {
							super.visitMethodInsn(opcode, cryptedLoaderClassName, "main", desc);
						} else {
							super.visitMethodInsn(opcode, retype(owner), name, desc);
						}
					}

					public void visitLdcInsn(Object cst) {
						if (cst.equals("TARGET_CLASS_NAME"))
							cst = targetClassName;
						if (cst.equals("STUB_CLASS_NAME"))
							cst = stubClassName;
						super.visitLdcInsn(cst);
					}

					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						super.visitFieldInsn(opcode, retype(owner), name, desc);
					}

					public void visitTypeInsn(int opcode, String type) {
						super.visitTypeInsn(opcode, retype(type));
					}

					private String retype(String owner) {
						if (owner.equals(templateClass.getName().replace('.', '/')))
							owner = stubClassName;
						return owner;
					}
				};
			}
		};
		ClassBuilder.visitClass(templateClass, templateAdapter, cw);
		jos.putNextEntry(new ZipEntry(getPrefix() + stubClassName + ".class"));
		jos.write(cw.toByteArray());
	}

	// call this method from your templates
	public static void cryptedMain(String[] args) {
		throw new IllegalStateException("This method is never called!");
	}
}
