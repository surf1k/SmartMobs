package froz8n.client;

import froz8n.SmartMobs;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

public final class JammerKeys {
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(SmartMobs.MODID,"jammer"));
    private static final KeyMapping DOWN=new KeyMapping("key.smartmobs.jammer_down",264,CATEGORY);
    private static final KeyMapping UP=new KeyMapping("key.smartmobs.jammer_up",265,CATEGORY);
    private JammerKeys(){}
    public static void registerMappings(RegisterKeyMappingsEvent e){e.registerCategory(CATEGORY);e.register(DOWN);e.register(UP);}
    public static void registerInput(){
        NeoForge.EVENT_BUS.addListener(JammerKeys::onKey);
        NeoForge.EVENT_BUS.addListener(JammerKeys::onScroll);
    }
    private static void onScroll(InputEvent.MouseScrollingEvent event){
        var mc=net.minecraft.client.Minecraft.getInstance();
        boolean shift=com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                ||com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        if(mc.player==null||!shift
                ||(!mc.player.getMainHandItem().is(SmartMobs.SOUND_JAMMER.get())
                &&!mc.player.getOffhandItem().is(SmartMobs.SOUND_JAMMER.get())))return;
        int mode=event.getScrollDeltaY()>0?0:1;
        JammerHud.selectLocal(mode);setMode(mode);
        event.setCanceled(true);
    }
    private static void onKey(InputEvent.Key e){
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.player==null||(!mc.player.getMainHandItem().is(SmartMobs.SOUND_JAMMER.get())
                &&!mc.player.getOffhandItem().is(SmartMobs.SOUND_JAMMER.get())))return;
        if(DOWN.consumeClick()){JammerHud.selectLocal(1);setMode(1);}
        if(UP.consumeClick()){JammerHud.selectLocal(0);setMode(0);}
    }
    private static void setMode(int mode){
        ClientPacketDistributor.sendToServer(new froz8n.combat.SoundWaveNetwork.SetMode(mode));
    }
}
