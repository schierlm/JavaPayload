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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javapayload.Module;
import javapayload.NamedElement;
import javapayload.Parameter;
import javapayload.cli.param.ParameterHandler;
import javapayload.cli.param.ParameterHandler.ParameterFormatException;
import javapayload.stage.StageMenu;

public class AskCommand extends Command {

	public AskCommand() {
		super("Interactively build a command line for a command",
				"This command can be used to build a command line for another interactively,\r\n" +
				"just by answering questions.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("COMMAND", true, Command.TYPE_COMMAND, "Command to build parameters of")
		};
	}

	public void execute(String[] parameters) throws Exception {
		Command cmd = null;
		StringBuffer commandBuffer = new StringBuffer();
		if (parameters.length == 1) {
			try {
				cmd = (Command) Module.load(Command.class, Command.getModuleName(parameters[0]));
				commandBuffer.append(parameters[0]).append(' ');
			} catch (IllegalArgumentException ex) {
				consoleOut.println("Command not found: " + parameters[0]);
				return;
			}
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(consoleIn));
		List /* <ParameterHandler> */remainingHandlers = new ArrayList();
		try {
			remainingHandlers.addAll(Arrays.asList(ParameterHandler.getRootHandlers(cmd)));
		} catch (ParameterFormatException ex) {
			System.out.println("Error: " + ex.getMessage());
			return;
		}
		consoleOut.println();
		consoleOut.println("Press [Tab][Return] for more options.");
		consoleOut.println();
		while (remainingHandlers.size() > 0) {
			ParameterHandler handler = (ParameterHandler) remainingHandlers.get(0);
			consoleOut.print(handler.getName() + ": ");
			String line = br.readLine();
			if (line == null || line.equalsIgnoreCase("\tq"))
				return;
			if (line.equals("\t?")) {
				consoleOut.println();
				consoleOut.println(handler.getName() + " - " + handler.getSummary());
				consoleOut.println("Optional: " + (handler.isOptional() ? "yes" : "no"));
				consoleOut.println();
				NamedElement[] elems = handler.getPossibleValues();
				if (elems != null && elems.length > 0) {
					consoleOut.println("Possible values: ");
					printList(consoleOut, elems);
					consoleOut.println();
				}
				continue;
			}
			if (line.equalsIgnoreCase("\tv")) {
				consoleOut.println("Command line: [" + commandBuffer.toString() + "]");
				continue;
			}
			if (line.length() > 1 && line.endsWith("\t")) {
				String[] completions = handler.autoComplete(line.substring(0, line.length() - 1));
				if (completions.length == 0) {
					consoleOut.println("No completions");
					continue;
				} else if (completions.length == 1) {
					consoleOut.println("Using completion: " + completions[0]);
					line = completions[0];
				} else {
					consoleOut.println();
					consoleOut.println("Completions:");
					for (int i = 0; i < completions.length; i++) {
						consoleOut.println("\t" + completions[i]);
					}
					consoleOut.println();
				}
			} else if (line.indexOf("\t") != -1) {
				consoleOut.println();
				consoleOut.println("Enter a value for the given parameter, or enter one of these:");
				consoleOut.println("    [Tab]Q - quit from this command without executing anything");
				consoleOut.println("    [Tab]V - view current command line");
				consoleOut.println("    [Tab]? - Show help for this parameter");
				consoleOut.println("    [Tab]H - Show this help");
				consoleOut.println("    value[Tab] - Tab complete value and show matches, or take if unique");
				consoleOut.println();
				continue;
			}
			try {
				ParameterHandler[] nextHandlers = handler.getNextHandlers(line, commandBuffer);
				remainingHandlers.remove(0);
				remainingHandlers.addAll(0, Arrays.asList(nextHandlers));
			} catch (ParameterFormatException ex) {
				System.out.println("Error: " + ex.getMessage());
			}
		}
		String command = commandBuffer.toString().trim();
		while (command.indexOf("  ") != -1)
			command = replaceString(command, "  ", " ");
		consoleOut.println("(JP)> " + command);
		String[] args = StageMenu.splitArgs(command);
		mainClass.runCommand(args);
	}
}