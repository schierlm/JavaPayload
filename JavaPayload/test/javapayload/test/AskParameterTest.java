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

package javapayload.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javapayload.Module;
import javapayload.NamedElement;
import javapayload.Parameter;
import javapayload.builder.Discovery;
import javapayload.builder.Injector;
import javapayload.cli.Command;
import javapayload.cli.param.ParameterContext;
import javapayload.cli.param.ParameterHandler;
import javapayload.cli.param.ParameterHandler.ParameterFormatException;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.stage.StageHandler;
import javapayload.handler.stager.StagerHandler;

public class AskParameterTest {

	public static void main(String[] args) throws Exception {
		System.out.println("Testing parameter types...");
		testParameterTypes();
		System.out.println("Testing random commands...");
		for (int i = 0; i < 100; i++) {
			testRandomCommand();
		}
		System.out.println("Testing root walk...");
		if (args.length > 0)
			testRootWalkDebug();
		testRootWalk();
		System.out.println("Done.");
	}

	private static void testParameterTypes() throws Exception {
		Class[] allTypes = new Class[] {
				Command.class,
				Discovery.class,
				DynStagerHandler.class,
				Injector.class,
				StageHandler.class,
				StagerHandler.class,
		};
		for (int i = 0; i < allTypes.length; i++) {
			Module[] modules = Module.loadAll(allTypes[i]);
			for (int j = 0; j < modules.length; j++) {
				Parameter[] params = modules[j].getParameters();
				for (int k = 0; k < params.length; k++) {
					if (params[k].getType() == Command.TYPE_MODULE_BY_TYPE)
						continue;
					if (params[k].getType() == Command.TYPE_STAGER_AND_HANDLER)
						continue;
					ParameterHandler.getHandler(ParameterContext.STAGER, params[k]);
				}
			}
		}
	}

	private static void testRandomCommand() throws Exception {
		List remainingHandlers = new ArrayList();
		remainingHandlers.addAll(Arrays.asList(ParameterHandler.getRootHandlers(null)));
		StringBuffer resultBuffer = new StringBuffer();
		Random rnd = new Random();
		while (remainingHandlers.size() > 0) {
			ParameterHandler handler = (ParameterHandler) remainingHandlers.remove(0);
			String[] completions = testHandler(handler);
			String completion = completions[rnd.nextInt(completions.length)];
			ParameterHandler[] next = handler.getNextHandlers(completion, resultBuffer);
			remainingHandlers.addAll(0, Arrays.asList(next));
		}
		System.out.println(resultBuffer.toString());
	}

	private static void testRootWalkDebug() throws Exception {
		testRootWalkRecursive(0, false, ParameterHandler.getRootHandlers(null), "");
	}

	private static void testRootWalkRecursive(int depth, boolean loopyUsed, ParameterHandler[] handlers, String cmdline) throws Exception {
		List remainingHandlers = new ArrayList();
		remainingHandlers.addAll(Arrays.asList(handlers));
		if (depth == 50)
			throw new RuntimeException("Loop detected: " + cmdline);
		if (remainingHandlers.size() > 0) {
			ParameterHandler handler = (ParameterHandler) remainingHandlers.remove(0);
			String[] completions = testHandler(handler);
			for (int i = 0; i < completions.length; i++) {
				boolean newLoopyUsed = loopyUsed;
				if (Arrays.asList(LOOPY_COMMANDS).contains(completions[i])) {
					if (loopyUsed)
						continue;
					newLoopyUsed = true;
				}
				StringBuffer buf = new StringBuffer(cmdline);
				ParameterHandler[] next = handler.getNextHandlers(completions[i], buf);
				List remainingHandlersNew = new ArrayList(Arrays.asList(next));
				remainingHandlersNew.addAll(remainingHandlers);
				testRootWalkRecursive(depth + 1, newLoopyUsed, (ParameterHandler[]) remainingHandlersNew.toArray(new ParameterHandler[remainingHandlersNew.size()]), buf.toString());
			}
		}
	}

	private static String[] LOOPY_COMMANDS = {
			"StageMenu",
			"LocalStageMenu",
			"LaunchStager",
			"SendParameters",
			"AES",
			"GZIP",
			"GZ"
	};

	private static void testRootWalk() throws Exception {
		List remainingHandlers = new ArrayList();
		remainingHandlers.addAll(Arrays.asList(ParameterHandler.getRootHandlers(null)));
		Module[] commands = Module.loadAll(Command.class);
		for (int i = 0; i < commands.length; i++) {
			remainingHandlers.addAll(Arrays.asList(ParameterHandler.getRootHandlers(((Command) commands[i]))));
		}
		StringBuffer tempBuffer = new StringBuffer(1024);
		int[] loopyCounters = new int[LOOPY_COMMANDS.length];
		Arrays.fill(loopyCounters, 5);
		int counter = 0;
		while (remainingHandlers.size() > 0) {
			ParameterHandler handler = (ParameterHandler) remainingHandlers.remove(0);
			String[] completions = testHandler(handler);
			if (completions.length == 0)
				throw new RuntimeException("No possible values for " + handler.getClass());
			for (int i = 0; i < completions.length; i++) {
				int idx = Arrays.asList(LOOPY_COMMANDS).indexOf(completions[i]);
				if (idx != -1) {
					if (loopyCounters[idx] > 0)
						loopyCounters[idx]--;
					else
						continue;
				}
				try {
					ParameterHandler[] next = handler.getNextHandlers(completions[i], tempBuffer);
					checkArrayNotNull(handler, next, "NextHandlers for " + completions[i]);
					if (tempBuffer.length() > 768)
						tempBuffer.setLength(0);
					remainingHandlers.addAll(Arrays.asList(next));
				} catch (ParameterFormatException ex) {
					throw new RuntimeException("Exception while requesting command " + completions[i] + " of " + handler.getClass(), ex);
				}
			}
			if (++counter % 10000 == 0)
				System.out.println(remainingHandlers.size() + "\t" + counter);
		}
	}

	private static String[] testHandler(ParameterHandler handler) {
		checkNotNull(handler, handler.getName(), "Name");
		checkNotNull(handler, handler.getSummary(), "Summary");
		checkNotNull(handler, handler.autoComplete("42"), "AutoComplete for 42");
		List results = new ArrayList();
		if (handler.isOptional())
			results.add("");
		NamedElement[] values = handler.getPossibleValues();
		checkArrayNotNull(handler, values, "Possible values");
		for (int i = 0; i < values.length; i++) {
			checkNotNull(handler, values[i].getSummary(), "Summary " + i + " of possible values");
			checkNotNull(handler, values[i].getName(), "Summary " + i + " of possible values");
			results.add(values[i].getName());
		}
		if (values.length == 0)
			results.add("42");
		int cnt = handler.getRepeatCount();
		if (cnt < 0 || cnt > 2)
			System.out.println("Repeat count for " + handler.getClass() + "is out of bounds: " + cnt);
		if (cnt > 0 && !handler.isOptional())
			throw new RuntimeException("Handler " + handler.getClass() + " is repeated (" + cnt + ")but not optional");
		if (cnt == 2)
		{
			results.clear();
			results.add("");
		}
		String[] result = (String[]) results.toArray(new String[results.size()]);
		for (int i = 0; i < result.length; i++) {
			checkArrayNotNull(handler, handler.autoComplete(result[i]), "AutoComplete for " + result[i]);
			if (handler.autoComplete(result[i]) == null)
				throw new RuntimeException("AutoComplete of " + handler.getClass() + "returned null for " + result[i]);
		}
		return result;
	}

	private static void checkArrayNotNull(ParameterHandler handler, Object[] value, String description) {
		checkNotNull(handler, value, description);
		for (int i = 0; i < value.length; i++) {
			checkNotNull(handler, value[i], description + " element " + i);
		}
	}

	private static void checkNotNull(ParameterHandler handler, Object value, String description) {
		if (value == null)
			throw new RuntimeException(description + " of " + handler.getClass() + " is null");
	}
}
