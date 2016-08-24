/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.app.Application;

/**
 * Test application without the roboguice phoenix workaround or stetho
 * @author kedzie
 */
public class TestApp extends Application {

   @Override
   public void onCreate() {
      super.onCreate();
   }
}
