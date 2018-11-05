/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.ComponentName;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.control.InputField;
import com.cnh.android.widget.control.PickList;
import com.cnh.pf.android.data.management.BaseTestRobolectric;
import com.cnh.pf.android.data.management.DataManagementActivity;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestModule;
import com.cnh.pf.android.data.management.dialog.DialogTestUtilities;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.configuration.VarietyColor;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

import java.util.ArrayList;
import java.util.Collections;
import roboguice.RoboGuice;

import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 *  Tests for AddOrEditVarietyDialog
 *
 * @author vogt
 */
public class AddOrEditVarietyDialogTestRobolectric extends BaseTestRobolectric {

   @Mock
   DataManagementService.LocalBinder binder;
   @Mock
   DataManagementService service;

   @Mock
   IVIPServiceAIDL vipService;
   private DataManagementActivity activity;
   AddOrEditVarietyDialog testDialog;

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

      ActivityController<DataManagementActivity> controller = Robolectric.buildActivity(DataManagementActivity.class);
      when(binder.getService()).thenReturn(service);
      activity = controller.get();
      shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()), binder);
      activity = controller.create().start().resume().visible().get();

      testDialog = new AddOrEditVarietyDialog(RuntimeEnvironment.application, null);

   }

   @Test
   public void testConstructor() {

      testDialog.onAttachedToWindow();
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.VISIBLE, View.GONE, testDialog);

   }

   @Test
   public void testDialogButtonsInitalState() {

      testDialog.onAttachedToWindow();
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.VISIBLE, View.GONE, testDialog);

      Button btnFirst = (Button) testDialog.findViewById(R.id.btFirst);
      Button btnSecond = (Button) testDialog.findViewById(R.id.btSecond);

      //Text needs to be tested from ProductLibraryFragment

      assertFalse(btnFirst.isEnabled());
      assertTrue(btnSecond.isEnabled());
   }

   @Test
   public void testApplyButtonIsEnabled() throws RemoteException {
      //prepare mock
      ArrayList<VarietyColor> varietyColors = new ArrayList<VarietyColor>();
      varietyColors.add(new VarietyColor());

      when(vipService.getVarietyColorList()).thenReturn(varietyColors);

      testDialog.onAttachedToWindow();
      testDialog.setVarietyList(Collections.EMPTY_LIST);
      testDialog.setVIPService(vipService);
      Button btnFirst = (Button) testDialog.findViewById(R.id.btFirst);
      assertFalse(btnFirst.isEnabled());
      //add a valid name and select a valid CropType
      InputField inputField = (InputField) testDialog.findViewById(R.id.variety_dialog_name_input_field);
      PickList pickList = (PickList) testDialog.findViewById(R.id.variety_dialog_crop_type_pick_list);
      inputField.setText("Test");

      pickList.setSelectionByPosition(1);
      assertTrue(btnFirst.isEnabled());

   }

   @Test
   public void testApplyButtonIsNotEnabled() throws RemoteException {
      //prepare mock
      ArrayList<VarietyColor> varietyColors = new ArrayList<VarietyColor>();
      varietyColors.add(new VarietyColor());
      when(vipService.getVarietyColorList()).thenReturn(varietyColors);

      //Set name but don't select CropType

      testDialog.onAttachedToWindow();
      testDialog.setVarietyList(Collections.EMPTY_LIST);
      testDialog.setVIPService(vipService);
      Button btnFirst = (Button) testDialog.findViewById(R.id.btFirst);
      assertFalse(btnFirst.isEnabled());
      InputField inputField = (InputField) testDialog.findViewById(R.id.variety_dialog_name_input_field);
      inputField.setText("Test");
      assertFalse(btnFirst.isEnabled());

      //Select Crop type, but don't set name
      testDialog = new AddOrEditVarietyDialog(RuntimeEnvironment.application, null);
      testDialog.onAttachedToWindow();
      testDialog.setVarietyList(Collections.EMPTY_LIST);
      testDialog.setVIPService(vipService);
      btnFirst = (Button) testDialog.findViewById(R.id.btFirst);
      assertFalse(btnFirst.isEnabled());
      PickList pickList = (PickList) testDialog.findViewById(R.id.variety_dialog_crop_type_pick_list);
      pickList.setSelectionByPosition(1);
      assertFalse(btnFirst.isEnabled());

   }

   @Test
   @Ignore("pfhmi-dev-defects-14003: Enable if defect is solved")
   public void testErrorMessageValue() throws RemoteException {
      ArrayList<VarietyColor> varietyColors = new ArrayList<VarietyColor>();
      varietyColors.add(new VarietyColor());
      when(vipService.getVarietyColorList()).thenReturn(varietyColors);
      Variety varietyMock = mock(Variety.class);
      when(varietyMock.getName()).thenReturn("Test");

      ArrayList<Variety> varietyArrayList = new ArrayList<Variety>();
      varietyArrayList.add(varietyMock);

      testDialog.onAttachedToWindow();
      testDialog.setVarietyList(varietyArrayList);
      testDialog.setVIPService(vipService);
      InputField inputField = (InputField) testDialog.findViewById(R.id.variety_dialog_name_input_field);

      assertNull(inputField.getError());

      inputField.setText("Test");
      assertThat(inputField.getError().toString(), equalToIgnoringWhiteSpace("Duplicate Entry Found.\\nPlease Enter New Input."));

   }

}