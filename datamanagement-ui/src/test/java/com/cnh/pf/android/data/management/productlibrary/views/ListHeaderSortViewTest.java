/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;

import com.cnh.pf.android.data.management.TestApp;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.robolectric.RuntimeEnvironment.application;

/*ListHeaderSortViewTest Handles UnitTests for ListHeaderSortView
 *@author Pallab Datta
 * */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class ListHeaderSortViewTest {

   private ListHeaderSortView ListObj;

   @Before
   public void setUp() throws Exception {
      ListObj = new ListHeaderSortView(application);
   }

   @Test
   public void SetTest() {
      ListObj.setState(4);
      assertEquals(2, ListObj.getState());

   }

   @Test
   public void getTest() {
      ListObj.setState(5);
      assertEquals(2, ListObj.getState());
   }

}


