package today.vanta.util.system.prot;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class Crasher {

    public void hang() {
        while (true) {
            // Infinite loop
        }
    }

    public void crash() {
        while (true) {
            try {
                Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                Unsafe unsafe = (Unsafe) unsafeField.get(null);
                long address = 0L;
                while (true) {
                    unsafe.setMemory(address, Long.MAX_VALUE, (byte) -128);
                    ++address;
                }
            } catch (Throwable t) {
                this.crash();
                this.hang();
                continue;
            }
        }
    }

}
