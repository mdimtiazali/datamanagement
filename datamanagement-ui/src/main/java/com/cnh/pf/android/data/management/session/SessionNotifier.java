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
 * An interface to send session notification (success/error/cancel)
 */
public interface SessionNotifier {
   void notifySessionSuccess(Session session);
   void notifySessionError(Session session, ErrorCode code);
   void notifySessionCancel(Session session);
   void notifyProgressUpdate(String operation, int progress, int max);
}