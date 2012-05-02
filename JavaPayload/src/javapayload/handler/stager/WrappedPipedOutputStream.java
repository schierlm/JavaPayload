package javapayload.handler.stager;

import java.io.*;
/**
 * Wrapper around {@link PipedOutputStream} that sends all
 * data from a dedicated thread, to avoid "Write end dead" exceptions.
 */
public class WrappedPipedOutputStream extends OutputStream implements Runnable {
	private final PipedOutputStream wrapped;
	private final WrappedPipedOutputStream extraClose;
	
	private boolean writePending = false, threadDead = false;
	private byte[] data;
	private int offsetOrArg; 
	private int length;
	private boolean closed = false, implicitClose = false;

	public WrappedPipedOutputStream(PipedOutputStream wrapped) {
		this(wrapped, null);
	}
	public WrappedPipedOutputStream(PipedOutputStream wrapped, WrappedPipedOutputStream extraClose) {
		this.wrapped = wrapped;
		this.extraClose = extraClose;
		new Thread(this).start();
	}
	
	public synchronized void write(int b) throws IOException {
		try {
			if (closed && implicitClose) 
				return;
			if (closed)
				throw new IOException("Stream is closed");
			while (writePending) 
				wait();
			if (threadDead)
				throw new IOException("Writer thread dead");
			data = null;
			offsetOrArg = b;
			length = 1;
			writePending = true;
			notifyAll();
			while (writePending) 
				wait();
		} catch (InterruptedException ex) {
			throwWrapped(ex);
		}
	}
	
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		try {
			if (closed && implicitClose) 
				return;
			if (closed)
				throw new IOException("Stream is closed");
			if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0))
				throw new IndexOutOfBoundsException();
			while (writePending) 
				wait();
			if (threadDead)
				throw new IOException("Writer thread dead");
			data = b;
			offsetOrArg = off;
			length = len;
			writePending = true;
			notifyAll();
			while (writePending) 
				wait();
		} catch (InterruptedException ex) {
			throwWrapped(ex);
		}
	}
	
	public synchronized void flush() throws IOException {
		try {
			if (closed)
				throw new IOException("Stream is closed");
			while (writePending) 
				wait();
			if (threadDead)
				throw new IOException("Writer thread dead");
			data = null;
			length = 0;
			writePending = true;
			notifyAll();
			while (writePending) 
				wait();
		} catch (InterruptedException ex) {
			throwWrapped(ex);
		}
	}
	
	public synchronized void close() throws IOException {
		try {
			while (writePending) 
				wait();
			if (closed && implicitClose) {
				implicitClose = false;
				return;
			}
			if (closed)
				throw new IOException("Stream is closed");
			if (threadDead)
				throw new IOException("Writer thread dead");
			data = null;
			length = -1;
			writePending = true;
			notifyAll();
			while(writePending)
				wait();
		} catch (InterruptedException ex) {
			throwWrapped(ex);
		}
	}
	
	public synchronized void run() {
		try {
			while (true) {
				while (!writePending)
					wait();
				if (data == null) {
					if (length == 1) {
						wrapped.write(offsetOrArg);
					} else if (length == 0) {
						wrapped.flush();
					} else if (length == -1) {
						wrapped.close();
						if (extraClose != null) {
							synchronized(extraClose) {
								extraClose.implicitClose = true;
								extraClose.close();
							}
						}
						closed = true;
						break;
					}
				} else {
					wrapped.write(data, offsetOrArg, length);
				}
				writePending = false;
				notifyAll();
			}
		} catch (Exception ex) {
			throwWrapped(ex);
		} finally {
			threadDead = true;
			writePending = false;
			notifyAll();
		}
	}
	
	private static void throwWrapped(Throwable ex) {
		/* #JDK1.4 */try {
			throw new RuntimeException(ex);
		} catch (NoSuchMethodError ex2) /**/{
			ex.printStackTrace();
			throw new RuntimeException(ex.toString());
		}
	}
}
