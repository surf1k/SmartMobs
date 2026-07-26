package froz8n.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class GardenZombieSystem {
    private static final String COOLDOWN="smartmobs_roots_cooldown";
    private static final String LAST_DISTANCE="smartmobs_roots_last_distance";
    private static final String ROOT_UNTIL="smartmobs_rooted_until";
    private static final String CHARGE_COOLDOWN="smartmobs_garden_charge_cooldown";
    private static final String CHARGE_RECOVERY_UNTIL="smartmobs_garden_charge_recovery";
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private static final Map<UUID,Charge> CHARGES=new HashMap<>();
    private record Pending(UUID player,long expires){}
    private record Charge(ZombieHorse horse,UUID rider,UUID target,long end){}
    private GardenZombieSystem(){}

    public static void tickGarden(Zombie zombie){
        if(!(zombie.level() instanceof ServerLevel level))return;
        maintainHat(zombie);

        // Garden zombies sense players in a 128 block sphere even through walls.
        // The two active skills below still retain their own strict LOS checks.
        if((zombie.tickCount+zombie.getId())%10==0){
            Player sensed=nearest(level,zombie);
            if(sensed!=null&&!ZombieSerumSystem.isMasked(sensed))zombie.setTarget(sensed);
        }
        if((zombie.tickCount+zombie.getId())%40==0){
            var follow=zombie.getAttribute(Attributes.FOLLOW_RANGE);
            if(follow!=null)follow.setBaseValue(128);
        }

        if(zombie.isPassenger())return;

        Pending pending=PENDING.get(zombie.getUUID());
        if(pending!=null){
            Player player=level.getPlayerByUUID(pending.player);
            if(player==null||level.getGameTime()>pending.expires||zombie.distanceToSqr(player)>900
                    ||!strictLineOfSight(level,zombie,player)){
                PENDING.remove(zombie.getUUID());
            }else if(player.onGround()){
                PENDING.remove(zombie.getUUID());
                activateRoots(level,zombie,player);
            }
            return;
        }
        if(zombie.tickCount%3!=Math.floorMod(zombie.getId(),3))return;
        Player player=zombie.getTarget() instanceof Player p?p:nearest(level,zombie);
        if(player==null||ZombieSerumSystem.isMasked(player))return;
        double distance=zombie.distanceTo(player);
        double previous=zombie.getPersistentData().getDoubleOr(LAST_DISTANCE,distance);
        zombie.getPersistentData().putDouble(LAST_DISTANCE,distance);
        if(distance<=30&&distance>=4&&level.getGameTime()>=zombie.getPersistentData().getLongOr(CHARGE_COOLDOWN,0)
                &&froz8n.smart.SmartMobWorldRules.canUseOutdoorNightBehavior(level,zombie.blockPosition().above())&&player.onGround()
                &&strictLineOfSight(level,zombie,player)){
            startCharge(level,zombie,player);
            return;
        }
        if(distance>30||level.getGameTime()<zombie.getPersistentData().getLongOr(COOLDOWN,0)
                ||!strictLineOfSight(level,zombie,player))return;
        Vec3 horizontal=player.getDeltaMovement().multiply(1,0,1);
        Vec3 away=new Vec3(player.getX()-zombie.getX(),0,player.getZ()-zombie.getZ());
        double awaySpeed=away.lengthSqr()<.01?0:horizontal.dot(away.normalize());
        if(horizontal.lengthSqr()<=.0064||awaySpeed<=.055||distance<=previous+.025)return;
        if(player.onGround())activateRoots(level,zombie,player);
        else PENDING.put(zombie.getUUID(),new Pending(player.getUUID(),level.getGameTime()+40));
    }

    private static void startCharge(ServerLevel level,Zombie zombie,Player player){
        ZombieHorse horse=EntityType.ZOMBIE_HORSE.create(level,EntitySpawnReason.MOB_SUMMONED);
        if(horse==null)return;
        horse.snapTo(zombie.getX(),zombie.getY(),zombie.getZ(),zombie.getYRot(),0);
        horse.setPersistenceRequired();
        horse.setItemSlot(EquipmentSlot.SADDLE,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
        var movement=horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if(movement!=null)movement.setBaseValue(.45);
        var step=horse.getAttribute(Attributes.STEP_HEIGHT);
        if(step!=null)step.setBaseValue(1.15);
        var health=horse.getAttribute(Attributes.MAX_HEALTH);
        if(health!=null){health.setBaseValue(60);horse.setHealth(60);}
        level.addFreshEntity(horse);
        zombie.startRiding(horse,true,true);
        long now=level.getGameTime();
        zombie.getPersistentData().putLong(CHARGE_COOLDOWN,now+600);
        CHARGES.put(horse.getUUID(),new Charge(horse,zombie.getUUID(),player.getUUID(),now+100));
        level.playSound(null,horse.blockPosition(),SoundEvents.HORSE_SADDLE.value(),SoundSource.HOSTILE,1.4F,.85F);
        level.sendParticles(ParticleTypes.CLOUD,horse.getX(),horse.getY()+.2,horse.getZ(),18,.5,.15,.5,.05);
    }

    public static void tickCharges(){
        Iterator<Charge> it=CHARGES.values().iterator();
        while(it.hasNext()){
            Charge charge=it.next();ZombieHorse horse=charge.horse;
            if(horse.isRemoved()){it.remove();continue;}
            if(!(horse.level() instanceof ServerLevel level)){horse.discard();it.remove();continue;}
            if(level.getGameTime()>=charge.end){finishCharge(level,charge);it.remove();continue;}
            Player target=level.getPlayerByUUID(charge.target);
            if(target==null||!target.isAlive()){finishCharge(level,charge);it.remove();continue;}
            // Reaching the player ends the cinematic charge immediately. The rider
            // gets a short recovery window, so it cannot land a cheap mounted hit.
            if(horse.distanceToSqr(target)<=2.25){finishCharge(level,charge);it.remove();continue;}
            if(horse.getRemainingFireTicks()>0)horse.clearFire();
            Vec3 flat=new Vec3(target.getX()-horse.getX(),0,target.getZ()-horse.getZ());
            if(flat.lengthSqr()<.01)continue;
            Vec3 direction=flat.normalize();
            horse.setYRot((float)(net.minecraft.util.Mth.atan2(-direction.x,direction.z)*net.minecraft.util.Mth.RAD_TO_DEG));
            horse.setYBodyRot(horse.getYRot());
            breakChargeObstacles(level,horse,direction);
            Vec3 old=horse.getDeltaMovement();
            horse.setDeltaMovement(direction.x*.48,old.y,direction.z*.48);
            horse.hurtMarked=true;
            if((level.getGameTime()&7)==0)
                level.playSound(null,horse.blockPosition(),SoundEvents.HORSE_GALLOP,SoundSource.HOSTILE,1.1F,.8F);
        }
    }

    private static void finishCharge(ServerLevel level,Charge charge){
        Zombie rider=level.getEntity(charge.rider) instanceof Zombie z?z:null;
        Player target=level.getPlayerByUUID(charge.target);
        charge.horse.ejectPassengers();
        charge.horse.discard();
        if(rider!=null){
            rider.getPersistentData().putLong(CHARGE_RECOVERY_UNTIL,level.getGameTime()+4);
            rider.setDeltaMovement(Vec3.ZERO);
            rider.getNavigation().stop();
            if(target!=null&&target.isAlive()){
                rider.setTarget(target);rider.setAggressive(true);
                rider.getLookControl().setLookAt(target,90,90);
                rider.getNavigation().moveTo(target,1.25);
            }
        }
        level.sendParticles(ParticleTypes.CLOUD,charge.horse.getX(),charge.horse.getY()+.5,
                charge.horse.getZ(),14,.45,.3,.45,.035);
    }

    public static boolean suppressChargeAttack(net.minecraftforge.event.entity.living.LivingHurtEvent event){
        if(event.getSource().getEntity() instanceof Zombie zombie
                &&zombie.getPersistentData().getLongOr(CHARGE_RECOVERY_UNTIL,0)>zombie.level().getGameTime())return true;
        if(event.getSource().getEntity() instanceof Zombie zombie&&zombie.getVehicle() instanceof ZombieHorse)return true;
        return false;
    }

    private static void breakChargeObstacles(ServerLevel level,ZombieHorse horse,Vec3 direction){
        net.minecraft.world.phys.AABB box=horse.getBoundingBox().move(direction.scale(1.15)).inflate(.18,.1,.18);
        // Preserve terrain and one-block steps; only clear blocks intersecting
        // the horse/rider above foot level.
        int floor=net.minecraft.util.Mth.floor(horse.getY()+.15);
        for(BlockPos pos:BlockPos.betweenClosed(
                net.minecraft.util.Mth.floor(box.minX),floor,net.minecraft.util.Mth.floor(box.minZ),
                net.minecraft.util.Mth.floor(box.maxX),net.minecraft.util.Mth.floor(box.maxY),net.minecraft.util.Mth.floor(box.maxZ))){
            var state=level.getBlockState(pos);
            float hardness=state.getDestroySpeed(level,pos);
            if(pos.getY()==floor&&!level.getBlockState(pos.above()).blocksMotion())continue;
            if(state.isAir()||!state.getFluidState().isEmpty()||!state.blocksMotion()||hardness<0||state.hasBlockEntity())continue;
            level.destroyBlock(pos,false,horse,512);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,state),
                    pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,8,.3,.3,.3,.08);
        }
    }

    private static void activateRoots(ServerLevel level,Zombie zombie,Player player){
        if(!player.onGround()||!strictLineOfSight(level,zombie,player))return;
        BlockPos ground=player.blockPosition().below();
        if(!level.getBlockState(ground).is(net.minecraft.tags.BlockTags.DIRT))return;
        long until=level.getGameTime()+60;
        player.getPersistentData().putLong(ROOT_UNTIL,until);
        zombie.getPersistentData().putLong(COOLDOWN,until+600+zombie.getRandom().nextInt(301));
        if(player instanceof ServerPlayer sp)SoundWaveNetwork.sendRooted(sp,60);
        level.playSound(null,ground,SoundEvents.ROOTED_DIRT_BREAK,SoundSource.HOSTILE,1.35F,.62F);
        level.playSound(null,ground,SoundEvents.MANGROVE_ROOTS_BREAK,SoundSource.HOSTILE,.9F,.78F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,level.getBlockState(ground)),
                player.getX(),player.getY()+.03,player.getZ(),32,.7,.04,.7,.075);
        SoundWaveNetwork.sendRootBurst(level,player.getId(),player.getX(),player.getY(),player.getZ(),level.random.nextLong(),60);
    }

    public static void tickRooted(Player player){
        if(player.getPersistentData().getLongOr(ROOT_UNTIL,0)<=player.level().getGameTime())return;
        player.setSprinting(false);
        Vec3 m=player.getDeltaMovement();
        player.setDeltaMovement(0,Math.min(0,m.y),0);
    }

    private static boolean strictLineOfSight(ServerLevel level,Zombie zombie,Player player){
        if(zombie.level()!=player.level()||zombie.distanceToSqr(player)>900)return false;
        HitResult hit=level.clip(new ClipContext(zombie.getEyePosition(),player.getEyePosition(),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,zombie));
        return hit.getType()==HitResult.Type.MISS&&zombie.hasLineOfSight(player);
    }

    private static void maintainHat(Zombie zombie){
        if((zombie.tickCount+zombie.getId())%40==0
                &&!zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(froz8n.smartmobs.GARDEN_HAT.get())){
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(froz8n.smartmobs.GARDEN_HAT.get()));
            zombie.setDropChance(net.minecraft.world.entity.EquipmentSlot.HEAD,.05F);
        }
    }
    private static Player nearest(ServerLevel level,Zombie zombie){
        Player best=null;double d=128D*128D;
        for(Player p:level.players())if(p.isAlive()&&!p.isSpectator()&&!p.isCreative()){
            double q=zombie.distanceToSqr(p);if(q<d){d=q;best=p;}
        }return best;
    }
    public static void tickRootVisuals(){}
    private static double findGroundY(ServerLevel level,double x,double fromY,double z){
        int y=net.minecraft.util.Mth.floor(fromY),min=level.getMinY()+1;
        while(y>min){BlockPos below=BlockPos.containing(x,y-1,z);
            if(!level.getBlockState(below).getCollisionShape(level,below).isEmpty())return y;y--;}
        return fromY;
    }
}
