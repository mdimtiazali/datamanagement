/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TreeEntityHelper;

import java.util.List;
/**
 * Adapter to show the data item list
 * @author Lifeng Liang
 */
public class DialogListViewAdapter extends BaseAdapter{
    private List<ObjectGraph> data;
    private Context context;
    private LayoutInflater inflater;
    /**
     * Adapter construction
     *
     * @param context context
     * @param inflater view inflater
     * @param data list of ObjectGraph data to show
     */
    public DialogListViewAdapter(Context context, LayoutInflater inflater,List<ObjectGraph> data) {
        this.context = context;
        this.inflater = inflater;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if(view == null){
            viewHolder = new ViewHolder();
            view = inflater.inflate(R.layout.object_item_in_dialog,null);
            viewHolder.imageView = (ImageView)view.findViewById(R.id.data_icon);
            viewHolder.tvContent = (TextView)view.findViewById(R.id.data_string);
            view.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder)view.getTag();
        }
        ObjectGraph objectGraph = data.get(i);
        Integer icon_id = TreeEntityHelper.getIcon(objectGraph.getType());
        if(icon_id != null) {
            viewHolder.imageView.setImageResource(icon_id);
        }
        else{
            viewHolder.imageView.setImageResource(R.drawable.ic_app_icon);
        }
        viewHolder.tvContent.setText(objectGraph.getName());
        return view;
    }
    class ViewHolder {
        ImageView imageView;
        TextView tvContent;
    }
}
