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
import com.cnh.pf.model.product.library.ProductMix;
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
 * Test Class for ProductMixNameSortComparator.
 *
 * @author: Aakanksha Supekar
 */

@RunWith(RobolectricMavenTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class) public class ProductMixNameSortComparatorTest {

   private ProductMixNameSortComparator productMixNameSortComparator;
   private Activity activity;

   @Before public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);

      activity = Robolectric.setupActivity(Activity.class);

      productMixNameSortComparator = new ProductMixNameSortComparator(activity);

      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();

   }

   @Test public void testEqual() {
      ProductMix productMixOne = new ProductMix();
      ProductMix productMixTwo = new ProductMix();

      Product productOne = new Product();
      Product productTwo = new Product();

      productOne.setName("abc");
      productTwo.setName("abc");

      productMixOne.setProductMixParameters(productOne);
      productMixTwo.setProductMixParameters(productTwo);

      int result = productMixNameSortComparator.compare(productMixOne, productMixTwo);
      Assert.assertTrue("expected to be equal", result == 0);
   }

   @Test

   public void testGreaterThan() {
      ProductMix productMixOne = new ProductMix();
      ProductMix productMixTwo = new ProductMix();

      Product productOne = new Product();
      Product productTwo = new Product();

      productOne.setName("xyz");
      productTwo.setName("abc");

      productMixOne.setProductMixParameters(productOne);
      productMixTwo.setProductMixParameters(productTwo);
      int result = productMixNameSortComparator.compare(productMixOne, productMixTwo);

      Assert.assertTrue("expected to be greater than", result >= 1);
   }

   @Test public void testLessThan() {
      ProductMix productMixOne = new ProductMix();
      ProductMix productMixTwo = new ProductMix();

      Product productOne = new Product();
      Product productTwo = new Product();

      productOne.setName("abc");
      productTwo.setName("xyz");

      productMixOne.setProductMixParameters(productOne);
      productMixTwo.setProductMixParameters(productTwo);

      int result = productMixNameSortComparator.compare(productMixOne, productMixTwo);
      Assert.assertTrue("expected to be less than", result <= -1);
   }

}
