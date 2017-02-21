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
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
         tableRow = new TableRow(context);
         ProductUnits unit = ProductHelperMethods.retrieveProductRateUnits(product, measurementSystemProductOther);
         int tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell;
         if (productMixTable.getChildCount() - 1 % 2 == 1) {
            tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell_gray;
         }
         tableRow.setGravity(Gravity.CENTER);

         LinearLayout productNameLayout = new LinearLayout(context);
         productNameLayout.setBackgroundResource(tableRowBackground);
         productNameLayout.setOrientation(LinearLayout.HORIZONTAL);
         productNameLayout.setGravity(Gravity.START | Gravity.CENTER);

         ImageView productTypeImage = new ImageView(context);
         productTypeImage.setMaxWidth(context.getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setMinimumWidth(context.getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setMaxHeight(context.getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setMinimumHeight(context.getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setBackgroundResource(getProductTypeImageId(product.getForm()));
         productNameLayout.addView(productTypeImage, context.getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height),
               context.getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));

         TextView productNameTextView = new TextView(context);
         productNameTextView.setText(product.getName());
         productNameTextView.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.product_mix_dialog_application_rate_name_text_size));
         productNameTextView.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
         productNameTextView.setPadding(10, 0, 0, 0);
         productNameTextView.setTypeface(null, Typeface.BOLD);
         productNameLayout.addView(productNameTextView);

         tableRow.addView(productNameLayout);

         TextView applicationRate1TextView = new TextView(context);
         applicationRate1TextView.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.product_mix_dialog_application_rate_text_size));
         applicationRate1TextView.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
         applicationRate1TextView.setTypeface(null, Typeface.BOLD);
         applicationRate1TextView.setGravity(Gravity.START | Gravity.CENTER);
         applicationRate1TextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
         applicationRate1TextView.setBackgroundResource(tableRowBackground);
         if (unit != null) {
            applicationRate1TextView.setText(String.format("   %.2f %s", product.getDefaultRate() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate1TextView.setText(String.format("   %.2f", product.getDefaultRate()));
         }
         tableRow.addView(applicationRate1TextView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

         TextView applicationRate2TextView = new TextView(context);
         applicationRate2TextView.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.product_mix_dialog_application_rate_text_size));
         applicationRate2TextView.setTypeface(null, Typeface.BOLD);
         applicationRate2TextView.setGravity(Gravity.START | Gravity.CENTER);
         applicationRate2TextView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
         applicationRate2TextView.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
         applicationRate2TextView.setBackgroundResource(tableRowBackground);
         if (unit != null) {
            applicationRate2TextView.setText(String.format("   %.2f %s", product.getRate2() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate2TextView.setText(String.format("   %.2f", product.getRate2()));
         }

         tableRow.addView(applicationRate2TextView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
      }
      return tableRow;
   }

   /**
    * create Tablerow with product data
    * @param product current product to extract the data into the several cells
    * @return created TableRow
    */
   public static TableRow createTableRowForProductMixAdapter(Product product, Context context, MeasurementSystem volumeMeasurementSystem, MeasurementSystem massMeasurementSystem) {
      TableRow tableRow = new TableRow(context);
      if (product != null) {
         ProductUnits unit = ProductHelperMethods.retrieveProductRateUnits(product,
               ProductHelperMethods.getMeasurementSystemForProduct(product, volumeMeasurementSystem, massMeasurementSystem));
         tableRow.setGravity(Gravity.CENTER);
         TextView productNameTextView = new TextView(context);
         productNameTextView.setText(" " + product.getName());
         productNameTextView.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
         productNameTextView.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
         productNameTextView.setTypeface(null, Typeface.BOLD);
         productNameTextView.setBackgroundResource(R.drawable.product_mix_dialog_application_rates_table_background_cell);
         tableRow.addView(productNameTextView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

         TextView applicationRate1TextView = new TextView(context);
         applicationRate1TextView.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
         applicationRate1TextView.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
         applicationRate1TextView.setTypeface(null, Typeface.BOLD);
         applicationRate1TextView.setBackgroundResource(R.drawable.product_mix_dialog_application_rates_table_background_cell);
         if (unit != null) {
            applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate(), ""));
         }
         tableRow.addView(applicationRate1TextView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

         TextView applicationRate2TextView = new TextView(context);
         applicationRate2TextView.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
         applicationRate2TextView.setTextColor(context.getResources().getColor(R.color.defaultTextColor));
         applicationRate2TextView.setTypeface(null, Typeface.BOLD);
         applicationRate2TextView.setBackgroundResource(R.drawable.product_mix_dialog_application_rates_table_background_cell);
         if (unit != null) {
            applicationRate2TextView.setText(String.format(" %.2f %s", product.getRate2() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate2TextView.setText(String.format(" %.2f %s", product.getRate2(), ""));
         }
         tableRow.addView(applicationRate2TextView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
      }
      return tableRow;
   }

   /**
    * Helper Method to get the right Asset of the Producttype
    * @param productType the selected producttype
    * @return return the assset of the producttype if will found, otherwise use default the Seed Asset
    */
   public static int getProductTypeImageId(ProductForm productType) {
      if (productType != null) {
         switch (productType) {
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
               log.warn("ProductType not found: " + productType.name());
               return R.drawable.ic_seed;
         }
      }
      log.warn("ProductType is null");
      return R.drawable.ic_seed;
   }
}
