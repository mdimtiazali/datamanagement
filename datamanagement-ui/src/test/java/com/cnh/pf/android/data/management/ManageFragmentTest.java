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

import android.content.ComponentName;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cnh.jgroups.DataTypes;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.helper.DataExchangeBlockedOverlay;
import com.cnh.pf.android.data.management.misc.DeleteButton;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.model.pfds.Customer;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.library.Product;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.util.ActivityController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import pl.polidea.treeview.TreeViewList;
import roboguice.RoboGuice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class ManageFragmentTest {
   private DataManagementActivity activity;
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
      ActivityController<DataManagementActivity> controller = Robolectric.buildActivity(DataManagementActivity.class);
      activity = controller.get();
      when(binder.getService()).thenReturn(service);
      shadowOf(RuntimeEnvironment.application)
         .setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()), binder);
      //Start activity
      controller.create().start().resume();
      //      sessionManager = activity.sessionManager;
   }

   @Test
   public void testOnPCMDisconnected() {
      ManageFragment view = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");
      assertThat(view, notNullValue());

      DeleteButton delButton = (DeleteButton) activity.findViewById(R.id.dm_delete_button);
      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);
      TextView headerTextView = (TextView) activity.findViewById(R.id.header_text);

      //init the treeView
      view.initAndPopulateTree(DataManagementUITest.getAllKindOfData());
      assertThat(delButton.getVisibility(), is(View.VISIBLE));

      assertThat(treeViewList.getVisibility(), is(View.VISIBLE));
      assertThat(headerTextView.getText().toString(), is(""));

      view.onPCMDisconnected();
      assertThat(view.disabledOverlay.getMode(), is(DataExchangeBlockedOverlay.MODE.DISCONNECTED));
      assertThat(treeViewList.getVisibility(), is(View.GONE));
      assertThat(headerTextView.getText().toString(), is(""));

   }

   @Test
   public void testItemSelectionUpdatesHeaderText() {
      ManageFragment view = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");
      assertThat(view, notNullValue());

      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);
      TextView headerTextView = (TextView) activity.findViewById(R.id.header_text);

      //init the treeView
      view.initAndPopulateTree(getAllKindOfData());
      ShadowListView shadowListView = shadowOf(treeViewList);
      shadowListView.populateItems();
      assertThat(treeViewList.getVisibility(), is(View.VISIBLE));

      assertThat(headerTextView.getText().toString(), is(""));
      shadowListView.clickFirstItemContainingText("Crop");

      //selecting "Crop and Product Library"
      //header text should have been changed and delete button should have been enabled
      int cropId = shadowListView.findIndexOfItemContainingText("Crop");
      assertThat(headerTextView.getText().toString(), is("3 Items Selected"));

      expandListItem(treeViewList, cropId);
      shadowListView.clickFirstItemContainingText("Products");
      int productsId = shadowListView.findIndexOfItemContainingText("Products");

      shadowListView.populateItems();
      expandListItem(treeViewList, productsId);
      shadowListView.clickFirstItemContainingText("Prod1");
      assertThat(headerTextView.getText().toString(), is("1 Item Selected"));

      //again clicking on Item should disable del button and update Header Text
      shadowListView.clickFirstItemContainingText("Prod1");
      View prod1 = shadowListView.findItemContainingText("Prod1");

      assertThat(headerTextView.getText().toString(), is("Select item(s) to edit, copy, or delete"));

   }

   @Test
   public void testItemSelectionUpdatesDelBtn() {
      ManageFragment view = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");
      assertThat(view, notNullValue());
      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);
      DeleteButton delButton = (DeleteButton) activity.findViewById(R.id.dm_delete_button);

      //init the treeView
      view.initAndPopulateTree(getAllKindOfData());
      ShadowListView shadowListView = shadowOf(treeViewList);
      shadowListView.populateItems();
      assertThat(treeViewList.getVisibility(), is(View.VISIBLE));
      assertFalse(delButton.isEnabled());

      int cropId = shadowListView.findIndexOfItemContainingText("Crop");
      shadowListView.performItemClick(cropId);

      //selecting "Crop and Product Library"
      //delete button should have been enabled

      assertTrue(delButton.isEnabled());
      shadowListView.performItemClick(cropId);
      assertFalse(delButton.isEnabled());

      expandListItem(treeViewList, cropId);
      expandListItem(treeViewList, shadowListView.findIndexOfItemContainingText("Products"));
      shadowListView.clickFirstItemContainingText("Prod1");
      assertTrue(delButton.isEnabled());
      //again clicking on Item should disable del button

      shadowListView.clickFirstItemContainingText("Prod1");
      assertFalse(delButton.isEnabled());

   }

   @Test
   public void testDelBtn() {
      ManageFragment view = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");
      assertThat(view, notNullValue());
      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);
      DeleteButton delButton = (DeleteButton) activity.findViewById(R.id.dm_delete_button);

      //init the treeView
      view.initAndPopulateTree(getAllKindOfData());
      ShadowListView shadowListView = shadowOf(treeViewList);
      shadowListView.populateItems();
      assertThat(treeViewList.getVisibility(), is(View.VISIBLE));
      assertFalse(delButton.isEnabled());

      int cropId = shadowListView.findIndexOfItemContainingText("Crop");
      shadowListView.performItemClick(cropId);

      assertTrue(delButton.isEnabled());
      delButton.performClick();
      //DeleteDialog should be visible
      View viewById = activity.findViewById(R.id.rlPopupArea);

      assertThat(viewById.getVisibility(), is(View.VISIBLE));
      TextView textview = (TextView) viewById.findViewById(R.id.delete_confirm_tv);
      assertThat(textview.getText().toString(), containsString("3 items"));

      //check cancel dialog
      Button cancleBtn = (Button) viewById.findViewById(R.id.btSecond);
      cancleBtn.performClick();
      assertThat(viewById.getVisibility(), is(View.GONE));

      //check Delete Button
      delButton.performClick();
      //DeleteDialog should be visible
      viewById = activity.findViewById(R.id.rlPopupArea);

      assertThat(viewById.getVisibility(), is(View.VISIBLE));

      //check cancel dialog
      Button deleteBtn = (Button) viewById.findViewById(R.id.btFirst);
      deleteBtn.performClick();
      assertThat(viewById.getVisibility(), is(View.GONE));

   }

   @Test
   public void testEditBtn() {
      ManageFragment view = (ManageFragment) activity.getFragmentManager().findFragmentByTag("Data Management");
      assertThat(view, notNullValue());
      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);
      DeleteButton delButton = (DeleteButton) activity.findViewById(R.id.dm_delete_button);

      //init the treeView
      view.initAndPopulateTree(getAllKindOfData());
      ShadowListView shadowListView = shadowOf(treeViewList);
      shadowListView.populateItems();
      assertThat(treeViewList.getVisibility(), is(View.VISIBLE));
      assertFalse(delButton.isEnabled());

      int cropId = shadowListView.findIndexOfItemContainingText("Crop");

      expandListItem(treeViewList, cropId);
      expandListItem(treeViewList, shadowListView.findIndexOfItemContainingText("Products"));
      shadowListView.clickFirstItemContainingText("Prod1");
      assertTrue(delButton.isEnabled());
      //again clicking on Item should disable del button

      shadowListView.clickFirstItemContainingText("Prod1");
      View prod1 = shadowListView.findItemContainingText("Prod1");
      ImageButton editBtn = (ImageButton) prod1.findViewById(R.id.mng_edit_button);
      assertTrue(editBtn.isEnabled());
      editBtn.performClick();

      //Edit should be visible
      View editDialogView = activity.findViewById(R.id.rlPopupArea);

      shadowOf(editDialogView).callOnAttachedToWindow();
      View dialogRoot = editDialogView.findViewById(R.id.dialogRoot);

      shadowOf(dialogRoot).callOnAttachedToWindow();

      assertThat(editDialogView.getVisibility(), is(View.VISIBLE));

   }

   private void expandListItem(TreeViewList treeViewList, int id) {
      ListAdapter adapter = treeViewList.getAdapter();
      View itemView = adapter.getView(id, null, null);
      ImageView imageView = (ImageView) itemView.findViewById(R.id.treeview_list_item_toggle);

      imageView.performClick();
      assertNotNull(imageView);
      shadowOf(treeViewList).populateItems();

   }

   public static List<ObjectGraph> getAllKindOfData() {
      List<ObjectGraph> graphList = new ArrayList<ObjectGraph>();
      graphList.addAll(getTestObjectGraphMap(Customer.class));
      graphList.addAll(getTestObjectGraphMap(Product.class));
      graphList.addAll(getTestObjectGraphMap(Variety.class));
      return graphList;
   }

   public static <T> List<ObjectGraph> getTestObjectGraphMap(Class<T> type) {
      ArrayList<ObjectGraph> list = new ArrayList<ObjectGraph>();

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

         list.add(cust);

      }
      else if (type.equals(Product.class)) {
         ObjectGraph pr1 = new ObjectGraph(null, DataTypes.PRODUCT, "1", "Prod1");
         ObjectGraph pr2 = new ObjectGraph(null, DataTypes.PRODUCT, "2", "Prod2");
         list.add(pr1);
         list.add(pr2);

         list.add(new ObjectGraph(null, DataTypes.PRODUCT, "3", "Prod3"));
      }
      else if (type.equals(Variety.class)) {
      }
      return list;
   }
}