/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;

import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.pf.android.data.management.dialog.ImportSourceDialog;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.SessionExtra;
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
import roboguice.event.EventManager;

/**
 * Tests for the import fragment
 *
 * @author krueger
 * @since 24.08.2018
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class ImportFragmentTest {
   private static int IMPORT_SOURCE_TAB_POSITION = 1;

   ActivityController<DataManagementActivity> controller;
   DataManagementActivity activity;
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;
   SessionManager sessionManager;

   EventManager eventManager;

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
   public void testLoadingImportOverlay() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment view = (ImportFragment) activity.getFragmentManager().findFragmentByTag("Import");
      eventManager = RoboGuice.getInjector(activity).getInstance(EventManager.class);
      SessionExtra sessionExtra = new SessionExtra(SessionExtra.USB, "noname", SessionExtra.DISPLAY);
      ImportSourceDialog.ImportSourceSelectedEvent event = new ImportSourceDialog.ImportSourceSelectedEvent(sessionExtra);
      eventManager.fire(event);
      assertTrue("Loading Overlay is active while loading datatree", DisabledOverlay.MODE.LOADING.equals(view.treeLoadingOverlay.getMode()));
   }

   /***************************
    * General Utility Methods *
    ***************************/
   private void activateTab(int tabPosition) {
      //Select Tab at position
      activity.selectTabAtPosition(tabPosition);
   }
}
