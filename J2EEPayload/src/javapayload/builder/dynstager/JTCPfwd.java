/*
 * Java Payloads.
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

package javapayload.builder.dynstager;

import j2eepayload.builder.JTCPfwdBuilder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import jtcpfwd.CustomLiteBuilder;
import jtcpfwd.Lookup;
import jtcpfwd.Module;
import jtcpfwd.forwarder.Forwarder;
import jtcpfwd.listener.Listener;

public class JTCPfwd extends DynStagerBuilder {

	public byte[] buildStager(String stagerResultName, Class baseStagerClass, String extraArg, String[] args) throws Exception {
		boolean listener = false;
		if (baseStagerClass == javapayload.stager.Listener.class) {
			listener = true;
		} else if (baseStagerClass != javapayload.stager.Forwarder.class) {
			throw new IllegalStateException("Unsupported base stager: " + baseStagerClass);
		}
		HashSet /* <String> */classNameSet = new HashSet();
		if (extraArg != null && extraArg.length() > 0) {
			StringTokenizer st = new StringTokenizer(extraArg, "$");
			while (st.hasMoreTokens()) {
				CustomLiteBuilder.addRequiredClasses(classNameSet, Module.lookup(listener ? Listener.class : Forwarder.class, st.nextToken()));
			}
		} else {
			Module mainModule = Lookup.lookupClass(listener ? Listener.class : Forwarder.class, args[1]);
			CustomLiteBuilder.addRequiredClasses(new int[CustomLiteBuilder.BASECLASS.length], classNameSet, mainModule);
			mainModule.dispose();
		}
		List /* <String> */classNames = new ArrayList(classNameSet);
		return JTCPfwdBuilder.buildClass("javapayload/stager/" + stagerResultName, listener, classNames, new PrintStream(new ByteArrayOutputStream()));
	}
}
