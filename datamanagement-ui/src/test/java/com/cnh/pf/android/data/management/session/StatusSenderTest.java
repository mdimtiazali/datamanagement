/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management.session;

import com.cnh.pf.android.data.management.TestApp;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import static org.robolectric.RuntimeEnvironment.application;

/**
 *StatusSenderTest Handles all UnitTests for StatusSender.
 *@auther Himanshu Ranawat
 */

@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class StatusSenderTest {

   private StatusSender statusSender;

   @Before
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);
      statusSender = new StatusSender(application);
      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();
   }

   @After
   public void tearDown() {
      statusSender = null;
   }

   @Test
   public void testSendStatusExportPass() {
      statusSender.sendStatus("Error", true);
      Assert.assertEquals("Exporting Error", statusSender.getStatus().getMessage());
   }

   @Test
   public void testSendStatusExportFail() {
      statusSender.sendStatus("Error", false);
      Assert.assertNotEquals("Exporting Error", statusSender.getStatus().getMessage());
   }

   @Test
   public void testSendStatusImportPass() {
      statusSender.sendStatus("Error", false);
      Assert.assertEquals("Importing Error", statusSender.getStatus().getMessage());
   }

   @Test
   public void testSendStatusImportFail() {
      statusSender.sendStatus("Error", true);
      Assert.assertNotEquals("Importing Error", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendStartingStatusPass() {
      statusSender.sendStartingStatus(true);
      Assert.assertEquals("Exporting Starting...", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendStartingStatusFail() {
      statusSender.sendStartingStatus(false);
      Assert.assertNotEquals("Exporting Starting...", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendCancelledStatusPass() {
      statusSender.sendCancelledStatus(true);
      Assert.assertEquals("Exporting Cancelled", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendCancelledStatusFail() {
      statusSender.sendCancelledStatus(false);
      Assert.assertNotEquals("Exporting Cancelled", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendSuccessfulStatusPass() {
      statusSender.sendSuccessfulStatus(true);
      Assert.assertEquals("Exporting Successful", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendSuccessfulStatusFail() {
      statusSender.sendSuccessfulStatus(false);
      Assert.assertNotEquals("Exporting Successful", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendCancellingStatusPass() {
      statusSender.sendCancellingStatus(true);
      Assert.assertEquals("Exporting Cancelling...", statusSender.getStatus().getMessage());
   }

   @Test
   public void testsendCancellingStatusFail() {
      statusSender.sendCancellingStatus(false);
      Assert.assertNotEquals("Exporting Cancelling...", statusSender.getStatus().getMessage());
   }

}
