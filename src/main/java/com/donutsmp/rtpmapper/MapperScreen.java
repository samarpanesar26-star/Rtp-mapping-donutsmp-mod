package com.donutsmp.rtpmapper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.List;

public final class MapperScreen extends Screen {
    private final Screen parent;
    private double zoom = 1.0;
    private double centerX = 0.0;
    private double centerZ = 0.0;
    private double panX = 0.0;
    private double panZ = 0.0;
    private double dragLastX, dragLastY;
    private boolean dragging;
    private boolean sessionOnly;

    private static final int CYAN = 0xFF7DF9FF;
    private static final int DIM_CYAN = 0xFF1E7080;
    private static final int PANEL = 0xE90A111C;
    private static final int PANEL2 = 0xE6101724;
    private static final int WHITE = 0xFFE6EEF8;
    private static final int MUTED = 0xFF98A7B8;
    private static final int BLUE = 0xFF3E9CFF;
    private static final int GREEN = 0xFF48E0A2;

    public MapperScreen(Screen parent) {
        super(Text.literal("DonutSMP RTP Mapper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = 26;
        addDrawableChild(ButtonWidget.builder(Text.literal(DonutRtpMapperClient.isMappingEnabled() ? "Stop Mapping" : "Start Mapping"), b -> {
            DonutRtpMapperClient.setMappingEnabled(!DonutRtpMapperClient.isMappingEnabled());
            clearAndRebuild();
        }).dimensions(20, y, 150, 26).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear Data"), b -> {
            DonutRtpMapperClient.STORE.clear();
            DonutRtpMapperClient.STORE.save(client);
        }).dimensions(158, y, 120, 26).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Export CSV"), b -> {
            try { DonutRtpMapperClient.STORE.exportCsv(client); } catch (IOException ignored) { }
        }).dimensions(286, y, 120, 26).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset View"), b -> resetView()).dimensions(414, y, 120, 26).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"), b -> {}).dimensions(542, y, 120, 26).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(sessionOnly ? "View All" : "View Session"), b -> {
            sessionOnly = !sessionOnly; clearAndRebuild();
        }).dimensions(width - 190, y, 170, 26).build());
        resetView();
    }

    private void clearAndRebuild() {
        if (client != null) client.setScreen(new MapperScreen(parent));
    }

    private void resetView() {
        List<RtpPoint> pts = sessionOnly ? DonutRtpMapperClient.STORE.sessionPoints() : DonutRtpMapperClient.STORE.all();
        if (client != null && client.player != null) { centerX = client.player.getX(); centerZ = client.player.getZ(); }
        else if (!pts.isEmpty()) { centerX = pts.get(pts.size()-1).x(); centerZ = pts.get(pts.size()-1).z(); }
        zoom = 1.0; panX = panZ = 0;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xD0080D16);
        ctx.drawText(textRenderer, "DonutSMP RTP Mapper", 20, 8, CYAN, false);
        ctx.drawText(textRenderer, DonutRtpMapperClient.isMappingEnabled() ? "MAPPING" : "STOPPED", 290, 8,
                DonutRtpMapperClient.isMappingEnabled() ? GREEN : MUTED, false);

        int left = 20, top = 64, leftW = Math.min(420, width / 3), gap = 16;
        int mapL = left + leftW + gap, mapT = 64, mapR = width - 20, mapB = height - 24;

        panel(ctx, left, top, leftW, 250);
        panel(ctx, left, top + 264, leftW, height - (top + 264) - 24);
        panel(ctx, mapL, mapT, mapR - mapL, mapB - mapT);
        drawLeft(ctx, left + 12, top + 12);
        drawStats(ctx, left + 12, top + 276);
        drawMap(ctx, mapL, mapT, mapR, mapB);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void panel(DrawContext c, int x, int y, int w, int h) {
        c.fill(x, y, x + w, y + h, PANEL);
        c.fill(x, y, x + w, y + 1, DIM_CYAN);
        c.fill(x, y + h - 1, x + w, y + h, DIM_CYAN);
        c.fill(x, y, x + 1, y + h, DIM_CYAN);
        c.fill(x + w - 1, y, x + w, y + h, DIM_CYAN);
    }

    private void drawLeft(DrawContext c, int x, int y) {
        int yy = y;
        heading(c, "MAPPER STATUS", x, yy); yy += 20;
        RtpStore s = DonutRtpMapperClient.STORE;
        c.drawText(textRenderer, "Samples       " + s.sessionSize() + " session / " + s.size() + " total", x, yy, WHITE, false); yy += 16;
        String pos = client.player == null ? "- / - / -" : String.format("%.0f / %.0f / %.0f", client.player.getX(), client.player.getY(), client.player.getZ());
        c.drawText(textRenderer, "Current X/Y/Z " + pos, x, yy, WHITE, false); yy += 16;
        c.drawText(textRenderer, "Next RTP      " + (DonutRtpMapperClient.isMappingEnabled() ? "Waiting for /rtp" : "Idle"), x, yy, MUTED, false); yy += 16;
        c.drawText(textRenderer, "Session       #" + s.session(), x, yy, MUTED, false); yy += 16;
        c.drawText(textRenderer, DonutRtpMapperClient.isMappingEnabled() ? "DonutSMP server accepted" : "Mapper stopped", x, yy, GREEN, false);
    }

    private void drawStats(DrawContext c, int x, int y) {
        heading(c, "STATISTICS", x, y); y += 20;
        List<RtpPoint> pts = DonutRtpMapperClient.STORE.all();
        if (pts.isEmpty()) { c.drawText(textRenderer, "No RTP samples yet.", x, y, MUTED, false); return; }
        double minX=Double.MAX_VALUE,maxX=-Double.MAX_VALUE,minZ=Double.MAX_VALUE,maxZ=-Double.MAX_VALUE,sumX=0,sumZ=0;
        for (RtpPoint p: pts) { minX=Math.min(minX,p.x()); maxX=Math.max(maxX,p.x()); minZ=Math.min(minZ,p.z()); maxZ=Math.max(maxZ,p.z()); sumX+=p.x(); sumZ+=p.z(); }
        double meanX=sumX/pts.size(), meanZ=sumZ/pts.size();
        double radiusMin=Double.MAX_VALUE,radiusMax=0;
        for (RtpPoint p:pts) { double r=Math.hypot(p.x(),p.z()); radiusMin=Math.min(radiusMin,r); radiusMax=Math.max(radiusMax,r); }
        c.drawText(textRenderer, String.format("Mean X / Z    %.1fk / %.1fk", meanX/1000, meanZ/1000), x, y, WHITE, false); y+=16;
        c.drawText(textRenderer, String.format("X range       %.1fk .. %.1fk", minX/1000, maxX/1000), x, y, WHITE, false); y+=16;
        c.drawText(textRenderer, String.format("Z range       %.1fk .. %.1fk", minZ/1000, maxZ/1000), x, y, WHITE, false); y+=16;
        c.drawText(textRenderer, String.format("Radius range  %.1fk .. %.1fk", radiusMin/1000, radiusMax/1000), x, y, WHITE, false); y+=24;
        heading(c, "QUADRANTS", x, y); y+=18;
        int ne=0,nw=0,se=0,sw=0;
        for(RtpPoint p:pts){ if(p.x()>=0&&p.z()<0)ne++; else if(p.x()<0&&p.z()<0)nw++; else if(p.x()>=0)se++; else sw++; }
        c.drawText(textRenderer,String.format("NE %3.0f%%   NW %3.0f%%",100.0*ne/pts.size(),100.0*nw/pts.size()),x,y,WHITE,false);y+=16;
        c.drawText(textRenderer,String.format("SE %3.0f%%   SW %3.0f%%",100.0*se/pts.size(),100.0*sw/pts.size()),x,y,WHITE,false);y+=24;
        heading(c,"RADIUS BUCKETS · WHEEL",x,y);y+=18;
        int[] buckets=new int[10]; for(RtpPoint p:pts){int b=(int)(Math.hypot(p.x(),p.z())/25000); if(b>=buckets.length)b=buckets.length-1;buckets[b]++;}
        for(int i=0;i<10;i++){c.drawText(textRenderer,String.format("%-8s %d",(i*25)+"k-"+((i+1)*25)+"k",buckets[i]),x,y, i>=8?CYAN:MUTED,false);y+=15;}
        c.drawText(textRenderer,"Version 1.0.0 · MC 1.21.1",x,height-38,MUTED,false);
    }

    private void heading(DrawContext c,String s,int x,int y){c.drawText(textRenderer,s,x,y,CYAN,false);}

    private void drawMap(DrawContext c,int l,int t,int r,int b){
        int w=r-l,h=b-t; c.fill(l+1,t+1,r-1,b-1,PANEL2);
        double cx=centerX+panX, cz=centerZ+panZ; double unitsPerPixel=1800.0/zoom;
        int midX=(l+r)/2, midY=(t+b)/2;
        for(int gx=-8;gx<=8;gx++){int px=midX+(int)(gx*50000/unitsPerPixel); if(px>=l&&px<=r)c.fill(px,t,px+1,b,0x403F6175);}
        for(int gz=-8;gz<=8;gz++){int py=midY+(int)(gz*50000/unitsPerPixel); if(py>=t&&py<=b)c.fill(l,py,r,py+1,0x403F6175);}
        double[] rings={25000,50000,75000,100000,150000,200000,250000,300000,350000,400000};
        for(double radius:rings){drawCircle(c,midX,midY,(int)(radius/unitsPerPixel),0x70305A72);}
        c.fill(midX,t,midX+2,b,CYAN); c.fill(l,midY,r,midY+1,CYAN);
        List<RtpPoint> pts=sessionOnly?DonutRtpMapperClient.STORE.sessionPoints():DonutRtpMapperClient.STORE.all();
        for(RtpPoint p:pts){int px=midX+(int)((p.x()-cx)/unitsPerPixel), py=midY+(int)((p.z()-cz)/unitsPerPixel); if(px>=l&&px<r&&py>=t&&py<b){c.fill(px-2,py-2,px+3,py+3,BLUE);}}
        if(client.player!=null){int px=midX+(int)((client.player.getX()-cx)/unitsPerPixel),py=midY+(int)((client.player.getZ()-cz)/unitsPerPixel);c.fill(px-3,py-3,px+4,py+4,GREEN);}
        c.drawText(textRenderer,"X",r-18, t+6,WHITE,false); c.drawText(textRenderer,"Z",midX+6,t+6,WHITE,false);
        c.drawText(textRenderer,String.format("Center %.0f, %.0f  ·  Zoom %.2fx",cx,cz,zoom),l+8,b-16,MUTED,false);
        c.drawText(textRenderer,"Saved "+DonutRtpMapperClient.STORE.size()+" all-time samples",l+8,b-31,MUTED,false);
    }

    private void drawCircle(DrawContext c,int cx,int cy,int radius,int color){
        if(radius<2)return; int steps=180; int lastX=cx+radius,lastY=cy;
        for(int i=1;i<=steps;i++){double a=i*Math.PI*2/steps;int x=cx+(int)(Math.cos(a)*radius),y=cy+(int)(Math.sin(a)*radius); if(Math.abs(x-lastX)<=3&&Math.abs(y-lastY)<=3)c.fill(x,y,x+1,y+1,color);lastX=x;lastY=y;}
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){
        if(mouseX>680){zoom*=verticalAmount>0?1.15:0.87;zoom=Math.max(0.2,Math.min(6.0,zoom));return true;}return super.mouseScrolled(mouseX,mouseY,horizontalAmount,verticalAmount);
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){dragging=true;dragLastX=mouseX;dragLastY=mouseY;return super.mouseClicked(mouseX,mouseY,button);}
    @Override public boolean mouseReleased(double mouseX,double mouseY,int button){dragging=false;return super.mouseReleased(mouseX,mouseY,button);}
    @Override public boolean mouseDragged(double mouseX,double mouseY,int button,double deltaX,double deltaY){
        if(dragging){double unitsPerPixel=1800.0/zoom;panX-=deltaX*unitsPerPixel;panZ-=deltaY*unitsPerPixel;return true;}return super.mouseDragged(mouseX,mouseY,button,deltaX,deltaY);
    }
    @Override public void close(){if(client!=null)client.setScreen(parent);}
}
