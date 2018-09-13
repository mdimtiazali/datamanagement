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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.pf.android.data.management.DataManagementActivity;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.misc.IconizedFile;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.SessionExtra;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowListView;
import org.robolectric.util.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pl.polidea.treeview.TreeViewList;
import roboguice.RoboGuice;
import roboguice.activity.event.OnResumeEvent;
import roboguice.event.EventManager;

/**
 *
 * Tests for ImportSourceDialog
 *
 * @author cvogt
 * @since 06.09.2018
 */

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class ImportSourceDialogTest {

   @Rule
   public TemporaryFolder tempFolder = new TemporaryFolder();
   @Mock
   protected EventManager eventManager;
   @Mock
   DataManagementService.LocalBinder binder;
   @Mock
   DataManagementService service;
   private DataManagementActivity activity;
   private ImportSourceDialog importSourceDialog;

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new TestModule(eventManager));

      ActivityController<DataManagementActivity> controller = Robolectric.buildActivity(DataManagementActivity.class);
      when(binder.getService()).thenReturn(service);
      activity = controller.get();
      shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(new ComponentName(activity.getPackageName(), DataManagementService.class.getName()),
            binder);
      activity = controller.create().start().resume().visible().get();

   }

   @Test
   public void testTitleText() {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);

      assertThat(((TextView) importSourceDialog.findViewById(R.id.tvTitle)).getText().toString(), is("Select Import Source"));

   }

   @Test
   public void updateView_EmptyList() {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      //Todo:better use hamcrest matcher equalToIgnoringWhiteSpace
      String expectedMessage = "No import source device is detected. \\nPlease check the device connection.";
      assertThat(((TextView) importSourceDialog.findViewById(R.id.no_import_source_view)).getText().toString(), is(expectedMessage));

      Button okButton = (Button) importSourceDialog.findViewById(R.id.btFirst);
      assertThat(okButton.getText().toString(), is("OK"));
      //only the OK button is shown, the others should be GONE

      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.GONE, View.GONE, importSourceDialog);
      assertTrue(okButton.isEnabled());

   }

   @Test
   public void testUpdateView_EmptyList_ButtonEnabled() {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      //Todo:better use hamcrest matcher equalToIgnoringWhiteSpace
      String expectedMessage = "No import source device is detected. \\nPlease check the device connection.";
      assertThat(((TextView) importSourceDialog.findViewById(R.id.no_import_source_view)).getText().toString(), is(expectedMessage));

      Button okButton = (Button) importSourceDialog.findViewById(R.id.btFirst);
      assertThat(okButton.getText().toString(), is("OK"));
      //only the OK button is shown, the others should be GONE

      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.GONE, View.GONE, importSourceDialog);
      assertTrue(okButton.isEnabled());

      //simulate plugging usb
      sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra usbSessionExtra = new SessionExtra(SessionExtra.USB, "Test", 0);
      usbSessionExtra.setPath("Test");
      sessionExtras.add(usbSessionExtra);
      sessionExtras.clear();
      sessionExtras.add(usbSessionExtra);
      importSourceDialog.updateView(sessionExtras);

      //buttons should have changed
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.VISIBLE, View.GONE, importSourceDialog);
      Button selectButton = (Button) importSourceDialog.findViewById(R.id.btFirst);
      assertThat(selectButton.getText().toString(), is("Select"));
      Button cancelButton = (Button) importSourceDialog.findViewById(R.id.btSecond);
      assertThat(cancelButton.getText().toString(), is("Cancel"));
      assertFalse(selectButton.isEnabled());

      //simulate pulling usb

      sessionExtras = new ArrayList<SessionExtra>();
      importSourceDialog.updateView(sessionExtras);
      //again buttons should have changed

      assertThat(okButton.getText().toString(), is("OK"));
      //only the OK button is shown, the others should be GONE
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.GONE, View.GONE, importSourceDialog);
      assertTrue(okButton.isEnabled());
   }

   @Test
   public void testUpdateView_Null() {

      importSourceDialog = new ImportSourceDialog(activity, null);
      assertNotNull(importSourceDialog);
      String expectedMessage = "No import source device is detected. \\nPlease check the device connection.";
      assertThat(((TextView) importSourceDialog.findViewById(R.id.no_import_source_view)).getText().toString(), is(expectedMessage));

      Button okButton = (Button) importSourceDialog.findViewById(R.id.btFirst);
      assertThat(okButton.getText().toString(), is("OK"));
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.GONE, View.GONE, importSourceDialog);

   }

   @Test
   public void testUpdateView_CloudSession() {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra cloudExtra = new SessionExtra(SessionExtra.CLOUD, "Test", 0);
      sessionExtras.add(cloudExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);

      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.VISIBLE, View.GONE, importSourceDialog);

      Button selectButton = (Button) importSourceDialog.findViewById(R.id.btFirst);
      assertThat(selectButton.getText().toString(), is("Select"));
      Button cancelButton = (Button) importSourceDialog.findViewById(R.id.btSecond);
      assertThat(cancelButton.getText().toString(), is("Cancel"));

   }

   @Test
   public void testUpdateView_DisplayExtraSession() {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra displayExtra = new SessionExtra(SessionExtra.DISPLAY, "Test", 1);
      sessionExtras.add(displayExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.VISIBLE, View.GONE, importSourceDialog);

   }

   @Test
   public void testUpdateView_UsbSession() {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra usbSessionExtra = new SessionExtra(SessionExtra.USB, "Test", 0);
      usbSessionExtra.setPath("Test");
      sessionExtras.add(usbSessionExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      DialogTestUtilities.checkVisiblityOfDialogButtons(View.VISIBLE, View.VISIBLE, View.GONE, importSourceDialog);

      Button selectButton = (Button) importSourceDialog.findViewById(R.id.btFirst);
      assertThat(selectButton.getText().toString(), is("Select"));
      Button cancelButton = (Button) importSourceDialog.findViewById(R.id.btSecond);
      assertThat(cancelButton.getText().toString(), is("Cancel"));
   }

   @Test
   public void testUsbImportSource() throws IOException {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra usbSessionExtra = new SessionExtra(SessionExtra.USB, "Test", 0);
      //import a real data file
      File tempDir = tempFolder.newFolder();
      File parent = new File(tempDir.getAbsolutePath() + "/parent");
      parent.mkdir();

      IconizedFile testFile = new IconizedFile(File.createTempFile("test", ".dbf", parent));

      usbSessionExtra.setPath(tempDir.getAbsolutePath());
      sessionExtras.add(usbSessionExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      importSourceDialog.usbImportSource(testFile, 0, usbSessionExtra);
      assertThat(importSourceDialog.getCurrentExtra(), is(usbSessionExtra.getType()));

   }

   @Test
   public void testUsbImportSourceDisplay() throws IOException {

      //Only a workaroud to test SessionExtra.Display
      //Implementation is not done yet, needs to be enhanced
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra usbSessionExtra = new SessionExtra(SessionExtra.DISPLAY, "Test", 0);
      File tempDir = tempFolder.newFolder();
      File parent = new File(tempDir.getAbsolutePath() + "/parent");
      parent.mkdir();

      IconizedFile testFile = new IconizedFile(File.createTempFile("test", ".dbf", parent));

      usbSessionExtra.setPath(tempDir.getAbsolutePath());
      sessionExtras.add(usbSessionExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      importSourceDialog.usbImportSource(testFile, 0, usbSessionExtra);
      // enable assert if SessionExtra.Display is implemented
      //      assertThat(importSourceDialog.getCurrentExtra(), is(usbSessionExtra.getType()));

   }

   @Test
   public void testClickSelectButton() {

      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra usbSessionExtra = new SessionExtra(SessionExtra.USB, "Test", 0);
      usbSessionExtra.setPath("Test");
      sessionExtras.add(usbSessionExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      Button selectBtn = (Button) importSourceDialog.findViewById(R.id.btFirst);
      assertThat(selectBtn.getText().toString(), is("Select"));
      selectBtn.performClick();
      assertThat(importSourceDialog.isShown(), is(false));
      verify(eventManager).fire(isA(OnResumeEvent.class));
   }

   @Test
   public void testSelectButtonIsEnabled() throws IOException {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra usbSessionExtra = new SessionExtra(SessionExtra.USB, "Test", 0);
      File parent = tempFolder.newFolder();

      File.createTempFile("test", ".dbf", parent);

      usbSessionExtra.setPath(parent.getAbsolutePath());
      sessionExtras.add(usbSessionExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);

      Button selectBtn = (Button) importSourceDialog.findViewById(R.id.btFirst);
      //to make sure, we have got the correct button
      assertThat(selectBtn.getText().toString(), is("Select"));

      assertFalse(selectBtn.isEnabled());
      TreeViewList treeViewList = (TreeViewList) importSourceDialog.findViewById(R.id.source_path_usb_tree_view);
      assertNotNull(treeViewList);
      shadowOf(treeViewList).populateItems();
      ShadowListView shadowListView = shadowOf(treeViewList);
      //open the treeview
      shadowListView.performItemClick(0);
      shadowListView.populateItems();
      shadowListView.performItemClick(1);
      //check selecting file enables Button
      assertTrue(selectBtn.isEnabled());

      //deselecting file disables Button
      shadowListView.performItemClick(1);
      assertFalse(selectBtn.isEnabled());

   }

   @Test
   public void testClickCancelButton() {
      List<SessionExtra> sessionExtras = new ArrayList<SessionExtra>();
      SessionExtra usbSessionExtra = new SessionExtra(SessionExtra.USB, "Test", 0);
      usbSessionExtra.setPath("Test");
      sessionExtras.add(usbSessionExtra);
      importSourceDialog = new ImportSourceDialog(activity, sessionExtras);
      shadowOf(importSourceDialog).setMyParent(ReflectionHelpers.newInstance(LinearLayout.class));

      //Todo: make sure Dialog is shown.
      Button cancelBtn = (Button) importSourceDialog.findViewById(R.id.btSecond);
      assertThat(cancelBtn.getText().toString(), is("Cancel"));
      cancelBtn.performClick();
      verify(eventManager).fire(isA(OnResumeEvent.class));
      assertThat(importSourceDialog.isShown(), is(false));

   }

   /**
    * Test module to be used in the unit test for dependency injection
    *
    */
   public static class TestModule extends com.cnh.pf.android.data.management.TestModule {
      private EventManager eventManager;

      private TestModule(EventManager eventManager) {
         this.eventManager = eventManager;
      }

      @Override
      protected void configure() {
         super.configure();
         bind(EventManager.class).toInstance(eventManager);
      }
   }
}