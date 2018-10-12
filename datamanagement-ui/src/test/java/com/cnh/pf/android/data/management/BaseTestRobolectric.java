/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management;

import android.content.Context;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/**
 * Base Robolectric class, uses TestApp as application
 * and custom RobolectricMavenTestRunner
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
@SuppressWarnings("squid:S2187")
public class BaseTestRobolectric {

   protected Context getAppContext() {
      return RuntimeEnvironment.application;
   }
}
