package com.kevin.widgetstack;

import android.app.Activity;
import android.appwidget.*;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.*;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Native, searchable app groups with provider-supplied widget previews. */
public final class WidgetPickerActivity extends Activity {
    private final List<Group> all=new ArrayList<>(),shown=new ArrayList<>();
    private final ExecutorService images=Executors.newFixedThreadPool(2);
    private final LruCache<String,Drawable> cache=new LruCache<>(20);
    private ExpandableListView list;private Adapter adapter;private EditText search;
    private int dp(float n){return Ui.dp(this,n);}
    private static final class Group {
        String label,key;Drawable icon;final List<AppWidgetProviderInfo> providers=new ArrayList<>();
    }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);setResult(RESULT_CANCELED);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets s=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(s.left,s.top,s.right,Math.max(s.bottom,in.getInsets(WindowInsets.Type.ime()).bottom));return in;});
        setContentView(root);
        Toolbar toolbar=new Toolbar(this);toolbar.setTitle("Choose a widget");toolbar.setBackgroundColor(Ui.BG);toolbar.setTitleTextColor(Ui.TEXT);toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);toolbar.setNavigationContentDescription("Close");toolbar.setNavigationOnClickListener(v->finish());root.addView(toolbar);
        toolbar.getMenu().add("Refresh").setOnMenuItemClickListener(item->{reload();return true;});
        search=new EditText(this);search.setSingleLine();search.setTextSize(17);search.setHint("Search apps and widgets");search.setPadding(dp(20),dp(12),dp(16),dp(12));search.setBackground(rounded(Ui.SURFACE_TONAL,28));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(56));sp.setMargins(dp(16),dp(8),dp(16),dp(16));root.addView(search,sp);
        list=new ExpandableListView(this);list.setGroupIndicator(null);list.setDivider(null);list.setChildDivider(null);list.setSelector(android.R.color.transparent);list.setClipToPadding(false);list.setPadding(dp(12),0,dp(12),dp(20));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        adapter=new Adapter();list.setAdapter(adapter);
        TextView empty=new TextView(this);empty.setText("No matching widgets. Try Refresh. If an app is missing, open it once and check your home-screen widget picker.");empty.setGravity(Gravity.CENTER);empty.setPadding(dp(16),dp(32),dp(16),dp(32));root.addView(empty);list.setEmptyView(empty);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){filter(s.toString());}});
    }
    @Override protected void onResume(){super.onResume();reload();}
    private void reload(){
        if(adapter==null)return;
        Set<String> expanded=new HashSet<>();for(int i=0;i<shown.size();i++)if(list.isGroupExpanded(i))expanded.add(shown.get(i).key);
        for(int i=0;i<shown.size();i++)list.collapseGroup(i);
        all.clear();
        TreeMap<String,Group> groups=new TreeMap<>();
        for(AppWidgetProviderInfo p:WidgetCatalog.installed(this)){
            String key=p.provider.getPackageName()+":"+p.getProfile();Group g=groups.get(key);
            if(g==null){g=new Group();g.key=key;g.label=p.provider.getPackageName();try{g.label=getPackageManager().getApplicationLabel(p.getActivityInfo().applicationInfo).toString();g.icon=p.loadIcon(this,0);}catch(RuntimeException ignored){}groups.put(key,g);}
            g.providers.add(p);
        }
        all.addAll(groups.values());all.sort(Comparator.comparing(g->g.label,String.CASE_INSENSITIVE_ORDER));
        for(Group g:all)g.providers.sort(Comparator.comparing(p->p.loadLabel(getPackageManager()),String.CASE_INSENSITIVE_ORDER));
        filter(search.getText().toString());
        if(search.getText().toString().trim().isEmpty())for(int i=0;i<shown.size();i++)if(expanded.contains(shown.get(i).key))list.expandGroup(i);
    }
    private void filter(String query){
        String q=query.trim().toLowerCase(Locale.ROOT);shown.clear();
        for(Group g:all){
            if(q.isEmpty()||WidgetCatalog.matches(q,g.label,g.key)){shown.add(g);continue;}
            Group copy=new Group();copy.key=g.key;copy.label=g.label;copy.icon=g.icon;
            for(AppWidgetProviderInfo p:g.providers)if(WidgetCatalog.matches(q,p.loadLabel(getPackageManager()),p.provider.getPackageName()))copy.providers.add(p);
            if(!copy.providers.isEmpty())shown.add(copy);
        }
        adapter.notifyDataSetChanged();if(!q.isEmpty())for(int i=0;i<shown.size();i++)list.expandGroup(i);
    }
    private GradientDrawable rounded(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Drawable ripple(int color,int radius){return new RippleDrawable(android.content.res.ColorStateList.valueOf(0x224f5f91),rounded(color,radius),null);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    private View card(AppWidgetProviderInfo info){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(12),dp(12),dp(12));card.setBackground(ripple(Ui.SURFACE,20));card.setContentDescription(info.loadLabel(getPackageManager())+", "+sizeLabel(info));
        FrameLayout preview=new FrameLayout(this);card.addView(preview,new LinearLayout.LayoutParams(-1,dp(148)));
        boolean inflated=false;
        try{
            RemoteViews generated=null;
            if(Build.VERSION.SDK_INT>=35)generated=AppWidgetManager.getInstance(this).getWidgetPreview(info.provider,info.getProfile(),AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
            if(generated==null&&info.previewLayout!=0)generated=new RemoteViews(info.provider.getPackageName(),info.previewLayout);
            if(generated!=null){
                AppWidgetHostView host=new AppWidgetHostView(this);host.setAppWidget(-1,info);host.setPadding(0,0,0,0);host.updateAppWidget(generated);
                float density=getResources().getDisplayMetrics().density;
                int width=Math.max(dp(120),Math.max(info.minWidth,dp(Math.max(1,info.targetCellWidth)*70-30)));
                int height=Math.max(dp(64),Math.max(info.minHeight,dp(Math.max(1,info.targetCellHeight)*70-30)));
                PreviewScale scale=new PreviewScale(width,height);scale.addView(host);preview.addView(scale,new FrameLayout.LayoutParams(-1,-1));inflated=true;
            }
        }catch(RuntimeException|LinkageError|StackOverflowError ignored){}
        if(!inflated){
            ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.addView(image,new FrameLayout.LayoutParams(-1,-1));
            String key=info.provider.flattenToString()+info.getProfile();Drawable saved=cache.get(key);
            if(saved!=null){image.setImageDrawable(saved);preview.setBackground(rounded(previewBackdrop(saved),14));}
            else images.execute(()->{
                Drawable result=null;try{result=info.loadPreviewImage(this,0);}catch(RuntimeException ignored){}
                final Drawable drawable=result;final int backdrop=drawable==null?Color.TRANSPARENT:previewBackdrop(drawable);
                runOnUiThread(()->{if(isDestroyed()||isFinishing())return;if(drawable!=null){cache.put(key,drawable);image.setImageDrawable(drawable);preview.setBackground(rounded(backdrop,14));}else{
                    try{image.setImageDrawable(info.loadIcon(this,0));}catch(RuntimeException ignored){}
                    FrameLayout.LayoutParams icon=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER);image.setLayoutParams(icon);
                    TextView missing=text("Preview not provided",12,Ui.MUTED);FrameLayout.LayoutParams hint=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);missing.setGravity(Gravity.CENTER);preview.addView(missing,hint);
                }});
            });
        }
        TextView title=text(info.loadLabel(getPackageManager()),15,Ui.TEXT);title.setMaxLines(2);title.setPadding(0,dp(10),0,dp(4));card.addView(title);
        card.addView(text(sizeLabel(info),13,Ui.MUTED));
        float density=getResources().getDisplayMetrics().density;
        card.addView(text(Math.round(info.minWidth/density)+" × "+Math.round(info.minHeight/density)+" dp",12,Ui.MUTED));
        card.setOnClickListener(v->{setResult(RESULT_OK,new Intent().putExtra("provider",info));finish();});return card;
    }
    // Some providers supply a white clock face on a transparent preview image.
    // Give those images contrast without recolouring the provider's artwork.
    private static int previewBackdrop(Drawable drawable){
        Bitmap sample=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);Rect bounds=new Rect(drawable.getBounds());
        try{
            drawable.setBounds(0,0,32,32);drawable.draw(new Canvas(sample));int count=0;long brightness=0;
            for(int y=0;y<32;y++)for(int x=0;x<32;x++){int pixel=sample.getPixel(x,y);if(Color.alpha(pixel)>=128){count++;brightness+=(Color.red(pixel)*299+Color.green(pixel)*587+Color.blue(pixel)*114)/1000;}}
            return count>0&&count<820&&brightness/count>170?0xff52645a:Color.TRANSPARENT;
        }catch(RuntimeException ignored){return Color.TRANSPARENT;}
        finally{drawable.setBounds(bounds);sample.recycle();}
    }
    private String sizeLabel(AppWidgetProviderInfo p){
        if(p.targetCellWidth>0&&p.targetCellHeight>0)return p.targetCellWidth+" × "+p.targetCellHeight;
        float d=getResources().getDisplayMetrics().density;
        return "≈ "+Math.max(1,(int)Math.ceil((p.minWidth/d+30)/70))+" × "+Math.max(1,(int)Math.ceil((p.minHeight/d+30)/70));
    }
    private final class PreviewScale extends FrameLayout {
        final int width,height;float scale;
        PreviewScale(int w,int h){super(WidgetPickerActivity.this);width=w;height=h;setClipChildren(true);}
        @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){return true;}
        @Override protected void onMeasure(int ws,int hs){int w=MeasureSpec.getSize(ws),h=MeasureSpec.getSize(hs);setMeasuredDimension(w,h);if(getChildCount()>0)getChildAt(0).measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(height,MeasureSpec.EXACTLY));scale=Math.min((float)w/width,(float)h/height);}
        @Override protected void onLayout(boolean changed,int l,int t,int r,int b){if(getChildCount()>0){View v=getChildAt(0);v.layout(0,0,width,height);v.setPivotX(0);v.setPivotY(0);v.setScaleX(scale);v.setScaleY(scale);v.setTranslationX((getWidth()-width*scale)/2);v.setTranslationY((getHeight()-height*scale)/2);}}
        @Override public boolean onTouchEvent(android.view.MotionEvent event){return false;}
    }
    private final class Adapter extends BaseExpandableListAdapter {
        public int getGroupCount(){return shown.size();}public int getChildrenCount(int g){return 1;}public Object getGroup(int g){return shown.get(g);}public Object getChild(int g,int c){return shown.get(g).providers;}public long getGroupId(int g){return g;}public long getChildId(int g,int c){return c;}public boolean hasStableIds(){return false;}public boolean isChildSelectable(int g,int c){return false;}
        public View getGroupView(int pos,boolean expanded,View reuse,android.view.ViewGroup parent){
            Group g=shown.get(pos);LinearLayout row=new LinearLayout(WidgetPickerActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(14),dp(12),dp(14));row.setBackground(ripple(Ui.BG,16));
            ImageView icon=new ImageView(WidgetPickerActivity.this);icon.setImageDrawable(g.icon);row.addView(icon,new LinearLayout.LayoutParams(dp(40),dp(40)));
            LinearLayout labels=new LinearLayout(WidgetPickerActivity.this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(16),0,dp(8),0);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));labels.addView(text(g.label,17,Ui.TEXT));labels.addView(text(g.providers.size()+" widget"+(g.providers.size()==1?"":"s"),13,Ui.MUTED));row.addView(text(expanded?"⌃":"⌄",22,Ui.MUTED));return row;
        }
        public View getChildView(int g,int c,boolean last,View reuse,android.view.ViewGroup parent){
            LinearLayout rows=new LinearLayout(WidgetPickerActivity.this);rows.setOrientation(LinearLayout.VERTICAL);
            List<AppWidgetProviderInfo> pp=shown.get(g).providers;
            for(int i=0;i<pp.size();i+=2){LinearLayout row=new LinearLayout(WidgetPickerActivity.this);rows.addView(row);for(int j=0;j<2;j++){
                View v=i+j<pp.size()?card(pp.get(i+j)):new View(WidgetPickerActivity.this);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(4),dp(4),dp(4),dp(8));row.addView(v,lp);
            }}return rows;
        }
    }
    @Override protected void onDestroy(){images.shutdownNow();cache.evictAll();super.onDestroy();}
}
