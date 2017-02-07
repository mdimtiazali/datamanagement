/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl;
import com.google.common.base.Strings;
import com.google.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import roboguice.RoboGuice;

/**
 * DialogView shows backend errors to user
 * @author oscar.salazar@cnhind.com
 */
public class ErrorDialog extends DialogView {

   @Inject
   LayoutInflater layoutInflater;
   @Bind(R.id.error_string)
   TextView errorString;

   public ErrorDialog(Context context, DataServiceConnectionImpl.ErrorEvent event) {
      super(context);
      RoboGuice.getInjector(context).injectMembers(this);
      View view = layoutInflater.inflate(R.layout.error_layout, null);
      ButterKnife.bind(this, view);

      this.setTitle(event.getType().toString());
      errorString.setText(Strings.isNullOrEmpty(event.getError()) ? "No Details" : event.getError());
      setBodyView(view);
      setFirstButtonText(getResources().getString(R.string.cancel));
      showFirstButton(true);
      showSecondButton(false);
      showThirdButton(false);
   }
}
