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

package javapayload;

import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class Module implements NamedElement {

	private static final Map modulesCache = new HashMap();

	public static Module[] loadAll(Class moduleType) throws Exception {
		Module[] resultArray = (Module[]) modulesCache.get(moduleType.getName());
		if (resultArray == null) {
			String[] packageNames = getPackageNames(moduleType);
			List resultClasses = new ArrayList();
			URL[] urls = new URL[0];
			ClassLoader ucl = Module.class.getClassLoader();
			if (ucl instanceof URLClassLoader)
				urls = ((URLClassLoader) ucl).getURLs();
			for (int i = 0; i < urls.length; i++) {
				if (urls[i].getFile().endsWith(".jar")) {
					ZipFile jarfile = new ZipFile(urlToFile(urls[i]));
					Enumeration files = jarfile.entries();
					while (files.hasMoreElements()) {
						ZipEntry ze = (ZipEntry) files.nextElement();
						if (!ze.isDirectory()) {
							String name = ze.getName().replace('/', '.');
							for (int j = 0; j < packageNames.length; j++) {
								if (name.startsWith(packageNames[j]) && name.endsWith(".class")) {
									try {
										resultClasses.add(Class.forName(name.substring(0, name.length() - 6)));
									} catch (NoClassDefFoundError ex) {
										// referenced classes from tools.jar
									}
								}
							}
						}
					}
					jarfile.close();
				} else {
					for (int j = 0; j < packageNames.length; j++) {
						String[] files = urlToFile(new URL(urls[i], packageNames[j].replace('.', '/'))).list();
						if (files != null) {
							for (int k = 0; k < files.length; k++) {
								if (files[k].endsWith(".class"))
									resultClasses.add(Class.forName(packageNames[j] + "." + files[k].substring(0, files[k].length() - 6)));
							}
						}
					}
				}
			}
			List result = new ArrayList();
			for (int i = 0; i < resultClasses.size(); i++) {
				Class clazz = (Class) resultClasses.get(i);
				if (moduleType.isAssignableFrom(clazz)) {
					try {
						result.add(clazz.getConstructor(new Class[0]).newInstance(new Object[0]));
					} catch (NoSuchMethodException ex) {
						// ignore
					}
				}
			}
			Collections.sort(result, new Comparator() {
				public int compare(Object arg0, Object arg1) {
					return ((Module) arg0).getName().compareTo(((Module) arg1).getName());
				}
			});
			resultArray = (Module[]) result.toArray(new Module[result.size()]);
			modulesCache.put(moduleType.getName(), resultArray);
		}
		return resultArray;
	}

	private static String[] getPackageNames(Class moduleType) {
		String[] packageNames = new String[] { moduleType.getPackage().getName() };
		if (packageNames[0].equals("javapayload.builder")) {
			packageNames = new String[] {
					"javapayload.builder",
					"javapayload.exploit"
			};
		}
		return packageNames;
	}

	public static Module load(Class moduleType, String name) throws Exception {
		String[] packageNames = getPackageNames(moduleType);
		for (int i = 0; i < packageNames.length; i++) {
			try {
				return (Module) Class.forName(packageNames[i] + "." + name).newInstance();
			} catch (ClassNotFoundException ex) {
				// ignore
			}
		}
		throw new IllegalArgumentException("Module " + name + " not found");
	}

	protected static void printList(PrintStream out, NamedElement[] elements) {
		int maxNameLength = 0;
		for (int i = 0; i < elements.length; i++) {
			if (elements[i].getName().length() > maxNameLength)
				maxNameLength = elements[i].getName().length();
		}
		for (int i = 0; i < elements.length; i++) {
			char[] spaces = new char[maxNameLength - elements[i].getName().length() + 2];
			Arrays.fill(spaces, ' ');
			out.println(elements[i].getName() + new String(spaces) + elements[i].getSummary());
		}
	}

	public static void list(PrintStream out, Class moduleType) throws Exception {
		printList(out, loadAll(moduleType));
	}
	
	// utility methods for older JDK versions
	
	public static File urlToFile(URL fileURL) throws Exception {
		/* #JDK1.4 */try {
			return new File(new java.net.URI(fileURL.toString()));
		} catch (NoClassDefFoundError ex) /**/{
			return new File(fileURL.getFile());
		}
	}
	
	public static String replaceString(String base, String original, String replacement) {
		/* #JDK1.5 */try {
			// note that we cannot use String#replace here since its method signature uses the
			// interface CharSequence which does not exist in earlier Java versions.
			return base.replaceAll(java.util.regex.Pattern.quote(original), java.util.regex.Matcher.quoteReplacement(replacement));
		} catch (NoClassDefFoundError ex) /**/{
			StringBuffer result = new StringBuffer();
			int pos;
			while ((pos = base.indexOf(original)) != -1) {
				result.append(base.substring(0, pos)).append(replacement);
				base = base.substring(pos + original.length());
			}
			result.append(base);
			return result.toString();
		}
	}

	private final String summary;
	private final String description;
	private final Class moduleType;
	private final String name;

	public Module(String nameSuffix, Class moduleType, String summary, String description) {
		String moduleName = getClass().getName();
		if (moduleName.lastIndexOf('.') != 0)
			moduleName = moduleName.substring(moduleName.lastIndexOf('.') + 1);
		if (nameSuffix != null) {
			if (!moduleName.endsWith(nameSuffix))
				throw new IllegalArgumentException(moduleName + " does not end with " + nameSuffix);
			moduleName = moduleName.substring(0, moduleName.length() - nameSuffix.length());
		}
		this.name = moduleName;
		this.moduleType = moduleType;
		this.summary = summary;
		this.description = description;
	}

	public Class getModuleType() {
		return moduleType;
	}

	public String getName() {
		return name;
	}

	public String getSummary() {
		return summary;
	}

	public String getDescription() {
		return description;
	}

	public void printParameterDescription(PrintStream out) {
		if (getParameters().length == 0)
			out.println("No parameters.");
		else
			out.println("Parameters:");
		printList(out, getParameters());
	}
	
	public String getNameAndParameters() {
		Parameter[] params = getParameters();
		StringBuffer commandParams = new StringBuffer();
		for (int i = 0; i < params.length; i++) {
			if (params[i].isOptional())
				commandParams.append(" [<").append(params[i].getName()).append(">]");
			else
				commandParams.append(" <").append(params[i].getName()).append(">");
		}
		return getName() + commandParams.toString();
	}

	public abstract Parameter[] getParameters();
}
