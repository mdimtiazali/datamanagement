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
import com.cnh.jgroups.data.service.aidl.IDataManagementListenerAIDL;

interface IDataManagementServiceAIDL {
   void register(String name, IDataManagementListenerAIDL listener);
   void unregister(String name);
   DataManagementSession getSession();
   void processOperation(in DataManagementSession session, int sessionOperation);

}