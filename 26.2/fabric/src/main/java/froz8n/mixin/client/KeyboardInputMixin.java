package froz8n.mixin.client;

import froz8n.client.RootedInputControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels movement at the input stage while the player is rooted - the Fabric
 * equivalent of Forge's MovementInputUpdateEvent.
 *
 * <p>{@code KeyboardInput#tick} does not call {@code super.tick()}: it writes the two
 * inherited fields itself. Mixin only resolves {@code @Shadow} against the target class,
 * and both fields are declared one level up on {@link ClientInput}, so they are reached
 * through a cast (public field) and {@link ClientInputAccessor} (protected field).
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void smartmobs$cancelRootedInput(CallbackInfo ci) {
        if (!RootedInputControl.isRooted()) return;
        ClientInput self = (ClientInput) (Object) this;
        self.keyPresses = Input.EMPTY;
        ((ClientInputAccessor) self).setMoveVector(Vec2.ZERO);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        player.setSprinting(false);
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(0.0, Math.min(0.0, movement.y), 0.0);
    }
}
