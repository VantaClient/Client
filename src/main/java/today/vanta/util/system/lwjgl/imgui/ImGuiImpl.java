package today.vanta.util.system.lwjgl.imgui;

import imgui.ImDrawData;
import imgui.ImGui;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.Util;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ImGuiImpl {
    // this is technically a separate class so static {...} blocks of classes referenced here
    // won't be loaded unless cat
    private static final class Ok {
        private static final List<ImGuiCall> list = new ArrayList<>();
        private static final ImGuiGL3 imGuiGl = new ImGuiGL3();
        private static final ImGuiLwjgl2 imGuiLwjgl = new ImGuiLwjgl2();
        public static void init() {
            ImGui.createContext();
            imGuiLwjgl.init();
            imGuiGl.init();
            ImGui.init();
        }
        public static void imguiNewFrame() {
            ImGui.newFrame();
        }
        public static void imguiRender() {
            ImGui.render();
        }
        public static ImDrawData imguiGetDrawData() {
            return ImGui.getDrawData();
        }
    }
    public static final boolean canUseImGui = Util.getOSType() != Util.EnumOS.OSX && !System.getProperty("os.arch").contains("aarch64");
    public static void init() {
        Ok.init();
    }
    public static void draw(ImGuiCall call) {
        if (!canUseImGui) return;
        Ok.list.add(call);
    }

    public static void key(int key) {
        if (!canUseImGui) return;
        Ok.imGuiLwjgl.charCallback(key);
    }

    public static void scroll() {
        if (!canUseImGui) return;
        if (Mouse.getEventDWheel() != 0) {
            Ok.imGuiLwjgl.scrollCallback(Mouse.getEventDWheel());
        }
    }

    public static void render(Framebuffer fb, float delta) {
        if (!canUseImGui) return;
        //startup fix
        if (delta <= 0f) delta = 0.1f;
        Ok.imGuiLwjgl.newFrame(fb.framebufferWidth, fb.framebufferHeight, delta);
        Ok.imguiNewFrame();

        Ok.list.forEach(ImGuiCall::execute);
        Ok.list.clear();

        Ok.imguiRender();
        Ok.imGuiGl.renderDrawData(Objects.requireNonNull(Ok.imguiGetDrawData()));
    }

    public interface ImGuiCall {
        void execute();
    }
}