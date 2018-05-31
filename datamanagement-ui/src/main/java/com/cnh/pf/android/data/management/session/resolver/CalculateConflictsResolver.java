/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.resolver;

import com.cnh.jgroups.Datasource;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionUtil;
import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Resolve session property data for CALCULATE_CONFLICTS task.
 *
 * @author: junsu.shin@cnhind.com
 */
public class CalculateConflictsResolver implements Resolver {
   private static final Logger logger = LoggerFactory.getLogger(CalculateConflictsResolver.class);
   private final DatasourceHelper dsHelper;

   public CalculateConflictsResolver(final DatasourceHelper dsHelper) {
      this.dsHelper = dsHelper;
   }

   @Override
   public void resolve(Session session) throws SessionException {
      if (SessionUtil.isImportAction(session)) {
         logger.trace("resolve() - IMPORT");

         // Find the destination addresses
         List<Address> destinations = dsHelper.getAddressesForLocation(Datasource.LocationType.PCM);
         if (destinations.isEmpty()) {
            logger.debug("resolve() - IMPORT: No destinations");
            throw new SessionException(ErrorCode.NO_DESTINATION_DATASOURCE);
         }
         session.setDestinations(destinations);
      }
      else {
         logger.info("Undefined case for session resolution, CALCULATE_CONFLICT");
         throw new SessionException(ErrorCode.CALCULATE_CONFLICT_ERROR);
      }
   }
}
