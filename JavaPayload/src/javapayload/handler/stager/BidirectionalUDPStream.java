/*
 * Java Payloads.
 * 
 * Copyright (c) 2010, 2011 Michael 'mihi' Schierl.
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
package javapayload.handler.stager;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class BidirectionalUDPStream extends OutputStream implements Runnable {

	private final OutputStream writeTo;
	private final DatagramSocket socket;
	private final InetAddress remoteHost;
	private final int remotePort;

	private volatile int closeState = 0;

	private final Object swLock = new Object();
	private long swFirstFullSeq = -1, inSeq = 0;

	private final byte[][] sendWindow = new byte[1024][];
	private int swFirstFull = 0, swNextEmpty = 0;
	private int swOut = 0;
	private long lastAckRequestSent = Long.MAX_VALUE;

	public BidirectionalUDPStream(OutputStream writeTo, DatagramSocket socket, InetAddress remoteHost, int remotePort) {
		this.writeTo = writeTo;
		this.socket = socket;
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		new Thread(this).start();
	}

	public void run() {
		try {
			if (swFirstFullSeq == -1) {
				swFirstFullSeq = 0;
				new Thread(this).start();
				runReaderThread();
			} else {
				runWriterThread();
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex.toString());
		}
	}

	public void runWriterThread() throws Exception {
		int wait = 5000;
		while (true) {
			byte[] dgram;
			int idx = swOut - swFirstFull;
			if (idx < 0)
				idx += sendWindow.length;
			long seq = swFirstFullSeq + idx;
			synchronized (swLock) {
				if (swOut != swNextEmpty) {
					dgram = new byte[8 + sendWindow[swOut].length];
					System.arraycopy(sendWindow[swOut], 0, dgram, 8, sendWindow[swOut].length);
					swOut = (swOut + 1) % sendWindow.length;
					lastAckRequestSent = Long.MAX_VALUE;
					wait = 1;
				} else if (swOut != swFirstFull) {
					dgram = new byte[8];
					seq |= 0x4000000000000000L;
					if (lastAckRequestSent == Long.MAX_VALUE) {
						lastAckRequestSent = System.currentTimeMillis();
					} else if (System.currentTimeMillis() - lastAckRequestSent > 60000) {
						close();
						throw new IOException("Packet was not acked");
					}
					if (wait < 5000)
						wait *= 2;
					if (wait < 50)
						wait = 50;
				} else if (closeState == -1) {
					dgram = null;
					wait = 1;
					closeState = 1;
				} else if (closeState == 1) {
					dgram = new byte[8];
					seq = 0x7F00000000000001L;
					if (wait < 5000)
						wait *= 2;
					if (wait < 50)
						wait = 50;
				} else if (closeState == 2) {
					dgram = new byte[8];
					seq = 0x7F00000000000002L;
					wait = 100;
				} else if (closeState >= 3) {
					break;
				} else {
					// queue empty
					dgram = null;
					wait = 5000;
				}
			}
			if (dgram != null) {
				send(seq, dgram);
			}
			synchronized (swLock) {
				swLock.wait(wait);
			}
		}
	}

	private void send(long seq, byte[] dgram) throws IOException {
		dgram[0] = (byte) (seq >> 56);
		dgram[1] = (byte) (seq >> 48);
		dgram[2] = (byte) (seq >> 40);
		dgram[3] = (byte) (seq >> 32);
		dgram[4] = (byte) (seq >> 24);
		dgram[5] = (byte) (seq >> 16);
		dgram[6] = (byte) (seq >> 8);
		dgram[7] = (byte) (seq);
		socket.send(new DatagramPacket(dgram, dgram.length, remoteHost, remotePort));
	}

	public void runReaderThread() throws Exception {
		while (closeState != 4) {
			byte[] buffer = new byte[512];
			DatagramPacket dp = new DatagramPacket(buffer, 512);
			try {
				socket.receive(dp);
			} catch (InterruptedIOException ex) {
				if (closeState != 3)
					throw ex;
				closeState = 4;
				socket.close();
				break;
			}
			if (buffer[0] == -1 && dp.getLength() >= 3) {
				// that was a packet from the first stager,
				// just ack it
				socket.send(new DatagramPacket(buffer, 1, remoteHost, remotePort));
				continue;
			}
			if (dp.getLength() < 8 || dp.getLength() > 508)
				continue;
			int flags = ((buffer[0] & 0xFF) >> 6);
			if (flags == 3)
				continue;
			long seq = ((buffer[0] & 0x3FL) << 56) | ((buffer[1] & 0xFFL) << 48) | ((buffer[2] & 0xFFL) << 40) | (buffer[3] & 0xFFL + 32) | ((buffer[4] & 0xFFL) << 24) | ((buffer[5] & 0xFFL) << 16) | ((buffer[6] & 0xFFL) << 8) | (buffer[7] & 0xFFL);
			switch (flags) {
			case 0: // data
				if (seq == inSeq) {
					inSeq++;
					writeTo.write(buffer, 8, dp.getLength() - 8);
				} else if (seq > inSeq) {
					// send ack so that peer resends missing
					// data
					send(inSeq | 0x8000000000000000L, new byte[8]);
				}
				break;
			case 1: // no new data after, please ack
				if (seq == 0x3F00000000000001L) {
					writeTo.close();
					closeState = 2;
				} else if (seq == 0x3F00000000000002L) {
					writeTo.close();
					socket.setSoTimeout(5000);
					send(0x7F00000000000003L, new byte[8]);
					closeState = 3;
				} else if (seq == 0x3F00000000000003L) {
					closeState = 4;
					socket.close();
				} else if (seq >= inSeq) {
					// send ack so that peer resends missing
					// data if needed
					send(inSeq | 0x8000000000000000L, new byte[8]);
				}
				break;
			case 2: // ack / please resend after
				synchronized (swLock) {
					if (seq < swFirstFullSeq) // old ack
						continue;
					while (swFirstFullSeq < seq) {
						if (swFirstFull == swNextEmpty)
							throw new IOException("Unsent data acked");
						sendWindow[swFirstFull] = null;
						swFirstFull = (swFirstFull + 1) % sendWindow.length;
						swFirstFullSeq++;
					}
					swOut = swFirstFull;
					swLock.notifyAll();
				}
				break;
			}
		}
		synchronized(this) {
			notifyAll();
		}
	}

	public void write(int b) throws IOException {
		write(new byte[] { (byte) b }, 0, 1);
	}

	public synchronized void write(byte[] b, int off, int len) throws IOException {
		if (closeState != 0)
			throw new IOException("Socket is closed");
		if (len == 0)
			return;
		while (len > 500) {
			write(b, off, 500);
			off += 500;
			len -= 500;
		}
		byte[] copy = new byte[len];
		System.arraycopy(b, off, copy, 0, len);
		synchronized (swLock) {
			int swNextEmptyNew = (swNextEmpty + 1) % sendWindow.length;
			try {
				while (swNextEmptyNew == swFirstFull) {
					swLock.wait();
				}
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex.toString());
			}
			sendWindow[swNextEmpty] = copy;
			swNextEmpty = swNextEmptyNew;
			swLock.notifyAll();
		}
	}

	public synchronized void close() throws IOException {
		closeState = -1;
	}
	
	public synchronized void waitFinished() throws InterruptedException {
		while(closeState != 4)
			wait();
	}
}