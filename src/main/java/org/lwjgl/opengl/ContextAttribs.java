package org.lwjgl.opengl;

public class ContextAttribs {

    private int majorVersion;
    private int minorVersion;
    private boolean debug;

    public ContextAttribs() {
        this(2, 1);
    }

    public ContextAttribs(int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public ContextAttribs withDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

}
