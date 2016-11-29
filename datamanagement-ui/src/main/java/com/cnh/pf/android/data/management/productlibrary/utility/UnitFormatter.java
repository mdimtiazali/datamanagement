/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.utility;

import com.cnh.android.pf.widget.utilities.MathUtility;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductUnits;

/**
 * Utility class to create output strings for product values
 * @author waldschmidt
 */
public final class UnitFormatter {

   /**
    * Retrieve package unit values formatted properly per product settings
    * @param product the {@link Product} containing the package size units
    * @param value the value of the product
    * @param measurementSystem the {@link MeasurementSystem} used for the package size
    * @return appropriate formatted string for the unit
    * @throws IllegalArgumentException if one of the parameters is null
    */
   public static String formatPackageUnits(Product product, double value, MeasurementSystem measurementSystem) {
      if (measurementSystem == null){
         throw new IllegalArgumentException("got null argument parameter: measurementSystem");
      }
      if (product == null){
         throw new IllegalArgumentException("got null argument parameter: product");
      }

      // Try to find a unit - if you got none return default String in default formatting
      ProductUnits productUnits;
      switch (measurementSystem) {
         case IMPERIAL:
            productUnits = product.getPackageSizeUnitsImperial();
            break;
         case METRIC:
            productUnits = product.getPackageSizeUnitsMetric();
            break;
         case USA:
            productUnits = product.getPackageSizeUnitsUSA();
            break;
         default:
            return  String.format("%1.2f", value);
      }
      return createFormattedString(value, productUnits);
   }

   /**
    * Retrieve rate unit values formatted properly per product settings
    * @param product the {@link Product} containing the rate units
    * @param value the value of the product
    * @return appropriate formatted string for the unit
    * @throws IllegalArgumentException if one of the parameters is null
    */
   public static String formatRateUnits(Product product, double value) {
      if (product == null){
         throw new IllegalArgumentException("got null argument parameter: product");
      }

      // Try to find a unit - if you got none return default String in default formatting
      ProductUnits productUnits = product.getRateDisplayUnitsImperial();
      if (productUnits == null){
         productUnits = product.getRateDisplayUnitsMetric();
         if (productUnits == null){
            productUnits = product.getRateDisplayUnitsUSA();
            if (productUnits == null) {
               return String.format("%1.2f", value);
            }
         }
      }
      return createFormattedStringForUnitAndValue(value, productUnits);
   }

   /**
    * Retrieve density unit values formatted properly per product settings
    * @param product product the {@link Product} containing the density units
    * @param value the value of the product
    * @param measurementSystem the {@link MeasurementSystem} used for the package units
    * @return appropriate formatted string for the unit
    * @throws IllegalArgumentException if one of the parameters is null
    */
   public static String formatDensityUnits(Product product, double value, MeasurementSystem measurementSystem) {
      if (measurementSystem == null){
         throw new IllegalArgumentException("got null argument parameter: measurementSystem");
      }
      if (product == null){
         throw new IllegalArgumentException("got null argument parameter: product");
      }

      // Try to find a unit - if you got none return default String in default formatting
      ProductUnits productUnits;
      switch (measurementSystem) {
         case IMPERIAL:
            productUnits = product.getDensityUnitsImperial();
            break;
         case METRIC:
            productUnits = product.getDensityUnitsMetric();
            break;
         case USA:
            productUnits = product.getDensityUnitsUSA();
            break;
         default:
            return  String.format("%1.2f", value);
      }
      return createFormattedString(value, productUnits);
   }


   /**
    * @param value the product value
    * @param productUnits the product unit
    * @return a formatted string with value and possibly the unit
    */
   private static String createFormattedString(double value, ProductUnits productUnits) {
      if (productUnits != null){
         return createFormattedStringForUnitAndValue(value, productUnits);
      } else {
         return String.format("%1.2f", value);
      }
   }

   /**
    * @param value the product value
    * @param productUnits the product unit
    * @return a formatted string with value and unit
    * @throws NullPointerException if productUnits is null
    */
   private static String createFormattedStringForUnitAndValue(double value, ProductUnits productUnits) {
      String result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, productUnits.getMultiplyFactorFromBaseUnits()));
      String productUnitsName = productUnits.getName();
      if (productUnitsName != null){
         result = result + " " + productUnitsName;
      }
      return result;
   }
}