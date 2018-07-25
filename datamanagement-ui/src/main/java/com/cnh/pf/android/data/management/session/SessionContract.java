/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Interfaces for session manager & session view.
 *
 * @author: junsu.shin@cnhind.com
 */
public interface SessionContract {
   interface SessionManager {
      /**
       * Return currently active session pertaining to session action (MANAGE, IMPORT & EXPORT)
       *
       * @param action  session action (MANAGE, IMPORT & EXPORT)
       * @return  currently active session
       */
      @NotNull
      Session getCurrentSession(Session.Action action);

      /**
       * Return whether a active session exists for the given session action (MANAGE, IMPORT & EXPORT)
       *
       * @param action Session.Action its state is to be determined.
       * @return True if action does currently run an action, false otherwise
       */
      boolean actionIsActive(Session.Action action);

      /**
       * Set session view for the session manager to interact with the view
       *
       * @param view session view
       */
      void setView(SessionView view);

      /**
       * Nullify session view in the manager
       */
      void resetView();

      /**
       * Execute DISCOVERY
       *
       * @param extra   session extra
       */
      void discovery(SessionExtra extra);

      /**
       * Execute PERFORM_OPERATIONS
       *
       * @param extra   session extra
       * @param operations list of operation data to be used in the session
       */
      void performOperations(SessionExtra extra, List<Operation> operations);

      /**
       * Execute CALCULATE_CONFLICTS
       *
       * @param operations list of operation data to be used in the session
       */
      void calculateConflicts(List<Operation> operations);

      /**
       * Execute CALCULATE_OPERATIONS
       *
       * @param objectGraphs  list of object graph data to be used in the session
       */
      void calculateOperations(List<ObjectGraph> objectGraphs);

      /**
       * Execute UPDATE
       *
       * @param operations  list of operation data to be used in the session
       */
      void update(List<Operation> operations);

      /**
       * Execute PASTE
       *
       * @param operations  list of operation data to be used in the session
       */
      void paste(List<Operation> operations);

      /**
       * Execute DELETE
       *
       * @param operations  list of operation data to be used in the session
       */
      void delete(List<Operation> operations);

      /**
       * Cancel session
       */
      void cancel();

      /**
       * Return true if DM service is connected
       *
       * @return true if DM service is connected
       */
      boolean isServiceConnected();
   }

   interface SessionView {
      /**
       * Set the session manager for the view to interact with the manager
       *
       * @param sessionManager   session manager
       */
      void setSessionManager(SessionManager sessionManager);

      /**
       * Callback to notify session error
       *
       * @param session the session
       * @param errorCode  error code
       */
      void onMyselfSessionError(Session session, ErrorCode errorCode);

      /**
       * Callback to notify session error, but session doesn't belong to the current action
       *
       * @param session the session
       * @param errorCode  error code
       */
      void onOtherSessionError(Session session, ErrorCode errorCode);

      /**
       * Callback to notify successful session
       *
       * @param session the session
       */
      void onMyselfSessionSuccess(Session session);

      /**
       * Callback to notify successful session, but session doesn't belong to the current action
       *
       * @param session the session
       */
      void onOtherSessionSuccess(Session session);

      /**
       * Callback to notify session cancellation
       *
       * @param session the session
       */
      void onSessionCancelled(Session session);

      /**
       * Callback for PCM disconnection notification
       */
      void onPCMDisconnected();

      /**
       * Callback for PCM connection notification
       */
      void onPCMConnected();

      /**
       * Callback for progress update during PERFORM_OPERATIONS
       *
       * @param operation
       * @param progress
       * @param max
       */
      void onProgressUpdate(String operation, int progress, int max);

      /**
       * Callback to notify updates in medium (USB, CLOUD)
       */
      void onMediumUpdate();

      /**
       * Callback to notify membership change in group channel for datasource communication
       *
       * @param updateNeeded  true if view update is needed
       */
      void onChannelConnectionChange(boolean updateNeeded);

      /**
       * Call back to notify a change in DM service connection
       * @param connected
       */
      void onDataServiceConnectionChange(boolean connected);

      /**
       * Return action that the view represents (MANAGE, IMPORT & EXPORT)
       *
       * @return  the action
       */
      Session.Action getAction();
   }
}
