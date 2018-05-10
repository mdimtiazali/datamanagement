/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

/**
 * Exception that carries session error code.
 *
 * @author: junsu.shin@cnhind.com
 */
public class SessionException extends Exception {
   private final ErrorCode errorCode;

   public SessionException(ErrorCode code) {
      errorCode = code;
   }

   public ErrorCode getErrorCode() {
      return errorCode;
   }
}
