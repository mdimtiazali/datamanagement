/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management.misc;

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

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/*IconizedFileTest Handles all the UnitTests for IconizedFile
 *@author Pallab Datta
 * */

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class IconizedFileTest {

   private File file;
   private IconizedFile mIconizedFile;

   @Before
   public void setUp() throws Exception {
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
      mIconizedFile = new IconizedFile(file, IconizedFile.INVALID_RESSOURCE_ID);
      file = mock(File.class);

   }

   @Test
   public void ConstructorTest() {
      IconizedFile mIconizedFile0 = new IconizedFile(file);
      assertNotNull(mIconizedFile0);
      IconizedFile mIconziedFile1 = new IconizedFile(file, IconizedFile.INVALID_RESSOURCE_ID);
      assertNotNull(mIconziedFile1);
   }

   @Test
   public void getFileTest() {
      mIconizedFile.setFile(file);
      assertTrue(mIconizedFile.getFile() instanceof File);

   }

   @Test
   public void setFileTest() {
      mIconizedFile.setFile(file);
      assertTrue(mIconizedFile.getFile() instanceof File);

   }

   @Test
   public void getIconResourceIdTest() {
      int i = 5;
      mIconizedFile.setIconResourceId(i);
      assertTrue(mIconizedFile.getIconResourceId() == i);
   }

   @Test
   public void setIconResourceIdTest() {
      int i = 7;
      mIconizedFile.setIconResourceId(i);
      assertTrue(mIconizedFile.getIconResourceId() == i);
   }

   @After
   public void tearDown() throws Exception {
      mIconizedFile = null;
      assertNull(mIconizedFile);

   }
}