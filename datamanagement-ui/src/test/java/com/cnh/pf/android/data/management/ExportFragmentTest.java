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
import android.widget.ImageButton;
import com.cnh.pf.android.data.management.helper.DataExchangeBlockedOverlay;
import com.cnh.pf.android.data.management.helper.VIPDataHandler;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.SessionManager;
import com.cnh.pf.model.constants.stringsConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import pl.polidea.treeview.TreeViewList;
import roboguice.RoboGuice;
import roboguice.event.EventManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class ExportFragmentTest {
   private static final int IMPORT_SOURCE_TAB_POSITION = 2;
   private DataManagementActivity activity;
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;
   @Mock
   SessionManager sessionManager;

   private EventManager eventManager;

   ExportFragment exportFragment;

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
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      exportFragment = (ExportFragment) activity.getFragmentManager().findFragmentByTag("Export");
   }

   @Test
   public void onPCMDisconnected() {

      assertThat(exportFragment, notNullValue());

      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);

      //init the treeView
      exportFragment.initAndPopulateTree(DataManagementUITest.getAllKindOfData());

      assertThat(treeViewList.getVisibility(), is(View.VISIBLE));

      exportFragment.onPCMDisconnected();
      assertThat(exportFragment.disabledOverlay.getMode(), is(DataExchangeBlockedOverlay.MODE.DISCONNECTED));
      assertThat(treeViewList.getVisibility(), is(View.GONE));

   }

   @Test
   public void onPCMConnected() {

      assertThat(exportFragment, notNullValue());

      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);
      exportFragment.onPCMDisconnected();
      assertThat(exportFragment.disabledOverlay.getMode(), is(DataExchangeBlockedOverlay.MODE.DISCONNECTED));
      assertThat(treeViewList.getVisibility(), is(View.GONE));

      exportFragment.onPCMConnected();
      assertThat(exportFragment.disabledOverlay.getMode(), is(DataExchangeBlockedOverlay.MODE.LOADING));

   }

   @Test
   public void testDestInfoBtn() {
      VIPDataHandler vipDataHandler = mock(VIPDataHandler.class);
      when(vipDataHandler.getMakeOfVehicle(Mockito.anyString())).thenReturn(stringsConstants.BRAND_CASE_IH);

      exportFragment.setVipDataHandler(vipDataHandler);

      ImageButton destInfoBtn = (ImageButton) activity.findViewById(R.id.dest_info_btn);
      assertThat(destInfoBtn, notNullValue());
      destInfoBtn.performClick();
      when(vipDataHandler.getMakeOfVehicle(Mockito.anyString())).thenReturn(stringsConstants.BRAND_NEW_HOLLAND);
      destInfoBtn.performClick();
      //Todo: ckeck text

   }

   @Test
   public void testExportInfoBtn() {

      ImageButton formatInfoBtn = (ImageButton) activity.findViewById(R.id.format_info_btn);
      assertThat(formatInfoBtn, notNullValue());
      formatInfoBtn.performClick();

   }

   /***************************
    * General Utility Methods *
    ***************************/
   private void activateTab(int tabPosition) {
      //Select Tab at position
      activity.selectTabAtPosition(tabPosition);
   }
}