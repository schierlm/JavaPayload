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

package javapayload.cli.param;

import javapayload.NamedElement;
import javapayload.Parameter;

class RepeatedTypeParameterHandler extends ParameterHandler {

	protected final ParameterHandler target;
	private final ParameterContext context;
	private final int repeatCount;

	public RepeatedTypeParameterHandler(ParameterContext context, Parameter parameter, int repeatCount) {
		super(parameter);
		this.target = getHandler(context, new Parameter(parameter.getName(), parameter.isOptional(), parameter.getType() - 100, parameter.getSummary()));
		this.context = context;
		this.repeatCount = repeatCount;
	}

	public int getRepeatCount() {
		return repeatCount;
	}

	public ParameterHandler[] getNextHandlers(String input, StringBuffer commandBuffer) throws ParameterFormatException {
		if (input.length() == 0) {
			if (!parameter.isOptional())
				throw new IllegalArgumentException("Mandatory parameter cannot be empty");
			return new ParameterHandler[0];
		}
		ParameterHandler[] nextOnes = target.getNextHandlers(input, commandBuffer);
		ParameterHandler[] result = new ParameterHandler[nextOnes.length + 1];
		System.arraycopy(nextOnes, 0, result, 0, nextOnes.length);
		result[nextOnes.length] = new RepeatedTypeParameterHandler(context, parameter.isOptional() ? parameter : new Parameter(parameter.getName(), true, parameter.getType(), parameter.getSummary()), repeatCount + 1);
		return result;
	}

	public NamedElement[] getPossibleValues() {
		return target.getPossibleValues();
	}

	public String[] autoComplete(String prefix) {
		return target.autoComplete(prefix);
	}
}