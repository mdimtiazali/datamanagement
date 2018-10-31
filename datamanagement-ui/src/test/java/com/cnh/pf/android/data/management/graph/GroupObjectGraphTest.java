/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.graph;

import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.TestApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

/* GroupObjectGraphTest Handles UnitTests for GroupObjectGraph
 *@author Pallab Datta
 * */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class GroupObjectGraphTest {
   private GroupObjectGraph mGroupObjectGraph;

   @Test
   public void ConstructorTest() {
      mGroupObjectGraph = new GroupObjectGraph(null, "test", "DataManagement");
      assertNotNull(mGroupObjectGraph);
      mGroupObjectGraph = new GroupObjectGraph(null, "UnitTesT", "DataManagement", null, new ObjectGraph());
      assertNotNull(mGroupObjectGraph);
   }
}