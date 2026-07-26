package froz8n.combat;

import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class ZombieSerumSystem {
    private static final String UNTIL="smartmobs_zombie_serum_until";
    private ZombieSerumSystem(){}
    public static void apply(Player player){player.getPersistentData().putLong(UNTIL,player.level().getGameTime()+300);}
    public static boolean isMasked(Player player){return player.getPersistentData().getLongOr(UNTIL,0)>player.level().getGameTime();}
    public static void tickZombie(Zombie zombie){
        if(zombie.getTarget() instanceof Player player&&isMasked(player)){
            zombie.setTarget(null);zombie.getNavigation().stop();zombie.setAggressive(false);
        }
    }
    public static boolean preventAttack(LivingHurtEvent event){
        if(event.getEntity() instanceof Player player&&isMasked(player)
                &&(event.getSource().getEntity() instanceof Zombie
                ||event.getSource().getDirectEntity() instanceof Zombie))return true;
        return false;
    }
}
