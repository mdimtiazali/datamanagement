/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.jgroups.data.service.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jgroups.Address;
import org.jgroups.View;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Class that maps Datasource.Source types to actual datasource addresses
 * @author oscar.salazar@cnhind.com
 */
@Singleton public class DatasourceHelper {
   private static final Logger logger = LoggerFactory.getLogger(DatasourceHelper.class);

   private @Inject Mediator mediator;
   private ConcurrentHashMap<Datasource.Source, Map<Address, List<Datasource.DataType>>> sourceMap;

   public void setSourceMap(View view) throws Exception {
      sourceMap = new ConcurrentHashMap<Datasource.Source, Map<Address, List<Datasource.DataType>>>();
      try {
         RspList<Datasource.Source[]> rsp = mediator.getAllSources();
         RspList<Datasource.DataType[]> rspType = mediator.getAllDatatypes();
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
                     sourceMap.put(source, new HashMap<Address, List<Datasource.DataType>>());
                  }
                  List<Datasource.DataType> types = getDataTypes(entry.getKey(), rspType);
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
   }

   private List<Datasource.DataType> getDataTypes(Address addr, RspList<Datasource.DataType[]> rsp) {
      for (Map.Entry<Address, Rsp<Datasource.DataType[]>> entry : rsp.entrySet()) {
         if (entry.getKey().equals(addr)) {
            return Arrays.asList(entry.getValue().getValue());
         }
      }
      return null;
   }

   /**
    * Return list of address of this source type {INTERNAL, USB, EXTERNAL}
    * @param sourceType {@link Datasource.Source }
    * @return
    */
   public Address[] getAddressForSourceType(Datasource.Source sourceType) {
      return sourceMap.get(sourceType).keySet().toArray(new Address[sourceMap.get(sourceType).keySet().size()]);
   }

   /**
    * Finds the datasource that handles Datatype depending on internal, usb, external source type
    * @param sourceType {Internal, USB}
    * @param type {PFDS, VIP, User Layout}
    * @return Address of responsible datasource
    */
   public Address[] getDestinationAddresses(Datasource.Source sourceType, Datasource.DataType type) throws Exception {
      Set<Address> addresses = new HashSet<Address>();
      for (Map.Entry<Address, List<Datasource.DataType>> map : sourceMap.get(sourceType).entrySet()) {
         if (map.getValue().contains(type)) {
            addresses.add(map.getKey());
         }
      }
      return addresses.toArray(new Address[addresses.size()]);
   }
}
