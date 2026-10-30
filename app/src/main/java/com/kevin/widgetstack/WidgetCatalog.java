package com.kevin.widgetstack;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import java.util.*;

/** Query accessible Android widget profiles; package visibility is declared in the manifest. */
final class WidgetCatalog {
    static List<AppWidgetProviderInfo> installed(Context c){
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        Map<String,AppWidgetProviderInfo> found=new LinkedHashMap<>();
        try{add(c,found,manager.getInstalledProviders());}catch(RuntimeException e){Log.w("WidgetStack","Unable to list widgets",e);}
        UserManager users=c.getSystemService(UserManager.class);
        if(users!=null)for(UserHandle profile:users.getUserProfiles()){
            try{add(c,found,manager.getInstalledProvidersForProfile(profile));}
            catch(RuntimeException e){Log.w("WidgetStack","Widget profile is unavailable",e);}
        }
        return new ArrayList<>(found.values());
    }
    private static void add(Context c,Map<String,AppWidgetProviderInfo> found,List<AppWidgetProviderInfo> providers){
        for(AppWidgetProviderInfo info:providers){
            if(info.provider==null||info.provider.getPackageName().equals(c.getPackageName()))continue;
            found.put(info.provider.flattenToString()+":"+info.getProfile(),info);
        }
    }
    static String searchKey(String value){
        if(value==null)return "";
        StringBuilder result=new StringBuilder();
        value.toLowerCase(Locale.ROOT).codePoints().filter(Character::isLetterOrDigit).forEach(result::appendCodePoint);
        return result.toString();
    }
    static boolean matches(String query,String... values){
        String key=searchKey(query);if(key.isEmpty())return true;
        for(String value:values)if(searchKey(value).contains(key))return true;
        return false;
    }
}
