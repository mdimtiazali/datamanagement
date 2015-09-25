/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.View;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.data.management.DataManagementSession;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import roboguice.RoboGuice;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test UI elements reflecting events sent from backend
 * @author oscar.salazar@cnhind.com
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")
public class DataManagementUITest {
   ActivityController<DataManagementActivity> controller;
   DataManagementActivity activity;
   EventManager eventManager;
   @Mock DataManagementService service;
   @Mock DataManagementService.LocalBinder binder;

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new MyTestModule());
      eventManager = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(Key.get(EventManager.class, Names.named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)));
      controller = Robolectric.buildActivity(DataManagementActivity.class);
      activity = controller.get();
      when(binder.getService()).thenReturn(service);
      shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()), binder);
   }

   @After
   public void tearDown() {
      RoboGuice.Util.reset();
   }

   /** Test whether the no data found dialog comes up when no discovery*/
   @Test
   public void testEmptyDiscovery() {
      controller.create().start().resume();
      DataManagementSession session = new DataManagementSession(Datasource.Source.INTERNAL, Datasource.Source.INTERNAL, null);
      session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
      session.setObjectData(new ArrayList<ObjectGraph>());
      eventManager.fire(new DataServiceConnectionImpl.DataSessionEvent(session));
      assertEquals(View.VISIBLE, activity.findViewById(R.id.empty_discovery_text).getVisibility());
   }

   public class MyTestModule extends AbstractModule {

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
   }
}
