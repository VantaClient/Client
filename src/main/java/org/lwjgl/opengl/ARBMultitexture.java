package org.lwjgl.opengl;

public class ARBMultitexture {

    public static void glActiveTextureARB(int texture) {
        GL13.glActiveTexture(texture);
    }

    public static void glClientActiveTextureARB(int texture) {
        GL13.glClientActiveTexture(texture);
    }

    public static void glMultiTexCoord2fARB(int target, float s, float t) {
        GL13.glMultiTexCoord2f(target, s, t);
    }

}
