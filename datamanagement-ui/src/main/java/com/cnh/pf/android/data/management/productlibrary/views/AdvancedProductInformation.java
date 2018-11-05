/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.cnh.android.pf.widget.utilities.MathUtility;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.android.pf.widget.utilities.listeners.ClearFocusOnDoneOnEditorActionListener;
import com.cnh.android.widget.control.InputField;
import com.cnh.android.widget.control.SegmentedToggleButtonGroup;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.util.unit.UnitConverter;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AdvancedProductInformation to display epa number, manufacturer, buffer distance, max wind speed, restricted use, posting required
 *
 * @author mpadmanabhan
 * @since 9/7/2018.
 */

public class AdvancedProductInformation {

   private static final Logger logger = LoggerFactory.getLogger(AdvancedProductInformation.class);
   private final ArrayMap<String, MeasurementSystem> measurementSystemUnitsArrayMap;

   private InputField epaNumberInputField;
   private InputField manufacturerInputField;
   private InputField bufferDistanceInputField;
   private InputField maxWindSpeedInputField;

   private MeasurementSystem currentMeasurementSystem;

   private SegmentedToggleButtonGroup restrictedUseToggleButtonGroup;
   private SegmentedToggleButtonGroup postingRequiredToggleButtonGroup;

   private boolean restrictedBoolValue;
   private boolean postingRequiredBoolValue;

   private double bufferDistance;
   private double maxWindSpeed;
   private double rateUnitFactor = 1.0;

   private String epa;
   private String manufacturer;

   private Context context;
   private Product product;

   /**
    * Constructor for AdvancedProductInformation
    *
    * @param context current resource information
    */
   public AdvancedProductInformation(@Nonnull Context context) {
      this.context = context;
      measurementSystemUnitsArrayMap = UnitsSettings.queryMeasurementSystemSet(context);
   }

   /**
    * Method to initialize views for advanced product information
    *
    * @param view current view
    */
   public void initializeViews(View view) {
      this.epaNumberInputField = (InputField) view.findViewById(R.id.epa_number_input_field);
      this.manufacturerInputField = (InputField) view.findViewById(R.id.manufacturer_input_field);
      this.bufferDistanceInputField = (InputField) view.findViewById(R.id.buffer_distance_input_field);
      this.maxWindSpeedInputField = (InputField) view.findViewById(R.id.max_wind_speed_input_field);
      this.restrictedUseToggleButtonGroup = (SegmentedToggleButtonGroup) view.findViewById(R.id.restricted_use_segmented_button_toggle_group);
      this.postingRequiredToggleButtonGroup = (SegmentedToggleButtonGroup) view.findViewById(R.id.posting_required_segmented_button_toggle_group);
      initListeners();
      setUnits();
   }

   private void initListeners() {
      epaNumberInputField.addTextChangedListener(new EpaNumberTextWatcher());
      epaNumberInputField.setOnEditorActionListener(new EpaNumberOnEditActionListener());
      manufacturerInputField.addTextChangedListener(new ManufacturerTextWatcher());
      manufacturerInputField.setOnEditorActionListener(new ManufacturerOnEditActionListener());
      bufferDistanceInputField.addTextChangedListener(new BufferDistanceTextWatcher());
      bufferDistanceInputField.setOnEditorActionListener(new BufferDistanceOnEditActionListener());
      maxWindSpeedInputField.addTextChangedListener(new MaxWindSpeedTextWatcher());
      maxWindSpeedInputField.setOnEditorActionListener(new MaxWindSpeedOnEditorActionListener());
      restrictedUseToggleButtonGroup.setOnCheckedChangeListener(new RestrictedUseToggleButtonGroup());
      postingRequiredToggleButtonGroup.setOnCheckedChangeListener(new PostingRequiredToggleButtonGroup());
   }

   private void setUnits() {
      if (measurementSystemUnitsArrayMap != null && !measurementSystemUnitsArrayMap.isEmpty()) {
         currentMeasurementSystem = measurementSystemUnitsArrayMap.get(UnitsSettings.DISTANCE);
         if ((currentMeasurementSystem == MeasurementSystem.USA) || (currentMeasurementSystem == MeasurementSystem.IMPERIAL)) {
            bufferDistanceInputField.setUnits(context.getString(R.string.advanced_product_information_buffer_distance_in_ft));
            maxWindSpeedInputField.setUnits(context.getString(R.string.advanced_product_information_max_wind_speed_in_mph));
         }
         else {
            bufferDistanceInputField.setUnits(context.getString(R.string.advanced_product_information_buffer_distance_in_mts));
            maxWindSpeedInputField.setUnits(context.getString(R.string.advanced_product_information_max_wind_speed_in_kph));
         }
      }
   }

   /**
    * Sets the values from the given product to the UI.
    *
    * @param product the product which includes the values
    */
   public void setValuesToUi(@Nonnull Product product) {
      logger.trace("setValuesToUi {}", product);
      this.product = product;
      epaNumberInputField.setText(product.getEpaNumber());
      manufacturerInputField.setText(product.getManufacturer());
      if (measurementSystemUnitsArrayMap != null && !measurementSystemUnitsArrayMap.isEmpty()) {
         currentMeasurementSystem = measurementSystemUnitsArrayMap.get(UnitsSettings.DISTANCE);
         if (currentMeasurementSystem == MeasurementSystem.USA || currentMeasurementSystem == MeasurementSystem.IMPERIAL) {
            bufferDistanceInputField.setText(Double.toString(MathUtility.getConvertedFromBase(convertBufferDistanceToUsa(product.getBufferDistanceMeters()), rateUnitFactor)));
            maxWindSpeedInputField.setText(Double.toString(MathUtility.getConvertedFromBase(convertMaxWindSpeedToUsa(product.getMaxWindSpeedKph()), rateUnitFactor)));
         }
         else {
            setValuesToUiInDefaultMeasurementSystem();
         }
      }
      else {
         setValuesToUiInDefaultMeasurementSystem();
      }
   }

   /**
    * Sets the values to the given product.
    *
    * @param product the product to change
    */
   public void setValuesToProduct(@Nonnull Product product) {
      logger.trace("setValuesToProduct {}", product);
      this.product = product;
      product.setEpaNumber(epa);
      product.setManufacturer(manufacturer);
      product.setRestrictedUse(restrictedBoolValue);
      product.setPostingRequired(postingRequiredBoolValue);
      if (measurementSystemUnitsArrayMap != null && !measurementSystemUnitsArrayMap.isEmpty()) {
         currentMeasurementSystem = measurementSystemUnitsArrayMap.get(UnitsSettings.DISTANCE);
         if (currentMeasurementSystem == MeasurementSystem.USA || currentMeasurementSystem == MeasurementSystem.IMPERIAL) {
            product.setBufferDistanceMeters(MathUtility.getConvertedFromBase(convertBufferDistanceToMetric(bufferDistance), rateUnitFactor));
            product.setMaxWindSpeedKph(MathUtility.getConvertedFromBase(convertMaxWindSpeedToMetric(maxWindSpeed), rateUnitFactor));
         }
         else {
            setValuesToProductInDefaultMeasurementSystem();
         }
      }
      else {
         setValuesToProductInDefaultMeasurementSystem();
      }
   }

   private void setValuesToProductInDefaultMeasurementSystem() {
      logger.trace("setValuesToProductInDefaultMeasurementSystem");
      try {
         product.setBufferDistanceMeters(MathUtility.getConvertedFromBase(Double.valueOf(bufferDistanceInputField.getText().toString()), rateUnitFactor));
         product.setMaxWindSpeedKph(MathUtility.getConvertedFromBase(Double.valueOf(maxWindSpeedInputField.getText().toString()), rateUnitFactor));
      }
      catch (NumberFormatException ne) {
         logger.error("Number format exception: ", ne);
      }
   }

   private void setValuesToUiInDefaultMeasurementSystem() {
      logger.trace("setValuesToUiInDefaultMeasurementSystem");
      try {
         bufferDistanceInputField.setText(Double.toString(MathUtility.getConvertedFromBase(product.getBufferDistanceMeters(), rateUnitFactor)));
         maxWindSpeedInputField.setText(Double.toString(MathUtility.getConvertedFromBase(product.getMaxWindSpeedKph(), rateUnitFactor)));
      }
      catch (NumberFormatException ne) {
         logger.error("Number format exception: ", ne);
      }
   }

   private double convertBufferDistanceToMetric(double bufferDistance) {
      return UnitConverter.feetToMeter(bufferDistance);
   }

   private double convertBufferDistanceToUsa(double bufferDistance) {
      return UnitConverter.metersToFeet(bufferDistance);
   }

   private double convertMaxWindSpeedToMetric(double maxWindSpeed) {
      return UnitConverter.milesPerHourToKilometersPerHour(maxWindSpeed);
   }

   private double convertMaxWindSpeedToUsa(double maxWindSpeed) {
      return UnitConverter.kilometersPerHourToMilesPerHour(maxWindSpeed);
   }

   private class EpaNumberTextWatcher implements TextWatcher {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void afterTextChanged(Editable editable) {
         epa = epaNumberInputField.getText().toString();
      }
   }

   private class ManufacturerTextWatcher implements TextWatcher {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void afterTextChanged(Editable editable) {
         manufacturer = manufacturerInputField.getText().toString();
      }
   }

   private class BufferDistanceTextWatcher implements TextWatcher {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void afterTextChanged(Editable editable) {
         try {
            bufferDistance = Double.valueOf(bufferDistanceInputField.getText().toString());
         }
         catch (NumberFormatException e) {
            logger.error("BufferDistanceTextWatcher afterTextChanged Exception: ", e);
         }
      }
   }

   private class MaxWindSpeedTextWatcher implements TextWatcher {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
         //not used
      }

      @Override
      public void afterTextChanged(Editable editable) {
         try {
            maxWindSpeed = Double.valueOf(maxWindSpeedInputField.getText().toString());
         }
         catch (NumberFormatException e) {
            logger.error("MaxWindSpeedTextWatcher afterTextChanged Exception: ", e);
         }
      }
   }

   private class EpaNumberOnEditActionListener extends ClearFocusOnDoneOnEditorActionListener {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
         if (actionId == EditorInfo.IME_ACTION_DONE) {
            epaNumberInputField.setText(epa);
         }
         return super.onEditorAction(textView, actionId, keyEvent);
      }
   }

   private class ManufacturerOnEditActionListener extends ClearFocusOnDoneOnEditorActionListener {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
         if (actionId == EditorInfo.IME_ACTION_DONE) {
            manufacturerInputField.setText(manufacturer);
         }
         return super.onEditorAction(textView, actionId, keyEvent);
      }
   }

   private class BufferDistanceOnEditActionListener extends ClearFocusOnDoneOnEditorActionListener {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
         if (actionId == EditorInfo.IME_ACTION_DONE) {
            bufferDistanceInputField.setText(Double.toString(bufferDistance));
         }
         return super.onEditorAction(textView, actionId, keyEvent);
      }
   }

   private class MaxWindSpeedOnEditorActionListener extends ClearFocusOnDoneOnEditorActionListener {
      @Override
      public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
         if (actionId == EditorInfo.IME_ACTION_DONE) {
            maxWindSpeedInputField.setText(Double.toString(maxWindSpeed));
         }
         return super.onEditorAction(textView, actionId, keyEvent);
      }
   }

   private class RestrictedUseToggleButtonGroup implements RadioGroup.OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup radioGroup, int buttonID) {
         restrictedUseToggleButtonGroup.setEnabled(buttonID, true);
         restrictedUseBoolean(buttonID);
      }

      private void restrictedUseBoolean(int buttonID) {
         if (buttonID == 1) {
            restrictedBoolValue = true;
         }
         else {
            restrictedBoolValue = false;
         }
      }
   }

   private class PostingRequiredToggleButtonGroup implements RadioGroup.OnCheckedChangeListener {
      @Override
      public void onCheckedChanged(RadioGroup radioGroup, int buttonID) {
         postingRequiredToggleButtonGroup.setEnabled(buttonID, true);
         postingRequiredBoolean(buttonID);
      }

      private void postingRequiredBoolean(int buttonID) {
         if (buttonID == 1) {
            postingRequiredBoolValue = true;
         }
         else {
            postingRequiredBoolValue = false;
         }
      }
   }
}
