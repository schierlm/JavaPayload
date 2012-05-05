/*
 * JpMsfBridge.
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
package jpmsfbridge;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javapayload.Module;
import javapayload.builder.Injector;
import javapayload.handler.dynstager.DynStagerHandler;
import javapayload.handler.stage.StageHandler;
import javapayload.handler.stager.StagerHandler;

public class Generator {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println("Usage: java jpmfsbridge.Generator <outDir> <javaExecutable>");
			return;
		}
		File outDir = new File(args[0]).getAbsoluteFile();
		if (!outDir.exists() || !outDir.isDirectory()) {
			System.out.println("output dir must be an existing directory");
			return;
		}
		File javaExecutable = new File(args[1]).getAbsoluteFile();
		if (!javaExecutable.exists() || !javaExecutable.isFile()) {
			System.out.println("Java executable must be an existing executable file (like java.exe)");
			return;
		}
		Globals.instance.setJavaExecutable(javaExecutable.getPath());
		Module[] dynstagers = Module.loadAll(DynStagerHandler.class);
		List/*<DynStagerHandler>*/ remainingDynstagers = new ArrayList();
		for (int i = 0; i < dynstagers.length; i++) {
			DynStagerHandler d = (DynStagerHandler) dynstagers[i];
			if ((d.getExtraArg() == null ? 0 : 1 ) + d.getParameters().length > 1) 
				continue;
			remainingDynstagers.add(d);
		}
		List/*<DynStagerHandler>*/ dynstagerList = new ArrayList();
		while(remainingDynstagers.size() > 0) {
			boolean changed = false;
			for (int i = 0; i < remainingDynstagers.size(); i++) {
				DynStagerHandler d1 = (DynStagerHandler) remainingDynstagers.get(i);
				boolean mustWait = false;
				for (int j = 0; j < remainingDynstagers.size(); j++) {
					DynStagerHandler d2 = (DynStagerHandler) remainingDynstagers.get(j);
					if (d2.isDynstagerUsableWith(new DynStagerHandler[] {d1}) && !d1.isDynstagerUsableWith(new DynStagerHandler[] {d2})) {
						mustWait = true;
						break;
					}
				}
				if (!mustWait) {
					changed = true;
					dynstagerList.add(d1);
					remainingDynstagers.remove(i);
					i--;
				}
			}
			if (!changed)
				throw new RuntimeException("Dynstagers cannot be created due to circular dependencies between "+remainingDynstagers);
		}
		Globals.instance.setDynstagers((DynStagerHandler[]) dynstagerList.toArray(new DynStagerHandler[dynstagerList.size()]));
		TemplateTransformer transformer = new TemplateTransformer(new File("template").getAbsoluteFile(), outDir);
		System.out.println("Parsing global templates...");
		transformer.transformDirectory("global/", ".", null);
		Module[] stages = Module.loadAll(StageHandler.class);
		for (int i = 0; i < stages.length; i++) {
			StageHandler stage = (StageHandler) stages[i];
			if (stage.isHandlerUsable() && stage.isTargetUsable()) {
				System.out.println("Parsing templates for stage " + stage.getName() + "...");
				transformer.transformDirectory("stage/", ".", new ModuleInfo(stage));
			}
		}
		Class[] extraArgClasses = findExtraArgClasses();
		Module[] stagers = Module.loadAll(StagerHandler.class);
		Method canHandleExtraArg = StagerHandler.class.getDeclaredMethod("canHandleExtraArg", new Class[] { Class.class });
		canHandleExtraArg.setAccessible(true);
		Method needHandleBeforeStart = StagerHandler.class.getDeclaredMethod("needHandleBeforeStart", new Class[0]);
		needHandleBeforeStart.setAccessible(true);
		for (int i = 0; i < stagers.length; i++) {
			StagerHandler stager = (StagerHandler) stagers[i];
			if (!stager.isHandlerUsable() || !stager.isTargetUsable())
				continue;
			if (!stager.isStagerUsableWith(new DynStagerHandler[0]))
				continue;
			String generatedConnectionType = null;
			for (int j = 0; j < extraArgClasses.length; j++) {
				if (((Boolean) canHandleExtraArg.invoke(stager, new Object[] { extraArgClasses[j] })).booleanValue()) {
					String connectionType;
					if (extraArgClasses[j] == null) {
						connectionType = ((Boolean) needHandleBeforeStart.invoke(stager, new Object[0])).booleanValue() ? "reverse" : "bind";
					} else {
						connectionType = "javapayload_" + extraArgClasses[j].getName().replace('.', '_').toLowerCase();
					}
					if (generatedConnectionType == null) {
						System.out.println("Parsing templates for stager " + stager.getName() + " (connection type " + connectionType + ")...");
						transformer.transformDirectory("stager/", ".", new StagerInfo(stager, connectionType));
						generatedConnectionType = connectionType;
					} else if (!generatedConnectionType.equals(connectionType)) {
						System.out.println("Warning: " + stager.getName() + " supports both " + generatedConnectionType + " and " + connectionType + ", using the former.");
					}
				}
			}
		}
		System.out.println("Done.");
		Logger.logEntry("Templates regenerated.\r\nJava exectutable: " + Globals.instance.getJavaExecutable() + "\r\nClassPath: " + Globals.instance.getClassPath());
	}

	private static Class[] findExtraArgClasses() throws Exception {
		List/* <Class> */result = new ArrayList();
		Module[] injectors = Module.loadAll(Injector.class);
		for (int i = 0; i < injectors.length; i++) {
			Class[] classes = ((Injector) injectors[i]).getSupportedExtraArgClasses();
			for (int j = 0; j < classes.length; j++) {
				if (!result.contains(classes[j]))
					result.add(classes[j]);
			}
		}
		return (Class[]) result.toArray(new Class[result.size()]);
	}
}
