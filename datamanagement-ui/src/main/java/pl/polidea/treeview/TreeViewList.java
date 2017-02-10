/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package pl.polidea.treeview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.cnh.pf.android.data.management.R;

import java.lang.reflect.Field;

/**
 * Tree view, expandable multi-level.
 * 
 * <pre>
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_collapsible
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_src_expanded
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_src_collapsed
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_indent_width
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_handle_trackball_press
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_indicator_gravity
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_indicator_background
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_row_background
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_draw_line
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_line_color
 * attr ref pl.polidea.treeview.R.styleable#TreeViewList_line_width
 * </pre>
 */
public class TreeViewList extends ListView {
   private static final int DEFAULT_COLLAPSED_RESOURCE = R.drawable.collapsed;
   private static final int DEFAULT_EXPANDED_RESOURCE = R.drawable.expanded;
   private static final int DEFAULT_INDENT = 0;
   private static final int DEFAULT_LINE_WIDTH = 2;
   private static final int DEFAULT_GRAVITY = Gravity.LEFT | Gravity.CENTER_VERTICAL;
   private Drawable expandedDrawable;
   private Drawable collapsedDrawable;
   private Drawable rowBackgroundDrawable;
   private Drawable indicatorBackgroundDrawable;
   private int indentWidth = 0;
   private int indicatorGravity = 0;
   private AbstractTreeViewAdapter<?> treeAdapter;
   private boolean collapsible;
   private boolean handleTrackballPress;
   private Paint linePaint = new Paint();
   private boolean drawLine;
   private static Field fFirstPosition;
   private Rect mTempRect = new Rect();

   static {
      try {
         fFirstPosition = AdapterView.class.getDeclaredField("mFirstPosition");
         fFirstPosition.setAccessible(true);
      }
      catch (NoSuchFieldException e) {
         Log.e("TreeViewList", "Error", e);
      }
   }

   private Integer getFirstPosition() {
      try {
         return (Integer) fFirstPosition.get(this);
      }
      catch (IllegalAccessException e) {
         Log.e("TreeViewList", "Error", e);
         return -1;
      }
   }

   public TreeViewList(final Context context, final AttributeSet attrs) {
      this(context, attrs, R.style.treeViewListStyle);
   }

   public TreeViewList(final Context context) {
      this(context, null);
   }

   public TreeViewList(final Context context, final AttributeSet attrs, final int defStyle) {
      super(context, attrs, defStyle);
      linePaint.setStyle(Paint.Style.STROKE);
      parseAttributes(context, attrs);
   }

   @Override
   protected void layoutChildren() {
      if (isLayoutRequested()) {
         super.layoutChildren();
      }
   }

   private void parseAttributes(final Context context, final AttributeSet attrs) {
      final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TreeViewList);
      expandedDrawable = a.getDrawable(R.styleable.TreeViewList_src_expanded);
      if (expandedDrawable == null) {
         expandedDrawable = context.getResources().getDrawable(DEFAULT_EXPANDED_RESOURCE);
      }
      collapsedDrawable = a.getDrawable(R.styleable.TreeViewList_src_collapsed);
      if (collapsedDrawable == null) {
         collapsedDrawable = context.getResources().getDrawable(DEFAULT_COLLAPSED_RESOURCE);
      }
      indentWidth = a.getDimensionPixelSize(R.styleable.TreeViewList_indent_width, DEFAULT_INDENT);
      indicatorGravity = a.getInteger(R.styleable.TreeViewList_indicator_gravity, DEFAULT_GRAVITY);
      indicatorBackgroundDrawable = a.getDrawable(R.styleable.TreeViewList_indicator_background);
      rowBackgroundDrawable = a.getDrawable(R.styleable.TreeViewList_row_background);
      collapsible = a.getBoolean(R.styleable.TreeViewList_collapsible, true);
      handleTrackballPress = a.getBoolean(R.styleable.TreeViewList_handle_trackball_press, true);

      drawLine = a.getBoolean(R.styleable.TreeViewList_draw_line, false);
      linePaint.setStrokeWidth(a.getDimensionPixelSize(R.styleable.TreeViewList_line_width, DEFAULT_LINE_WIDTH));
      linePaint.setColor(a.getColor(R.styleable.TreeViewList_line_color, getResources().getColor(R.color.tree_line)));
   }

   @Override
   public void setAdapter(final ListAdapter adapter) {
      if (!(adapter instanceof AbstractTreeViewAdapter)) {
         throw new TreeConfigurationException("The adapter is not of TreeViewAdapter type");
      }
      treeAdapter = (AbstractTreeViewAdapter<?>) adapter;
      syncAdapter();
      super.setAdapter(treeAdapter);
   }

   private void calculateIndentWidth() {
      if (expandedDrawable != null) {
         indentWidth = Math.max(getIndentWidth(), expandedDrawable.getIntrinsicWidth());
      }
      if (collapsedDrawable != null) {
         indentWidth = Math.max(getIndentWidth(), collapsedDrawable.getIntrinsicWidth());
      }
   }

   private void syncAdapter() {
      calculateIndentWidth();
      treeAdapter.setCollapsedDrawable(collapsedDrawable);
      treeAdapter.setExpandedDrawable(expandedDrawable);
      treeAdapter.setIndicatorGravity(indicatorGravity);
      treeAdapter.setIndentWidth(indentWidth);
      treeAdapter.setIndicatorBackgroundDrawable(indicatorBackgroundDrawable);
      treeAdapter.setRowBackgroundDrawable(rowBackgroundDrawable);
      treeAdapter.setCollapsible(collapsible);
      if (handleTrackballPress) {
         setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
               treeAdapter.handleItemClick(parent, view, position, view.getTag());
            }
         });
      }
      else {
         setOnClickListener(null);
      }

   }

   public void setExpandedDrawable(final Drawable expandedDrawable) {
      this.expandedDrawable = expandedDrawable;
      syncAdapter();
      treeAdapter.refresh();
   }

   public void setCollapsedDrawable(final Drawable collapsedDrawable) {
      this.collapsedDrawable = collapsedDrawable;
      syncAdapter();
      treeAdapter.refresh();
   }

   public void setRowBackgroundDrawable(final Drawable rowBackgroundDrawable) {
      this.rowBackgroundDrawable = rowBackgroundDrawable;
      syncAdapter();
      treeAdapter.refresh();
   }

   public void setIndicatorBackgroundDrawable(final Drawable indicatorBackgroundDrawable) {
      this.indicatorBackgroundDrawable = indicatorBackgroundDrawable;
      syncAdapter();
      treeAdapter.refresh();
   }

   public void setIndentWidth(final int indentWidth) {
      this.indentWidth = indentWidth;
      syncAdapter();
      treeAdapter.refresh();
   }

   public void setIndicatorGravity(final int indicatorGravity) {
      this.indicatorGravity = indicatorGravity;
      syncAdapter();
      treeAdapter.refresh();
   }

   public void setCollapsible(final boolean collapsible) {
      this.collapsible = collapsible;
      syncAdapter();
      treeAdapter.refresh();
   }

   public void setHandleTrackballPress(final boolean handleTrackballPress) {
      this.handleTrackballPress = handleTrackballPress;
      syncAdapter();
      treeAdapter.refresh();
   }

   public Drawable getExpandedDrawable() {
      return expandedDrawable;
   }

   public Drawable getCollapsedDrawable() {
      return collapsedDrawable;
   }

   public Drawable getRowBackgroundDrawable() {
      return rowBackgroundDrawable;
   }

   public Drawable getIndicatorBackgroundDrawable() {
      return indicatorBackgroundDrawable;
   }

   public int getIndentWidth() {
      return indentWidth;
   }

   public int getIndicatorGravity() {
      return indicatorGravity;
   }

   public boolean isCollapsible() {
      return collapsible;
   }

   public boolean isHandleTrackballPress() {
      return handleTrackballPress;
   }

   public boolean isDrawLine() {
      return drawLine;
   }

   public void setDrawLine(boolean drawLine) {
      this.drawLine = drawLine;
      invalidate();
   }

   public float getLineWidth() {
      return linePaint.getStrokeWidth();
   }

   public void setLineWidth(float width) {
      linePaint.setStrokeWidth(width);
      invalidate();
   }

   public int getLineColor() {
      return linePaint.getColor();
   }

   public void setLineColor(int color) {
      linePaint.setColor(color);
      invalidate();
   }

   @Override
   protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      int heightMode = MeasureSpec.getMode(heightMeasureSpec);
      //get big in ScrollView
      if (heightMode == MeasureSpec.UNSPECIFIED) {
         super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(99999, MeasureSpec.AT_MOST));
      }
      else {
         super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      }
   }

   @Override
   protected void dispatchDraw(Canvas canvas) {
      super.dispatchDraw(canvas);

      //draw the tree structure lines
      // ----------------- -----------
      //|      | indicator|           |
      //| space|          |           |
      //|      |     |    | frame     |
      // ----------------- -----------
      //             |
      //        -----|----------------   -----------
      //       |     |    |           | |           |
      //       |     -----|      | |  frame    |
      //       |  space   | indicator | |           |
      //        ----------------------   -----------
      if (drawLine) {
         //y coordinate of previous sibling's horizontal centerline
         int[] prevHorizLineByLevel = new int[20];
         //x coordinate of previous sibling's vertical centerline
         int[] prevVertLineByLevel = new int[20];
         for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            int treeLevel = treeAdapter.getTreeNodeInfo(i + getFirstPosition()).getLevel();

            View indicatorLayout = (View) child.getTag(R.id.treeview_list_item_frame_layout);
            View indicator = (View) child.getTag(R.id.treeview_list_item_toggle);

            int toggleLeft = child.getLeft() + indicatorLayout.getLeft() + indicatorLayout.getPaddingLeft();
            int toggleCenter = child.getLeft() + indicatorLayout.getLeft() + indicatorLayout.getPaddingLeft() + (indicator.getRight() - indicator.getLeft()) / 2;

            int toggleBottom = child.getTop() + indicatorLayout.getBottom() - indicatorLayout.getPaddingBottom();

            mTempRect.set(prevVertLineByLevel[treeLevel], prevHorizLineByLevel[treeLevel], toggleLeft, (child.getBottom() - child.getTop()) / 2 + child.getTop());

            prevHorizLineByLevel[treeLevel + 1] = toggleBottom;
            prevVertLineByLevel[treeLevel + 1] = toggleCenter;
            if (treeLevel > 0) {
               canvas.drawLine(mTempRect.left, mTempRect.top, mTempRect.left, mTempRect.bottom, linePaint);
               canvas.drawLine(mTempRect.left, mTempRect.bottom, mTempRect.right, mTempRect.bottom, linePaint);
            }
         }
      }
   }
}
