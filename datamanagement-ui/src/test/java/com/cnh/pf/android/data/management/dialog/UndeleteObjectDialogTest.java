/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.dialog;

import android.widget.Button;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/* UndeleteObjectDialogTest Handles UnitTests for UndeleteObjectDialog
 *@author Pallab Datta
 * */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)

public class UndeleteObjectDialogTest {
   private UndeleteObjectDialog mUndeleObjectDialog;
   List<ObjectGraph> obj = new LinkedList<ObjectGraph>();

   @Before
   public void setUp() throws Exception {
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

   }

   @Test
   public void ConstructorTest() {
      mUndeleObjectDialog = new UndeleteObjectDialog(RuntimeEnvironment.application, obj);
      assertNotNull(mUndeleObjectDialog);
      Button button1 = (Button) mUndeleObjectDialog.findViewById(R.id.btFirst);
      Button button2 = (Button) mUndeleObjectDialog.findViewById(R.id.btSecond);
      assertThat(button1.getText().toString(), is("OK"));
      assertTrue(button1.isEnabled());
      assertThat(button2.getText().toString(), is("Cancel"));
      assertTrue(button2.isEnabled());
   }

}