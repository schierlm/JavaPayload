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
package jpmsfbridge.stager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.Properties;

import javapayload.loader.DynLoader;
import javapayload.stage.StreamForwarder;
import javapayload.stager.Stager;
import jpmsfbridge.Logger;

public class StagerEncoder {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("This program is called internally from JpMsfBridge.");
			return;
		}
		try {
			Logger.startLogging(false);
			Properties props = new Properties();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				if (i != 0)
					sb.append(" ");
				sb.append(args[i]);
			}
			props.setProperty("StageParameters", sb.toString());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			Class[] classes = new Class[] {
					Stager.class,
					DynLoader.loadStager(args[0], args, 0),
					StagerLoader.class
			};
			for (int i = 0; i < classes.length; i++) {
				final InputStream classStream = classes[i].getResourceAsStream("/" + classes[i].getName().replace('.', '/') + ".class");
				final ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
				StreamForwarder.forward(classStream, baos2);
				final byte[] clazz = baos2.toByteArray();
				dos.writeInt(clazz.length);
				dos.write(clazz);
			}
			dos.writeInt(0);
			dos.flush();
			props.setProperty("URL", "raw:" + new String(baos.toByteArray(), "ISO-8859-1"));
			props.store(System.out, "payload.dat");
		} catch (Throwable ex) {
			ex.printStackTrace();
		} finally {
			Logger.stopLogging(args);
		}
	}
}