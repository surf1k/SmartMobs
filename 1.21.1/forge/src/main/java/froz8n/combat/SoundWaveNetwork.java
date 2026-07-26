package froz8n.combat;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class SoundWaveNetwork {
    public record Start(int playerId) {}
    public record Status(int mode, int downTicks, int upTicks) {}
    public record SetMode(int mode) {}
    public record Rooted(int durationTicks) {}
    public record RootBurst(int targetId,double x,double y,double z,long seed,int durationTicks) {}
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath("smartmobs", "sound_wave"))
            .networkProtocolVersion(6).simpleChannel();
    private SoundWaveNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Start.class).direction(PacketFlow.CLIENTBOUND)
                .encoder((m,b)->b.writeVarInt(m.playerId()))
                .decoder(b->new Start(b.readVarInt()))
                .consumerMainThread((m,c)->{
                    froz8n.client.SoundWaveRenderer.activate(m.playerId());
                    c.setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Status.class).direction(PacketFlow.CLIENTBOUND)
                .encoder((m,b)->{b.writeByte(m.mode());b.writeVarInt(m.downTicks());b.writeVarInt(m.upTicks());})
                .decoder(b->new Status(b.readUnsignedByte(),b.readVarInt(),b.readVarInt()))
                .consumerMainThread((m,c)->{froz8n.client.JammerHud.update(m.mode(),m.downTicks(),m.upTicks());c.setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(SetMode.class).direction(PacketFlow.SERVERBOUND)
                .encoder((m,b)->b.writeByte(m.mode())).decoder(b->new SetMode(b.readUnsignedByte()))
                .consumerMainThread((m,c)->{
                    var player=c.getSender();
                    if(player!=null){player.getPersistentData().putInt("smartmobs_jammer_mode",m.mode()==1?1:0);sendStatus(player);}
                    c.setPacketHandled(true);
                }).add();
        CHANNEL.messageBuilder(Rooted.class).direction(PacketFlow.CLIENTBOUND)
                .encoder((m,b)->b.writeVarInt(m.durationTicks())).decoder(b->new Rooted(b.readVarInt()))
                .consumerMainThread((m,c)->{froz8n.client.RootedInputControl.rootFor(m.durationTicks());c.setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(RootBurst.class).direction(PacketFlow.CLIENTBOUND)
                .encoder((m,b)->{b.writeVarInt(m.targetId());b.writeDouble(m.x());b.writeDouble(m.y());b.writeDouble(m.z());b.writeLong(m.seed());b.writeVarInt(m.durationTicks());})
                .decoder(b->new RootBurst(b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),b.readLong(),b.readVarInt()))
                .consumerMainThread((m,c)->{froz8n.client.RootVisualClient.activate(m.targetId(),m.x(),m.y(),m.z(),m.seed(),m.durationTicks());c.setPacketHandled(true);}).add();
    }
    public static void send(Player player) {
        if(!(player instanceof net.minecraft.server.level.ServerPlayer sender))return;
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null)return;
        for(var receiver:server.getPlayerList().getPlayers()){
            var connection=receiver.connection.getConnection();
            if(CHANNEL.isRemotePresent(connection))CHANNEL.send(new Start(player.getId()),connection);
        }
    }
    public static void sendStatus(Player player){
        long now=System.currentTimeMillis();
        int down=(int)Math.min(Integer.MAX_VALUE,Math.max(0,froz8n.data.Nbt.getLongOr(player.getPersistentData(), "smartmobs_jammer_cooldown_until",0)-now));
        int up=(int)Math.min(Integer.MAX_VALUE,Math.max(0,froz8n.data.Nbt.getLongOr(player.getPersistentData(), "smartmobs_jammer_up_cooldown_until",0)-now));
        var receiver=(net.minecraft.server.level.ServerPlayer)player;
        var connection=receiver.connection.getConnection();
        if(CHANNEL.isRemotePresent(connection))CHANNEL.send(
                new Status(froz8n.data.Nbt.getIntOr(player.getPersistentData(), "smartmobs_jammer_mode",0),down,up),connection);
    }
    public static void setMode(int mode){CHANNEL.send(new SetMode(mode),PacketDistributor.SERVER.noArg());}
    public static void sendRooted(net.minecraft.server.level.ServerPlayer player,int ticks){
        var connection=player.connection.getConnection();
        if(CHANNEL.isRemotePresent(connection))CHANNEL.send(new Rooted(ticks),connection);
    }
    public static void sendRootBurst(net.minecraft.server.level.ServerLevel level,int targetId,double x,double y,double z,long seed,int ticks){
        for(var receiver:level.players()){
            if(receiver.distanceToSqr(x,y,z)>4096)continue;
            var connection=receiver.connection.getConnection();
            if(CHANNEL.isRemotePresent(connection))CHANNEL.send(new RootBurst(targetId,x,y,z,seed,ticks),connection);
        }
    }
}
