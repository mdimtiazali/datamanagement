/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.task;

import android.os.Handler;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.android.data.management.session.StatusSender;
import com.cnh.pf.datamng.Process;
import org.jgroups.Address;
import org.jgroups.util.Rsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Session task class to execute PERFORM_OPERATIONS session
 *
 * @author: junsu.shin@cnhind.com
 */
public class PerformOperationsTask extends SessionOperationTask<Void> {
   private static final Logger logger = LoggerFactory.getLogger(PerformOperationsTask.class);
   public static final int KILL_STATUS_DELAY = 5000;
   private StatusSender statusSender;

   public PerformOperationsTask(@Nonnull Mediator mediator, @Nonnull SessionNotifier notifier, StatusSender statusSender) {
      super(mediator, notifier);
      this.statusSender = statusSender;
   }

   @Override
   protected void execute(@Nonnull Session session) throws SessionException {
      logger.debug("{}:execute()", this.getClass().getSimpleName());
      try {
         if (statusSender != null) {
            statusSender.sendStartingStatus(SessionUtil.isExportAction(session));
         }
         Address[] addresses = session.getDestinations().toArray(new Address[0]);
         if (addresses != null && addresses.length > 0) {
            logger.debug("PerformOperations-Dst Addresses: {}", SessionUtil.addressToString(addresses));

            session.setResults(getMediator().performOperations(session.getOperations(), addresses));
            boolean hasCancelled = Process.Result.CANCEL.equals(session.getResultCode());

            for (Rsp<Process> ret : session.getResults()) {
               if (ret.hasException()) throw ret.getException();
               if (ret.wasReceived() && ret.getValue() != null && ret.getValue().getResult() != null) {
                  hasCancelled |= Process.Result.CANCEL.equals(ret.getValue().getResult());
               }
               else {
                  session.setResultCode(Process.Result.ERROR);
                  throw new SessionException(ErrorCode.PERFORM_ERROR);
               }
            }

            session.setResultCode(hasCancelled ? Process.Result.CANCEL : Process.Result.SUCCESS);
         }
         else {
            session.setResultCode(Process.Result.ERROR);
            throw new SessionException(ErrorCode.NO_DESTINATION_DATASOURCE);
         }
      }
      catch (Throwable e) {
         logger.error("Exception in PERFORM_OPERATIONS: ", e);
         session.setResultCode(Process.Result.ERROR);
         throw new SessionException(ErrorCode.PERFORM_ERROR);
      }
   }

   @Override
   protected void onPostExecute(Session session) {
      if (statusSender != null) {
         boolean exporting = SessionUtil.isExportAction(session);

         if (SessionUtil.isErroneous(session)) {
            statusSender.sendStatus("Error", exporting);
         } else if (SessionUtil.isCancelled(session)) {
            statusSender.sendCancelledStatus(exporting);
         } else if (SessionUtil.isSuccessful(session)) {
            statusSender.sendSuccessfulStatus(exporting);
         }

         new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
               statusSender.removeStatus();
            }
         }, KILL_STATUS_DELAY);
      }

      // Call super.onPostExecute() after finishing status notification.
      // Session data get resets in the super.onPostExecute() call.
      super.onPostExecute(session);
   }
}