/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.datamng.DataUtils;
import com.cnh.pf.datamng.HostnameAddressGenerator;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.jgroups.Address;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Class that maps Datasource.Source types to actual datasource addresses
 * @author oscar.salazar@cnhind.com
 */
@Singleton
public class DatasourceHelper {
   private static final Logger logger = LoggerFactory.getLogger(DatasourceHelper.class);

   private Mediator mediator;
   private ConcurrentHashMap<Datasource.LocationType, Set<Address>> addressMapForLocationType;
   private volatile View currentView;

   @Inject
   public DatasourceHelper(Mediator mediator) {
      this.mediator = mediator;

      addressMapForLocationType = new ConcurrentHashMap<Datasource.LocationType, Set<Address>>();
   }

   /**
    * An interface to get notified of a change in group channel
    */
   public interface onConnectionChangeListener {
      /**
       * Callback when there is a change in group channel
       *
       * @param left    array of addresses for left nodes
       * @param join    array of addresses for joined nodes
       * @param updateNeeded     true if view (UI) update is needed
       */
      void onConnectionChange(Address[] left, Address[] join, boolean updateNeeded);

      /**
       * Callback when there is a change in cloud datasource connection
       */
      void onCloudConnectionChange();
   }

   /**
    * Update the address map given updated View of the channel. View provides a snapshot of current list of
    * nodes in the channel.
    *
    * @param view    channel view
    */
   public void updateView(View view, final onConnectionChangeListener listener) {
      final View oldView = currentView;
      currentView = view;
      final Address[][] diff = View.diff(oldView, view);

      logger.trace("View change.  Left: {}, Joined: {}", Arrays.toString(diff[1]), Arrays.toString(diff[0]));
      new Thread("view map update") {
         @Override
         public void run() {
            addJoinedAddresses(diff[0]);
            boolean cloudChanged = cloudConnectionChanged(diff[1], diff[0]);
            boolean updateNeeded = viewUpdateNeeded(diff[1], diff[0]);
            // NOTE: viewUpdateNeeded() needs to look at info associated with LEFT addresses. Remove left
            // addresses after calling viewUpdateNeeded().
            removeLeftAddresses(diff[1]);
            if (listener != null) {
               listener.onConnectionChange(diff[1], diff[0], updateNeeded);
               if (cloudChanged) listener.onCloudConnectionChange();
            }
         }
      }.start();
   }

   /**
    * Return true if there is a change in CLOUD datasource connection.
    *
    * @param left       left nodes
    * @param joined     joined nodes
    * @return
    */
   private boolean cloudConnectionChanged(Address[] left, Address[] joined) {
      // CLOUD datasource left the group.
      for (Address addr : left) {
         if (Datasource.LocationType.CLOUD.equals(findLocationType(addr))) {
            return true;
         }
      }

      // CLOUD datasource joined the group.
      for (Address addr : joined) {
         if (Datasource.LocationType.CLOUD.equals(findLocationType(addr))) {
            return true;
         }
      }
      return false;
   }

   /**
    * Return true if view update is needed. View update is considered to be required when at least
    * one of internal datasource join or left the channel.
    *
    * @param left    left nodes
    * @param joined  joined nodes
    * @return  true if view update is needed
    */
   public boolean viewUpdateNeeded(Address[] left, Address[] joined) {
      boolean updateNeeded = false;

      // At least one of INTERNAL datasource left the group.
      for (Address addr : left) {
         if (Datasource.LocationType.PCM.equals(findLocationType(addr))) {
            updateNeeded = true;
            break;
         }
      }

      // At least one of INTERNAL datasource joined the group.
      for (Address addr : joined) {
         if (Datasource.LocationType.PCM.equals(findLocationType(addr))) {
            updateNeeded = true;
            break;
         }
      }

      logger.debug("viewUpdateNeeded(): updateNeeded - {}", updateNeeded);
      return updateNeeded;
   }

   /**
    * Remove addresses of left nodes from the address map.
    *
    * @param left    left node addresses
    */
   private synchronized void removeLeftAddresses(Address[] left) {
      //remove all left members from map
      for (Address addr : left) {
         logger.trace("Remove left address: {}", addr);
         for (Iterator<Map.Entry<Datasource.LocationType, Set<Address>>> it = addressMapForLocationType.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Datasource.LocationType, Set<Address>> entry = it.next();
            entry.getValue().remove(addr);
            if (entry.getValue().isEmpty()) {
               it.remove();
            }
         }
      }
   }

   /**
    * Add addresses of joined nodes to the address map.
    *
    * @param joined  joined node addresses
    */
   private synchronized void addJoinedAddresses(Address[] joined) {
      try {
         //add joined members
         for (Address addr : joined) {
            logger.trace("Add joined address: {}", addr);

            if (addr.equals(mediator.getAddress())) continue;
            Datasource.LocationType locType = mediator.getLocationType(addr);
            if (!addressMapForLocationType.containsKey(locType)) {
               addressMapForLocationType.put(locType, new HashSet<Address>());
            }
            addressMapForLocationType.get(locType).add(addr);
         }
      }
      catch (Throwable e) {
         logger.error("", e);
      }
   }

   /**
    * Return list of address of this location type {USB, DISPLAY, PCM, CLOUD}
    * @param locType {@link Datasource.LocationType }
    * @return
    */
   public synchronized List<Address> getAddressesForLocation(Datasource.LocationType locType) {
      if (addressMapForLocationType.containsKey(locType)) {
         return new ArrayList<Address>(addressMapForLocationType.get(locType));
      }
      return new ArrayList<Address>();
   }

   /**
    * Find out location type (PCM, DISPLAY, USB, CLOUD) for given address
    *
    * @param address    input address
    * @return     location type
    */
   public synchronized Datasource.LocationType findLocationType(Address address) {
      for (Map.Entry<Datasource.LocationType, Set<Address>> map : addressMapForLocationType.entrySet()) {
         for (Address addr : map.getValue()) {
            if (addr.equals(address)) {
               return map.getKey();
            }
         }
      }

      logger.error("This address({}) is not registered in the location type map.", address.toString());
      return null;
   }

   /**
    * Return hostname string given node address.
    *
    * @param addr    node address
    * @return  hostname string
    */
   public static String getHostname(Address addr) {
      String name = DataUtils.getHostname(addr);
      if (Strings.isNullOrEmpty(name)) name = DataUtils.getMac(addr);
      if (Strings.isNullOrEmpty(name)) {
         InetAddress inetAddress = DataUtils.getPropertyAddress(addr, HostnameAddressGenerator.INET4);
         if (inetAddress != null) {
            name = inetAddress.toString();
         }
      }
      if (Strings.isNullOrEmpty(name)) {
         InetAddress inetAddress = DataUtils.getInetAddress(addr);
         if (inetAddress != null) {
            name = inetAddress.toString();
         }
      }
      return name;
   }
}
