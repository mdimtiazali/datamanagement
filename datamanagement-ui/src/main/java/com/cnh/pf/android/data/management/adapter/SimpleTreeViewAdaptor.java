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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.misc.TreeLineWidget;
import java.util.List;
/**
 * SimpleTreeView adaptor.
 */
public class SimpleTreeViewAdaptor extends BaseAdapter {
    private List<String> names;
    private List<Integer> ids;
    private Context context;
    private int size;
    public SimpleTreeViewAdaptor(Context context, List<Integer> ids, List<String> names) {
        this.ids = ids;
        this.names = names;
        this.context = context;
        this.size = names.size()>=ids.size()?ids.size():names.size();
    }

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int i) {
        return names.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if(view == null){
            view = View.inflate(context, R.layout.simple_treeview_item,null);
            holder = new ViewHolder();
            holder.line = (TreeLineWidget)view.findViewById(R.id.line);
            holder.icon = (ImageView)view.findViewById(R.id.icon);
            holder.content = (TextView)view.findViewById(R.id.content);
            view.setTag(holder);
        }
        else {
            holder = (ViewHolder)view.getTag();
        }
        if(i == 0){
            holder.line.setVisibility(View.GONE);
        }
        else{
            holder.line.setVisibility(View.VISIBLE);
        }
        if(i == (size-1)){
            holder.line.setCornerFlag(true);
        }
        else{
            holder.line.setCornerFlag(false);
        };
        holder.icon.setImageResource(ids.get(i));
        holder.content.setText(names.get(i));
        return view;
    }

    class ViewHolder{
        public TreeLineWidget line;
        public ImageView icon;
        public TextView content;
    }
}

