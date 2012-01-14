/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, 2011, 2012 Michael 'mihi' Schierl
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javapayload.builder.EmbeddedClassBuilder.EmbeddedClassBuilderTemplate;

public class TemplateBuilder extends Builder {
	
	public TemplateBuilder() {
		super("Build a text or zip based file from a template", 
				"This builder is used to build a text or ZIP based file (like a script or\r\n" +
				"OpenOffice document) from a template file. The variable ${[BASE64PAYLOAD]} is\r\n" +
				"replaced by an BASE64 encoded Java class created by EmbeddedClassBuilder. The\r\n" +
				"alternative form ${[BASE64PAYLOAD|<separator>|]} can be used to cut the data\r\n" +
				"in line-sized chunks. Replace <separator> by the separator characters in this\r\n" +
				"case. All other content in the template is preserved as is.");
	}
	
	public int getMinParameterCount() {
		return 5;
	}
	
	public String getParameterSyntax() {
		return "<template> <outputfile|-> <stager> [stageroptions] -- <stage> [stageoptions]";
	}
	
	public void build(String[] args) throws Exception {
		String template = args[0];
		if (template.indexOf('/') == -1 && template.indexOf('\\') == -1 && new File("templates",template).exists()) {
			template = new File("templates",template).getPath();
		}
		String outfile = args[1];
		String[] builderArgs = new String[args.length-1];
		System.arraycopy(args, 2, builderArgs, 1, args.length - 2);
		builderArgs[0] = "Tmp";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream in = new ByteArrayInputStream(ClassBuilder.buildClassBytes(builderArgs[0], builderArgs[1], EmbeddedClassBuilderTemplate.class, EmbeddedClassBuilder.buildEmbeddedArgs(builderArgs), builderArgs));
		new sun.misc.BASE64Encoder().encode(in, baos);
		in.close();
		String payload = new String(baos.toByteArray(), "ISO-8859-1");
		payload = replaceString(payload,"\r\n", "\n");
		payload = payload.replace('\r','\n');
		OutputStream outStream;
		if (outfile.equals("-"))
			outStream = System.out;
		else
			outStream = new FileOutputStream(outfile);
		if (isZipFile(template)) {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(template));
			ZipOutputStream zos = new ZipOutputStream(outStream);
			ZipEntry ze;
			while((ze = zis.getNextEntry()) != null) {
				zos.putNextEntry(ze);
				applyTemplate(payload, zis, zos);
				zos.closeEntry();
			}
			zis.close();
			zos.close();
		} else {
			FileInputStream fis = new FileInputStream(template);
			applyTemplate(payload, fis, outStream);
			fis.close();
			if (outStream != System.out)
				outStream.close();
		}
	}

	private boolean isZipFile(String template) throws IOException {
		ZipInputStream zis = new ZipInputStream(new FileInputStream(template));
		try {
			ZipEntry ze = zis.getNextEntry();
			if (ze == null) return false;
			while (ze != null) {
				ze = zis.getNextEntry();
			}
			return true;
		} catch (IOException ex) {
			return false;
		} finally {
			zis.close();
		}
	}
	
	private void applyTemplate(String payload, InputStream in, OutputStream out) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final byte[] buf = new byte[4096];
		int length;
		while ((length = in.read(buf)) != -1) {
			if (baos != null) {
				baos.write(buf, 0, length);
			}
		}
		String rawData = new String(baos.toByteArray(), "ISO-8859-1");
		if (rawData.indexOf("${[BASE64PAYLOAD]}") != -1) {
			rawData = replaceString(rawData, "${[BASE64PAYLOAD]}", replaceString(payload, "\n", ""));
		}
		while (rawData.indexOf("${[BASE64PAYLOAD|") != -1) {
			int pos = rawData.indexOf("${[BASE64PAYLOAD|");
			int pos2 = rawData.indexOf("|]}", pos + 17);
			if (pos == -1 || pos2 == -1)
				break;
			rawData = rawData.substring(0, pos) + replaceString(payload, "\n", rawData.substring(pos + 17, pos2)) + rawData.substring(pos2 + 3);
		}
		out.write(rawData.getBytes("ISO-8859-1"));
	}
}