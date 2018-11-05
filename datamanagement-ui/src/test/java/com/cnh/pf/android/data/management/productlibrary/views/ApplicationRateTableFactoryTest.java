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

import android.widget.TableLayout;
import android.widget.TableRow;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductDisplayItem;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductUnits;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.robolectric.RuntimeEnvironment.application;

/**
 *ApplicationRateTableFactoryTest Handles all UnitTests for ApplicationRateTableFactory.
 *@auther Himanshu Ranawat
 */

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class ApplicationRateTableFactoryTest {

   private ApplicationRateTableFactory applicationRateTableFactory;
   private ApplicationRateTableFactory.ApplicationRateTableData applicationRateTableData;
   private Product product;
   private ProductUnits productUnit;
   private Map<Product, ApplicationRateTableFactory.ApplicationRateTableData> applicationRateTableDataMap;
   @Mock
   TableLayout tableLayout;

   @Before
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);
      productUnit = new ProductUnits();
      productUnit.setName("kg/ha");
      productUnit.setMultiplyFactorFromBaseUnits(10000);
      productUnit.setMeasurementSystem(MeasurementSystem.METRIC);
      productUnit.setForm(ProductForm.BULK_SEED);
      productUnit.setDisplayItem(ProductDisplayItem.RATES);
      product = new Product();
      product.setName("Wheat");
      product.setForm(ProductForm.SEED);
      product.setRateDisplayUnitsMetric(productUnit);
      applicationRateTableData = new ApplicationRateTableFactory.ApplicationRateTableData();
      applicationRateTableFactory = new ApplicationRateTableFactory();
      applicationRateTableDataMap = new HashMap<Product, ApplicationRateTableFactory.ApplicationRateTableData>();
      applicationRateTableDataMap.put(product, applicationRateTableData);
      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();

   }

   @After
   public void tearDown() throws Exception {

      applicationRateTableFactory = null;
      applicationRateTableDataMap = null;
      applicationRateTableData = null;
      product = null;
      productUnit = null;
   }

   @Test
   public void testcreateTableRowForProductMixDialog() {

      Assert.assertTrue(applicationRateTableFactory.createTableRowForProductMixDialog(applicationRateTableDataMap.get(product), application, tableLayout) instanceof TableRow);

   }

   @Test
   public void testcreateTableRowForProductMixAdapter() {

      Assert.assertTrue(applicationRateTableFactory.createTableRowForProductMixAdapter(applicationRateTableDataMap.get(product), application, tableLayout) instanceof TableRow);

   }

   @Test
   public void testgetProductForm() {
      applicationRateTableData.setProductForm(product.getForm());
      Assert.assertEquals(product.getForm(), applicationRateTableData.getProductForm());
   }

   @Test
   public void testsetProductForm() {
      applicationRateTableData.setProductForm(product.getForm());
      Assert.assertEquals(product.getForm(), applicationRateTableData.getProductForm());
   }

   @Test
   public void testgetProductName() {
      applicationRateTableData.setProductName(product.getName());
      Assert.assertEquals(product.getName(), applicationRateTableData.getProductName());
   }

   @Test
   public void testsetProductName() {
      applicationRateTableData.setProductName(product.getName());
      Assert.assertEquals(product.getName(), applicationRateTableData.getProductName());
   }

   @Test
   public void testgetDefaultRate() {
      applicationRateTableData.setDefaultRate(4512.0502);
      Assert.assertTrue(applicationRateTableData.getDefaultRate() == 4512.0502);
   }

   @Test
   public void testsetDefaultRate() {
      applicationRateTableData.setDefaultRate(2000.0502);
      Assert.assertTrue(applicationRateTableData.getDefaultRate() == 2000.0502);
   }

   @Test
   public void testgetRate2() {
      applicationRateTableData.setRate2(1005.0502);
      Assert.assertTrue(applicationRateTableData.getRate2() == 1005.0502);
   }

   @Test
   public void testsetRate2() {
      applicationRateTableData.setRate2(1200.0502);
      Assert.assertTrue(applicationRateTableData.getRate2() == 1200.0502);
   }

   @Test
   public void testsetUnit() {
      applicationRateTableData.setUnit(product.getRateDisplayUnitsMetric());
      Assert.assertEquals(product.getRateDisplayUnitsMetric(), applicationRateTableData.getUnit());
   }

   @Test
   public void testgetUnit() {
      applicationRateTableData.setUnit(product.getRateDisplayUnitsMetric());
      Assert.assertEquals(product.getRateDisplayUnitsMetric(), applicationRateTableData.getUnit());
   }
}
