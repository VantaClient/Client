package today.vanta;

import de.florianmichael.viamcp.ViaMCP;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import today.vanta.storage.impl.*;
import today.vanta.util.client.Strings;
import today.vanta.util.game.events.bus.EventBus;
import today.vanta.util.system.FileUtil;
import today.vanta.util.system.lwjgl.imgui.ImGuiImpl;

public enum Vanta {
    instance;

    public final Logger logger = LogManager.getLogger();
    public final EventBus eventBus = new EventBus();

    public ModuleStorage moduleStorage;
    public CommandStorage commandStorage;
    public ProcessorStorage processorStorage;
    public ScreenStorage screenStorage;
    public FileStorage fileStorage;
    public AccountStorage accountStorage;

    static {
        FileUtil.createFolder(Strings.CLIENT_NAME);
        FileUtil.createFolder(Strings.CLIENT_NAME + "/configs");
    }

    public void start() {
        try {
            if (ImGuiImpl.canUseImGui) {
                ImGuiImpl.init();
            }
            ViaMCP.create();
            ViaMCP.INSTANCE.initAsyncSlider();
        } catch (Exception e) {
            logger.warn("Failed to initialise Vanta client");
        }

        moduleStorage = new ModuleStorage();
        commandStorage = new CommandStorage();
        processorStorage = new ProcessorStorage();
        screenStorage = new ScreenStorage();
        fileStorage = new FileStorage();
        accountStorage = new AccountStorage();

        moduleStorage.subscribe();
        commandStorage.subscribe();
        screenStorage.subscribe();
        processorStorage.subscribe();
        fileStorage.subscribe();
        accountStorage.subscribe();
    }

    public void stop() {
        fileStorage.stop();
        screenStorage.stop();
    }
}