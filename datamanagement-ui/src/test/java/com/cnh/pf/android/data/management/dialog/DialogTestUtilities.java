/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management.dialog;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.cnh.pf.android.data.management.R;

import android.view.View;
import android.widget.Button;

/**
 * Static class, contains testMethods for com.cnh.android.dialog.DialogView
 */
public class DialogTestUtilities {
   private DialogTestUtilities() {
   }

   /**
    * Checks the Visibility of the 3 buttons that are available at each DialogView
    * @param btn1
    * @param btn2
    * @param btn3
    * @param view
    */
   public static void checkVisiblityOfDialogButtons(int btn1, int btn2, int btn3, View view) {
      Button firstButton = (Button) view.findViewById(R.id.btFirst);
      Button secondButton = (Button) view.findViewById(R.id.btSecond);
      Button thirdButton = (Button) view.findViewById(R.id.btThird);

      assertThat(firstButton.getVisibility(), is(btn1));
      assertThat(secondButton.getVisibility(), is(btn2));
      assertThat(thirdButton.getVisibility(), is(btn3));
   }
}
