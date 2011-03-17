/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, 2011 Michael 'mihi' Schierl.
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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class AESStagerBuilder {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.AESStagerBuilder [--strip] <stager> [<moreStagers...>]");
			return;
		}
		final Class[] classes = new Class[] {
				javapayload.handler.stager.AESStageHandler.class,
				javapayload.handler.stage.AES.class,
				javapayload.stage.AES.class,
				javapayload.stage.AESHelper.class
		};
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream("AESStagers.jar"), new Manifest());
		boolean stripDebugInfo = false;
		for (int i = 0; i < args.length; i++) {
			String name = args[i];
			if (name.equals("--strip"))
				stripDebugInfo = true;
		}
		JarBuilder.addClasses(jos, "", classes, stripDebugInfo);
		for (int i = 0; i < args.length; i++) {
			String name = args[i];
			if (name.equals("--strip"))
				continue;
			jos.putNextEntry(new ZipEntry("javapayload/handler/stager/AES" + name + ".class"));
			jos.write(buildStagerHandler(name));
			jos.putNextEntry(new ZipEntry("javapayload/stager/AES" + name + ".class"));
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
					type = "javapayload/handler/stager/" + stagerName;
				} else if (type.equals("javapayload/handler/stager/AESStagerTemplate")) {
					type = "javapayload/handler/stager/AES" + stagerName;
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
				super.visit(version, access, "javapayload/handler/stager/AES" + stagerName, signature, superName, interfaces);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
			}
		};
		visitClass(Class.forName("javapayload.handler.stager.AESStagerTemplate"), cv, cw);
		return cw.toByteArray();
	}

	protected static byte[] buildStager(final String stagerName) throws Exception {
		final ClassWriter cw = new ClassWriter(0);

		final List bootstrapMethods = new ArrayList();
		final List bootstrapOrigMethods = new ArrayList();
		final ClassVisitor stagerVisitor0 = new ClassVisitor() {
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.startsWith("bootstrapAES"))
					bootstrapMethods.add(name);
				if (name.startsWith("bootstrapOrig"))
					bootstrapOrigMethods.add(name);
				return null;
			}

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			}

			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				return null;
			}

			public void visitAttribute(Attribute attr) {
			}

			public void visitEnd() {
			}

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				return null;
			}

			public void visitInnerClass(String name, String outerName, String innerName, int access) {
			}

			public void visitOuterClass(String owner, String name, String desc) {
			}

			public void visitSource(String source, String debug) {
			}
		};
		visitClass(Class.forName("javapayload.stager." + stagerName), stagerVisitor0, cw);
		String bootstrapNameTemp = "bootstrapAES";
		if (bootstrapMethods.contains(bootstrapNameTemp)) {
			int counter = 2;
			while (bootstrapMethods.contains("bootstrapAES" + counter))
				counter++;
			bootstrapNameTemp = "bootstrapAES" + counter;
		}
		final String bootstrapName = bootstrapNameTemp;
		String bootstrapOrigNameTemp = "bootstrapOrig";
		if (bootstrapOrigMethods.contains(bootstrapOrigNameTemp)) {
			int counter = 2;
			while (bootstrapOrigMethods.contains("bootstrapOrig" + counter))
				counter++;
			bootstrapOrigNameTemp = "bootstrapOrig" + counter;
		}
		final String bootstrapOrigName = bootstrapOrigNameTemp;

		class MyMethodVisitor extends MethodAdapter {

			private final boolean changeBootstrapCall;
			private final boolean changeOrigCall;

			public MyMethodVisitor(MethodVisitor mv, boolean changeBootstrapCall, boolean changeOrigCall) {
				super(mv);
				this.changeBootstrapCall = changeBootstrapCall;
				this.changeOrigCall = changeOrigCall;
			}

			private String cleanType(String type) {
				if (type.equals("javapayload/stager/" + stagerName) || type.equals("javapayload/builder/AESStagerTemplate")) {
					type = "javapayload/stager/AES" + stagerName;
				}
				return type;
			}

			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				super.visitFieldInsn(opcode, cleanType(owner), name, desc);
			}

			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				if (changeBootstrapCall && owner.equals("javapayload/stager/Stager") && name.equals("bootstrap") && desc.equals("(Ljava/io/InputStream;Ljava/io/OutputStream;[Ljava/lang/String;)V")) {
					name = bootstrapName;
					owner = "javapayload/stager/" + stagerName;
				}
				if (changeOrigCall && owner.equals("javapayload/builder/AESStagerTemplate") && name.equals("bootstrapOrig") && desc.equals("([Ljava/lang/String;)V")) {
					name = bootstrapOrigName;
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
				super.visit(version, access, "javapayload/stager/AES" + stagerName, signature, superName, interfaces);
			}

			public void visitEnd() {
				// not the end!
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.equals("bootstrap") && desc.equals("([Ljava/lang/String;)V"))
					name = bootstrapOrigName;
				return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), true, false);
			}
		};
		visitClass(Class.forName("javapayload.stager." + stagerName), stagerVisitor, cw);
		final ClassVisitor templateVisitor = new ClassAdapter(cw) {

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				// not the beginning!
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				// take only the method we need
				if (name.equals("bootstrapAES")) {
					return new MyMethodVisitor(super.visitMethod(access, bootstrapName, desc, signature, exceptions), false, false);
				} else if (name.equals("bootstrap")) {
					return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), false, true);
				} else {
					return null;
				}
			}
		};
		visitClass(AESStagerTemplate.class, templateVisitor, cw);
		return cw.toByteArray();
	}

	private static void visitClass(Class clazz, ClassVisitor stagerVisitor, ClassWriter cw) throws Exception {
		final InputStream is = ClassBuilder.class.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class");
		final ClassReader cr = new ClassReader(is);
		cr.accept(stagerVisitor, ClassReader.SKIP_DEBUG);
		is.close();
	}
}
