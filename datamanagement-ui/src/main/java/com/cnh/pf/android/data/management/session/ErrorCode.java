/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import com.cnh.pf.android.data.management.R;

/**
 * Enum class to define all potential error cases for the data managemement.
 */
public enum ErrorCode {
   NO_SOURCE_DATASOURCE(R.string.error_no_source, "No Source Datasource"),
   NO_DATA(R.string.error_no_source, "No DATA"),
   NO_DESTINATION_DATASOURCE(R.string.error_no_target, "No Destination Datasource"),
   CALCULATE_CONFLICT_ERROR(R.string.error_calculate_conflicts, "Calculate Conflict Error"),
   CALCULATE_OPERATIONS_ERROR(R.string.error_calculate_operations, "Calculate Targets Error"),
   DISCOVERY_ERROR(R.string.error_discovery, "Discovery Error"),
   PERFORM_ERROR(R.string.error_perform_ops, "Perform Operations Error"),
   UPDATE_ERROR(R.string.error_update, "Update Error"),
   DELETE_ERROR(R.string.error_delete, "Delete Error"),
   NEED_DATA_PATH(R.string.error_no_source, "Please Specify A Valid Data Path To Start"),
   INVALID_FORMAT(R.string.error_perform_ops, "Invalid Format."),
   USB_REMOVED(R.string.error_perform_ops, "USB Removal During Process"),
   PASTE_ERROR(R.string.error_paste, "Paste Error");


   private String value;
   private int res;

   ErrorCode(int res, String v) {
      this.res = res;
      this.value = v;
   }

   /**
    * Return string resource ID for error code
    * @return  string resource ID
    */
   public int resource() {
      return res;
   }

   /**
    * Return error code description in string
    * @return  error code description
    */
   public String value() {
      return value;
   }

   @Override
   public String toString() {
      return value;
   }
}