/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

/**
 * An interface to provide listener for session events. The DM service uses this interface to notify
 * session related events.
 *
 * @author: junsu.shin@cnhind.com
 */
public interface SessionEventListener {
   /**
    * Callback to notify successful session
    *
    * @param session the session
    */
   void onSessionSuccess(Session session);

   /**
    * Callback to notify session error
    *
    * @param session the session
    * @param errorCode  error code
    */
   void onSessionError(Session session, ErrorCode errorCode);

   /**
    * Callback to notify session cancellation
    *
    * @param session the session
    */
   void onSessionCancelled(Session session);

   /**
    * Callback for progress update during PERFORM_OPERATIONS
    *
    * @param operation
    * @param progress
    * @param max
    */
   void onProgressUpdate(String operation, int progress, int max);

   /**
    * Callback to notify membership change in group channel for datasource communication
    *
    * @param updateNeeded  true if view update is needed
    */
   void onChannelConnectionChange(boolean updateNeeded);

   /**
    * Callback to notify updates in medium (USB, CLOUD)
    */
   void onMediumUpdate();
}
