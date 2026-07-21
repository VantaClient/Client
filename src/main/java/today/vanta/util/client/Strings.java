package today.vanta.util.client;

import java.util.Arrays;
import java.util.List;

public interface Strings {
    String CLIENT_NAME = "Vanta";
    String CLIENT_VERSION = "1.7";
    String DEVELOPERS = "made by mark & luna";
    String CLIENT_FULL_TITLE = CLIENT_NAME + " - " + CLIENT_VERSION + " - " + DEVELOPERS;

    List<String> CHANGELOG = Arrays.asList(
            "[#] Fixed macOS ImGui issues (now optional with env var ``DISABLE_IMGUI=true``)",
            "[#] Fixed Boxy ClickGUI size & position not saving",
            "[#] Fixed glass check checking for barriers",
            "[#] Fixed potential ViaVersion issues",
            "[#] Fixed Boxy ClickGUI stutterness",
            "[#] Fixed module event crashes",
            "[+] Added StaffDetector",
            "[~] Added display names for ESP & ChestESP",
            "[~] Moved NoWeb to separate module class",
            "[~] Moved ImGui ini file path",
            "[~] Changed VClip wording"
    );
}