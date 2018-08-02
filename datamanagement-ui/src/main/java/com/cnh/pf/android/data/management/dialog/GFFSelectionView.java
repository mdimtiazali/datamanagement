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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.cnh.android.dialog.DialogView;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.adapter.GFFAdapter;
import com.cnh.pf.android.data.management.graph.GFFObject;

import java.util.List;

public class GFFSelectionView extends DialogView{
    private List<GFFObject> data;
    private LayoutInflater inflater;
    private GFFAdapter adapter;

    public GFFSelectionView(Context context, List<GFFObject> data, ObjectGraph object) {
        super(context);
        this.data = data;
        inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.gff_selection_dialog,null);
        TextView header = (TextView)view.findViewById(R.id.header);
        String headline = String.format(getResources().getString(R.string.copy_paste_header),object.getName());
        header.setText(Html.fromHtml(headline));
     //   header.setText(String.format(getResources().getString(R.string.copy_paste_header),object.getName()));
        adapter = new GFFAdapter(context, data);
        ListView listView = (ListView)view.findViewById(R.id.gff_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(adapter.isField(i)) {
                    adapter.setSelectedPosition(i);
                    disableButtonFirst(false);
                }

            }
        });
        setContentPaddings(0,0,0,32);
        setFirstButtonText(getResources().getString(R.string.ok));
        showFirstButton(true);
        disableButtonFirst(true);
        setSecondButtonText(getResources().getString(R.string.cancel));
        showSecondButton(true);
        showThirdButton(false);
        setBodyView(view);

    }

    public GFFObject getSelected() {
        int selected = adapter.getSelectedPosition();
        if(selected != -1) {
            return data.get(adapter.getSelectedPosition());
        }
        return null;
    }
}
