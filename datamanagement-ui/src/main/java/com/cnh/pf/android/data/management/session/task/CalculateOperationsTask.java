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
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.datamng.Process;
import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Session task class to execute CALCULATE_OPERATIONS session
 *
 * @author: junsu.shin@cnhind.com
 */
public class CalculateOperationsTask extends SessionOperationTask<Void> {
   private static final Logger logger = LoggerFactory.getLogger(CalculateOperationsTask.class);

   public CalculateOperationsTask(@Nonnull Mediator mediator, @Nonnull SessionNotifier notifier) {
      super(mediator, notifier);
   }

   @Override
   protected void processSession(@Nonnull Session session) throws SessionException {
      logger.debug("{}:processSession()", this.getClass().getSimpleName());
      try {
         Address[] addresses = session.getDestinations().toArray(new Address[0]);
         logger.debug("Calculate operations to address: {}", SessionUtil.addressToString(addresses));
         if (addresses != null && addresses.length > 0) {
            List<Operation> operations = getMediator().calculateOperations(session.getObjectData(), addresses);
            session.setOperations(operations);

            boolean hasCancelled = Process.Result.CANCEL.equals(session.getResultCode());
            session.setResultCode(hasCancelled ? Process.Result.CANCEL : Process.Result.SUCCESS);
         }
         else {
            logger.warn("Skipping calculate operations");
            session.setResultCode(Process.Result.ERROR);
            throw new SessionException(ErrorCode.NO_DESTINATION_DATASOURCE);
         }
      }
      catch (Throwable e) {
         logger.error("Send exception in CALCULATE_OPERATIONS: ", e);
         session.setResultCode(Process.Result.ERROR);
         throw new SessionException(ErrorCode.CALCULATE_OPERATIONS_ERROR);
      }
   }
}
