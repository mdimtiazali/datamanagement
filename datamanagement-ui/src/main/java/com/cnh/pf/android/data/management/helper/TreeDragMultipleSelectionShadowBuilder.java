/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import static java.lang.Math.max;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import com.cnh.pf.android.data.management.R;

/**
 * Draws abstract representative for multiple selection.
 *
 * @author krueger
 */
public class TreeDragMultipleSelectionShadowBuilder extends View.DragShadowBuilder {

   private int selectedItems = 0;

   //styleable parameter
   private static Bitmap folderImage = null;
   private static Paint backgroundPaint = null;
   private static Paint backgroundBorderPaint = null;
   private static RectF backgroundBox = null;
   private static float backgroundCornerRadius = 0f;
   private static Paint circlePaint = null;
   private static float minimumCircleRadius = 0f;
   private static Paint numberPaint = null;
   private static int fontSize;

   /**
    * Constructor generating instance of TreeDragMultipleSelectionShadowBuilder
    *
    * @param selectedItems Number to be shown in the circle
    * @param resources Resources to load attributes from
    */
   public TreeDragMultipleSelectionShadowBuilder(int selectedItems, Resources resources) {
      this.selectedItems = selectedItems;
      loadResources(resources);
   }

   private void loadResources(Resources resources) {
      //load image
      if (folderImage == null) {
         folderImage = BitmapFactory.decodeResource(resources, R.drawable.ic_datatree_copy);
      }
      //define box variables
      if (backgroundBox == null) {
         backgroundCornerRadius = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_corner_radius);
         float backgroundBoxWidthHalf = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_width) / 2f;
         float backgroundBoxHeightHalf = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_height) / 2f;
         float left = -backgroundBoxWidthHalf;
         float right = backgroundBoxWidthHalf;
         float top = -backgroundBoxHeightHalf;
         float bottom = backgroundBoxHeightHalf;
         backgroundBox = new RectF(left, top, right, bottom);
      }
      //define background color
      if (backgroundPaint == null) {
         backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
         backgroundPaint.setStyle(Paint.Style.FILL);
         backgroundPaint.setColor(resources.getColor(R.color.tree_drag_multiple_selection_shadow_box_background_color));
         float curveRadius = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_shadow_radius);
         float xOffset = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_shadow_x_offset);
         float yOffset = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_shadow_y_offset);
         int color = resources.getColor(R.color.tree_drag_multiple_selection_shadow_box_shadow_color);
         backgroundPaint.setShadowLayer(curveRadius, xOffset, yOffset, color);
      }
      //define border color
      if (backgroundBorderPaint == null) {
         backgroundBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
         backgroundBorderPaint.setStyle(Paint.Style.STROKE);
         backgroundBorderPaint.setColor(resources.getColor(R.color.tree_drag_multiple_selection_shadow_box_border_color));
         backgroundBorderPaint.setStrokeWidth(resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_border_width));
      }
      //define red circle
      if (circlePaint == null) {
         circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
         circlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
         circlePaint.setColor(resources.getColor(R.color.tree_drag_multiple_selection_shadow_box_circle_color));
         minimumCircleRadius = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_circle_minimum_radius);
      }
      //define number of selected items
      if (numberPaint == null) {
         fontSize = resources.getInteger(R.integer.tree_drag_multiple_selection_shadow_box_selected_number_font_size);
         numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
         numberPaint.setTextAlign(Paint.Align.CENTER);
         numberPaint.setTypeface(Typeface.DEFAULT_BOLD);
         numberPaint.setTextSize(fontSize);
         numberPaint.setColor(resources.getColor(R.color.tree_drag_multiple_selection_shadow_box_font_color));
      }
   }

   /**
    * Returns the boundary box of the given text painted with the given paint
    *
    * @param paint Paint the text should be measured with
    * @param text String to be measured
    * @return Rect representing the boundary of the given text painted with the given paint
    */
   private Rect getTextBounds(Paint paint, String text) {
      Rect textBounds = new Rect();
      paint.getTextBounds(text, 0, text.length(), textBounds);
      return textBounds;
   }

   /**
    * Returns the radius of the circle containing the String of selectedItems
    *
    * @return Float value representing the radius of the drawn circle
    */
   private float getCircleRadius() {
      Rect textBounds = getTextBounds(numberPaint, String.valueOf(selectedItems));
      float minimumTextRadius = textBounds.width() / 2f + textBounds.height() / 2f;
      return max(minimumCircleRadius, minimumTextRadius);
   }

   @Override
   public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
      //define canvas size which is box dimensions + circle diameter (canvas will be centered)
      float circleDiameter = getCircleRadius() * 2f;
      shadowSize.set((int) (backgroundBox.width() + circleDiameter), (int) (backgroundBox.height() + circleDiameter));
      //set touch point to the lower right corner of the representative
      shadowTouchPoint.set((int) (shadowSize.x / 2 + backgroundBox.right), (int) (shadowSize.y / 2 + backgroundBox.bottom));
   }

   @Override
   public void onDrawShadow(Canvas canvas) {
      //save canvas since origin is moved to the center in the following
      canvas.save();

      //set canvas to 0/0 representing the middle
      float canvasWidth = canvas.getWidth();
      float canvasHeight = canvas.getWidth();
      canvas.translate(canvasWidth / 2f, canvasHeight / 2f);

      //draw background
      canvas.drawRoundRect(backgroundBox, backgroundCornerRadius, backgroundCornerRadius, backgroundPaint);
      canvas.drawRoundRect(backgroundBox, backgroundCornerRadius, backgroundCornerRadius, backgroundBorderPaint);
      canvas.drawBitmap(folderImage, -folderImage.getWidth() / 2f, -folderImage.getHeight() / 2f, backgroundBorderPaint);

      //measure dimension of text to get dynamic radius of the circle
      String numberOfSelectedEntries = String.valueOf(selectedItems);
      float radius = getCircleRadius();
      Rect textBounds = getTextBounds(numberPaint, numberOfSelectedEntries);

      //draw number of selected items in a circle
      canvas.drawCircle(backgroundBox.left, backgroundBox.top, radius, circlePaint);
      canvas.drawText(numberOfSelectedEntries, backgroundBox.left, backgroundBox.top + (textBounds.height() / 2f), numberPaint);

      //restore old canvas
      canvas.restore();
   }

}
