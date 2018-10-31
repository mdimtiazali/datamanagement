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

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.TestModule;
import com.cnh.pf.android.data.management.TreeEntityHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import roboguice.RoboGuice;

import java.util.LinkedList;
import java.util.List;

import static android.view.View.VISIBLE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.text.IsEqualIgnoringWhiteSpace.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test Class for CpCompleteDialogTest.
 *
 * @author: Ranjith P.A
 */

@RunWith(RobolectricMavenTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class) public class CpCompleteDialogTest {

   private CpCompleteDialog cpCompleteDialog;
   List<Integer> ids = new LinkedList<Integer>();
   List<String> names = new LinkedList<String>();
   private int swathFolderIcon;
   private String swathFolderName;

   @Before public void setUp() {

      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule());

      swathFolderIcon = TreeEntityHelper.getIcon(TreeEntityHelper.GUIDANCE_GROUPS);
      swathFolderName = TreeEntityHelper.getGroupName(RuntimeEnvironment.application, TreeEntityHelper.GUIDANCE_GROUPS);

   }

   @Test public void testConstructors() {

      cpCompleteDialog = new CpCompleteDialog(RuntimeEnvironment.application, "test", swathFolderIcon, swathFolderName, ids, names);

      DialogTestUtilities.checkVisiblityOfDialogButtons(VISIBLE, View.GONE, View.GONE, cpCompleteDialog);

      TextView textView = (TextView) cpCompleteDialog.findViewById(R.id.notice);

      assertThat(textView.getText().toString(), is("Copied to \"test\":"));

   }

   @Test public void testValidName() {
      cpCompleteDialog = new CpCompleteDialog(RuntimeEnvironment.application, "test", swathFolderIcon, swathFolderName, ids, names);
      Button firstButton = (Button) cpCompleteDialog.findViewById(R.id.btFirst);
      TextView singleItemView = (TextView) cpCompleteDialog.findViewById(R.id.singleitem);

      assertTrue(firstButton.isEnabled());
      assertEquals(firstButton.getText().toString(), "Done");

   }

}