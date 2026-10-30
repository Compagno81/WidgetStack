package com.kevin.widgetstack;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;

/** Small, dependency-free Material-style building blocks for the app's native Views UI. */
final class Ui {
 static final int BG=0xfff6f6fa, SURFACE=0xffffffff, SURFACE_TONAL=0xffeef0f8;
 static final int PRIMARY=0xff4f5f91, ON_PRIMARY=Color.WHITE, TEXT=0xff191b22, MUTED=0xff5e6069, OUTLINE=0xffc5c6d0;
 static int dp(Activity a,float n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 static LinearLayout page(Activity a,String title){
  ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setClipToPadding(true);scroll.setBackgroundColor(BG);
  LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,20),dp(a,16),dp(a,20),dp(a,32));scroll.addView(root);
  scroll.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets b=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);return in;});
  a.setContentView(scroll);heading(a,root,title);return root;
 }
 static TextView heading(Activity a,LinearLayout root,String value){TextView t=text(a,root,value,30);t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));t.setPadding(0,dp(a,8),0,dp(a,18));return t;}
 static TextView text(Activity a,LinearLayout root,String value,int size){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(size>=20?TEXT:size>=16?TEXT:MUTED);t.setLineSpacing(dp(a,2),1f);t.setPadding(0,dp(a,5),0,dp(a,7));root.addView(t);return t;}
 static TextView section(Activity a,LinearLayout root,String value){TextView t=text(a,root,value,19);t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));t.setPadding(dp(a,2),dp(a,18),dp(a,2),dp(a,8));return t;}
 static LinearLayout card(Activity a,LinearLayout parent){LinearLayout c=new LinearLayout(a);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(a,16),dp(a,14),dp(a,16),dp(a,14));c.setBackground(shape(SURFACE,dp(a,20),OUTLINE,dp(a,1)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(a,10);parent.addView(c,lp);return c;}
 static LinearLayout row(Activity a,LinearLayout parent){LinearLayout r=new LinearLayout(a);r.setGravity(Gravity.CENTER_VERTICAL);parent.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
 static Button button(Activity a,LinearLayout root,String label,Runnable action){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(PRIMARY);b.setMinHeight(dp(a,48));b.setPadding(dp(a,18),0,dp(a,18),0);b.setBackground(ripple(SURFACE_TONAL,dp(a,18)));b.setElevation(0);b.setStateListAnimator(null);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(a,4);lp.bottomMargin=dp(a,4);root.addView(b,lp);return b;}
 static Button tonalButton(Activity a,LinearLayout root,String label,Runnable action){return button(a,root,label,action);}
 static Button primaryButton(Activity a,LinearLayout root,String label,Runnable action){Button b=button(a,root,label,action);b.setTextColor(ON_PRIMARY);b.setBackground(ripple(PRIMARY,dp(a,18)));return b;}
 static Button compactButton(Activity a,LinearLayout parent,String label,Runnable action){Button b=new Button(a);b.setText(label);b.setAllCaps(false);b.setSingleLine(true);b.setTextSize(13);b.setTextColor(PRIMARY);b.setMinHeight(dp(a,40));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(a,4),0,dp(a,4),0);b.setBackground(ripple(SURFACE_TONAL,dp(a,14)));b.setElevation(0);b.setStateListAnimator(null);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(a,3),dp(a,4),dp(a,3),dp(a,2));parent.addView(b,lp);return b;}
 static GradientDrawable shape(int color,int radius,int strokeColor,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);if(stroke>0)d.setStroke(stroke,strokeColor);return d;}
 static RippleDrawable ripple(int color,int radius){return new RippleDrawable(ColorStateList.valueOf(0x22000000),shape(color,radius,Color.TRANSPARENT,0),null);}
}
