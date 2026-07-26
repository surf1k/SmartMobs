package froz8n.smart;

import com.mojang.brigadier.CommandDispatcher;
import froz8n.SmartMobs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * All gameplay (NeoForge game-bus) event handlers for SmartMobs: the {@code /spawnsmart}
 * command, the per-tick AI driver, temporary-block ageing and the no-drop rule for
 * mob-placed blocks.
 */
public final class SmartMobsEvents {

    /** Persistent-data flag marking an entity as "smart". */
    public static final String SMART_KEY = "smartmobs_smart";
    public static final String GARDEN_KEY = "smartmobs_garden_zombie";
    private static final String LEGACY_BOX_KEY = "smartmobs_box_zombie";
    private static final String LEGACY_BOX_SHIELD_KEY = "smartmobs_box_shield";
    /** Direct water pursuit velocity. Used by smart and regular zombies. */
    public static final double SWIM_SPEED = 0.15D;

    private SmartMobsEvents() {
    }

    /** Wire all gameplay handlers onto the NeoForge game bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(SmartMobsEvents::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(SmartMobsEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(SmartMobsEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SmartMobsEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SmartMobsEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(SmartMobsEvents::onBlockBreak);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("spawnsmart")
                        // 1.21.11 replaced source.hasPermission(int) with the PermissionCheck API.
                        // LEVEL_GAMEMASTERS corresponds to the old op permission level 2.
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("zombie")
                                .executes(ctx -> spawnSmartZombie(ctx.getSource())))
        );
    }

    private static int spawnSmartZombie(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Vec3 p = source.getPosition();
        BlockPos pos = BlockPos.containing(p);

        Entity entity = EntityType.ZOMBIE.spawn(level, pos, MobSpawnType.COMMAND);
        if (!(entity instanceof Zombie zombie)) {
            source.sendSuccess(() -> Component.literal("Failed to spawn smart zombie."), false);
            return 0;
        }
        if (zombie.isBaby()) zombie.setBaby(false);

        makeSmart(zombie);
        source.sendSuccess(() -> Component.literal("Spawned a smart miner zombie."), true);
        return 1;
    }

    private static void makeSmart(Zombie zombie) {
        zombie.getPersistentData().putBoolean(SMART_KEY, true);
        zombie.setPersistenceRequired();
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SmartMobs.MINING_HELMET.get()));
        damageHat(zombie.getItemBySlot(EquipmentSlot.HEAD),zombie);
        zombie.setDropChance(EquipmentSlot.HEAD, 0.05F);
    }

    private static void makeGarden(Zombie zombie){
        zombie.getPersistentData().putBoolean(GARDEN_KEY,true);
        zombie.setPersistenceRequired();
        zombie.setItemSlot(EquipmentSlot.HEAD,new ItemStack(SmartMobs.GARDEN_HAT.get()));
        damageHat(zombie.getItemBySlot(EquipmentSlot.HEAD),zombie);
        zombie.setDropChance(EquipmentSlot.HEAD,.05F);
    }

    private static void damageHat(ItemStack stack,Zombie zombie){
        if(!stack.isDamageableItem())return;
        int remaining=Math.max(1,Math.round(stack.getMaxDamage()*(.05F+zombie.getRandom().nextFloat()*.25F)));
        stack.setDamageValue(Math.max(0,stack.getMaxDamage()-remaining));
    }

    /** Cancels the spawn of any baby zombie and applies random gear to adult zombies. */
    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        // Block ALL baby zombies from spawning, regardless of variant or equipment.
        if (zombie.isBaby()) {
            event.setCanceled(true); // baby zombie never joins the level
            return;
        }

        if (zombie.getType() != EntityType.ZOMBIE) return;
        if (isSmartMobZombie(zombie) || ZombieBreeds.isBreed(zombie)) return;
        double roll = zombie.getRandom().nextDouble();
        if (roll < froz8n.Config.gardenChance) makeGarden(zombie);
        else if (roll < froz8n.Config.gardenChance + froz8n.Config.smartChance) makeSmart(zombie);
        else ZombieBreeds.assign(zombie);
    }

    private static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) {
            // The humanoid renderer builds swimAmount locally from this pose. Keep it
            // in step with the server so zombies use the swimming model animation.
            if (entity instanceof Zombie zombie && zombie.getType() == EntityType.ZOMBIE) syncSwimmingPose(zombie);
            return;
        }
        if (entity instanceof Zombie zombie) {
            // Safety net: if anything turned this zombie into a baby after it joined,
            // remove it so no baby zombie (any variant, hat or helmet included) survives.
            if (zombie.isBaby()) {
                zombie.discard();
                return;
            }
            if (zombie.getType() != EntityType.ZOMBIE) return;
            clearLegacyBoxZombie(zombie);
            boolean smartMob = isSmartMobZombie(zombie);
            if (smartMob) {
                // SmartMobs use the tuned day/night pace and may stay outdoors.
                // Updating once per second avoids needless attribute work.
                if ((zombie.tickCount + zombie.getId()) % 20 == 0) {
                    var speed=zombie.getAttribute(Attributes.MOVEMENT_SPEED);
                    if(speed!=null) speed.setBaseValue(SmartMobWorldRules.isNightLike(zombie.level())
                            ?SmartZombieBrain.nightMoveSpeed():SmartZombieBrain.dayMoveSpeed());
                }
            }
            syncSwimmingPose(zombie);
            // Daylight is the player's ally again: only the hat wearers survive it, and
            // that is plain vanilla behaviour for a mob with a head item.
            if(froz8n.Config.sunlightImmunity&&!zombie.isInLava()&&zombie.getRemainingFireTicks()>0)zombie.clearFire();
            if(froz8n.data.Nbt.getBooleanOr(zombie.getPersistentData(), GARDEN_KEY,false))
                froz8n.combat.GardenZombieSystem.tickGarden(zombie);
            froz8n.combat.ZombieSerumSystem.tickZombie(zombie);
            froz8n.combat.SoundJammerSystem.tickZombie(zombie);
            if (froz8n.combat.SoundJammerSystem.isControlled(zombie)) return;
            ZombieBreeds.tick(zombie);
            if(SmartMobWorldRules.tryBreakVisiblePortal(zombie)) return;
            boolean smart = isSmart(zombie);
            if (smart) SmartZombieBrain.tickFallClutch(zombie);
            if(!smart&&zombie.isInWater()&&zombie.getTarget() instanceof net.minecraft.world.entity.player.Player target)
                swimLikePlayer(zombie,target);
            if (!smart) {
                return;
            }
            // Persistence and the helmet are the visible, durable identity of a miner.
            // Durable identity maintenance does not need to allocate/check equipment
            // 20 times per second for every mob. Stagger it across a 2-second window.
            if ((zombie.tickCount + zombie.getId()) % 40 == 0) {
                zombie.setPersistenceRequired();
                if (!zombie.getItemBySlot(EquipmentSlot.HEAD).is(SmartMobs.MINING_HELMET.get())) {
                    zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SmartMobs.MINING_HELMET.get()));
                }
                zombie.setDropChance(EquipmentSlot.HEAD, 0.05F);
            }
            // When the smart zombie dies or is being removed, clean up its brain
            // state, remove the crack overlay, drop the pickaxe from its hand and
            // clear the glow effect.
            if (!zombie.isAlive() || zombie.isRemoved()) {
                SmartZombieBrain.cleanup(zombie);
                return;
            }
            SmartZombieBrain.tick(zombie);
        }
        if(entity instanceof net.minecraft.world.entity.player.Player player)
            froz8n.combat.GardenZombieSystem.tickRooted(player);
    }

    /** Damage suppressors plus the two breed reactions, all on one hook. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (froz8n.combat.SoundJammerSystem.suppressFearedAttack(event.getSource())) event.setAmount(0);
        if (froz8n.combat.ZombieSerumSystem.preventAttack(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
            return;
        }
        if (froz8n.combat.GardenZombieSystem.suppressChargeAttack(event.getSource())) {
            event.setCanceled(true);
            return;
        }
        // A thief robs the player it just hit, a sapper goes off on the blow that kills it.
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player victim
                && event.getSource().getEntity() instanceof Zombie attacker) {
            ZombieBreeds.onZombieHitPlayer(attacker, victim);
            if (ZombieBreeds.isFleeing(attacker)) {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getEntity() instanceof Zombie hurt) {
            ZombieBreeds.onZombieDamaged(hurt, event.getSource(), event.getAmount());
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        froz8n.combat.SoundJammerSystem.tickFields(event.getServer());
        froz8n.combat.GardenZombieSystem.tickRootVisuals();
        froz8n.combat.GardenZombieSystem.tickCharges();
        TempBlockManager.tick();
    }

    /** Cancels the break of blocks placed by smart mobs so they never drop items. */
    private static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (TempBlockManager.isTemp(level, pos)) {
            // Block was placed by a smart mob: remove it without dropping anything.
            level.removeBlock(pos, false);
            TempBlockManager.untrack(level, pos);
            event.setCanceled(true); // cancel the vanilla break so nothing drops
        }
    }

    public static boolean isSmart(Entity entity) {
        return froz8n.data.Nbt.getBooleanOr(entity.getPersistentData(), SMART_KEY, false);
    }

    public static boolean isSmartMobZombie(Zombie zombie) {
        return froz8n.data.Nbt.getBooleanOr(zombie.getPersistentData(), SMART_KEY, false)
                || froz8n.data.Nbt.getBooleanOr(zombie.getPersistentData(), GARDEN_KEY, false);
    }

    private static void clearLegacyBoxZombie(Zombie zombie) {
        if (froz8n.data.Nbt.getBooleanOr(zombie.getPersistentData(), LEGACY_BOX_KEY, false)) {
            zombie.getPersistentData().remove(LEGACY_BOX_KEY);
            zombie.getPersistentData().remove(LEGACY_BOX_SHIELD_KEY);
        }
        if (zombie.getItemBySlot(EquipmentSlot.HEAD).is(SmartMobs.CARDBOARD_BOX.get())) {
            zombie.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            zombie.setDropChance(EquipmentSlot.HEAD, 0.0F);
        }
    }

    private static void swimLikePlayer(Zombie zombie,net.minecraft.world.entity.player.Player target){
        Vec3 toward=target.getEyePosition().subtract(zombie.getEyePosition());
        if(toward.lengthSqr()<.0001)return;
        Vec3 direction=toward.normalize();
        Vec3 desired=direction.scale(SWIM_SPEED);
        if (zombie.getFluidHeight(FluidTags.WATER) < 0.85D) {
            desired = new Vec3(desired.x, Math.min(desired.y, -0.16D), desired.z);
        }
        zombie.setDeltaMovement(desired);
        faceVector(zombie, direction, 75.0F);
        zombie.setSwimming(true);
        zombie.setPose(Pose.SWIMMING);
        zombie.setSprinting(true);
        zombie.getLookControl().setLookAt(target,75,75);
    }

    private static void faceVector(Zombie zombie, Vec3 direction, float maxPitch) {
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = net.minecraft.util.Mth.wrapDegrees((float)(net.minecraft.util.Mth.atan2(direction.z, direction.x) * 180.0F / Math.PI) - 90.0F);
        float pitch = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees((float)(-(net.minecraft.util.Mth.atan2(direction.y, horizontal) * 180.0F / Math.PI))), -maxPitch, maxPitch);
        zombie.setYRot(yaw);
        zombie.setYHeadRot(yaw);
        zombie.setYBodyRot(yaw);
        zombie.setXRot(pitch);
    }

    private static void syncSwimmingPose(Zombie zombie) {
        if (zombie.isInWater()) {
            if (!zombie.isSwimming()) zombie.setSwimming(true);
            if (zombie.getPose() != Pose.SWIMMING) zombie.setPose(Pose.SWIMMING);
        } else {
            if (zombie.isSwimming()) zombie.setSwimming(false);
            if (zombie.getPose() == Pose.SWIMMING) zombie.setPose(Pose.STANDING);
        }
    }
}
