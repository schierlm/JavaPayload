package j2eepayload.builder;

import java.net.Socket;

import jtcpfwd.Module;
import jtcpfwd.forwarder.Forwarder;

public class JTCPfwdForwarderAdapter {
	
	public static Socket getSocket(String parameter) throws Exception {
		if (parameter.indexOf('@') == -1)
			parameter = "Simple@" + parameter;
		int pos = parameter.indexOf("@");
		Class c = Module.lookup(Forwarder.class, parameter.substring(0, pos));
		Forwarder f = (Forwarder) c.getConstructor(new Class[] { String.class }).newInstance(new Object[] { parameter.substring(pos + 1) });
		final Socket s = f.connect(null);
		f.dispose();
		return s;
	}
}
