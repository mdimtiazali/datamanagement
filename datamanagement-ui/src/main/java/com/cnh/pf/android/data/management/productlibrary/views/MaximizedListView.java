/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * ListView with maximum size.
 *
 * Remark: The onMeasure is copied from NestedExpandableListView. Setting the height to wrap_content is not working
 * as expected. The list has the height of one element then.
 *
 * @author waldschmidt
 */
public class MaximizedListView extends ListView {

   private static final int MEASURE_SPECIFICATION_SIZE = 99999;

   public MaximizedListView(Context context, AttributeSet attrs) {
      super(context, attrs);
   }

   @Override
   protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MEASURE_SPECIFICATION_SIZE, MeasureSpec.AT_MOST));
   }
}