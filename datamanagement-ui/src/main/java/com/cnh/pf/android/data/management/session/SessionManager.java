/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Session manager manages session data for the three fragments (actions), connection to the data management
 * service. The manager act as an intermediate object between DM service and session view. It provides interface
 * to the DM service and directs session events to registered session view.
 */
@Singleton
public class SessionManager implements SessionContract.SessionManager, SessionEventListener, ServiceConnection {
   private final Logger logger = LoggerFactory.getLogger(SessionManager.class);
   private Map<Session.Action, Session> sessionMap = new EnumMap<Session.Action, Session>(Session.Action.class);
   private SessionContract.SessionView view;
   private DataManagementService dmService;
   private Application context;

   @Override
   public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
      logger.debug("DM service is connected.");
      DataManagementService.LocalBinder binder = (DataManagementService.LocalBinder) iBinder;
      dmService = binder.getService();
      dmService.registerSessionEventListener(this);
      if (view != null) {
         view.onDataServiceConnectionChange(true);
      }
   }

   @Override
   public void onServiceDisconnected(ComponentName componentName) {
      logger.debug("DM service is disconnected.");
      dmService.unregisterSessionEventListener(this);
      if (view != null) {
         view.onDataServiceConnectionChange(false);
      }
      dmService = null;
   }

   @Inject
   public SessionManager(Application context) {
      this.context = context;
   }

   /**
    * Connect to the DM service
    */
   public void connect() {
      logger.debug("Connect to the data-management service.");
      context.bindService(new Intent(context, DataManagementService.class), this, Context.BIND_AUTO_CREATE);
   }

   /**
    * Disconnect from the DM service
    */
   public void disconnect() {
      logger.debug("Disconnect the data-management service.");
      dmService.unregisterSessionEventListener(SessionManager.this);
      context.unbindService(this);
      // Notify the service disconnection. onServiceDisconnected doesn't occur for normal disconnection.
      // At least the intention here is to disconnect the service.
      if (view != null) {
         view.onDataServiceConnectionChange(false);
      }

      // Clear stored session data for IMPORT action before exiting the application
      if (sessionMap.containsKey(Session.Action.IMPORT)) {
         sessionMap.remove(Session.Action.IMPORT);
      }
   }

   @Override
   public Session getCurrentSession(Session.Action action) {
      Session retSession;
      if (sessionMap.containsKey(action)) {
         retSession = sessionMap.get(action);
      }
      else {
         Session newSession = new Session();
         newSession.setAction(action);
         sessionMap.put(action, newSession);
         retSession = newSession;
      }

      return retSession;
   }

   @Override
   public boolean actionIsActive(Session.Action action) {
      if (action != Session.Action.UNKNOWN && sessionMap.containsKey(action) && sessionMap.get(action) != null) {
         //TODO: determine, if WAIT is different on initial loading and waiting for user input / conflict solving.
         return sessionMap.get(action).getState() != Session.State.COMPLETE && sessionMap.get(action).getState() != Session.State.WAIT;
      }
      else {
         return false;
      }
   }

   /**
    * Return the current active session view
    *
    * @return  the session view
    */
   public SessionContract.SessionView getView() {
      return this.view;
   }

   @Override
   public void setView(SessionContract.SessionView view) {
      if (isServiceConnected()) {
         dmService.setActiveView(view);
      }
      this.view = view;
   }

   @Override
   public void resetView() {
      if (isServiceConnected()) {
         dmService.setActiveView(null);
      }
      view = null;
   }

   @Override
   public boolean isServiceConnected() {
      return dmService != null;
   }

   private void executeSession(Session session) {
      if (isServiceConnected() && !SessionUtil.isInProgress(session)) {
         dmService.processSession(session);
      }
      else {
         logger.debug("executeSession(), {}: The session is still in progress. Just return", session.getType());
      }
   }

   /**
    * Request DM service to execute DISCOVERY task. Session extra is required for Import process.
    *
    * @param extra      session extra
    */
   @Override
   public void discovery(SessionExtra extra) {
      if (view != null) {
         Session.Action action = view.getAction();
         logger.trace("disovery(): {}", action);

         Session session = getCurrentSession(action);
         session.setType(Session.Type.DISCOVERY);
         session.setResultCode(null);
         session.setExtra(extra);
         executeSession(session);
      }
      else {
         logger.debug("discovery(): Session view is null");
      }
   }

   /**
    * Request DM service to execute PERFORM_OPERATIONS task.
    *
    * @param extra      session extra
    * @param operations a list of operation to be used in PERFORM_OPERATIONS
    */
   @Override
   public void performOperations(SessionExtra extra, List<Operation> operations) {
      if (view != null) {
         Session.Action action = view.getAction();
         logger.trace("performOperations(): {}", action);

         Session session = getCurrentSession(action);
         session.setType(Session.Type.PERFORM_OPERATIONS);
         session.setExtra(extra);
         session.setResultCode(null);
         session.setOperations(operations);
         executeSession(session);
      }
      else {
         logger.debug("performOperations(): Session view is null");
      }
   }

   /**
    * Request DM service to execute CALCULATE_CONFLICTS task.
    *
    * @param operations a list of operation to be used in CALCULATE_CONFLICTS
    */
   @Override
   public void calculateConflicts(List<Operation> operations) {
      if (view != null) {
         Session.Action action = view.getAction();
         logger.trace("calculateConflicts(): {}", action);

         Session session = getCurrentSession(action);
         session.setType(Session.Type.CALCULATE_CONFLICTS);
         session.setResultCode(null);
         session.setOperations(operations);
         executeSession(session);
      }
      else {
         logger.debug("calculateConflicts(): Session view is null");
      }
   }

   /**
    * Request DM service to execute CALCULATE_OPERATIONS task.
    * NOTE: New session instance gets created to execute a session to maintain the original
    *       ObjectGraph and Operation. The session result will get updated with updateSession()
    *       later after getting notified by the service.
    *
    * @param objectGraphs  a list of object graph data to be used in CALCULATE_OPERATIONS
    */
   @Override
   public void calculateOperations(List<ObjectGraph> objectGraphs) {
      if (view != null) {
         Session.Action action = view.getAction();
         logger.trace("calculateOperations(): {}", action);

         getCurrentSession(action).setType(Session.Type.CALCULATE_OPERATIONS);
         getCurrentSession(action).setResultCode(null);

         Session session = new Session(getCurrentSession(action));
         session.setObjectData(objectGraphs);
         executeSession(session);
      }
      else {
         logger.debug("calculateOperations(): Session view is null");
      }
   }

   /**
    * Request DM service to execute UPDATE task.
    *
    * @param operations a list of operation to be used in UPDATE
    */
   @Override
   public void update(List<Operation> operations) {
      if (view != null) {
         Session.Action action = view.getAction();
         logger.trace("update(): {}", action);

         Session session = getCurrentSession(action);

         session.setType(Session.Type.UPDATE);
         session.setResultCode(null);
         session.setObjectData(new ArrayList<ObjectGraph>());
         session.setOperations(operations);
         executeSession(session);
      }
      else {
         logger.debug("update(): Session view is null");
      }
   }

   /**
    * Request DM service to execute DELETE task.
    *
    * @param operations a list of operation to be used in DELETE
    */
   @Override
   public void delete(List<Operation> operations) {
      if (view != null) {
         Session.Action action = view.getAction();
         logger.trace("delete(): {}", action);

         Session session = getCurrentSession(action);

         session.setType(Session.Type.DELETE);
         session.setResultCode(null);
         session.setObjectData(new ArrayList<ObjectGraph>());
         session.setOperations(operations);
         executeSession(session);
      }
      else {
         logger.debug("delete(): Session view is null");
      }
   }

   /**
    * Request DM service to cancel current session
    */
   @Override
   public void cancel() {
      if (view != null) {
         if (isServiceConnected()) {
            Session.Action action = view.getAction();
            dmService.cancelSession(getCurrentSession(action));
         }
         else {
            logger.error("cancel(): DM serivice is null");
         }
      }
      else {
         logger.debug("cancel(): Sesson view is null");
      }
   }

   @Override
   public void onSessionSuccess(Session session) {
      logger.trace("onSessionSuccess(): {}, {}", session.getType(), session.getAction());

      updateSession(session);
      if (view != null) {
         if (isServiceConnected() && dmService.isPCMOnline()) {
            if (isCurrentSession(session)) {
               view.onMyselfSessionSuccess(session);
            }
            else {
               view.onOtherSessionSuccess(session);
            }
         }
         else {
            view.onPCMDisconnected();
         }
      }
      else {
         logger.debug("onSessionSuccess(): Session view or DM service is null");
      }
   }

   @Override
   public void onSessionError(Session session, ErrorCode errorCode) {
      logger.trace("onSessionError(): {}, {}", session.getType(), session.getAction());

      updateSession(session);
      if (view != null) {
         if (isServiceConnected() && dmService.isPCMOnline()) {
            if (isCurrentSession(session)) {
               view.onMyselfSessionError(session, errorCode);
            }
            else {
               view.onOtherSessionError(session, errorCode);
            }
         }
         else {
            view.onPCMDisconnected();
         }
      }
      else {
         logger.debug("onSessionError(): Session view is null or DM service is null");
      }
   }

   @Override
   public void onSessionCancelled(Session session) {
      logger.trace("onSessionCancelled(): {}, {}", session.getType(), session.getAction());

      updateSession(session);
      if (view != null) {
         view.onSessionCancelled(session);
      }
      else {
         logger.debug("onSessionCancelled(): Session view is null");
      }
   }

   @Override
   public void onProgressUpdate(String operation, int progress, int max) {
      if (view != null) {
         view.onProgressUpdate(operation, progress, max);
      }
      else {
         logger.debug("onProgressUpdate(): Session view is null");
      }
   }

   @Override
   public void onMediumUpdate() {
      logger.debug("{}:onMediumUpdate()", this.getClass().getSimpleName());
      // Need to hijack before calling callback. Reset cached data for IMPORT action when
      // different fragment is focused
      if ((view == null || !Session.Action.IMPORT.equals(view.getAction())) && sessionMap.containsKey(Session.Action.IMPORT)) {
         sessionMap.remove(Session.Action.IMPORT);
      }

      if (view != null) {
         view.onMediumUpdate();
      }
      else {
         logger.debug("onMediumUpdate(): Session view is null");
      }
   }

   @Override
   public void onChannelConnectionChange(boolean updateNeeded) {
      if (view != null) {
         view.onChannelConnectionChange(updateNeeded);
      }
      else {
         logger.debug("onChannelConnectionChange(): Session view is null");
      }
   }

   /**
    * Return true if the input session is the same instance as the current active session.
    *
    * @param session    the input session
    * @return  True if the input session is the same instance as the current active session.
    */
   public boolean isCurrentSession(Session session) {
      if (view != null) {
         logger.trace("isCurrentOperation( {} == {})", session.getUuid(), getCurrentSession(view.getAction()).getUuid());
         return session.equals(getCurrentSession(view.getAction()));
      }
      return false;
   }

   /**
    * Update stored session with object data, state & result code.
    *
    * @param session
    */
   private void updateSession(Session session) {
      if (sessionMap.containsKey(session.getAction()) && SessionUtil.isCalculateOperationsTask(session)) {
         Session storedSession = sessionMap.get(session.getAction());

         storedSession.setResultCode(session.getResultCode());
         storedSession.setState(session.getState());
      }
   }
}
