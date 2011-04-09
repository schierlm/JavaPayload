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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javapayload.handler.stage.StageHandler;
import javapayload.loader.DynLoader;
import javapayload.stage.StreamForwarder;

public class JarBuilder {

	public static final String ARGS_SYNTAX = "[--strip] [<filename>.jar] <stager> [<moreStagers...>] [-- [<filename.ext>] <stage> [<moreStages...>]]";

	protected static void buildJar(String filename, Class[] classes, boolean stripDebugInfo, Manifest manifest, String extraResourceName, byte[] extraResource) throws Exception {
		manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
		final JarOutputStream jos = new JarOutputStream(new FileOutputStream(filename), manifest);
		addClasses(jos, "", classes, stripDebugInfo);
		if (extraResource != null) {
			jos.putNextEntry(new ZipEntry(extraResourceName));
			jos.write(extraResource, 0, extraResource.length);
		}
		jos.close();
	}

	public static void addClasses(final JarOutputStream jos, final String prefix, Class[] classes, boolean stripDebugInfo) throws IOException {
		final byte[] buf = new byte[4096];
		int len;
		for (int i = 0; i < classes.length; i++) {
			final String classname = classes[i].getName().replace('.', '/') + ".class";
			jos.putNextEntry(new ZipEntry(prefix + classname));
			InputStream in = classes[i].getResourceAsStream("/" + classname);
			if (stripDebugInfo) {
				ClassBuilder.writeClassWithoutDebugInfo(in, jos);
			} else {
				while ((len = in.read(buf)) != -1) {
					jos.write(buf, 0, len);
				}
				in.close();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.JarBuilder " + ARGS_SYNTAX);
			return;
		}
		final Class[] baseClasses = new Class[] {
				javapayload.loader.StandaloneLoader.class,
				javapayload.stager.Stager.class,
		};
		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Main-Class", "javapayload.loader.StandaloneLoader");
		buildJarFromArgs(args, "", baseClasses, manifest, null, null);
	}

	public static void buildJarFromArgs(String[] args, String baseName, final Class[] baseClasses, final Manifest manifest, String extraResourceName, byte[] extraResource) throws ClassNotFoundException, Exception {
		boolean stripDebugInfo = false;
		final List classes = new ArrayList();
		classes.addAll(Arrays.asList(baseClasses));
		StringBuffer jarName = new StringBuffer(baseName);
		String overrideName = null;
		for (int i = 0; i < args.length; i++) {
			if (jarName.length() > 0)
				jarName.append('_');
			if (args[i].equals("--strip")) {
				jarName.append("stripped");
				stripDebugInfo = true;
			} else if (args[i].endsWith(".jar")) {
				overrideName = args[i]; 
			} else if (args[i].equals("--")) {
				jarName.append("stages");
				int firstArg = i+1;
				if (extraResource == null && args[i+1].indexOf('.') != -1) {
					firstArg = i+2;
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					FileInputStream fis = new FileInputStream( args[i+1]);
					StreamForwarder.forward(fis, baos);
					extraResource = baos.toByteArray();
					extraResourceName = new File(args[i+1]).getName();
					if (args[i+1].contains("/./"))
						extraResourceName = args[i+1].substring(args[i+1].indexOf("/./")+3);
				}
				Set classNames = new HashSet();
				for (int j = firstArg; j < args.length; j++) {
					StageHandler sh = (StageHandler) Class.forName("javapayload.handler.stage." + args[j]).newInstance();
					Class[] neededClasses;
					try {
						neededClasses = sh.getNeededClasses();
					} catch (IllegalStateException ex) {
						neededClasses = new Class[] {
								 javapayload.stage.Stage.class,
								 Class.forName("javapayload.stage."+args[j])
						};
					}
					for (int k = 0; k < neededClasses.length; k++) {
						if (!classNames.contains(neededClasses[k].getName())) {
							classNames.add(neededClasses[k].getName());
							classes.add(neededClasses[k]);
						}
					}
				}
				break;
			} else {
				jarName.append(args[i]);
				classes.add(DynLoader.loadStager(args[i], null, 0));
			}
		}
		buildJar(overrideName != null ? overrideName : jarName.append(".jar").toString(), (Class[]) classes.toArray(new Class[classes.size()]), stripDebugInfo, manifest, extraResourceName, extraResource);
	}
}
