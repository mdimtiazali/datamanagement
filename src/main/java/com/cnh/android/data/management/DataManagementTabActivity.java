/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.android.data.management;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.TabActivityListeners;
import com.cnh.android.widget.control.TabActivityTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Management Tab Activity
 * Contains test UI, will later include Data Management, Data Sync, Import & Export
 */
public class DataManagementTabActivity extends TabActivity {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementTabActivity.class);

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
      super.onCreate(savedInstanceState);
      TabActivityTab importExportTestTab = new TabActivityTab(R.string.tab_import, android.R.drawable.ic_dialog_alert, "import_export_test_tab",
              new DataManagementTabListener(new ImportFragment(), this));
      addTab(importExportTestTab);
      setTabActivityTitle(getString(R.string.app_name));
      selectTabAtPosition(0);
   }
}
