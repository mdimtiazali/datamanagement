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
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.android.util.prefs.GlobalPreferencesNotAvailableException;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.SessionManager;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test SessionView interface managed by the session manager.
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class SessionViewTest {
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
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new SessionViewTest.MyTestModule());
      controller = Robolectric.buildActivity(DataManagementActivity.class);
      activity = controller.get();
      when(binder.getService()).thenReturn(service);
      shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()),
              binder);
      //Start activity
      controller.create().start().resume();
      sessionManager = activity.sessionManager;

      ManageFragment manage = new ManageFragment();
   }

   @Test
   public void testServiceConnected() {
      assertTrue(sessionManager.isServiceConnected());
   }

   @Test
   public void testManageView() {
      activateTab(MANAGE_SOURCE_TAB_POSITION);
      ManageFragment view = (ManageFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Management");
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

   public class MyTestModule extends AbstractModule {

      @Override
      protected void configure() {
         bind(Mediator.class).toInstance(mock(Mediator.class));
      }

      @Provides
      @Singleton
      @Named("global")
      @SuppressWarnings("deprecation")
      private SharedPreferences getPrefs() throws PackageManager.NameNotFoundException {
         return null;
      }

      @Provides
      @Singleton
      public GlobalPreferences getGlobalPreferences(Context context) throws GlobalPreferencesNotAvailableException {
         GlobalPreferences prefs = mock(GlobalPreferences.class);
         when(prefs.hasPCM()).thenReturn(true);
         return prefs;
      }

      @Provides
      @Singleton
      @Named("daemon")
      public HostAndPort getDaemon() {
         return HostAndPort.fromParts("127.0.0.1", 14000);
      }
   }
}
