/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.utility;

import android.content.Context;
import android.view.View;
import com.cnh.pf.android.data.management.R;

/**
 * Class for small helper methods which are used by productLibraryFragment but are better placed outside of it.
 * Maybe it's possible to place the class somewhere else later.
 */
public class UiHelper {

   /**
    * Sets the background for a view - alternating two colors depending on the groupOrPositionId.
    * @param context the context
    * @param groupOrPositionId the group id or position inside a list
    * @param view the view the background should be set for
    */
   public static void setAlternatingTableItemBackground(Context context, int groupOrPositionId, View view) {
      if (groupOrPositionId > 0 && groupOrPositionId % 2 != 0) {
         view.setBackgroundColor(context.getResources().getColor(R.color.odd_rows));
      }
      else {
         view.setBackgroundColor(context.getResources().getColor(R.color.even_rows));
      }
   }
}
