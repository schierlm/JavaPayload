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
import java.io.IOException;

import javapayload.builder.CVE_2008_5353_AppletJarBuilder;
import javapayload.builder.CVE_2010_0840_AppletJarBuilder;

public class BuilderTest14 extends BuilderTest {
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
	
	public static class CVE_2010_0840TestRunner extends CVE_2008_5353TestRunner implements BuilderTestRunner {
		protected String getCVEName() { return "2010_0840"; }

		public void runBuilder(String[] args) throws Exception {
			CVE_2010_0840_AppletJarBuilder.main(new String[] { "cve.jar", args[0] });
		}
	}
}
