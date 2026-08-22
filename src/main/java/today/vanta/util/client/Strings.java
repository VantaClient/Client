package today.vanta.util.client;

import java.util.Arrays;
import java.util.List;

public interface Strings {
    String CLIENT_NAME = "Vanta";
    String CLIENT_VERSION = "1.8";
    String DEVELOPERS = "made by mark & luna";
    String CLIENT_FULL_TITLE = CLIENT_NAME + " - " + CLIENT_VERSION + " - " + DEVELOPERS;
    String USERNAME = System.getProperty("user.name").isEmpty() ? "Person That I don't know" : System.getProperty("user.name");

    List<String> TIPS = Arrays.asList(
            "Fun Fact: Money doesn't grow on trees",
            "I like trains",
            "Vanta Banta Fanta Canta Zanta Lanta Nanta :)",
            "Rick Ross admits he uses Vanta to get autobanned on Miniblox",
            "Why settle for rice when you have Vanta?",
            "I like turtles",
            "45 monkeys vs 45 markipliers",
            "You are so portugese",
            "Did you know? That cats don't spawn in trees, they actually climb up there",
            "WHATSUP YOUTUBE! WELCOME TO A NEW VIDEO",
            "Welcome " + USERNAME,
            "Vanta sponsored by Chicago Grim Reaper",
            "LOLZ",
            "dadam"
    );

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