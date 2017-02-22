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

import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductUnits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Factory for creation of application rate table elements.
 *
 * @author waldschmidt
 */
public class ApplicationRateTableFactory {

   private static final Logger log = LoggerFactory.getLogger(ApplicationRateTableFactory.class);

   /**
    * TODO: fix javadoc/replace product parameter
    *
    * create TableRow with product data
    * @param product current product to extract the data into the several cells
    * @return created TableRow
    */
   public static TableRow createTableRowForProductMixDialog(Product product, Context context, TableLayout productMixTable, MeasurementSystem measurementSystemProductOther) {
      TableRow tableRow = null;
      if (product != null) {
         LayoutInflater layoutInflater = LayoutInflater.from(context);
         tableRow = (TableRow) layoutInflater.inflate(R.layout.product_mix_dialog_application_rate_table_row, null);

         ProductUnits unit = ProductHelperMethods.retrieveProductRateUnits(product, measurementSystemProductOther);
         int tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell;
         if (productMixTable.getChildCount() - 1 % 2 == 1) {
            tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell_gray;
         }

         TextView productNameTextView = (TextView) tableRow.findViewById(R.id.product_name_text_view);
         productNameTextView.setText(product.getName());
         productNameTextView.setCompoundDrawablesWithIntrinsicBounds(getProductFormImageId(product.getForm()), 0, 0, 0);
         productNameTextView.setBackgroundResource(tableRowBackground);

         configureApplicationRateTextView(tableRow, unit, tableRowBackground, R.id.application_rate1_text_view, product.getDefaultRate());
         configureApplicationRateTextView(tableRow, unit, tableRowBackground, R.id.application_rate2_text_view, product.getRate2());
      }
      return tableRow;
   }

   /**
    * Configures an application rate text view.
    * @param tableRow the TableRow where the TextView is inside
    * @param unit the unit of the application rate
    * @param tableRowBackgroundId the backgroundId of the TextViews new background (optional)
    * @param textViewId the id of the TextView
    * @param rate the application rate
    */
   private static void configureApplicationRateTextView(TableRow tableRow, @Nullable ProductUnits unit, @Nullable Integer tableRowBackgroundId, int textViewId, double rate) {
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
    * TODO: fix javadoc/replace product parameter
    *
    * Create TableRow with product data
    * @param product current product to extract the data into the several cells
    * @return created TableRow
    */
   public static TableRow createTableRowForProductMixAdapter(Product product, Context context, MeasurementSystem volumeMeasurementSystem, MeasurementSystem massMeasurementSystem) {
      TableRow tableRow = null;
      if (product != null) {
         LayoutInflater layoutInflater = LayoutInflater.from(context);
         tableRow = (TableRow) layoutInflater.inflate(R.layout.product_mix_application_rate_table_row, null);

         ProductUnits unit = ProductHelperMethods.retrieveProductRateUnits(product,
               ProductHelperMethods.getMeasurementSystemForProduct(product, volumeMeasurementSystem, massMeasurementSystem));

         TextView productNameTextView = (TextView) tableRow.findViewById(R.id.product_name_text_view);
         productNameTextView.setText(" " + product.getName());

         configureApplicationRateTextView(tableRow, unit, null, R.id.application_rate1_text_view, product.getDefaultRate());
         configureApplicationRateTextView(tableRow, unit, null, R.id.application_rate2_text_view, product.getRate2());
      }
      return tableRow;
   }

   /**
    * Helper Method to get the right icon for a {@link ProductForm}
    * @param productForm the selected productForm
    * @return return the icon of the productForm or the Seed-Icon if the productForm is not implemented
    */
   public static int getProductFormImageId(ProductForm productForm) {
      if (productForm != null) {
         switch (productForm) {
         case ANHYDROUS:
            return R.drawable.ic_anhydrous;
         case BULK_SEED:
            return R.drawable.ic_bulk_seed;
         case GRANULAR:
            return R.drawable.ic_granular;
         case LIQUID:
            return R.drawable.ic_liquid;
         case SEED:
            return R.drawable.ic_seed;
         default:
            log.warn("ProductFrom not found: {}", productForm.name());
            return R.drawable.ic_seed;
         }
      }
      log.warn("ProductFrom is null");
      return R.drawable.ic_seed;
   }
}