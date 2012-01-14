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

package javapayload.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import javapayload.builder.EmbeddedClassBuilder.EmbeddedClassBuilderTemplate;

public class BeanShellMacroBuilder extends Builder {
	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("Usage: java javapayload.builder.BeanShellMacroBuilder "+new BeanShellMacroBuilder().getParameterSyntax());
			return;
		}
		new BeanShellMacroBuilder().build(args);
	}
	
	public BeanShellMacroBuilder() {
		super("Build a BeanShell Macro for OpenOffice.org", "");
	}
	
	public int getMinParameterCount() {
		return 3;
	}
	
	public String getParameterSyntax() {
		return "<stager> [stageroptions] -- <stage> [stageoptions]";
	}
	
	public void build(String[] args) throws Exception {
		String[] builderArgs = new String[args.length+2];
		System.arraycopy(args, 0, builderArgs, 2, args.length);
		builderArgs[0] = "BeanShellMacro.bsh";
		builderArgs[1] = "-";
		new TemplateBuilder().build(builderArgs);
	}
}
