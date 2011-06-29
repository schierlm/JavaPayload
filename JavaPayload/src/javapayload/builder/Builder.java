/*
 * Java Payloads.
 * 
 * Copyright (c) 2011 Michael 'mihi' Schierl
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

import java.io.PrintStream;

import javapayload.Module;
import javapayload.Parameter;

public abstract class Builder extends Module {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.Builder <builder> [<arguments>]");
			System.out.println();
			System.out.println("Supported builders:");
			Module.list(System.out, Builder.class);
			return;
		}
		Builder builder = (Builder) Module.load(Builder.class, args[0] + "Builder");
		if (args.length < builder.getMinParameterCount() + 1) {
			System.out.println("Usage: java javapayload.builder.Builder " + builder.getNameAndParameters());
			System.out.println();
			System.out.println(builder.getSummary());
			System.out.println();
			System.out.println(builder.getDescription());
			return;
		}
		String[] builderArgs = new String[args.length - 1];
		System.arraycopy(args, 1, builderArgs, 0, builderArgs.length);
		builder.build(builderArgs);
	}

	protected Builder(String summary, String description) {
		super("Builder", Builder.class, summary, description);
	}

	public Parameter[] getParameters() {
		throw new UnsupportedOperationException("Structured parameters not available for builders");
	}

	public int getMinParameterCount() {
		return 1;
	}
	
	public String getNameAndParameters() {
		return getName() + " " + getParameterSyntax();
	}
	
	public void printParameterDescription(PrintStream out) {
	}
	
	public abstract void build(String[] args) throws Exception;

	public abstract String getParameterSyntax();
}
