package froz8n.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Item-side hook supplying the medic_cap geometry. Forge's answer to Fabric's ArmorRenderer. */
public final class MedicCapItem extends Item {
    public MedicCapItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntityRenderState state, ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                return new MedicCapModel<>(Minecraft.getInstance().getEntityModels()
                        .bakeLayer(MedicCapModel.LAYER));
            }
        });
    }
}
