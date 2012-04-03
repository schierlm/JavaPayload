/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, Michael 'mihi' Schierl
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
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Random;

public class TestStub implements Stage, Runnable {
	public void start(DataInputStream in, OutputStream out, String[] parameters) throws Exception {
		int bufsize = 4096;
		if (parameters[parameters.length-1].equals("Fast")) {
			bufsize = 50;
		} else {
			byte[] concurrentRead = new byte[4096];
			in.readFully(concurrentRead);
			if (!checkConcurrentRead(concurrentRead))
				throw new RuntimeException("Concurrent read returned wrong result");
			final byte[] concurrentWrite = new byte[128];
			for (int i = 0; i < concurrentWrite.length; i++) {
				concurrentWrite[i] = (byte)i;
			}
			Thread[] threads = new Thread[32];
			for (int i = 0; i < threads.length; i++) {
				threads[i] = new WriterThread(out, concurrentWrite);
				threads[i].start();
			}
			for (int i = 0; i < threads.length; i++) {
				threads[i].join();
			}
			System.out.println("\t\t\t\t\t(System.out)");
			System.err.println("\t\t\t\t\t(System.err)");
		}
		byte[] outdata = new byte[bufsize];
		Random r = new Random();
		r.nextBytes(outdata);
		out.write(outdata);
		out.flush();
		byte[] indata = new byte[bufsize];
		in.readFully(indata);
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] indigest = digest.digest(indata);
		digest.reset();
		byte[] outdigest = digest.digest(outdata);
		out.write(indigest);
		out.flush();
		byte[] outdigest2 = new byte[outdigest.length];
		in.readFully(outdigest2);
		if (!Arrays.equals(outdigest, outdigest2))
			throw new RuntimeException("Digests do not match");
		if(in.readBoolean()) {
			new Thread(this).start();
		}
		out.close();
	}
	
	public static boolean checkConcurrentRead(byte[] concurrentRead) {
		int[] counts = new int[128];
		for (int i = 0; i < concurrentRead.length; i++) {
			if (concurrentRead[i] < 0 || concurrentRead[i] > 127)
				return false;
			counts[concurrentRead[i]]++;
		}
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] != 32)
				return false;
		}
		return true;
	}

	public void run() {
		try {
			Thread.sleep(100);
			System.exit(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
