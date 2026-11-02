package com.kevin.widgetstack;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Widget identity, rather than position, keeps selection stable across edits. */
public final class StackState {
    public final int id;
    public String name;
    /** Keep the tap-to-switch rail visible on the home-screen widget. */
    public boolean showRail = true;
    public static final int TOP=0, BOTTOM=1, LEFT=2, RIGHT=3;
    public int railEdge=RIGHT, railWidth=36, railOpacity=20;
    public boolean animateSwitch=false, buttonsOnly=false, overlapBar=false, autoContract=false;
    public boolean cornerArrows=false;
    public int cornerArrowSize=32;
    public int contractedWidth=8;
    public int contractDelaySeconds=2;
    public boolean contractToCorner=false;
    /** 0: top/left end; 1: bottom/right end of the chosen edge. */
    public int cornerEnd=0;
    public long contractDelayMillis(){return Math.max(2L,contractDelaySeconds)*1000L;}
    public boolean horizontalRail(){return railEdge==TOP||railEdge==BOTTOM;}
    public void step(int direction){
        normalize(); if(!children.isEmpty())selected=children.get(Math.floorMod(children.indexOf(selected)+(direction<0?-1:1),children.size()));
    }
    public final List<Integer> children = new ArrayList<>();
    private final Map<Integer,Integer> logicalSizePercent = new HashMap<>();
    private final Map<Integer,Integer> widthsPx = new HashMap<>(), heightsPx = new HashMap<>();
    private final Map<Integer,Integer> alignments=new HashMap<>();
    public int alignment(int child){return alignments.getOrDefault(child,4);}
    public void setAlignment(int child,int value){if(children.contains(child))alignments.put(child,Math.max(0,Math.min(8,value)));}
    private final Map<Integer,Integer> visualScales = new HashMap<>();
    public int visualScalePercent(int child){return visualScales.getOrDefault(child,100);}
    public void setVisualScalePercent(int child,int percent){if(children.contains(child))visualScales.put(child,Math.max(25,Math.min(300,percent)));}
    public static final int MAX_SIZE_PX = 4000;
    public int widthPx(int child) { return widthsPx.getOrDefault(child,0); }
    public int heightPx(int child) { return heightsPx.getOrDefault(child,0); }
    public void setDimensions(int child,int width,int height) {
        if(!children.contains(child))return;
        widthsPx.put(child,Math.max(0,Math.min(MAX_SIZE_PX,width)));
        heightsPx.put(child,Math.max(0,Math.min(MAX_SIZE_PX,height)));
        setLogicalSizePercent(child,100);
    }
    public int selected = -1;
    public StackState(int id) { this.id = id; name = "Stack " + id; }
    public void add(int child) { if (!children.contains(child)) children.add(child); logicalSizePercent.putIfAbsent(child,100); normalize(); }
    public int logicalSizePercent(int child) { return Math.max(100, Math.min(150, logicalSizePercent.getOrDefault(child,100))); }
    public void setLogicalSizePercent(int child,int percent) { if(children.contains(child)) logicalSizePercent.put(child,Math.max(100,Math.min(150,percent))); }
    public void remove(int child) {
        int old = children.indexOf(child);
        children.remove(Integer.valueOf(child));
        alignments.remove(child);visualScales.remove(child);logicalSizePercent.remove(child);widthsPx.remove(child);heightsPx.remove(child);
        if (selected == child) selected = children.isEmpty() ? -1 : children.get(Math.min(Math.max(old, 0), children.size()-1));
        normalize();
    }
    public void move(int from, int to) {
        if (from < 0 || to < 0 || from >= children.size() || to >= children.size()) return;
        children.add(to, children.remove(from)); normalize();
    }
    public void select(int child) { if (children.contains(child)) selected = child; }
    public void normalize() {
        cornerArrowSize=Math.max(16,Math.min(80,cornerArrowSize));
        if(railEdge<TOP||railEdge>RIGHT)railEdge=RIGHT;
        contractDelaySeconds=Math.max(2,contractDelaySeconds);cornerEnd=cornerEnd==1?1:0;
        railWidth=Math.max(8,Math.min(80,railWidth)); railOpacity=Math.max(0,Math.min(100,railOpacity)); contractedWidth=Math.max(4,Math.min(railWidth-1,contractedWidth));
        alignments.keySet().retainAll(children);visualScales.keySet().retainAll(children);widthsPx.keySet().retainAll(children);heightsPx.keySet().retainAll(children);
        logicalSizePercent.keySet().retainAll(children);for(int child:children)logicalSizePercent.put(child,Math.max(100,Math.min(150,logicalSizePercent.getOrDefault(child,100))));
        if (!children.contains(selected)) selected = children.isEmpty() ? -1 : children.get(0); }
}
