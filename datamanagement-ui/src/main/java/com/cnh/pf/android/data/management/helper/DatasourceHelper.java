/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import android.os.Environment;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.cnh.pf.data.management.MediumImpl;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.datamng.DataUtils;
import com.cnh.pf.datamng.HostnameAddressGenerator;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;

/**
 * Class that maps Datasource.Source types to actual datasource addresses
 * @author oscar.salazar@cnhind.com
 */
@Singleton
public class DatasourceHelper implements MediumImpl {
   private static final Logger logger = LoggerFactory.getLogger(DatasourceHelper.class);

   private Mediator mediator;
   private ConcurrentHashMap<Datasource.Source, Map<Address, List<String>>> sourceMap = new ConcurrentHashMap<Datasource.Source, Map<Address, List<String>>>();;
   private volatile View currentView;
   private EventManager eventManager;

   @Inject
   public DatasourceHelper(Mediator mediator, @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME) EventManager eventManager) {
      this.mediator = mediator;
      this.eventManager = eventManager;
   }

   public void setSourceMap(View view) throws Exception {
      View oldView = currentView;
      currentView = view;
      final Address[][] diff = View.diff(oldView, view);
      logger.trace("View change.  Left: {}, Joined: {}", Arrays.toString(diff[1]), Arrays.toString(diff[0]));
      new Thread("view map update") {
         @Override
         public void run() {
            updateViewMaps(diff);
         }
      }.start();
      eventManager.fire(new DataServiceConnectionImpl.ViewChangeEvent(oldView, view));
   }

   //read pending diff and update the map
   synchronized void updateViewMaps(Address[][] diff) {
      //remove all left members from map
      for (Address left : diff[1]) {
         logger.trace("Removing address: {}", left);
         for (Iterator<Map.Entry<Datasource.Source, Map<Address, List<String>>>> sourceIt = sourceMap.entrySet().iterator(); sourceIt.hasNext();) {
            Map.Entry<Datasource.Source, Map<Address, List<String>>> entry = sourceIt.next();
            entry.getValue().remove(left);
            if (entry.getValue().isEmpty()) {
               sourceIt.remove();
            }
         }
      }
      try {
         //add joined members
         for (Address joined : diff[0]) {
            if (joined.equals(mediator.getAddress())) continue;
            String[] types = mediator.getDatatypes(joined);
            Datasource.Source[] sources = mediator.getSources(joined);
            if (types != null && sources != null) {
               logger.info("addr: {} is valid datasource", joined.toString());
               List<String> dataTypes = Arrays.asList(types);
               for (Datasource.Source source : sources) {
                  HashMap<Address, List<String>> typeMap = (HashMap<Address, List<String>>) sourceMap.get(source);
                  if (typeMap == null) {
                     typeMap = new HashMap<Address, List<String>>();
                     HashMap<Address, List<String>> tmp = (HashMap<Address, List<String>>) sourceMap.putIfAbsent(source, typeMap);
                     if (tmp != null) typeMap = tmp;
                  }
                  typeMap.put(joined, dataTypes);
               }
            }
            else {
               logger.warn("addr: {} is not valid datasource", joined.toString());
            }
         }
      }
      catch (Throwable e) {
         logger.error("", e);
      }
   }

   /**
    * Return list of address of this source type {INTERNAL, USB, EXTERNAL}
    * @param sourceType {@link Datasource.Source }
    * @return
    */
   public synchronized Address[] getAddressForSourceType(Datasource.Source sourceType) {
      if (sourceMap.containsKey(sourceType)) {
         return sourceMap.get(sourceType).keySet().toArray(new Address[0]);
      }
      return new Address[0];
   }

   /**
    * Return list of address of these source types {INTERNAL, USB, DISPLAY} ONLY on this host.
    * @param sourceTypes {@link Datasource.Source }
    * @return
    */
   public synchronized List<MediumDevice> getLocalDatasources(Datasource.Source... sourceTypes) {
      List<MediumDevice> devices = new ArrayList<MediumDevice>();
      final String myHostname = getHostname(mediator.getAddress());
      for (Datasource.Source source : sourceTypes) {
         if (!sourceMap.containsKey(source)) continue;
         for (Address addr : sourceMap.get(source).keySet()) {
            if (!source.equals(Datasource.Source.DISPLAY) || myHostname.equals(getHostname(addr))) {
               devices.add(new MediumDevice(source, addr, UUID.get(addr)));
            }
         }
      }
      return devices;
   }

   /**
    * Finds the datasource that handles Datatype depending on internal, usb, external source type.
    * @param sourceType {Internal, USB}
    * @param type {PFDS, VIP, User Layout}
    * @return Address of responsible datasource
    */
   public synchronized Address[] getDestinationAddresses(Datasource.Source sourceType, String type) throws Exception {
      Set<Address> addresses = new HashSet<Address>();
      for (Map.Entry<Address, List<String>> map : sourceMap.get(sourceType).entrySet()) {
         if (map.getValue().contains(type)) {
            addresses.add(map.getKey());
         }
      }
      return addresses.toArray(new Address[addresses.size()]);
   }

   @Override
   public synchronized List<MediumDevice> getTargetDevices() {
      List<MediumDevice> devs = new ArrayList<MediumDevice>();
      //temporarily always add usb for testing
      logger.debug("getDevices external storage state = {}", Environment.getExternalStorageState());
      if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
         devs.add(new MediumDevice(Datasource.Source.USB, Environment.getExternalStorageDirectory(), "USB"));
      }
      String myHostname = getHostname(mediator.getAddress());
      logger.trace("My HOSTNAME {}", myHostname);
      if (Strings.isNullOrEmpty(myHostname)) {
         throw new IllegalStateException("No hostname for mediator connection");
      }
      for (Address addr : getAddressForSourceType(Datasource.Source.DISPLAY)) {
         String name = getHostname(addr);
         if (Strings.isNullOrEmpty(name)) {
            logger.warn("Datasource without machine name");
            continue;
         }
         logger.trace("Display hostname {}", name);
         if (myHostname.equals(name)) continue;
         devs.add(new MediumDevice(Datasource.Source.DISPLAY, addr, name));
      }
      return devs;
   }

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
