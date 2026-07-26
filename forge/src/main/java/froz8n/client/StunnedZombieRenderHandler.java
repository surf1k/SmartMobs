package froz8n.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLivingEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Smooth knock-down, unconscious twitch and recovery animation. */
public final class StunnedZombieRenderHandler {
    private record Anim(long fallStarted, long recoveryStarted) {}
    private static final Map<Integer, Anim> ANIMS = new HashMap<>();
    private static final ThreadLocal<ArrayDeque<Boolean>> PUSHED =
            ThreadLocal.withInitial(ArrayDeque::new);

    private StunnedZombieRenderHandler() {}

    public static void register() {
        RenderLivingEvent.Pre.BUS.addListener(StunnedZombieRenderHandler::before);
        RenderLivingEvent.Post.BUS.addListener(StunnedZombieRenderHandler::after);
    }

    private static boolean before(RenderLivingEvent.Pre event) {
        Zombie zombie = zombieNear(event.getState().x, event.getState().y, event.getState().z);
        Transform transform = zombie == null ? null : transform(zombie);
        boolean active = transform != null && transform.fall > 0.001F;
        PUSHED.get().push(active);
        if (!active) return false;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // Pivot near the feet, not the model centre: the head follows a falling arc
        // and the body finishes just above the ground instead of rotating in mid-air.
        pose.translate(0.0, 0.08 + 0.10 * transform.fall, 0.0);
        pose.mulPose(Axis.ZP.rotationDegrees(transform.wobble));
        pose.mulPose(Axis.XP.rotationDegrees(88.0F * transform.fall));
        pose.translate(0.0, -0.88 * transform.fall, 0.12 * transform.fall);
        return false;
    }

    private static void after(RenderLivingEvent.Post event) {
        ArrayDeque<Boolean> stack = PUSHED.get();
        if (!stack.isEmpty() && stack.pop()) event.getPoseStack().popPose();
    }

    private static Transform transform(Zombie zombie) {
        long now = System.currentTimeMillis();
        Anim anim = ANIMS.get(zombie.getId());
        if (zombie.isNoAi()) {
            if (anim == null || anim.recoveryStarted != 0) {
                anim = new Anim(now, 0); ANIMS.put(zombie.getId(), anim);
            }
            float t = clamp((now - anim.fallStarted) / 420.0F);
            float eased = t * t * (3.0F - 2.0F * t);
            float impact = t < .72F ? (float)Math.sin(t * Math.PI * 3.0) * (1.0F-t) * 8.0F
                    : (float)Math.sin(now * .018) * 1.2F;
            return new Transform(eased, impact);
        }
        if (anim == null) return null;
        if (anim.recoveryStarted == 0) {
            anim = new Anim(anim.fallStarted, now); ANIMS.put(zombie.getId(), anim);
        }
        float t = clamp((now - anim.recoveryStarted) / 520.0F);
        if (t >= 1.0F) { ANIMS.remove(zombie.getId()); return null; }
        float upright = 1.0F - t*t*(3.0F-2.0F*t);
        return new Transform(upright, (float)Math.sin(t*Math.PI*2.0)*2.0F);
    }

    private static Zombie zombieNear(double x, double y, double z) {
        if (Minecraft.getInstance().level == null) return null;
        Zombie best = null; double bestDistance = .5;
        for (Entity e : Minecraft.getInstance().level.getEntities(null,
                new AABB(x-.5,y-.3,z-.5,x+.5,y+2.2,z+.5))) {
            if (e instanceof Zombie zombie) {
                double distance = zombie.distanceToSqr(x,y,z);
                if (distance < bestDistance) { bestDistance=distance; best=zombie; }
            }
        }
        return best;
    }

    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private record Transform(float fall, float wobble) {}
}
