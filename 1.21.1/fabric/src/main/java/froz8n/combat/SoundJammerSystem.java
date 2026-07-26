package froz8n.combat;

import froz8n.data.Nbt;
import froz8n.data.PersistentData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class SoundJammerSystem {
    private static final String STUN_UNTIL = "smartmobs_stun_until";
    private static final String FORCED = "smartmobs_jammer_forced";
    private static final String FEAR_UNTIL = "smartmobs_fear_until";
    private static final String FEAR_X = "smartmobs_fear_x";
    private static final String FEAR_Z = "smartmobs_fear_z";
    private static final int FIELD_TICKS = 60;
    private static final int AFTER_TICKS = 20;
    private static final double RADIUS = 5.0;
    private record Field(long started, long ends) {}
    private static final Map<UUID, Field> ACTIVE = new HashMap<>();

    private SoundJammerSystem() {}

    public static void activate(Player player) {
        long now = player.level().getGameTime();
        ACTIVE.put(player.getUUID(), new Field(now, now + FIELD_TICKS));
    }

    public static void activateFear(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        long until=level.getGameTime()+100;
        AABB area=player.getBoundingBox().inflate(10.0);
        for(Zombie zombie:level.getEntitiesOfClass(Zombie.class,area,
                z->z.isAlive()&&z.distanceToSqr(player)<=100.0)){
            PersistentData.of(zombie).putLong(FEAR_UNTIL,until);
            PersistentData.of(zombie).putDouble(FEAR_X,player.getX());
            PersistentData.of(zombie).putDouble(FEAR_Z,player.getZ());
            zombie.setTarget(null);
        }
    }

    public static void tickFields(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Field>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Field> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive() || player.level().getGameTime() >= entry.getValue().ends) {
                it.remove();
                continue;
            }
            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();
            long age = now - entry.getValue().started;
            long stunUntil = entry.getValue().ends + AFTER_TICKS;
            emitSound(level, player, age);
            AABB area = player.getBoundingBox().inflate(RADIUS);
            for (Zombie zombie : level.getEntitiesOfClass(Zombie.class, area,
                    z -> z.isAlive() && z.distanceToSqr(player) <= RADIUS * RADIUS)) {
                PersistentData.of(zombie).putLong(STUN_UNTIL, stunUntil);
                PersistentData.of(zombie).putBoolean(FORCED, true);
            }
        }
    }

    private static void emitSound(ServerLevel level, ServerPlayer player, long age) {
        // A low electronic drone is refreshed often enough to sound continuous.
        if (age % 12 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_AMBIENT,
                    SoundSource.PLAYERS, 0.75F, 0.58F);
        }
        if (age == 0 || age == 20 || age == 40) {
            level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.PLAYERS, 0.72F, 1.25F);
        }
    }

    public static boolean isStunned(Zombie zombie) {
        return Nbt.getLongOr(PersistentData.of(zombie), STUN_UNTIL, 0L) > zombie.level().getGameTime();
    }
    public static boolean isFeared(Zombie zombie){return Nbt.getLongOr(PersistentData.of(zombie), FEAR_UNTIL,0)>zombie.level().getGameTime();}
    public static boolean isControlled(Zombie zombie){return isStunned(zombie)||isFeared(zombie);}
    /** @return {@code true} when a panicking zombie is the attacker, so the hit deals nothing. */
    public static boolean suppressFearedAttack(DamageSource source){
        return source.getEntity() instanceof Zombie z&&isFeared(z);
    }

    public static void tickZombie(Zombie zombie) {
        boolean stunned = isStunned(zombie);
        boolean forced = Nbt.getBooleanOr(PersistentData.of(zombie), FORCED, false);
        if (stunned) {
            zombie.setTarget(null);
            zombie.getNavigation().stop();
            zombie.setSprinting(false);
            zombie.setDeltaMovement(Vec3.ZERO);
            zombie.setNoAi(true);
            zombie.setNoGravity(true);
            zombie.noPhysics = true;
        } else if (forced) {
            zombie.setNoAi(false);
            zombie.setNoGravity(false);
            zombie.noPhysics = false;
            PersistentData.of(zombie).remove(FORCED);
            PersistentData.of(zombie).remove(STUN_UNTIL);
        }
        if(isFeared(zombie)){
            zombie.setTarget(null);
            zombie.setNoAi(false);
            zombie.setNoGravity(false);
            zombie.noPhysics=false;
            if((zombie.tickCount+zombie.getId())%8==0||zombie.getNavigation().isDone()){
                double fx=Nbt.getDoubleOr(PersistentData.of(zombie), FEAR_X,zombie.getX());
                double fz=Nbt.getDoubleOr(PersistentData.of(zombie), FEAR_Z,zombie.getZ());
                double dx=zombie.getX()-fx,dz=zombie.getZ()-fz,len=Math.max(.01,Math.sqrt(dx*dx+dz*dz));
                zombie.getNavigation().moveTo(zombie.getX()+dx/len*14,zombie.getY(),zombie.getZ()+dz/len*14,1.35);
            }
        }
    }
}
