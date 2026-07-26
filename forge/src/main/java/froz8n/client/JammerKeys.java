package froz8n.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

public final class JammerKeys {
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("smartmobs","jammer"));
    private static final KeyMapping DOWN=new KeyMapping("key.smartmobs.jammer_down",264,CATEGORY);
    private static final KeyMapping UP=new KeyMapping("key.smartmobs.jammer_up",265,CATEGORY);
    private JammerKeys(){}
    public static void registerMappings(RegisterKeyMappingsEvent e){e.register(DOWN);e.register(UP);}
    public static void registerInput(){
        InputEvent.Key.BUS.addListener(JammerKeys::onKey);
        InputEvent.MouseScrollingEvent.BUS.addListener(JammerKeys::onScroll);
    }
    private static boolean onScroll(InputEvent.MouseScrollingEvent event){
        var mc=net.minecraft.client.Minecraft.getInstance();
        boolean shift=com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                ||com.mojang.blaze3d.platform.InputConstants.isKeyDown(mc.getWindow(),org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        if(mc.player==null||!shift
                ||(!mc.player.getMainHandItem().is(froz8n.smartmobs.SOUND_JAMMER.get())
                &&!mc.player.getOffhandItem().is(froz8n.smartmobs.SOUND_JAMMER.get())))return false;
        int mode=event.getDeltaY()>0?0:1;
        JammerHud.selectLocal(mode);froz8n.combat.SoundWaveNetwork.setMode(mode);
        return true;
    }
    private static void onKey(InputEvent.Key e){
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(mc.player==null||(!mc.player.getMainHandItem().is(froz8n.smartmobs.SOUND_JAMMER.get())
                &&!mc.player.getOffhandItem().is(froz8n.smartmobs.SOUND_JAMMER.get())))return;
        if(DOWN.consumeClick()){JammerHud.selectLocal(1);froz8n.combat.SoundWaveNetwork.setMode(1);}
        if(UP.consumeClick()){JammerHud.selectLocal(0);froz8n.combat.SoundWaveNetwork.setMode(0);}
    }
}
