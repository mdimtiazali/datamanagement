/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.resolver;

import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;

/**
 * Provides an interface to resolve session property data.
 *
 * @author: junsu.shin@cnhind.com
 */
public interface Resolver {
   /**
    * Resolve session property data (sources & destinations, etc) to execute
    * session task.
    *
    * @param session    the session object
    * @throws SessionException
    */
   void resolve(Session session) throws SessionException;
}
