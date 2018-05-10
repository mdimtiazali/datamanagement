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
 * Resolve session property data for DISCOVERY task
 *
 * @author: junsu.shin@cnhind.com
 */
public class DiscoveryResolver implements Resolver {
   private static final Logger logger = LoggerFactory.getLogger(DiscoveryResolver.class);
   private final DatasourceHelper dsHelper;

   public DiscoveryResolver(final DatasourceHelper dsHelper) {
      this.dsHelper = dsHelper;
   }

   @Override
   public void resolve(Session session) throws SessionException {
      if (SessionUtil.isManageAction(session) || SessionUtil.isExportAction(session)) {
         logger.trace("resolve() - MANAGE, EXPORT");

         List<Address> sources = dsHelper.getAddressesForLocation(Datasource.LocationType.PCM);
         if (sources.isEmpty()) {
            logger.debug("resolve() - MANAGE, EXPORT: No sources");
            throw new SessionException(ErrorCode.NO_SOURCE_DATASOURCE);
         }
         session.setSources(sources);
      }
      else if (SessionUtil.isImportAction(session)) {
         logger.trace("resolve() - IMPORT");

         List<Address> sources = dsHelper.getAddressesForLocation(Datasource.LocationType.USB);
         if (sources.isEmpty()) {
            logger.debug("resolve() - IMPORT: No sources");
            throw new SessionException(ErrorCode.NO_SOURCE_DATASOURCE);
         }
         session.setSources(sources);
      }
   }
}
