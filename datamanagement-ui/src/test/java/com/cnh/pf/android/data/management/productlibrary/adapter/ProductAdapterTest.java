/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.adapter;

import static org.junit.Assert.*;

import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.android.pf.widget.utilities.MeasurementSystemCache;
import com.cnh.pf.android.data.management.TestApp;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;
import android.app.Activity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.pf.api.pvip.IPVIPServiceAIDL;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.pf.model.product.library.ProductUnits;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.vip.vehimp.Implement;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductForm;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.List;


import static org.mockito.Mockito.*;
import static org.robolectric.RuntimeEnvironment.application;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class ProductAdapterTest {
    private TabActivity tabActivity;
    private IVIPServiceAIDL vipService;
    private IPVIPServiceAIDL pvipService;
    private ProductAdapter productAdapter;
    private List<ProductUnits> productUnits = null;
    private ProductUnits productUnit = null;
    private List<Product> products = null;
    private Product product1 = null;
    private Implement currentImplement;
    private ProductLibraryFragment productLibraryFragment;
    private MeasurementSystemCache measurementSystemCache;

    @org.junit.Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        pvipService = mock(IPVIPServiceAIDL.class);
        vipService = mock(IVIPServiceAIDL.class);
        tabActivity = mock(TabActivity.class);
        currentImplement = mock(Implement.class);
        productLibraryFragment = mock(ProductLibraryFragment.class);
        measurementSystemCache = mock(MeasurementSystemCache.class);
        productUnits = new ArrayList<ProductUnits>();
        products = new ArrayList<Product>();

        productUnit = new ProductUnits();
        productUnit.setId(1);
        productUnit.setName("ProductUnit A");
        productUnits.add(productUnit);


        product1 = new Product();
        product1.setId(1);
        product1.setName("Product A");
        products.add(product1);
        productAdapter = new ProductAdapter(application, products, tabActivity, vipService, pvipService, productLibraryFragment, productUnits, currentImplement, measurementSystemCache);
    }

    @org.junit.Test
    public void testObjectsNotNull() {
        Assert.assertNotNull(vipService);
        Assert.assertNotNull(tabActivity);
        Assert.assertNotNull(productUnits);
        Assert.assertNotNull(product1);
        Assert.assertNotNull(pvipService);
        Assert.assertNotNull(currentImplement);
    }

    @org.junit.Test
    public void setVIPService() {
        productAdapter.setVIPService(vipService);
        Assert.assertEquals(vipService, productAdapter.getVipService());
    }

    @org.junit.Test
    public void setPVIPService() {
        productAdapter.setPVIPService(pvipService);
        Assert.assertEquals(pvipService, productAdapter.getPvipService());
    }

    @org.junit.Test
    public void setProductUnits() {
        productAdapter.setProductUnits(productUnits);
        Assert.assertEquals(productUnits, productAdapter.getProductUnits());

    }

    @org.junit.Test
    public void setCurrentImplement() {
        productAdapter.setCurrentImplement(currentImplement);
        Assert.assertEquals(currentImplement, productAdapter.getCurrentImplement());
    }

}
