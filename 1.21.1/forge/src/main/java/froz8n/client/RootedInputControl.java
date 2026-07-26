package froz8n.client;

import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;

/** Cancels movement at the input stage, so rooting never modifies speed/FOV. */
public final class RootedInputControl {
    private static long rootedUntil;
    private RootedInputControl(){}
    public static void register(){MinecraftForge.EVENT_BUS.addListener(RootedInputControl::onInput);}
    public static void rootFor(int ticks){rootedUntil=System.currentTimeMillis()+ticks*50L;}
    private static void onInput(MovementInputUpdateEvent event){
        if(System.currentTimeMillis()>=rootedUntil)return;
        // 1.21.1 has no Input.keyPresses record: the flags and impulses are plain fields.
        var input=event.getInput();
        input.up=false;input.down=false;input.left=false;input.right=false;input.jumping=false;
        input.forwardImpulse=0;input.leftImpulse=0;
        event.getEntity().setSprinting(false);
        var m=event.getEntity().getDeltaMovement();
        event.getEntity().setDeltaMovement(0,Math.min(0,m.y),0);
    }
}