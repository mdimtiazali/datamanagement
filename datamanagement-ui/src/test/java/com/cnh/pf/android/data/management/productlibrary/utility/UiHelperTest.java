/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.utility;

import android.view.View;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.*;
import static org.robolectric.RuntimeEnvironment.application;

/**
 *UiHelperTest Handles all UnitTests for UiHelper.
 *@auther Himanshu Ranawat
 */

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class UiHelperTest {

   private UiHelper uiHelper;
   private View view;

   @Before
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);
      uiHelper = new UiHelper();
      view = mock(View.class);
      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();
   }

   @After
   public void tearDown() throws Exception {
      uiHelper = null;
      view = null;
   }

   @Test
   public void setAlternatingTableItemBackgroundOdd() {
      uiHelper.setAlternatingTableItemBackground(application, 3, view);
      verify(view).setBackgroundColor(application.getResources().getColor(R.color.odd_rows));
   }

   @Test
   public void setAlternatingTableItemBackgroundEven() {
      uiHelper.setAlternatingTableItemBackground(application, 2, view);
      verify(view).setBackgroundColor(application.getResources().getColor(R.color.even_rows));
   }
}