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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javapayload.builder.ClassBuilder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public abstract class WrappingDynStagerBuilder extends DynStagerBuilder {

	public byte[] buildStager(final String stagerName, final Class baseStagerClass, String extraArg, String[] args) throws Exception {
		final ClassWriter cw = new ClassWriter(0);
		final List bootstrapMethods = new ArrayList();
		final ClassVisitor stagerCheckVisitor = new ClassVisitor() {
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.startsWith("bootstrap"))
					bootstrapMethods.add(name);
				return null;
			}

			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return null;
			}

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				return null;
			}

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {}
			public void visitAttribute(Attribute attr) {}
			public void visitEnd() {}
			public void visitInnerClass(String name, String outerName, String innerName, int access) {}
			public void visitOuterClass(String owner, String name, String desc) {}
			public void visitSource(String source, String debug) {}
		};
		ClassBuilder.visitClass(baseStagerClass, stagerCheckVisitor, cw);
		int counter = 0;
		while (bootstrapMethods.contains("bootstrap" + counter))
			counter++;
		final String bootstrapName = "bootstrap" + counter;
		
		class MyMethodVisitor extends MethodAdapter {

			public MyMethodVisitor(MethodVisitor mv) {
				super(mv);
			}

			private String cleanType(String type) {
				if (type.equals(baseStagerClass.getName().replace('.','/')) || type.equals(WrappingDynStagerBuilder.this.getClass().getName().replace('.','/'))) {
					type = "javapayload/stager/" + stagerName;
				}
				return type;
			}

			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				super.visitFieldInsn(opcode, cleanType(owner), name, desc);
			}

			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				if (name.equals("bootstrap")) {
					name = bootstrapName;
					owner = "javapayload/stager/" + stagerName;
				}
				if (name.equals("bootstrapOrig") && owner.equals("javapayload/builder/dynstager/WrappingDynStagerBuilder")) {
					if (desc.equals("([Ljava/lang/String;)V")) {
						name = bootstrapName;
					} else if (desc.equals("(Ljava/io/InputStream;Ljava/io/OutputStream;[Ljava/lang/String;)V")) {
						name = "bootstrap";
					} else {
						throw new RuntimeException("Unknown bootstrapOrig method: "+desc);
					}
					owner = "javapayload/stager/" + stagerName;
				}
				super.visitMethodInsn(opcode, cleanType(owner), name, desc);
			}

			public void visitTypeInsn(int opcode, String type) {
				super.visitTypeInsn(opcode, cleanType(type));
			}
		}
		final ClassVisitor stagerVisitor = new ClassAdapter(cw) {

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, "javapayload/stager/" + stagerName, signature, superName, interfaces);
			}

			public void visitEnd() {
				// not the end!
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.equals("bootstrap"))
					name = bootstrapName;
				return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
			}
		};
		ClassBuilder.visitClass(baseStagerClass, stagerVisitor, cw);
		final ClassVisitor templateVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				// not the beginning!
			}
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// take only the method we need
				if (name.equals("bootstrapWrap")) {
					if (desc.equals("([Ljava/lang/String;)V")) {
						name = "bootstrap";
					} else if (desc.equals("(Ljava/io/InputStream;Ljava/io/OutputStream;[Ljava/lang/String;)V")) {
						name = bootstrapName;
					} else {
						throw new RuntimeException("Unknown bootstrapWrap method: "+desc);
					}
					return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
				} else {
					return null;
				}
			}
		};
		ClassBuilder.visitClass(getClass(), templateVisitor, cw);
		return cw.toByteArray();
	}

	public abstract void bootstrapWrap(String[] parameters) throws Exception;
	protected abstract void bootstrapWrap(InputStream rawIn, OutputStream out, String[] parameters);
	protected final void bootstrapOrig(String[] parameters) throws Exception {}
	protected final void bootstrapOrig(InputStream rawIn, OutputStream out, String[] parameters) {}
}