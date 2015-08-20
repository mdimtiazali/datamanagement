/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.jgroups.data.service.aidl;

import com.cnh.jgroups.data.service.DataManagementSession;

interface IDataManagementListenerAIDL{
   void onProgressUpdated();
   void onDataSessionUpdated(in DataManagementSession session);
}