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
package jpmsfbridge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class TemplateTransformer {
	
	private final File templateDir;
	private final File outputDir;

	public TemplateTransformer(File templateDir, File outputDir) throws IOException {
		this.templateDir = templateDir;
		this.outputDir = outputDir;
		Velocity.init();
	}
	
	public void transformDirectory(String templateClass, String dirName, ModuleInfo info) throws Exception {
		File[] files = new File(templateDir, templateClass+dirName).listFiles();
		for (int i = 0; i < files.length; i++) {
			String name = dirName+"/"+files[i].getName();
			if (files[i].isDirectory()) {
				if (files[i].getName().equalsIgnoreCase(".svn"))
					continue;
				transformDirectory(templateClass, name, info);
			} else {
				transformFile(templateClass, name, info);
			}
		}
	}
	
	public void transformFile(String templateClass, String templateName, ModuleInfo info) throws Exception {
		VelocityContext context = new VelocityContext();
		context.put( "globals", Globals.instance);
		context.put("info", info);
		File inputfile = new File(templateDir, templateClass + templateName);
		String outputName = templateName;
		StringBuffer sb = new StringBuffer();
		List/*<String>*/ literalLines = new ArrayList();
		BufferedReader br = new BufferedReader(new FileReader(inputfile));
		String line = br.readLine();
		if (line != null && line.startsWith("$$output")) {
			StringWriter sw = new StringWriter();
			Velocity.evaluate(context, sw, templateName+"$$output", line.substring(9).trim());
			outputName = outputName.substring(0, outputName.lastIndexOf('/')+1)+sw.toString();
			line = br.readLine();
		}
		while (line != null) {
			if (line.startsWith("$$")) {
				sb.append(line.substring(2));
			} else {
				sb.append(":line:"+literalLines.size());
				literalLines.add(line);
			}
			sb.append("\n");
			line = br.readLine();
		}
		br.close();
		File outputFile = new File(outputDir, outputName);
		outputFile.getParentFile().mkdirs();
		StringWriter sw = new StringWriter();
		Velocity.evaluate(context, sw, templateName, sb.toString());
		br = new BufferedReader(new StringReader(sw.toString()));
		line = br.readLine();
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		while (line != null) {
			if (line.startsWith(":line:")) {
				bw.write((String)literalLines.get(Integer.parseInt(line.substring(6))));
			} else {
				bw.write(line);
			}
			line = br.readLine();
			bw.write('\n');
		}
		br.close();
		bw.close();
	}
}
