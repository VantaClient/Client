import net.minecraft.client.main.Main;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;

public class Start {
    public static void main(String[] args) {
        final String os = System.getProperty("os.name").startsWith("Windows")
                ? "windows"
                : "linux";
        final String arch = System.getProperty("os.arch");
        final Path allNativesFolder = FileSystems
                .getDefault() // Path.of does this
                .getPath("../natives/");
        final Path osNativesFolder = allNativesFolder.resolve(os);
        final Path nativesPath = osNativesFolder.resolve(arch).toAbsolutePath();
        if (!nativesPath.toFile().exists()) {
            throw new UnsupportedOperationException(
                    String.format(
                            "Unsupported architecture/OS combo: %s/%s. "
                                    + "Please create an issue! "
                                    + "If you want to fix it yourself, "
                                    + "find the natives required, "
                                    + "create a folder in %s",
                            os, arch, nativesPath
                    )
            );
        }
        System.setProperty("org.lwjgl.librarypath", nativesPath.toString());
        Main.main(concat(new String[]{
                "--version", "1.8.9", "--accessToken", "0",
                "--assetsDir", "assets",
                "--assetIndex", "1.8", "--userProperties", "{}"
        }, args));
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
