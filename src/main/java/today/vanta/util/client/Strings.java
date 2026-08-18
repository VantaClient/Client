package today.vanta.util.client;

import java.util.Arrays;
import java.util.List;

public interface Strings {
    String CLIENT_NAME = "Vanta";
    String CLIENT_VERSION = "1.8";
    String DEVELOPERS = "made by mark & luna";
    String CLIENT_FULL_TITLE = CLIENT_NAME + " - " + CLIENT_VERSION + " - " + DEVELOPERS;

    List<String> CHANGELOG = Arrays.asList(
            "[~] Combined Theme & ClientSounds into ClientSettings",
            "[~] Renamed Phase to Spider",
            "[#] Fixed error logging messages",
            "[#] Fixed possible crashes",
            "[#] Fixed UI offsets",
            "[-] Removed bundled Minecraft sounds",
            "[-] Removed FontSettings",
            "[-] Removed unused fonts",
            "[+] Added IceSpeedBoost",
            "[+] Added HighJump"
    );
}