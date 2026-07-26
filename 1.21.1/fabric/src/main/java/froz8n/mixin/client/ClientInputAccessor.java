package froz8n.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code moveVector} is protected and declared on {@link ClientInput}, not on the
 * {@code KeyboardInput} subclass the rooting mixin targets - and Mixin only resolves
 * {@code @Shadow} against the target class itself. This accessor bridges the gap.
 */
@Mixin(ClientInput.class)
public interface ClientInputAccessor {

    @Accessor("moveVector")
    void setMoveVector(Vec2 moveVector);
}
