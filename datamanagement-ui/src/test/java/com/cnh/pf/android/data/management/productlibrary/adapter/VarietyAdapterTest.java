/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.adapter;

import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.pf.api.pvip.IPVIPServiceAIDL;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import com.cnh.pf.android.data.management.productlibrary.views.AddOrEditVarietyDialog;
import com.cnh.pf.model.product.configuration.Variety;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.robolectric.RuntimeEnvironment.application;

/**
 *VarietyAdapterTest Handles all UnitTests for VarietyAdapter.
 *@auther Himanshu Ranawat
 */

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class VarietyAdapterTest {

   private VarietyAdapter varietyAdatpter;
   private IVIPServiceAIDL vipservice;
   private IPVIPServiceAIDL pvipService;
   private TabActivity tabActivity;
   private List<Variety> varieties = null;
   private Variety varietyA;
   private Variety varietyB;
   private AddOrEditVarietyDialog editVarietyDialogmock;

   @Before
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
      vipservice = mock(IVIPServiceAIDL.class);
      pvipService = mock(IPVIPServiceAIDL.class);
      tabActivity = mock(TabActivity.class);
      varieties = new ArrayList<Variety>();
      varietyA = new Variety();
      varietyA.setId(1);
      varietyA.setName("Variety A");
      varietyA.setUsed(true);
      varietyB = new Variety();
      varietyB.setId(2);
      varietyB.setName("Variety B");
      varietyB.setUsed(true);
      varieties.add(varietyA);
      varieties.add(varietyB);
      varietyAdatpter = new VarietyAdapter(RuntimeEnvironment.application,varieties,tabActivity,vipservice,pvipService);
      editVarietyDialogmock = mock(AddOrEditVarietyDialog.class);
      varietyAdatpter.setEditVarietyDialog(editVarietyDialogmock);

      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();
   }

   @After
   public void tearDown() throws Exception {
      vipservice = null;
      pvipService = null;
      tabActivity = null;
      varieties = null;
      varietyA = null;
      varietyB = null;
      editVarietyDialogmock = null;
   }

   @Test
   public void testObjectsNotNull(){
      Assert.assertNotNull(vipservice);
      Assert.assertNotNull(pvipService);
      Assert.assertNotNull(tabActivity);
      Assert.assertNotNull(varieties);
      Assert.assertNotNull(varietyA);
      Assert.assertNotNull(varietyB);
      Assert.assertNotNull(editVarietyDialogmock);
   }

   @Test
   public void testsetVIPService() {
      varietyAdatpter.setVIPService(vipservice);
      Assert.assertEquals(vipservice,varietyAdatpter.getVipService());
      verify(editVarietyDialogmock).setVIPService(varietyAdatpter.getVipService());
   }

   @Test
   public void testsetVarietyList() {
      varietyAdatpter.setVarietyList(varieties);
      Assert.assertEquals(varieties,varietyAdatpter.getFilteredList());
      Assert.assertNull(varietyAdatpter.getOriginalList());
   }
}
