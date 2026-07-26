package froz8n.smart.viz;

import froz8n.SmartMobs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal clientbound channel that streams a smart zombie's pathfinding plan to
 * players tracking it, for the debug overlay ("/spawnsmart zombie path").
 */
public final class PathVizNetwork {

    public static final ResourceLocation CHANNEL = new ResourceLocation(SmartMobs.MODID, "pathviz");

    private PathVizNetwork() {
    }

    /** Nothing to declare on 1.20.1: a clientbound channel exists once someone listens. */
    public static void register() {
    }

    /** Sends the given plan to every player currently tracking the entity. */
    public static void sendToTrackers(Entity entity, PathVizData data) {
        org.slf4j.LoggerFactory.getLogger("smartmobs-viz")
                .info("SERVER sending path: entity={} cells={}", data.entityId, data.cells.size());
        for (ServerPlayer receiver : PlayerLookup.tracking(entity)) {
            if (!ServerPlayNetworking.canSend(receiver, CHANNEL)) continue;
            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            encode(data, buf);
            ServerPlayNetworking.send(receiver, CHANNEL, buf);
        }
    }

    private static void encode(PathVizData data, FriendlyByteBuf buf) {
        buf.writeVarInt(data.entityId);
        buf.writeVarInt(data.cells.size());
        for (PathVizData.Cell c : data.cells) {
            buf.writeBlockPos(c.pos());
            buf.writeBoolean(c.dig());
        }
    }

    public static PathVizData decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int n = buf.readVarInt();
        List<PathVizData.Cell> cells = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos pos = buf.readBlockPos();
            boolean dig = buf.readBoolean();
            cells.add(new PathVizData.Cell(pos, dig));
        }
        return new PathVizData(entityId, cells);
    }
}