package froz8n.combat;

import froz8n.SmartMobs;
import froz8n.data.Nbt;
import froz8n.data.PersistentData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The jammer/roots channel. Forge's SimpleChannel becomes a set of vanilla
 * {@link CustomPacketPayload} records registered with Fabric's networking API; the
 * clientbound handlers live in {@code froz8n.client.SoundWaveClientNetwork} so no
 * client-only class is reachable from common code.
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

    /** Declares every payload on both sides and installs the one serverbound handler. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(Start.TYPE, Start.CODEC);
        PayloadTypeRegistry.playS2C().register(Status.TYPE, Status.CODEC);
        PayloadTypeRegistry.playS2C().register(Rooted.TYPE, Rooted.CODEC);
        PayloadTypeRegistry.playS2C().register(RootBurst.TYPE, RootBurst.CODEC);
        PayloadTypeRegistry.playC2S().register(SetMode.TYPE, SetMode.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetMode.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            PersistentData.of(player).putInt("smartmobs_jammer_mode", payload.mode() == 1 ? 1 : 0);
            sendStatus(player);
        });
    }

    public static void send(Player player) {
        if (!(player instanceof ServerPlayer sender)) return;
        MinecraftServer server = sender.level().getServer();
        if (server == null) return;
        Start payload = new Start(player.getId());
        for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(receiver, Start.TYPE)) ServerPlayNetworking.send(receiver, payload);
        }
    }

    public static void sendStatus(Player player) {
        if (!(player instanceof ServerPlayer receiver)) return;
        long now = System.currentTimeMillis();
        int down = (int) Math.min(Integer.MAX_VALUE, Math.max(0,
                Nbt.getLongOr(PersistentData.of(player), "smartmobs_jammer_cooldown_until", 0) - now));
        int up = (int) Math.min(Integer.MAX_VALUE, Math.max(0,
                Nbt.getLongOr(PersistentData.of(player), "smartmobs_jammer_up_cooldown_until", 0) - now));
        if (ServerPlayNetworking.canSend(receiver, Status.TYPE)) {
            ServerPlayNetworking.send(receiver, new Status(
                    Nbt.getIntOr(PersistentData.of(player), "smartmobs_jammer_mode", 0), down, up));
        }
    }

    public static void sendRooted(ServerPlayer player, int ticks) {
        if (ServerPlayNetworking.canSend(player, Rooted.TYPE)) {
            ServerPlayNetworking.send(player, new Rooted(ticks));
        }
    }

    public static void sendRootBurst(ServerLevel level, int targetId, double x, double y, double z,
                                     long seed, int ticks) {
        RootBurst payload = new RootBurst(targetId, x, y, z, seed, ticks);
        for (ServerPlayer receiver : level.players()) {
            if (receiver.distanceToSqr(x, y, z) > 4096) continue;
            if (ServerPlayNetworking.canSend(receiver, RootBurst.TYPE)) {
                ServerPlayNetworking.send(receiver, payload);
            }
        }
    }
}
