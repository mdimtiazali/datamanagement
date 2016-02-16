/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.View;

import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.adapter.SelectionTreeViewAdapter;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.data.management.DataManagementSession;

import com.cnh.pf.data.management.aidl.MediumDevice;
import com.google.common.collect.Collections2;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.bouncycastle.jce.provider.symmetric.ARC4;
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
import org.xmlpull.v1.XmlPullParserException;
import roboguice.RoboGuice;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

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
   @Mock DataManagementService service;
   @Mock DataManagementService.LocalBinder binder;
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
      DataManagementSession session = new DataManagementSession(new Datasource.Source[] { Datasource.Source.INTERNAL}, new ArrayList<MediumDevice>(0), new Datasource.Source[] {Datasource.Source.INTERNAL});
      session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
      session.setObjectData(new ArrayList<ObjectGraph>());
      eventManager.fire(new DataServiceConnectionImpl.DataSessionEvent(session));
      assertEquals("Empty discovery dialog is visible", View.VISIBLE, activity.findViewById(R.id.empty_discovery_text).getVisibility());
   }

   @Test
   public void testParseXml() throws IOException, XmlPullParserException {
      FormatManager parser = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(FormatManager.class);
      parser.parseXml();
      Set<String> formats = parser.getFormats();
      assertTrue("xml specifies isoxml format", formats.contains("ISOXML"));
      assertTrue("isoxml supporrts customery type", parser.formatSupportsType("ISOXML", "com.cnh.pf.model.pfds.Customer"));
      assertFalse("isoxml does not support VEHICLE type", parser.formatSupportsType("ISOXML", "VEHICLE"));
      assertTrue("xml specifies cnh format", formats.contains("CNH"));
      assertTrue("cnh supports vehicle type", parser.formatSupportsType("CNH", "VEHICLE"));
      assertFalse("cnh does not support customer type", parser.formatSupportsType("CNH", "com.cnh.pf.model.pfds.Customer"));
   }

   @Test
   public void testISOSupport() {
      //Initialize export fragment
      activateTab(1);
      //Check export fragment visible
      assertTrue("export drop zone is visible", activity.findViewById(R.id.export_drop_zone).getVisibility() == View.VISIBLE);
      ExportFragment fragment = (ExportFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Export");
      assertTrue("export fragment is visible", fragment != null);
      //Start new discovery
      fireDiscoveryEvent();
      //Assert tree view shows results of discovery
      assertEquals("Object Tree View is visible", View.VISIBLE, fragment.getView().findViewById(R.id.tree_view_list).getVisibility());
      //Mock picklist, select ISOXML as export format$
      fragment.exportFormatPicklist = mock(PickListEditable.class);
      when(fragment.exportFormatPicklist.getSelectedItemValue()).thenReturn("ISOXML");
      //Select non-supported format from tree to export, check to make sure its supported state is false
      ObjectTreeViewAdapter adapter = (ObjectTreeViewAdapter) fragment.treeViewList.getAdapter();
      assertTrue("isoxml does not support com.cnh.prescription.Shapefile type", !adapter.isSupportedEntitiy(testObject));
      //Now check supported format
      assertTrue("isoxml supports customer type", adapter.isSupportedEntitiy(customer));
   }

   /** Test to make sure that the data sent to destination datasource only has supported types */
   @Test
   public void testRecursiveFormatSupport() {
      //Initialize export fragment
      activateTab(1);  //0 - Import 1 - Export
      //Mock picklist, select ISOXML as export format$
      ExportFragment fragment = (ExportFragment) ((TabActivity) activity).getFragmentManager().findFragmentByTag("Export");
      fireDiscoveryEvent();
      fragment.exportFormatPicklist = mock(PickListEditable.class);
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

   private void fireDiscoveryEvent() {
      //Start new discovery
      DataManagementSession session = new DataManagementSession(new Datasource.Source[] { Datasource.Source.INTERNAL }, (List<MediumDevice>)null, new Datasource.Source[] { Datasource.Source.INTERNAL});
      session.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
      session.setObjectData(getTestObjectData());
      eventManager.fire(new DataServiceConnectionImpl.DataSessionEvent(session));
   }

   /* Generate Object tree for testing */
   private List<ObjectGraph> getTestObjectData() {
      customer = new ObjectGraph(null, "com.cnh.pf.model.pfds.Customer", "Oscar");
      ObjectGraph farm = new ObjectGraph(null, "com.cnh.pf.model.pfds.Farm", "Dekalp");
      ObjectGraph field = new ObjectGraph(null, "com.cnh.pf.model.pfds.Field", "North");
      ObjectGraph task = new ObjectGraph(null, "com.cnh.pf.model.pfds.Task", "Task1");
      ObjectGraph task2 = new ObjectGraph(null, "com.cnh.pf.model.pfds.Task", "Task2");
      testObject = new ObjectGraph(null, "com.cnh.prescription.shapefile", "Shapefile Prescription"); // Not real object, used to demostrate format support
      field.addChild(testObject);
      field.addChild(task);
      field.addChild(task2);
      farm.addChild(field);
      customer.addChild(farm);
      return (new ArrayList<ObjectGraph>() {{add(customer);}});
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
   }
}
