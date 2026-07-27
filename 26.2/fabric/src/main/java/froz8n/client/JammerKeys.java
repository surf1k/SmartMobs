package froz8n.client;

import froz8n.SmartMobs;
import froz8n.combat.SoundWaveNetwork;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public final class JammerKeys {
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(SmartMobs.MODID,"jammer"));
    private static final KeyMapping DOWN=KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.smartmobs.jammer_down",264,CATEGORY));
    private static final KeyMapping UP=KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.smartmobs.jammer_up",265,CATEGORY));
    private JammerKeys(){}

    /**
     * Fabric has no key-press event, so the bindings are polled once per client tick.
     * The scroll wheel arrives from {@code froz8n.mixin.client.MouseHandlerMixin}.
     */
    public static void register(){
        ClientTickEvents.END_CLIENT_TICK.register(client->onTick());
    }

    /** @return {@code true} when the scroll was consumed and must not reach the hotbar. */
    public static boolean onScroll(double deltaY){
        if(!holdingJammer())return false;
        var mc=Minecraft.getInstance();
        boolean shift=com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                ||com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        if(!shift)return false;
        int mode=deltaY>0?0:1;
        JammerHud.selectLocal(mode);setMode(mode);
        return true;
    }

    private static void onTick(){
        if(!holdingJammer())return;
        if(DOWN.consumeClick()){JammerHud.selectLocal(1);setMode(1);}
        if(UP.consumeClick()){JammerHud.selectLocal(0);setMode(0);}
    }

    private static void setMode(int mode){
        if(ClientPlayNetworking.canSend(SoundWaveNetwork.SetMode.TYPE))
            ClientPlayNetworking.send(new SoundWaveNetwork.SetMode(mode));
    }

    private static boolean holdingJammer(){
        var mc=Minecraft.getInstance();
        return mc.player!=null&&(mc.player.getMainHandItem().is(SmartMobs.SOUND_JAMMER)
                ||mc.player.getOffhandItem().is(SmartMobs.SOUND_JAMMER));
    }
}
