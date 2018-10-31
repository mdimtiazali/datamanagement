/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.productlibrary.utility.sorts;

import android.app.Activity;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.library.CropType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.*;

/**
 * Test Class for VarietyByNameComparator.
 *
 * @author: Aakanksha Supekar
 */

@RunWith(RobolectricMavenTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class) public class VarietyByNameComparatorTest {

   private VarietyByNameComparator varietyByNameComparator;
   private Activity activity;

   @Before public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);

      activity = Robolectric.setupActivity(Activity.class);

      varietyByNameComparator = new VarietyByNameComparator(activity);

      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();

   }

   @Test public void testEqual() {

      Variety varietyOne = new Variety();
      Variety varietyTwo = new Variety();

      varietyOne.setName("abc");
      varietyTwo.setName("abc");

      varietyOne.setCropType(CropType.CORN);
      varietyTwo.setCropType(CropType.CORN);

      int result = varietyByNameComparator.compare(varietyOne, varietyTwo);
      Assert.assertTrue("expected to be equal", result == 0);
   }

   @Test public void testGreaterThan() {
      Variety varietyOne = new Variety();
      Variety varietyTwo = new Variety();
      varietyOne.setName("xyz");
      varietyTwo.setName("abc");
      int result = varietyByNameComparator.compare(varietyOne, varietyTwo);
      Assert.assertTrue("expected to be greater than", result >= 1);
   }

   @Test public void testLessThan() {
      Variety varietyOne = new Variety();
      Variety varietyTwo = new Variety();
      varietyOne.setName("abc");
      varietyTwo.setName("xyz");
      int result = varietyByNameComparator.compare(varietyOne, varietyTwo);
      Assert.assertTrue("expected to be less than", result <= -1);
   }

}
