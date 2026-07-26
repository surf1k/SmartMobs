package froz8n.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public final class CardboardBoxItem extends Item {
    public CardboardBoxItem(Properties properties){super(properties);}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntityRenderState state,ItemStack stack,
                    EquipmentSlot slot,HumanoidModel<?> original){
                return new CardboardBoxModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(CardboardBoxModel.LAYER));
            }
        });
    }
}
