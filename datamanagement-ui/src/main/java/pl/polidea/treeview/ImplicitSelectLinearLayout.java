/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package pl.polidea.treeview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import com.cnh.pf.android.data.management.R;

/**
 * @author kedzie
 */
public class ImplicitSelectLinearLayout extends LinearLayout implements ImplicitlySelectable {

      private static final int[] DRAWABLE_STATE_IMPLICIT = { R.attr.state_implicit };

      private static final int[] DRAWABLE_STATE_SUPPORTED = { R.attr.state_supported};

      private boolean implicitlySelected;
      private boolean supported = true;

      public ImplicitSelectLinearLayout(Context context) {
            super(context);
      }

      public ImplicitSelectLinearLayout(Context context, AttributeSet attrs) {
            super(context, attrs);
      }

      public ImplicitSelectLinearLayout(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
      }

      @Override
      public void setImplicitlySelected(boolean selected) {
            boolean changed = implicitlySelected != selected;
            implicitlySelected = selected;
            if(changed) refreshDrawableState();
      }

      @Override
      public void setSupported(boolean supported) {
            boolean changed = this.supported != supported;
            this.supported = supported;
            if(changed) refreshDrawableState();
      }

      @Override
      public boolean isSupported() {
            return supported;
      }

      @Override
      public boolean isImplicitlySelected() {
            return implicitlySelected;
      }

      @Override
      protected int[] onCreateDrawableState(int extraSpace) {
            int[] state = super.onCreateDrawableState(extraSpace+2);
            if(implicitlySelected)
                  mergeDrawableStates(state, DRAWABLE_STATE_IMPLICIT);
            if(supported)
                  mergeDrawableStates(state, DRAWABLE_STATE_SUPPORTED);
            return state;
      }
}
