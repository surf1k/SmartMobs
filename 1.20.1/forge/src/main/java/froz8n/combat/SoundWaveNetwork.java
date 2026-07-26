package froz8n.combat;

import froz8n.SmartMobs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * The jammer/roots channel. Forge 47 still uses a hand-numbered SimpleChannel, so each
 * message is a record plus an explicit encoder, decoder and main-thread consumer.
 */
public final class SoundWaveNetwork {
    public record Start(int playerId) {}
    public record Status(int mode, int downTicks, int upTicks) {}
    public record SetMode(int mode) {}
    public record Rooted(int durationTicks) {}
    public record RootBurst(int targetId,double x,double y,double z,long seed,int durationTicks) {}

    private static final String PROTOCOL = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SmartMobs.MODID, "sound_wave"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private SoundWaveNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(Start.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m,b)->b.writeVarInt(m.playerId()))
                .decoder(b->new Start(b.readVarInt()))
                .consumerMainThread((m,c)->{
                    froz8n.client.SoundWaveRenderer.activate(m.playerId());
                    c.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Status.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m,b)->{b.writeByte(m.mode());b.writeVarInt(m.downTicks());b.writeVarInt(m.upTicks());})
                .decoder(b->new Status(b.readUnsignedByte(),b.readVarInt(),b.readVarInt()))
                .consumerMainThread((m,c)->{
                    froz8n.client.JammerHud.update(m.mode(),m.downTicks(),m.upTicks());
                    c.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(SetMode.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder((m,b)->b.writeByte(m.mode()))
                .decoder(b->new SetMode(b.readUnsignedByte()))
                .consumerMainThread((m,c)->{
                    var player=c.get().getSender();
                    if(player!=null){
                        player.getPersistentData().putInt("smartmobs_jammer_mode",m.mode()==1?1:0);
                        sendStatus(player);
                    }
                    c.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Rooted.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m,b)->b.writeVarInt(m.durationTicks()))
                .decoder(b->new Rooted(b.readVarInt()))
                .consumerMainThread((m,c)->{
                    froz8n.client.RootedInputControl.rootFor(m.durationTicks());
                    c.get().setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(RootBurst.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m,b)->{b.writeVarInt(m.targetId());b.writeDouble(m.x());b.writeDouble(m.y());
                                 b.writeDouble(m.z());b.writeLong(m.seed());b.writeVarInt(m.durationTicks());})
                .decoder(b->new RootBurst(b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),
                        b.readLong(),b.readVarInt()))
                .consumerMainThread((m,c)->{
                    froz8n.client.RootVisualClient.activate(m.targetId(),m.x(),m.y(),m.z(),m.seed(),m.durationTicks());
                    c.get().setPacketHandled(true);
                }).add();
        froz8n.smart.viz.PathVizNetwork.register(CHANNEL, id);
    }

    public static void send(Player player) {
        if(!(player instanceof net.minecraft.server.level.ServerPlayer))return;
        CHANNEL.send(PacketDistributor.ALL.noArg(), new Start(player.getId()));
    }

    public static void sendStatus(Player player){
        if(!(player instanceof net.minecraft.server.level.ServerPlayer receiver))return;
        long now=System.currentTimeMillis();
        int down=(int)Math.min(Integer.MAX_VALUE,Math.max(0,
                froz8n.data.Nbt.getLongOr(player.getPersistentData(),"smartmobs_jammer_cooldown_until",0)-now));
        int up=(int)Math.min(Integer.MAX_VALUE,Math.max(0,
                froz8n.data.Nbt.getLongOr(player.getPersistentData(),"smartmobs_jammer_up_cooldown_until",0)-now));
        CHANNEL.send(PacketDistributor.PLAYER.with(()->receiver), new Status(
                froz8n.data.Nbt.getIntOr(player.getPersistentData(),"smartmobs_jammer_mode",0),down,up));
    }

    /** Called from the client key handler. */
    public static void setMode(int mode){ CHANNEL.send(PacketDistributor.SERVER.noArg(), new SetMode(mode)); }

    public static void sendRooted(net.minecraft.server.level.ServerPlayer player,int ticks){
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player), new Rooted(ticks));
    }

    public static void sendRootBurst(net.minecraft.server.level.ServerLevel level,int targetId,
                                     double x,double y,double z,long seed,int ticks){
        RootBurst payload=new RootBurst(targetId,x,y,z,seed,ticks);
        for(var receiver:level.players()){
            if(receiver.distanceToSqr(x,y,z)>4096)continue;
            CHANNEL.send(PacketDistributor.PLAYER.with(()->receiver), payload);
        }
    }

    /** Lets the path-viz channel reuse this one. */
    public static SimpleChannel channel(){ return CHANNEL; }

    static FriendlyByteBuf unused(FriendlyByteBuf buf){ return buf; }
}