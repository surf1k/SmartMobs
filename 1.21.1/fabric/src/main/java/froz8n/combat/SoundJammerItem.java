package froz8n.combat;

import froz8n.data.Nbt;
import froz8n.data.PersistentData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

import java.util.List;

public final class SoundJammerItem extends Item {
    private static final String COOLDOWN_UNTIL = "smartmobs_jammer_cooldown_until";
    private static final String UP_COOLDOWN_UNTIL = "smartmobs_jammer_up_cooldown_until";
    private static final String MODE = "smartmobs_jammer_mode";
    public SoundJammerItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int mode=Nbt.getIntOr(PersistentData.of(player), MODE,0);
        String cooldownKey=mode==0?COOLDOWN_UNTIL:UP_COOLDOWN_UNTIL;
        long remaining = Nbt.getLongOr(PersistentData.of(player), cooldownKey, 0L) - System.currentTimeMillis();
        if (remaining > 0) {
            if (!level.isClientSide()) {
                int seconds = Math.max(1, (int)Math.ceil(remaining / 1000.0));
                player.displayClientMessage(Component.translatable(
                        "item.smartmobs.sound_jammer.cooldown", seconds).withStyle(ChatFormatting.RED), true);
                SoundWaveNetwork.sendStatus(player);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide()) {
            if(mode==0){ SoundJammerSystem.activate(player); SoundWaveNetwork.send(player); }
            else SoundJammerSystem.activateFear(player);
            level.playSound(null, player.blockPosition(),
                    mode==0?SoundEvents.WARDEN_SONIC_BOOM:SoundEvents.RAVAGER_ROAR,
                    SoundSource.PLAYERS, mode==0?1.4F:1.1F, mode==0?0.72F:0.58F);
            PersistentData.of(player).putLong(cooldownKey,
                    System.currentTimeMillis() + (mode==0?20_000L:30_000L));
            SoundWaveNetwork.sendStatus(player);
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.smartmobs.sound_jammer.desc.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.smartmobs.sound_jammer.desc.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.smartmobs.sound_jammer.desc.3").withStyle(ChatFormatting.GRAY));
    }
}