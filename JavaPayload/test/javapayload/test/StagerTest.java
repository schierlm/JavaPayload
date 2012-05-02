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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javapayload.Module;
import javapayload.handler.stager.StagerHandler;
import javapayload.loader.DynLoader;
import javapayload.loader.StandaloneLoader;
import javapayload.stage.StageMenu;
import javapayload.stager.Stager;

public class StagerTest {

	public static void main(String[] args) throws Exception {
		System.out.println("Testing stagers...");
		String[] localStagers = new String[] {"LocalTest", "AES_LocalTest #", "SpawnConsole", "LocalPollingTunnel"};
		for (int i = 0; i < localStagers.length; i++) {
			if (localStagers[i].startsWith("AES_")) {
				try {
					Class.forName("javapayload.handler.dynstager.AES");
				} catch (ClassNotFoundException ex) {
					continue;
				}
			}
			System.out.println("\t"+localStagers[i]);
			final StagerHandler.Loader loader = new StagerHandler.Loader(StageMenu.splitArgs(localStagers[i] + " -- TestStub"));
			loader.handle(System.err, null);
		}
		System.out.println("\tMultiListen");
		String[] multiListenArgs = new String[] {"ReverseTCP localhost #", "ReverseSSL localhost 61234"};
		for (int j = 0; j < multiListenArgs.length; j++) {
			System.out.println("\t\t" + multiListenArgs[j]);
			testMultiListen(multiListenArgs[j]);
		}
		String[] stagers = getStagers();
		for (int i = 0; i < stagers.length; i++) {
			String name = stagers[i];
			testStager(name, name.indexOf("UDP") != -1 || name.indexOf('$') != -1);
		}
		System.out.println("Stager tests finished.");
		new ThreadWatchdogThread(5000).start();
	}

	public static void testStager(String name, boolean fast) throws Exception {
		System.out.println("\t" + name);
		String[] testArgs = getTestArgs(name);
		for (int j = 0; j < testArgs.length; j++) {
			System.out.println("\t\t" + testArgs[j]);
			testStager(name, testArgs[j], fast);
		}
	}

	private static void testStager(String name, String testArgs, boolean fast) throws Exception {
		String[] args = StageMenu.splitArgs(name + " " + testArgs + " -- TestStub" + (fast ? " Fast" : ""));
		final StagerHandler.Loader loader = new StagerHandler.Loader(args);
		loader.handleBefore(System.err, null);
		final Throwable[] tt = new Throwable[1];
		final Stager[] ss = new Stager[1];
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					String[] args = loader.getArgs();
					final Stager stager = (Stager) DynLoader.loadStager(args[0], args, 0).newInstance();
					synchronized(ss) {
						ss[0] = stager;
						ss.notifyAll();
					}
					stager.bootstrap(args, true);
				} catch (Throwable t) {
					t.printStackTrace();
					tt[0] = t;
				}
			};
		});
		t.start();
		synchronized(ss) {
			while (ss[0] == null) {
				ss.wait();
			}
		}
		ss[0].waitReady();
		loader.handleAfter(System.err, null);
		t.join();
		if (tt[0] != null)
		{
			/* #JDK1.4 */try {
				throw new Exception("Stager died", tt[0]);
			} catch (NoSuchMethodError ex2) /**/{
				throw new Exception("Stager died: " + tt[0]);
			}
		}
	}

	private static void testMultiListen(String testArgs) throws Exception {
		String[] args = StageMenu.splitArgs("MultiListen " + testArgs + " -- TestStub");
		if (args[1].endsWith("SSL")) {
			try {
				Class.forName("javapayload.handler.stager."+args[1]);
			} catch (ClassNotFoundException ex) {
				return;
			}
		}
		final StagerHandler.Loader loader = new StagerHandler.Loader(args);
		PipedOutputStream pos = new PipedOutputStream();
		loader.stageHandler.consoleIn = new PipedInputStream(pos);
		loader.stageHandler.consoleOut = new PrintStream(new ByteArrayOutputStream());
		loader.handleBefore(System.err, null);
		String[] stagerArgs = new String[loader.getArgs().length-1];
		System.arraycopy(loader.getArgs(), 1, stagerArgs, 0, stagerArgs.length);
		StandaloneLoader.main(stagerArgs);
		StandaloneLoader.main(stagerArgs);
		StandaloneLoader.main(stagerArgs);
		pos.close();
		loader.handleAfter(System.err, null);
	}

	public static String[] getStagers() throws Exception {
		List result = new ArrayList();
		File stagerDir = Module.urlToFile(Stager.class.getResource("/" + Stager.class.getPackage().getName().replace('.', '/')));
		File[] classFiles = stagerDir.listFiles();
		for (int i = 0; i < classFiles.length; i++) {
			File classFile = classFiles[i];
			if (!classFile.getName().endsWith(".class"))
				continue;
			String className = classFile.getName();
			className = className.substring(0, className.length() - 6);
			String[] args = getTestArgs(className);
			if (args != null)
				result.add(className);
		}
		// TODO still needed?
		// add AES/Integrated dynstagers
		int origSize = result.size();
		for (int i = 0; i < origSize; i++) {
			String stager = (String) result.get(i);
			if (stager.equals("BindMultiTCP"))
				continue;
			result.add("AES_"+stager);
			if (stager.equals("LocalTest") || stager.equals("ReverseTCP") || stager.equals("JDWPTunnel"))
				result.add("AES_AES_"+stager);
			result.add("Integrated$Stager"+i+"_"+stager);
			result.add("Integrated$AESStager"+i+"_AES_"+stager);
		}
		// add spawn dynstagers
		origSize = result.size();
		for (int i = 0; i < origSize; i++) {
			String stager = (String) result.get(i);
			result.add("Spawn_"+stager);
			result.add("Spawn_Spawn_"+stager);
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public static String[] getTestArgs(String className) throws Exception {
		try {
			return StagerHandler.getStagerHandler(className).getTestArgumentArray();
		} catch (ClassNotFoundException ex) {
			return null;
		}
	}
}
