package org.lwjgl.opengl;

import org.lwjgl.opengl.GL43;

public class ARBDebugOutputCallback {

    private final Handler handler;

    public ARBDebugOutputCallback() {
        this.handler = null;
    }

    public ARBDebugOutputCallback(Handler handler) {
        this.handler = handler;
    }

    public Handler getHandler() {
        return handler;
    }

    public interface Handler {
        void handleMessage(int source, int type, int id, int severity, String message);
    }

}
