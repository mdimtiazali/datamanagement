/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.adapter;

/**
 * Interface used to call a filter a second time which shall use the old CharSequence to filter new lists.
 *
 * @author waldschmidt
 */
interface UpdateableFilter {

   /**
    * Throws away temporary lists and re triggers the filtering.
    */
   void updateFiltering();
}
