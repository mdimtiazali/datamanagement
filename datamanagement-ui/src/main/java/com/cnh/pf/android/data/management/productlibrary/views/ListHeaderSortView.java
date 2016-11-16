/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.cnh.pf.android.data.management.R;

//TODO: instead of copy-paste from coreapks com.cnh.android.core.component.fault.FaultListHeaderSortButton, use library?

/**
 * ListHeaderSortView
 * Sort buttons along the header of an ExpandableListView
 */
public class ListHeaderSortView extends Button {
   private int currentState = 2;
   public static final int STATE_SORT_DESC = 0;
   public static final int STATE_SORT_ASC = 1;
   public static final int STATE_NO_SORT = 2;
   private static final int[] ATTRS_STATE_SORT_DESC = { R.attr.state_sort_desc };
   private static final int[] ATTRS_STATE_SORT_ASC = { R.attr.state_sort_asc };
   private static final int[] ATTRS_STATE_NO_SORT = { R.attr.state_no_sort };

   public ListHeaderSortView(Context context) {
      super(context);
   }

   public ListHeaderSortView(Context context, AttributeSet attrs) {
      super(context, attrs);
   }

   public ListHeaderSortView(Context context, AttributeSet attrs, int defStyle) {
      super(context, attrs, defStyle);
   }

   /**
    * Manually
    * @return result of click event
    */
   public boolean performClick() {
      nextState();
      return super.performClick();
   }

   protected int[] onCreateDrawableState(int extraSpace) {
      int[] drawableState = super.onCreateDrawableState(extraSpace + 3);
      if (this.currentState == STATE_SORT_ASC) {
         mergeDrawableStates(drawableState, ATTRS_STATE_SORT_ASC);
      }
      else if (this.currentState == STATE_SORT_DESC) {
         mergeDrawableStates(drawableState, ATTRS_STATE_SORT_DESC);
      }
      else if (this.currentState == STATE_NO_SORT) {
         mergeDrawableStates(drawableState, ATTRS_STATE_NO_SORT);
      }
      return drawableState;
   }

   /**
    * Set sort button state
    * @param state
    */
   public void setState(int state) {
      if ((state >= STATE_SORT_ASC) && (state <= STATE_NO_SORT)) {
         this.currentState = state;
         refreshDrawableState();
      }
   }

   /**
    * Retrieve sort button state
    * @return
    */
   public int getState() {
      return this.currentState;
   }

   /**
    * Increment sort button state
    * Only allows ascending or descending as states
    */
   public void nextState() {
      this.currentState += 1;
      if (this.currentState > STATE_SORT_ASC) {
         this.currentState = STATE_SORT_DESC;
      }
   }

   /**
    * Decrement sort button state
    * Only allows ascending or descending as states
    */
   public void previousState() {
      this.currentState -= 1;
      if (this.currentState < STATE_SORT_DESC) {
         this.currentState = STATE_SORT_ASC;
      }
   }
}
