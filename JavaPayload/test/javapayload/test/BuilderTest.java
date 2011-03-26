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
package javapayload.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javapayload.builder.AgentJarBuilder;
import javapayload.builder.AppletJarBuilder;
import javapayload.builder.AttachInjector;
import javapayload.builder.CVE_2008_5353_AppletJarBuilder;
import javapayload.builder.CVE_2010_0094_AppletJarBuilder;
import javapayload.builder.CVE_2010_0840_AppletJarBuilder;
import javapayload.builder.ClassBuilder;
import javapayload.builder.EmbeddedClassBuilder;
import javapayload.builder.EmbeddedJarBuilder;
import javapayload.builder.JDWPInjector;
import javapayload.builder.JarBuilder;
import javapayload.builder.SpawnTemplate;
import javapayload.handler.stage.TestStub;
import javapayload.handler.stager.StagerHandler;
import javapayload.stage.StreamForwarder;

public class BuilderTest {

	public static void main(String[] args) throws Exception {
		System.out.println("Testing builders...");
		String[] stagers = StagerTest.getStagers();
		BuilderTestRunner[] runners = new BuilderTestRunner[] {
				new ClassBuilderTestRunner(),
				new EmbeddedClassBuilderTestRunner(),
				new JarBuilderTestRunner(),
				new StripJarBuilderTestRunner(),
				new EmbeddedJarBuilderTestRunner(),
				new StripEmbeddedJarBuilderTestRunner(),
				new AgentJarBuilderTestRunner(),
				new AppletJarBuilderTestRunner(),
				// new CVE_2008_5353TestRunner(),
				// new CVE_2010_0094TestRunner(),
				// new CVE_2010_0840TestRunner(),
				new AttachInjectorTestRunner(),
				new JDWPInjectorTestRunner(),
		};
		for (int i = 0; i < runners.length; i++) {
			BuilderTestRunner runner = runners[i];
			System.out.println("\t" + runner.getName());
			for (int j = 0; j < stagers.length; j++) {
				String name = stagers[j];
				System.out.println("\t\t" + name);
				String[] testArgs = StagerTest.getTestArgs(name);
				for (int k = 0; k < testArgs.length; k++) {
					System.out.println("\t\t\t" + testArgs[k]);
					testBuilder(runner, name, testArgs[k]);
				}
			}
			runner.cleanup();
		}
		System.out.println("Builder tests finished.");
		new ThreadWatchdogThread(5000).start();
	}

	protected static void testBuilder(final BuilderTestRunner runner, String name, String testArgs) throws Exception {
		final String[] args = (name + " " + testArgs + " -- TestStub").split(" ");
		final StagerHandler.Loader loader = (runner.getName().indexOf("Injector") != -1) ? null : new StagerHandler.Loader(args);
		if (runner.getName().indexOf("[kill]") != -1) {
			if (name.startsWith("Spawn"))
				return;
			((TestStub) loader.stageHandler).sendExit = true;
		}
		if (loader != null)
			loader.handleBefore(System.err, null);
		runner.runBuilder(loader == null ? args : loader.getArgs());
		final Throwable[] tt = new Throwable[1];
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					runner.runResult(loader == null ? args : loader.getArgs());
				} catch (Throwable t) {
					tt[0] = t;
				}
			};
		});
		t.start();
		Thread.sleep(runner.getDelay());
		if (loader != null)
			loader.handleAfter(System.err, null);
		t.join();
		if (tt[0] != null)
			throw new Exception("Builder result died", tt[0]);
	}

	public static void runJavaAndWait(String classpath, String jvmarg, String mainClass, String[] args) throws Exception {
		Process proc = runJava(classpath, jvmarg, mainClass, args);
		if (proc.waitFor() != 0)
			throw new IOException("Build result exited with error code " + proc.exitValue());
	}

	public static Process runJava(String classpath, String jvmarg, String mainClass, String[] args) throws IOException {
		List commands = new ArrayList();
		commands.add(SpawnTemplate.getJreExecutable("java"));
		commands.add("-classpath");
		commands.add(classpath + System.getProperty("javapayload.child.classpath", ""));
		if (System.getProperty("net.sourceforge.cobertura.datafile") != null) {
			commands.add("-Dnet.sourceforge.cobertura.datafile=" + System.getProperty("net.sourceforge.cobertura.datafile").replace('1', '2'));
		}
		if (jvmarg != null)
			commands.add(jvmarg);
		commands.add(mainClass);
		commands.addAll(Arrays.asList(args));
		String[] commandArgs = (String[]) commands.toArray(new String[commands.size()]);
		Process proc = Runtime.getRuntime().exec(commandArgs);
		return proc;
	}

	public static void runAppletAndWait(String archive, String className, String policyfile, String[] args) throws Exception {
		FileWriter fw = new FileWriter("applettest.html");
		String childClassPath = System.getProperty("javapayload.child.classpath", "");
		if (childClassPath.startsWith(";")) {
			childClassPath=",file:///"+childClassPath.substring(1);
		}
		fw.write("<applet archive=\"" + archive + childClassPath + "\" code=\"" + className + "\" width=\"100\" height=\"100\">\r\n" +
				"		<param name=\"argc\" value=\"" + args.length + "\">\r\n");
		for (int i = 0; i < args.length; i++) {
			fw.write("		<param name=\"arg" + i + "\" value=\"" + args[i] + "\" />\r\n");
		}
		fw.write("		</applet></tt></p>");
		fw.close();
		List commands = new ArrayList();
		commands.add(System.getProperty("javapayload.vulnerable.jdk") + "\\bin\\appletviewer");
		if (System.getProperty("net.sourceforge.cobertura.datafile") != null) {
			commands.add("-J-Dnet.sourceforge.cobertura.datafile=" + System.getProperty("net.sourceforge.cobertura.datafile").replace('1', '2'));
		}
		if (policyfile != null) {
			fw = new FileWriter("applettest.policy");
			fw.write(policyfile);
			fw.close();
			commands.add("-J-Djava.security.policy=applettest.policy");
		}
		commands.add("applettest.html");
		String[] commandArgs = (String[]) commands.toArray(new String[commands.size()]);
		Process proc = Runtime.getRuntime().exec(commandArgs);
		new StreamForwarder(proc.getErrorStream(), System.err, System.err).start();
		if (proc.waitFor() != 0)
			throw new IOException("Build result exited with error code " + proc.exitValue());
	}

	public static interface BuilderTestRunner {
		String getName();
		int getDelay();
		void runBuilder(String[] args) throws Exception;
		void runResult(String[] args) throws Exception;
		void cleanup() throws Exception;
	}

	public static class ClassBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "ClassBuilder"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			ClassBuilder.main(new String[] { args[0], "BuilderTestClass" });
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait(".", null, "BuilderTestClass", args);
		}

		public void cleanup() throws Exception {
			if (!new File("BuilderTestClass.class").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class EmbeddedClassBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "EmbeddedClassBuilder"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			List builderArgs = new ArrayList();
			builderArgs.add("BuilderTestClass");
			builderArgs.addAll(Arrays.asList(args));
			EmbeddedClassBuilder.main((String[]) builderArgs.toArray(new String[builderArgs.size()]));
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait(".", null, "BuilderTestClass", new String[0]);
		}

		public void cleanup() throws Exception {
			if (!new File("BuilderTestClass.class").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class JarBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "JarBuilder"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			JarBuilder.main(new String[] { args[0] });
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait(args[0] + ".jar", null, "javapayload.loader.StandaloneLoader", args);
			if (!new File(args[0] + ".jar").delete())
				throw new IOException("Unable to delete file");
		}

		public void cleanup() throws Exception {
		}
	}
	
	public static class StripJarBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "JarBuilder (stripped)"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			JarBuilder.main(new String[] { "--strip", "stripped.jar", args[0] });
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait("stripped.jar", null, "javapayload.loader.StandaloneLoader", args);
		}

		public void cleanup() throws Exception {
			if (!new File("stripped.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class EmbeddedJarBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "EmbeddedJarBuilder"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			EmbeddedJarBuilder.main(args);
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait("embedded.jar", null, "javapayload.loader.EmbeddedJarLoader", new String[0]);
		}

		public void cleanup() throws Exception {
			if (!new File("embedded.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}
	
	public static class StripEmbeddedJarBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "EmbeddedJarBuilder (stripped)"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			String[] newArgs = new String[args.length+2];
			newArgs[0] = "--strip";
			newArgs[1] = "embstrip.jar";
			System.arraycopy(args, 0, newArgs, 2, args.length);
			EmbeddedJarBuilder.main(newArgs);
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait("embstrip.jar", null, "javapayload.loader.EmbeddedJarLoader", new String[0]);
		}

		public void cleanup() throws Exception {
			if (!new File("embstrip.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class AgentJarBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "AgentJarBuilder"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			AgentJarBuilder.main(new String[] { args[0] });
			StreamForwarder.forward(BuilderTest.class.getResourceAsStream("/DummyClass.class"), new FileOutputStream("DummyClass.class"));
		}

		public void runResult(String[] args) throws Exception {
			StringBuilder allArgs = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				if (i != 0)
					allArgs.append(' ');
				allArgs.append(args[i]);
			}
			String agentArg = "-javaagent:Agent_" + args[0] + ".jar=" + allArgs.toString();
			runJavaAndWait(".", agentArg, "DummyClass", new String[0]);
			if (!new File("Agent_" + args[0] + ".jar").delete())
				throw new IOException("Unable to delete file");
		}

		public void cleanup() throws Exception {
			if (!new File("DummyClass.class").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class AppletJarBuilderTestRunner implements BuilderTestRunner {
		public String getName() { return "AppletJarBuilder [kill]";	}
		public int getDelay() { return 3000; }

		public void runBuilder(String[] args) throws Exception {
			AppletJarBuilder.main(new String[] { args[0] });
		}

		public void runResult(String[] args) throws Exception {
			runAppletAndWait("Applet_" + args[0] + ".jar", "javapayload.loader.AppletLoader", "grant { permission java.security.AllPermission; };", args);
			if (!new File("Applet_" + args[0] + ".jar").delete())
				throw new IOException("Unable to delete file");
		}

		public void cleanup() throws Exception {
			if (!new File("applettest.html").delete())
				throw new IOException("Unable to delete file");
			if (!new File("applettest.policy").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class CVE_2008_5353TestRunner implements BuilderTestRunner {
		protected String getCVEName() { return "2008_5353"; }
		public String getName() { return "CVE_"+getCVEName()+" [kill]"; }
		public int getDelay() { return 3000; }

		public void runBuilder(String[] args) throws Exception {
			CVE_2008_5353_AppletJarBuilder.main(new String[] { "cve.jar", args[0] });
		}

		public void runResult(String[] args) throws Exception {
			runAppletAndWait("cve.jar", "javapayload.exploit.CVE_"+getCVEName(), null, args);
		}

		public void cleanup() throws Exception {
			if (!new File("applettest.html").delete())
				throw new IOException("Unable to delete file");
			if (!new File("cve.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}
	
	public static class CVE_2010_0094TestRunner extends CVE_2008_5353TestRunner implements BuilderTestRunner {
		protected String getCVEName() { return "2010_0094"; }

		public void runBuilder(String[] args) throws Exception {
			CVE_2010_0094_AppletJarBuilder.main(new String[] { "cve.jar", args[0] });
		}
	}
	
	public static class CVE_2010_0840TestRunner extends CVE_2008_5353TestRunner implements BuilderTestRunner {
		protected String getCVEName() { return "2010_0840"; }

		public void runBuilder(String[] args) throws Exception {
			CVE_2010_0840_AppletJarBuilder.main(new String[] { "cve.jar", args[0] });
		}
	}

	public static class AttachInjectorTestRunner implements BuilderTestRunner {
		public String getName() { return "AttachInjector"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			AgentJarBuilder.main(new String[] { args[0] });
			StreamForwarder.forward(BuilderTest.class.getResourceAsStream("/DummyClass.class"), new FileOutputStream("DummyClass.class"));
		}

		public void runResult(String[] args) throws Exception {
			Process proc = runJava(".", null, "DummyClass", new String[] { "1001" });
			Thread.sleep(50);
			List vms = VirtualMachine.list();
			String id = null;
			for (int i = 0; i < vms.size(); i++) {
				VirtualMachineDescriptor desc = (VirtualMachineDescriptor) vms.get(i);
				if (desc.displayName().equals("DummyClass 1001")) {
					id = desc.id();
					break;
				}
			}
			String[] attachArgs = new String[args.length+2];
			attachArgs[0] = id;
			attachArgs[1] = "Agent_" + args[0] + ".jar";
			System.arraycopy(args, 0, attachArgs, 2, args.length);
			AttachInjector.main(attachArgs);
			if (proc.waitFor() != 0)
				throw new IOException("Build result exited with error code " + proc.exitValue());
			if (!new File("Agent_" + args[0] + ".jar").delete())
				throw new IOException("Unable to delete file");
		}

		public void cleanup() throws Exception {
			AttachInjector.listVMs(new PrintStream(new ByteArrayOutputStream()));
			if (!new File("DummyClass.class").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class JDWPInjectorTestRunner implements BuilderTestRunner {
		public String getName() { return "JDWPInjector"; }
		public int getDelay() { return 100; }

		public void runBuilder(String[] args) throws Exception {
			StreamForwarder.forward(BuilderTest.class.getResourceAsStream("/DummyClass.class"), new FileOutputStream("DummyClass.class"));
		}

		public void runResult(String[] args) throws Exception {
			StringBuilder allArgs = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				if (i != 0)
					allArgs.append(' ');
				allArgs.append(args[i]);
			}
			String jdwpArg = "-Xrunjdwp:transport=dt_socket,suspend=y,server=y,address=62468";
			Process proc = runJava(".", jdwpArg, "DummyClass", new String[0]);
			String[] injectorArgs = new String[args.length + 1];
			injectorArgs[0] = "localhost:62468";
			System.arraycopy(args, 0, injectorArgs, 1, args.length);
			TestStub.wait = 1100;
			if (args[0].endsWith("JDWPTunnel") || args[0].startsWith("Spawn_"))
				TestStub.wait = 5000;
			if (args[0].startsWith("Spawn_Spawn_"))
				TestStub.wait=9000;
			JDWPInjector.main(injectorArgs);
			if (proc.waitFor() != 0)
				throw new IOException("Build result exited with error code " + proc.exitValue());
		}

		public void cleanup() throws Exception {
			Thread.sleep(1000);
			System.out.println("\t\tJDWPTunnel");
			testBuilder(this, "JDWPTunnel", "");
			System.out.println("\t\tAESJDWPTunnel");
			testBuilder(this, "AESJDWPTunnel", "#");
			System.out.println("\t\tAES_AES_JDWPTunnel");
			testBuilder(this, "AES_AES_JDWPTunnel", "# #");
			if (!new File("DummyClass.class").delete())
				throw new IOException("Unable to delete file");
			TestStub.wait = 0;
		}
	}

}
