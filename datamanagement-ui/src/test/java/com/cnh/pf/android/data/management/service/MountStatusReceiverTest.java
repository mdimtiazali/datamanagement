/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.service;

import android.content.Context;
import android.content.Intent;

import com.cnh.pf.android.data.management.service.DataManagementService;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import android.test.mock.MockContext;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.jgroups.util.Util.assertTrue;


/*MountStatusReceiverTest Handles all UnitTests for MountStatusReceiver
 *@author Pallab Datta
 * */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")
public class MountStatusReceiverTest {
   private MountStatusReceiver mReceiver;
   private Context mContext;

   @Before
   public void setUp() throws Exception {
      mContext = mock(Context.class);
      mReceiver = new MountStatusReceiver();
   }

   @Test
   public void onReceiveMediaMountedTest() {
      Intent intent = new Intent(mContext, DataManagementService.class);
      intent.setAction("android.intent.action.MEDIA_MOUNTED");
      intent.putExtra(Intent.ACTION_MEDIA_MOUNTED, "android.intent.action.MEDIA_MOUNTED");
      mReceiver.onReceive(mContext, intent);
      assertNull(mReceiver.getResultData());
      ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
      verify(mContext, times(1)).startService(argument.capture());
      Intent receivedIntent = argument.getValue();
      assertNotNull(receivedIntent.getAction());
      assertEquals("android.intent.action.MEDIA_MOUNTED", receivedIntent.getStringExtra(Intent.ACTION_MEDIA_MOUNTED));
      assertTrue((receivedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
   }

   @Test
   public void onReceiveMediaUnMountedTest() {
      Intent intent = new Intent(mContext, DataManagementService.class);
      intent.setAction("android.intent.action.MEDIA_UNMOUNTED");
      intent.putExtra(Intent.ACTION_MEDIA_UNMOUNTED, "android.intent.action.MEDIA_UNMOUNTED");
      mReceiver.onReceive(mContext, intent);
      assertNull(mReceiver.getResultData());
      ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
      verify(mContext, times(1)).startService(argument.capture());
      Intent receivedIntent = argument.getValue();
      assertNotNull(receivedIntent.getAction());
      assertEquals("android.intent.action.MEDIA_UNMOUNTED", receivedIntent.getStringExtra(Intent.ACTION_MEDIA_UNMOUNTED));
      assertTrue((receivedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
   }

   @Test
   public void onReceiveMediaEjectTest() {
      Intent intent = new Intent(mContext, DataManagementService.class);
      intent.setAction("android.intent.action.MEDIA_EJECT");
      intent.putExtra(Intent.ACTION_MEDIA_EJECT, "android.intent.action.MEDIA_EJECT");
      mReceiver.onReceive(mContext, intent);
      assertNull(mReceiver.getResultData());
      ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
      verify(mContext, times(1)).startService(argument.capture());
      Intent receivedIntent = argument.getValue();
      assertNotNull(receivedIntent.getAction());
      assertEquals("android.intent.action.MEDIA_EJECT", receivedIntent.getStringExtra(Intent.ACTION_MEDIA_EJECT));
      assertTrue((receivedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
   }

   @Test
   public void onReceiveMediaBadRemovalTest() {
      Intent intent = new Intent(mContext, DataManagementService.class);
      intent.setAction("android.intent.action.MEDIA_BAD_REMOVAL");
      intent.putExtra(Intent.ACTION_MEDIA_BAD_REMOVAL, "android.intent.action.MEDIA_BAD_REMOVAL");
      mReceiver.onReceive(mContext, intent);
      assertNull(mReceiver.getResultData());
      ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
      verify(mContext, times(1)).startService(argument.capture());
      Intent receivedIntent = argument.getValue();
      assertNotNull(receivedIntent.getAction());
      assertEquals("android.intent.action.MEDIA_BAD_REMOVAL", receivedIntent.getStringExtra(Intent.ACTION_MEDIA_BAD_REMOVAL));
      assertTrue((receivedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
   }

   @Test
   public void onReceiveMediaRemovedTest() {
      Intent intent = new Intent(mContext, DataManagementService.class);
      intent.setAction("android.intent.action.MEDIA_REMOVED");
      intent.putExtra(Intent.ACTION_MEDIA_REMOVED, "android.intent.action.MEDIA_REMOVED");
      mReceiver.onReceive(mContext, intent);
      assertNull(mReceiver.getResultData());
      ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
      verify(mContext, times(1)).startService(argument.capture());
      Intent receivedIntent = argument.getValue();
      assertNotNull(receivedIntent.getAction());
      assertEquals("android.intent.action.MEDIA_REMOVED", receivedIntent.getStringExtra(Intent.ACTION_MEDIA_REMOVED));
      assertTrue((receivedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
   }

}
