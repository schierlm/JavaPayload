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

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javapayload.Module;
import javapayload.crypter.Crypter;
import javapayload.crypter.JarCrypter;
import javapayload.crypter.JarLayout;

public class CryptedJarBuilder extends Builder {
	
	public CryptedJarBuilder() {
		super("Crypt a Jar file", "");
	}

	public String getParameterSyntax() {
		return "<inputJar> <outputJar> <crypter> <jarCrypter> <jarLayout> [<layoutArgs>]";
	}

	public void build(String[] args) throws Exception {
		Crypter crypter = (Crypter)Module.load(Crypter.class, args[2]);
		JarCrypter jarCrypter = (JarCrypter)Module.load(JarCrypter.class, args[3]);
		JarLayout jarLayout = (JarLayout)Module.load(JarLayout.class, args[4]);
		String[] jarLayoutArgs = new String[args.length-5];
		System.arraycopy(args, 5, jarLayoutArgs, 0, jarLayoutArgs.length);
		JarInputStream jis = new JarInputStream(new FileInputStream(args[0]));
		Manifest manifest = jis.getManifest();
		ByteArrayOutputStream manifestStream = new ByteArrayOutputStream();
		manifest.write(manifestStream);
		jarLayout.init(jarLayoutArgs, manifest);
		String prefix = jarLayout.getPrefix();
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(args[1]), manifest);
		jarCrypter.addFile(jos, prefix, JarFile.MANIFEST_NAME, manifestStream.toByteArray());
		JarEntry je;
		int length;
		byte[] buf = new byte[4096];
		while ((je = jis.getNextJarEntry()) != null) {
			if (!jarLayout.shouldInclude(je, jis))
				continue;
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while ((length = jis.read(buf)) != -1) {
				out.write(buf, 0, length);
			}
			if (!je.getName().startsWith(prefix)) {
				jos.putNextEntry(je);
				out.writeTo(jos);
				continue;
			}
			jarCrypter.addFile(jos, prefix, je.getName().substring(prefix.length()), out.toByteArray());
		}
		String cryptedLoaderName = createRandomClassName();
		byte[] cryptedLoader = crypter.crypt(cryptedLoaderName, jarCrypter.createLoaderClass(jos, prefix, cryptedLoaderName+"$"));
		jos.putNextEntry(new ZipEntry(prefix + cryptedLoaderName + ".class"));
		jos.write(cryptedLoader);
		jarLayout.addStubs(jos, cryptedLoaderName);
		jis.close();
		jos.close();
	}

	public static String createRandomClassName() {
		Random rnd = new Random();
		String name = "";
		int len = rnd.nextInt(20)+10;
		while(name.length() < len) {
			char next = (char)('A' + rnd.nextInt(26));
			if (name.length() > 0 && rnd.nextBoolean())
				next = Character.toLowerCase(next);
			name += next;
		}
		return name;
	}
}
