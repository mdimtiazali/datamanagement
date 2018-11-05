/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.dialog;

import android.widget.Button;
import android.widget.TextView;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import com.cnh.pf.android.data.management.graph.GFFObject;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/* GFFSelectionViewTest Handles UnitTests for GFFSelectionView
 *@author Pallab Datta
 * */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class GFFSelectionViewTest {
   private GFFSelectionView mGFFSelectionView;
   List<GFFObject> obj = new LinkedList<GFFObject>();

   @Before
   public void setUp() throws Exception {
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

   }

   @Test
   public void ConstructorTest() {
      mGFFSelectionView = new GFFSelectionView(RuntimeEnvironment.application, obj, "DataManagement");
      assertNotNull(mGFFSelectionView);
      TextView mtextView = (TextView) mGFFSelectionView.findViewById(R.id.header);
      assertTrue(mtextView.isEnabled());

   }

   @Test
   public void GFFSelectionViewTest() {
      mGFFSelectionView = new GFFSelectionView(RuntimeEnvironment.application, obj, "UnitTest");
      Button button1 = (Button) mGFFSelectionView.findViewById(R.id.btFirst);
      assertFalse(button1.isEnabled());
      assertThat(button1.getText().toString(), is("Next"));
      Button button2 = (Button) mGFFSelectionView.findViewById(R.id.btSecond);
      assertTrue(button2.isEnabled());
      assertThat(button2.getText().toString(), is("Cancel"));
   }

   @Test
   public void GFFSelectionViewTitleTest() {
      mGFFSelectionView = new GFFSelectionView(RuntimeEnvironment.application, obj, "UnitTest");
      TextView title = (TextView) mGFFSelectionView.findViewById(R.id.tvTitle);
      assertThat(title.getText().toString(), Matchers.equalToIgnoringWhiteSpace("copy"));
   }

   @Test
   public void getSelectedTest() {
      mGFFSelectionView = new GFFSelectionView(RuntimeEnvironment.application, getGFFTDataList(), "UnitTest");
      mGFFSelectionView.setSelectedPosition(0);
      GFFObject gffObject = mGFFSelectionView.getSelected();
      assertNotNull(gffObject);
      assertEquals("testName", gffObject.getName());
      assertEquals("Testtype1", gffObject.getType());
      mGFFSelectionView.setSelectedPosition(1);
      GFFObject gffObject1 = mGFFSelectionView.getSelected();
      assertEquals("testName2", gffObject1.getName());
      assertEquals("Testtype2", gffObject1.getType());
      mGFFSelectionView.setSelectedPosition(2);
      GFFObject gffObject3 = mGFFSelectionView.getSelected();
      assertEquals("testName3", gffObject3.getName());
      assertEquals("Testtype3", gffObject3.getType());
   }

   private List<GFFObject> getGFFTDataList() {
      List<GFFObject> gffObjectList = new ArrayList<GFFObject>();
      GFFObject gffObject = new GFFObject("testName", "Testtype1", new ObjectGraph());
      gffObjectList.add(gffObject);
      gffObject = new GFFObject("testName2", "Testtype2", new ObjectGraph());
      gffObjectList.add(gffObject);
      gffObject = new GFFObject("testName3", "Testtype3", new ObjectGraph());
      gffObjectList.add(gffObject);
      return gffObjectList;
   }
}



