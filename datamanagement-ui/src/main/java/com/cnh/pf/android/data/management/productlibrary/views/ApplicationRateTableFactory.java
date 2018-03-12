/*
 * Copyright (C) 2017 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.cnh.android.pf.widget.utilities.ControllerTypeOrProductFormIconHelper;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductUnits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Factory for creation of application rate table elements.
 *
 * @author waldschmidt
 */
public class ApplicationRateTableFactory {

   private static final Logger log = LoggerFactory.getLogger(ApplicationRateTableFactory.class);

   /**
    * Creates {@link TableRow} for an applicationRateTable with the style fitting to the {@link ProductMixDialog}.
    * @param applicationRateTableData container for all data which should be shown
    * @param context the context in which we show the {@link TableRow}
    * @param productMixTable the {@link TableLayout} (needed to calculate the color for the alternating background)
    * @return the created {@link TableRow}
    */
   @Nullable
   public static TableRow createTableRowForProductMixDialog(@Nullable ApplicationRateTableData applicationRateTableData, @Nonnull Context context,
         @Nonnull TableLayout productMixTable) {
      return createTableRowInternal(applicationRateTableData, context, productMixTable, true);
   }

   /**
    * Creates {@link TableRow} for an applicationRateTable with the style fitting to the expanded product mix in product library tab.
    * @param applicationRateTableData container for all data which should be shown
    * @param context the context in which we show the {@link TableRow}.
    * @param productMixTable the {@link TableLayout} (needed to calculate the color for the alternating background)
    * @return the created {@link TableRow}.
    */
   @Nullable
   public static TableRow createTableRowForProductMixAdapter(@Nullable ApplicationRateTableData applicationRateTableData, @Nonnull Context context,
         @Nonnull TableLayout productMixTable) {
      return createTableRowInternal(applicationRateTableData, context, productMixTable, false);
   }

   /**
    * Creates {@link TableRow} for an applicationRateTable alternating the style regarding to the boolean "forDialog".
    * @param applicationRateTableData container for all data which should be shown
    * @param context the context in which we show the {@link TableRow}
    * @param productMixTable the {@link TableLayout} (needed to calculate the color for the alternating background).
    * @param forDialog use true if you want to create a row for the dialog and false if you want to create it for a expanded product mix in product library tab
    * @return the created {@link TableRow}
    */
   @Nullable
   private static TableRow createTableRowInternal(@Nullable ApplicationRateTableData applicationRateTableData, @Nonnull Context context, @Nonnull TableLayout productMixTable,
         boolean forDialog) {
      TableRow tableRow = null;
      if (applicationRateTableData != null) {
         LayoutInflater layoutInflater = LayoutInflater.from(context);
         if (forDialog) {
            tableRow = (TableRow) layoutInflater.inflate(R.layout.product_mix_dialog_application_rate_table_row, null);
         }
         else {
            tableRow = (TableRow) layoutInflater.inflate(R.layout.product_mix_application_rate_table_row, null);
         }

         int tableRowBackgroundId = getAlternatingTableRowBackgroundId(productMixTable);
         TextView productNameTextView = (TextView) tableRow.findViewById(R.id.product_name_text_view);
         productNameTextView.setText(applicationRateTableData.productName);
         if (forDialog) {
            productNameTextView.setCompoundDrawablesWithIntrinsicBounds(ControllerTypeOrProductFormIconHelper.retrieveProductFormIconId(applicationRateTableData.productForm), 0, 0, 0);
         }
         productNameTextView.setBackgroundResource(tableRowBackgroundId);

         configureApplicationRateTextView(tableRow, applicationRateTableData.unit, tableRowBackgroundId, R.id.application_rate1_text_view, applicationRateTableData.defaultRate);
         configureApplicationRateTextView(tableRow, applicationRateTableData.unit, tableRowBackgroundId, R.id.application_rate2_text_view, applicationRateTableData.rate2);
      }
      return tableRow;
   }

   /**
    * Return the backgroundId for the table tow. Alternates between two colors.
    * @param productMixTable the table where want to add the row to.
    * @return the backgroundId.
    */
   private static int getAlternatingTableRowBackgroundId(@Nonnull TableLayout productMixTable) {
      int tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell;
      if ((productMixTable.getChildCount() - 1) % 2 == 1) {
         tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell_gray;
      }
      return tableRowBackground;
   }

   /**
    * Configures an application rate text view.
    * @param tableRow the TableRow where the TextView is inside
    * @param unit the unit of the application rate
    * @param tableRowBackgroundId the backgroundId of the TextViews new background (optional)
    * @param textViewId the id of the TextView.
    * @param rate the application rate.
    */
   private static void configureApplicationRateTextView(@Nonnull TableRow tableRow, @Nullable ProductUnits unit, @Nullable Integer tableRowBackgroundId, int textViewId,
         double rate) {
      TextView applicationRateTextView = (TextView) tableRow.findViewById(textViewId);
      if (tableRowBackgroundId != null) {
         applicationRateTextView.setBackgroundResource(tableRowBackgroundId);
      }
      if (unit != null) {
         applicationRateTextView.setText(String.format("%.2f %s", rate * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
      }
      else {
         applicationRateTextView.setText(String.format("%.2f", rate));
      }
   }

   /**
    * Container object for factory methods and to hold regarding temporary values.
    */
   public final static class ApplicationRateTableData {
      public ProductForm productForm;
      public String productName;
      public ProductUnits unit;
      public double defaultRate;
      public double rate2;
   }
}