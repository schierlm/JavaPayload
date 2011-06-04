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
import java.io.FileOutputStream;
import java.io.IOException;

import javapayload.builder.JDWPInjector;
import javapayload.handler.stage.TestStub;
import javapayload.stage.StreamForwarder;

public class BuilderTest13 extends BuilderTest {
	public static class JDWPInjectorTestRunner extends WaitingBuilderTestRunner {
		public String getName() { return "JDWPInjector"; }

		public void runBuilder(String[] args) throws Exception {
			StreamForwarder.forward(BuilderTest.class.getResourceAsStream("/DummyClass.class"), new FileOutputStream("DummyClass.class"));
		}

		public void runResult(String[] args) throws Exception {
			StringBuffer allArgs = new StringBuffer();
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
}
