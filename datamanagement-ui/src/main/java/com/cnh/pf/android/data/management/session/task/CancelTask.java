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
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.datamng.Process;
import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Session task class to conduct session cancellation.
 *
 * @author: junsu.shin@cnhind.com
 */
public class CancelTask extends SessionOperationTask<Void> {
   private static final Logger logger = LoggerFactory.getLogger(CancelTask.class);

   public CancelTask(@Nonnull Mediator mediator) {
      super(mediator, null);
   }

   @Override
   protected void processSession(@Nonnull Session session) throws SessionException {
      logger.debug("{}:processSession()", this.getClass().getSimpleName());
      try {
         List<Address> targetAddresses = session.getDestinations();
         List<Address> sourceAddresses = session.getSources();
         //if process already running tell it to cancel.
         if (SessionUtil.isInProgress(session)) {
            if (sourceAddresses != null && !sourceAddresses.isEmpty()) {
               logger.trace("Request CANCEL to sources in the session.");
               getMediator().cancel(sourceAddresses.toArray(new Address[0]));
            }

            if (targetAddresses != null && !targetAddresses.isEmpty()) {
               logger.trace("Request CANCEL to destinations in the session.");
               getMediator().cancel(targetAddresses.toArray(new Address[0]));
            }
         }
      } catch (Exception e) {
         logger.debug("Exception in CANCEL: ", e);
      }
   }
}
