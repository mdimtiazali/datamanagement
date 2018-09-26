/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import com.cnh.android.widget.control.PickList;
import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.adapter.SelectionTreeViewAdapter;
import com.cnh.pf.android.data.management.dialog.ImportSourceDialog;
import com.cnh.pf.android.data.management.misc.IconizedFile;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.productlibrary.views.AddOrEditVarietyDialog;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.model.pfds.Customer;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.library.CropType;
import com.cnh.pf.model.product.library.Product;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import roboguice.RoboGuice;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.cnh.pf.android.data.management.session.Session.Type.DISCOVERY;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Test UI elements reflecting events sent from backend
 * @author oscar.salazar@cnhind.com
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class DataManagementUITest {
   private static final int MANAGE_SOURCE_TAB_POSITION = 0;
   private static final int IMPORT_SOURCE_TAB_POSITION = 1;
   private static final int EXPORT_SOURCE_TAB_POSITION = 2;
   private static final int PRODUCT_LIBRARY_TAB_POSITION = 3;

   private ActivityController<DataManagementActivity> controller;
   private DataManagementActivity activity;
   private EventManager eventManager;
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;

   private ObjectGraph customer;
   private ObjectGraph testObject;

   private ImportSourceDialog importSourceDialog;
   private SessionExtra currentSessionExtra;
   private final List<SessionExtra> mockSessionExtraList = new ArrayList<SessionExtra>();
   private final SessionExtra sessionExtra = new SessionExtra(SessionExtra.USB, "USB", 0);

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
      eventManager = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(Key.get(EventManager.class, Names.named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)));
      controller = Robolectric.buildActivity(DataManagementActivity.class);
      activity = controller.get();
      when(binder.getService()).thenReturn(service);
      shadowOf(RuntimeEnvironment.application)
         .setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()), binder);
      currentSessionExtra = createCurrentSessionExtra();
      List<SessionExtra> sessionExtraList = createSessionExtraList();
      importSourceDialog = createImportSourceDialog();
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
   }

   @Test
   public void testDiscoveryComplete() {
      activateTab(MANAGE_SOURCE_TAB_POSITION);
      final ManageFragment manageFragment = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");

      final Session session = new Session();
      session.setType(Session.Type.DISCOVERY);
      session.setAction(Session.Action.MANAGE);
      doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) {
            session.setObjectData(getTestObjectData());
            manageFragment.onMyselfSessionSuccess(session);
            return null;
         }
      }).when(service).processSession(any(Session.class));

      service.processSession(session);
      assertThat(manageFragment.getTreeAdapter(), is(notNullValue()));
      assertThat(manageFragment.getTreeAdapter().getCount() > 0, is(true));
   }

   @Test
   public void testISOSupport() {
      //Initialize exportFragment fragment
      activateTab(EXPORT_SOURCE_TAB_POSITION);
      //Check exportFragment fragment visible
      assertThat("exportFragment drop zone is visible", activity.findViewById(R.id.export_drop_zone).getVisibility(), is(View.VISIBLE));
      ExportFragment fragment = (ExportFragment) activity.getFragmentManager().findFragmentByTag("Export");
      assertThat("exportFragment fragment is visible", fragment, notNullValue());
      //Start new discovery
      Session session = new Session();
      session.setType(DISCOVERY);
      session.setObjectData(getTestObjectData());
      SessionExtra exportExtra = new SessionExtra(SessionExtra.USB, "test extra", 1);
      exportExtra.setFormat("ISOXML");
      session.setExtra(exportExtra);
      fireDiscoveryEvent(fragment, session);
      //Assert tree exportFragment shows results of discovery
      assertEquals("Object Tree View is visible", View.VISIBLE, fragment.getView().findViewById(R.id.tree_view_list).getVisibility());
      //Mock picklist, select ISOXML as exportFragment format$
      fragment.exportFormatPicklist = mock(PickList.class);
      when(fragment.exportFormatPicklist.getSelectedItemValue()).thenReturn("ISOXML");
      //Select non-supported format from tree to exportFragment, check to make sure its supported state is false
      ObjectTreeViewAdapter adapter = (ObjectTreeViewAdapter) fragment.treeViewList.getAdapter();
      assertTrue("isoxml does not support com.cnh.prescription.Shapefile type", !adapter.isSupportedEntity(testObject));
      //Now check supported format
      assertTrue("isoxml supports customer type", adapter.isSupportedEntity(customer));
   }

   /** Test to make sure that the data sent to destination datasource only has supported types */
   @Test
   public void testRecursiveFormatSupport() {
      //Initialize exportFragment fragment
      activateTab(EXPORT_SOURCE_TAB_POSITION);
      //Mock picklist, select ISOXML as exportFragment format$
      ExportFragment fragment = (ExportFragment) activity.getFragmentManager().findFragmentByTag("Export");
      Session session = new Session();
      session.setType(DISCOVERY);
      session.setObjectData(getTestObjectData());
      SessionExtra exportExtra = new SessionExtra(SessionExtra.USB, "test extra", 1);
      exportExtra.setFormat("ISOXML");
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
      activity.selectTabAtPosition(tabPosition);
   }

   private void fireDiscoveryEvent(BaseDataFragment fragment, Session session) {
      //Start new discovery
      fragment.onMyselfSessionSuccess(session);
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

   @Test
   public void testForVarietiesCropTypeWhenIsUsed() {
      activateTab(PRODUCT_LIBRARY_TAB_POSITION);
      AddOrEditVarietyDialog addOrEditVarietyDialog = new AddOrEditVarietyDialog(activity);
      LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      Variety TEST_VARIETY = new Variety();
      TEST_VARIETY.setName("Mock Variety");
      TEST_VARIETY.setCropType(CropType.CARROTS);
      TEST_VARIETY.setUsed(true);
      addOrEditVarietyDialog.setCurrentVariety(TEST_VARIETY);
      View editView = layoutInflater.inflate(R.layout.variety_add_or_edit_dialog, null);
      PickList pickList = (PickList) editView.findViewById(R.id.variety_dialog_crop_type_pick_list);
      assertTrue(!pickList.isPickListEditable());
   }

   private SessionExtra createCurrentSessionExtra() {
      SessionExtra mockSessionExtra = spy(sessionExtra);
      when(mockSessionExtra.isUsbExtra()).thenReturn(true);
      return mockSessionExtra;
   }

   private ImportSourceDialog createImportSourceDialog() {
      ImportSourceDialog mockImportSource = mock(ImportSourceDialog.class);
      File mockFile = new File("USB");
      IconizedFile mockUsbRootFile = new IconizedFile(mockFile, TreeEntityHelper.getIcon("USB"));
      mockImportSource.usbImportSource(mockUsbRootFile, 0, createCurrentSessionExtra());
      when(mockImportSource.getCurrentExtra()).thenReturn(SessionExtra.USB);
      when(mockImportSource.checkUSBType()).thenReturn(true);
      return mockImportSource;
   }

   private List<SessionExtra> createSessionExtraList() {
      mockSessionExtraList.add(sessionExtra);
      return mockSessionExtraList;
   }

   @Test
   public void testForNoImportSource() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View view = layoutInflater.inflate(R.layout.no_device_layout, null);
      assertEquals("No Import Source", View.VISIBLE, view.findViewById(R.id.no_import_source_view).getVisibility());
   }

   @Test
   public void testForUSBImportSource() {
      //checking for current source and current source type
      assertEquals("Checking currentExtra", importSourceDialog.getCurrentExtra(), SessionExtra.USB);
      assertEquals("The currentExtra type is USB", importSourceDialog.checkUSBType(), currentSessionExtra.isUsbExtra());
      //verifying method calls in the ImportSourceDialog class
      verify(importSourceDialog).getCurrentExtra();
      verify(importSourceDialog).checkUSBType();
   }

   @Test
   public void testDynamicUITree() {
      activateTab(MANAGE_SOURCE_TAB_POSITION);
      final ManageFragment manageFragment = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");
      BaseDataFragment.setDsPerfFlag(true);
      final Session session = new Session();
      session.setType(Session.Type.DISCOVERY);
      session.setAction(Session.Action.MANAGE);
      doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) {
            session.setObjectData(getAllKindOfData());
            manageFragment.onMyselfSessionSuccess(session);
            return null;
         }
      }).when(service).processSession(any(Session.class));

      service.processSession(session);
      assertThat(manageFragment.getTreeAdapter(), is(notNullValue()));
      assertThat(manageFragment.getTreeAdapter().getCount() > 0, is(true));
   }

   public static List<ObjectGraph> getAllKindOfData() {
      List<ObjectGraph> graphList = new ArrayList<ObjectGraph>();
      graphList.addAll(getTestObjectGraphMap(Customer.class).values());
      graphList.addAll(getTestObjectGraphMap(Product.class).values());
      graphList.addAll(getTestObjectGraphMap(Variety.class).values());
      return graphList;
   }

   public static <T> Map<T, ObjectGraph> getTestObjectGraphMap(Class<T> type) {
      Map<T, ObjectGraph> map = new HashMap<T, ObjectGraph>();
      if (type.equals(Customer.class)) {
         ObjectGraph cust = new ObjectGraph(null, DataTypes.GROWER, "Oscar");
         cust.setId(UUID.randomUUID().toString());
         ObjectGraph farm = new ObjectGraph(null, DataTypes.FARM, "Dekalp");
         farm.setId(UUID.randomUUID().toString());
         ObjectGraph field = new ObjectGraph(null, DataTypes.FIELD, "North");
         field.setId(UUID.randomUUID().toString());
         ObjectGraph task = new ObjectGraph(null, DataTypes.TASK, "Task1");
         task.setId(UUID.randomUUID().toString());
         ObjectGraph task2 = new ObjectGraph(null, DataTypes.TASK, "Task2");
         task2.setId(UUID.randomUUID().toString());

         ObjectGraph prescription1 = new ObjectGraph(null, DataTypes.RX, "Prescription1");
         prescription1.setId(UUID.randomUUID().toString());
         ObjectGraph prescription2 = new ObjectGraph(null, DataTypes.RX, "Prescription2");
         prescription2.setId(UUID.randomUUID().toString());

         ObjectGraph boundary1 = new ObjectGraph(null, DataTypes.BOUNDARY, "Boundary1");
         boundary1.setId(UUID.randomUUID().toString());
         ObjectGraph boundary2 = new ObjectGraph(null, DataTypes.BOUNDARY, "Boundary2");
         boundary2.setId(UUID.randomUUID().toString());

         ObjectGraph landmark1 = new ObjectGraph(null, DataTypes.LANDMARK, "Landmark1");
         landmark1.setId(UUID.randomUUID().toString());
         ObjectGraph landmark2 = new ObjectGraph(null, DataTypes.LANDMARK, "Landmark2");
         landmark1.setId(UUID.randomUUID().toString());

         ObjectGraph gg = new ObjectGraph(null, DataTypes.GUIDANCE_GROUP, "GG2");
         gg.setId(UUID.randomUUID().toString());
         ObjectGraph gf = new ObjectGraph(null, DataTypes.GUIDANCE_CONFIGURATION, "GC");
         gf.setId(UUID.randomUUID().toString());
         ObjectGraph testObject = new ObjectGraph(null, "com.cnh.prescription.shapefile", "Shapefile Prescription"); // Not real object, used to demostrate format support
         testObject.setId(UUID.randomUUID().toString());
         field.addChild(testObject);
         field.addChild(task);
         field.addChild(task2);
         field.addChild(prescription1);
         field.addChild(prescription2);
         field.addChild(boundary1);
         field.addChild(boundary2);
         field.addChild(landmark1);
         field.addChild(landmark2);

         field.addChild(gg);
         field.addChild(gf);

         farm.addChild(field);
         cust.addChild(farm);

         map.put((T) Customer.class, cust);
      }
      else if (type.equals(Product.class)) {
         ObjectGraph pr1 = new ObjectGraph(null, DataTypes.PRODUCT, "Pord1");
         ObjectGraph pr2 = new ObjectGraph(null, DataTypes.PRODUCT, "Pord2");
         map.put((T) Product.class, pr1);
         map.put((T) Product.class, pr2);
      }
      else if (type.equals(Variety.class)) {
      }
      return map;
   }
}
