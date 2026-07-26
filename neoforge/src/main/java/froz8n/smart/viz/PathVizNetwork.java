package froz8n.smart.viz;

import froz8n.SmartMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal clientbound payload that streams a smart zombie's pathfinding plan to
 * players tracking it, for the debug overlay ("/spawnsmart zombie path").
 */
public final class PathVizNetwork {

    /** Wraps {@link PathVizData} so it can travel as a vanilla custom payload. */
    public record Payload(PathVizData data) implements CustomPacketPayload {
        public static final Type<Payload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(SmartMobs.MODID, "pathviz"));
        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = CustomPacketPayload.codec(
                (m, b) -> encode(m.data(), b),
                b -> new Payload(decode(b)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private PathVizNetwork() {
    }

    /** Sends the given plan to every player currently tracking the entity. */
    public static void sendToTrackers(Entity entity, PathVizData data) {
        org.slf4j.LoggerFactory.getLogger("smartmobs-viz")
                .info("SERVER sending path: entity={} cells={}", data.entityId, data.cells.size());
        PacketDistributor.sendToPlayersTrackingEntity(entity, new Payload(data));
    }

    private static void encode(PathVizData data, FriendlyByteBuf buf) {
        buf.writeVarInt(data.entityId);
        buf.writeVarInt(data.cells.size());
        for (PathVizData.Cell c : data.cells) {
            buf.writeBlockPos(c.pos());
            buf.writeBoolean(c.dig());
        }
    }

    private static PathVizData decode(FriendlyByteBuf buf) {
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
