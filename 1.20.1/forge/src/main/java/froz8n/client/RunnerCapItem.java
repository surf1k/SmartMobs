package froz8n.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Item-side hook supplying the runner_cap geometry. Forge's answer to Fabric's ArmorRenderer. */
public final class RunnerCapItem extends ArmorItem {
    public RunnerCapItem(ArmorMaterial material, Properties properties) {
        super(material, ArmorItem.Type.HELMET, properties);
    }

    // Vanilla 1.20.1 would look this up in the minecraft namespace; Forge lets the item
    // name the file itself.
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "smartmobs:textures/models/armor/runner_cap_layer_1.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                HumanoidModel<LivingEntity> model = new RunnerCapModel<>(Minecraft.getInstance()
                        .getEntityModels().bakeLayer(RunnerCapModel.LAYER));
                // Both are HumanoidModel; the wildcard on the vanilla side makes the copy raw.
                ((HumanoidModel) original).copyPropertiesTo(model);
                return model;
            }
        });
    }
}
