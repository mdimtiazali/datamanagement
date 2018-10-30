/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.misc;

import com.cnh.pf.android.data.management.TestApp;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.*;
import static org.robolectric.RuntimeEnvironment.application;

/**
 *TreeLineWidgetTest Handles all UnitTests for TreeLineWidget.
 *@auther Himanshu Ranawat
 */

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class TreeLineWidgetTest {

   private TreeLineWidget treeLineWidget;

   @Before
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);
      treeLineWidget = new TreeLineWidget(application);
      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();

   }

   @After
   public void tearDown() throws Exception {

      treeLineWidget = null;
   }

   @Test
   public void testsetCornerFlag() {
      //case if the value of flag is true
      treeLineWidget.setCornerFlag(true);
      Assert.assertEquals(true, treeLineWidget.isCorner());

      //case if the value of the flag is false
      treeLineWidget.setCornerFlag(false);
      Assert.assertEquals(false, treeLineWidget.isCorner());
   }
}