/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.productlibrary.utility.sorts;

import android.app.Activity;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.model.product.library.Product;
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
 * Test Class for DefaultRateSortComparator.
 *
 * @author: Aakanksha Supekar
 */

@RunWith(RobolectricMavenTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class) public class DefaultRateSortComparatorTest {

   private DefaultRateSortComparator defaultRateSortComparator;
   private Activity activity;

   @Before public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);

      activity = Robolectric.setupActivity(Activity.class);

      defaultRateSortComparator = new DefaultRateSortComparator(activity);

      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();
   }

   @Test public void testEqual() {

      Product product1 = new Product();
      product1.setDefaultRate(5.0);
      product1.setName("abc");
      Product product2 = new Product();
      product2.setDefaultRate(5.0);
      product2.setName("abc");
      int result = defaultRateSortComparator.compare(product1, product2);
      Assert.assertTrue("expected to be equal", result == 0);
   }

   @Test public void testGreaterThan() {
      Product product1 = new Product();
      Product product2 = new Product();
      product1.setDefaultRate(6.0);
      product2.setDefaultRate(5.0);
      int result = defaultRateSortComparator.compare(product1, product2);
      Assert.assertTrue("expected to be greater than", result >= 1);
   }

   @Test public void testLessThan() {
      Product product1 = new Product();
      Product product2 = new Product();
      product1.setDefaultRate(5.0);
      product2.setDefaultRate(6.0);
      int result = defaultRateSortComparator.compare(product1, product2);
      Assert.assertTrue("expected to be less than", result <= -1);
   }

}
