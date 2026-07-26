package froz8n.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Cancels movement at the input stage, so rooting never modifies speed/FOV.
 *
 * <p>Forge had MovementInputUpdateEvent; on Fabric the zeroing itself happens in
 * {@code froz8n.mixin.client.KeyboardInputMixin} right after the input is polled,
 * and this class only holds the window during which it applies.
 */
@Environment(EnvType.CLIENT)
public final class RootedInputControl {
    private static long rootedUntil;
    private RootedInputControl(){}
    public static void rootFor(int ticks){rootedUntil=System.currentTimeMillis()+ticks*50L;}
    public static boolean isRooted(){return System.currentTimeMillis()<rootedUntil;}
}
