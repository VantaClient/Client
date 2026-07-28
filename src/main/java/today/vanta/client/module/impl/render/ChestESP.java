package today.vanta.client.module.impl.render;

import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import today.vanta.Vanta;
import today.vanta.client.event.impl.client.RenderOverlayEvent;
import today.vanta.client.module.Category;
import today.vanta.client.module.Module;
import today.vanta.client.module.impl.client.Theme;
import today.vanta.util.game.events.EventListen;
import today.vanta.util.game.render.ProjectionUtil;
import today.vanta.util.game.render.shape.impl.Rectangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChestESP extends Module {
    private final List<AxisAlignedBB> boxes = new ArrayList<>();
    private Color color = Vanta.instance.moduleStorage.getT(Theme.class).colors[1];

    public ChestESP() {
        super("ChestESP", "Shows you where chests are.", Category.PLAYER);
    }

    @EventListen
    private void onRenderOverlay(RenderOverlayEvent event) {
        color = Vanta.instance.moduleStorage.getT(Theme.class).colors[0];
        boxes.clear();

        List<TileEntityChest> chests = new ArrayList<>();
        for (Object chestTile : mc.theWorld.loadedTileEntityList) {
            if (chestTile instanceof TileEntityChest) {
                chests.add((TileEntityChest) chestTile);
            }
        }

        Set<BlockPos> visited = new HashSet<>();

        for (TileEntityChest chest : chests) {
            BlockPos pos = chest.getPos();
            if (visited.contains(pos)) continue;
            visited.add(pos);

            TileEntityChest partner = findAdjacentPartner(chest, chests, visited);

            if (partner != null) {
                BlockPos p2 = partner.getPos();
                visited.add(p2);

                double minX = Math.min(pos.getX(), p2.getX());
                double minY = Math.min(pos.getY(), p2.getY());
                double minZ = Math.min(pos.getZ(), p2.getZ());
                double maxX = Math.max(pos.getX(), p2.getX()) + 1;
                double maxY = Math.max(pos.getY(), p2.getY()) + 1;
                double maxZ = Math.max(pos.getZ(), p2.getZ()) + 1;

                boxes.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
            } else {
                boxes.add(new AxisAlignedBB(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1
                ));
            }
        }

        for (AxisAlignedBB box : boxes) {
            ProjectionUtil.ScreenBounds bounds = ProjectionUtil.projectSelectionBoundingBox(box, event.scaledResolution);
            if (bounds == null) continue;

            float x = (float) bounds.minX;
            float y = (float) bounds.minY;
            float width = (float) (bounds.maxX - bounds.minX);
            float height = (float) (bounds.maxY - bounds.minY);
            if (width <= 0.0F || height <= 0.0F) continue;

            Rectangle.create(x - 0.5f, y - 0.5f, width + 1, height + 1)
                    .outline(true).color(Color.BLACK).outlineWidth(1.0f).push(event);

            Rectangle.create(x, y, width, height)
                    .outline(true).color(color).outlineWidth(1.0f).push(event);

            Rectangle.create(x + 0.5f, y + 0.5f, width - 1, height - 1)
                    .outline(true).color(Color.BLACK).outlineWidth(1.0f).push(event);
        }
    }

    private TileEntityChest findAdjacentPartner(TileEntityChest chest, List<TileEntityChest> all, Set<BlockPos> visited) {
        BlockPos pos = chest.getPos();
        BlockPos[] neighbors = {
                pos.north(), pos.south(), pos.east(), pos.west()
        };

        for (TileEntityChest other : all) {
            if (other == chest) continue;
            BlockPos otherPos = other.getPos();
            if (visited.contains(otherPos)) continue;

            for (BlockPos n : neighbors) {
                if (n.equals(otherPos) && chest.getChestType() == other.getChestType()) {
                    return other;
                }
            }
        }
        return null;
    }
}