/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.TabActivityListeners;
import com.cnh.android.widget.control.TabActivityTab;
import com.cnh.pf.jgroups.ChannelModule;
import com.google.inject.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.RoboGuice;
import roboguice.RoboGuiceHelper;
import roboguice.activity.event.OnPauseEvent;
import roboguice.activity.event.OnResumeEvent;
import roboguice.event.EventManager;
import roboguice.util.RoboContext;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Management Tab Activity
 * Contains test UI, will later include Data Management, Data Sync, Import & Export
 */
public class DataManagementActivity extends TabActivity implements RoboContext {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementActivity.class);

   protected HashMap<Key<?>,Object> scopedObjects = new HashMap<Key<?>, Object>();
   protected EventManager eventManager;

   private class DataManagementTabListener implements TabActivityListeners.TabListener {
      private final Activity a;
      private final Fragment fragment;

      public DataManagementTabListener(Fragment fragment, Activity a) {
         this.fragment = fragment;
         this.a = a;
      }

      @Override
      public void onTabUnselected(TabActivityTab tab) {
         a.getFragmentManager().beginTransaction().detach(fragment).commit();
      }

      @Override
      public void onTabSelected(TabActivityTab tab) {
         Fragment existing = a.getFragmentManager().findFragmentByTag(tab.getTabId());
         final FragmentTransaction ft = a.getFragmentManager().beginTransaction();
         if (existing != null) {
            ft.attach(existing);
         }
         else {
            ft.add(R.id.fragment_content, fragment, tab.getTabId());
         }
         ft.commit();
      }

      @Override
      public void onTabClickable(TabActivityTab tabActivityTab, boolean b) {

      }

      @Override
      public void onTabGrayedout(TabActivityTab tabActivityTab, boolean b) {

      }

      @Override
      public void onTabDisabled(TabActivityTab tab) {

      }

      @Override
      public void onTabEnabled(TabActivityTab tab) {

      }

      @Override
      public void onTabHidden(TabActivityTab tab) {

      }

      @Override
      public void onTabShown(TabActivityTab tab) {

      }

      @Override
      public void onTabAdded(TabActivityTab tab) {

      }

      @Override
      public void onTabRemoved(TabActivityTab tab) {
         a.getFragmentManager().beginTransaction().remove(fragment).commit();
      }
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" },
         new RoboModule(app), new ChannelModule(app));
      super.onCreate(savedInstanceState);

      eventManager = RoboGuice.getInjector(this).getInstance(EventManager.class);
      TabActivityTab importTab = new TabActivityTab(R.string.tab_import, R.drawable.tab_import, getResources().getString(R.string.tab_import),
            new DataManagementTabListener(new ImportFragment(), this));
      addTab(importTab);
      TabActivityTab exportTab = new TabActivityTab(R.string.tab_export, R.drawable.tab_export, getResources().getString(R.string.tab_export),
              new DataManagementTabListener(new ExportFragment(), this));
      addTab(exportTab);
      TabActivityTab importExportTestTab = new TabActivityTab(R.string.tab_import_test, R.drawable.tab_import, "import_export_test_tab",
            new DataManagementTabListener(new TestImportFragment(), this));
      addTab(importExportTestTab);
      setTabActivityTitle(getString(R.string.app_name));
      selectTabAtPosition(0);
   }

   @Override
   public Map<Key<?>, Object> getScopedObjectMap() {
      return scopedObjects;
   }

   /**
    * @return true if name begins with a lowercase character (indicating a package) and it doesn't start with com.android
    */
   protected static boolean shouldInjectOnCreateView(String name) {
      return Character.isLowerCase(name.charAt(0)) && !name.startsWith("com.android") && !name.equals("fragment");
   }

   @Override
   public View onCreateView(String name, Context context, AttributeSet attrs) {
      if (shouldInjectOnCreateView(name))
         return injectOnCreateView(name, context, attrs);

      return super.onCreateView(name, context, attrs);
   }

   @Override
   public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
      if (shouldInjectOnCreateView(name))
         return injectOnCreateView(name, context, attrs);

      return super.onCreateView(parent, name, context, attrs);
   }

   protected static View injectOnCreateView(String name, Context context, AttributeSet attrs) {
      try {
         final Constructor<?> constructor = Class.forName(name).getConstructor(Context.class, AttributeSet.class);
         final View view = (View) constructor.newInstance(context, attrs);
         RoboGuice.getInjector(context).injectMembers(view);
         RoboGuice.getInjector(context).injectViewMembers(view);
         return view;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void onResume() {
      super.onResume();
      eventManager.fire(new OnResumeEvent(this));
      logger.debug("Sending INTERNAL_DATA broadcast");
      sendBroadcast(new Intent("com.cnh.pf.data.INTERNAL_DATA").addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
   }

   @Override
   protected void onPause() {
      super.onPause();
      eventManager.fire(new OnPauseEvent(this));
   }
}
