/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.lwjgl.input;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.compatibility.LWJGLImplementationUtils;
import org.lwjgl.compatibility.input.InputImplementation;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.Display;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;


/**
 * <br>
 * A raw Mouse interface. This can be used to poll the current state of the
 * mouse buttons, and determine the mouse movement delta since the last poll.
 * <p>
 * n buttons supported, n being a native limit. A scrolly wheel is also
 * supported, if one such is available. Movement is reported as delta from
 * last position or as an absolute position. If the window has been created
 * the absolute position will be clamped to 0 - width | height.
 *
 * @author cix_foo <cix_foo@users.sourceforge.net>
 * @author elias_naur <elias_naur@users.sourceforge.net>
 * @author Brian Matzon <brian@matzon.dk>
 * @version $Revision$
 * $Id$
 */
public class Mouse {
    public static final int EVENT_SIZE = 1 + 1 + 4 + 4 + 4 + 8;

    private static boolean created;

    private static ByteBuffer buttons;

    private static int x;

    private static int y;

    private static int absoluteX;

    private static int absoluteY;

    private static IntBuffer coordBuffer;

    private static int dX;

    private static int dY;

    private static int dWheel;

    private static int buttonCount = -1;

    private static boolean hasWheel;

    private static String[] buttonName;

    private static final Map<String, Integer> buttonMap = new HashMap<String, Integer>(16);

    private static boolean initialized;

    private static ByteBuffer readBuffer;

    private static int eventButton;

    private static boolean eventState;

    private static int eventDX;
    private static int eventDY;
    private static int eventWheel;

    private static int eventX;
    private static int eventY;
    private static long eventNanos;

    private static int grabX;
    private static int grabY;

    private static int lastEventRawX;
    private static int lastEventRawY;

    private static final int BUFFER_SIZE = 50;

    private static boolean isGrabbed;

    private static InputImplementation implementation;

    private static boolean clipMouseCoordinatesToWindow = !getPrivilegedBoolean("org.input.org.lwjgl.Mouse.allowNegativeMouseCoords");

    private Mouse() {
    }

    public static boolean isClipMouseCoordinatesToWindow() {
        return clipMouseCoordinatesToWindow;
    }

    public static void setClipMouseCoordinatesToWindow(boolean clip) {
        clipMouseCoordinatesToWindow = clip;
    }

    public static void setCursorPosition(int new_x, int new_y) {
        if (!isCreated())
            throw new IllegalStateException("Mouse is not created");

        eventX = new_x;
        x = new_x;
        eventY = new_y;
        y = new_y;
        absoluteX = new_x;
        absoluteY = new_y;
        if (!isGrabbed() && Display.isCreated()) {
            try {
                GLFW.glfwSetCursorPos(Display.getWindowHandle(), new_x, new_y);
            } catch (Exception e) {
                // Wayland may not support cursor positioning, update internal state only
                grabX = new_x;
                grabY = new_y;
            }
        } else {
            grabX = new_x;
            grabY = new_y;
        }
    }

    private static void initialize() {
        Sys.initialize();

        buttonName = new String[16];
        for (int i = 0; i < 16; i++) {
            buttonName[i] = "BUTTON" + i;
            buttonMap.put(buttonName[i], i);
        }

        initialized = true;
    }

    private static void resetMouse() {
        dX = dY = dWheel = 0;
        if (readBuffer != null) {
            readBuffer.position(readBuffer.limit());
        }
    }

    static InputImplementation getImplementation() {
        return implementation;
    }

    private static void create(InputImplementation impl) throws LWJGLException {
        if (created)
            return;

        if (!initialized)
            initialize();

        implementation = impl;
        implementation.createMouse();
        hasWheel = implementation.hasWheel();
        created = true;

        buttonCount = implementation.getButtonCount();
        buttons = BufferUtils.createByteBuffer(buttonCount);
        coordBuffer = BufferUtils.createIntBuffer(3);

        readBuffer = ByteBuffer.allocate(EVENT_SIZE * BUFFER_SIZE);
        readBuffer.limit(0);
        setGrabbed(isGrabbed);
    }

    public static void create() throws LWJGLException {
        if (!Display.isCreated()) throw new IllegalStateException("Display must be created.");

        create(LWJGLImplementationUtils.getOrCreateInputImplementation());
    }

    public static boolean isCreated() {
        return created;
    }

    public static void destroy() {
        if (!created)
            return;

        created = false;
        buttons = null;
        coordBuffer = null;
        readBuffer = null;

        if (implementation != null) {
            implementation.destroyMouse();
            implementation = null;
        }
    }

    public static void poll() {
        if (!created)
            throw new IllegalStateException("Mouse must be created before you can poll it");

        if (isFixClip()) {
            return;
        }

        implementation.pollMouse(coordBuffer, buttons);

        int pollX = coordBuffer.get(0);
        int pollY = coordBuffer.get(1);
        int pollWheel = coordBuffer.get(2);

        if (isGrabbed()) {
            dX += pollX;
            dY += pollY;

            x += pollX;
            y += pollY;

            absoluteX += pollX;
            absoluteY += pollY;
        } else {
            dX = pollX - x;
            dY = pollY - y;

            absoluteX = x = pollX;
            absoluteY = y = pollY;
        }

        if (clipMouseCoordinatesToWindow) {
            x = Math.min(Display.getWidth() - 1, Math.max(0, x));
            y = Math.min(Display.getHeight() - 1, Math.max(0, y));
        }

        dWheel += pollWheel;

        read();
    }

    private static void read() {
        if (readBuffer != null) {
            readBuffer.compact();
            implementation.readMouse(readBuffer);
            readBuffer.flip();
        }
    }

    public static boolean isButtonDown(int button) {
        if (!created)
            throw new IllegalStateException("Mouse must be created before you can poll the button state");

        if (button >= buttonCount || button < 0)
            return false;
        else
            return buttons.get(button) == 1;
    }

    public static String getButtonName(int button) {
        if (button >= buttonName.length || button < 0)
            return null;
        else
            return buttonName[button];
    }

    public static int getButtonIndex(String buttonName) {
        Integer ret = buttonMap.get(buttonName);

        if (ret == null)
            return -1;
        else
            return ret;
    }

    public static boolean next() {
        if (!created)
            throw new IllegalStateException("Mouse must be created before you can read events");

        if (isFixClip()) {
            setFixClip(false);
            return false;
        }

        if (readBuffer != null && readBuffer.hasRemaining()) {
            eventButton = readBuffer.get();
            eventState = readBuffer.get() != 0;

            if (isGrabbed()) {
                eventDX = readBuffer.getInt();
                eventDY = readBuffer.getInt();

                eventX += eventDX;
                eventY += eventDY;

                lastEventRawX += eventDX;
                lastEventRawY += eventDY;
            } else {
                int newEventX = readBuffer.getInt();
                int newEventY = readBuffer.getInt();

                eventDX = newEventX - lastEventRawX;
                eventDY = newEventY - lastEventRawY;

                eventX = newEventX;
                eventY = newEventY;

                lastEventRawX = newEventX;
                lastEventRawY = newEventY;
            }

            if (clipMouseCoordinatesToWindow) {
                eventX = Math.min(Display.getWidth() - 1, Math.max(0, eventX));
                eventY = Math.min(Display.getHeight() - 1, Math.max(0, eventY));
            }

            eventWheel = readBuffer.getInt();
            eventNanos = readBuffer.getLong();

            if (ignoreFirstMove && (eventDX != 0 || eventDY != 0)) {
                ignoreFirstMove = false;
                return next();
            }

            return true;
        } else
            return false;
    }

    public static int getEventButton() {
        return eventButton;
    }

    public static boolean getEventButtonState() {
        return eventState;
    }

    public static int getEventDX() {
        return eventDX;
    }

    public static int getEventDY() {
        return eventDY;
    }

    public static int getEventX() {
        return eventX;
    }

    public static int getEventY() {
        return eventY;
    }

    public static int getEventDWheel() {
        return eventWheel;
    }

    public static long getEventNanoseconds() {
        return eventNanos;
    }

    public static int getX() {
        return x;
    }

    public static int getY() {
        return y;
    }

    public static int getDX() {
        int result = dX;
        dX = 0;
        return result;
    }

    public static int getDY() {
        int result = dY;
        dY = 0;
        return result;
    }

    public static int getDWheel() {
        int result = dWheel;
        dWheel = 0;
        return result;
    }

    public static int getButtonCount() {
        return buttonCount;
    }

    public static boolean hasWheel() {
        return hasWheel;
    }

    public static boolean isGrabbed() {
        return isGrabbed;
    }

    public static void setGrabbed(boolean grab) {
        if (isFixClip()) {
            grab = true;
        }

        boolean grabbed = isGrabbed;

        isGrabbed = grab;

        if (isCreated() && Display.isCreated()) {
            if (grab && !grabbed) {
                grabX = x;
                grabY = y;
                lastEventRawX = x;
                lastEventRawY = y;
                GLFW.glfwSetInputMode(Display.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            } else if (!grab && grabbed) {
                GLFW.glfwSetInputMode(Display.getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                // Skip glfwSetCursorPos on Wayland to avoid GLFW_FEATURE_UNAVAILABLE
                x = grabX;
                y = grabY;
                absoluteX = grabX;
                absoluteY = grabY;
                eventX = grabX;
                eventY = grabY;
            }

            implementation.grabMouse(grab);

            resetMouse();
        }
    }

    public static boolean getPrivilegedBoolean(final String property_name) {
        return Boolean.getBoolean(property_name);
    }

    public static boolean isInsideWindow() {
        return implementation != null && implementation.isInsideWindow();
    }

    private static boolean ignoreFirstMove;

    public static boolean isIgnoreFirstMove() {
        return ignoreFirstMove;
    }

    public static void setIgnoreFirstMove(boolean state) {
        ignoreFirstMove = state;
    }

    private static boolean fixClip;

    public static boolean isFixClip() {
        return fixClip;
    }

    public static void setFixClip(boolean state) {
        fixClip = state;
    }

    public static void setRawInput(boolean state) {
        if (Display.isCreated() && GLFW.glfwRawMouseMotionSupported() &&
                !Mouse.getPrivilegedBoolean("org.input.org.lwjgl.Mouse.disableRawInput")) {
            GLFW.glfwSetInputMode(Display.getWindowHandle(), GLFW.GLFW_RAW_MOUSE_MOTION,
                    state ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }
}
