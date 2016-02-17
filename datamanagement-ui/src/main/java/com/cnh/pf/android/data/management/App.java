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
import com.cnh.pf.jgroups.ChannelModule;
import com.facebook.stetho.Stetho;
import roboguice.RoboGuiceHelper;

/**
 * @author kedzie
 */
public class App extends Application {

   @Override
   public void onCreate() {
      super.onCreate();
      RoboGuiceHelper.help(this, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" },
            new RoboModule(this), new ChannelModule(this));
      Stetho.initializeWithDefaults(this);
   }
}
