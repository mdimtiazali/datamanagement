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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

/*GFFObjectTest Handles all the UnitTests for GFFObject
 *@author Pallab Datta
 * */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class GFFObjectTest {

   private GFFObject mGFFObj;

   @Before
   public void setUp() throws Exception {
      //mGFFObj = mock(GFFObject.class);
      mGFFObj = new GFFObject("--", "--", new ObjectGraph());

   }

   @Test
   public void getNameTest() {
      String GFFName = "DataManagement";
      mGFFObj.setName(GFFName);
      assertTrue(mGFFObj.getName() == GFFName);
   }

   @Test
   public void setNameTest() {
      String GFFSetName = "DataManagement";
      mGFFObj.setName(GFFSetName);
      assertTrue(mGFFObj.getName() == GFFSetName);
   }

   @Test
   public void getTypeTest() {
      String GFFType = "DataManagement";
      mGFFObj.setType(GFFType);
      assertTrue(mGFFObj.getType() == GFFType);
   }

   @Test
   public void setTypeTest() {
      String GFFSetType = "DataManagement";
      mGFFObj.setType(GFFSetType);
      assertTrue(mGFFObj.getType() == GFFSetType);
   }

   @Test
   public void getDataObjTest() {
      mGFFObj.setDataObj(new ObjectGraph());
      assertTrue(mGFFObj.getDataObj() instanceof ObjectGraph);

   }

   @Test
   public void setDataObjTest() {
      mGFFObj.setDataObj(new ObjectGraph());
      assertTrue(mGFFObj.getDataObj() instanceof ObjectGraph);

   }
}