package froz8n.smart.viz;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal clientbound message that streams a smart zombie's pathfinding plan to
 * players tracking it, for the debug overlay ("/spawnsmart zombie path").
 * It rides the jammer channel rather than opening a second one.
 */
public final class PathVizNetwork {

    public record Payload(PathVizData data) {}

    private static SimpleChannel channel;

    private PathVizNetwork() {
    }

    /** Registered from {@code SoundWaveNetwork.register()} with the next free message id. */
    public static void register(SimpleChannel simpleChannel, int id) {
        channel = simpleChannel;
        channel.messageBuilder(Payload.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, b) -> encode(m.data(), b))
                .decoder(b -> new Payload(decode(b)))
                .consumerMainThread((m, c) -> {
                    ClientPathStore.put(m.data());
                    c.get().setPacketHandled(true);
                }).add();
    }

    /** Sends the given plan to every player currently tracking the entity. */
    public static void sendToTrackers(net.minecraft.world.entity.Entity entity, PathVizData data) {
        if (channel == null) return;
        org.slf4j.LoggerFactory.getLogger("smartmobs-viz")
                .info("SERVER sending path: entity={} cells={}", data.entityId, data.cells.size());
        channel.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new Payload(data));
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