package net.optifine;

import net.minecraft.client.renderer.GLAllocation;

import java.awt.*;
import java.nio.IntBuffer;

public class Mipmaps {

    public static int alphaBlend(int c1, int c2, int c3, int c4) {
        int i = alphaBlend(c1, c2);
        int j = alphaBlend(c3, c4);
        int k = alphaBlend(i, j);
        return k;
    }

    private static int alphaBlend(int c1, int c2) {
        int i = (c1 & -16777216) >> 24 & 255;
        int j = (c2 & -16777216) >> 24 & 255;
        int k = (i + j) / 2;

        if (i == 0 && j == 0) {
            i = 1;
            j = 1;
        } else {
            if (i == 0) {
                c1 = c2;
                k /= 2;
            }

            if (j == 0) {
                c2 = c1;
                k /= 2;
            }
        }

        int l = (c1 >> 16 & 255) * i;
        int i1 = (c1 >> 8 & 255) * i;
        int j1 = (c1 & 255) * i;
        int k1 = (c2 >> 16 & 255) * j;
        int l1 = (c2 >> 8 & 255) * j;
        int i2 = (c2 & 255) * j;
        int j2 = (l + k1) / (i + j);
        int k2 = (i1 + l1) / (i + j);
        int l2 = (j1 + i2) / (i + j);
        return k << 24 | j2 << 16 | k2 << 8 | l2;
    }

    private int averageColor(int i, int j) {
        int k = (i & -16777216) >> 24 & 255;
        int p = (j & -16777216) >> 24 & 255;
        return (k + j >> 1 << 24) + ((k & 16711422) + (p & 16711422) >> 1);
    }

    public static IntBuffer[] makeMipmapBuffers(Dimension[] mipmapDimensions, int[][] mipmapDatas) {
        if (mipmapDimensions == null) {
            return null;
        } else {
            IntBuffer[] aintbuffer = new IntBuffer[mipmapDimensions.length];

            for (int i = 0; i < mipmapDimensions.length; ++i) {
                Dimension dimension = mipmapDimensions[i];
                int j = dimension.width * dimension.height;
                IntBuffer intbuffer = GLAllocation.createDirectIntBuffer(j);
                int[] aint = mipmapDatas[i];
                intbuffer.clear();
                intbuffer.put(aint);
                intbuffer.clear();
                aintbuffer[i] = intbuffer;
            }

            return aintbuffer;
        }
    }

}
