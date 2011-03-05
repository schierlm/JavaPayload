/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, Michael 'mihi' Schierl.
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
package javapayload.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class SpawnStagerBuilder {

	public static void main(String[] args) throws Exception {
		
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.SpawnStagerBuilder <stager> [<moreStagers...>]");
			return;
		}
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream("SpawnStagers.jar"), new Manifest());
		for (int i=0; i < args.length; i++) {
			String name = args[i];
			jos.putNextEntry(new ZipEntry("javapayload/handler/stager/Spawn"+name+".class"));
			jos.write(buildStagerHandler(name));
			jos.putNextEntry(new ZipEntry("javapayload/stager/Spawn"+name+".class"));
			jos.write(buildStager(name));
		}
		jos.close();
	}

	private static byte[] buildStagerHandler(final String stagerName) throws Exception {
		final ClassWriter cw = new ClassWriter(0);

		class MyMethodVisitor extends MethodAdapter {
			public MyMethodVisitor(MethodVisitor mv) {
				super(mv);
			}

			private String cleanType(String type) {
				if (type.equals("javapayload/handler/stager/LocalTest")) {
					type = "javapayload/handler/stager/"+stagerName;
				} else if (type.equals("javapayload/handler/stager/SpawnTemplate")) {
					type = "javapayload/handler/stager/Spawn"+stagerName;
				}
				return type;
			}

			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				super.visitFieldInsn(opcode, cleanType(owner), name, desc);
			}

			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				super.visitMethodInsn(opcode, cleanType(owner), name, desc);
			}

			public void visitTypeInsn(int opcode, String type) {
				super.visitTypeInsn(opcode, cleanType(type));
			}
		}
		final ClassVisitor cv = new ClassAdapter(cw) {

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, "javapayload/handler/stager/Spawn"+stagerName, signature, superName, interfaces);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
			}
		};
		visitClass(javapayload.handler.stager.SpawnTemplate.class, cv, cw);
		return cw.toByteArray();
	}

	protected static byte[] buildStager(final String stagerName) throws Exception {

		ClassBuilder.main(new String[] {stagerName, "SpawnedClass"});

		FileInputStream fis = new FileInputStream("SpawnedClass.class");
		StringBuffer sb = new StringBuffer();
		byte[] buf = new byte[4096];
		int len;
		while ((len = fis.read(buf)) != -1) {
			sb.append(new String(buf, 0, len, "ISO-8859-1"));
		}
		final String classContent = sb.toString();
		fis.close();
		if (!new File("SpawnedClass.class").delete()) 
			throw new IOException("Cannot delete file");

		final ClassWriter cw = new ClassWriter(0);
		final ClassVisitor templateVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, "javapayload/stager/Spawn"+stagerName, signature, superName, interfaces);
			}

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				return super.visitField(access, name, desc, signature, value);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

				return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {
					private String cleanType(String type) {
						if (type.equals("javapayload/builder/SpawnTemplate")) {
							type = "javapayload/stager/Spawn"+stagerName;
						}
						return type;
					}

					public void visitFieldInsn(int opcode, String owner, String name, String desc) {
						super.visitFieldInsn(opcode, cleanType(owner), name, desc);
					}

					public void visitMethodInsn(int opcode, String owner, String name, String desc) {
						super.visitMethodInsn(opcode, cleanType(owner), name, desc);
					}

					public void visitTypeInsn(int opcode, String type) {
						super.visitTypeInsn(opcode, cleanType(type));
					}
					
					public void visitLdcInsn(Object cst) {
						if ("TO_BE_REPLACED".equals(cst))
							cst = classContent;
						super.visitLdcInsn(cst);
					}
				};
			}
		};
		visitClass(SpawnTemplate.class, templateVisitor, cw);
		return cw.toByteArray();
	}

	private static void visitClass(Class clazz, ClassVisitor stagerVisitor, ClassWriter cw) throws Exception {
		final InputStream is = ClassBuilder.class.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class");
		final ClassReader cr = new ClassReader(is);
		cr.accept(stagerVisitor, ClassReader.SKIP_DEBUG);
		is.close();
	}
}
