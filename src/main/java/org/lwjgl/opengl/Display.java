package org.lwjgl.opengl;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.SoundSystemOpenAL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import today.vanta.util.os.Enhancements;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

public class Display {
    // ------------------------------ Variables here ------------------------------

    private static long windowHandle = -1L;

    private static String title = "";
    private static boolean resizable;

    private static org.lwjgl.opengl.DisplayMode displayMode = new org.lwjgl.opengl.DisplayMode(640, 480, 32, 60);

    private static int width;
    private static int height;

    // Used for fullscreen
    private static int lastX;
    private static int lastY;
    private static int lastWidth;
    private static int lastHeight;

    private static long monitor;

    private static boolean windowResized;

    private static GLFWWindowSizeCallback sizeCallback = null;

    private static int samples = 0;

    private static ByteBuffer[] cachedIcons = null;

    private static boolean wayland = false;

    private static final Logger LOGGER = LogManager.getLogger("Display");

    // ------------------------------ Functions here ------------------------------

    public static long getWindowHandle() {
        return windowHandle;
    }

    public static void create(PixelFormat pixelFormat) {
        setSamples(pixelFormat.getSamples());
        create();
    }

    public static void create(PixelFormat pixelFormat, ContextAttribs attribs) {
        create(pixelFormat);
    }

    public static void create() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Unable to initialize GLFW");
        }

        if (GLFW.glfwPlatformSupported(GLFW.GLFW_PLATFORM_WAYLAND)) {
            GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_WAYLAND);
            GLFW.glfwInitHint(GLFW.GLFW_WAYLAND_LIBDECOR, GLFW.GLFW_WAYLAND_PREFER_LIBDECOR);
            wayland = true;
            LOGGER.info("Using Wayland as windowing system with libdecor");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, Enhancements.supportsWindowBlur() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_COMPAT_PROFILE);

        if (samples > 0) {
            GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, samples);
        }

        windowHandle = GLFW.glfwCreateWindow(displayMode.getWidth(), displayMode.getHeight(), title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            GLFW.glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window");
        }

        width = displayMode.getWidth();
        height = displayMode.getHeight();

        monitor = GLFW.glfwGetPrimaryMonitor();
        if (monitor != MemoryUtil.NULL) {
            GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
            if (vidMode != null) {
                GLFW.glfwSetWindowPos(windowHandle, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
            }
        }

        GLFW.glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();

        SoundSystemOpenAL.create();

        sizeCallback = GLFWWindowSizeCallback.create(Display::resizeCallback);
        GLFW.glfwSetWindowSizeCallback(windowHandle, sizeCallback);

        try {
            Mouse.create();
            Keyboard.create();
        } catch (Exception e) {
            destroy();
            throw new RuntimeException(e);
        }

        GLFW.glfwShowWindow(windowHandle);
        if (cachedIcons != null && !wayland) {
            setIcon(cachedIcons);
        }
    }

    public static void update() {
        windowResized = false;

        Mouse.poll();
        Keyboard.poll();

        GLFW.glfwSwapBuffers(windowHandle);
        GLFW.glfwPollEvents();
    }

    public static void setIcon(ByteBuffer[] icons) {
        if (!Arrays.equals(cachedIcons, icons)) {
            cachedIcons = Arrays.stream(icons)
                    .map(Display::cloneByteBuffer)
                    .toArray(ByteBuffer[]::new);
        }

        if (isCreated() && !wayland) {
            GLFW.glfwSetWindowIcon(windowHandle, iconsToGLFWBuffer(cachedIcons));
        }
    }

    private static ByteBuffer cloneByteBuffer(ByteBuffer original) {
        ByteBuffer clone = BufferUtils.createByteBuffer(original.capacity());
        int oldPosition = original.position();

        clone.put(original);
        original.position(oldPosition);
        clone.flip();

        return clone;
    }

    private static GLFWImage.Buffer iconsToGLFWBuffer(ByteBuffer[] icons) {
        GLFWImage.Buffer buffer = GLFWImage.create(icons.length);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (ByteBuffer icon : icons) {
                int dimension = (int) Math.sqrt(icon.limit() / 4.0f);
                buffer.put(GLFWImage.malloc(stack).set(dimension, dimension, icon));
            }
        }

        buffer.flip();
        return buffer;
    }

    private static void resizeCallback(long window, int newWidth, int newHeight) {
        if (window == windowHandle) {
            windowResized = true;
            width = newWidth;
            height = newHeight;
            if (!isFullScreen()) {
                lastWidth = newWidth;
                lastHeight = newHeight;
            }
        }
    }

    public static boolean isFullScreen() {
        return GLFW.glfwGetWindowMonitor(windowHandle) != MemoryUtil.NULL;
    }

    public static void setFullscreen(boolean fullscreen) {
        if (isFullScreen() == fullscreen || !isCreated() || monitor == MemoryUtil.NULL)
            return;

        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);
        if (mode == null)
            return;

        if (fullscreen) {
            lastWidth = width;
            lastHeight = height;
            if (!wayland) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer x = stack.callocInt(1);
                    IntBuffer y = stack.callocInt(1);
                    GLFW.glfwGetWindowPos(windowHandle, x, y);
                    lastX = x.get(0);
                    lastY = y.get(0);
                }
            } else {
                lastX = 0;
                lastY = 0;
            }
            GLFW.glfwSetWindowMonitor(windowHandle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
        } else {
            GLFW.glfwSetWindowMonitor(windowHandle, MemoryUtil.NULL, lastX, lastY, lastWidth, lastHeight, GLFW.GLFW_DONT_CARE);
            if (Minecraft.getMinecraft() != null) {
                Minecraft.getMinecraft().resize(lastWidth, lastHeight);
            }
        }

        Mouse.setIgnoreFirstMove(true);
    }

    public static org.lwjgl.opengl.DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static void setDisplayMode(org.lwjgl.opengl.DisplayMode mode) {
        displayMode = mode;
        if (isCreated() && !isFullScreen()) {
            GLFW.glfwSetWindowSize(windowHandle, mode.getWidth(), mode.getHeight());
            width = mode.getWidth();
            height = mode.getHeight();
            lastWidth = width;
            lastHeight = height;
        }
    }

    public static void setTitle(String newTitle) {
        title = newTitle;
        if (isCreated()) {
            GLFW.glfwSetWindowTitle(windowHandle, title);
        }
    }

    public static String getTitle() {
        return title;
    }

    public static void setResizable(boolean isResizable) {
        resizable = isResizable;
        if (isCreated()) {
            GLFW.glfwSetWindowAttrib(windowHandle, GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static int getX() {
        if (isCreated()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.callocInt(1);
                IntBuffer y = stack.callocInt(1);
                GLFW.glfwGetWindowPos(windowHandle, x, y);
                return x.get(0);
            }
        }
        return 0;
    }

    public static int getY() {
        if (isCreated()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer x = stack.callocInt(1);
                IntBuffer y = stack.callocInt(1);
                GLFW.glfwGetWindowPos(windowHandle, x, y);
                return y.get(0);
            }
        }
        return 0;
    }

    public static org.lwjgl.opengl.DisplayMode[] getAvailableDisplayModes() {
        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        if (primaryMonitor == MemoryUtil.NULL) {
            return new org.lwjgl.opengl.DisplayMode[0];
        }

        GLFWVidMode.Buffer videoModes = GLFW.glfwGetVideoModes(primaryMonitor);
        if (videoModes == null) {
            return new org.lwjgl.opengl.DisplayMode[0];
        }

        org.lwjgl.opengl.DisplayMode[] modes = new org.lwjgl.opengl.DisplayMode[videoModes.capacity()];
        for (int i = 0; i < videoModes.capacity(); i++) {
            GLFWVidMode mode = videoModes.get(i);
            modes[i] = new org.lwjgl.opengl.DisplayMode(mode.width(), mode.height(), mode.redBits() + mode.blueBits() + mode.greenBits(), mode.refreshRate());
        }
        return modes;
    }

    public static org.lwjgl.opengl.DisplayMode getDesktopDisplayMode() {
        org.lwjgl.opengl.DisplayMode[] displayModes = getAvailableDisplayModes();
        if (displayModes.length == 0) {
            return null;
        }

        org.lwjgl.opengl.DisplayMode maxElement = displayModes[0];
        int maxValue = maxElement.getWidth() * maxElement.getHeight();

        for (org.lwjgl.opengl.DisplayMode element : displayModes) {
            int area = element.getWidth() * element.getHeight();
            if (maxValue < area) {
                maxElement = element;
                maxValue = area;
            }
        }
        return maxElement;
    }

    private static void destroyWindow() {
        if (sizeCallback != null) {
            sizeCallback.free();
            sizeCallback = null;
        }

        Mouse.destroy();
        Keyboard.destroy();

        if (windowHandle != MemoryUtil.NULL) {
            GLFW.glfwDestroyWindow(windowHandle);
            windowHandle = -1L;
        }
    }

    public static void destroy() {
        destroyWindow();

        GLFW.glfwTerminate();
        GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    public static boolean isCreated() {
        return windowHandle != -1L;
    }

    public static boolean isCloseRequested() {
        return isCreated() && GLFW.glfwWindowShouldClose(windowHandle);
    }

    public static boolean isActive() {
        return isCreated() && GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    public static void sync(int fps) {
        org.lwjgl.opengl.Sync.sync(fps);
    }

    public static void setVSyncEnabled(boolean enabled) {
        if (isCreated()) {
            GLFW.glfwSwapInterval(enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    public static boolean wasResized() {
        return windowResized;
    }

    public static void setLocation(int x, int y) {
        if (isCreated() && !wayland) {
            GLFW.glfwSetWindowPos(windowHandle, x, y);
        }
    }

    public static void setSamples(int i) {
        samples = i;
    }

    public static void setRawInputEnabled(boolean enabled) {
        if (isCreated() && GLFW.glfwRawMouseMotionSupported()) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_RAW_MOUSE_MOTION, enabled ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        }
    }

    public static boolean isUsingWayland() {
        return wayland;
    }

    public static void setIcon(String path) {
        IntBuffer w = MemoryUtil.memAllocInt(1);
        IntBuffer h = MemoryUtil.memAllocInt(1);
        IntBuffer comp = MemoryUtil.memAllocInt(1);

        // Icons
        {
            ByteBuffer icon16;
            ByteBuffer icon32;
            try {
                icon16 = ioResourceToByteBuffer(path, 2048);
                icon32 = ioResourceToByteBuffer(path, 4096);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            try (GLFWImage.Buffer icons = GLFWImage.malloc(2)) {
                ByteBuffer pixels16 = STBImage.stbi_load_from_memory(icon16, w, h, comp, 4);
                icons
                        .position(0)
                        .width(w.get(0))
                        .height(h.get(0))
                        .pixels(pixels16);

                ByteBuffer pixels32 = STBImage.stbi_load_from_memory(icon32, w, h, comp, 4);
                icons
                        .position(1)
                        .width(w.get(0))
                        .height(h.get(0))
                        .pixels(pixels32);

                icons.position(0);
                GLFW.glfwSetWindowIcon(windowHandle, icons);

                STBImage.stbi_image_free(pixels32);
                STBImage.stbi_image_free(pixels16);
            }
        }

        MemoryUtil.memFree(comp);
        MemoryUtil.memFree(h);
        MemoryUtil.memFree(w);
    }

    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;

        try (
                InputStream source = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
                ReadableByteChannel rbc = Channels.newChannel(source)
        ) {
            buffer = BufferUtils.createByteBuffer(bufferSize);

            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1)
                    break;
                if (buffer.remaining() == 0)
                    buffer = resizeBuffer(buffer, buffer.capacity() * 2);
            }
        }

        buffer.flip();
        return buffer;
    }
}
