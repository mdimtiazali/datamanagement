/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.SessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import roboguice.RoboGuice;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test SessionView interface managed by the session manager.
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class FragmentViewTest {
   private static int MANAGE_SOURCE_TAB_POSITION = 0;
   private static int IMPORT_SOURCE_TAB_POSITION = 1;
   private static int EXPORT_SOURCE_TAB_POSITION = 2;

   ActivityController<DataManagementActivity> controller;
   DataManagementActivity activity;
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;
   SessionManager sessionManager;

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
      controller = Robolectric.buildActivity(DataManagementActivity.class);
      activity = controller.get();
      when(binder.getService()).thenReturn(service);
      shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()),
              binder);
      //Start activity
      controller.create().start().resume();
      sessionManager = activity.sessionManager;
   }

   @Test
   public void testServiceConnected() {
      assertTrue(sessionManager.isServiceConnected());
   }


   @Test
   public void testManageView() {
      activateTab(MANAGE_SOURCE_TAB_POSITION);
      ManageFragment view = (ManageFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Data Management");
      assertTrue("SessionView is not null", sessionManager.getView() != null);
      assertTrue("SessionView is MANAGE view", sessionManager.getView() == view);
   }

   @Test
   public void testImportView() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment view = (ImportFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Import");
      assertTrue("SessionView is not null", sessionManager.getView() != null);
      assertTrue("SessionView is IMPORT view", sessionManager.getView() == view);
   }

   @Test
   public void testExportView() {
      activateTab(EXPORT_SOURCE_TAB_POSITION);
      ExportFragment view = (ExportFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Export");
      assertTrue("SessionView is not null", sessionManager.getView() != null);
      assertTrue("SessionView is EXPORT view", sessionManager.getView() == view);
   }

   private void activateTab(int tabPosition) {
      //Select Tab at position
      ((TabActivity) activity).selectTabAtPosition(tabPosition);
   }
}
