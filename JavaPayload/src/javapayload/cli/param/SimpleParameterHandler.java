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

class SimpleParameterHandler extends ParameterHandler {

	public SimpleParameterHandler(Parameter parameter) {
		super(parameter);
	}

	public NamedElement[] getPossibleValues() {
		return new NamedElement[0];
	}

	public int getRepeatCount() {
		return 0;
	}

	protected char getSeparator() {
		return ' ';
	}

	public ParameterHandler[] getNextHandlers(String input, StringBuffer commandBuffer) throws ParameterFormatException {
		if (input.length() == 0) {
			if (!isOptional())
				throw new ParameterFormatException("Parameter is not optional");
			return new ParameterHandler[0];
		}
		validate(input);
		commandBuffer.append(input).append(getSeparator());
		return new ParameterHandler[0];
	}

	protected void validate(String input) throws ParameterFormatException {
		// nothing to do
	}

	protected static void registerHandlers() {
		TypeHandler simpleTypeHandler = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new SimpleParameterHandler(param);
			}
		};
		TypeHandler numericTypeHandler = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new SimpleParameterHandler(param) {
					protected void validate(String input) throws ParameterFormatException {
						try {
							Integer.parseInt(input);
						} catch (NumberFormatException ex) {
							throw new ParameterFormatException("Parameter must be numeric");
						}
					}
				};
			}
		};
		TypeHandler portHashTypeHandler = new TypeHandler() {
			public ParameterHandler getHandler(ParameterContext context, Parameter param) {
				return new SimpleParameterHandler(param) {
					protected void validate(String input) throws ParameterFormatException {
						if (input.equals("#"))
							return;
						try {
							Integer.parseInt(input);
						} catch (NumberFormatException ex) {
							throw new ParameterFormatException("Parameter must be numeric or #");
						}
					}
				};
			}
		};

		registerTypeHandler(Parameter.TYPE_ANY, simpleTypeHandler);
		registerTypeHandler(Parameter.TYPE_PATH, simpleTypeHandler); // path
																		// completion?
		registerTypeHandler(Parameter.TYPE_HOST, simpleTypeHandler);
		registerTypeHandler(Parameter.TYPE_URL, simpleTypeHandler);
		registerTypeHandler(Parameter.TYPE_NUMBER, numericTypeHandler);
		registerTypeHandler(Parameter.TYPE_PORT, numericTypeHandler);
		registerTypeHandler(Parameter.TYPE_PORT_HASH, portHashTypeHandler);
	}
}