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

package javapayload.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javapayload.Module;
import javapayload.Parameter;
import javapayload.builder.Builder;
import javapayload.builder.Discovery;
import javapayload.builder.Injector;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.stage.StageHandler;
import javapayload.handler.stager.StagerHandler;

public class ShowCommand extends Command {

	private static final Map moduleTypes = new HashMap();

	static {
		addModuleType("builder", new ModuleType(Builder.class, "Builder", "builders"));
		addModuleType("command", new ModuleType(Command.class, "Command", "commands"));
		addModuleType("discovery", new ModuleType(Discovery.class, "Discovery", "discovery modules"));
		addModuleType("dynstager", new ModuleType(DynStagerHandler.class, "", "dynstagers"));
		addModuleType("injector", new ModuleType(Injector.class, "Injector", "injectors"));
		addModuleType("stage", new ModuleType(StageHandler.class, "", "stages / stage handlers"));
		addModuleType("stager", new ModuleType(StagerHandler.class, "", "stagers / stager handlers"));
	}

	// can be called by plugins to add more module types
	public static void addModuleType(String name, ModuleType type) {
		moduleTypes.put(name, type);
	}

	public ShowCommand() {
		super("Show information about a module",
				"This command can be used to show information about a module,\r\n" +
						"or list modules of a given type.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("TYPE", true, Command.TYPE_MODULETYPE, "Module type to get help of"),
				new Parameter("MODULE", true, Command.TYPE_MODULE_BY_TYPE, "Module to get help of"), };
	}

	public void execute(String[] parameters) throws Exception {
		if (parameters.length == 0) {
			execute(new String[] { "command", "show" });
			consoleOut.println();
			consoleOut.println("Supported module types: ");
			consoleOut.println("\tall  (to show all modules)");
			List keys = new ArrayList(moduleTypes.keySet());
			Collections.sort(keys);
			for (int i = 0; i < keys.size(); i++) {
				consoleOut.println("\t" + keys.get(i));
			}
			return;
		}

		ModuleType type = (ModuleType) moduleTypes.get(parameters[0].toLowerCase());
		if (type == null && parameters.length == 1 && parameters[0].endsWith("s")) {
			type = (ModuleType) moduleTypes.get(parameters[0].toLowerCase().substring(0, parameters[0].length() - 1));
		}
		if (type == null && parameters.length == 1 && parameters[0].equals("all")) {
			List keys = new ArrayList(moduleTypes.keySet());
			Collections.sort(keys);
			for (int i = 0; i < keys.size(); i++) {
				if (i != 0)
					consoleOut.println();
				execute(new String[] { (String) keys.get(i) });
			}
			return;
		}
		if (type == null) {
			consoleOut.println("Unsupported module type: " + parameters[0]);
			return;
		}

		if (parameters.length == 1) {
			consoleOut.println("Supported " + type.getPluralName() + ":");
			Module.list(consoleOut, type.getModuleClass());
		} else {
			Module mod;
			try {
				mod = load(type.getModuleClass(), type.getModuleName(parameters[1]));
			} catch (IllegalArgumentException ex) {
				consoleOut.println("No " + type.getPluralName() + " found: " + parameters[1]);
				return;
			}
			consoleOut.println(mod.getNameAndParameters());
			consoleOut.println();
			consoleOut.println(mod.getSummary());
			consoleOut.println();
			consoleOut.println(mod.getDescription());
			consoleOut.println();
			mod.printParameterDescription(consoleOut);
		}
	}

	public static class ModuleType {
		private final Class moduleClass;
		private final String suffix;
		private final String pluralName;

		public ModuleType(Class moduleClass, String suffix, String pluralName) {
			this.moduleClass = moduleClass;
			this.suffix = suffix;
			this.pluralName = pluralName;
		}

		public Class getModuleClass() {
			return moduleClass;
		}

		public String getSuffix() {
			return suffix;
		}

		public String getPluralName() {
			return pluralName;
		}

		public String getModuleName(String module) {
			if (moduleClass == Command.class)
				return Command.getModuleName(module);
			else
				return module + suffix;
		}
	}
}
