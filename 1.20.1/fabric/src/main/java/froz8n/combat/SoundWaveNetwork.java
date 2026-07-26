package froz8n.combat;

import froz8n.SmartMobs;
import froz8n.data.Nbt;
import froz8n.data.PersistentData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The jammer/roots channel. 1.20.1 predates CustomPacketPayload, so each message is a
 * channel id plus a hand-written FriendlyByteBuf; the records survive only as the
 * in-process shape of a message. The clientbound handlers live in
 * {@code froz8n.client.SoundWaveClientNetwork} so no client-only class is reachable
 * from common code.
 */
public final class SoundWaveNetwork {

    public static final ResourceLocation START = new ResourceLocation(SmartMobs.MODID, "sound_wave_start");
    public static final ResourceLocation STATUS = new ResourceLocation(SmartMobs.MODID, "sound_wave_status");
    public static final ResourceLocation SET_MODE = new ResourceLocation(SmartMobs.MODID, "sound_wave_set_mode");
    public static final ResourceLocation ROOTED = new ResourceLocation(SmartMobs.MODID, "sound_wave_rooted");
    public static final ResourceLocation ROOT_BURST = new ResourceLocation(SmartMobs.MODID, "sound_wave_root_burst");

    private SoundWaveNetwork() {}

    /** Installs the one serverbound handler. Clientbound ids need no declaration here. */
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SET_MODE, (server, player, handler, buf, sender) -> {
            int mode = buf.readUnsignedByte() == 1 ? 1 : 0;
            server.execute(() -> {
                PersistentData.of(player).putInt("smartmobs_jammer_mode", mode);
                sendStatus(player);
            });
        });
    }

    public static void send(Player player) {
        if (!(player instanceof ServerPlayer sender)) return;
        MinecraftServer server = sender.level().getServer();
        if (server == null) return;
        for (ServerPlayer receiver : server.getPlayerList().getPlayers()) {
            if (!ServerPlayNetworking.canSend(receiver, START)) continue;
            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            buf.writeVarInt(player.getId());
            ServerPlayNetworking.send(receiver, START, buf);
        }
    }

    public static void sendStatus(Player player) {
        if (!(player instanceof ServerPlayer receiver)) return;
        if (!ServerPlayNetworking.canSend(receiver, STATUS)) return;
        long now = System.currentTimeMillis();
        int down = (int) Math.min(Integer.MAX_VALUE, Math.max(0,
                Nbt.getLongOr(PersistentData.of(player), "smartmobs_jammer_cooldown_until", 0) - now));
        int up = (int) Math.min(Integer.MAX_VALUE, Math.max(0,
                Nbt.getLongOr(PersistentData.of(player), "smartmobs_jammer_up_cooldown_until", 0) - now));
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeByte(Nbt.getIntOr(PersistentData.of(player), "smartmobs_jammer_mode", 0));
        buf.writeVarInt(down);
        buf.writeVarInt(up);
        ServerPlayNetworking.send(receiver, STATUS, buf);
    }

    public static void sendRooted(ServerPlayer player, int ticks) {
        if (!ServerPlayNetworking.canSend(player, ROOTED)) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(ticks);
        ServerPlayNetworking.send(player, ROOTED, buf);
    }

    public static void sendRootBurst(ServerLevel level, int targetId, double x, double y, double z,
                                     long seed, int ticks) {
        for (ServerPlayer receiver : level.players()) {
            if (receiver.distanceToSqr(x, y, z) > 4096) continue;
            if (!ServerPlayNetworking.canSend(receiver, ROOT_BURST)) continue;
            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            buf.writeVarInt(targetId);
            buf.writeDouble(x); buf.writeDouble(y); buf.writeDouble(z);
            buf.writeLong(seed);
            buf.writeVarInt(ticks);
            ServerPlayNetworking.send(receiver, ROOT_BURST, buf);
        }
    }
}