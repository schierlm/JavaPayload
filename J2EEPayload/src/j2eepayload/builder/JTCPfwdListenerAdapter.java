package j2eepayload.builder;

import java.net.Socket;

import jtcpfwd.Module;
import jtcpfwd.listener.Listener;

public class JTCPfwdListenerAdapter {
	
	public static Socket getSocket(String parameter) throws Exception {
		if (parameter.indexOf('@') == -1)
			parameter = "Simple@" + parameter;
		int pos = parameter.indexOf("@");
		Class c = Module.lookup(Listener.class, parameter.substring(0, pos));
		Listener f = (Listener) c.getConstructor(new Class[] { String.class }).newInstance(new Object[] { parameter.substring(pos + 1) });
		final Socket s = f.accept();
		f.dispose();
		return s;
	}
}

