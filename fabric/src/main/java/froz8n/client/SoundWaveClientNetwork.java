package froz8n.client;

import froz8n.combat.SoundWaveNetwork;
import froz8n.smart.viz.ClientPathStore;
import froz8n.smart.viz.PathVizNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Clientbound handlers for the payloads declared in {@link SoundWaveNetwork}.
 * Fabric runs them on the client thread, like Forge's {@code consumerMainThread}.
 */
@Environment(EnvType.CLIENT)
public final class SoundWaveClientNetwork {

    private SoundWaveClientNetwork() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.Start.TYPE,
                (payload, context) -> SoundWaveRenderer.activate(payload.playerId()));
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.Status.TYPE,
                (payload, context) -> JammerHud.update(payload.mode(), payload.downTicks(), payload.upTicks()));
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.Rooted.TYPE,
                (payload, context) -> RootedInputControl.rootFor(payload.durationTicks()));
        ClientPlayNetworking.registerGlobalReceiver(SoundWaveNetwork.RootBurst.TYPE,
                (payload, context) -> RootVisualClient.activate(payload.targetId(), payload.x(), payload.y(),
                        payload.z(), payload.seed(), payload.durationTicks()));
        ClientPlayNetworking.registerGlobalReceiver(PathVizNetwork.Payload.TYPE,
                (payload, context) -> ClientPathStore.put(payload.data()));
    }
}
