package today.vanta.util.game.render.font.msdf;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class MsdfShader {
    private static MsdfShader instance;

    private final int program;
    private final int rangeUniform, atlasSizeUniform, thicknessUniform;

    private MsdfShader() {
        final int vertexShader = compile(
                GL20.GL_VERTEX_SHADER,
                loadSource("/assets/vanta/shaders/msdf/msdf_font.vsh")
        );
        final int fragmentShader = compile(
                GL20.GL_FRAGMENT_SHADER,
                loadSource("/assets/vanta/shaders/msdf/msdf_font.fsh")
        );

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new IllegalStateException("Failed to link MSDF font shader: " + GL20.glGetProgramInfoLog(program, 32768));

        GL20.glDetachShader(program, vertexShader);
        GL20.glDetachShader(program, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        rangeUniform = GL20.glGetUniformLocation(program, "Range");
        atlasSizeUniform = GL20.glGetUniformLocation(program, "AtlasSize");
        thicknessUniform = GL20.glGetUniformLocation(program, "Thickness");
    }

    public static MsdfShader getInstance() {
        if (instance == null)
            instance = new MsdfShader();
        return instance;
    }

    public int use(final MsdfFont font) {
        final int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL20.glUseProgram(program);
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "Sampler0"), 0);
        GL20.glUniform1f(rangeUniform, font.getAtlas().getRange());
        GL20.glUniform2f(atlasSizeUniform, font.getAtlas().getWidth(), font.getAtlas().getHeight());
        return previousProgram;
    }

    public void setThickness(final float thickness) {
        GL20.glUniform1f(thicknessUniform, thickness);
    }

    public void restore(final int program) {
        GL20.glUseProgram(program);
    }

    private static int compile(final int type, final String source) {
        final int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new IllegalStateException("Failed to compile MSDF font shader: " + GL20.glGetShaderInfoLog(shader, 32768));
        return shader;
    }

    private static String loadSource(final String resourcePath) {
        try (InputStream stream = MsdfShader.class.getResourceAsStream(resourcePath)) {
            if (stream == null)
                throw new IllegalArgumentException("Missing shader resource " + resourcePath);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                final StringBuilder source = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    source.append(line).append('\n');
                return source.toString();
            }
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to read shader resource " + resourcePath, exception);
        }
    }
}
