package com.kevin.widgetstack;
import android.content.*;
import java.util.*;

/** Independent active profile per saved stack; membership uses stable child widget IDs. */
final class VisibilityProfiles {
 final Context c;final android.content.SharedPreferences p;
 VisibilityProfiles(Context c){this.c=c;p=c.getSharedPreferences("visibility_profiles",0);}
 Map<Integer,String> all(){Map<Integer,String> out=new TreeMap<>();for(String k:p.getAll().keySet())if(k.startsWith("name_"))try{int id=Integer.parseInt(k.substring(5));out.put(id,p.getString(k,"Profile"));}catch(NumberFormatException ignored){}return out;}
 Map<Integer,String> forStack(int saved){Map<Integer,String> out=new TreeMap<>();for(Map.Entry<Integer,String> e:all().entrySet())if(owner(e.getKey())==saved)out.put(e.getKey(),e.getValue());return out;}
 int owner(int id){return p.getInt("owner_"+id,-1);}
 int active(int saved){int id=p.getInt("active_"+saved,0);return forStack(saved).containsKey(id)?id:0;}
 String activeName(int saved){return active(saved)==0?"Show all widgets":all().get(active(saved));}
 int create(int saved,String name){int id=Math.max(1,p.getInt("next",1));Set<String> visible=new HashSet<>();StackState s=new StackStore(c).get(saved);if(s!=null)for(int child:s.children)visible.add(String.valueOf(child));p.edit().putInt("next",id+1).putInt("owner_"+id,saved).putString("name_"+id,name.trim()).putStringSet("visible_"+id,visible).commit();return id;}
 void rename(int id,String name){if(all().containsKey(id)&&!name.trim().isEmpty())p.edit().putString("name_"+id,name.trim()).commit();}
 Set<String> members(int id){return new HashSet<>(p.getStringSet("visible_"+id,Collections.emptySet()));}
 void members(int id,Set<String> members){if(!all().containsKey(id))return;p.edit().putStringSet("visible_"+id,new HashSet<>(members)).commit();if(active(owner(id))==id)publish(owner(id));}
 boolean activate(int saved,int id){if(!new StackStore(c).isSaved(saved)||(id!=0&&owner(id)!=saved)|| (id!=0&&!all().containsKey(id)))return false;p.edit().putInt("active_"+saved,id).commit();if(!Recovery.paused(c)){StackHost.get(c).sync();HostService.ensure(c);}publish(saved);return true;}
 void delete(int id){if(!all().containsKey(id))return;int saved=owner(id);boolean wasActive=active(saved)==id;p.edit().remove("name_"+id).remove("visible_"+id).remove("owner_"+id).commit();if(wasActive)activate(saved,0);}
 static StackState filtered(Context c,StackState state){StackStore store=new StackStore(c);int saved=store.definitionId(state.id);VisibilityProfiles profiles=new VisibilityProfiles(c);int active=profiles.active(saved);if(active==0)return state;StackState copy=StackStore.copy(state);Set<String> visible=profiles.members(active);for(int child:new ArrayList<>(copy.children))if(!visible.contains(String.valueOf(child)))copy.remove(child);return copy;}
 void publish(int saved){StackStore store=new StackStore(c);for(int id:store.placements(saved)){StackState original=store.get(id),visible=filtered(c,original);if(visible.selected>=0&&visible.selected!=original.selected)store.selectOnHome(id,visible.selected);BarController.cancel(id);StackRenderer.updateBarMotion(c,id,0,0);}}
}
