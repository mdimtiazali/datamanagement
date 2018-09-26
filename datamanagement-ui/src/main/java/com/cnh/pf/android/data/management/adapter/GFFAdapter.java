/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import com.cnh.jgroups.DataTypes;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.TreeEntityHelper;
import com.cnh.pf.android.data.management.graph.GFFObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GFFAdapter extends BaseAdapter {
    private static final Logger log = LoggerFactory.getLogger(GFFAdapter.class);
    private final int GROWER =0;
    private final int FARM = 1;
    private final int FIELD = 2;
    private final int TYPE_COUNT = 3;

    private LayoutInflater inflater;
    private Context context;
    private List<GFFObject> data;
    private volatile int selectedPosition = -1;

    public GFFAdapter(Context context, List<GFFObject> data){
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data!=null?data.size():0;
    }

    @Override
    public Object getItem(int i) {
        return data != null?data.get(i):null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        switch (getItemViewType(i)){
            case GROWER:
                GrowerViewHolder growerViewHolder = null;
                if(view == null){
                    growerViewHolder = new GrowerViewHolder();
                    view = inflater.inflate(R.layout.grower_item,null);
                    growerViewHolder.grower = (TextView)view.findViewById(R.id.grower_name);
                    view.setTag(growerViewHolder);
                }
                else{
                    growerViewHolder = (GrowerViewHolder)view.getTag();
                }
                growerViewHolder.grower.setText(data.get(i).getName());
                break;
            case FARM:
                FarmViewHolder farmViewHolder;
                if(view == null){
                    farmViewHolder = new FarmViewHolder();
                    view = inflater.inflate(R.layout.farm_item,null);
                    farmViewHolder.farm = (TextView)view.findViewById(R.id.farm_name);
                    farmViewHolder.icon = (TextView)view.findViewById(R.id.icon);
                    view.setTag(farmViewHolder);
                }
                else{
                    farmViewHolder = (FarmViewHolder) view.getTag();
                }
                farmViewHolder.icon.setCompoundDrawablesWithIntrinsicBounds(TreeEntityHelper.getIcon(data.get(i).getType()), 0, 0, 0);
                farmViewHolder.farm.setText(data.get(i).getName());
                break;
            case FIELD:
                FieldViewHolder fieldViewholder;
                if(view == null){
                    fieldViewholder = new FieldViewHolder();
                    view = inflater.inflate(R.layout.field_item,null);
                    fieldViewholder.radioButton = (RadioButton) view.findViewById(R.id.checkbox);
                    fieldViewholder.icon = (ImageView) view.findViewById(R.id.field_icon);
                    fieldViewholder.name = (TextView) view.findViewById(R.id.field_name);
                    view.setTag(fieldViewholder);
                }
                else{
                    fieldViewholder = (FieldViewHolder) view.getTag();
                }
                fieldViewholder.icon.setImageResource(TreeEntityHelper.getIcon(data.get(i).getType()));
                fieldViewholder.name.setText(data.get(i).getName());
                log.debug("the selectedPosition is {}",selectedPosition);
                fieldViewholder.radioButton.setButtonDrawable(R.drawable.extended_radio_button_selector);
                fieldViewholder.radioButton.setChecked(i == selectedPosition? true:false);
                break;
            default:
                break;
        }
        return view;
    }

    @Override
    public int getItemViewType(int position) {
        GFFObject obj = (GFFObject)getItem(position);
        if(obj.getType().equals(DataTypes.GROWER)){
            return GROWER;
        }
        else if(obj.getType().equals(DataTypes.FARM)){
            return FARM;
        }
        else if(obj.getType().equals(DataTypes.FIELD)){
            return FIELD;
        }
        return -1;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        if(selectedPosition != this.selectedPosition){
            this.selectedPosition = selectedPosition;
            notifyDataSetChanged();
        }
    }
    public boolean isField(int position){
        return data.get(position).getType().equals(DataTypes.FIELD);
    }
    class GrowerViewHolder{
        TextView grower;
    }
    class FarmViewHolder{
        TextView icon;
        TextView farm;
    }
    class FieldViewHolder{
        RadioButton radioButton;
        ImageView icon;
        TextView name;
    }
}
