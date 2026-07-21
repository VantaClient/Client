package today.vanta.util.system.lwjgl.imgui;

import imgui.ImGui;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ImGuiImpl {
    private static final List<ImGuiCall> list = new ArrayList<>();
    private static final ImGuiGL3 imGuiGl = new ImGuiGL3();
    private static final ImGuiLwjgl2 imGuiLwjgl = new ImGuiLwjgl2();

    public static void init() {
        ImGui.createContext();
        imGuiLwjgl.init();
        imGuiGl.init();
    }

    public static void draw(ImGuiCall call) {
        list.add(call);
    }

    public static void key(int key) {
        imGuiLwjgl.charCallback(key);
    }

    public static void scroll() {
        if (Mouse.getEventDWheel() != 0) {
            imGuiLwjgl.scrollCallback(Mouse.getEventDWheel());
        }
    }

    public static void render(Framebuffer fb, float delta) {
        if (delta <= 0f) delta = 0.1f;
        imGuiLwjgl.newFrame(fb.framebufferWidth, fb.framebufferHeight, delta);

        if (!ImGui.getIO().getFonts().isBuilt()) {
            imGuiGl.updateFontsTexture();
        }

        ImGui.newFrame();

        list.forEach(ImGuiCall::execute);
        list.clear();

        ImGui.render();
        imGuiGl.renderDrawData(Objects.requireNonNull(ImGui.getDrawData()));
    }

    public interface ImGuiCall {
        void execute();
    }
}