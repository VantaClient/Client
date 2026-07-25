import net.minecraft.client.main.Main;

import java.io.File;
import java.util.Arrays;

public class Start {
    public static void main(final String[] args) {
        System.setProperty(
                "org.lwjgl.librarypath",
                new File("natives", System.getProperty("os.name").startsWith("Windows") ? "windows" : "linux").getAbsolutePath()
        );

        final String[] defaultArguments = new String[] {
                "--version", "1.8.9",
                "--accessToken", "0",
                "--assetsDir", "assets",
                "--assetIndex", "1.8",
                "--userProperties", "{}"
        };
        final String[] launchArguments = Arrays.copyOf(defaultArguments, defaultArguments.length + args.length);
        System.arraycopy(args, 0, launchArguments, defaultArguments.length, args.length);
        Main.main(launchArguments);
    }
}
