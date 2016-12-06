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
 * I don't understand why i don't get a ListView to show everything inside but this extension/override helps
 * @author waldschmidt
 */
public class MaximizedListView extends ListView {

   public MaximizedListView(Context context, AttributeSet attrs) {
      super(context, attrs);
   }

   @Override
   protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(99999, MeasureSpec.AT_MOST));
   }
}