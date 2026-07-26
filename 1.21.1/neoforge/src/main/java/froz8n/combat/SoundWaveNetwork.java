package froz8n.combat;

import froz8n.SmartMobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The jammer/roots channel. Forge's SimpleChannel becomes a set of vanilla
 * {@link CustomPacketPayload} records; the clientbound handlers are installed by
 * {@code froz8n.client.SmartMobsClient} so no client-only class is reachable from
 * common code.
 */
public final class SoundWaveNetwork {

    public record Start(int playerId) implements CustomPacketPayload {
        public static final Type<Start> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(SmartMobs.MODID, "sound_wave_start"));
        public static final StreamCodec<FriendlyByteBuf, Start> CODEC = CustomPacketPayload.codec(
                (m, b) -> b.writeVarInt(m.playerId()),
                b -> new Start(b.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Status(int mode, int downTicks, int upTicks) implements CustomPacketPayload {
        public static final Type<Status> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(SmartMobs.MODID, "sound_wave_status"));
        public static final StreamCodec<FriendlyByteBuf, Status> CODEC = CustomPacketPayload.codec(
                (m, b) -> { b.writeByte(m.mode()); b.writeVarInt(m.downTicks()); b.writeVarInt(m.upTicks()); },
                b -> new Status(b.readUnsignedByte(), b.readVarInt(), b.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SetMode(int mode) implements CustomPacketPayload {
        public static final Type<SetMode> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(SmartMobs.MODID, "sound_wave_set_mode"));
        public static final StreamCodec<FriendlyByteBuf, SetMode> CODEC = CustomPacketPayload.codec(
                (m, b) -> b.writeByte(m.mode()),
                b -> new SetMode(b.readUnsignedByte()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Rooted(int durationTicks) implements CustomPacketPayload {
        public static final Type<Rooted> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(SmartMobs.MODID, "sound_wave_rooted"));
        public static final StreamCodec<FriendlyByteBuf, Rooted> CODEC = CustomPacketPayload.codec(
                (m, b) -> b.writeVarInt(m.durationTicks()),
                b -> new Rooted(b.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record RootBurst(int targetId, double x, double y, double z, long seed, int durationTicks)
            implements CustomPacketPayload {
        public static final Type<RootBurst> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(SmartMobs.MODID, "sound_wave_root_burst"));
        public static final StreamCodec<FriendlyByteBuf, RootBurst> CODEC = CustomPacketPayload.codec(
                (m, b) -> { b.writeVarInt(m.targetId()); b.writeDouble(m.x()); b.writeDouble(m.y());
                            b.writeDouble(m.z()); b.writeLong(m.seed()); b.writeVarInt(m.durationTicks()); },
                b -> new RootBurst(b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(),
                        b.readLong(), b.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private SoundWaveNetwork() {}

    /** Mod-bus handler: declares every payload and the one serverbound handler. */
    public static void register(RegisterPayloadHandlersEvent event) {
        // NeoForge 21.1 has no handler-less playToClient overload and no separate
        // client-payload event: the clientbound handler goes here. Each lambda body only
        // resolves its client class when it actually runs, so a dedicated server never
        // loads one.
        PayloadRegistrar registrar = event.registrar("6").optional();
        registrar.playToClient(Start.TYPE, Start.CODEC, (payload, context) ->
                context.enqueueWork(() -> froz8n.client.SoundWaveRenderer.activate(payload.playerId())));
        registrar.playToClient(Status.TYPE, Status.CODEC, (payload, context) ->
                context.enqueueWork(() -> froz8n.client.JammerHud.update(
                        payload.mode(), payload.downTicks(), payload.upTicks())));
        registrar.playToClient(Rooted.TYPE, Rooted.CODEC, (payload, context) ->
                context.enqueueWork(() -> froz8n.client.RootedInputControl.rootFor(payload.durationTicks())));
        registrar.playToClient(RootBurst.TYPE, RootBurst.CODEC, (payload, context) ->
                context.enqueueWork(() -> froz8n.client.RootVisualClient.activate(payload.targetId(),
                        payload.x(), payload.y(), payload.z(), payload.seed(), payload.durationTicks())));
        registrar.playToServer(SetMode.TYPE, SetMode.CODEC, SoundWaveNetwork::handleSetMode);
        registrar.playToClient(froz8n.smart.viz.PathVizNetwork.Payload.TYPE,
                froz8n.smart.viz.PathVizNetwork.Payload.CODEC, (payload, context) ->
                        context.enqueueWork(() -> froz8n.smart.viz.ClientPathStore.put(payload.data())));
    }

    private static void handleSetMode(SetMode payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            player.getPersistentData().putInt("smartmobs_jammer_mode", payload.mode() == 1 ? 1 : 0);
            sendStatus(player);
        });
    }

    public static void send(Player player) {
        if (!(player instanceof ServerPlayer)) return;
        PacketDistributor.sendToAllPlayers(new Start(player.getId()));
    }

    public static void sendStatus(Player player) {
        if (!(player instanceof ServerPlayer receiver)) return;
        long now = System.currentTimeMillis();
        int down = (int) Math.min(Integer.MAX_VALUE, Math.max(0,
                froz8n.data.Nbt.getLongOr(player.getPersistentData(), "smartmobs_jammer_cooldown_until", 0) - now));
        int up = (int) Math.min(Integer.MAX_VALUE, Math.max(0,
                froz8n.data.Nbt.getLongOr(player.getPersistentData(), "smartmobs_jammer_up_cooldown_until", 0) - now));
        PacketDistributor.sendToPlayer(receiver, new Status(
                froz8n.data.Nbt.getIntOr(player.getPersistentData(), "smartmobs_jammer_mode", 0), down, up));
    }

    public static void sendRooted(ServerPlayer player, int ticks) {
        PacketDistributor.sendToPlayer(player, new Rooted(ticks));
    }

    public static void sendRootBurst(ServerLevel level, int targetId, double x, double y, double z,
                                     long seed, int ticks) {
        RootBurst payload = new RootBurst(targetId, x, y, z, seed, ticks);
        for (ServerPlayer receiver : level.players()) {
            if (receiver.distanceToSqr(x, y, z) > 4096) continue;
            PacketDistributor.sendToPlayer(receiver, payload);
        }
    }
}
