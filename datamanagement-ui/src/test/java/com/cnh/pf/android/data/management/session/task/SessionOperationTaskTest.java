package com.cnh.pf.android.data.management.session.task;

import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.fault.DMFaultHandler;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.StatusSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test session task builder.
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

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      session = new Session();
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
}
