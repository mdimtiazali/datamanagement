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
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;

import com.cnh.pf.android.data.management.helper.DataExchangeBlockedOverlay;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import roboguice.RoboGuice;

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
      ManageFragment view = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");
      assertTrue("SessionView is not null", sessionManager.getView() != null);
      assertTrue("SessionView is MANAGE view", sessionManager.getView() == view);
   }

   @Test
   public void testImportView() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment view = (ImportFragment) activity.getFragmentManager().findFragmentByTag("Import");
      assertTrue("SessionView is not null", sessionManager.getView() != null);
      assertTrue("SessionView is IMPORT view", sessionManager.getView() == view);
   }

   @Test
   public void testExportView() {
      activateTab(EXPORT_SOURCE_TAB_POSITION);
      ExportFragment view = (ExportFragment) activity.getFragmentManager().findFragmentByTag("Export");
      assertTrue("SessionView is not null", sessionManager.getView() != null);
      assertTrue("SessionView is EXPORT view", sessionManager.getView() == view);
   }

   /****************************
    * Testing Blocked Overlays *
    ****************************/
   private SessionManager setSessionProcessActive(Session.Action action) {
      final SessionManager modifiedSessionManager = Mockito.spy(sessionManager);
      when(modifiedSessionManager.actionIsActive(action)).thenReturn(true);
      final Session modifiedSession = Mockito.spy(sessionManager.getCurrentSession(action));
      when(modifiedSession.getType()).thenReturn(Session.Type.PERFORM_OPERATIONS);
      when(modifiedSession.getResultCode()).thenReturn(com.cnh.pf.datamng.Process.Result.SUCCESS);
      return modifiedSessionManager;
   }

   private BaseDataFragment prepareFragmentForBlockedOverlay(Session.Action action, int tab, String fragmentName) {
      SessionManager modifiedSessionManager = setSessionProcessActive(action);
      activateTab(tab);
      BaseDataFragment view = (BaseDataFragment) activity.getFragmentManager().findFragmentByTag(fragmentName);
      view.setSessionManager(modifiedSessionManager);
      view.requestAndUpdateBlockedOverlay(view.getBlockingActions());
      return view;
   }

   @Test
   public void testBlockedOverlayInExportByImport() {
      BaseDataFragment view = prepareFragmentForBlockedOverlay(Session.Action.IMPORT, EXPORT_SOURCE_TAB_POSITION, "Export");
      assertTrue("Export is blocked by Import", DataExchangeBlockedOverlay.MODE.BLOCKED_BY_IMPORT.equals(view.disabledOverlay.getMode()));
   }

   @Test
   public void testBlockedOverlayInManagementByImport() {
      BaseDataFragment view = prepareFragmentForBlockedOverlay(Session.Action.IMPORT, MANAGE_SOURCE_TAB_POSITION, "Data Management");
      assertTrue("Management is blocked by Import", DataExchangeBlockedOverlay.MODE.BLOCKED_BY_IMPORT.equals(view.disabledOverlay.getMode()));
   }

   @Test
   public void testBlockedOverlayInImportByExport() {
      BaseDataFragment view = prepareFragmentForBlockedOverlay(Session.Action.EXPORT, IMPORT_SOURCE_TAB_POSITION, "Import");
      assertTrue("Import is blocked by Export", DataExchangeBlockedOverlay.MODE.BLOCKED_BY_EXPORT.equals(view.disabledOverlay.getMode()));
   }

   @Test
   public void testBlockedOverlayInManagementByExport() {
      BaseDataFragment view = prepareFragmentForBlockedOverlay(Session.Action.EXPORT, MANAGE_SOURCE_TAB_POSITION, "Data Management");
      assertTrue("Management is blocked by Export", DataExchangeBlockedOverlay.MODE.BLOCKED_BY_EXPORT.equals(view.disabledOverlay.getMode()));
   }

   /***************************
    * General Utility Methods *
    ***************************/
   private void activateTab(int tabPosition) {
      //Select Tab at position
      activity.selectTabAtPosition(tabPosition);
   }
}
