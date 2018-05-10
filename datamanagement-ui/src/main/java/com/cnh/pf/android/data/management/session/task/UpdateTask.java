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
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.datamng.Process;
import org.jgroups.Address;
import org.jgroups.util.Rsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Session task class to execute UPDATE session
 *
 * @author: junsu.shin@cnhind.com
 */
public class UpdateTask extends SessionOperationTask {
   private static final Logger logger = LoggerFactory.getLogger(UpdateTask.class);

   public UpdateTask(@Nonnull Mediator mediator, @Nonnull SessionNotifier notifier) {
      super(mediator, notifier);
   }

   @Override
   protected void execute(@Nonnull Session session) throws SessionException {
      logger.debug("{}:execute()", this.getClass().getSimpleName());
      try {
         session.setResults(getMediator().updateOperations(session.getOperations(), null));
         boolean hasCancelled = Process.Result.CANCEL.equals(session.getResultCode());

         for (Rsp<Process> ret : session.getResults()) {
            if (ret.hasException()) throw ret.getException();
            if (ret.wasReceived() && ret.getValue() != null && ret.getValue().getResult() != null) {
               hasCancelled |= Process.Result.CANCEL.equals(ret.getValue().getResult());
            }
            else {
               session.setResultCode(Process.Result.ERROR);
               throw new SessionException(ErrorCode.UPDATE_ERROR);
            }
         }

         session.setResultCode(hasCancelled ? Process.Result.CANCEL : Process.Result.SUCCESS);
      }
      catch (Throwable e) {
         logger.error("Send exception in UPDATE: ", e);
         session.setResultCode(Process.Result.ERROR);
         throw new SessionException(ErrorCode.UPDATE_ERROR);
      }
   }
}
