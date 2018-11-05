/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.service;

import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import android.test.mock.MockContext;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.service.MulticastRouteService;

/*BootReceiverTest Handles all UnitTests for BootReceiver
 *@author Pallab Datta
 * */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")

public class BootReceiverTest {

   private BootReceiver mReceiver;
   private Context mContext;

   @Before
   public void setUp() throws Exception {
      mReceiver = new BootReceiver();
      mContext = mock(Context.class);
   }

   @Test
   public void onReceiveDataManagementServiceTest() {
      Intent intent = new Intent(mContext, DataManagementService.class);
      intent.setAction("com.cnh.android.intent.action.BOOT_COMPLETED_PRI_2");
      mReceiver.onReceive(mContext, intent);
      assertNull(mReceiver.getResultData());
      ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
      verify(mContext, times(1)).startService(argument.capture());
      Intent receivedIntent = argument.getValue();
      assertNotNull(receivedIntent.getAction());

   }

   @Test
   public void onReceiveMulticastRouteServiceTest() {
      Intent intent = new Intent(mContext, MulticastRouteService.class);
      intent.setAction("android.intent.action.BOOT_COMPLETED_PRI_2");
      mReceiver.onReceive(mContext, intent);
      assertNull(mReceiver.getResultData());
      ArgumentCaptor<Intent> argument = ArgumentCaptor.forClass(Intent.class);
      verify(mContext, times(1)).startService(argument.capture());
      Intent receivedIntent = argument.getValue();
      assertNotNull(receivedIntent.getAction());
   }
}