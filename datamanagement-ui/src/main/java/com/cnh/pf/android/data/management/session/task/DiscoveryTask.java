/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.task;

import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

   /**
    * Restructure ObjectGraph hierarchy. This performs re-binding of ObjectGraph nodes as necessary.
    *
    * @param objectGraphs  a list of ObjectGraph nodes
    */
   private void resturctureObjectGraph(List<ObjectGraph> objectGraphs) {
      // Separate tractor control config node from the list and keep them aside
      final Map<String, ObjectGraph> configObjectGraphMap = new HashMap<String, ObjectGraph>();

      Iterator<ObjectGraph> it = objectGraphs.iterator();
      while (it.hasNext()) {
         ObjectGraph og = it.next();
         if (DataTypes.TRACTOR_CONTROL_CONFIG.equals(og.getType())) {
            it.remove();
            configObjectGraphMap.put(og.getId(), og);
         }
      }
      if (configObjectGraphMap.isEmpty()) return;

      // Re-bind tractor control config node under veh-imp config node
      for (ObjectGraph og : objectGraphs) {
         ObjectGraph.traverse(og, ObjectGraph.TRAVERSE_DOWN, new ObjectGraph.Visitor<ObjectGraph>() {
            @Override
            public boolean visit(ObjectGraph objectGraph) {
               if (DataTypes.VEHICLE_IMPLEMENT_CONFIG.equals(objectGraph.getType())) {
                  String uuid = objectGraph.getId();
                  if (configObjectGraphMap.containsKey(uuid)) {
                     objectGraph.addChild(configObjectGraphMap.get(uuid));
                  }
               }
               return true;
            }
         });
      }
   }

   @Override
   protected void processSession(@Nonnull Session session) throws SessionException {
      logger.debug("{}:processSession()", this.getClass().getSimpleName());
      Address[] addrs = session.getSources().toArray(new Address[0]);

      try {
         if (addrs != null && addrs.length > 0) {
            logger.debug("Discovery-Src Addresses: {}", SessionUtil.addressToString(addrs));

            List<ObjectGraph> discoveryObjs;
            if (BaseDataFragment.isDsPerfFlag()) {
               discoveryObjs = getMediator().discoveryNoMerge(addrs);
            }
            else {
               discoveryObjs = getMediator().discovery(addrs);
            }

            if (!SessionUtil.isImportAction(session)) {
               resturctureObjectGraph(discoveryObjs);
            }

            session.setObjectData(discoveryObjs);
            session.setResultCode(Process.Result.SUCCESS);
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
