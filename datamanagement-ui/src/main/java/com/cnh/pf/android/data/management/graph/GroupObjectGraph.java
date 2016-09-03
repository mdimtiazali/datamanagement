/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.graph;

import com.cnh.pf.model.TypedValue;
import com.cnh.jgroups.ObjectGraph;
import org.jgroups.Address;

import java.util.Map;

/**
 * ObjectGraph that represents groups of a specified type.
 * @author oscar.salazar@cnhind.com
 */
public class GroupObjectGraph extends ObjectGraph {

   public GroupObjectGraph(Address source, String type, String name) {
      super(source, type, name);
   }

   public GroupObjectGraph(Address source, String type, String name, Map<String, TypedValue> data, ObjectGraph parent) {
      super(source, type, name, data, parent);
   }
}
