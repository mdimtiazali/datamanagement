/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.views;

import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricMavenTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class NestedExpandableListViewTest {
   private NestedExpandableListView mNestedExpandableListView;

   @Before public void setUp() throws Exception {
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

   }

   @Test public void constructorTest() {
      mNestedExpandableListView = new NestedExpandableListView(RuntimeEnvironment.application, null);
      assertNotNull(mNestedExpandableListView);
   }

   @After public void tearDown() throws Exception {
      mNestedExpandableListView = null;
      assertNull(mNestedExpandableListView);
   }
}