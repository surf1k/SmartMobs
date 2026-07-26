package froz8n.client;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.MovementInputUpdateEvent;

/** Cancels movement at the input stage, so rooting never modifies speed/FOV. */
public final class RootedInputControl {
    private static long rootedUntil;
    private RootedInputControl(){}
    public static void register(){MovementInputUpdateEvent.BUS.addListener(RootedInputControl::onInput);}
    public static void rootFor(int ticks){rootedUntil=System.currentTimeMillis()+ticks*50L;}
    private static void onInput(MovementInputUpdateEvent event){
        if(System.currentTimeMillis()>=rootedUntil)return;
        event.getInput().keyPresses=Input.EMPTY;
        event.getInput().moveVector=Vec2.ZERO;
        event.getEntity().setSprinting(false);
        var m=event.getEntity().getDeltaMovement();
        event.getEntity().setDeltaMovement(0,Math.min(0,m.y),0);
    }
}
