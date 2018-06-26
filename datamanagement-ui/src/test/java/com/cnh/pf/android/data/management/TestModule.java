/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.android.util.prefs.GlobalPreferencesNotAvailableException;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.helper.DmAccessibleObserver;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test module to be used in the unit test for dependency injection
 */
public class TestModule extends AbstractModule {
   @Override
   protected void configure() {
      bind(Mediator.class).toInstance(mock(Mediator.class));
   }

   @Provides
   @Singleton
   @Named("global")
   @SuppressWarnings("deprecation")
   private SharedPreferences getPrefs() throws PackageManager.NameNotFoundException {
      return null;
   }

   @Provides
   @Singleton
   public DmAccessibleObserver getDmAccessibleObserver(){
      DmAccessibleObserver observer = mock(DmAccessibleObserver.class);
      doNothing().when(observer).start();
      doNothing().when(observer).stop();
      return observer;
   }

   @Provides
   @Singleton
   public GlobalPreferences getGlobalPreferences(Context context) throws GlobalPreferencesNotAvailableException {
      GlobalPreferences prefs = mock(GlobalPreferences.class);
      when(prefs.hasPCM()).thenReturn(true);
      return prefs;
   }

   @Provides
   @Singleton
   @Named("daemon")
   public HostAndPort getDaemon() {
      return HostAndPort.fromParts("127.0.0.1", 14000);
   }
}