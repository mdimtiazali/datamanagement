/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import android.content.ComponentName;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import com.cnh.pf.android.data.management.service.DataManagementService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test SessionManager class to make sure the manager interacts with the back-end service properly.
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class SessionManagerTest {
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;
   @Mock
   SessionContract.SessionView mockView;
   SessionManager sessionManager;

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
      sessionManager = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(SessionManager.class);
      when(binder.getService()).thenReturn(service);
      String pkgName = RuntimeEnvironment.application.getPackageName();
      shadowOf(RuntimeEnvironment.application)
              .setComponentNameAndServiceForBindService(new ComponentName(pkgName, DataManagementService.class.getName()), binder);

      sessionManager.setView(mockView);
      sessionManager.connect();
   }

   @Test
   public void testServiceConnection() {
      // Check for listener call
      verify(mockView).onDataServiceConnectionChange(true);
   }

   @Test
   public void testDiscoveryCall() {
      ArgumentCaptor<Session> argumentCaptor = ArgumentCaptor.forClass(Session.class);

      when(mockView.getAction()).thenReturn(Session.Action.MANAGE);
      sessionManager.discovery(null);

      // Make sure the backend calls gets executed with proper argument
      verify(service).processSession(argumentCaptor.capture());
      Session capturedSession = argumentCaptor.getValue();
      assertTrue(SessionUtil.isDiscoveryTask(capturedSession));
      assertTrue(SessionUtil.isManageAction(capturedSession));
   }

   @Test
   public void testPerformOperationsCall() {
      ArgumentCaptor<Session> argumentCaptor = ArgumentCaptor.forClass(Session.class);

      when(mockView.getAction()).thenReturn(Session.Action.EXPORT);
      SessionExtra extra = new SessionExtra(SessionExtra.USB, "dummy", 0);
      sessionManager.performOperations(extra, new ArrayList<Operation>());

      // Make sure the backend call gets executed with proper argument
      verify(service).processSession(argumentCaptor.capture());
      Session capturedSession = argumentCaptor.getValue();
      assertTrue(SessionUtil.isPerformOperationsTask(capturedSession));
      assertTrue(SessionUtil.isExportAction(capturedSession));
   }

   @Test
   public void testCalculateOperationsCall() {
      ArgumentCaptor<Session> argumentCaptor = ArgumentCaptor.forClass(Session.class);

      when(mockView.getAction()).thenReturn(Session.Action.IMPORT);
      sessionManager.calculateOperations(new ArrayList<ObjectGraph>());

      // Make sure the backend call gets executed with proper argument
      verify(service).processSession(argumentCaptor.capture());
      Session capturedSession = argumentCaptor.getValue();
      assertTrue(SessionUtil.isCalculateOperationsTask(capturedSession));
      assertTrue(SessionUtil.isImportAction(capturedSession));
   }

   @Test
   public void testCalculateConflictsCall() {
      ArgumentCaptor<Session> argumentCaptor = ArgumentCaptor.forClass(Session.class);

      when(mockView.getAction()).thenReturn(Session.Action.IMPORT);
      sessionManager.calculateConflicts(new ArrayList<Operation>());

      // Make sure the backend call gets executed with proper argument
      verify(service).processSession(argumentCaptor.capture());
      Session capturedSession = argumentCaptor.getValue();
      assertTrue(SessionUtil.isCalculateConflictsTask(capturedSession));
      assertTrue(SessionUtil.isImportAction(capturedSession));
   }

   @Test
   public void testUpdateCall() {
      ArgumentCaptor<Session> argumentCaptor = ArgumentCaptor.forClass(Session.class);

      when(mockView.getAction()).thenReturn(Session.Action.MANAGE);
      sessionManager.update(new ArrayList<Operation>());

      // Make sure the backend call gets executed with proper argument
      verify(service).processSession(argumentCaptor.capture());
      Session capturedSession = argumentCaptor.getValue();
      assertTrue(SessionUtil.isUpdateTask(capturedSession));
      assertTrue(SessionUtil.isManageAction(capturedSession));
   }

   @Test
   public void testDeleteCall() {
      ArgumentCaptor<Session> argumentCaptor = ArgumentCaptor.forClass(Session.class);

      when(mockView.getAction()).thenReturn(Session.Action.MANAGE);
      sessionManager.delete(new ArrayList<Operation>());

      // Make sure the backend call gets executed with proper argument
      verify(service).processSession(argumentCaptor.capture());
      Session capturedSession = argumentCaptor.getValue();
      assertTrue(SessionUtil.isDeleteTask(capturedSession));
      assertTrue(SessionUtil.isManageAction(capturedSession));
   }

   @After
   public void tearDown() {
      sessionManager.disconnect();
   }
}
