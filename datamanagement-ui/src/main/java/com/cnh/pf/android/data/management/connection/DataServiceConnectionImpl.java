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

import com.cnh.pf.data.management.DataManagementSession;

/**
 * Service must handle its own connection to service. Must provide Service connection and status.
 * Service can fire {@link ConnectionEvent}, and use {@link roboguice.activity.event.OnResumeEvent} and {@link roboguice.activity.event.OnPauseEvent}
 * for handling connection.
 * @author oscar.salazar@cnhind.com
 */
public interface DataServiceConnectionImpl {
   public boolean isConnected();

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
}
