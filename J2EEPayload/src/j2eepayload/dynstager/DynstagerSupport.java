/*
 * J2EE Payloads.
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

package j2eepayload.dynstager;

import java.io.InputStream;
import java.io.OutputStream;

import javapayload.stager.LocalTest;
import javapayload.stager.Stager;

public class DynstagerSupport {

	public static void run(InputStream in, OutputStream out, String[] args) throws Exception {
		DynstagerSupport support = new DynstagerSupport();
		for (int i = 1; i < 10; i++) {
			try {
				support = (DynstagerSupport) Class.forName(DynstagerSupport.class.getName() + i).newInstance();
				break;
			} catch (Exception ex) {
			}
		}

		String[] extraArgs = support.getExtraArgs();
		if (extraArgs.length > 0) {
			String[] realArgs = args;
			args = new String[extraArgs.length + realArgs.length];
			System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
			System.arraycopy(realArgs, 1, args, extraArgs.length + 1, realArgs.length - 1);
			args[0] = realArgs[0];
		}

		support.createStager(in, out).bootstrap(args, false);
	}

	protected Stager createStager(InputStream in, OutputStream out) throws Exception {
		return new LocalTest(in, out);
	}

	protected String[] getExtraArgs() {
		return new String[0];
	}
}
