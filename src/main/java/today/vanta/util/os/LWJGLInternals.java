package today.vanta.util.os;

import org.jetbrains.annotations.NotNullByDefault;
import org.lwjgl.opengl.Display;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@NotNullByDefault
public final class LWJGLInternals {
    public static Object displayImpl() {
        final Method getImplementation;
        try {
            getImplementation = Display.class.getDeclaredMethod("getImplementation");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        getImplementation.setAccessible(true);
        try {
            return getImplementation.invoke(null);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
