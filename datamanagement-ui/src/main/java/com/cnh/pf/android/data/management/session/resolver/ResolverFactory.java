/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.resolver;

import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionUtil;

/**
 * Factory class to create resolver object.
 *
 * @author: junsu.shin@cnhind.com
 */
public class ResolverFactory {
   private ResolverFactory() {}     // This is to avoid code analysis warning.

   public static Resolver createResolver(DatasourceHelper dsHelper, final Session session) {
      Resolver newResolver = null;
      if (SessionUtil.isDiscoveryTask(session)) {
         newResolver = new DiscoveryResolver(dsHelper);
      }
      else if (SessionUtil.isPerformOperationsTask(session)) {
         newResolver = new PerformOperationsResolver(dsHelper);
      }
      else if (SessionUtil.isCalculateOperationsTask(session)) {
         newResolver = new CalculateOperationsResolver(dsHelper);
      }
      else if (SessionUtil.isCalculateConflictsTask(session)) {
         newResolver = new CalculateConflictsResolver(dsHelper);
      }
      else if (SessionUtil.isUpdateTask(session) || SessionUtil.isDeleteTask(session) || SessionUtil.isPasteTask(session)) {
         // UPDATE & DELETE tasks don't need to resolve addresses for source & destination. They pull
         // address from object itself.
         newResolver = new DummyResolver();
      }

      return newResolver;
   }
}
