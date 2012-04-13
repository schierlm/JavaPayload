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

package javapayload.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javapayload.Module;
import javapayload.loader.AppletLoader;
import javapayload.stage.StageMenu;
import javapayload.stage.StreamForwarder;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EmbeddedAppletJarBuilder extends Builder {

	public EmbeddedAppletJarBuilder() {
		super("Build a Java applet that includes all applet parameters.", "");
	}

	public String getParameterSyntax() {
		return "[--builder AppletJar] [--readyURL http://example.com/] [--name some.java.ClassName] [--strip] [<filename>.jar] <stager> [stageroptions] -- <stage> [stageoptions]";
	}

	public void build(String[] args) throws Exception {
		String builderName = "AppletJar", readyURL = null, name = null, filename = "EmbeddedAppletJar.jar";
		boolean strip = false;
		int firstArg = 0;
		while (true) {
			if (args[firstArg].equals("--builder")) {
				builderName = args[firstArg + 1];
				firstArg += 2;
			} else if (args[firstArg].equals("--readyURL")) {
				readyURL = args[firstArg + 1];
				firstArg += 2;
			} else if (args[firstArg].equals("--name")) {
				name = args[firstArg + 1];
				firstArg += 2;
			} else if (args[firstArg].equals("--strip")) {
				strip = true;
				firstArg++;
			} else if (args[firstArg].endsWith(".jar")) {
				filename = args[firstArg];
				firstArg++;
			} else {
				break;
			}
		}
		String realArgs = (name != null ? "--name " + name + " " : "") + (strip ? "--strip " : "") + filename + " " + args[firstArg];
		Builder builder = (Builder) Module.load(Builder.class, builderName + "Builder");
		builder.build(StageMenu.splitArgs(realArgs));
		final List/* <String> */argValuePairs = new ArrayList();
		if (readyURL != null) {
			argValuePairs.add("readyURL");
			argValuePairs.add(readyURL);
		}
		argValuePairs.add("argc");
		argValuePairs.add("" + (args.length - firstArg));
		for (int i = firstArg; i < args.length; i++) {
			argValuePairs.add("arg" + (i - firstArg));
			argValuePairs.add(args[i]);
		}
		FileInputStream fis = new FileInputStream(filename);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamForwarder.forward(fis, baos);
		JarInputStream jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()));
		Manifest manifest = jis.getManifest();
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(filename), manifest);
		final byte[] buf = new byte[4096];
		int length;
		while (true) {
			JarEntry je = jis.getNextJarEntry();
			if (je == null)
				break;
			jos.putNextEntry(je);
			ByteArrayOutputStream buffer = null;
			OutputStream out = jos;
			if (je.getName().equals(AppletLoader.class.getName().replace('.', '/') + ".class")) {
				buffer = new ByteArrayOutputStream();
				out = buffer;
			}
			while ((length = jis.read(buf)) != -1) {
				out.write(buf, 0, length);
			}
			if (buffer != null) {
				buffer.close();
				ClassReader cr = new ClassReader(buffer.toByteArray());
				ClassWriter cw = new ClassWriter(0);
				ClassVisitor adapter = new ClassAdapter(cw) {
					public void visitEnd() {
						MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "getParameter", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
						mv.visitCode();
						for (int i = 0; i < argValuePairs.size(); i += 2) {
							String arg = (String) argValuePairs.get(i);
							String value = (String) argValuePairs.get(i + 1);
							mv.visitVarInsn(Opcodes.ALOAD, 1);
							mv.visitLdcInsn(arg);
							mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
							Label jmp = new Label();
							mv.visitJumpInsn(Opcodes.IFEQ, jmp);
							mv.visitLdcInsn(value);
							mv.visitInsn(Opcodes.ARETURN);
							mv.visitLabel(jmp);
						}
						mv.visitVarInsn(Opcodes.ALOAD, 0);
						mv.visitVarInsn(Opcodes.ALOAD, 1);
						mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/applet/Applet", "getParameter", "(Ljava/lang/String;)Ljava/lang/String;");
						mv.visitInsn(Opcodes.ARETURN);
						mv.visitMaxs(2, 2);
						mv.visitEnd();
						super.visitEnd();
					}
				};
				cr.accept(adapter, ClassReader.SKIP_DEBUG);
				jos.write(cw.toByteArray());
			}
		}
		jos.close();
	}
}
