/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.cnh.pf.data.management.MediumImpl;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.datamng.DataUtils;
import com.cnh.pf.datamng.HostnameAddressGenerator;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nullable;
import javax.inject.Named;

/**
 * Class that maps Datasource.Source types to actual datasource addresses
 * @author oscar.salazar@cnhind.com
 */
@Singleton public class DatasourceHelper implements MediumImpl {
   private static final Logger logger = LoggerFactory.getLogger(DatasourceHelper.class);

   private Mediator mediator;
   private File usbFile;
   private ConcurrentHashMap<Datasource.Source, Map<Address, List<String>>> sourceMap = null;

   @Inject
   public DatasourceHelper(Mediator mediator, @Named("usb") File usbFile) {
      this.mediator = mediator;
      this.usbFile = usbFile;
   }

   public void setSourceMap(View view) throws Exception {
      sourceMap = new ConcurrentHashMap<Datasource.Source, Map<Address, List<String>>>();
      try {
         RspList<Datasource.Source[]> rsp = mediator.getAllSources(null);
         RspList<String[]> rspType = mediator.getAllDatatypes(null);
         for (Map.Entry<Address, Rsp<Datasource.Source[]>> entry : rsp.entrySet()) {
            Rsp<Datasource.Source[]> response = entry.getValue();
            if (response.hasException()) {
               logger.warn("Couldn't receive sources from {}" + UUID.get(entry.getKey()));
               continue;
            }
            if (response.wasReceived()) {
               logger.info("addr: " + entry.getKey().toString() + " is valid datasource");
               for (Datasource.Source source : Arrays.asList(entry.getValue().getValue())) {
                  if (sourceMap.get(source) == null) {
                     sourceMap.put(source, new HashMap<Address, List<String>>());
                  }
                  List<String> types = getDataTypes(entry.getKey(), rspType);
                  sourceMap.get(source).put(entry.getKey(), types);
               }
            }
            else {
               logger.info("addr: " + entry.getKey().toString() + " is not valid datasource");
            }
         }
      }
      catch (Exception e) {
         logger.error("Exception getSources", e);
      }
      logger.debug("sourceMap: {}", sourceMap.toString());
   }

   private List<String> getDataTypes(Address addr, RspList<String[]> rsp) {
      return Arrays.asList(rsp.get(addr).getValue());
   }

   /**
    * Return list of address of this source type {INTERNAL, USB, EXTERNAL}
    * @param sourceType {@link Datasource.Source }
    * @return
    */
   public Address[] getAddressForSourceType(Datasource.Source sourceType) {
      if (sourceMap != null && sourceMap.containsKey(sourceType)) {
         return sourceMap.get(sourceType).keySet().toArray(new Address[0]);
      }
      return new Address[0];
   }

   /**
    * Return list of address of these source types {INTERNAL, USB, DISPLAY} ONLY on this host.
    * @param sourceTypes {@link Datasource.Source }
    * @return
    */
   public Address[] getAddressForSourceType(Datasource.Source []sourceTypes) {
      Collection<Address> addresses = new ArrayList<Address>();
      final String myHostname = getHostname(mediator.getAddress());
      if (sourceMap != null) {
         for(Datasource.Source source : sourceTypes) {
            if(!sourceMap.containsKey(source)) continue;
            if(source.equals(Datasource.Source.DISPLAY)) { //filter display sources by host
               addresses.addAll(Collections2.filter(sourceMap.get(source).keySet(), new Predicate<Address>() {
                  @Override public boolean apply(@Nullable Address input) {
                     return myHostname.equals(getHostname(input));
                  }
               }));
            }
            else {
               addresses.addAll(sourceMap.get(source).keySet());
            }
         }
      }

      return addresses.toArray(new Address[addresses.size()]);
   }

   /**
    * Finds the datasource that handles Datatype depending on internal, usb, external source type.
    * @param sourceType {Internal, USB}
    * @param type {PFDS, VIP, User Layout}
    * @return Address of responsible datasource
    */
   public Address[] getDestinationAddresses(Datasource.Source sourceType, String type) throws Exception {
      Set<Address> addresses = new HashSet<Address>();
      for (Map.Entry<Address, List<String>> map : sourceMap.get(sourceType).entrySet()) {
         if (map.getValue().contains(type)) {
            addresses.add(map.getKey());
         }
      }
      return addresses.toArray(new Address[addresses.size()]);
   }

   @Override public List<MediumDevice> getDevices() {
      List<MediumDevice> devs = new ArrayList<MediumDevice>();
      if(usbFile != null) {
         devs.add(new MediumDevice(Datasource.Source.USB, usbFile, "USB"));
      }
      String myHostname = getHostname(mediator.getAddress());
      if(Strings.isNullOrEmpty(myHostname)) {
         throw new IllegalStateException("No hostname for mediator connection");
      }
      for(Address addr : getAddressForSourceType(Datasource.Source.DISPLAY)) {
         String name = getHostname(addr);
         if(Strings.isNullOrEmpty(name)) {
            logger.warn("Datasource without machine name");
            continue;
         }
         if(myHostname.equals(name)) continue;
         devs.add(new MediumDevice(Datasource.Source.DISPLAY, addr, name));
      }
      return devs;
   }

   public static String getHostname(Address addr) {
      String name = DataUtils.getHostname(addr);
      if(Strings.isNullOrEmpty(name)) name = DataUtils.getProperty(addr, HostnameAddressGenerator.INET4);
      if(Strings.isNullOrEmpty(name)) name = DataUtils.getInetAddress(addr);
      return name;
   }
}
