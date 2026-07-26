package froz8n.mixin.client;

import froz8n.client.JammerKeys;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shift + mouse wheel selects the jammer mode instead of scrolling the hotbar. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void smartmobs$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (JammerKeys.onScroll(yOffset)) {
            ci.cancel();
        }
    }
}
