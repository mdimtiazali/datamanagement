/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;
import com.cnh.pf.android.data.management.adapter.SimpleTreeViewAdaptor;

import java.util.List;
/**
 * Simple tree view, simple to show a tree view to present one/none folder and child(ren).
 */
public class SimpleTreeView extends ListView {
    private SimpleTreeViewAdaptor adaptor;

    public SimpleTreeView(Context context) {
        super(context);
    }

    /**
     * construct
     * @param context
     * @param attrs
     */
    public SimpleTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * load the data to tree view
     * @param folderIconId
     * @param folderName
     * @param icons
     * @param data
     */
    public void setItems(Integer folderIconId, String folderName, List<Integer> icons, List<String> data){
        if(folderName != null){
            icons.add(0,folderIconId);
            data.add(0,folderName);
        }
        adaptor = new SimpleTreeViewAdaptor(getContext(), icons, data);
        setAdapter(adaptor);
    }

    /**
     * Getter for adapter count
     * @return  adapter count
     */
    public int getAdaptorCount() {
        return adaptor.getCount();
    }
}
