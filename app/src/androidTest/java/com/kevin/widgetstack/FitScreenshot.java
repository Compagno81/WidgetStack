package com.kevin.widgetstack;

import android.app.UiAutomation;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import java.io.*;
import junit.framework.Assert;

/** Capture a fully composed frame; keep initial/failing frames for diagnosis. */
final class FitScreenshot {
 static void assertPixel(UiAutomation automation,Context context,String name,int x,int y,int expected) throws Exception {
  long deadline=SystemClock.elapsedRealtime()+20000;int actual=0;boolean first=true;
  while(SystemClock.elapsedRealtime()<deadline){
   Bitmap frame=automation.takeScreenshot();Assert.assertNotNull("Screenshot must be available",frame);
   try{
    actual=frame.getPixel(x,y);
    if(first&&actual!=expected)save(context,name+"-initial",frame);
    first=false;
    if(actual==expected){save(context,name,frame);return;}
   }finally{frame.recycle();}
   Thread.sleep(150);
  }
  Bitmap last=automation.takeScreenshot();if(last!=null)try{save(context,name+"-failure",last);}finally{last.recycle();}
  Assert.fail("Provider edge pixel did not settle: expected="+Integer.toHexString(expected)+" actual="+Integer.toHexString(actual));
 }
 static void save(Context context,String name,Bitmap frame) throws IOException {
  File directory=context.getExternalFilesDir("fit-evidence");Assert.assertNotNull(directory);
  if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create screenshot directory");
  try(FileOutputStream output=new FileOutputStream(new File(directory,name+".png"))){Assert.assertTrue("Screenshot compression failed",frame.compress(Bitmap.CompressFormat.PNG,100,output));}
 }
}
