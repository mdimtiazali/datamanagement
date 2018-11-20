/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.fault;

import com.cnh.alert.types.Status;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.fault.FaultHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;
import roboguice.context.event.OnDestroyEvent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class) public class DMFaultHandlerTest {

   private DMFaultHandler dmFaultHandler;
   private FaultCode faultcode;
   private DMFaultHandler.Fault dmfault;
   private FaultHandler faultHandlermock;
   private Mediator mediatormock;
   private OnDestroyEvent onDestroyEventmock;

   @Before
   public void setUp() throws Exception {
      dmFaultHandler = new DMFaultHandler();
      faultcode = FaultCode.USB_NOT_ENOUGH_MEMORY;
      faultHandlermock = mock(FaultHandler.class);
      mediatormock = mock(Mediator.class);
      onDestroyEventmock = mock(OnDestroyEvent.class);

   }

   @Test
   public void onConnectionChangedPositiveTest() {
      dmFaultHandler.onConnectionChanged(true);
      assertFalse(dmFaultHandler.isRecoverConnection());
      dmFaultHandler.onConnectionChanged(false);
      assertTrue(dmFaultHandler.isRecoverConnection());
   }

   @Test
   public void getFaultTest() {
      dmFaultHandler.getFault(faultcode);

      /*check for size mismatch for the map */
      assertEquals(1, dmFaultHandler.getFaultMap().size());

      /*check for key*/
      assertEquals(true, dmFaultHandler.getFaultMap().keySet().contains(faultcode));
   }

   @Test
   public void setMediatorTest() {
      dmFaultHandler.setMediator(mediatormock);
      assertTrue(dmFaultHandler.getMediator() == mediatormock);
   }

   @Test
   public void resetTest() {
      /*Mock clearFault method*/
      doNothing().when(faultHandlermock).clearFault(faultcode.getCode());

      /*Mock faulthandler intialization*/
      when(faultHandlermock.isInitialized()).thenReturn(true);

      DMFaultHandler.Fault mockfault = DMFaultHandler.Fault.newFault(faultHandlermock, faultcode);

      /*Unit test reset method*/
      mockfault.reset();

      /* verify method was executed */
      verify(faultHandlermock).clearFault(faultcode.getCode());
   }

   @Test
   public void alertTest() {
      /*Mock setFault method*/
      doNothing().when(faultHandlermock).setFault(faultcode.getCode());

      /*Mock faulthandler intialization*/
      when(faultHandlermock.isInitialized()).thenReturn(true);

      DMFaultHandler.Fault mockfault = DMFaultHandler.Fault.newFault(faultHandlermock, faultcode);

      /*Unit test reset method*/
      mockfault.alert();

      /* verify method was executed */
      verify(faultHandlermock).setFault(faultcode.getCode());
   }

   @Test
   public void changeUSBDetectedStatusTest() {

      dmFaultHandler = mock(DMFaultHandler.class);

      //      assertNull(alertManager);
      //      assertNull(producer);

      dmFaultHandler.changeUSBDetectedStatus(true);

      verify(dmFaultHandler).changeUSBDetectedStatus(true);

      dmFaultHandler.changeUSBDetectedStatus(false);

      verify(dmFaultHandler).changeUSBDetectedStatus(false);

   }

   @Test
   public void showImportExportStatusTest() {

      dmFaultHandler = mock(DMFaultHandler.class);

      dmFaultHandler.showImportExportStatus(Status.StatusType.DM_SESSION_IMPORT_IN_PROGRESS);
      verify(dmFaultHandler).showImportExportStatus(Status.StatusType.DM_SESSION_IMPORT_IN_PROGRESS);

      dmFaultHandler.showImportExportStatus(Status.StatusType.DM_SESSION_EXPORT_IN_PROGRESS);
      verify(dmFaultHandler).showImportExportStatus(Status.StatusType.DM_SESSION_EXPORT_IN_PROGRESS);

   }

}