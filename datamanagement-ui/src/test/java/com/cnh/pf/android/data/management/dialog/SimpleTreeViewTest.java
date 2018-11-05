/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.dialog;

import android.app.Activity;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Test Class for SimpleTreeView.
 *
 * @author: Ranjith P.A
 */

@RunWith(RobolectricTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class) public class SimpleTreeViewTest {
   SimpleTreeView listView;
   List<Integer> ids = new LinkedList<Integer>();
   List<String> names = new LinkedList<String>();
   private int swathFolderIcon = 0;
   private String swathFolderName = "test";
   private Activity activity;

   @Before public void setUp() throws Exception {

      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

      listView = new SimpleTreeView(RuntimeEnvironment.application);
   }

   @Test public void constructorTest() {
      listView = new SimpleTreeView(RuntimeEnvironment.application);
      assertNotNull(listView);
      listView = new SimpleTreeView(RuntimeEnvironment.application, null);
      assertNotNull(listView);
   }

   @Test public void setItemsTest() {

      ids.add(1);
      ids.add(2);
      ids.add(3);
      ids.add(4);

      names.add("a");
      names.add("b");
      names.add("c");
      names.add("d");

      listView.setItems(swathFolderIcon, swathFolderName, ids, names);

      assertTrue(names.size() == listView.getAdaptorCount());

   }

}


