package today.vanta.util.system;

import net.minecraft.client.Minecraft;
import today.vanta.Vanta;
import today.vanta.util.client.IClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    public static Path getRunningPath() {
        return Paths.get(Minecraft.getMinecraft().mcDataDir.getPath(), IClient.CLIENT_NAME);
    }

    public static void createFolder(String name) {
        try {
            Files.createDirectories(Paths.get(name));
        } catch (IOException exception) {
            Vanta.instance.logger.error("Failed creating the folder {} due to {}", name, exception.getMessage());
        }
    }
}
