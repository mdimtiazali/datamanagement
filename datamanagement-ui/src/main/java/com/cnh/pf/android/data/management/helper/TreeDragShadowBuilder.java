/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.pf.android.data.management.adapter.ObjectTreeViewAdapter;
import pl.polidea.treeview.TreeViewList;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws all selected nodes.
 * Created by mkedzierski on 1/12/16.
 */
public class TreeDragShadowBuilder extends View.DragShadowBuilder {

   private TreeViewList listView;
   private ObjectTreeViewAdapter treeAdapter;

   public TreeDragShadowBuilder(View view, TreeViewList listView, ObjectTreeViewAdapter treeAdapter) {
      super(listView);
      this.listView = listView;
      this.treeAdapter = treeAdapter;
   }

   private List<View> getViews() {
      List<View> views = new ArrayList<View>();
      for(int i=0; i<listView.getChildCount(); i++) {
         View child = listView.getChildAt(i);
         ObjectGraph node = (ObjectGraph) child.getTag(); //tree associates ObjectGraph with each view
         if(node==null) continue;
         if(treeAdapter.getSelectionMap().containsKey(node)) {
            views.add(child);
         }
      }
      return views;
   }

   @Override public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
      List<View> views = getViews();

      int vWidth = getView().getWidth();
      int vHeight = getView().getHeight();

      int index = views.indexOf(getView());

      shadowSize.set(vWidth, vHeight* views.size());
      shadowTouchPoint.set(shadowSize.x / 2, index*vHeight + vHeight / 2);
   }

   @Override public void onDrawShadow(Canvas canvas) {
      for(View view : getViews()) {
         view.draw(canvas);
         canvas.translate(0f, view.getHeight());
      }
   }
}
