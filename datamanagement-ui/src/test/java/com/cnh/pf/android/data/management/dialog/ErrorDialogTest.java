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

import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import com.cnh.pf.android.data.management.session.ErrorCode;

import android.view.View;
import android.widget.TextView;
import roboguice.RoboGuice;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class ErrorDialogTest {

   @Before
   public void setUp() {

      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

   }

   @Test
   public void testConstructor() {
      ErrorDialog errorDialog = new ErrorDialog(RuntimeEnvironment.application, ErrorCode.USB_REMOVED);
      assertNotNull(errorDialog);
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.GONE, View.GONE, errorDialog);
   }

   @Test
   public void testTitle() {
      ErrorCode performError = ErrorCode.PERFORM_ERROR;
      String expectedTitle = RuntimeEnvironment.application.getString(performError.resource());
      ErrorDialog errorDialog = new ErrorDialog(RuntimeEnvironment.application, performError);
      assertNotNull(errorDialog);

      TextView titleView = (TextView) errorDialog.findViewById(R.id.tvTitle);
      assertThat(titleView.getText().toString(), equalToIgnoringWhiteSpace(expectedTitle));
   }

   @Test
   public void testErrorMessage() {
      ErrorCode deleteError = ErrorCode.DELETE_ERROR;
      String expectedErrorMessage = deleteError.toString();
      ErrorDialog errorDialog = new ErrorDialog(RuntimeEnvironment.application, deleteError);
      assertNotNull(errorDialog);

      TextView errorView = (TextView) errorDialog.findViewById(R.id.error_string);
      assertThat(errorView.getText().toString(), equalToIgnoringWhiteSpace(expectedErrorMessage));
   }

}