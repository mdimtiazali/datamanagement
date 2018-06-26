/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CacheManager provides caching feature for session data.
 *
 * @author: junsu.shin@cnhind.com
 */
public class CacheManager {
   private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
   // Session cache data is categorized first by the session action and per action it's further
   // broken into by the session type. CacheItem defines what to store into the cache.
   private Map<Session.Action, Map<Session.Type, CacheItem>> cache
           = new EnumMap<Session.Action, Map<Session.Type, CacheItem>>(Session.Action.class);

   /**
    * Save session data. CacheItemImpl will pick item within session selectively and provide spaces
    * to store data.
    *
    * @param session    the session object
    */
   public void save(final Session session) {
      // Cache session data only when it's successfully completed.
      if (SessionUtil.isComplete(session) && SessionUtil.isSuccessful(session)) {
         logger.trace("Cache session data {}, {}.", session.getType(), session.getAction());

         Map<Session.Type, CacheItem> newItem;

         if (cache.containsKey(session.getAction())) {
            newItem = cache.get(session.getAction());
         }
         else {
            newItem = new EnumMap<Session.Type, CacheItem>(Session.Type.class);
         }

         newItem.put(session.getType(), new CacheItemImpl(session));
         cache.put(session.getAction(), newItem);

         // DISCOVERY data from MANAGE, EXPORT can be shared.
         if (SessionUtil.isDiscoveryTask(session) && SessionUtil.isManageAction(session)) {
            logger.debug("Share MANAGE-DISCOVERY data for EXPORT.");
            cache.put(Session.Action.EXPORT, newItem);
         }
         else if (SessionUtil.isDiscoveryTask(session) && SessionUtil.isExportAction(session)) {
            logger.debug("Share EXPORT-DISCOVERY data for MANAGE.");
            cache.put(Session.Action.MANAGE, newItem);
         }
      }
   }

   /**
    * Return whether the session data is cached. It looks at session action & type and see if data is
    * stored.
    *
    * @param session    the session object (action & type)
    * @return  true if the session data is cached
    */
   public boolean cached(final Session session) {
      return cached(session.getAction(), session.getType());
   }

   /**
    * Return whether the session data is cached. It looks at session action & type and see if data is
    * stored.
    *
    * @param action  the session action
    * @param type    the session type
    * @return  true if the session data is cached
    */
   public boolean cached(final Session.Action action, final Session.Type type) {
      return cache.containsKey(action) && cache.get(action).containsKey(type);
   }

   /**
    * Retrieve cache item
    *
    * @param action  session action to search for cache item
    * @param type    session type to search for cache item
    * @return  cache item
    */
   public CacheItem retrieve(final Session.Action action, final Session.Type type) {
      if (cached(action, type)) {
         return cache.get(action).get(type);
      }
      return new NullCachItemImpl();
   }

   /**
    * Retrieve cache item
    *
    * @param session session object to search for cache item
    * @return  cache item
    */
   public CacheItem retrieve(final Session session) {
      return retrieve(session.getAction(), session.getType());
   }

   /**
    * Reset cache data corresponding to the given session
    *
    * @param session the session
    */
   public void reset(final Session session) {
      reset(session.getAction(), session.getType());
   }

   /**
    * Reset cache data corresponding to the given action & type.
    *
    * @param action     the session action
    * @param type    the session type
    */
   public void reset(final Session.Action action, final Session.Type type) {
      if (cached(action, type)) {
         cache.get(action).remove(type);

         if (cache.get(action).isEmpty()) {
            cache.remove(action);
         }
      }
   }

   /**
    * Reset all cache data defined under the given session action.
    *
    * @param action     the session action
    */
   public void reset(final Session.Action action) {
      if (cache.containsKey(action)) {
         Map<Session.Type, CacheItem> item = cache.get(action);
         if (!item.isEmpty()) {
            item.clear();
         }

         cache.remove(action);
      }
   }

   /**
    * Reset all cached data
    */
   public void resetAll() {
      if (!cache.isEmpty()) {
         for (Map<Session.Type, CacheItem> entry : cache.values()) {
            entry.clear();
         }

         cache.clear();
      }
   }

   /**
    * An interface to provide an access to cache data
    */
   public interface CacheItem {
      List<ObjectGraph> getObjectGraph();
      List<Operation> getOperations();
   }

   private class NullCachItemImpl implements CacheItem {
      @Override
      public List<ObjectGraph> getObjectGraph() {
         return new ArrayList<ObjectGraph>();
      }

      @Override
      public List<Operation> getOperations() {
         return new ArrayList<Operation>();
      }
   }

   private class CacheItemImpl implements CacheItem {
      private List<ObjectGraph> objectGraphs;
      private List<Operation> operations;

      public CacheItemImpl(final Session session) {
         objectGraphs = new ArrayList<ObjectGraph>();
         if (session.getObjectData() != null) {
            for (ObjectGraph obj : session.getObjectData()) {
               objectGraphs.add(obj);
            }
         }

         operations = new ArrayList<Operation>();
         if (session.getOperations() != null) {
            for (Operation op : session.getOperations()) {
               operations.add(op);
            }
         }
      }

      @Override
      public List<ObjectGraph> getObjectGraph() {
         return objectGraphs;
      }

      @Override
      public List<Operation> getOperations() {
         return operations;
      }
   }
}
