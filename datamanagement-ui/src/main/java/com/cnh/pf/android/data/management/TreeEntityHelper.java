/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import com.cnh.jgroups.DataTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class deals with entity grouping for all Groups {PFDS, VIP, ...}
 * @author oscar.salazar@cnhind.com
 */
public class TreeEntityHelper {

   /* Set lists all entities which are groupable in ui, entities with types specified in
   this list will be grouped in ui.
    */
   public static Set<String> groupables = new HashSet<String>() {{
      add(DataTypes.CUSTOMER);
      add(DataTypes.TASK);
      add(DataTypes.PRESCRIPTION);
      add(DataTypes.BOUNDARY);
      add(DataTypes.VEHICLE);
      add(DataTypes.IMPLEMENT);
   }};

   public static Map<String, Integer> topLevelEntites = new HashMap<String, Integer>() {{
      put(DataTypes.CUSTOMER, R.string.pfds);
      put(DataTypes.VEHICLE, R.string.vehicles);
      put(DataTypes.IMPLEMENT, R.string.imps);
   }};
}
