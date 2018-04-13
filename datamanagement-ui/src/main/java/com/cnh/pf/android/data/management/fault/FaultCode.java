/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.fault;

/**
 * Represents fault codes concerning data management alerts
 *
 * @author: junsu.shin@cnhind.com
 */
public enum FaultCode {
   USB_NOT_ENOUGH_MEMORY ("A-PF-170"),
   USB_REMOVED_DURING_EXPORT ("A-PF-171"),
   USB_REMOVED_DURING_IMPORT ("A-PF-172");

   // The actual fault code in string format
   private final String code;

   FaultCode(String code) {
      this.code = code;
   }

   // getter method for the string code
   public String getCode() {
      return this.code;
   }

   @Override
   public String toString() {
      return this.code;
   }
}
