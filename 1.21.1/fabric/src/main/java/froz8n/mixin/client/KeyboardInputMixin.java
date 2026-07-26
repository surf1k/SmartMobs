package froz8n.mixin.client;

import froz8n.client.RootedInputControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Cancels movement at the input stage while the player is rooted. On 1.21.1 the impulses
// and key flags are plain public fields on Input, so no accessor is needed.
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick(ZF)V", at = @At("TAIL"))
    private void smartmobs$cancelRootedInput(boolean isSneaking, float sneakingSpeed, CallbackInfo ci) {
        if (!RootedInputControl.isRooted()) return;
        Input self = (Input) (Object) this;
        self.up = false;
        self.down = false;
        self.left = false;
        self.right = false;
        self.jumping = false;
        self.forwardImpulse = 0.0F;
        self.leftImpulse = 0.0F;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.setSprinting(false);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0, Math.min(0.0, movement.y), 0.0);
    }
}