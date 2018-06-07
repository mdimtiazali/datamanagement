/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.task;

import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.fault.DMFaultHandler;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.StatusSender;
import org.jgroups.Address;
import org.jgroups.util.ExtendedUUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test session task builder and session tasks.
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class SessionOperationTaskTest {
   @Mock
   private Mediator mediator;
   @Mock
   private SessionNotifier notifier;
   @Mock
   private StatusSender statusSender;
   @Mock
   private DMFaultHandler faultHandler;
   @Mock
   private FormatManager formatManager;

   private Session session;
   private List<Address> sources = new ArrayList<Address>();
   private List<Address> destinations = new ArrayList<Address>();

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      session = new Session();
      sources.add(new ExtendedUUID());
      destinations.add(new ExtendedUUID());

      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();
   }

   @After
   public void tearDown() throws InterruptedException {
      // This is a workaround for Roboelectric issue throwing Exception while running ShadowAsyncTask
      Thread.sleep(100);
   }

   private void testBuild(final Session session, Class taskClass) {
      SessionOperationTask task = new SessionOperationTask.TaskBuilder()
              .session(session)
              .notifier(notifier)
              .mediator(mediator)
              .build();
      assertNotNull(task);
      assertTrue(task.getClass().equals(taskClass));
   }

   @Test
   public void testSessionTaskBuild() {
      session.setType(Session.Type.DISCOVERY);
      testBuild(session, DiscoveryTask.class);

      session.setType(Session.Type.CALCULATE_OPERATIONS);
      testBuild(session, CalculateOperationsTask.class);

      session.setType(Session.Type.CALCULATE_CONFLICTS);
      testBuild(session, CalculateConflictsTask.class);

      session.setType(Session.Type.UPDATE);
      testBuild(session, UpdateTask.class);

      session.setType(Session.Type.DELETE);
      testBuild(session, DeleteTask.class);
   }

   // Separate test for PERFORM_OPERATIONS since it requires more parameters to create.
   @Test
   public void testPerformOpeationsSessionTaskBuild() {
      session.setType(Session.Type.PERFORM_OPERATIONS);
      SessionOperationTask performOperationsTask = new SessionOperationTask.TaskBuilder()
              .session(session)
              .notifier(notifier)
              .mediator(mediator)
              .faultHandler(faultHandler)
              .formatManager(formatManager)
              .statusSender(statusSender)
              .build();
      assertNotNull(performOperationsTask);
      assertTrue(performOperationsTask instanceof PerformOperationsTask);
   }

   private void testBuildFail(final Session session) {
      try {
         SessionOperationTask task = new SessionOperationTask.TaskBuilder()
                 .session(session)
                 .notifier(notifier)
                 .build();
         fail("IllegalStateException should be thrown.");
      }
      catch (IllegalStateException e) {
         // Signifies a successful test execution
         assertTrue(true);
      }
   }

   @Test
   public void testSessionTaskBuildFail() {
      session.setType(Session.Type.DISCOVERY);
      testBuildFail(session);

      session.setType(Session.Type.PERFORM_OPERATIONS);
      testBuildFail(session);

      session.setType(Session.Type.CALCULATE_CONFLICTS);
      testBuildFail(session);

      session.setType(Session.Type.CALCULATE_OPERATIONS);
      testBuildFail(session);

      session.setType(Session.Type.UPDATE);
      testBuildFail(session);

      session.setType(Session.Type.DELETE);
      testBuildFail(session);
   }

   @Test
   public void testDiscoveryTask() throws Throwable {
      Robolectric.getBackgroundThreadScheduler().unPause();
      session.setType(Session.Type.DISCOVERY);
      session.setSources(sources);
      DiscoveryTask task = new DiscoveryTask(mediator, notifier);

      // Used AsyncTask.execute() instead of SessionOperationTask.executeTask() due to a
      // problem with ShadowAyncTask in Robolectric. It'll achieve same except it doesn't
      // use THREAD_POOL_EXECUTOR but it's still sufficient to test the code.
      task.execute(session);
      ShadowApplication.runBackgroundTasks();

      // Make sure the mediator responds correctly in the task
      verify(mediator).discovery(any(Address.class));
   }

   @Test
   public void testPerformOperationsTask() throws Throwable {
      session.setType(Session.Type.PERFORM_OPERATIONS);
      session.setAction(Session.Action.EXPORT);
      session.setDestinations(destinations);
      PerformOperationsTask task = new PerformOperationsTask(mediator, notifier, faultHandler, formatManager, statusSender);

      task.execute(session);
      ShadowApplication.runBackgroundTasks();

      // Make sure the mediator responds correctly in the task
      verify(mediator).performOperations(anyListOf(Operation.class), any(Address.class));
   }

   @Test
   public void testCalculateOperationsTask() throws Throwable {
      session.setType(Session.Type.CALCULATE_OPERATIONS);
      session.setAction(Session.Action.IMPORT);
      session.setDestinations(destinations);
      CalculateOperationsTask task = new CalculateOperationsTask(mediator, notifier);

      task.execute(session);
      ShadowApplication.runBackgroundTasks();

      // Make sure the mediator responds correctly in the task
      verify(mediator).calculateOperations(anyListOf(ObjectGraph.class), any(Address.class));
   }

   @Test
   public void testCalculateConflictsTask() throws Throwable {
      session.setType(Session.Type.CALCULATE_CONFLICTS);
      session.setAction(Session.Action.IMPORT);
      session.setDestinations(destinations);
      CalculateConflictsTask task = new CalculateConflictsTask(mediator, notifier);

      task.execute(session);
      ShadowApplication.runBackgroundTasks();

      // Make sure the mediator responds correctly in the task
      verify(mediator).calculateConflicts(anyListOf(Operation.class), any(Address.class));
   }

   @Test
   public void testUpdateTask() throws Throwable {
      session.setType(Session.Type.UPDATE);
      session.setAction(Session.Action.MANAGE);
      UpdateTask task = new UpdateTask(mediator, notifier);

      task.execute(session);
      ShadowApplication.runBackgroundTasks();

      // Make sure the mediator responds correctly in the task
      verify(mediator).updateOperations(anyListOf(Operation.class), any(Address.class));
   }

   @Test
   public void testDeleteTask() throws Throwable {
      session.setType(Session.Type.DELETE);
      session.setAction(Session.Action.MANAGE);
      DeleteTask task = new DeleteTask(mediator, notifier);

      task.execute(session);
      ShadowApplication.runBackgroundTasks();

      // Make sure the mediator responds correctly in the task
      verify(mediator).deleteOperations(anyListOf(Operation.class), any(Address.class));
   }

   @Test
   public void testCancelTask() throws Throwable {
      session.setAction(Session.Action.MANAGE);
      session.setSources(sources);
      session.setDestinations(destinations);
      session.setState(Session.State.IN_PROGRESS);
      CancelTask task = new CancelTask(mediator);

      task.execute(session);
      ShadowApplication.runBackgroundTasks();

      // Make sure the mediator responds correctly in the task.
      // cancel() gets called twice for source and destination.
      verify(mediator, times(2)).cancel(any(Address.class));
   }
}
