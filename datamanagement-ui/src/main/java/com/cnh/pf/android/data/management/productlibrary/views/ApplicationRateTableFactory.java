/*
 * Copyright (C) 2017 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
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

/**
 * TODO: class description
 *
 * @author waldschmidt
 */
public class ApplicationRateTableFactory {

   private static final Logger log = LoggerFactory.getLogger(ApplicationRateTableFactory.class);

   /**
    * TODO: fix javadoc
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

         TextView applicationRate1TextView = (TextView) tableRow.findViewById(R.id.application_rate1_text_view);
         applicationRate1TextView.setBackgroundResource(tableRowBackground);
         if (unit != null) {
            applicationRate1TextView.setText(String.format("   %.2f %s", product.getDefaultRate() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate1TextView.setText(String.format("   %.2f", product.getDefaultRate()));
         }

         TextView applicationRate2TextView = (TextView) tableRow.findViewById(R.id.application_rate2_text_view);
         applicationRate2TextView.setBackgroundResource(tableRowBackground);
         if (unit != null) {
            applicationRate2TextView.setText(String.format("   %.2f %s", product.getRate2() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate2TextView.setText(String.format("   %.2f", product.getRate2()));
         }
      }
      return tableRow;
   }

   /**
    * TODO: fix javadoc
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

         TextView applicationRate1TextView = (TextView) tableRow.findViewById(R.id.application_rate1_text_view);
         if (unit != null) {
            applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate(), ""));
         }

         TextView applicationRate2TextView = (TextView) tableRow.findViewById(R.id.application_rate2_text_view);;
         if (unit != null) {
            applicationRate2TextView.setText(String.format(" %.2f %s", product.getRate2() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate2TextView.setText(String.format(" %.2f %s", product.getRate2(), ""));
         }
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
               log.warn("ProductFrom not found: " + productForm.name());
               return R.drawable.ic_seed;
         }
      }
      log.warn("ProductFrom is null");
      return R.drawable.ic_seed;
   }
}