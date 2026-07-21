package org.lwjgl.opengl;

public class ARBDebugOutput {

    public static void glDebugMessageCallbackARB(ARBDebugOutputCallback callback) {
        GL43.glDebugMessageCallback(new GLDebugMessageCallback() {
            @Override
            public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
                if (callback.getHandler() != null) {
                    callback.getHandler().handleMessage(source, type, id, severity, GLDebugMessageCallback.getMessage(length, message));
                }
            }
        }, 0);
    }

    public static void glDebugMessageControlARB(int source, int type, int severity, java.nio.IntBuffer ids, boolean enabled) {
        GL43.glDebugMessageControl(source, type, severity, ids, enabled);
    }

}
