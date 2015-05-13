/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.android.data.management.adapter;

import android.app.Activity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.cnh.android.data.management.R;
import pl.polidea.treeview.AbstractTreeViewAdapter;
import pl.polidea.treeview.TreeNodeInfo;
import pl.polidea.treeview.TreeStateManager;
import com.cnh.jgroups.ObjectGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter feeds data to TreeView
 * Created by oscar.salazar@cnhind.com
 */
public class ObjectTreeViewAdapater extends AbstractTreeViewAdapter<ObjectGraph> {
   private static final Logger logger = LoggerFactory.getLogger(ObjectTreeViewAdapater.class);

   private final Set<ObjectGraph> selected;
   private List<ObjectGraph> data;

   public ObjectTreeViewAdapater(Activity activity, TreeStateManager treeStateManager, int numberOfLevels) {
      super(activity, treeStateManager, numberOfLevels);
      selected = new HashSet<ObjectGraph>();
   }

   private final CompoundButton.OnCheckedChangeListener onCheckedChange = new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(final CompoundButton buttonView,
                                   final boolean isChecked) {
         final ObjectGraph id = (ObjectGraph) buttonView.getTag();
         changeSelected(isChecked, id);
      }

   };

   private void changeSelected(final boolean isChecked, final ObjectGraph id) {
      logger.debug("changeSelected: " +id.getName());
      if (isChecked) {
         selected.add(id);
         //TODO recursively select children
//         setChildrenSelected(id);
      } else {
         selected.remove(id);
      }
   }

   private void setChildrenSelected(ObjectGraph id) {
      if (!id.getChildren().isEmpty()) {
         //TODO recursively select children
         for (ObjectGraph child : id.getChildren()) {
            setChildrenSelected(child);
         }
      }
      //TODO set TreeNode selected
   }

   @Override
   public void handleItemClick(View view, Object id) {
      super.handleItemClick(view, id);
      logger.debug("handleItemClick");
   }

   @Override
   public View getNewChildView(TreeNodeInfo treeNodeInfo) {
      final LinearLayout viewLayout = (LinearLayout) getActivity()
              .getLayoutInflater().inflate(R.layout.tree_list_item, null);
      return updateView(viewLayout, treeNodeInfo);
   }

   @Override
   public View updateView(View view, TreeNodeInfo treeNodeInfo) {
      final LinearLayout viewLayout = (LinearLayout) view;
      final TextView levelView = (TextView) viewLayout
              .findViewById(R.id.demo_list_item_level);
      final TextView nameView = (TextView) viewLayout.findViewById(R.id.item_name);
      final CheckBox itemBox = (CheckBox) viewLayout.findViewById(R.id.item_checkbox);
      ObjectGraph graph = (ObjectGraph) treeNodeInfo.getId();
      levelView.setText(Integer.toString(treeNodeInfo.getLevel()));
      nameView.setText(graph.getName());
      itemBox.setTag(graph);
      itemBox.setOnCheckedChangeListener(onCheckedChange);
      return viewLayout;
   }

   @Override
   public long getItemId(int position) {
      return 0;
   }

   /**
    * Save all node data for tree
    * @param data Object Data
    */
   public void setData(List<ObjectGraph> data) {
      this.data = data;
   }

   /**
    * Grab all Tree View origin data
    * @return Tree Origin Data
    */
   public List<ObjectGraph> getData() {
      return data;
   }

   /**
    * Get Set of selected ObjectGraphs
    * @return Set<ObjectGraph>
    */
   public Set<ObjectGraph> getSelected() {
      return selected;
   }
}
