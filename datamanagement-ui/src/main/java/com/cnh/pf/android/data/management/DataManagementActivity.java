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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.View;

import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.android.util.prefs.GlobalPreferencesNotAvailableException;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.vip.constants.VIPConstants;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.TabActivityListeners;
import com.cnh.android.widget.control.TabActivityTab;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.data.management.service.ServiceConstants;
import com.cnh.pf.jgroups.ChannelModule;
import com.google.inject.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.RoboGuiceHelper;
import roboguice.activity.event.OnPauseEvent;
import roboguice.activity.event.OnResumeEvent;
import roboguice.event.EventManager;
import roboguice.util.RoboContext;

/**
 * Data Management Tab Activity
 * Contains test UI, will later include Data Management, Data Sync, Import & Export
 */
public class DataManagementActivity extends TabActivity implements RoboContext, ServiceConnection {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementActivity.class);

   protected HashMap<Key<?>, Object> scopedObjects = new HashMap<Key<?>, Object>();
   protected EventManager eventManager;
   private IVIPServiceAIDL vipService;
   private WeakReference<ProductLibraryFragment> productLibraryFragmentWeakReference;

   private class DataManagementTabListener implements TabActivityListeners.TabListener {
      private final Activity a;
      private final Fragment fragment;

      public DataManagementTabListener(Fragment fragment, Activity a) {
         this.fragment = fragment;
         this.a = a;
      }

      /**
       * Returns the current ProductLibraryFragment for testing/injection
       *
       * @return
       */
      public ProductLibraryFragment getProductLibraryFragment() {
         return productLibraryFragmentWeakReference != null ? productLibraryFragmentWeakReference.get() : null;
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
      logger.debug("onCreate called");
      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" }, new RoboModule(app), new ChannelModule(app));
      super.onCreate(savedInstanceState);

      eventManager = RoboGuice.getInjector(this).getInstance(EventManager.class);
      TabActivityTab importTab = new TabActivityTab(R.string.tab_import, R.drawable.tab_import_selector, getResources().getString(R.string.tab_import),
            new DataManagementTabListener(new ImportFragment(), this));
      addTab(importTab);
      TabActivityTab exportTab = new TabActivityTab(R.string.tab_export, R.drawable.tab_export_selector, getResources().getString(R.string.tab_export),
            new DataManagementTabListener(new ExportFragment(), this));
      addTab(exportTab);
      if(hasPCM()) {
         productLibraryFragmentWeakReference = new WeakReference<ProductLibraryFragment>(new ProductLibraryFragment());
         TabActivityTab productLibraryTab = new TabActivityTab(R.string.tab_product_library, R.drawable.tab_product_library_selector, "product_library_tab",
               new DataManagementTabListener(productLibraryFragmentWeakReference.get(), this));
         addTab(productLibraryTab);
      }
      setTabActivityTitle(getString(R.string.app_name));
      selectTabAtPosition(0);
   }

   /**
    * Check if the configuration includes a pcm
    * @return true if the configuration includes a pcm
    */
   private boolean hasPCM(){
      //pref_key_pcm_standalone
      boolean hasPCM = false;
      try {
         GlobalPreferences globalPreferences = new GlobalPreferences(this);
         hasPCM = globalPreferences.hasPCM();
      } catch (GlobalPreferencesNotAvailableException e){
         if (logger.isWarnEnabled()) {
            logger.warn("global preferences not available - guess that is has pcm? :" + hasPCM, e);
         }
      }
      logger.debug("hasPCM?: {}", hasPCM);
      return hasPCM;
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
      if (shouldInjectOnCreateView(name)) return injectOnCreateView(name, context, attrs);

      return super.onCreateView(name, context, attrs);
   }

   @Override
   public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
      if (shouldInjectOnCreateView(name)) return injectOnCreateView(name, context, attrs);

      return super.onCreateView(parent, name, context, attrs);
   }

   protected static View injectOnCreateView(String name, Context context, AttributeSet attrs) {
      try {
         final Constructor<?> constructor = Class.forName(name).getConstructor(Context.class, AttributeSet.class);
         final View view = (View) constructor.newInstance(context, attrs);
         RoboGuice.getInjector(context).injectMembers(view);
         RoboGuice.getInjector(context).injectViewMembers(view);
         return view;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public void onResume() {
      logger.debug("onResume called");
      super.onResume();
      eventManager.fire(new OnResumeEvent(this));
      logger.debug("Sending INTERNAL_DATA broadcast");
      sendBroadcast(new Intent(ServiceConstants.ACTION_INTERNAL_DATA).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
      getApplicationContext().bindService(new Intent(VIPConstants.VIP_SERVICE_NAME), this, Context.BIND_AUTO_CREATE);
   }

   @Override
   protected void onPause() {
      logger.debug("onPause called");
      super.onPause();
      eventManager.fire(new OnPauseEvent(this));
      getApplicationContext().unbindService(this);
   }

   @Override
   public void onServiceConnected(ComponentName name, IBinder service) {
      logger.debug("onSeviceConnected called - componentClassName: " + name.getClassName() + " service: " + service.toString());
      this.vipService = IVIPServiceAIDL.Stub.asInterface(service);
      if (vipService != null) {
         logger.debug("onSeviceConnected called - vipService != null");
         if(productLibraryFragmentWeakReference != null) {
            ProductLibraryFragment productLibraryFragment = productLibraryFragmentWeakReference.get();
            if (productLibraryFragment != null) {
               logger.debug("onSeviceConnected called - productLibraryFragment != null");
               productLibraryFragment.setVipService(this.vipService);
            }
         }
      }
   }

   @Override
   public void onServiceDisconnected(ComponentName name) {
      logger.debug("onSeviceDisconnected called - componentClassName: " + name.getClassName());
      if (vipService != null) {
         if (productLibraryFragmentWeakReference != null) {
            ProductLibraryFragment productLibraryFragment = productLibraryFragmentWeakReference.get();
            if (productLibraryFragment != null) {
               productLibraryFragment.setVipService(null);
            }
         }
      }
   }
}
