/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, 2011 Michael 'mihi' Schierl.
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


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.StringTokenizer;

import javapayload.stager.Stager;

public class SpawnTemplate extends Stager {

	public void bootstrap(String[] parameters) throws Exception {
		File tempFile = File.createTempFile("~spawn", ".tmp");
		tempFile.delete();
		File tempDir = new File(tempFile.getAbsolutePath()+".dir");
		tempDir.mkdir();
		tempFile = new File(tempDir, "SpawnedClass.class");
		FileOutputStream fos = new FileOutputStream(tempFile);
		fos.write("TO_BE_REPLACED".getBytes("ISO-8859-1"));
		fos.close();
		if (parameters[0].startsWith("Spawn"))
			parameters[0] = parameters[0].substring(5);
		launch("SpawnedClass", tempDir.getAbsolutePath(), parameters);
		Thread.sleep(1000);
		tempFile.delete();
		tempDir.delete();		
	}

	///
	/// The rest of the file is based on code from Apache Ant 1.8.1
	///
	public static Process launch(String className, String classPath, String[] args) throws IOException {
		return Runtime.getRuntime().exec(buildCommandLine(className, classPath, args));
	}

	private static String[] buildCommandLine(String className, String classPath, String[] args) {
		List commands = new ArrayList();
		String vm = getJreExecutable("java");
	    commands.add(vm);
		if (classPath.length() > 0) {
		    commands.add("-classpath");
		    commands.add(classPath);
		}
		commands.add(className);
		commands.addAll(Arrays.asList(args));
		return (String[]) commands.toArray(new String[commands.size()]);
	}
	
    public static final String FAMILY_NETWARE = "netware";
    public static final String FAMILY_DOS = "dos";

    public static boolean isOs(String family, String name) {
        boolean retValue = false;

        if (family != null || name != null) {

            boolean isFamily = true;
            boolean isName = true;

            if (family != null) {

                //windows probing logic relies on the word 'windows' in
                //the OS
                if (family.equals(FAMILY_NETWARE)) {
                    isFamily = OS_NAME.indexOf(FAMILY_NETWARE) > -1;
                } else if (family.equals(FAMILY_DOS)) {
                    isFamily = PATH_SEP.equals(";") && !isOs(FAMILY_NETWARE, null);
                } else {
                    throw new RuntimeException(
                        "Don\'t know how to detect os family \""
                        + family + "\"");
                }
            }
            if (name != null) {
                isName = name.equals(OS_NAME);
            }
            retValue = isFamily && isName;
        }
        return retValue;
    }

    private static final String OS_NAME =
        System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String PATH_SEP =
        System.getProperty("path.separator");
    
    private static final boolean IS_DOS = isOs(FAMILY_DOS, null);
    private static final boolean IS_NETWARE = isOs(null, FAMILY_NETWARE);
    private static final boolean IS_AIX = isOs(null, "aix");

    private static final String JAVA_HOME = System.getProperty("java.home");

    public static String getJreExecutable(String command) {
        if (IS_NETWARE) {
            // Extrapolating from:
            // "NetWare may have a "java" in that directory, but 99% of
            // the time, you don't want to execute it" -- Jeff Tulley
            // <JTULLEY@novell.com>
            return command;
        }

        File jExecutable = null;

        if (IS_AIX) {
            // On IBM's JDK 1.2 the directory layout is different, 1.3 follows
            // Sun's layout.
            jExecutable = findInDir(JAVA_HOME + "/sh", command);
        }

        if (jExecutable == null) {
            jExecutable = findInDir(JAVA_HOME + "/bin", command);
        }

        if (jExecutable != null) {
            return jExecutable.getAbsolutePath();
        } else {
            // Unfortunately on Windows java.home doesn't always refer
            // to the correct location, so we need to fall back to
            // assuming java is somewhere on the PATH.
            return addExtension(command);
        }
    }

    private static String addExtension(String command) {
        // This is the most common extension case - exe for windows and OS/2,
        // nothing for *nix.
        return command + (IS_DOS ? ".exe" : "");
    }

    private static File findInDir(String dirName, String commandName) {
        File dir = normalize(dirName);
        File executable = null;
        if (dir.exists()) {
            executable = new File(dir, addExtension(commandName));
            if (!executable.exists()) {
                executable = null;
            }
        }
        return executable;
    }

    private static File normalize(final String path) {
        Stack s = new Stack();
        String[] dissect = dissect(path);
        s.push(dissect[0]);

        StringTokenizer tok = new StringTokenizer(dissect[1], File.separator);
        while (tok.hasMoreTokens()) {
            String thisToken = tok.nextToken();
            if (".".equals(thisToken)) {
                continue;
            }
            if ("..".equals(thisToken)) {
                if (s.size() < 2) {
                    // Cannot resolve it, so skip it.
                    return new File(path);
                }
                s.pop();
            } else { // plain component
                s.push(thisToken);
            }
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.size(); i++) {
            if (i > 1) {
                // not before the filesystem root and not after it, since root
                // already contains one
                sb.append(File.separatorChar);
            }
            sb.append(s.elementAt(i));
        }
        return new File(sb.toString());
    }
    
    private static String[] dissect(String path) {
        char sep = File.separatorChar;
        path = path.replace('/', sep).replace('\\', sep);

        // make sure we are dealing with an absolute path
        String root = null;
        int colon = path.indexOf(':');
        if (colon > 0 && (IS_DOS || IS_NETWARE)) {

            int next = colon + 1;
            root = path.substring(0, next);
            char[] ca = path.toCharArray();
            root += sep;
            //remove the initial separator; the root has it.
            next = (ca[next] == sep) ? next + 1 : next;

            StringBuffer sbPath = new StringBuffer();
            // Eliminate consecutive slashes after the drive spec:
            for (int i = next; i < ca.length; i++) {
                if (ca[i] != sep || ca[i - 1] != sep) {
                    sbPath.append(ca[i]);
                }
            }
            path = sbPath.toString();
        } else if (path.length() > 1 && path.charAt(1) == sep) {
            // UNC drive
            int nextsep = path.indexOf(sep, 2);
            nextsep = path.indexOf(sep, nextsep + 1);
            root = (nextsep > 2) ? path.substring(0, nextsep + 1) : path;
            path = path.substring(root.length());
        } else {
            root = File.separator;
            path = path.substring(1);
        }
        return new String[] {root, path};
    }
}
