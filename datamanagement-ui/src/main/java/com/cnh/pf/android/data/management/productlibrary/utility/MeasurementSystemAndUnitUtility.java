/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.utility;

import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductUnits;

/**
 * Helper class to get measurement system or unit of a product if you know the opposite.
 *
 * @author waldschmidt
 */
public final class MeasurementSystemAndUnitUtility {

   /**
    * get Productunit from product with the correct measurementsystem
    * @param product use to get the Product units
    * @return get the Productunit for the application rate if product is not null and measurementsystem, too, otherwise return null
    */
   public final static ProductUnits getApplicationRateProductUnit(Product product, MeasurementSystem measurementSystem) {
      if (product != null && measurementSystem != null) {
         switch (measurementSystem) {
            case IMPERIAL:
               return product.getRateDisplayUnitsImperial();
            case METRIC:
               return product.getRateDisplayUnitsMetric();
            case USA:
               return product.getRateDisplayUnitsUSA();
         }
      }
      return null;
   }

   /**
    * get Measurementsystem of the Product
    * @param product use to set the measurementsystem
    * @return the measurementsystem of volume or mass
    */
   public final static MeasurementSystem getProductUnitMeasurementSystem(Product product, MeasurementSystem volumeMeasurementSystem, MeasurementSystem massMeasurementSystem) {
      MeasurementSystem measurementSystem = MeasurementSystem.METRIC;
      if (product != null && volumeMeasurementSystem != null && massMeasurementSystem != null) {
         if (product.getForm() == ProductForm.ANHYDROUS || product.getForm() == ProductForm.LIQUID) {
            measurementSystem = volumeMeasurementSystem;
         }
         else {
            measurementSystem = massMeasurementSystem;
         }
      }
      return measurementSystem;
   }
}
