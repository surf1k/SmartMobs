package froz8n.client;

import froz8n.combat.SoundWaveNetwork;
import froz8n.smart.viz.ClientPathStore;
import froz8n.smart.viz.PathVizNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Clientbound handlers for the channels declared in {@link SoundWaveNetwork}.
 * The buffer is read on the network thread and the effect applied on the client
 * thread, which is what Forge's {@code consumerMainThread} did.
 */
@Environment(EnvType.CLIENT)
public final class SoundWaveClientNetwork {

    private SoundWaveClientNetwork() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.START,
                (client, handler, buf, sender) -> {
                    int playerId = buf.readVarInt();
                    client.execute(() -> SoundWaveRenderer.activate(playerId));
                });
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.STATUS,
                (client, handler, buf, sender) -> {
                    int mode = buf.readUnsignedByte();
                    int down = buf.readVarInt();
                    int up = buf.readVarInt();
                    client.execute(() -> JammerHud.update(mode, down, up));
                });
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.ROOTED,
                (client, handler, buf, sender) -> {
                    int ticks = buf.readVarInt();
                    client.execute(() -> RootedInputControl.rootFor(ticks));
                });
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.ROOT_BURST,
                (client, handler, buf, sender) -> {
                    int targetId = buf.readVarInt();
                    double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
                    long seed = buf.readLong();
                    int ticks = buf.readVarInt();
                    client.execute(() -> RootVisualClient.activate(targetId, x, y, z, seed, ticks));
                });
        ClientPlayNetworking.registerGlobalReceiver(PathVizNetwork.CHANNEL,
                (client, handler, buf, sender) -> {
                    var data = PathVizNetwork.decode(buf);
                    client.execute(() -> ClientPathStore.put(data));
                });
    }
}