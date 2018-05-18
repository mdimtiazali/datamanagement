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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import butterknife.ButterKnife;
import com.cnh.android.dialog.DialogView;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.adapter.DialogListViewAdapter;

import java.util.List;
/**
 * Dialog view to show the undeleted object list
 * @author lifeng.liang@cnhind.com
 */
public class UndeleteObjectDialog extends DialogView {
    private LayoutInflater layoutInflater;
    /**
     * constrctor
     * @param  context
     * @param  objectGraphs undeleted object list to show
     */
    public UndeleteObjectDialog(Context context, List<ObjectGraph> objectGraphs) {
        super(context);
        this.layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.undelete_obj_dialog, null);
        ButterKnife.bind(this, view);
        ListView listView = (ListView)view.findViewById(R.id.object_list);
        listView.setAdapter(new DialogListViewAdapter(context, layoutInflater,objectGraphs));
        setBodyView(view);
        setFirstButtonText(getResources().getString(R.string.ok));
        showFirstButton(true);
        setSecondButtonText(getResources().getString(R.string.cancel));
        showSecondButton(false);
        showThirdButton(false);
    }
}
