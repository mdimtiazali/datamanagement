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

/**
 * Dummy resolver that does nothing.
 *
 * @author: junsu.shin@cnhind.com
 */
public class DummyResolver implements Resolver {
   @Override
   public void resolve(Session session) {
      // Do nothing
   }
}
