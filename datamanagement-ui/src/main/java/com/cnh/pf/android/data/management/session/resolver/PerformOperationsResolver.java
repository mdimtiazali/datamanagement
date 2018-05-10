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
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionUtil;
import org.jgroups.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolve session property data for PERFORM_OPERATIONS task.
 *
 * @author: junsu.shin@cnhind.com
 */
public class PerformOperationsResolver implements Resolver {
   private static final Logger logger = LoggerFactory.getLogger(PerformOperationsResolver.class);
   private final DatasourceHelper dsHelper;

   public PerformOperationsResolver(final DatasourceHelper dsHelper) {
      this.dsHelper = dsHelper;
   }

   @Override
   public void resolve(Session session) throws SessionException {
      if (SessionUtil.isExportAction(session)) {
         logger.trace("resolve() - EXPORT");

         String format = session.getExtra().getFormat();
         if (format == null || (!format.equals("ISOXML") && !format.equals("PF Database"))) {
            logger.debug("resolve() - EXPORT: Invalid format");
            throw new SessionException(ErrorCode.INVALID_FORMAT);
         }

         List<Address> destinations = new ArrayList<Address>();
         if (session.getExtra().isUsbExtra()) {
            destinations = dsHelper.getAddressesForLocation(Datasource.LocationType.USB);
         }
         else if (session.getExtra().isCloudExtra()) {
            destinations = dsHelper.getAddressesForLocation(Datasource.LocationType.CLOUD);
         }

         if (destinations.isEmpty()) {
            logger.debug("resolve() - EXPORT: No destinations");
            throw new SessionException(ErrorCode.NO_DESTINATION_DATASOURCE);
         }
         session.setDestinations(destinations);

         // Fill in operations linked to object data
         if (session.getOperations() == null || session.getOperations().isEmpty()) {
            List<Operation> operations = new ArrayList<Operation>();
            for (ObjectGraph obj : session.getObjectData()) {
               operations.add(new Operation(obj, null));
            }
            session.setOperations(operations);
         }
      }
   }
}