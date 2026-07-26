package froz8n.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Client-owned root actors: smooth on every frame and identical for all packet receivers. */
@Environment(EnvType.CLIENT)
public final class RootVisualClient {
    private record Root(FallingBlockEntity entity,double x,double y,double z,long born,int duration,float delay){}
    private static final List<Root> ROOTS=new ArrayList<>();
    private RootVisualClient(){}
    public static void register(){ClientTickEvents.END_CLIENT_TICK.register(client->tick());}

    public static void activate(int targetId,double fallbackX,double fallbackY,double fallbackZ,long seed,int duration){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;
        var target=mc.level.getEntity(targetId);
        double cx=target==null?fallbackX:target.getX();
        double groundY=target==null?fallbackY:target.getY();
        double cz=target==null?fallbackZ:target.getZ();
        Random random=new Random(seed);long born=System.currentTimeMillis();
        for(int i=0;i<9;i++){
            double x=cx+(random.nextDouble()-.5)*1.35,z=cz+(random.nextDouble()-.5)*1.35;
            int rotation=random.nextInt(4);
            var state=froz8n.SmartMobs.GRASPING_ROOTS.defaultBlockState()
                    .setValue(froz8n.block.GraspingRootsBlock.ROTATION,rotation);
            FallingBlockEntity entity=new FallingBlockEntity(mc.level,x,groundY-1.0,z,state);
            entity.setNoGravity(true);entity.noPhysics=true;entity.disableDrop();entity.setDeltaMovement(Vec3.ZERO);
            mc.level.putNonPlayerEntity(entity.getId(),entity);ROOTS.add(new Root(entity,x,groundY,z,born,duration,random.nextFloat()*2.2F));
        }
        // A clearly audible, low earthen burst. Packet delivery means every nearby
        // client hears it at the same world position, including observers.
        mc.level.playLocalSound(cx,groundY,cz,SoundEvents.WARDEN_EMERGE,SoundSource.HOSTILE,.35F,1.55F,false);
        mc.level.playLocalSound(cx,groundY,cz,SoundEvents.ROOTED_DIRT_BREAK,SoundSource.HOSTILE,.65F,.62F,false);
        mc.level.playLocalSound(cx,groundY,cz,SoundEvents.MANGROVE_ROOTS_BREAK,SoundSource.HOSTILE,.55F,.72F,false);
        mc.level.playLocalSound(cx,groundY,cz,SoundEvents.GRASS_PLACE,SoundSource.HOSTILE,.35F,.48F,false);
    }

    private static void tick(){
        long now=System.currentTimeMillis();Iterator<Root> iterator=ROOTS.iterator();
        while(iterator.hasNext()){
            Root root=iterator.next();
            if(root.entity.isRemoved()){iterator.remove();continue;}
            float age=(now-root.born)/50F;
            if(age>=root.duration){root.entity.discard();iterator.remove();continue;}
            float local=age-root.delay;
            float rise=local<=0?0:bounceOut(Math.min(1,local/3.2F));
            float retract=age>root.duration-5?smooth((age-(root.duration-5))/5F):0;
            double y=root.y-1.0+rise-retract;
            root.entity.setPos(root.x,y,root.z);root.entity.setDeltaMovement(Vec3.ZERO);
        }
    }
    private static float smooth(float t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    private static float bounceOut(float t){
        // Back-out curve: shoots a little above the ground and snaps into place.
        float c1=1.7F,c3=c1+1F,x=t-1F;
        return 1F+c3*x*x*x+c1*x*x;
    }
}
