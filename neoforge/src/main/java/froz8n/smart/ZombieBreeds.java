package froz8n.smart;

import froz8n.Config;
import froz8n.data.PersistentData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The seven lesser zombie breeds. None of them can dig, build or outrun the player - they
 * are flavour and pressure, not a siege engine. Every one of them wears the mod helmet, so
 * a modded night is obvious at a glance; the hand item says which breed you are looking at.
 *
 * <ul>
 *   <li><b>brute</b> - 30 hearts of meat, hits harder, shrugs off knockback, walks slowly.</li>
 *   <li><b>runner</b> - fast and paper-thin.</li>
 *   <li><b>screamer</b> - shrieks and turns every zombie nearby onto you.</li>
 *   <li><b>thief</b> - snatches a stack out of your hotbar and runs; drops it when killed.</li>
 *   <li><b>medic</b> - patches up wounded zombies around it, barely fights.</li>
 *   <li><b>sapper</b> - detonates when killed, without touching the terrain.</li>
 *   <li><b>ghost</b> - invisible, drifts straight through walls, marked only by soul flame.</li>
 * </ul>
 *
 * <p>This file is shared verbatim by the Fabric, Quilt, NeoForge and Forge trees: it only
 * touches vanilla APIs plus {@link PersistentData} and the {@code SmartMobs} accessors.
 */
public final class ZombieBreeds {

    public static final String BREED_KEY = "smartmobs_breed";
    private static final String THIEF_FLEE_UNTIL = "smartmobs_thief_flee_until";
    private static final String SCREAM_COOLDOWN = "smartmobs_scream_cooldown";
    private static final String GHOST_NEXT_HIT = "smartmobs_ghost_next_hit";
    private static final String SAPPER_SPENT = "smartmobs_sapper_spent";
    private static final String SAPPER_FUSE = "smartmobs_sapper_fuse";

    public static final String BRUTE = "brute";
    public static final String RUNNER = "runner";
    public static final String SCREAMER = "screamer";
    public static final String THIEF = "thief";
    public static final String MEDIC = "medic";
    public static final String SAPPER = "sapper";
    public static final String GHOST = "ghost";

    private static final String[] ALL = {BRUTE, RUNNER, SCREAMER, THIEF, MEDIC, SAPPER, GHOST};

    private static final int SCREAM_RANGE = 32;
    private static final int SCREAM_RALLY_RANGE = 40;
    private static final int SCREAM_COOLDOWN_TICKS = 200;
    private static final int MEDIC_RANGE = 12;
    private static final float MEDIC_HEAL = 3.0F;
    private static final int THIEF_FLEE_TICKS = 600;
    private static final float SAPPER_POWER = 2.8F;
    /** How close a sapper gets before it lights itself, in blocks. */
    private static final double SAPPER_FUSE_RANGE = 3.5;
    /** Fuse length once lit. A creeper burns 30 ticks; this one does not give you that long. */
    private static final int SAPPER_FUSE_TICKS = 24;
    private static final double GHOST_SPEED = 0.145;
    private static final int GHOST_HIT_COOLDOWN = 18;
    private static final float GHOST_DAMAGE = 4.5F;

    private ZombieBreeds() {
    }

    /** @return the breed id of this zombie, or an empty string for a plain one. */
    public static String breedOf(Zombie zombie) {
        return PersistentData.of(zombie).getStringOr(BREED_KEY, "");
    }

    public static boolean isBreed(Zombie zombie) {
        return !breedOf(zombie).isEmpty();
    }

    /** Rolls a breed for a freshly spawned ordinary zombie. */
    public static void assign(Zombie zombie) {
        if (!Config.enableBreeds || Config.breedChance <= 0.0) return;
        if (zombie.getRandom().nextDouble() >= Config.breedChance) return;
        apply(zombie, ALL[zombie.getRandom().nextInt(ALL.length)]);
    }

    private static void apply(Zombie zombie, String breed) {
        PersistentData.of(zombie).putString(BREED_KEY, breed);
        // Every breed has its own hat, so you can read the threat off its head before it
        // reaches you. The ghost's veil is deliberately included: vanilla renders armour on
        // invisible mobs, so the veil drifting through a wall is the only warning you get.
        wearHat(zombie, breed);
        switch (breed) {
            case BRUTE -> {
                setMaxHealth(zombie, 30.0);
                setAttribute(zombie, Attributes.ATTACK_DAMAGE, 5.0);
                setAttribute(zombie, Attributes.KNOCKBACK_RESISTANCE, 0.6);
                setAttribute(zombie, Attributes.MOVEMENT_SPEED, 0.195);
                hold(zombie, Items.IRON_INGOT);
            }
            case RUNNER -> {
                setMaxHealth(zombie, 12.0);
                setAttribute(zombie, Attributes.MOVEMENT_SPEED, 0.33);
                hold(zombie, Items.FEATHER);
            }
            case SCREAMER -> {
                setMaxHealth(zombie, 16.0);
                hold(zombie, Items.GOAT_HORN);
            }
            case THIEF -> {
                setMaxHealth(zombie, 14.0);
                setAttribute(zombie, Attributes.MOVEMENT_SPEED, 0.28);
                setAttribute(zombie, Attributes.ATTACK_DAMAGE, 2.0);
            }
            case MEDIC -> {
                setMaxHealth(zombie, 18.0);
                setAttribute(zombie, Attributes.ATTACK_DAMAGE, 1.5);
                hold(zombie, Items.GLASS_BOTTLE);
            }
            case SAPPER -> {
                setMaxHealth(zombie, 14.0);
                setAttribute(zombie, Attributes.ATTACK_DAMAGE, 2.0);
                hold(zombie, Items.GUNPOWDER);
            }
            case GHOST -> {
                setMaxHealth(zombie, 10.0);
                setAttribute(zombie, Attributes.ATTACK_DAMAGE, 3.0);
                zombie.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,
                        MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                zombie.setNoGravity(true);
                zombie.noPhysics = true;
            }
            default -> { }
        }
    }

    /** Per-tick behaviour. Called from the mod's living-tick handler, server side only. */
    public static void tick(Zombie zombie) {
        if (!(zombie.level() instanceof ServerLevel level)) return;
        String breed = breedOf(zombie);
        if (breed.isEmpty()) return;
        switch (breed) {
            case SCREAMER -> tickScreamer(level, zombie);
            case MEDIC -> tickMedic(level, zombie);
            case THIEF -> tickThief(level, zombie);
            case SAPPER -> tickSapper(level, zombie);
            case GHOST -> tickGhost(level, zombie);
            default -> { }
        }
    }

    /**
     * The ghost drifts straight through walls at a crawl, invisible except for a thin
     * trail of soul flame. It never suffocates - vanilla skips the in-wall check for a
     * noPhysics entity - and it hits by hand, because no navigation survives phasing.
     */
    private static void tickGhost(ServerLevel level, Zombie zombie) {
        zombie.noPhysics = true;
        zombie.setNoGravity(true);
        zombie.setNoActionTime(0);
        if ((zombie.tickCount + zombie.getId()) % 4 == 0) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, zombie.getX(), zombie.getY(0.6), zombie.getZ(),
                    1, 0.18, 0.35, 0.18, 0.0);
        }
        if ((zombie.tickCount + zombie.getId()) % 90 == 0) {
            level.playSound(null, zombie.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.HOSTILE, 0.6F, 0.6F);
        }

        Player target = zombie.getTarget() instanceof Player p && p.isAlive() ? p
                : level.getNearestPlayer(zombie, froz8n.Config.detectionRange);
        if (target == null || target.isSpectator() || target.isCreative()
                || froz8n.combat.ZombieSerumSystem.isMasked(target)) {
            zombie.setDeltaMovement(zombie.getDeltaMovement().scale(0.5));
            return;
        }
        zombie.setTarget(target);
        zombie.getLookControl().setLookAt(target, 30.0F, 30.0F);

        Vec3 toward = target.position().add(0.0, 0.35, 0.0).subtract(zombie.position());
        double distance = toward.length();
        if (distance > 1.2) {
            // Deliberately slower than walking: you cannot outfight it, but you can walk away.
            zombie.setDeltaMovement(toward.normalize().scale(GHOST_SPEED));
            zombie.hurtMarked = true;
        } else {
            zombie.setDeltaMovement(Vec3.ZERO);
            long now = level.getGameTime();
            if (PersistentData.of(zombie).getLongOr(GHOST_NEXT_HIT, 0L) <= now) {
                PersistentData.of(zombie).putLong(GHOST_NEXT_HIT, now + GHOST_HIT_COOLDOWN);
                zombie.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                target.hurtServer(level, level.damageSources().mobAttack(zombie), GHOST_DAMAGE);
                level.playSound(null, zombie.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                        SoundSource.HOSTILE, 0.9F, 1.4F);
            }
        }
    }

    private static void tickScreamer(ServerLevel level, Zombie zombie) {
        if ((zombie.tickCount + zombie.getId()) % 20 != 0) return;
        long now = level.getGameTime();
        if (PersistentData.of(zombie).getLongOr(SCREAM_COOLDOWN, 0L) > now) return;
        Player target = level.getNearestPlayer(zombie, SCREAM_RANGE);
        if (target == null || target.isSpectator() || target.isCreative()
                || froz8n.combat.ZombieSerumSystem.isMasked(target)
                || !zombie.hasLineOfSight(target)) {
            return;
        }
        PersistentData.of(zombie).putLong(SCREAM_COOLDOWN, now + SCREAM_COOLDOWN_TICKS);
        zombie.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
        level.playSound(null, zombie.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 1.6F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, zombie.getX(), zombie.getEyeY(), zombie.getZ(), 1, 0, 0, 0, 0);
        for (Zombie other : level.getEntitiesOfClass(Zombie.class,
                zombie.getBoundingBox().inflate(SCREAM_RALLY_RANGE),
                z -> z.isAlive() && z.getTarget() == null)) {
            other.setTarget(target);
        }
    }

    private static void tickMedic(ServerLevel level, Zombie zombie) {
        if ((zombie.tickCount + zombie.getId()) % 40 != 0) return;
        AABB area = zombie.getBoundingBox().inflate(MEDIC_RANGE);
        boolean healed = false;
        for (Zombie patient : level.getEntitiesOfClass(Zombie.class, area,
                z -> z.isAlive() && z != zombie && z.getHealth() < z.getMaxHealth())) {
            patient.heal(MEDIC_HEAL);
            level.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getEyeY() + 0.4,
                    patient.getZ(), 2, 0.25, 0.25, 0.25, 0.0);
            healed = true;
        }
        if (healed) {
            level.playSound(null, zombie.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE,
                    SoundSource.HOSTILE, 0.4F, 1.6F);
        }
    }

    private static void tickThief(ServerLevel level, Zombie zombie) {
        long until = PersistentData.of(zombie).getLongOr(THIEF_FLEE_UNTIL, 0L);
        if (until <= level.getGameTime()) return;
        zombie.setTarget(null);
        zombie.setAggressive(false);
        if ((zombie.tickCount + zombie.getId()) % 10 != 0 && !zombie.getNavigation().isDone()) return;
        Player chaser = level.getNearestPlayer(zombie, 24.0);
        if (chaser == null) return;
        Vec3 away = zombie.position().subtract(chaser.position());
        if (away.lengthSqr() < 0.01) return;
        Vec3 flee = zombie.position().add(away.normalize().scale(12.0));
        zombie.getNavigation().moveTo(flee.x, flee.y, flee.z, 1.3);
    }

    /**
     * A zombie just landed a hit on a player. Thieves grab a stack and leave.
     * Called from the mod's incoming-damage handler.
     */
    public static void onZombieHitPlayer(Zombie zombie, Player player) {
        if (!THIEF.equals(breedOf(zombie))) return;
        if (player.isCreative() || !(zombie.level() instanceof ServerLevel level)) return;
        if (!zombie.getMainHandItem().isEmpty()) return; // already carrying loot

        int slot = -1;
        for (int attempt = 0; attempt < 9; attempt++) {
            int candidate = zombie.getRandom().nextInt(9);
            if (!player.getInventory().getItem(candidate).isEmpty()) {
                slot = candidate;
                break;
            }
        }
        if (slot < 0) return;

        ItemStack loot = player.getInventory().removeItemNoUpdate(slot);
        if (loot.isEmpty()) return;
        zombie.setItemSlot(EquipmentSlot.MAINHAND, loot);
        zombie.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        PersistentData.of(zombie).putLong(THIEF_FLEE_UNTIL, level.getGameTime() + THIEF_FLEE_TICKS);
        zombie.setTarget(null);
        level.playSound(null, zombie.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, zombie.getX(), zombie.getEyeY(), zombie.getZ(),
                4, 0.3, 0.3, 0.3, 0.0);
    }

    /**
     * A zombie is about to take damage. Sappers go off when the hit would finish them.
     * Called from the mod's incoming-damage handler.
     */
    public static void onZombieDamaged(Zombie zombie, DamageSource source, float amount) {
        if (!SAPPER.equals(breedOf(zombie))) return;
        if (!(zombie.level() instanceof ServerLevel level)) return;
        if (amount < zombie.getHealth()) return;
        detonate(level, zombie);
    }

    /**
     * The sapper is a creeper in a cap: it closes the distance, lights itself and goes off in
     * your face. Backing away does not put the fuse out - once lit it burns down wherever the
     * sapper happens to be. Killing one by hand is still possible, and still a bad idea.
     */
    private static void tickSapper(ServerLevel level, Zombie zombie) {
        if (PersistentData.of(zombie).getBooleanOr(SAPPER_SPENT, false)) return;
        int fuse = PersistentData.of(zombie).getIntOr(SAPPER_FUSE, -1);
        if (fuse < 0) {
            if (!(zombie.getTarget() instanceof Player target) || target.isSpectator() || target.isCreative()
                    || froz8n.combat.ZombieSerumSystem.isMasked(target)
                    || zombie.distanceToSqr(target) > SAPPER_FUSE_RANGE * SAPPER_FUSE_RANGE) {
                return;
            }
            PersistentData.of(zombie).putInt(SAPPER_FUSE, SAPPER_FUSE_TICKS);
            level.playSound(null, zombie.blockPosition(), SoundEvents.CREEPER_PRIMED,
                    SoundSource.HOSTILE, 1.4F, 0.8F);
            zombie.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                    SAPPER_FUSE_TICKS + 5, 0, false, false, true));
            return;
        }
        level.sendParticles(ParticleTypes.SMOKE, zombie.getX(), zombie.getEyeY(), zombie.getZ(),
                3, 0.18, 0.18, 0.18, 0.01);
        if (fuse > 0) {
            PersistentData.of(zombie).putInt(SAPPER_FUSE, fuse - 1);
            return;
        }
        detonate(level, zombie);
    }

    /** One detonation per sapper, whether the fuse ran out or something killed it first. */
    private static void detonate(ServerLevel level, Zombie zombie) {
        // The blast catches the sapper too, which would re-enter this handler; one detonation
        // per zombie, flagged before the explosion goes off.
        if (PersistentData.of(zombie).getBooleanOr(SAPPER_SPENT, false)) return;
        PersistentData.of(zombie).putBoolean(SAPPER_SPENT, true);
        // Damage-free terrain: the blast hurts whoever is standing next to it, nothing else.
        level.explode(zombie, zombie.getX(), zombie.getY(0.5), zombie.getZ(),
                SAPPER_POWER, Level.ExplosionInteraction.NONE);
        zombie.discard();
    }

    /** @return {@code true} while a thief is running away with something of yours. */
    public static boolean isFleeing(Zombie zombie) {
        return THIEF.equals(breedOf(zombie))
                && PersistentData.of(zombie).getLongOr(THIEF_FLEE_UNTIL, 0L) > zombie.level().getGameTime();
    }

    private static void wearHat(Zombie zombie, String breed) {
        net.minecraft.world.item.Item hat = froz8n.SmartMobs.breedHatFor(breed);
        if (hat == null) return;
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(hat));
        // Low enough that a night of fighting does not bury the ground in hats.
        zombie.setDropChance(EquipmentSlot.HEAD, 0.02F);
    }

    private static void hold(Zombie zombie, net.minecraft.world.item.Item item) {
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
        zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static void setMaxHealth(LivingEntity entity, double value) {
        setAttribute(entity, Attributes.MAX_HEALTH, value);
        entity.setHealth((float) value);
    }

    private static void setAttribute(LivingEntity entity,
                                     net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                     double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }
}
