package org.lwjgl.compatibility;


import org.lwjgl.compatibility.glfw.GLFWKeyboardImplementation;
import org.lwjgl.compatibility.glfw.GLFWMouseImplementation;
import org.lwjgl.compatibility.input.CombinedInputImplementation;
import org.lwjgl.compatibility.input.InputImplementation;

public class LWJGLImplementationUtils {
    private static InputImplementation _inputImplementation;

    public static InputImplementation getOrCreateInputImplementation() {
        if (_inputImplementation == null) {
            _inputImplementation = createImplementation();
        }
        return _inputImplementation;
    }

    private static InputImplementation createImplementation() {
        return new CombinedInputImplementation(
                new GLFWKeyboardImplementation(),
                new GLFWMouseImplementation()
        );
    }

}
