/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.ParcelUuid;
import com.cnh.android.status.Status;
import com.cnh.pf.android.data.management.R;

/**
 * StatusSender sends status notification message. It sends status notification via Intent.
 *
 * @author: junsu.shin@cnhind.com
 */
public class StatusSender {
   private Context context;
   private BitmapDrawable statusDrawable;
   private Status status;

   public StatusSender(Context context) {
      this.context = context;
      this.statusDrawable = (BitmapDrawable) context.getResources().getDrawable(R.drawable.ic_usb);
      this.status = new Status("", statusDrawable, context.getPackageName());
   }

   /**
    * Utility function to send status notification
    *
    * @param message notification message
    * @param exporting  true if it's export process
    */
   public void sendStatus(String message, boolean exporting) {
      StringBuilder stringBuilder = new StringBuilder();
      if (exporting) {
         stringBuilder.append(context.getResources().getString(R.string.exporting_string));
      }
      else {
         stringBuilder.append(context.getResources().getString(R.string.importing_string));
      }
      stringBuilder.append(" ");
      stringBuilder.append(message);

      status.setMessage(stringBuilder.toString());
      removeStatus();

      Intent intent = new Intent(Status.ACTION_STATUS_DISPLAY).putExtra(Status.NAME, status);
      context.sendBroadcast(intent);
   }

   /**
    * Send starting status notification
    *
    * @param exporting  true if it's export process
    */
   public void sendStartingStatus(boolean exporting) {
      sendStatus(context.getResources().getString(R.string.status_starting), exporting);
   }

   /**
    * Send cancelling status notification
    *
    * @param exporting  true if it's export process
    */
   public void sendCancellingStatus(boolean exporting) {
      sendStatus(context.getResources().getString(R.string.status_cancelling), exporting);
   }

   /**
    * Send cancelled status notification
    *
    * @param exporting  true if it's export process
    */
   public void sendCancelledStatus(boolean exporting) {
      sendStatus(context.getResources().getString(R.string.status_cancelled), exporting);
   }

   /**
    * Send success status notification
    *
    * @param exporting  true if it's export process
    */
   public void sendSuccessfulStatus(boolean exporting) {
      sendStatus(context.getResources().getString(R.string.status_successful), exporting);
   }

   /**
    * Remove status notification
    */
   public void removeStatus() {
      Intent intent = new Intent(Status.ACTION_STATUS_REMOVE).putExtra(Status.ID, ParcelUuid.fromString(status.getID().toString()));
      context.sendBroadcast(intent);
   }

   /**
    * Get Status
    */
   public Status getStatus() {
      return status;
   }
}
