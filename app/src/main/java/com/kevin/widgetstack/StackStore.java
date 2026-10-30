package com.kevin.widgetstack;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Saved definitions own child IDs. Home-screen placements only refer to definitions. */
final class StackStore {
    private static final Object LOCK=new Object();
    private final SharedPreferences prefs;
    StackStore(Context c){prefs=c.getSharedPreferences("stacks",Context.MODE_PRIVATE);}
    private StackState raw(int id){
        String value=prefs.getString("stack_"+id,null);if(value==null)return null;
        try{return decode(id,new JSONObject(value));}catch(Exception e){return null;}
    }
    boolean isSaved(int id){return id<=-2&&raw(id)!=null;}
    boolean isPlacement(int id){return id>=0&&isSaved(prefs.getInt("placement_"+id,-1));}
    int definitionId(int id){return isPlacement(id)?prefs.getInt("placement_"+id,-1):id;}
    StackState get(int id){
        synchronized(LOCK){
            int definition=definitionId(id);StackState s=raw(definition);if(s==null)return null;
            if(definition!=id){
                s=decode(id,encode(s));s.selected=prefs.getInt("selection_"+id,s.selected);s.normalize();
            }
            return s;
        }
    }
    void put(StackState s){
        synchronized(LOCK){
            s.normalize();int definition=definitionId(s.id);
            JSONObject value=encode(s);
            if(definition!=s.id){
                StackState old=raw(definition);StackState shared=decode(definition,value);
                shared.selected=old.selected;shared.normalize();value=encode(shared);
                prefs.edit().putInt("selection_"+s.id,s.selected).commit();
            }
            prefs.edit().putString("stack_"+definition,value.toString()).commit();
        }
    }
    void selectOnHome(int id,int child){
        synchronized(LOCK){StackState s=get(id);if(s==null||!s.children.contains(child))return;
            if(isPlacement(id))prefs.edit().putInt("selection_"+id,child).commit();else{s.select(child);put(s);}
        }
    }
    List<Integer> placements(int saved){
        synchronized(LOCK){List<Integer> out=new ArrayList<>();for(String key:prefs.getAll().keySet())if(key.startsWith("placement_"))try{int id=Integer.parseInt(key.substring(10));if(prefs.getInt(key,-1)==saved&&isSaved(saved))out.add(id);}catch(NumberFormatException ignored){}Collections.sort(out);return out;}
    }
    List<StackState> saved(){List<StackState> out=new ArrayList<>();for(StackState s:all())if(s.id<=-2)out.add(s);out.sort(Comparator.comparing(s->s.name,String.CASE_INSENSITIVE_ORDER));return out;}
    List<StackState> all(){
        synchronized(LOCK){Set<Integer> ids=new TreeSet<>();for(String key:prefs.getAll().keySet())try{if(key.startsWith("stack_"))ids.add(Integer.parseInt(key.substring(6)));else if(key.startsWith("placement_"))ids.add(Integer.parseInt(key.substring(10)));}catch(NumberFormatException ignored){}
            List<StackState> out=new ArrayList<>();for(int id:ids){StackState s=get(id);if(s!=null)out.add(s);}return out;}
    }
    StackState createSaved(String name){
        synchronized(LOCK){int id=nextId();StackState s=new StackState(id);s.name=name==null||name.trim().isEmpty()?"Stack "+(-id-1):name.trim();prefs.edit().putString("stack_"+id,encode(s).toString()).putInt("next_saved_id",id-1).commit();return s;}
    }
    private int nextId(){int id=Math.min(-2,prefs.getInt("next_saved_id",-2));while(raw(id)!=null)id--;return id;}
    /** Preserve bindings, widget setup, order, names and controls in one atomic migration. */
    void migrateLegacy(){
        synchronized(LOCK){
            SharedPreferences.Editor edit=prefs.edit();int next=nextId();boolean changed=false;
            for(String key:new TreeSet<>(prefs.getAll().keySet()))if(key.startsWith("stack_"))try{
                int home=Integer.parseInt(key.substring(6));if(home<0)continue;StackState old=raw(home);if(old==null)continue;
                if(!isPlacement(home)){
                    while(raw(next)!=null)next--;
                    edit.putString("stack_"+next,encode(old).toString()).putInt("placement_"+home,next).putInt("selection_"+home,old.selected);next--;
                }
                edit.remove(key);changed=true;
            }catch(NumberFormatException ignored){}
            if(changed)edit.putInt("next_saved_id",next).commit();
        }
    }
    boolean assign(int home,int saved){
        synchronized(LOCK){if(home<0||!isSaved(saved))return false;migrateLegacy();StackState s=raw(saved);return prefs.edit().putInt("placement_"+home,saved).putInt("selection_"+home,s.selected).remove("stack_"+home).commit();}
    }
    void delete(int id){synchronized(LOCK){if(id<=-2&&!placements(id).isEmpty())throw new IllegalStateException("Remove its home-screen copies first");prefs.edit().remove("stack_"+id).remove("placement_"+id).remove("selection_"+id).commit();}}
    /** Explicit user deletion detaches every copy in the same preference commit. */
    boolean deleteSaved(int id){synchronized(LOCK){
        if(!isSaved(id))return false;SharedPreferences.Editor edit=prefs.edit();
        for(int home:placements(id))edit.remove("placement_"+home).remove("selection_"+home);
        return edit.remove("stack_"+id).commit();
    }}
    void prunePlacements(Set<Integer> active){synchronized(LOCK){for(StackState s:saved())for(int home:placements(s.id))if(!active.contains(home))delete(home);}}
    private static StackState decode(int id,JSONObject j){
        StackState s=new StackState(id);s.name=j.optString("name",s.name);s.showRail=j.optBoolean("showRail",true);
        s.railEdge=j.optInt("railEdge",StackState.RIGHT);s.railWidth=j.optInt("railWidth",36);s.railOpacity=j.optInt("railOpacity",20);s.animateSwitch=j.optBoolean("animateSwitch",false);s.buttonsOnly=j.optBoolean("buttonsOnly",false);s.overlapBar=j.optBoolean("overlapBar",false);s.autoContract=j.optBoolean("autoContract",false);s.contractedWidth=j.optInt("contractedWidth",8);s.contractDelaySeconds=j.optInt("contractDelaySeconds",2);s.contractToCorner=j.optBoolean("contractToCorner",false);s.cornerEnd=j.optInt("cornerEnd",0);
        JSONArray a=j.optJSONArray("children");if(a!=null)for(int i=0;i<a.length();i++){int child=a.optInt(i,-1);if(child>=0){s.add(child);JSONObject sizes=j.optJSONObject("logicalSizePercent");if(sizes!=null)s.setLogicalSizePercent(child,sizes.optInt(Integer.toString(child),100));}}s.selected=j.optInt("selected",-1);s.normalize();return s;
    }
    private static JSONObject encode(StackState s){
        try{s.normalize();JSONObject j=new JSONObject();j.put("name",s.name);j.put("showRail",s.showRail);j.put("railEdge",s.railEdge);j.put("railWidth",s.railWidth);j.put("railOpacity",s.railOpacity);j.put("animateSwitch",s.animateSwitch);j.put("buttonsOnly",s.buttonsOnly);j.put("overlapBar",s.overlapBar);j.put("autoContract",s.autoContract);j.put("contractedWidth",s.contractedWidth);j.put("contractDelaySeconds",s.contractDelaySeconds);j.put("contractToCorner",s.contractToCorner);j.put("cornerEnd",s.cornerEnd);j.put("children",new JSONArray(s.children));JSONObject sizes=new JSONObject();for(int child:s.children)sizes.put(Integer.toString(child),s.logicalSizePercent(child));j.put("logicalSizePercent",sizes);j.put("selected",s.selected);return j;}catch(Exception e){throw new IllegalStateException(e);}
    }
}
