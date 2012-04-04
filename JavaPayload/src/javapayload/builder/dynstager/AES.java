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

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.security.AllPermission;
import java.security.MessageDigest;
import java.security.Permissions;
import java.security.SecureRandom;

import javapayload.handler.dynstager.SynchronizedOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AES extends WrappingDynStagerBuilder {

	public void bootstrapWrap(String[] parameters, boolean needWait) throws Exception {
		// move the password to the end
		String[] newParameters = new String[parameters.length];
		newParameters[0] = parameters[0];
		System.arraycopy(parameters, 2, newParameters, 1, parameters.length - 2);
		newParameters[parameters.length - 1] = parameters[1];
		bootstrapOrig(newParameters, needWait);
	}
	
	protected void bootstrapWrap(InputStream rawIn, OutputStream out, String[] parameters) {
		try {
			DataInputStream din = new DataInputStream(rawIn);
			din.readInt();
			String key = parameters[parameters.length - 1];
			String[] newParameters = new String[parameters.length - 1];
			System.arraycopy(parameters, 0, newParameters, 0, newParameters.length);
			SecureRandom sr = new SecureRandom();
			byte[] outIV = new byte[16];
			sr.nextBytes(outIV);
			out.write(outIV);
			out.flush();
			byte[] inIV = new byte[16];
			din.readFully(inIV);
			byte[] keyBytes = MessageDigest.getInstance("MD5").digest(key.getBytes());
			Cipher co = Cipher.getInstance("AES/CFB8/NoPadding");
			co.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(outIV), sr);
			Cipher ci = Cipher.getInstance("AES/CFB8/NoPadding");
			ci.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(inIV), sr);
			final Permissions permissions = new Permissions();
			permissions.add(new AllPermission());
			Class synchronizedOutputStreamClass;
			synchronizedOutputStreamClass = bootstrap();
			OutputStream so = (OutputStream) synchronizedOutputStreamClass.getConstructor(new Class[] { Class.forName("java.io.OutputStream") }).newInstance(new Object[] { new CipherOutputStream(out, co) });
			bootstrapOrig(new CipherInputStream(din, ci), so, newParameters);
		} catch (final Throwable t) {
			t.printStackTrace(new PrintStream(out, true));
		}
	}

	static long counter = 0;
	
	protected void handleCustomMethods(String bootstrapName, ClassWriter cw, String stagerName, Class baseStagerClass, String extraArg, String[] args) throws Exception {
		// give the class a new name, to avoid clashes when staging the class
		InputStream in = SynchronizedOutputStream.class.getResourceAsStream("/"+SynchronizedOutputStream.class.getName().replace('.', '/')+".class");
		final ClassReader cr = new ClassReader(in);
		final ClassWriter cw2 = new ClassWriter(0);
		cr.accept(new ClassAdapter(cw2) {
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, name+(counter++), signature, superName, interfaces);
			}
		}, ClassReader.SKIP_DEBUG);
		in.close();
		String classString = new String(cw2.toByteArray());
		
		// create the bootstrap method
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, bootstrapName, "()Ljava/lang/Class;", null, new String[] { "java/lang/Exception" });
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitLdcInsn(classString);
		mv.visitLdcInsn("ISO-8859-1");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "javapayload/stager/Stager", "define", "([B)Ljava/lang/Class;");
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitMaxs(3, 1);
		mv.visitEnd();
	}
	
	private Class bootstrap() throws Exception {
		throw new IllegalStateException("This method is replaced in the final stager");
		// return define("TO_BE_REPLACED".getBytes("ISO-8859-1"));
	}
}
