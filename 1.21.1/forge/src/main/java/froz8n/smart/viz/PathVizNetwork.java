package froz8n.smart.viz;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal clientbound channel that streams a smart zombie's pathfinding plan to
 * players tracking it, for the debug overlay ("/spawnsmart zombie path").
 */
public final class PathVizNetwork {

    private static final int PROTOCOL = 1;
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath("smartmobs", "pathviz"))
            .networkProtocolVersion(PROTOCOL)
            .optional()
            .simpleChannel();

    private PathVizNetwork() {
    }

    /** Registers the message. Call once during common setup. */
    public static void register() {
        CHANNEL.messageBuilder(PathVizData.class)
                .direction(PacketFlow.CLIENTBOUND)
                .encoder(PathVizNetwork::encode)
                .decoder(PathVizNetwork::decode)
                .consumerMainThread((msg, ctx) -> {
                    // Runs on the client main thread.
                    org.slf4j.LoggerFactory.getLogger("smartmobs-viz")
                            .info("CLIENT received path: entity={} cells={}", msg.entityId, msg.cells.size());
                    ClientPathStore.put(msg);
                    ctx.setPacketHandled(true);
                })
                .add();
    }

    /** Sends the given plan to every player currently tracking the entity. */
    public static void sendToTrackers(Entity entity, PathVizData data) {
        org.slf4j.LoggerFactory.getLogger("smartmobs-viz")
                .info("SERVER sending path: entity={} cells={}", data.entityId, data.cells.size());
        CHANNEL.send(data, PacketDistributor.TRACKING_ENTITY.with(entity));
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
