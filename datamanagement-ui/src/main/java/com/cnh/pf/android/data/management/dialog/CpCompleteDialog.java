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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;
import com.cnh.android.dialog.DialogView;
import com.cnh.pf.android.data.management.R;
import java.util.List;
/**
 * Dialog view to show cp complete information.
 */
public class CpCompleteDialog extends DialogView {

    public CpCompleteDialog(Context context, String field, int folderIconId, String folderName, List<Integer> ids, List<String> names) {
        super(context);
        View view = View.inflate(context, R.layout.cp_complete_dialog, null);
        TextView notice = (TextView) view.findViewById(R.id.notice);
        View v = view.findViewById(R.id.span);
        if(names.size() > 3){
            v.setVisibility(View.GONE);
        }

        notice.setText(getResources().getString(R.string.cp_complete_content,field));
        SimpleTreeView treeView = (SimpleTreeView)view.findViewById(R.id.simpletreeview);
        TextView singleItemView = (TextView)view.findViewById(R.id.singleitem);
        if(names.size() == 1){
            treeView.setVisibility(View.GONE);
            ImageSpan imgSpan = new ImageSpan(context, ids.get(0));
            String temp = "12  "+names.get(0);
            SpannableString spannableString = new SpannableString(temp);
            spannableString.setSpan(imgSpan, 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            singleItemView.setText(spannableString);
        }
        else{
            singleItemView.setVisibility(View.GONE);
            treeView.setItems(folderIconId, folderName,ids,names);
        }

        setTitle(getResources().getString(R.string.copy));
        setContentPaddings(0,0,0,0);
        setBodyView(view);
        setFirstButtonText(getResources().getString(R.string.done));
        showFirstButton(true);
        showSecondButton(false);
        showThirdButton(false);
        setDialogWidth(getResources().getInteger(R.integer.copy_paste_dialog_width));
        setBodyHeight(getResources().getInteger(R.integer.copy_paste_dialog_height_high));
    }
}