package froz8n.combat;

import froz8n.data.PersistentData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;

public final class ZombieSerumSystem {
    private static final String UNTIL="smartmobs_zombie_serum_until";
    private ZombieSerumSystem(){}
    public static void apply(Player player){PersistentData.of(player).putLong(UNTIL,player.level().getGameTime()+300);}
    public static boolean isMasked(Player player){return PersistentData.of(player).getLongOr(UNTIL,0)>player.level().getGameTime();}
    public static void tickZombie(Zombie zombie){
        if(zombie.getTarget() instanceof Player player&&isMasked(player)){
            zombie.setTarget(null);zombie.getNavigation().stop();zombie.setAggressive(false);
        }
    }
    /** @return {@code true} when a masked player is being hit by a zombie, so the hit is denied. */
    public static boolean preventAttack(LivingEntity victim,DamageSource source){
        return victim instanceof Player player&&isMasked(player)
                &&(source.getEntity() instanceof Zombie||source.getDirectEntity() instanceof Zombie);
    }
}
