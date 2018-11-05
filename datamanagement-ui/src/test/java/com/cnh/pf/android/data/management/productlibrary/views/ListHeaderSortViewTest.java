/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.productlibrary.views;

import com.cnh.pf.android.data.management.TestApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.RuntimeEnvironment.application;

/*ListHeaderSortViewTest Handles UnitTests for ListHeaderSortView
 *@author Pallab Datta
 * */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class ListHeaderSortViewTest {

   private ListHeaderSortView ListObj;

   @Before public void setUp() throws Exception {
      ListObj = new ListHeaderSortView(application);
      ListObj = new ListHeaderSortView(application, null);
      ListObj = new ListHeaderSortView(application, null, 0);

   }

   @Test public void SetTest() {
      ListObj.setState(4);
      assertEquals(2, ListObj.getState());

   }

   @Test public void getTest() {
      ListObj.setState(5);
      assertEquals(2, ListObj.getState());

   }

   @Test public void PerformClickTest() {
          ListObj.nextState();
          assertFalse("false",ListObj.performClick());
   }

   @Test public void SetStateTest() {
         int state = 4;
         ListObj.setState(state);
         assertFalse("False",(state>=ListObj.STATE_SORT_ASC && state<=ListObj.STATE_NO_SORT));
         assertEquals(2, ListObj.getCurrentState());
   }

   @Test public void NextStateTest(){
      int state = 3;
      ListObj.setCurrentState(state);
      assertTrue(ListObj.getCurrentState()>ListObj.STATE_SORT_ASC);
      assertEquals(3,ListObj.getCurrentState());
   }
   @Test public void PreviousStateTest(){
      int state =0 ;
      ListObj.setCurrentState(state);
      assertFalse(ListObj.getCurrentState()<ListObj.STATE_SORT_DESC);
      assertEquals(0,ListObj.getCurrentState());
   }
}



