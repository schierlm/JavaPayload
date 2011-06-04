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
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import javapayload.builder.AgentJarBuilder;
import javapayload.builder.AttachInjector;
import javapayload.stage.StreamForwarder;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

public class BuilderTest16 extends BuilderTest {
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
}
