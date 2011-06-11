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

import javapayload.builder.AgentJarBuilder;
import javapayload.builder.CVE_2010_0094_AppletJarBuilder;
import javapayload.builder.CVE_2010_4465_AppletJarBuilder;
import javapayload.stage.StreamForwarder;
import javapayload.test.BuilderTest14.CVE_2008_5353TestRunner;

public class BuilderTest15 extends BuilderTest {
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
	
	public static class CVE_2010_0094TestRunner extends CVE_2008_5353TestRunner implements BuilderTestRunner {
		protected String getCVEName() { return "2010_0094"; }

		public void runBuilder(String[] args) throws Exception {
			CVE_2010_0094_AppletJarBuilder.main(new String[] { "cve.jar", args[0] });
		}
	}
	
	public static class CVE_2010_4465TestRunner extends CVE_2008_5353TestRunner implements BuilderTestRunner {
		protected String getCVEName() { return "2010_4465"; }

		public void runBuilder(String[] args) throws Exception {
			CVE_2010_4465_AppletJarBuilder.main(new String[] { "cve.jar", args[0] });
		}
	}
}
