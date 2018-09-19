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

import static android.view.View.VISIBLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.robolectric.RuntimeEnvironment.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import roboguice.RoboGuice;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class DeleteDialogTest {

   private DeleteDialog deleteDialog;

   @Before
   public void setUp() {
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());
   }

   @Test
   public void testConstructors() {

      deleteDialog = new DeleteDialog(application, 0);
      DialogTestUtilities.checkVisiblityOfDialogButtons(VISIBLE, VISIBLE, View.GONE, deleteDialog);
      Button firstButton = (Button) deleteDialog.findViewById(R.id.btFirst);
      Button secondButton = (Button) deleteDialog.findViewById(R.id.btSecond);

      assertThat(firstButton.getText().toString(), is("Delete"));
      assertTrue(firstButton.isEnabled());
      assertThat(secondButton.getText().toString(), is("Cancel"));
      assertTrue(secondButton.isEnabled());
   }

   @Test
   public void testTitle() {
      testTitleView(0);
      testTitleView(100);
      testTitleView(1000);
      testTitleView(10000);
   }

   @Test
   public void testMessage() {
      int noOfItems = 0;
      deleteDialog = new DeleteDialog(application, noOfItems);
      TextView deleteConformView = (TextView) deleteDialog.findViewById(R.id.delete_confirm_tv);
      assertThat(deleteConformView.getText().toString(),
            equalToIgnoringWhiteSpace("Are you sure you want to delete " + noOfItems + " items? The items will be permanently deleted."));
   }

   private void testTitleView(int noOfItems) {

      deleteDialog = new DeleteDialog(application, noOfItems);
      TextView titleView = (TextView) deleteDialog.findViewById(R.id.tvTitle);
      assertThat(titleView.getText().toString(), equalToIgnoringWhiteSpace("Delete Confirmation (" + noOfItems + " Items)"));
   }

}