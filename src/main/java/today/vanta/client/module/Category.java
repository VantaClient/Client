package today.vanta.client.module;

import org.lwjgl.util.vector.Vector2f;

public enum Category {
    COMBAT("Combat", new Vector2f(0, 0)),
    MOVEMENT("Movement", new Vector2f(0, 0)),
    PLAYER("Player", new Vector2f(0, 0)),
    RENDER("Render", new Vector2f(0, 0)),
    HUD("Hud", new Vector2f(0, 0)),
    MISC("Misc", new Vector2f(0, 0)),
    CLIENT("Client", new Vector2f(0, 0));

    public final String name;
    public final Vector2f position;
    Category(String name, Vector2f position) {
        this.name = name;
        this.position = position;
    }
}
