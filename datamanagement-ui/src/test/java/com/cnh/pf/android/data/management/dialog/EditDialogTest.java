/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management.dialog;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import roboguice.RoboGuice;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class EditDialogTest {

   private EditDialog editDialog;

   @Before
   public void setUp() {

      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

   }

   @Test
   public void testConstructors() {

      editDialog = new EditDialog(RuntimeEnvironment.application, null);
      editDialog.onAttachedToWindow();

      DialogTestUtilities.checkVisiblityOfDialogButtons(VISIBLE, VISIBLE, View.GONE, editDialog);

      Button firstButton = (Button) editDialog.findViewById(R.id.btFirst);
      Button secondButton = (Button) editDialog.findViewById(R.id.btSecond);

      assertThat(firstButton.getText().toString(), is("Save"));
      assertFalse(firstButton.isEnabled());
      assertThat(secondButton.getText().toString(), is("Cancel"));
      assertTrue(secondButton.isEnabled());

      assertThat(((TextView) editDialog.findViewById(R.id.tvTitle)).getText().toString(), is("Edit Name"));

   }

   @Test
   public void testValidName() {
      editDialog = new EditDialog(RuntimeEnvironment.application, new HashSet<String>());
      editDialog.onAttachedToWindow();
      Button firstButton = (Button) editDialog.findViewById(R.id.btFirst);
      EditText textView = (EditText) editDialog.findViewById(R.id.edit_dialog_name_input_field);

      assertFalse(firstButton.isEnabled());
      textView.setText("Test");
      //new value is unique, so button 1 should enable
      assertTrue(firstButton.isEnabled());

   }

   @Test
   public void testNameNotUnique() {
      HashSet<String> names = new HashSet<String>();
      names.add("NewValue");
      editDialog = new EditDialog(RuntimeEnvironment.application, names);
      editDialog.onAttachedToWindow();
      Button firstButton = (Button) editDialog.findViewById(R.id.btFirst);
      EditText textView = (EditText) editDialog.findViewById(R.id.edit_dialog_name_input_field);
      TextView warning = (TextView) editDialog.findViewById(R.id.edit_name_warning);
      assertThat(warning.getVisibility(), is(INVISIBLE));
      assertFalse(firstButton.isEnabled());
      textView.setText("NewValue");
      //new value is already used, so button 1 should not enable
      assertFalse(firstButton.isEnabled());

      //a warning message should appear
      assertThat(warning.getVisibility(), is(VISIBLE));

      //check error icon appears at right side
      Drawable[] compoundDrawables = textView.getCompoundDrawables();
      assertNotNull(compoundDrawables[2]);

   }

   @Test
   public void testNameAsBefore() {
      HashSet<String> names = new HashSet<String>();
      names.add("NewValue");
      editDialog = new EditDialog(RuntimeEnvironment.application, names);
      editDialog.setDefaultStr("Test");
      editDialog.onAttachedToWindow();
      Button firstButton = (Button) editDialog.findViewById(R.id.btFirst);
      EditText textView = (EditText) editDialog.findViewById(R.id.edit_dialog_name_input_field);

      assertFalse(firstButton.isEnabled());
      textView.setText("Test");
      //new value is same as default value, no change in value, so button 1 should not enable
      assertFalse(firstButton.isEnabled());

   }

   @Test
   public void testSaveBtnClick() {
      EditDialog.UserSelectCallback mock = mock(EditDialog.UserSelectCallback.class);

      editDialog = new EditDialog(RuntimeEnvironment.application, new HashSet<String>());
      editDialog.onAttachedToWindow();
      editDialog.setUserSelectCallback(mock);
      Button firstButton = (Button) editDialog.findViewById(R.id.btFirst);
      EditText textView = (EditText) editDialog.findViewById(R.id.edit_dialog_name_input_field);

      assertFalse(firstButton.isEnabled());
      textView.setText("Test");
      //new value is unique, so button 1 should enable
      assertTrue(firstButton.isEnabled());

      firstButton.performClick();
      verify(mock).inputStr("Test");

   }

   @Test
   public void testErrorMessageValue() {
      HashSet<String> names = new HashSet<String>();
      names.add("NewValue");
      editDialog = new EditDialog(RuntimeEnvironment.application, names);
      editDialog.onAttachedToWindow();

      EditText textView = (EditText) editDialog.findViewById(R.id.edit_dialog_name_input_field);
      TextView warning = (TextView) editDialog.findViewById(R.id.edit_name_warning);
      textView.setText("NewValue");

      assertThat(warning.getText().toString(), equalToIgnoringWhiteSpace("Duplicate Entry Found.\\nPlease Enter New Input"));

   }

   @Test
   public void testErrorMessageDisappers() {
      HashSet<String> names = new HashSet<String>();
      names.add("NewValue");
      editDialog = new EditDialog(RuntimeEnvironment.application, names);
      editDialog.onAttachedToWindow();

      EditText editView = (EditText) editDialog.findViewById(R.id.edit_dialog_name_input_field);
      TextView warningTextView = (TextView) editDialog.findViewById(R.id.edit_name_warning);
      assertThat(warningTextView.getVisibility(), is(INVISIBLE));
      editView.setText("NewValue");
      //new value is already used, so button 1 should not enable
      assertThat(warningTextView.getVisibility(), is(VISIBLE));
      assertNotNull(editView.getCompoundDrawables()[2]);
      //a warning message should appear
      editView.setText("Value");
      //a warning message should disappear
      assertThat(warningTextView.getVisibility(), is(INVISIBLE));
      assertNull(editView.getCompoundDrawables()[2]);

      //testing different substrings
      editView.setText("NewValue");
      assertThat(warningTextView.getVisibility(), is(VISIBLE));
      assertNotNull(editView.getCompoundDrawables()[2]);
      editView.append("1");
      assertThat(warningTextView.getVisibility(), is(INVISIBLE));
      assertNull(editView.getCompoundDrawables()[2]);

      editView.setText("NewValue");
      assertThat(warningTextView.getVisibility(), is(VISIBLE));
      assertNotNull(editView.getCompoundDrawables()[2]);
      editView.append("1NewValue");
      assertThat(warningTextView.getVisibility(), is(INVISIBLE));
      assertNull(editView.getCompoundDrawables()[2]);

   }

}