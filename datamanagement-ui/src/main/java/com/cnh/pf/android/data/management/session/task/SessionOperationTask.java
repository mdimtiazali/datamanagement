/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.task;

import android.os.AsyncTask;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.fault.DMFaultHandler;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.android.data.management.session.StatusSender;
import com.cnh.pf.datamng.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Abstract class to define basic session task behavior and handles the session state (IN_PROGRESS,
 * COMPLETE, CANCELLED). This requires callback object to start with and it will report the result
 * of session activity in the end using the callback. Child class needs to define concrete task in
 * the execute() function.
 *
 * @author: junsu.shin@cnhind.com
 */
public abstract class SessionOperationTask<Progress> extends AsyncTask<Session, Progress, Session> {
   private static final Logger logger = LoggerFactory.getLogger(SessionOperationTask.class);
   private final SessionNotifier notifier;
   private final Mediator mediator;
   private ErrorCode sessionErrCode;

   public SessionOperationTask(@Nonnull Mediator mediator, SessionNotifier notifier) {
      this.mediator = mediator;
      this.notifier = notifier;
      this.sessionErrCode = null;
   }

   /**
    * Execute a session. This is used for child class to define concrete task.
    *
    * @param session
    * @throws SessionException
    */
   protected abstract void execute(@Nonnull Session session) throws SessionException;

   /**
    * A function to expose the mediator to child task class.
    *
    * @return  the Mediator object
    */
   protected Mediator getMediator() {
      return mediator;
   }

   /**
    * Execute session task. This actually calls AsyncTask.executeOnExecutor to run the task defined
    * in the abstract method execute().
    *
    * @param session    session data
    */
   public void executeTask(Session session) {
      executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
   }

   @Override
   protected Session doInBackground(Session... params) {
      Session session = params[0];

      try {
         session.setState(Session.State.IN_PROGRESS);
         session.setResultCode(null);
         execute(session);
      }
      catch (SessionException e) {
         sessionErrCode = e.getErrorCode();
         session.setResultCode(Process.Result.ERROR);
      }

      session.setState(Session.State.COMPLETE);
      return session;
   }

   @Override
   protected void onPostExecute(Session session) {
      super.onPostExecute(session);

      logger.debug("onPostExecute: {}", this.getClass().getSimpleName());
      if (notifier == null) {
         logger.debug("SessionNotifier is set to be NULL!");
         return;
      }

      if (SessionUtil.isSuccessful(session)) {
         notifier.notifySessionSuccess(session);
      }
      else if (SessionUtil.isErroneous(session)) {
         notifier.notifySessionError(session, sessionErrCode);
      }
      else if (SessionUtil.isCancelled(session)) {
         notifier.notifySessionCancel(session);
      }
   }

   /**
    * Builder class to create specific session task instance depending on session input.
    */
   public static class TaskBuilder {
      private Mediator _mediator;
      private SessionNotifier _notifier;
      private StatusSender _statusSender;
      private Session _session;
      private DMFaultHandler _faultHandler;
      private FormatManager _formatManager;

      public TaskBuilder mediator(Mediator mediator) {
         this._mediator = mediator;
         return this;
      }

      public TaskBuilder notifier(SessionNotifier notifier) {
         this._notifier = notifier;
         return this;
      }

      public TaskBuilder session(Session session) {
         this._session = session;
         return this;
      }

      public TaskBuilder statusSender(StatusSender statusSender) {
         this._statusSender = statusSender;
         return this;
      }

      public TaskBuilder faultHandler(DMFaultHandler faultHandler) {
         this._faultHandler = faultHandler;
         return this;
      }

      public TaskBuilder formatManager(FormatManager formatManager) {
         this._formatManager = formatManager;
         return this;
      }

      public SessionOperationTask build() throws IllegalStateException {
         SessionOperationTask task = null;

         if (_session == null || _mediator == null || _notifier == null) {
            throw new IllegalStateException("Mediator, Session & SessionNotifier objects are required");
         }
         if (SessionUtil.isDiscoveryTask(_session)) {
            task = new DiscoveryTask(_mediator, _notifier);
         }
         else if (SessionUtil.isPerformOperationsTask(_session)) {
            if (_faultHandler == null || _formatManager == null) {
               throw new IllegalStateException("FaultHandler & FormatManager is required.");
            }
            task = new PerformOperationsTask(_mediator, _notifier, _faultHandler, _formatManager, _statusSender);
         }
         else if (SessionUtil.isCalculateOperationsTask(_session)) {
            task = new CalculateOperationsTask(_mediator, _notifier);
         }
         else if (SessionUtil.isCalculateConflictsTask(_session)) {
            task = new CalculateConflictsTask(_mediator, _notifier);
         }
         else if (SessionUtil.isUpdateTask(_session)) {
            task = new UpdateTask(_mediator, _notifier);
         }
         else if (SessionUtil.isDeleteTask(_session)) {
            task = new DeleteTask(_mediator, _notifier);
         }

         return task;
      }
   }
}