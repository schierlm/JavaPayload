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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javapayload.builder.ClassBuilder;
import javapayload.builder.ClassBuilder.ClassBuilderTemplate;
import javapayload.builder.SpawnTemplate;
import javapayload.stage.StreamForwarder;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class Spawn extends DynStagerBuilder {
	public byte[] buildStager(String stagerResultName, Class baseStagerClass, String extraArg, String[] args) throws Exception {
		if (extraArg != null)
			throw new IllegalArgumentException("Spawn stagers do not support an extra argument");
		String stagerFullName = baseStagerClass.getName();
		final String stagerName = stagerFullName.substring(stagerFullName.lastIndexOf('.') + 1);
		byte[] classBytes = ClassBuilder.buildClassBytes("SpawnedClass", stagerName, ClassBuilderTemplate.class, null, null);
		// see SpawnStagerPrebuilder for details
		InputStream prebuiltSpawnedClass = Spawn.class.getResourceAsStream("/"+stagerName+".spawned");
		if(prebuiltSpawnedClass != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamForwarder.forward(prebuiltSpawnedClass, baos);
			classBytes = baos.toByteArray();
		}
		final String classContent = new String(classBytes, "ISO-8859-1");
		final ClassWriter cw = new ClassWriter(0);
		final ClassVisitor templateVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, "javapayload/stager/Spawn_" + stagerName, signature, superName, interfaces);
			}

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				return super.visitField(access, name, desc, signature, value);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

				return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {
					private String cleanType(String type) {
						if (type.equals("javapayload/builder/SpawnTemplate")) {
							type = "javapayload/stager/Spawn_" + stagerName;
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
							visitStringConstant(mv, classContent);
						else
							super.visitLdcInsn(cst);
					}
				};
			}
		};
		ClassBuilder.visitClass(SpawnTemplate.class, templateVisitor, cw);
		return cw.toByteArray();
	}
}
