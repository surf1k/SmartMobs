package froz8n.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;

public final class JammerHud {
    private static int mode; private static long downReady,upReady;
    private JammerHud(){}
    public static void addLayer(AddGuiOverlayLayersEvent e){e.getLayeredDraw().add(
            Identifier.fromNamespaceAndPath("smartmobs","jammer_hud"),JammerHud::render);}
    public static void update(int newMode,int downTicks,int upTicks){mode=newMode;long now=System.currentTimeMillis();downReady=now+downTicks;upReady=now+upTicks;}
    public static void selectLocal(int newMode){mode=newMode;}
    private static void render(GuiGraphics g,DeltaTracker delta){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||(!mc.player.getMainHandItem().is(froz8n.SmartMobs.SOUND_JAMMER.get())
                &&!mc.player.getOffhandItem().is(froz8n.SmartMobs.SOUND_JAMMER.get())))return;
        int pw=184,ph=82,x=Math.max(6,g.guiWidth()-pw-8),y=Math.max(6,g.guiHeight()-ph-8);
        g.fill(x,y,x+pw,y+ph,0xE0060D09);g.fill(x,y,x+pw,y+1,0xFF55D881);
        g.fill(x,y+ph-1,x+pw,y+ph,0xFF244A31);
        for(int sy=y+3;sy<y+ph-2;sy+=4)g.fill(x+1,sy,x+pw-1,sy+1,0x1600FF55);
        g.drawString(mc.font,Component.literal("SM-JAMMER // SYS.R2"),x+7,y+6,0xFF7CFF9D,false);
        g.drawString(mc.font,Component.literal("SHIFT + MWHEEL : SELECT"),x+7,y+17,0xFF527C5E,false);
        drawMode(g,mc,x+6,y+29,pw-12,0,downReady,30_000L,0xFF55F58A,"[01] STUN");
        drawMode(g,mc,x+6,y+51,pw-12,1,upReady,45_000L,0xFFFFC65C,"[02] PANIC");
    }
    private static void drawMode(GuiGraphics g,Minecraft mc,int x,int y,int width,int thisMode,long ready,long total,int accent,String title){
        boolean selected=mode==thisMode;long now=System.currentTimeMillis();
        g.fill(x,y,x+width,y+19,selected?0xB0122A1A:0x700A120D);g.fill(x,y,x+3,y+19,selected?accent:0xFF294333);
        g.drawString(mc.font,(selected?"> ":"  ")+title,x+8,y+4,selected?accent:0xFF6B8673,false);
        String status=ready<=now?"READY":String.format("%.1fs",(ready-now)/1000F);int sw=mc.font.width(status);
        g.drawString(mc.font,status,x+width-sw-6,y+4,ready<=now?0xFF67EF8B:0xFFFFCE73,false);
        float remaining=Math.max(0,Math.min(1,(ready-now)/(float)total));int bx=x+8,by=y+15,bw=width-16;
        g.fill(bx,by,bx+bw,by+1,0xFF203229);g.fill(bx,by,bx+(int)(bw*(1-remaining)),by+1,selected?accent:0xFF53665A);
    }
}
