package today.vanta.client.event.impl.game.world;

import net.minecraft.entity.Entity;
import today.vanta.client.event.Event;

public class EntityCollisionBorderSizeEvent extends Event {
    public Entity entity;
    public float size;

    public EntityCollisionBorderSizeEvent(Entity entity, float size) {
        this.entity = entity;
        this.size = size;
    }
}