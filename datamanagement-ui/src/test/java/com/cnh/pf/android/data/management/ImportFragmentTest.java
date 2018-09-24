/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import android.content.ComponentName;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import com.cnh.pf.android.data.management.dialog.ImportSourceDialog;
import com.cnh.pf.android.data.management.helper.DataExchangeBlockedOverlay;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.android.data.management.session.SessionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;
import pl.polidea.treeview.TreeViewList;
import roboguice.RoboGuice;
import roboguice.event.EventManager;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Tests for the import fragment
 *
 * @author krueger
 * @since 24.08.2018
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class ImportFragmentTest {
   private static final int IMPORT_SOURCE_TAB_POSITION = 1;

   private DataManagementActivity activity;
   @Mock
   DataManagementService service;
   @Mock
   DataManagementService.LocalBinder binder;
   @Mock
   SessionManager sessionManager;

   private EventManager eventManager;

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
   public void onPCMDisconnected() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment view = (ImportFragment) activity.getFragmentManager().findFragmentByTag("Import");
      assertThat(view, notNullValue());

      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);

      //init the treeView
      view.initAndPopulateTree(DataManagementUITest.getAllKindOfData());

      assertThat(treeViewList.getVisibility(), is(View.VISIBLE));

      view.onPCMDisconnected();
      assertThat(view.disabledOverlay.getMode(), is(DataExchangeBlockedOverlay.MODE.DISCONNECTED));
      assertThat(treeViewList.getVisibility(), is(View.GONE));

   }

   @Test
   public void onPCMConnected() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment view = (ImportFragment) activity.getFragmentManager().findFragmentByTag("Import");
      assertThat(view, notNullValue());

      TreeViewList treeViewList = (TreeViewList) activity.findViewById(R.id.tree_view_list);
      view.onPCMDisconnected();
      assertThat(view.disabledOverlay.getMode(), is(DataExchangeBlockedOverlay.MODE.DISCONNECTED));
      assertThat(treeViewList.getVisibility(), is(View.GONE));

      view.onPCMConnected();
      assertThat(view.disabledOverlay.getMode(), is(DataExchangeBlockedOverlay.MODE.HIDDEN));

   }

   @Test
   public void testLoadingImportOverlay() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment view = (ImportFragment) activity.getFragmentManager().findFragmentByTag("Import");
      assertThat(view, notNullValue());
      eventManager = RoboGuice.getInjector(activity).getInstance(EventManager.class);
      SessionExtra sessionExtra = new SessionExtra(SessionExtra.USB, "noname", SessionExtra.DISPLAY);
      ImportSourceDialog.ImportSourceSelectedEvent event = new ImportSourceDialog.ImportSourceSelectedEvent(sessionExtra);
      eventManager.fire(event);
      assertThat("Loading Overlay is active while loading datatree", view.treeLoadingOverlay.getMode(), is(DisabledOverlay.MODE.LOADING));
   }

   @Test
   public void testLabelsIntialView() {
      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment fragment = (ImportFragment) activity.getFragmentManager().findFragmentByTag("Import");

      assertThat(fragment, notNullValue());

      Button selectImportBtn = (Button) activity.findViewById(R.id.import_source_btn);
      TextView importDropZoneTextView = (TextView) activity.findViewById(R.id.import_drop_zone_text);
      Button importBtn = (Button) activity.findViewById(R.id.import_selected_btn);
      TextView startTextView = (TextView) activity.findViewById(R.id.start_text);
      Button selectAllBtn = (Button) activity.findViewById(R.id.select_all_btn);

      //check elements  for correct labels
      assertThat(selectImportBtn.getText().toString(), equalToIgnoringWhiteSpace("Select Import Source"));
      assertThat(importDropZoneTextView.getText().toString(), equalToIgnoringWhiteSpace("Drag & Drop\\nto Import\\nto Vehicle"));
      assertThat(importBtn.getText().toString(), equalToIgnoringWhiteSpace("Import Selected"));
      assertThat(startTextView.getText().toString(), equalToIgnoringWhiteSpace("Select Import Source \\nto get started"));

      //check elements for visibility and enabled status
      assertThat(selectImportBtn.getVisibility(), is(View.VISIBLE));
      assertTrue(selectImportBtn.isEnabled());
      assertThat(importBtn.getVisibility(), is(View.VISIBLE));
      assertFalse(importBtn.isEnabled());
      assertThat(startTextView.getVisibility(), is(View.VISIBLE));
      assertThat(importDropZoneTextView.getVisibility(), is(View.VISIBLE));

      assertThat(selectAllBtn.getVisibility(), is(View.GONE));

   }

   @Test
   public void testLabelsWithTreeView() {
      //prepare the mocks
      ObjectGraph objectGraphMock = mock(ObjectGraph.class);
      when(objectGraphMock.getType()).thenReturn("Test");
      ArrayList<ObjectGraph> objectGraphs = new ArrayList<ObjectGraph>();
      objectGraphs.add(objectGraphMock);

      Session sessionMock = mock(Session.class);
      when(sessionMock.getType()).thenReturn(Session.Type.DISCOVERY);
      when(sessionMock.getObjectData()).thenReturn(objectGraphs);
      when(sessionMock.getAction()).thenReturn(Session.Action.IMPORT);

      when(sessionManager.getCurrentSession(Session.Action.IMPORT)).thenReturn(sessionMock);
      service.processSession(sessionMock);

      ObjectTreeViewAdapter treeViewAdapter = mock(ObjectTreeViewAdapter.class);
      when(treeViewAdapter.getSelected()).thenReturn(Collections.<ObjectGraph>emptySet());
      when(treeViewAdapter.getCount()).thenReturn(1);
      when(treeViewAdapter.getData()).thenReturn(objectGraphs);
      when(treeViewAdapter.getViewTypeCount()).thenReturn(1);
      //done

      activateTab(IMPORT_SOURCE_TAB_POSITION);
      ImportFragment fragment = (ImportFragment) activity.getFragmentManager().findFragmentByTag("Import");

      assertThat(fragment, notNullValue());
      //use the mock sessionManager
      fragment.setSessionManager(sessionManager);

      Button selectImportBtn = (Button) activity.findViewById(R.id.import_source_btn);
      Button importBtn = (Button) activity.findViewById(R.id.import_selected_btn);
      TextView startTextView = (TextView) activity.findViewById(R.id.start_text);
      Button selectAllBtn = (Button) activity.findViewById(R.id.select_all_btn);

      //we need a tree
      eventManager = RoboGuice.getInjector(activity).getInstance(EventManager.class);
      SessionExtra sessionExtra = new SessionExtra(SessionExtra.USB, "noname", SessionExtra.DISPLAY);
      ImportSourceDialog.ImportSourceSelectedEvent event = new ImportSourceDialog.ImportSourceSelectedEvent(sessionExtra);
      eventManager.fire(event);

      fragment.treeAdapter = treeViewAdapter;
      fragment.onMyselfSessionSuccess(sessionMock);

      assertThat(selectAllBtn.getVisibility(), is(View.VISIBLE));
      assertTrue(selectAllBtn.isEnabled());
      assertThat(selectImportBtn.getVisibility(), is(View.VISIBLE));
      assertTrue(selectImportBtn.isEnabled());
      assertThat(startTextView.getVisibility(), is(View.GONE));

      //import Button should only enabled, if selection in tree has been made
      assertThat(importBtn.getVisibility(), is(View.VISIBLE));
      assertFalse(importBtn.isEnabled());

   }

   /***************************
    * General Utility Methods *
    ***************************/
   private void activateTab(int tabPosition) {
      //Select Tab at position
      activity.selectTabAtPosition(tabPosition);
   }
}
