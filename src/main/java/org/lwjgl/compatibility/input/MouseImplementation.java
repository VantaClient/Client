package org.lwjgl.compatibility.input;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public interface MouseImplementation {
    void createMouse();

    void destroyMouse();

    void pollMouse(IntBuffer coord_buffer, ByteBuffer buttons_buffer);

    void readMouse(ByteBuffer readBuffer);

    void setCursorPosition(int x, int y);

    void grabMouse(boolean grab);

    boolean hasWheel();

    int getButtonCount();

    boolean isInsideWindow();
}
