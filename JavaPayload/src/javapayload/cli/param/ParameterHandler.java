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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javapayload.NamedElement;
import javapayload.Parameter;
import javapayload.cli.Command;

public abstract class ParameterHandler {

	protected final Parameter parameter;

	public ParameterHandler(Parameter parameter) {
		this.parameter = parameter;
	}

	public final String getName() {
		return parameter.getName();
	}

	public final String getSummary() {
		return parameter.getSummary();
	}

	public final boolean isOptional() {
		return parameter.isOptional();
	}

	public abstract NamedElement[] getPossibleValues();

	public String[] autoComplete(String prefix) {
		List result = new ArrayList();
		NamedElement[] possibleValues = getPossibleValues();
		for (int i = 0; i < possibleValues.length; i++) {
			if (possibleValues[i].getName().startsWith(prefix))
				result.add(possibleValues[i].getName());
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	public abstract ParameterHandler[] getNextHandlers(String input, StringBuffer commandBuffer) throws ParameterFormatException;

	public abstract int getRepeatCount();

	public static interface TypeHandler {
		public ParameterHandler getHandler(ParameterContext context, Parameter param);
	}

	public static class ParameterFormatException extends Exception {
		public ParameterFormatException(String message) {
			super(message);
		}
	}

	public static void registerTypeHandler(int type, TypeHandler handler) {
		if (handlerMap.containsKey(new Integer(type)))
			throw new IllegalStateException("handler for type " + type + " already registered");
		handlerMap.put(new Integer(type), handler);
	}

	private static Map /* <Integer,ParameterTypeHandler> */handlerMap = new HashMap();

	static {
		SimpleParameterHandler.registerHandlers();
		ModuleParameterHandler.registerHandlers();
	}

	public static ParameterHandler[] getRootHandlers(Command cmd) throws ParameterFormatException {
		ParameterHandler[] result = new ParameterHandler[] {
				new ModuleParameterHandler.RootParameterHandler(new Parameter("COMMAND", false, Command.TYPE_COMMAND, "Command to build parameters of"))
		};
		if (cmd != null) {
			result = result[0].getNextHandlers(cmd.getName(), new StringBuffer());
		}
		return result;
	}

	public static ParameterHandler getHandler(ParameterContext context, Parameter parameter) {
		int type = parameter.getType();
		TypeHandler pth = (TypeHandler) handlerMap.get(new Integer(parameter.getType()));
		if (pth == null) {
			if (parameter.getType() >= 100 && parameter.getType() < 200) {
				return new RepeatedTypeParameterHandler(context, parameter, 0);
			} else if (parameter.getType() < 50) {
				pth = (TypeHandler) handlerMap.get(new Integer(0));
			}
		}
		if (pth == null)
			throw new IllegalArgumentException("No handler registered for type " + type);
		return pth.getHandler(context, parameter);
	}
}
