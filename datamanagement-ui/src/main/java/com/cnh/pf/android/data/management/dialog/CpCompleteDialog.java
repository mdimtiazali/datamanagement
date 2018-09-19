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
        notice.setText(getResources().getString(R.string.cp_complete_content,field));
        SimpleTreeView treeView = (SimpleTreeView)view.findViewById(R.id.simpletreeview);
        treeView.setItems(folderIconId, folderName,ids,names);
        setTitle(getResources().getString(R.string.copy));
        setBodyView(view);
        setFirstButtonText(getResources().getString(R.string.done));
        showFirstButton(true);
        showSecondButton(false);
        showThirdButton(false);
        setDialogWidth(getResources().getInteger(R.integer.copy_paste_dialog_width));
        setBodyHeight(names.size() < getResources().getInteger(R.integer.copy_paste_dialog_high_threld) ? getResources().getInteger(R.integer.copy_paste_dialog_height_low)
                :getResources().getInteger(R.integer.copy_paste_dialog_height_high));
    }
}