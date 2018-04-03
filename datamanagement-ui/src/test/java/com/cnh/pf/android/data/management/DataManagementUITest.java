/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.view.View;

import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.android.util.prefs.GlobalPreferencesNotAvailableException;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickList;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.adapter.SelectionTreeViewAdapter;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.google.common.net.HostAndPort;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import roboguice.RoboGuice;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;

/**
 * Test UI elements reflecting events sent from backend
 * @author oscar.salazar@cnhind.com
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class DataManagementUITest {
   ActivityController<DataManagementActivity> controller;
   DataManagementActivity activity;
   EventManager eventManager;
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;
   BaseDataFragment fragment;
   ObjectGraph customer;
   ObjectGraph testObject;

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new MyTestModule());
      eventManager = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(Key.get(EventManager.class, Names.named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)));
      controller = Robolectric.buildActivity(DataManagementActivity.class);
      activity = controller.get();
      when(binder.getService()).thenReturn(service);
      when(service.getMediums()).thenReturn(Arrays.asList(new MediumDevice(Datasource.Source.USB, RuntimeEnvironment.application.getFilesDir())));
      when(service.processOperation(Matchers.any(DataManagementSession.class), Matchers.any(DataManagementSession.SessionOperation.class)))
            .then(new Answer<DataManagementSession>() {
               @Override
               public DataManagementSession answer(InvocationOnMock invocation) throws Throwable {
                  DataManagementSession session = (DataManagementSession) invocation.getArguments()[0];
                  DataManagementSession.SessionOperation op = (DataManagementSession.SessionOperation) invocation.getArguments()[1];
                  session.setSessionOperation(op);
                  return session;
               }
            });
      shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()),
            binder);
   }

   @After
   public void tearDown() {
      RoboGuice.Util.reset();
   }

   @Test
   public void testParseXml() throws IOException, XmlPullParserException {
      FormatManager parser = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(FormatManager.class);
      parser.parseXml();
      Set<String> formats = parser.getFormats();
      assertTrue("xml specifies isoxml format", formats.contains("ISOXML"));
      assertTrue("isoxml supporrts customery type", parser.formatSupportsType("ISOXML", DataTypes.GROWER));
      assertFalse("isoxml does not support VEHICLE type", parser.formatSupportsType("ISOXML", "VEHICLE"));
      assertTrue("xml specifies cnh format", formats.contains("PF Database"));
      assertTrue("cnh supports vehicle type", parser.formatSupportsType("PF Database", "VEHICLE"));
      assertTrue("cnh does not support customer type", parser.formatSupportsType("PF Database", DataTypes.GROWER));
      assertFalse("cnh does not support prescription type", parser.formatSupportsType("PF Database", DataTypes.RX));
   }

   @Test
   public void testISOSupport() throws RemoteException {
      //Initialize export fragment
      activateTab(2);
      //Check export fragment visible
      assertTrue("export drop zone is visible", activity.findViewById(R.id.export_drop_zone).getVisibility() == View.VISIBLE);
      ExportFragment fragment = (ExportFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Export");
      assertTrue("export fragment is visible", fragment != null);
      //Start new discovery
      DataManagementSession session = new DataManagementSession(new Datasource.Source[] { Datasource.Source.INTERNAL }, new Datasource.Source[] { Datasource.Source.INTERNAL },
            null, null);
      session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
      session.setObjectData(getTestObjectData());
      session.setFormat("ISOXML");
      fragment.setSession(session);
      fireDiscoveryEvent(fragment, session);
      //Assert tree view shows results of discovery
      assertEquals("Object Tree View is visible", View.VISIBLE, fragment.getView().findViewById(R.id.tree_view_list).getVisibility());
      //Mock picklist, select ISOXML as export format$
      fragment.exportFormatPicklist = mock(PickList.class);
      when(fragment.exportFormatPicklist.getSelectedItemValue()).thenReturn("ISOXML");
      //Select non-supported format from tree to export, check to make sure its supported state is false
      ObjectTreeViewAdapter adapter = (ObjectTreeViewAdapter) fragment.treeViewList.getAdapter();
      assertTrue("isoxml does not support com.cnh.prescription.Shapefile type", !adapter.isSupportedEntitiy(testObject));
      //Now check supported format
      assertTrue("isoxml supports customer type", adapter.isSupportedEntitiy(customer));
   }

   /** Test to make sure that the data sent to destination datasource only has supported types */
   @Test
   public void testRecursiveFormatSupport() throws RemoteException {
      //Initialize export fragment
      activateTab(2); //0 - Import 1 - Export
      //Mock picklist, select ISOXML as export format$
      ExportFragment fragment = (ExportFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Export");
      DataManagementSession session = new DataManagementSession(new Datasource.Source[] { Datasource.Source.INTERNAL }, new Datasource.Source[] { Datasource.Source.INTERNAL },
            null, null);
      session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
      session.setObjectData(getTestObjectData());
      session.setFormat("ISOXML");
      fragment.setSession(session);
      fireDiscoveryEvent(fragment, session);
      fragment.exportFormatPicklist = mock(PickList.class);
      when(fragment.exportFormatPicklist.getSelectedItemValue()).thenReturn("ISOXML");
      //Set selected item to top of tree, will import everything recursive
      ObjectTreeViewAdapter adapter = (ObjectTreeViewAdapter) fragment.treeViewList.getAdapter();
      adapter.getSelectionMap().put(testObject, SelectionTreeViewAdapter.SelectionType.FULL);
      adapter.getSelectionMap().put(testObject.getParent(), SelectionTreeViewAdapter.SelectionType.FULL); // Field
      adapter.getSelectionMap().put(testObject.getParent().getParent(), SelectionTreeViewAdapter.SelectionType.FULL); // Farm
      adapter.getSelectionMap().put(testObject.getParent().getParent().getParent(), SelectionTreeViewAdapter.SelectionType.FULL); // Customer
      Set<ObjectGraph> exportEntities = adapter.getSelected();
      assertTrue("exported list does not contain unsupported types", !exportEntities.contains(testObject));
      assertTrue("exported list contains supported type", exportEntities.contains(customer));
   }

   private void activateTab(int tabPosition) {
      //Start activity
      controller.create().start().resume();
      //Select Tab at position
      ((TabActivity) activity).selectTabAtPosition(tabPosition);
   }

   private void fireDiscoveryEvent(BaseDataFragment fragment, DataManagementSession session) throws RemoteException {
      //Start new discovery
      fragment.onDataSessionUpdated(session);
   }

   /* Generate Object tree for testing */
   private List<ObjectGraph> getTestObjectData() {
      customer = new ObjectGraph(null, DataTypes.GROWER, "Oscar");
      ObjectGraph farm = new ObjectGraph(null, DataTypes.FARM, "Dekalp");
      ObjectGraph field = new ObjectGraph(null, DataTypes.FIELD, "North");
      ObjectGraph task = new ObjectGraph(null, DataTypes.TASK, "Task1");
      ObjectGraph task2 = new ObjectGraph(null, DataTypes.TASK, "Task2");
      testObject = new ObjectGraph(null, "com.cnh.prescription.shapefile", "Shapefile Prescription"); // Not real object, used to demostrate format support
      field.addChild(testObject);
      field.addChild(task);
      field.addChild(task2);
      farm.addChild(field);
      customer.addChild(farm);
      return (new ArrayList<ObjectGraph>() {
         {
            add(customer);
         }
      });
   }

   public class MyTestModule extends AbstractModule {

      @Override
      protected void configure() {
         //         bind(DataManagementService.class).toInstance(service);
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
}
