package today.vanta;

import de.florianmichael.viamcp.ViaMCP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import today.vanta.storage.impl.*;
import today.vanta.util.client.IClient;
import today.vanta.util.game.events.bus.EventBus;
import today.vanta.util.system.FileUtil;
import today.vanta.util.system.VantaFile;

public class Vanta {
    public final static Vanta instance = new Vanta();
    public final Logger logger = LogManager.getLogger();
    public final EventBus eventBus = new EventBus();

    public ModuleStorage moduleStorage;
    public ConfigStorage configStorage;
    public ProcessorStorage processorStorage;

    public ScreenStorage screenStorage;

    static {
        FileUtil.createFolder(IClient.CLIENT_NAME);
        FileUtil.createFolder(IClient.CLIENT_NAME + "/configs");
    }

    public void start() {
        try {
            ViaMCP.create();

            ViaMCP.INSTANCE.initAsyncSlider();
        } catch (Exception e) {
            logger.warn("Failed to create an instance of ViaMCP");
        }

        moduleStorage = new ModuleStorage();
        processorStorage = new ProcessorStorage();
        screenStorage = new ScreenStorage();
        configStorage = new ConfigStorage();

        moduleStorage.subscribe();
        screenStorage.subscribe();
        processorStorage.subscribe();
        configStorage.subscribe();
    }

    public void stop() {
        configStorage.saveConfig(VantaFile.getFile("configs/default.json"));
    }
}
