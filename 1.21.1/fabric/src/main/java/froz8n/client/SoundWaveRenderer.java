package froz8n.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Smooth world-space pressure rings, drawn straight into the world buffer on 1.21.1. */
@Environment(EnvType.CLIENT)
public final class SoundWaveRenderer {
    private static final Map<Integer, Long> STARTS = new HashMap<>();
    private SoundWaveRenderer() {}
    public static void register() { WorldRenderEvents.AFTER_ENTITIES.register(SoundWaveRenderer::render); }
    public static void activate(int playerId) { STARTS.put(playerId, System.currentTimeMillis()); }

    private static void render(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || STARTS.isEmpty() || context.consumers() == null) return;
        long now = System.currentTimeMillis();
        STARTS.entrySet().removeIf(e -> now - e.getValue() > 3100);
        Vec3 camera = context.camera().getPosition();
        VertexConsumer buffer = context.consumers().getBuffer(RenderType.debugQuads());
        for (var entry : STARTS.entrySet()) {
            Entity source = mc.level.getEntity(entry.getKey());
            if (source == null) continue;
            float age = (now - entry.getValue()) / 1000F;
            for (int pulse = 0; pulse < 3; pulse++) {
                float t = (age - pulse) / .48F;
                if (t < 0 || t > 1) continue;
                float eased = 1 - (1 - t) * (1 - t), radius = .25F + 4.75F * eased, alpha = (1 - t) * (1 - t);
                PoseStack pose = context.matrixStack();
                pose.pushPose();
                pose.translate(source.getX() - camera.x, source.getY() - camera.y + .08, source.getZ() - camera.z);
                drawWave(pose, buffer, radius, alpha);
                pose.popPose();
            }
        }
    }

    private static void drawWave(PoseStack pose, VertexConsumer c, float r, float a) {
        band(pose, c, r, .075F, color(a * .9F, 0x5DE8FF));
        band(pose, c, r - .13F, .055F, color(a * .48F, 0xD0FAFF));
        band(pose, c, r + .15F, .10F, color(a * .25F, 0x238AAE));
    }

    private static void band(PoseStack pose, VertexConsumer c, float r, float w, int color) {
        if (r <= 0) return;
        int n = 72;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n, b = Math.PI * 2 * (i + 1) / n;
            v(pose, c, (float)Math.cos(a)*(r-w), (float)Math.sin(a)*(r-w), color);
            v(pose, c, (float)Math.cos(b)*(r-w), (float)Math.sin(b)*(r-w), color);
            v(pose, c, (float)Math.cos(b)*(r+w), (float)Math.sin(b)*(r+w), color);
            v(pose, c, (float)Math.cos(a)*(r+w), (float)Math.sin(a)*(r+w), color);
            v(pose, c, (float)Math.cos(a)*(r+w), (float)Math.sin(a)*(r+w), color);
            v(pose, c, (float)Math.cos(b)*(r+w), (float)Math.sin(b)*(r+w), color);
            v(pose, c, (float)Math.cos(b)*(r-w), (float)Math.sin(b)*(r-w), color);
            v(pose, c, (float)Math.cos(a)*(r-w), (float)Math.sin(a)*(r-w), color);
        }
    }

    private static void v(PoseStack pose, VertexConsumer c, float x, float z, int color) {
        c.addVertex(pose.last(), x, 0, z).setColor(color).setNormal(pose.last(), 0, 1, 0);
    }

    private static int color(float a, int rgb) {
        return (Math.max(0, Math.min(255, (int)(a * 255))) << 24) | rgb;
    }
}