package com.kevin.widgetstack;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetSearchTest {
 @Test public void spacedAppNamesFindTheirUnspacedLabels(){
  assertTrue(WidgetCatalog.matches("duo lingo","Duolingo"));
  assertTrue(WidgetCatalog.matches("My Fitness Pal","MyFitnessPal"));
  assertTrue(WidgetCatalog.matches("my-fitness-pal","MYFITNESSPAL"));
 }
 @Test public void packageNameMatchesWhenTheAppUsesADifferentLabel(){
  assertTrue(WidgetCatalog.matches("myfitnesspal","Calorie Counter","com.myfitnesspal.android"));
  assertTrue(WidgetCatalog.matches("duo lingo","Streak","com.duolingo"));
  assertFalse(WidgetCatalog.matches("duolingo","Clock","com.example.clock"));
 }
 @Test public void partialNamesAndNonEnglishLabelsRemainSearchable(){
  assertTrue(WidgetCatalog.matches("fitness","MyFitnessPal"));
  assertTrue(WidgetCatalog.matches("école","Mon École"));
  assertTrue(WidgetCatalog.matches("  ","Any widget"));
 }
}
