/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.views;

import android.view.View;
import com.cnh.android.widget.control.InputField;
import com.cnh.android.widget.control.SegmentedToggleButtonGroup;
import com.cnh.pf.android.data.management.R;
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
import static org.mockito.Mockito.mock;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class AdvancedProductInformationTest {

   private AdvancedProductInformation mAdvancedProductInformation;
   private View mView;

   @Before
   public void setUp() throws Exception {
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
      mView = mock(View.class);
      mAdvancedProductInformation = new AdvancedProductInformation(RuntimeEnvironment.application);

   }

   @Test
   public void constructorTest() {
      mAdvancedProductInformation = new AdvancedProductInformation(RuntimeEnvironment.application);
      assertNotNull(mAdvancedProductInformation);
   }

   @Test
   public void UnitsetTest() {
      InputField epaNumberInputField = (InputField) mView.findViewById(R.id.epa_number_input_field);
      assertNull(epaNumberInputField);
      InputField manufacturerInputField = (InputField) mView.findViewById(R.id.manufacturer_input_field);
      assertNull(manufacturerInputField);
      InputField bufferDistanceInputField = (InputField) mView.findViewById(R.id.buffer_distance_input_field);
      assertNull(bufferDistanceInputField);
      InputField maxWindSpeedInputField = (InputField) mView.findViewById(R.id.max_wind_speed_input_field);
      assertNull(maxWindSpeedInputField);
      SegmentedToggleButtonGroup toggleButtonGroup1 = (SegmentedToggleButtonGroup) mView.findViewById(R.id.restricted_use_segmented_button_toggle_group);
      assertNull(toggleButtonGroup1);
      SegmentedToggleButtonGroup toggleButtonGroup2 = (SegmentedToggleButtonGroup) mView.findViewById(R.id.posting_required_segmented_button_toggle_group);
      assertNull(toggleButtonGroup2);
   }

   @After
   public void tearDown() throws Exception {
      mAdvancedProductInformation = null;
      assertNull(mAdvancedProductInformation);
   }
}