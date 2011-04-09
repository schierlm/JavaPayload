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

package javapayload.builder.dynstager;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class DynStagerBuilder {

	/**
	 * Build a new dynstager (<tt>Builder_BaseStager$extraArg</tt>).
	 * 
	 * @param baseStager
	 *            Stager to use as a base
	 * @param extraArg
	 *            Extra argument
	 * @param stagerArgs
	 *            Stager arguments
	 * @return Bytecode of the new dynstager
	 */
	public abstract byte[] buildStager(String stagerName, Class baseStagerClass, String extraArg, String[] args) throws Exception;
	

	protected void visitStringConstant(MethodVisitor mv, String constant) {
		final List stringParts = new ArrayList();
		final int MAXLEN = 65535;
		while (constant.length() > MAXLEN || getUTFLen(constant) > MAXLEN) {
			int len = Math.min(MAXLEN, constant.length()), utflen;
			while ((utflen = getUTFLen(constant.substring(0, len))) > MAXLEN) {
				len -= (utflen-MAXLEN+1)/2;
			}
			stringParts.add(constant.substring(0, len));
			constant=constant.substring(len);
		}
		stringParts.add(constant);		
		for (int i = 0; i < stringParts.size(); i++) {
			mv.visitLdcInsn((String)stringParts.get(i));
			if (i != 0) {
				mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;");
			}
		}
	}

	private static int getUTFLen(String str) {
		int utflen = 0;
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				throw new IllegalStateException();
			} else {
				utflen += 2;
			}
		}
		return utflen;
	}
}
