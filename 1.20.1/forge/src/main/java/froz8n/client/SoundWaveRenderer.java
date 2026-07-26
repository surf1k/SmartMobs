package froz8n.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;

// Smooth world-space pressure rings drawn as one geometry batch, not particles.
// 1.21.1 has no submit pipeline, so the rings go straight into the level buffer source.
public final class SoundWaveRenderer {
    private static final Map<Integer, Long> STARTS = new HashMap<>();
    private SoundWaveRenderer() {}
    public static void register() { MinecraftForge.EVENT_BUS.addListener(SoundWaveRenderer::render); }
    public static void activate(int playerId) { STARTS.put(playerId, System.currentTimeMillis()); }

    private static void render(RenderLevelStageEvent event) {
        if (event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null)return;
        long now=System.currentTimeMillis();
        STARTS.entrySet().removeIf(e->now-e.getValue()>3100);
        if(STARTS.isEmpty())return;
        Vec3 camera=event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
        VertexConsumer consumer=buffers.getBuffer(RenderType.debugQuads());
        PoseStack pose=event.getPoseStack();
        for(var entry:STARTS.entrySet()){
            Entity source=mc.level.getEntity(entry.getKey());
            if(source==null)continue;
            float age=(now-entry.getValue())/1000F;
            for(int pulse=0;pulse<3;pulse++){
                float t=(age-pulse)/.48F;
                if(t<0||t>1)continue;
                float eased=1-(1-t)*(1-t), radius=.25F+4.75F*eased, alpha=(1-t)*(1-t);
                pose.pushPose();
                pose.translate(source.getX()-camera.x,source.getY()-camera.y+.08,source.getZ()-camera.z);
                drawWave(pose.last(),consumer,radius,alpha);
                pose.popPose();
            }
        }
        buffers.endBatch(RenderType.debugQuads());
    }
    private static void drawWave(PoseStack.Pose p,VertexConsumer c,float r,float a){
        band(p,c,r,.075F,color(a*.9F,0x5DE8FF));
        band(p,c,r-.13F,.055F,color(a*.48F,0xD0FAFF));
        band(p,c,r+.15F,.10F,color(a*.25F,0x238AAE));
    }
    private static void band(PoseStack.Pose p,VertexConsumer c,float r,float w,int color){
        if(r<=0)return; int n=72;
        for(int i=0;i<n;i++){
            double a=Math.PI*2*i/n,b=Math.PI*2*(i+1)/n;
            v(p,c,(float)Math.cos(a)*(r-w),(float)Math.sin(a)*(r-w),color);
            v(p,c,(float)Math.cos(b)*(r-w),(float)Math.sin(b)*(r-w),color);
            v(p,c,(float)Math.cos(b)*(r+w),(float)Math.sin(b)*(r+w),color);
            v(p,c,(float)Math.cos(a)*(r+w),(float)Math.sin(a)*(r+w),color);
            v(p,c,(float)Math.cos(a)*(r+w),(float)Math.sin(a)*(r+w),color);
            v(p,c,(float)Math.cos(b)*(r+w),(float)Math.sin(b)*(r+w),color);
            v(p,c,(float)Math.cos(b)*(r-w),(float)Math.sin(b)*(r-w),color);
            v(p,c,(float)Math.cos(a)*(r-w),(float)Math.sin(a)*(r-w),color);
        }
    }
    // 1.20.1 still builds vertices the old way: position, colour, normal, endVertex.
    private static void v(PoseStack.Pose p,VertexConsumer c,float x,float z,int color){
        c.vertex(p.pose(),x,0,z)
                .color((color>>16)&0xFF,(color>>8)&0xFF,color&0xFF,(color>>>24)&0xFF)
                .normal(p.normal(),0,1,0)
                .endVertex();
    }
    private static int color(float a,int rgb){return (Math.max(0,Math.min(255,(int)(a*255)))<<24)|rgb;}
}