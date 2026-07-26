package froz8n.combat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ZombieSerumItem extends Item {
    public ZombieSerumItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        return ItemUtils.startUsingInstantly(level,player,hand);
    }
    @Override public int getUseDuration(ItemStack stack){return 32;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(entity instanceof Player player){
            if(!level.isClientSide()){
                ZombieSerumSystem.apply(player);
                player.addEffect(new MobEffectInstance(froz8n.SmartMobs.zombieDisguise(),300,0,false,true,true));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER,300,0,false,true,true));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,60,0,false,true,true));
            }
            return ItemUtils.createFilledResult(stack,player,new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,
                                          List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable("item.smartmobs.zombie_serum.desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.smartmobs.zombie_serum.desc.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.smartmobs.zombie_serum.desc.3").withStyle(ChatFormatting.GRAY));
    }
}