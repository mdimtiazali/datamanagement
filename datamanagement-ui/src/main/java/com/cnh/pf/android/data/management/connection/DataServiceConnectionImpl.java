/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.connection;

import android.app.Service;

import com.cnh.pf.android.data.management.R;
import com.cnh.pf.data.management.DataManagementSession;
import com.google.common.base.MoreObjects;

import org.jgroups.View;

/**
 * Service must handle its own connection to service. Must provide Service connection and status.
 * Service can fire {@link ConnectionEvent}, and use {@link roboguice.activity.event.OnResumeEvent} and {@link roboguice.activity.event.OnPauseEvent}
 * for handling connection.
 * @author oscar.salazar@cnhind.com
 */
public interface DataServiceConnectionImpl {
   boolean isConnected();

   interface ConnectionListener {
      public void onConnectionChange(boolean connected);
   }

   class ConnectionEvent {
      private boolean connected;
      private Service service;

      public ConnectionEvent(boolean connected, Service service) {
         this.connected = connected;
         this.service = service;
      }

      public Service getService() {
         return service;
      }

      public boolean isConnected() {
         return connected;
      }
   }

   class DataSessionEvent {
      private DataManagementSession session;

      public DataSessionEvent(DataManagementSession session) {
         this.session = session;
      }

      public DataManagementSession getSession() {
         return session;
      }
   }

   class ProgressEvent {
      String operation;
      int progress;
      int max;

      public ProgressEvent(String operation, int progress, int max) {
         this.operation = operation;
         this.progress = progress;
         this.max = max;
      }

      public String getOperation() {
         return operation;
      }

      public int getMax() {
         return max;
      }

      public int getProgress() {
         return progress;
      }
   }

   class ErrorEvent {
      private DataManagementSession session;
      private String error;
      private DataError type;

      public enum DataError {
         NO_SOURCE_DATASOURCE(R.string.error_no_source, "No Source Datasource"), NO_DATA(R.string.error_no_source, "No DATA"), NO_TARGET_DATASOURCE(R.string.error_no_target,
               "No Target Datasource"), CALCULATE_CONFLICT_ERROR(R.string.error_calculate_conflicts,
                     "Calculate Conflict Error"), CALCULATE_TARGETS_ERROR(R.string.error_calculate_operations, "Calculate Targets Error"), DISCOVERY_ERROR(R.string.error_discovery,
                           "Discovery Error"), PERFORM_ERROR(R.string.error_perform_ops, "Perform Operations Error"), NEED_DATA_PATH(R.string.error_no_source, "Please Specify A Valid Data Path To Start");

         private String value;
         private int res;

         DataError(int res, String v) {
            this.res = res;
            this.value = v;
         }

         public int resource() {
            return res;
         }

         public String value() {
            return value;
         }

         @Override
         public String toString() {
            return value;
         }
      }

      public ErrorEvent(DataManagementSession session, DataError type) {
         this(session, type, type.value());
      }

      public ErrorEvent(DataManagementSession session, DataError type, String error) {
         this.session = session;
         this.type = type;
         this.error = error;
      }

      public DataManagementSession getSession() {
         return session;
      }

      public String getError() {
         return error;
      }

      public DataError getType() {
         return type;
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this).add("session", session).add("error", error).add("type", type).toString();
      }
   }

   class ViewChangeEvent {
      private View newView;
      private View oldView;

      public ViewChangeEvent(View oldView, View newView) {
         this.newView = newView;
         this.oldView = oldView;
      }

      public View getNewView() {
         return newView;
      }

      public View getOldView() {
         return oldView;
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this).add("oldView", oldView).add("newView", newView).toString();
      }
   }
}
