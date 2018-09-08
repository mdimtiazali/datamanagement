/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.dialog;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.cnh.android.dialog.DialogView;
import com.cnh.pf.android.data.management.R;
import com.google.inject.Inject;
import roboguice.RoboGuice;

/**
 * Dialog view to show delete confirmation and gets user feedback on OK/Cancel.
 */
public class DeleteDialog extends DialogView {
   @Inject
   LayoutInflater layoutInflater;
   @Bind(R.id.delete_confirm_tv)
   TextView delete_confirm_content;

   public DeleteDialog(Context context, int number) {
      super(context);
      RoboGuice.getInjector(context).injectMembers(this);
      View view = layoutInflater.inflate(R.layout.delete_confirm_layout, null);
      ButterKnife.bind(this, view);
      setTitle(getResources().getQuantityString(R.plurals.delete_confirm_dialog_title, number,number));
      delete_confirm_content.setText(Html.fromHtml(getResources().getQuantityString(R.plurals.delete_confirm_dialog_content, number,number)));
      setBodyView(view);
      setFirstButtonText(getResources().getString(R.string.delete_dialog_confirm_button_text));
      showFirstButton(true);
      setSecondButtonText(getResources().getString(R.string.cancel));
      showSecondButton(true);
      showThirdButton(false);
      setDialogWidth(context.getResources().getDimensionPixelSize(R.dimen.delete_dialog_width));
   }
}
