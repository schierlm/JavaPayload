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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Pattern;

import javapayload.builder.ClassBuilder;
import javapayload.builder.JarBuilder;
import javapayload.handler.stager.StagerHandler;

public class StageTest {
	public static void main(String[] args) throws Exception {
		startMultiListen();
		startHelperSockets();
		try {
			File baseDir = new File(StageTest.class.getResource("stagetests").toURI());
			if (args.length == 1) {
				File file = new File(baseDir, args[0] + ".txt");
				testStage(file, "LocalTest", "", new OutputStreamWriter(System.out));
			} else {
				File[] files = baseDir.listFiles();
				System.out.println("Testing stages (LocalTest)...");
				for (int i = 0; i < files.length; i++) {
					testStage(files[i], "LocalTest");
				}
				System.out.println("Testing stages (AES_LocalTest)...");
				for (int i = 0; i < files.length; i++) {
					testStage(files[i], "AES_LocalTest #");
				}
				System.out.println("Testing stages (AES_AES_LocalTest)...");
				for (int i = 0; i < files.length; i++) {
					testStage(files[i], "AES_AES_LocalTest # #");
				}
				System.out.println("Testing stages (Integrated$#_LocalTest)...");
				for (int i = 0; i < files.length; i++) {
					testStage(files[i], "Integrated$Stage#_LocalTest");
				}
				System.out.println("Testing stages (LocalStage_LocalTest)...");
				for (int i = 0; i < files.length; i++) {
					testStage(files[i], "LocalStage_LocalTest");
				}
				System.out.println("Testing stages (BindTCP)...");
				String[] stagerArgs = new String[] { "BindMultiTCP", "localhost", "60123" };
				ClassBuilder.main(new String[] { stagerArgs[0], "BuilderTestClass" });
				Process proc = BuilderTest.runJava(".", null, "BuilderTestClass", stagerArgs);
				for (int i = 0; i < files.length; i++) {
					if(files[i].getName().startsWith("Drop")) {
						JarBuilder.main(new String[] {"StageTestJar.jar", "LocalStage_BindTCP", "--", "test/./javapayload/test/stagetests/DropExec.exe", "DropExec", "SendParameters"});
						Process proc2 = BuilderTest.runJava("StageTestJar.jar", null, "javapayload.loader.StandaloneLoader", new String[] {"LocalStage_BindTCP", "localhost", "60321", "--", "SendParameters"});
						Thread.sleep(500);
						testStage(files[i], "LocalStage_BindTCP localhost 60321", "");
						if (proc2.waitFor() != 0)
							throw new IOException("Build result exited with error code " + proc.exitValue());
						if (!new File("StageTestJar.jar").delete())
							throw new IOException("Unable to delete file");
						continue;
					} else if (files[i].getName().startsWith("LocalStageMenu")) {
						JarBuilder.main(new String[] {"StageTestJar.jar", "LocalStage_BindTCP", "--", "LocalStageMenu", "SendParameters", "Shell", "JSh", "Exec", "StopListening", "SystemInfo"});
						Process proc2 = BuilderTest.runJava("StageTestJar.jar", null, "javapayload.loader.StandaloneLoader", new String[] {"LocalStage_BindTCP", "localhost", "60321", "--", "SendParameters"});
						Thread.sleep(500);
						testStage(files[i], "LocalStage_BindTCP localhost 60321", "");
						if (proc2.waitFor() != 0)
							throw new IOException("Build result exited with error code " + proc.exitValue());
						if (!new File("StageTestJar.jar").delete())
							throw new IOException("Unable to delete file");
						continue;
					}
					testStage(files[i], "BindTCP localhost 60123");
				}
				StagerHandler.main("BindTCP localhost 60123 -- StopListening".split(" "));
				StagerHandler.main("BindTCP localhost 60123 -- StopListening".split(" "));
				if (proc.waitFor() != 0)
					throw new IOException("Build result exited with error code " + proc.exitValue());
				new File("jsh.txt").delete();
				if (!new File("BuilderTestClass.class").delete())
					throw new IOException("Unable to delete file");
				System.out.println("Stage tests finished.");
				new ThreadWatchdogThread(5000).start();
			}
		} finally {
			stopMultiListen();
			stopHelperSockets();
		}
	}

	private static void testStage(File file, String stager) throws Exception {
		testStage(file, stager, "");
		testStage(file, stager, "AES someV3rySecret ");
		testStage(file, stager, "AES oneSecret AES otherSecret ");
		if (!file.getName().equals("LocalProxy.txt")) {
			if (stager.equals("LocalTest"))
				testStage(file, stager, "GZ ");
			testStage(file, stager, "GZIP ");
		}
	}
	
	private static void testStage(File file, String stager, String stagePrefix) throws Exception {
		if (!file.getName().endsWith(".txt"))
			return;
		StringWriter sw = new StringWriter();
		Pattern regex = testStage(file, stager, stagePrefix, sw);
		String output = sw.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
		if (!regex.matcher(output).matches()) {
			System.err.println("Pattern:\r\n" + regex.pattern());
			System.err.println("Output:\r\n" + output);
			throw new Exception("Output did not match");
		}
	}
	
	private static int stageCounter = 0;
	
	private static Pattern testStage(File file, String stager, String stagePrefix, Writer output) throws Exception {
		BufferedReader desc = new BufferedReader(new FileReader(file));
		System.out.println("\t" + stagePrefix + file.getName().replaceAll("\\.txt", ""));
		String stage = stagePrefix + desc.readLine();
		StringBuffer sb = new StringBuffer();
		String delimiter = desc.readLine();
		String line;
		while ((line = desc.readLine()) != null) {
			if (line.equals(delimiter))
				break;
			sb.append(line).append("\r\n");
		}
		// use SendParameters stage when testing stages with parameters
		// with the BindMultiTCP stager
		if (stage.contains(" ") && !stager.endsWith("LocalTest") && !stage.startsWith("LocalProxy "))
			stage = "SendParameters " + stage;
		if (stager.contains("#"))
			stager = stager.replaceAll("#", ""+(++stageCounter));
		String[] args = (stager + " -- " + stage).split(" ");
		StagerHandler.Loader loader = new StagerHandler.Loader(args);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		loader.stageHandler.consoleErr = out;
		loader.stageHandler.consoleOut = out;
		loader.stageHandler.consoleIn = new BlockingInputStream(new ByteArrayInputStream(sb.toString().getBytes()));
		loader.handle(System.err, null);
		sb.setLength(0);
		while ((line = desc.readLine()) != null) {
			if (line.equals(delimiter)) break;
			sb.append(line).append("\n");
		}
		while(sb.length() > 0 && sb.charAt(sb.length()-1) == '\n')
			sb.setLength(sb.length()-1);
		Thread.sleep(500);
		out.flush();
		String outputStr = new String(baos.toByteArray());
		while (outputStr.endsWith("\n") || outputStr.endsWith("\r"))
			outputStr = outputStr.substring(0, outputStr.length() - 1);
		output.write(outputStr);
		output.flush();
		return Pattern.compile(sb.toString());
	}

	private static PipedOutputStream multiListenPOS;
	private static StagerHandler.Loader multiListenLoader;

	private static void startMultiListen() throws Exception {
		String[] args = ("MultiListen ReverseTCP localhost 59493 -- TestStub").split(" ");
		multiListenLoader = new StagerHandler.Loader(args);
		multiListenPOS = new PipedOutputStream();
		multiListenLoader.stageHandler.consoleIn = new PipedInputStream(multiListenPOS);
		multiListenLoader.stageHandler.consoleOut = new PrintStream(new ByteArrayOutputStream());
		multiListenLoader.handleBefore(System.err, null);
	}

	private static void stopMultiListen() throws Exception {
		multiListenPOS.close();
		multiListenLoader.handleAfter(System.err, null);
	}

	private static ServerSocket helperSocket;

	private static void startHelperSockets() throws Exception {
		helperSocket = new ServerSocket(32199);
		new Thread(new Runnable() {
			public void run() {
				try {
					while (true) {
						final Socket s = helperSocket.accept();
						new Thread(new Runnable() {
							public void run() {
								try {
									new javapayload.stage.TestStub().start(new DataInputStream(s.getInputStream()), s.getOutputStream(), null);
								} catch (Throwable ex) {
									ex.printStackTrace();
									System.exit(2);
								}
							}
						}).start();
						final Socket s2 = new Socket("localhost", 32198);
						new javapayload.handler.stage.TestStub().handleStreams(new DataOutputStream(s2.getOutputStream()), s2.getInputStream(), null);
					}
				} catch (SocketException ex) {
					// ignore
				} catch (Throwable ex) {
					ex.printStackTrace();
					System.exit(2);
				}
			}
		}).start();
	}

	private static void stopHelperSockets() throws Exception {
		helperSocket.close();
	}
}
