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
import com.cnh.pf.android.data.management.BaseDataFragment;
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

/**
 * Session task class to execute DISCOVERY session
 *
 * @author: junsu.shin@cnhind.com
 */
public class DiscoveryTask extends SessionOperationTask<Void> {
   private static final Logger logger = LoggerFactory.getLogger(DiscoveryTask.class);

   public DiscoveryTask(@Nonnull Mediator mediator, @Nonnull SessionNotifier notifier) {
      super(mediator, notifier);
   }

   @Override
   protected void processSession(@Nonnull Session session) throws SessionException {
      logger.debug("{}:processSession()", this.getClass().getSimpleName());
      Address[] addrs = session.getSources().toArray(new Address[0]);

      try {
         if (addrs != null && addrs.length > 0) {
            logger.debug("Discovery-Src Addresses: {}", SessionUtil.addressToString(addrs));

            if(BaseDataFragment.isDsPerfFlag()) {
               session.setObjectData(getMediator().discoveryNoMerge(addrs));
            }
            else {
               session.setObjectData(getMediator().discovery(addrs));
            }
            if (session.getObjectData() == null || session.getObjectData().isEmpty()) {
               session.setResultCode(Process.Result.ERROR);
               throw new SessionException(ErrorCode.NO_DATA);
            }
            else {
               session.setResultCode(Process.Result.SUCCESS);
            }
         }
         else {
            session.setResultCode(Process.Result.ERROR);
            throw new SessionException(ErrorCode.NO_SOURCE_DATASOURCE);
         }
      }
      catch (Throwable e) {
         logger.debug("Exception in DISCOVERY: ", e);
         session.setResultCode(Process.Result.ERROR);
         throw new SessionException(ErrorCode.DISCOVERY_ERROR);
      }
   }
}
