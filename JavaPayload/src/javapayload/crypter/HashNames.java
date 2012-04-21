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

package javapayload.crypter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javapayload.builder.ClassBuilder;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;

public class HashNames extends JarCrypter{

	private final SecureRandom rnd = new SecureRandom();
	private final byte[] key;
	private final LoaderClassTemplate tpl;

	public HashNames() {
		super("Create a Jar with hashes as file names",
				"All files added to the Jar are encrypted with a random AES key embedded in\r\n" +
				"the loader and get a file name derived from the file name hash.\r\n" +
				"\r\n" +
				"The loader uses a standard ClassLoader approach; therefore this JarCrypter\r\n" +
				"cannot handle JARs that run in a sandbox (like exploit applet jars).");
		key = new byte[16];
		rnd.nextBytes(key);
		tpl = new LoaderClassTemplate(key);
	}

	public void addFile(JarOutputStream jos, String filename, byte[] content) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] iv = new byte[16];
		rnd.nextBytes(iv);
		baos.write(iv);
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
		CipherOutputStream cos = new CipherOutputStream(baos, cipher);
		cos.write(content);
		cos.close();
		jos.putNextEntry(new ZipEntry(tpl.hashName("/"+filename)));
		baos.writeTo(jos);
	}

	public byte[] createLoaderClass(JarOutputStream jos, final String className) throws Exception {
		final String keyString = new String(key, "ISO-8859-1");
		class MyMethodVisitor extends MethodAdapter {
			public MyMethodVisitor(MethodVisitor mv) {
				super(mv);
			}
			
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				super.visitFieldInsn(opcode, retype(owner), name, desc);
			}
			
			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				super.visitMethodInsn(opcode, retype(owner), name, desc);
			}
			
			public void visitTypeInsn(int opcode, String type) {
				super.visitTypeInsn(opcode, retype(type));
			}
			
			private String retype(String owner) {
				if (owner.equals("javapayload/crypter/HashNames$HelperClassTemplate"))
					owner = "HelperClass";
				if (owner.equals("javapayload/crypter/HashNames$LoaderClassTemplate"))
					owner = className;
				return owner;
			}
			
			public void visitLdcInsn(Object cst) {
				if (cst.equals("TO_BE_REPLACED"))
					cst = keyString;
				super.visitLdcInsn(cst);
			}
		}

		ClassWriter cw = new ClassWriter(0);
		ClassVisitor helperVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, "HelperClass", signature, superName, interfaces);
			}
			
			public void visitOuterClass(String owner, String name, String desc) {
			}
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
			}
			
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
			}
		};
		ClassBuilder.visitClass(HelperClassTemplate.class, helperVisitor, cw);
		addFile(jos, "HelperClass", cw.toByteArray());
		
		cw = new ClassWriter(0);
		ClassVisitor loaderVisitor = new ClassAdapter(cw) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, className, signature, superName, interfaces);
			}
			
			public void visitOuterClass(String owner, String name, String desc) {
			}
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
			}
			
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MyMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
			}
		};
		ClassBuilder.visitClass(LoaderClassTemplate.class, loaderVisitor, cw);
		return cw.toByteArray();
	}
	
	public static class LoaderClassTemplate extends URLStreamHandler {
		
		private final byte[] key;

		private LoaderClassTemplate(byte[] key) {
			this.key = key;
		}
		
		public static void main(String[] args) throws Throwable {
			LoaderClassTemplate ct = new LoaderClassTemplate("TO_BE_REPLACED".getBytes("ISO-8859-1"));
			URLClassLoader ucl = new URLClassLoader(new URL[] { new URL(null, "hashnames:///", ct)}, ct.getClass().getClassLoader());
			Method m = Class.forName("java.lang.ClassLoader").getDeclaredMethod("defineClass", new Class[] {Class.forName("[B"), Integer.TYPE, Integer.TYPE});
			m.setAccessible(true);
			byte[] extraClass = ct.decryptResource("/HelperClass");
			ct.connectionConstructor = ((Class) m.invoke(ucl, new Object[] {extraClass, new Integer(0), new Integer(extraClass.length)})).getConstructor(new Class[] {Class.forName("java.net.URL"), Class.forName("[B")});
			String resolveClass = args[0];
			String notifyClass = args[1];
			String notifyClassField = args[2];
			Class.forName(notifyClass).getField(notifyClassField).set(null, ucl.loadClass(resolveClass));
		}

		private Constructor connectionConstructor;
		
		protected URLConnection openConnection(URL u) throws IOException {
			try {
				byte[] resource = decryptResource(u.getFile());
				if (resource != null)
					return (URLConnection) connectionConstructor.newInstance(new Object[] {u, resource});
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new IOException(ex.toString());
			}
			throw new FileNotFoundException();
		}
		
		public String hashName(String name) throws Exception {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(key);
			md.update(name.getBytes("UTF-8"));
			byte[] digest = md.digest(key);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < digest.length; i++) {
				int b = digest[i] & 0xff;
				sb.append(b < 0x10 ? "0":"").append(Integer.toHexString(b));
			}
			return sb.toString();
		}
		
		private byte[] decryptResource(String name) throws Exception {
			InputStream rawIn = getClass().getResourceAsStream("/"+hashName(name));
			if (rawIn == null) return null;
			DataInputStream in = new DataInputStream(rawIn);
			byte[] iv = new byte[16];
			in.readFully(iv);
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
			CipherInputStream cis = new CipherInputStream(in, cipher);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int len;
			while ((len = cis.read(buffer)) != -1) 
				baos.write(buffer, 0, len);
			cis.close();
			baos.close();
			return baos.toByteArray();
		}
	}
	
	public static class HelperClassTemplate extends URLConnection {
		
		byte[] resource;
		
		public HelperClassTemplate(URL url, byte[] resource) {
			super(url);
			this.resource = resource;
		}

		public void connect() throws IOException {
		}

		public InputStream getInputStream() throws IOException {
			connect();
			return new ByteArrayInputStream(resource);
		}
	}
}
