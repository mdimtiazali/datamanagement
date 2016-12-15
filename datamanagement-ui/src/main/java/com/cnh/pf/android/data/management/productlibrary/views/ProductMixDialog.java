/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.pf.widget.controls.SegmentedToggleButtonGroupPickList;
import com.cnh.android.pf.widget.controls.SegmentedToggleButtonGroupPickList.SegmentedTogglePickListListener;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.ProductMixHelper;
import com.cnh.android.pf.widget.utilities.ProductMixRecipeHelper;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.android.pf.widget.utilities.UnitsToggleHolder;
import com.cnh.android.pf.widget.utilities.commands.ProductCommandParams;
import com.cnh.android.pf.widget.utilities.commands.ProductMixCommandParams;
import com.cnh.android.pf.widget.utilities.commands.SaveProductCommand;
import com.cnh.android.pf.widget.utilities.commands.SaveProductMixCommand;
import com.cnh.android.pf.widget.utilities.listeners.ExtendedOnAdjustableBarChangedListener;
import com.cnh.android.pf.widget.utilities.listeners.GenericListener;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.android.pf.widget.view.DisabledOverlay.MODE;
import com.cnh.android.vip.aidl.IVIPListenerAIDL;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.vip.aidl.SimpleVIPListener;
import com.cnh.android.widget.control.AbstractStepperView;
import com.cnh.android.widget.control.CategoryButtons;
import com.cnh.android.widget.control.CategoryButtons.CategoryButtonsEventListener;
import com.cnh.android.widget.control.PickList;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListEditable.OnPickListItemActionListener;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.StepperView;
import com.cnh.android.widget.control.UnitText;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.MixType;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductDisplayItem;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductMix;
import com.cnh.pf.model.product.library.ProductMixRecipe;
import com.cnh.pf.model.product.library.ProductUnits;
import com.cnh.pf.model.product.library.ProductUsage;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DialogView to create ProductMixes
 *
 * @author helming
 * @since 29.10.2015
 */
public class ProductMixDialog extends DialogView {

   private static final String TAG = ProductMixDialog.class.getSimpleName();
   private static final Logger log = LoggerFactory.getLogger(ProductMixDialog.class);
   private static final int BUTTON_ADD = BUTTON_FIRST;
   private static final int BUTTON_CANCEL = BUTTON_SECOND;
   private static final int MAX_STEPPER = 100000;
   private static final int MIN_STEPPER = 0;
   private static final int STEP_SIZE = 1;
   private static final int PRECISION = 2;

   private IVIPServiceAIDL vipService;
   private ProductMix productMix;
   private ProductMixesDialogActionType actionType;
   private MeasurementSystem measurementSystemProductDensity = MeasurementSystem.IMPERIAL;
   private MeasurementSystem measurementSystemProductMass = MeasurementSystem.IMPERIAL;
   private MeasurementSystem measurementSystemProductVolume = MeasurementSystem.IMPERIAL;
   private MeasurementSystem measurementSystemProductOther = MeasurementSystem.IMPERIAL;
   private boolean isProductMixFormSet = false;
   private boolean isCarrierSet = false;
   private boolean isOneProductMixSet = false;
   private boolean isInitialized = false;
   private ProductMixCallBack callback;
   private ArrayList<Product> productList = null;
   private List<ProductUnits> productUnitsList = null;
   private LinearLayout mixProductsLayout;
   private CategoryButtons mixProductCategoryButton;
   private CategoryButtons applicationRatesCategoryButton;
   private CategoryButtons advancedCategoryButton;
   private LinearLayout applicationProductTableLayoutOverView;
   private ProductForm productMixForm = ProductForm.GRANULAR;
   private ProductMixElementHolder carrierProductHolder;
   private double totalAmount = 0;
   private final ArrayList<ProductMixElementHolder> productMixList = new ArrayList<ProductMixElementHolder>();
   private final ArrayList<ProductMixRecipe> recipeDeleteList = new ArrayList<ProductMixRecipe>();
   private StepperView applicationRate1Stepper;
   private StepperView applicationRate2Stepper;
   private StepperView applicationRateDeltaStepper;
   private StepperView applicationRateMaxStepper;
   private StepperView applicationRateMinStepper;
   private SegmentedToggleButtonGroupPickList applicationRatesUnitsPickList;
   private TableLayout productMixTable;
   private PickList productUsagePickList;
   private UnitText packageSizeValueUnitText;
   private SegmentedToggleButtonGroupPickList packageSizeUnitPickList;
   private UnitText densityValueUnitText;
   private TextView densityValueTitleText;
   private SegmentedToggleButtonGroupPickList densityUnitPickList;
   private Button addMoreButton;
   private EditText productMixTitleEditText;
   private PickList productMixFormPickList;
   private DisabledOverlay overlay;

   private CategoryButtonsEventListener eventListener = new CategoryButtonsEventListener() {
      @Override
      public void onCategoryButtonsBeforeExpand(CategoryButtons categoryButtons) {
         InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
         imm.hideSoftInputFromWindow(productMixTitleEditText.getWindowToken(), 0);
      }

      @Override
      public void onCategoryButtonsAfterExpand(CategoryButtons categoryButtons) {

      }

      @Override
      public void onCategoryButtonsBeforeCollapse(CategoryButtons categoryButtons) {

      }

      @Override
      public void onCategoryButtonsAfterCollapse(CategoryButtons categoryButtons) {

      }
   };

   private IVIPListenerAIDL vipListener = new SimpleVIPListener() {
      @Override
      public void deliverProductList(List<Product> products) throws RemoteException {
         log.info("deliverProductList: " + products.size());
         ArrayList<Product> tempProductList = new ArrayList<Product>();
         productList = new ArrayList<Product>();
         for (Product product : products) {
            if (product != null && product.getProductMixId() == 0) {
               tempProductList.add(product);
            }
         }
         productList = tempProductList;
         if (!isInitialized) {
            initializeViews();
            isInitialized = true;
         }
         if (productUnitsList != null && productList != null) {
            log.info("loadProductMixToDialog by deliverProductList");
            ProductMixDialog.this.post(new Runnable() {
               @Override
               public void run() {
                  if (!isInitialized) {
                     initializeViews();
                     isInitialized = true;
                  }
                  else {
                     log.info("is Initialized");
                  }
                  if (actionType != ProductMixesDialogActionType.ADD) {
                     loadProductMixToDialog();
                  }
               }
            });
         }
      }

      @Override
      public void deliverProductUnitsList(List<ProductUnits> productUnits) throws RemoteException {
         log.info("deliverProductUnitsList");
         if (productUnits != null && !productUnits.isEmpty()) {
            productUnitsList = productUnits;
            if (productList != null) {
               log.info("loadProductMixToDialog by deliverProductUnitsList");
               new Handler(Looper.getMainLooper()).post(new Runnable() {
                  @Override
                  public void run() {
                     if (!isInitialized) {
                        initializeViews();
                        isInitialized = true;
                     }
                     if (actionType != ProductMixesDialogActionType.ADD) {
                        loadProductMixToDialog();
                     }
                  }
               });
            }
         }
      }
   };

   /**
    * Use to set ActionType into the Dialog
    * user can create a new Product Mix(ADD)
    * User can edit an exist Product Mix (EDIT)
    * User can copy an exist Product Mix (COPY)
    */
   public enum ProductMixesDialogActionType {
      ADD, EDIT, COPY
   }

   public interface ProductMixCallBack {
      /**
       * after Creating ProductMix will call loadProductMix for the MainView
       * @param productMix new ProductMix
       */
      void loadProductMix(ProductMix productMix);
   }

   /**
    * Create ProductMixDialog for adding new Product Mix
    * @param context all ressource infomration
    * @param vipService communication with database
    * @param callback callbackMethod
    */
   public ProductMixDialog(Context context, IVIPServiceAIDL vipService, ProductMixCallBack callback) {
      this(context, ProductMixesDialogActionType.ADD, vipService, null, callback);
   }

   public ProductMixDialog(Context context, ProductMixesDialogActionType actionType, IVIPServiceAIDL vipService, ProductMix productMix, ProductMixCallBack callBack) {
      super(context);
      measurementSystemProductDensity = UnitsSettings.queryMeasurementSystem(context, UnitsSettings.DENSITY);
      measurementSystemProductMass = UnitsSettings.queryMeasurementSystem(context, UnitsSettings.MASS);
      measurementSystemProductVolume = UnitsSettings.queryMeasurementSystem(context, UnitsSettings.VOLUME);
      measurementSystemProductOther = UnitsSettings.queryMeasurementSystem(context, UnitsSettings.OTHER);
      this.actionType = actionType;
      this.productMix = productMix;
      this.vipService = vipService;
      this.callback = callBack;
      if (vipService != null) {
         try {
            vipService.register(ProductMixDialog.class.getSimpleName(), vipListener);
            vipService.requestProductList();
            vipService.requestProductUnitsList(null, null, null, null);
         }
         catch (RemoteException e) {
            log.error("can not register vipListener", e);
         }
      }
      initEventListener();
   }

   @Override
   public DialogView setBodyView(int layoutResId) {
      super.setBodyView(layoutResId);
      initializeGUI();
      initializeProductFormPickList();
      if (actionType != ProductMixesDialogActionType.ADD) {
         isCarrierSet = true;
         isOneProductMixSet = true;
         isProductMixFormSet = true;
      }

      if (actionType == ProductMixesDialogActionType.COPY) {
         if (productMix != null && productMix.getProductMixParameters() != null) {
            productMixTitleEditText.setText(String.format("%s -%s", productMix.getProductMixParameters().getName(), getContext().getString(R.string.copy)));
         }
      }
      return this;
   }

   /**
    * Initialize all Views and connect logic with the UI will be public because
    */
   private void initializeViews() {
      initializeMainView();
      initializeMixProductsView();
      initializeApplicationRatesView();
      initializeAdvancedView();
      isInitialized = true;
      overlay.setMode(MODE.HIDDEN);
   }

   private void initializeGUI() {
      overlay = (DisabledOverlay) this.findViewById(R.id.disabled_overlay);
      addMoreButton = (Button) this.findViewById(R.id.product_mix_dialog_mix_product_add_more);
      productUsagePickList = (PickList) this.findViewById(R.id.product_mix_advanced_picklist_usage);
      packageSizeValueUnitText = (UnitText) this.findViewById(R.id.product_mix_advanced_unittext_package_size);
      packageSizeValueUnitText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
      packageSizeUnitPickList = (SegmentedToggleButtonGroupPickList) this.findViewById(R.id.product_mix_segmentedtogglebuttongroup_advanced_package_size_units);
      densityValueUnitText = (UnitText) this.findViewById(R.id.product_mix_advanced_unittext_density);
      densityValueUnitText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
      densityValueTitleText = (TextView) this.findViewById(R.id.product_mix_advanced_titletext_density);
      densityUnitPickList = (SegmentedToggleButtonGroupPickList) this.findViewById(R.id.product_mix_segmentedtogglebuttongroup_advanced_density_units);
      applicationRate1Stepper = (StepperView) this.findViewById(R.id.product_mix_application_rates_stepperview_rate_1);
      applicationRate2Stepper = (StepperView) this.findViewById(R.id.product_mix_application_rates_stepperview_rate_2);
      applicationRateDeltaStepper = (StepperView) this.findViewById(R.id.product_mix_application_rates_stepperview_rate_delta);
      applicationRateMaxStepper = (StepperView) this.findViewById(R.id.product_mix_application_rates_stepperview_rate_max);
      applicationRateMinStepper = (StepperView) this.findViewById(R.id.product_mix_application_rates_stepperview_rate_min);
      applicationRatesUnitsPickList = (SegmentedToggleButtonGroupPickList) this.findViewById(R.id.product_mix_application_rates_segmentedtogglebuttongroup_units);
      productMixTable = (TableLayout) this.findViewById(R.id.product_mix_dialog_application_rates_table);
      productMixTitleEditText = (EditText) this.findViewById(R.id.product_mix_edittext_product_mix_name);
      productMixTitleEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
      productMixFormPickList = (PickList) this.findViewById(R.id.product_mix_picklist_product_form);
      mixProductsLayout = (LinearLayout) this.findViewById(R.id.product_mix_product_list_view);
      mixProductCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_categorybuttons_mix_product_view);
      mixProductCategoryButton.setCategoryButtonsEventListener(eventListener);
      CategoryButtons applicationRatesCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_categorybuttons_application_rate_view);
      applicationRatesCategoryButton.setCategoryButtonsEventListener(eventListener);
      CategoryButtons advancedCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_advanced_categorybuttons_view);
      applicationRatesCategoryButton.setCategoryButtonsEventListener(eventListener);
      overlay.setMode(MODE.LOADING);

   }

   /**
    * Initialize the main view, load ProductFormPickList and create a new ProductMix if actiontype is add
    */
   private void initializeMainView() {

      if (this.actionType == ProductMixesDialogActionType.ADD) {
         this.productMix = new ProductMix();
      }
      if (productMixTitleEditText != null) {
         productMixTitleEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
               if (actionId == EditorInfo.IME_ACTION_DONE) {
                  validateRequiredData();
               }
               return false;
            }
         });
      }
      initializeProductFormPickList();
   }

   /**
    * Load {@link ProductForm} into the {@link PickList}.
    * Will load Carrier Product with filtered products if {@link ProductForm} is selected
    */
   private void initializeProductFormPickList() {
      if (productMixFormPickList != null) {
         PickListAdapter carrierPickListAdapter = new PickListAdapter(productMixFormPickList, getContext());
         for (ProductForm form : ProductForm.values()) {
            if (form != ProductForm.ANY) {
               carrierPickListAdapter.add(new PickListItem(form.getValue(), friendlyName(form.name())));
            }
         }
         productMixFormPickList.setAdapter(carrierPickListAdapter);
         productMixFormPickList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
               productMixForm = ProductForm.values()[position];
               initializeApplicationRatesSegmentedToggleButtonGroup(productMixForm);
               if (productList == null){
                  log.warn("ProductList is null");
               } else if (productList.isEmpty()){
                  log.warn("ProductList is empty");
               } else if (carrierProductHolder.productPickList != null){
                  PickListAdapter productListAdapter = new PickListAdapter(carrierProductHolder.productPickList, getContext());
                  fillPickListAdapterWithProducts(productListAdapter, filterProductList(productList, productMixForm));
                  carrierProductHolder.productPickList.setAdapter(productListAdapter);
               }
               isProductMixFormSet = true;
               validateRequiredData();
               initializePackageSizeUnits();
               initializeDensityUnits();
               resetCarrierProduct();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
               //Do nothing
            }
         });
      }
   }

   private void loadProductMixToDialog() {
      Product productMixParameters = productMix.getProductMixParameters();
      //Add Product mix name and form to UI
      {
         productMixTitleEditText.setText(productMixParameters.getName());
         productMixForm = productMixParameters.getForm();
         productMixFormPickList.setSelectionByPosition(ProductForm.valueOf(productMixForm.name()).ordinal());
      }
      //Add Carrier Data to UI
      {
         ProductMixRecipe productCarrier = productMix.getProductCarrier();

         carrierProductHolder.currentRecipe = productCarrier;
         ArrayList<Product> filteredProduct = filterProductList(productList, productMixForm);
         addDataToProductElement(productCarrier, carrierProductHolder, filteredProduct);
         carrierProductHolder.currentRecipe = productCarrier;
      }

      //Add Product Mix element Data to UI
      for (int i = 0; i < productMix.getRecipe().size(); i++) {
         ProductMixRecipe recipeElement = productMix.getRecipe().get(i);
         if (i > 0) {
            addProduct();
         }
         ProductMixElementHolder element = this.productMixList.get(i);
         if (element != null) {
            addDataToProductElement(recipeElement, element, productList);
            element.currentRecipe = recipeElement;
         }
      }

      //Add Application Rate Data to UI
      {
         setUnitToSegmentedToggleButtonGroup(applicationRatesUnitsPickList, productMixForm, measurementSystemProductOther, ProductDisplayItem.RATES);
         UnitsToggleHolder unitHolder = (UnitsToggleHolder) applicationRatesUnitsPickList.getTag();
         if (unitHolder != null) {
            unitHolder.currentChoice = ProductHelperMethods.retrieveProductRateUnits(productMixParameters, measurementSystemProductOther);
            applicationRatesUnitsPickList.setSelectionById((long) unitHolder.unitChoices.indexOf(unitHolder.currentChoice));
            if (unitHolder.currentChoice != null) {
               setUnitToStepper(applicationRate1Stepper, unitHolder.currentChoice.getName());
               setUnitToStepper(applicationRate2Stepper, unitHolder.currentChoice.getName());
               setUnitToStepper(applicationRateDeltaStepper, unitHolder.currentChoice.getName());
               setUnitToStepper(applicationRateMinStepper, unitHolder.currentChoice.getName());
               setUnitToStepper(applicationRateMaxStepper, unitHolder.currentChoice.getName());
               applicationRate1Stepper.setValue((float) productMixParameters.getDefaultRate() * (float) unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
               applicationRate2Stepper.setValue((float) productMixParameters.getRate2() * (float) unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
               applicationRateMaxStepper.setValue((float) productMixParameters.getMaxRate() * (float) unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
               applicationRateMinStepper.setValue((float) productMixParameters.getMinRate() * (float) unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
               applicationRateDeltaStepper.setValue((float) productMixParameters.getDeltaRate() * (float) unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
            }
            updateApplicationRateStepSize();
            calculateTotalAmountForProductMix();
            calculateNewApplicationRate1PerProduct();
            calculateNewApplicationRate2PerProduct();
            updateApplicationRateTable();
         }
      }

      //Add Advanced Data to UI
      {
         productUsagePickList.setSelectionByPosition(ProductUsage.findByValue(productMixParameters.getUsage().getValue()).ordinal());
         ProductUnits packageSizeUnit = ProductHelperMethods.retrieveProductPackageSizeUnits(productMixParameters, measurementSystemProductOther);
         if (packageSizeUnit != null) {
            this.setPackageSizeUnitToUnitText(packageSizeUnit.getName());
            packageSizeValueUnitText.setText(String.format("%.2f", productMixParameters.getPackageSize()));
         }
         setUnitToSegmentedToggleButtonGroup(packageSizeUnitPickList, productMixForm, measurementSystemProductOther, ProductDisplayItem.PACKAGE_SIZE);
         initializeDensityUnits();
         if (densityUnitPickList.getVisibility() == VISIBLE) {
            UnitsToggleHolder unitHolder = (UnitsToggleHolder) packageSizeUnitPickList.getTag();
            ProductUnits densityUnit = ProductHelperMethods.retrieveProductDensityUnits(productMixParameters, measurementSystemProductDensity);
            unitHolder.currentChoice = densityUnit;
            densityUnitPickList.setSelectionById((long) unitHolder.unitChoices.indexOf(unitHolder.currentChoice));
            this.setDensityUnitToUnitText(densityUnit.getName());
            densityValueUnitText.setText(String.format("%.2f", productMixParameters.getDensity()));
         }
      }
      if (actionType == ProductMixesDialogActionType.COPY) {
         if (productMix != null && productMix.getProductMixParameters() != null) {
            productMixTitleEditText.setText(String.format("%s - %s", productMix.getProductMixParameters().getName(), getContext().getString(R.string.copy)));
         }
         validateRequiredData();
      }
   }

   private void addDataToProductElement(ProductMixRecipe recipeElement, ProductMixElementHolder element, ArrayList<Product> products) {
      if (recipeElement != null) {
         Product product = recipeElement.getProduct();
         if (product != null) {
            log.info("beforeSetUnitToSegmentedToggleButtonGroup");
            setUnitToSegmentedToggleButtonGroup(element.productUnit, product.getForm(), measurementSystemProductOther, ProductDisplayItem.AMOUNT);
            UnitsToggleHolder unitHolder = (UnitsToggleHolder) element.productUnit.getTag();
            if (unitHolder == null) {
               log.warn("unitHolder is null ");
               unitHolder = new UnitsToggleHolder();
            }
            unitHolder.currentChoice = ProductMixRecipeHelper.retrieveAmountUnitsFromProductMixRecipe(recipeElement, measurementSystemProductVolume, measurementSystemProductMass);
            element.currentRecipe = recipeElement;
            PickListAdapter productListAdapter = new PickListAdapter(element.productPickList, getContext());
            fillPickListAdapterWithProducts(productListAdapter, products);
            element.productPickList.setAdapter(productListAdapter);
            if (products != null) {
               int productIndex = -1;
               for (Product tempProduct : products) {
                  if (product.getId() == tempProduct.getId()) {
                     productIndex = products.indexOf(tempProduct);
                     break;
                  }
               }
               element.productPickList.setSelectionByPosition(productIndex);
               if (unitHolder.currentChoice != null && unitHolder.unitChoices != null) {
                  element.productUnit.setEnabled(true);
                  int unitIndex = unitHolder.unitChoices.indexOf(unitHolder.currentChoice);
                  element.productUnit.setSelectionById((long) unitIndex);
                  element.productAmount.setEnabled(true);
                  setUnitToStepper(element.productAmount, unitHolder.currentChoice.getName());
                  element.productAmount.setValue((float) recipeElement.getAmount() * (float) unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
               }
            }
         }
      }
   }

   private void setUnitToStepper(StepperView stepper, String unit) {
      if (stepper != null) {
         stepper.setParameters(BigDecimal.valueOf(MIN_STEPPER), BigDecimal.valueOf(MAX_STEPPER), BigDecimal.valueOf(STEP_SIZE), BigDecimal.valueOf(stepper.getValue()), PRECISION,
               unit);
      }
   }

   /**
    * add new Product to the ProductViewList and fill the Product with neeeded Data
    */
   private void addProduct() {
      try {
         LinearLayout productElementLayout = (LinearLayout) inflate(getContext(), R.layout.product_mix_dialog_mix_products_element_view, null);
         productElementLayout.setPadding(0, 10, 0, 0);
         ProductMixElementHolder productElement = new ProductMixElementHolder(productElementLayout);
         initializeNewProduct(productElementLayout, productElement);
         if (mixProductsLayout != null) {
            mixProductsLayout.addView(productElementLayout);
            productMixList.add(productElement);
            productElement.productUnit.setEnabled(false);
         }
      }
      catch (Exception e) {
         log.error("productElement could not created", e);
      }
   }

   /**
    * // FIXME: using enum names for ui is a bug - see
    * https://polarion.cnhind.com/polarion/#/project/pfhmidevdefects/workitem?id=pfhmi-dev-defects-3034
    *
    * Makes ENUM_NAMES into friendlier Enum Names
    *
    * @param input ENUM string
    * @return converted string
    * @deprecated never use see https://polarion.cnhind.com/polarion/#/project/pfhmidevdefects/workitem?id=pfhmi-dev-defects-3034
    */
   private String friendlyName(String input) {
      String spaced = input.replace("_", " ").trim();
      return WordUtils.capitalize(spaced.toLowerCase());
   }

   /**
    * fill into Product pickerList all products.
    * @param productElementView the new product view which created in addProduct.
    */
   private void initializeNewProduct(final LinearLayout productElementView, final ProductMixElementHolder productElement) {
      final TextView productTitle = (TextView) productElementView.findViewById(R.id.product_mix_new_product_title);
      if (productTitle != null) {
         productTitle.setText(getResources().getString(R.string.product_mix_title_product, productMixList.size() + 1));
      }

      if (productElement != null) {
         productElement.removeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
               removeProduct(productElement);
            }
         });

         if (productElement.productPickList != null) {
            productElement.productPickList.setHeaderText(getResources().getString(R.string.product_mix_title_product, productMixList.size() + 1));
            PickListAdapter productListAdapter = new PickListAdapter(productElement.productPickList, getContext());
            fillPickListAdapterWithProducts(productListAdapter, productList);
            productElement.productPickList.setAdapter(productListAdapter);
            productElement.productPickList.setWidgetHandlesAddNewMode(false);
            createNewProductDialog(productElementView, productElement);
            productElement.productPickList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
                  productElement.productAmount.setEnabled(true);
                  productElement.productUnit.setEnabled(true);
                  if (productElement.productPickList.getItemByPosition(position) != null) {
                     Product product = getProductByName(productElement.productPickList.getItemByPosition(position).getValue());
                     if (product != null) {
                        ProductMixRecipe recipe = new ProductMixRecipe();
                        recipe.setProduct(product);
                        productElement.currentRecipe = recipe;
                        productElement.productPickList.setTag(product);
                        recipeDeleteList.remove(productElement.currentRecipe);

                        setUnitToSegmentedToggleButtonGroup(productElement.productUnit, product.getForm(), measurementSystemProductOther, ProductDisplayItem.AMOUNT);
                        productElement.productUnit.setToggleListSelectionListener(new SegmentedTogglePickListListener() {
                           @Override
                           public void onToggleButtonCheckedChanged(RadioGroup radioGroup, int position) {

                              UnitsToggleHolder unit = (UnitsToggleHolder) productElement.productUnit.getTag();
                              if (unit.unitChoices.size() > position) {
                                 unit.currentChoice = unit.unitChoices.get(position);
                              }
                              if (unit.currentChoice != null) {
                                 setUnitToStepper(productElement.productAmount, unit.currentChoice.getName());
                              }
                              calculateTotalAmountForProductMix();
                           }

                           @Override
                           public void onListItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
                              UnitsToggleHolder unit = (UnitsToggleHolder) productElement.productUnit.getTag();
                              if (unit.unitChoices.size() > position) {
                                 unit.currentChoice = unit.unitChoices.get(position);
                              }

                              if (unit.currentChoice != null) {
                                 setUnitToStepper(productElement.productAmount, unit.currentChoice.getName());
                                 calculateTotalAmountForProductMix();
                              }
                           }

                           @Override
                           public void onNoItemsSelected(AdapterView<?> adapterView) {

                           }
                        });
                        productElement.productAmount.setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
                           @Override
                           public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double value, boolean fromUser) {
                              if (fromUser) {
                                 calculateTotalAmountForProductMix();
                              }
                           }
                        });
                     }
                  }
                  if (productMixList.size() > 1) {
                     addRemoveButtonToFirstProduct();
                     productElement.removeButton.setVisibility(VISIBLE);
                  }
                  updateApplicationRateTable();
                  if (mixProductCategoryButton.isExpanded()) {
                     mixProductCategoryButton.resizeContent(false);
                  }
                  UnitsToggleHolder unit = (UnitsToggleHolder) productElement.productUnit.getTag();
                  if (unit != null && unit.currentChoice != null) {
                     setUnitToStepper(productElement.productAmount, unit.currentChoice.getName());
                  }
               }

               @Override
               public void onNothingSelected(AdapterView<?> adapterView) {
               }
            });

            setUnitToSegmentedToggleButtonGroup(productElement.productUnit, productMixForm, measurementSystemProductOther, ProductDisplayItem.AMOUNT);
         }
      }
   }

   private void createNewProductDialog(final LinearLayout productElementView, final ProductMixElementHolder productElement) {
      productElement.productPickList.setOnItemActionListener(new OnPickListItemActionListener() {
         private boolean isTitleSet = false;
         private boolean isFormSet = false;

         private void checkSaveButton(Button saveButton) {
            if (saveButton != null && isTitleSet && isFormSet) {
               saveButton.setEnabled(true);
            }
         }

         @Override
         public void onItemEditSelected(PickListItem pickListItem) {
         }

         @Override
         public void onItemEditingCompleted(PickListItem pickListItem, boolean b) {
         }

         @Override
         public void onItemDeleteRequested(PickListItem pickListItem) {

         }

         @Override
         public void onNewItemAdded(PickListItem pickListItem) {
         }

         @Override
         public void onItemCopied(PickListItem pickListItem, PickListItem pickListItem1) {

         }

         @Override
         public void onButton1Press() {
            final LinearLayout selectProductView = (LinearLayout) productElementView.findViewById(R.id.product_mix_product_left_select_product);
            selectProductView.setVisibility(GONE);
            final LinearLayout newProductView = (LinearLayout) productElementView.findViewById(R.id.product_mix_product_left_new_product);
            newProductView.setVisibility(VISIBLE);
            final EditText newProductEditText = (EditText) newProductView.findViewById(R.id.product_mix_product_edittext_add_new_product);
            newProductEditText.setText("");
            final PickList newProductFormPickList = (PickList) newProductView.findViewById(R.id.product_mix_product_picklist_product_type);
            Button newProductButtonCancel = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_cancel);
            newProductButtonCancel.setOnClickListener(new OnClickListener() {
               @Override
               public void onClick(View view) {
                  selectProductView.setVisibility(VISIBLE);
                  newProductView.setVisibility(GONE);
               }
            });
            final Button newProductButtonAdd = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_add);
            newProductButtonAdd.setEnabled(false);
            PickListAdapter productFormAdapter = new PickListAdapter(newProductFormPickList, getContext());
            for (ProductForm form : ProductForm.values()) {
               if (form != ProductForm.ANY) {
                  productFormAdapter.add(new PickListItem(form.getValue(), friendlyName(form.name())));
               }
            }
            newProductFormPickList.setAdapter(productFormAdapter);
            newProductFormPickList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l, boolean fromUser) {
                  isProductMixFormSet = true;
                  checkSaveButton(newProductButtonAdd);
                  if (newProductEditText.getText().toString().length() > 0) {
                     newProductButtonAdd.setEnabled(true);
                  }
                  else {
                     newProductButtonAdd.setEnabled(false);
                  }
               }

               @Override
               public void onNothingSelected(AdapterView<?> adapterView) {

               }
            });
            newProductEditText.setOnEditorActionListener(new OnEditorActionListener() {
               @Override
               public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                  if (actionId == EditorInfo.IME_ACTION_DONE) {
                     if (productMixTitleEditText.getText().toString().length() > 0) {
                        isTitleSet = true;
                        checkSaveButton(newProductButtonAdd);
                        if (newProductFormPickList.getSelectedItemPosition() >= 0) {
                           newProductButtonAdd.setEnabled(true);
                        }
                        else {
                           newProductButtonAdd.setEnabled(false);
                        }
                     }
                  }
                  return false;
               }
            });
            newProductButtonAdd.setOnClickListener(new OnClickListener() {
               @Override
               public void onClick(View view) {
                  Product newProduct = new Product();
                  newProduct.setName(newProductEditText.getText().toString());
                  newProduct.setForm(ProductForm.findByValue(newProductFormPickList.getSelectedItemPosition()));

                  if (measurementSystemProductOther != null && productUnitsList != null && !productUnitsList.isEmpty()) {
                     ProductUnits filteredProductUnits = getFilteredProductUnits(newProduct.getForm(), measurementSystemProductOther, ProductDisplayItem.RATES, productUnitsList).get(0);
                     ProductHelperMethods.bindProductRateUnits(newProduct, filteredProductUnits, measurementSystemProductOther);
                  }

                  ProductCommandParams params = new ProductCommandParams();
                  params.vipService = vipService;
                  params.product = newProduct;
                  new VIPAsyncTask<ProductCommandParams, Product>(params, new GenericListener<Product>() {
                     @Override
                     public void handleEvent(Product product) {
                        productList.add(product);
                        PickListAdapter adapter = productElement.productPickList.getAdapter();
                        adapter.add(new PickListItem(adapter.getCount() - 1, product.getName()));
                        productElement.productPickList.setAdapter(adapter);
                        productElement.productPickList.setSelectionByPosition(adapter.getCount() - 1);
                        selectProductView.setVisibility(VISIBLE);
                        newProductView.setVisibility(GONE);
                     }
                  }).execute(new SaveProductCommand());

               }
            });
         }

         @Override
         public void onCopyButtonPress(PickListItem pickListItem) {

         }

         @Override
         public void onEditButtonPress(PickListItem pickListItem) {
         }

         @Override
         public void onButton2Press() {
         }
      });
   }

   private void setUnitToSegmentedToggleButtonGroup(SegmentedToggleButtonGroupPickList segmentedToggleButtonGroup, ProductForm productForm, MeasurementSystem measurementSystem,
         ProductDisplayItem productDisplayItem) {
      if (segmentedToggleButtonGroup != null && productUnitsList != null && !productUnitsList.isEmpty()) {
         int listCount = segmentedToggleButtonGroup.getEntryCount();
         for (int buttonCounter = 0; buttonCounter < listCount; buttonCounter++) {
            segmentedToggleButtonGroup.removeEntry((long) buttonCounter);
         }
         UnitsToggleHolder holder = new UnitsToggleHolder();
         holder.unitChoices = new ArrayList<ProductUnits>(getFilteredProductUnits(productForm, measurementSystem, productDisplayItem, this.productUnitsList));
         for (int i = 0; i < holder.unitChoices.size(); i++) {
            segmentedToggleButtonGroup.addEntry((long) i, holder.unitChoices.get(i).getName());
         }
         if (holder.unitChoices.size() > 0) {
            holder.currentChoice = holder.unitChoices.get(0);
         }
         segmentedToggleButtonGroup.setTag(holder);
         segmentedToggleButtonGroup.setSelectionById((long) 0);
      }
      else {
         log.warn("segmentedToggleButtonGroup " + segmentedToggleButtonGroup.getSelectedValue() + " productUnitsList " + productUnitsList);
      }
   }

   private static List<ProductUnits> getFilteredProductUnits(ProductForm form, MeasurementSystem measurementSystem, ProductDisplayItem displayItem, List<ProductUnits> unitList) {
      List<ProductUnits> filteredProductUnits = new ArrayList<ProductUnits>();
      for (ProductUnits unit : unitList) {
         if (unit != null && unit.getForm() == form && unit.getMeasurementSystem() == measurementSystem && unit.getDisplayItem() == displayItem) {
            filteredProductUnits.add(unit);
         }
      }
      if (filteredProductUnits.isEmpty()) {
         log.warn("filteredProductUnits is null " + form.name() + " " + measurementSystem.name() + " displayItem " + displayItem.name() + " unitList Size " + unitList.size());
      }
      return filteredProductUnits;
   }

   /**
    * Read all Data and save it to productMix
    */
   private void saveProductMixToPCM() {
      if (this.vipService != null) {
         Product productMixParameters;
         ProductMix tempProductMix;

         if (actionType == ProductMixesDialogActionType.EDIT) {
            tempProductMix = this.productMix;
            productMixParameters = tempProductMix.getProductMixParameters();
         }
         else if (actionType == ProductMixesDialogActionType.COPY) {
            tempProductMix = new ProductMix(this.productMix);
            tempProductMix.setId(0);
            productMixParameters = new Product(tempProductMix.getProductMixParameters());
            productMixParameters.setId(0);
         }
         else {
            tempProductMix = this.productMix;
            productMixParameters = new Product();
         }
         ProductUnits packageSizeUnit = null;
         if (packageSizeUnitPickList.getTag() != null) {
            UnitsToggleHolder holder = (UnitsToggleHolder) packageSizeUnitPickList.getTag();
            packageSizeUnit = holder.currentChoice;
         }
         ProductUnits densityUnit = null;
         if (densityUnitPickList.getTag() != null) {
            UnitsToggleHolder holder = (UnitsToggleHolder) densityUnitPickList.getTag();
            densityUnit = holder.currentChoice;
         }
         ProductUnits applicationUnit = null;
         if (applicationRatesUnitsPickList.getTag() != null) {
            UnitsToggleHolder holder = (UnitsToggleHolder) applicationRatesUnitsPickList.getTag();
            applicationUnit = holder.currentChoice;
         }
         ProductUnits carrierUnit = null;
         if (carrierProductHolder.productPickList.getTag() != null) {
            UnitsToggleHolder holder = (UnitsToggleHolder) carrierProductHolder.productUnit.getTag();
            carrierUnit = holder.currentChoice;
         }
         ProductHelperMethods.bindProductDensityUnits(productMixParameters, densityUnit, measurementSystemProductDensity);
         ProductHelperMethods.bindProductPackageSizeUnits(productMixParameters, packageSizeUnit, measurementSystemProductOther);
         ProductHelperMethods.bindProductRateUnits(productMixParameters, applicationUnit, measurementSystemProductOther);
         ProductMixHelper.bindMixTotalUnits(tempProductMix, carrierUnit, measurementSystemProductOther);
         double baseFactor = 1;

         if (applicationUnit != null) {
            baseFactor = applicationUnit.getMultiplyFactorFromBaseUnits();
         }
         productMixParameters.setDefaultRate(applicationRate1Stepper.getValue() / baseFactor);
         productMixParameters.setRate2(applicationRate2Stepper.getValue() / baseFactor);
         productMixParameters.setMinRate(applicationRateMinStepper.getValue() / baseFactor);
         productMixParameters.setMaxRate(applicationRateMaxStepper.getValue() / baseFactor);
         productMixParameters.setDeltaRate(applicationRateDeltaStepper.getValue() / baseFactor);
         productMixParameters.setForm(this.productMixForm);

         if (packageSizeValueUnitText.getText().toString().trim().length() > 0
               && !packageSizeValueUnitText.getText().toString().equalsIgnoreCase(getResources().getString(R.string.product_mix_unittext_default_text))) {
            productMixParameters.setPackageSize(Float.valueOf(packageSizeValueUnitText.getText().toString()));
         }

         if (packageSizeValueUnitText.getText().toString().trim().length() > 0 && densityValueUnitText.getVisibility() == VISIBLE
               && !densityValueUnitText.getText().toString().equalsIgnoreCase(getResources().getString(R.string.product_mix_unittext_default_text))) {
            productMixParameters.setDensity(Float.valueOf(densityValueUnitText.getText().toString()));
         }
         ProductMixRecipe productCarrier;
         if (actionType != ProductMixesDialogActionType.EDIT) {
            productCarrier = new ProductMixRecipe();
            productCarrier.setProduct(carrierProductHolder.currentRecipe.getProduct());
         }
         else {
            productCarrier = carrierProductHolder.currentRecipe;
         }
         ProductMixRecipeHelper.setAmountAndUnitsToProductMixRecipe(productCarrier,
               ((UnitsToggleHolder) carrierProductHolder.productUnit.getTag()).currentChoice, carrierProductHolder.productAmount.getValue(),
               measurementSystemProductVolume, measurementSystemProductMass);
         tempProductMix.setMixType(MixType.FORMULA);
         tempProductMix.setProductCarrier(productCarrier);
         tempProductMix.setRecipe(createProductRecipeList());
         productMixParameters.setName(productMixTitleEditText.getText().toString());
         tempProductMix.setProductMixParameters(productMixParameters);
         tempProductMix.setMixTotalAmount(totalAmount);

         try {
            ProductMixCommandParams params = new ProductMixCommandParams();
            params.vipService = vipService;
            params.productMix = tempProductMix;
            new VIPAsyncTask<ProductMixCommandParams, ProductMix>(params, new GenericListener<ProductMix>() {
               @Override
               public void handleEvent(ProductMix productMix) {
                  ProductMixDialog.this.productMix = productMix;
                  if (callback != null) {
                     callback.loadProductMix(productMix);
                  }
               }
            }).execute(new SaveProductMixCommand());
         }
         catch (Exception e) {
            log.error("could not save", e);
         }
      }
      else {
         log.warn("vipService is null");
      }
   }

   /**
    * check that application max rate is the highest rate, if not set the other to the same as application maximum rate
    */
   private void validateApplicationMaxRange() {
      if (applicationRateMaxStepper.getValue() < applicationRate1Stepper.getValue()) {
         applicationRate1Stepper.setValue(applicationRateMaxStepper.getValue());
      }

      if (applicationRateMaxStepper.getValue() < applicationRate2Stepper.getValue()) {
         applicationRate2Stepper.setValue(applicationRateMaxStepper.getValue());
      }

      if (applicationRateMaxStepper.getValue() < applicationRateMinStepper.getValue()) {
         applicationRateMaxStepper.setValue(applicationRateMinStepper.getValue());
      }
   }

   /**
    * check that application min rate is the lowest rate, if not set the other to the same as application minimum rate
    */
   private void validateApplicationMinRange() {
      if (applicationRateMinStepper.getValue() > applicationRate1Stepper.getValue()) {
         applicationRate1Stepper.setValue(applicationRateMinStepper.getValue());
      }

      if (applicationRateMinStepper.getValue() > applicationRate2Stepper.getValue()) {
         applicationRate2Stepper.setValue(applicationRateMinStepper.getValue());
      }
   }

   /**
    * check that application rate 2 is between the maximum and minimum rate, if not will decrease application minimum rate or increase the application maximum rate
    */
   private void validateApplicationRate1() {
      if (applicationRate1Stepper.getValue() < applicationRateMinStepper.getValue()) {
         applicationRateMinStepper.setValue(applicationRate1Stepper.getValue());
      }

      if (applicationRate1Stepper.getValue() > applicationRateMaxStepper.getValue()) {
         applicationRateMaxStepper.setValue(applicationRate1Stepper.getValue());
      }
   }

   /**
    * check that application rate 2 is between the maximum and minimum rate, if not will decrease application minimum rate or increase the application maximum rate
    */
   private void validateApplicationRate2() {
      if (applicationRate2Stepper.getValue() < applicationRateMinStepper.getValue()) {
         applicationRateMinStepper.setValue(applicationRate2Stepper.getValue());
      }

      if (applicationRate2Stepper.getValue() > applicationRateMaxStepper.getValue()) {
         applicationRateMaxStepper.setValue(applicationRate2Stepper.getValue());
      }
   }

   /**
    * update stepsize from application rate 1 and application rate 2 with Delta value
    */
   private void updateApplicationRateStepSize() {
      float stepSize = applicationRateDeltaStepper.getValue();
      applicationRate1Stepper.setStep(BigDecimal.valueOf(stepSize));
      applicationRate2Stepper.setStep(BigDecimal.valueOf(stepSize));
   }

   /**
    * create a Product recipe arrayList with all products from the productviewlist
    * @return arrayList with all products for the product mix
    */
   private List<ProductMixRecipe> createProductRecipeList() {

      List<ProductMixRecipe> productMixRecipeList = new ArrayList<ProductMixRecipe>();
      if (actionType == ProductMixesDialogActionType.EDIT) {
         productMixRecipeList = productMix.getRecipe();
      }
      productMixRecipeList.removeAll(recipeDeleteList);

      for (ProductMixElementHolder productElementHolder : productMixList) {
         if (productElementHolder.currentRecipe != null) {
            ProductMixRecipe recipe = productElementHolder.currentRecipe;
            if (actionType != ProductMixesDialogActionType.EDIT) {
               recipe = new ProductMixRecipe();
               recipe.setProduct(productElementHolder.currentRecipe.getProduct());
            }
            ProductMixRecipeHelper.setAmountAndUnitsToProductMixRecipe(recipe,((UnitsToggleHolder) productElementHolder.productUnit.getTag()).currentChoice, productElementHolder.productAmount.getValue(),
                  measurementSystemProductVolume, measurementSystemProductMass);
            if (!productMixRecipeList.contains(recipe)) {
               productMixRecipeList.add(recipe);
            }
         }
      }
      return productMixRecipeList;
   }

   /**
    * Carrier Product will get filtered data depend on the selection of the ProductForm, if the productForm is select.
    * the PickerList will filled with the filtered data.
    */
   private void initializeCarrierProduct() {
      final LinearLayout productElement = (LinearLayout) inflate(getContext(), R.layout.product_mix_dialog_mix_products_element_view, null);
      carrierProductHolder = new ProductMixElementHolder(productElement);
      carrierProductHolder.productPickList.setHeaderText(R.string.product_mix_title_carrier);
      if (carrierProductHolder.productPickList != null) {
         carrierProductHolder.productPickList.setWidgetHandlesAddNewMode(false);
         carrierProductHolder.productPickList.setOnItemActionListener(new OnProductPickListItemActionListener(productElement));
         carrierProductHolder.productPickList.setOnItemSelectedListener(new OnProductPickListItemSelectedListener());
      }
      if (actionType == ProductMixesDialogActionType.ADD) {
         setUnitToSegmentedToggleButtonGroup(carrierProductHolder.productUnit, productMixForm, measurementSystemProductOther, ProductDisplayItem.AMOUNT);
         carrierProductHolder.productUnit.setEnabled(false);
      }
      mixProductsLayout.addView(productElement);
   }

   private class OnProductPickListItemSelectedListener implements PickListEditable.OnItemSelectedListener {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
         carrierProductHolder.productAmount.setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
            @Override
            public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double v, boolean fromUser) {
               if (fromUser) {
                  validateRequiredData();
                  calculateTotalAmountForProductMix();
               }
            }
         });
         carrierProductHolder.productAmount.setEnabled(true);
         carrierProductHolder.productUnit.setEnabled(true);
         if (position >= 0) {
            Product product = getProductByName(carrierProductHolder.productPickList.getItemByPosition(position).getValue());
            carrierProductHolder.productPickList.setTag(product);
            ProductMixRecipe recipe = new ProductMixRecipe();
            recipe.setProduct(product);
            carrierProductHolder.currentRecipe = recipe;
            if (product != null) {
               setUnitToSegmentedToggleButtonGroup(carrierProductHolder.productUnit, product.getForm(), measurementSystemProductOther, ProductDisplayItem.AMOUNT);
            }
            carrierProductHolder.productUnit.setToggleListSelectionListener(new SegmentedTogglePickListListener() {
               @Override
               public void onToggleButtonCheckedChanged(RadioGroup radioGroup, int position) {
                  UnitsToggleHolder unit = (UnitsToggleHolder) carrierProductHolder.productUnit.getTag();
                  if (unit != null) {
                     if (unit.currentChoice != null) {
                        if (unit.unitChoices.size() > position) {
                           unit.currentChoice = unit.unitChoices.get(position);
                        }
                        setUnitToStepper(carrierProductHolder.productAmount, unit.currentChoice.getName());
                        carrierProductHolder.productUnit.setTag(unit);
                     }
                  }
                  calculateTotalAmountForProductMix();
               }

               @Override
               public void onListItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
                  UnitsToggleHolder unit = (UnitsToggleHolder) carrierProductHolder.productUnit.getTag();
                  if (unit != null) {
                     if (unit.currentChoice != null) {
                        if (unit.unitChoices.size() > position) {
                           unit.currentChoice = unit.unitChoices.get(position);
                        }
                        setUnitToStepper(carrierProductHolder.productAmount, unit.currentChoice.getName());
                        carrierProductHolder.productUnit.setTag(unit);
                        calculateTotalAmountForProductMix();
                     }
                  }
               }

               @Override
               public void onNoItemsSelected(AdapterView<?> adapterView) {

               }
            });

            isCarrierSet = true;
            validateRequiredData();
            UnitsToggleHolder unit = (UnitsToggleHolder) carrierProductHolder.productUnit.getTag();
            if (unit != null && unit.currentChoice != null) {
               setUnitToStepper(carrierProductHolder.productAmount, unit.currentChoice.getName());
            }
            updateApplicationRateTable();
         }
         else {
            log.warn("Wrong Position");
         }
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {
      }
   }

   private class OnProductPickListItemActionListener implements OnPickListItemActionListener {
      private final LinearLayout productElement;
      private boolean isTitleSet = false;
      private boolean isFormSet = false;

      private OnProductPickListItemActionListener(LinearLayout productElement){
         this.productElement = productElement;
      }

      private void checkSaveButton(Button saveButton) {
         if (saveButton != null && isTitleSet && isFormSet) {
            saveButton.setEnabled(true);
         }
      }

      @Override
      public void onItemEditSelected(PickListItem pickListItem) {
      }

      @Override
      public void onItemEditingCompleted(PickListItem pickListItem, boolean b) {
      }

      @Override
      public void onItemDeleteRequested(PickListItem pickListItem) {

      }

      @Override
      public void onNewItemAdded(PickListItem pickListItem) {
      }

      @Override
      public void onItemCopied(PickListItem pickListItem, PickListItem pickListItem1) {

      }

      @Override
      public void onButton1Press() {
         final LinearLayout selectProductView = (LinearLayout) productElement.findViewById(R.id.product_mix_product_left_select_product);
         selectProductView.setVisibility(GONE);
         final LinearLayout newProductView = (LinearLayout) productElement.findViewById(R.id.product_mix_product_left_new_product);
         newProductView.setVisibility(VISIBLE);
         final TextView newCarrierTitle = (TextView) newProductView.findViewById(R.id.product_mix_new_product_title);
         newCarrierTitle.setText(getResources().getString(R.string.product_mix_title_add_new_carrier));
         final EditText newProductEditText = (EditText) newProductView.findViewById(R.id.product_mix_product_edittext_add_new_product);
         newProductEditText.setText("");
         newProductEditText.setHint(getResources().getString(R.string.product_mix_title_carrier));
         newProductEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
         final PickList newProductFormPickList = (PickList) newProductView.findViewById(R.id.product_mix_product_picklist_product_type);
         Button newProductButtonCancel = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_cancel);
         newProductButtonCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
               selectProductView.setVisibility(VISIBLE);
               newProductView.setVisibility(GONE);
               productMixFormPickList.setEnabled(true);
            }
         });
         final Button newProductButtonAdd = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_add);
         newProductButtonAdd.setEnabled(false);
         PickListAdapter productFormAdapter = new PickListAdapter(newProductFormPickList, getContext());

         for (ProductForm form : ProductForm.values()) {
            if (form != ProductForm.ANY) {
               productFormAdapter.add(new PickListItem(form.getValue(), friendlyName(form.name())));
            }
         }
         newProductFormPickList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l, boolean fromUser) {
               isProductMixFormSet = true;
               checkSaveButton(newProductButtonAdd);
               if (newProductEditText.getText().toString().length() > 0) {
                  newProductButtonAdd.setEnabled(true);
               }
               productMixFormPickList.setSelectionByPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
         });

         newProductFormPickList.setAdapter(productFormAdapter);
         if (productMixFormPickList.getSelectedItemPosition() > -1) {
            newProductFormPickList.setSelectionByPosition(productMixFormPickList.getSelectedItemPosition());
            //TODO until core-team fixed class cast exception
            /*newProductFormPickList.setEnabled(false);
            productMixFormPickList.setEnabled(false);*/
         }
         //TODO until core-team fixed class cast exception
         /*else {
            newProductFormPickList.setEnabled(true);
            productMixFormPickList.setEnabled(true);
         }*/
         newProductEditText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
               if (actionId == EditorInfo.IME_ACTION_DONE) {
                  if (isTitleSet = !newProductEditText.getText().toString().isEmpty()) {
                     checkSaveButton(newProductButtonAdd);
                     if (newProductFormPickList.getSelectedItemPosition() >= 0) {
                        newProductButtonAdd.setEnabled(true);
                     }
                  }
                  else {
                     newProductButtonAdd.setEnabled(false);
                  }
               }
               return false;
            }
         });
         newProductButtonAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
               Product newProduct = new Product();
               newProduct.setName(newProductEditText.getText().toString());
               newProduct.setForm(ProductForm.findByValue(newProductFormPickList.getSelectedItemPosition()));
               productMixFormPickList.setSelectionByPosition(newProductFormPickList.getSelectedItemPosition());
               ProductHelperMethods.bindProductRateUnits(newProduct, getFilteredProductUnits(newProduct.getForm(), measurementSystemProductOther, ProductDisplayItem.RATES, productUnitsList).get(0), measurementSystemProductOther);
               ProductCommandParams params = new ProductCommandParams();
               params.vipService = vipService;
               params.product = newProduct;
               new VIPAsyncTask<ProductCommandParams, Product>(params, new GenericListener<Product>() {
                  @Override
                  public void handleEvent(Product product) {
                     productList.add(product);
                     PickListAdapter adapter = carrierProductHolder.productPickList.getAdapter();
                     adapter.add(new PickListItem(adapter.getCount() - 1, product.getName()));
                     carrierProductHolder.productPickList.setSelectionByPosition(adapter.getCount() - 1);
                     selectProductView.setVisibility(VISIBLE);
                     newProductView.setVisibility(GONE);
                  }
               }).execute(new SaveProductCommand());
               productMixFormPickList.setSelectionByPosition(newProductFormPickList.getSelectedItemPosition());
               productMixFormPickList.setEnabled(true);
            }
         });
      }

      @Override
      public void onCopyButtonPress(PickListItem pickListItem) {

      }

      @Override
      public void onEditButtonPress(PickListItem pickListItem) {
      }

      @Override
      public void onButton2Press() {
      }
   }

   /**
    * will initialize the Mix Products segement.
    */
   private void initializeMixProductsView() {
      if (mixProductsLayout != null) {
         initializeCarrierProduct();
         addProduct();

         addMoreButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
               addProduct();
               mixProductCategoryButton.resizeContent(false);
               ProductMixDialog.this.invalidate();
            }
         });
      }
   }

   /**
    * Initialize Application Rates View
    */
   private void initializeApplicationRatesView() {
      this.applicationProductTableLayoutOverView = (LinearLayout) this.findViewById(R.id.product_mix_tablelayout_application_rates_per_product);
      this.applicationRatesCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_categorybuttons_application_rate_view);
      if (applicationRate1Stepper != null) {
         applicationRate1Stepper.setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
            @Override
            public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double v, boolean fromUser) {
               validateRequiredData();
               validateApplicationRate1();
               calculateNewApplicationRate1PerProduct();
            }
         });
      }
      if (applicationRate2Stepper != null) {
         applicationRate2Stepper.setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
            @Override
            public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double v, boolean fromUser) {
               validateApplicationRate2();
               calculateNewApplicationRate2PerProduct();
            }
         });
      }
      if (applicationRateMinStepper != null) {
         applicationRateMinStepper.setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
            @Override
            public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double v, boolean fromUser) {
               if (fromUser) {
                  validateApplicationMinRange();
               }
            }
         });
      }

      if (applicationRateMaxStepper != null) {
         applicationRateMaxStepper.setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
            @Override
            public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double v, boolean fromUser) {
               if (fromUser) {
                  validateApplicationMaxRange();
               }
            }
         });
      }

      if (applicationRateDeltaStepper != null) {
         applicationRateDeltaStepper.setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
            @Override
            public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double v, boolean fromUser) {
               if (fromUser) {
                  updateApplicationRateStepSize();
               }
            }
         });

         if (actionType == ProductMixesDialogActionType.ADD) {
            applicationRateDeltaStepper.setValue(1);
         }
      }
      initializeApplicationRatesSegmentedToggleButtonGroup(productMixForm);
   }

   /**
    * initialize the Application Units in relation to the product form, fill the SegmentedToggleButtonGroup with Units to select
    * @param productForm the selected product from
    */
   private void initializeApplicationRatesSegmentedToggleButtonGroup(ProductForm productForm) {
      setUnitToSegmentedToggleButtonGroup(applicationRatesUnitsPickList, productForm, measurementSystemProductOther, ProductDisplayItem.RATES);
      if (applicationRatesUnitsPickList != null) {
         applicationRatesUnitsPickList.setToggleListSelectionListener(new SegmentedTogglePickListListener() {
            @Override
            public void onToggleButtonCheckedChanged(RadioGroup radioGroup, int position) {
               UnitsToggleHolder holder = (UnitsToggleHolder) applicationRatesUnitsPickList.getTag();
               if (holder.unitChoices.size() > position) {
                  holder.currentChoice = holder.unitChoices.get(position);
                  if (holder.currentChoice != null) {
                     updateApplicationRatesView(holder.currentChoice.getName());
                  }
                  applicationRatesUnitsPickList.setTag(holder);
                  calculateNewApplicationRate1PerProduct();
                  calculateNewApplicationRate2PerProduct();
               }
            }

            @Override
            public void onListItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
               UnitsToggleHolder holder = (UnitsToggleHolder) applicationRatesUnitsPickList.getTag();
               if (holder.unitChoices.size() > position) {
                  holder.currentChoice = holder.unitChoices.get(position);
                  if (holder.currentChoice != null) {
                     updateApplicationRatesView(holder.currentChoice.getName());
                  }
                  applicationRatesUnitsPickList.setTag(holder);
                  calculateNewApplicationRate1PerProduct();
                  calculateNewApplicationRate2PerProduct();
               }
            }

            @Override
            public void onNoItemsSelected(AdapterView<?> adapterView) {
            }
         });

         UnitsToggleHolder holder = (UnitsToggleHolder) applicationRatesUnitsPickList.getTag();
         if (holder != null && holder.currentChoice != null) {
            updateApplicationRatesView(holder.currentChoice.getName());
         }
      }
   }

   /**
    * Initialize the Advanced View
    */
   private void initializeAdvancedView() {
      PickListAdapter advancedUsagePicklistAdapter = new PickListAdapter(productUsagePickList, getContext());
      this.advancedCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_advanced_categorybuttons_view);
      for (ProductUsage usage : ProductUsage.values()) {
         advancedUsagePicklistAdapter.add(new PickListItem(usage.getValue(), friendlyName(usage.name())));
      }
      productUsagePickList.setAdapter(advancedUsagePicklistAdapter);
      initializePackageSizeUnits();
      initializeDensityUnits();

   }

   /**
    * initialize packagesize units, fill SegmentedToggleButtonGroupPickList with units in relation to the measurementsystem
    */
   private void initializePackageSizeUnits() {
      if (packageSizeUnitPickList != null) {
         setUnitToSegmentedToggleButtonGroup(packageSizeUnitPickList, productMixForm, measurementSystemProductOther, ProductDisplayItem.PACKAGE_SIZE);
         packageSizeUnitPickList.setToggleListSelectionListener(new SegmentedTogglePickListListener() {
            @Override
            public void onToggleButtonCheckedChanged(RadioGroup radioGroup, int position) {
               UnitsToggleHolder holder = (UnitsToggleHolder) packageSizeUnitPickList.getTag();
               if (holder.unitChoices.size() > position) {
                  holder.currentChoice = holder.unitChoices.get(position);
                  if (holder.currentChoice != null) {
                     setPackageSizeUnitToUnitText(holder.currentChoice.getName());
                  }
               }
            }

            @Override
            public void onListItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
               UnitsToggleHolder holder = (UnitsToggleHolder) packageSizeUnitPickList.getTag();
               if (holder.unitChoices.size() > position) {
                  holder.currentChoice = holder.unitChoices.get(position);
                  if (holder.currentChoice != null) {
                     setPackageSizeUnitToUnitText(holder.currentChoice.getName());
                  }
               }
            }

            @Override
            public void onNoItemsSelected(AdapterView<?> adapterView) {

            }
         });

         UnitsToggleHolder holder = (UnitsToggleHolder) packageSizeUnitPickList.getTag();
         if (holder != null && holder.currentChoice != null) {
            setPackageSizeUnitToUnitText(holder.currentChoice.getName());
         }
      }
   }

   /**
    * If new Carrier will select, reset the Carrier view, disable the stepper and the SegmentedToggleButtonGroupPickList.
    */
   private void resetCarrierProduct() {
      if (carrierProductHolder != null) {
         carrierProductHolder.productUnit.setEnabled(false);
         carrierProductHolder.productAmount.setEnabled(false);
         carrierProductHolder.productAmount.setValue(0);
         isCarrierSet = false;
         validateRequiredData();
      }
   }

   /**
    * InitializeDensityUnits if ProductForm is not Seed.
    * set Density units to toggle button and setup the unitText
    * if Product is Seed will remove the Density
    */
   private void initializeDensityUnits() {

      setUnitToSegmentedToggleButtonGroup(densityUnitPickList, productMixForm, measurementSystemProductOther, ProductDisplayItem.UNIT_DENSITY);
      if (!(productMixForm == ProductForm.SEED || productMixForm == ProductForm.PLANT)) {
         densityUnitPickList.setVisibility(GONE);
         densityValueUnitText.setVisibility(GONE);
         densityValueTitleText.setVisibility(GONE);
      }
      else {
         densityUnitPickList.setVisibility(VISIBLE);
         densityValueUnitText.setVisibility(VISIBLE);
         densityValueTitleText.setVisibility(VISIBLE);
         densityUnitPickList.setToggleListSelectionListener(new SegmentedTogglePickListListener() {
            @Override
            public void onToggleButtonCheckedChanged(RadioGroup radioGroup, int position) {
               UnitsToggleHolder holder = (UnitsToggleHolder) densityUnitPickList.getTag();
               if (holder.unitChoices.size() > position) {
                  holder.currentChoice = holder.unitChoices.get(position);
                  if (holder.currentChoice != null) {
                     setDensityUnitToUnitText(holder.currentChoice.getName());
                  }
               }
            }

            @Override
            public void onListItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
               UnitsToggleHolder holder = (UnitsToggleHolder) densityUnitPickList.getTag();
               if (holder.unitChoices.size() > position) {
                  holder.currentChoice = holder.unitChoices.get(position);
                  if (holder.currentChoice != null) {
                     setDensityUnitToUnitText(holder.currentChoice.getName());
                  }
               }
            }

            @Override
            public void onNoItemsSelected(AdapterView<?> adapterView) {

            }
         });
         UnitsToggleHolder holder = (UnitsToggleHolder) densityUnitPickList.getTag();
         if (holder != null && holder.currentChoice != null) {
            setDensityUnitToUnitText(holder.currentChoice.getName());
         }
      }
   }

   private void setPackageSizeUnitToUnitText(String unitString) {
      if (this.packageSizeValueUnitText != null && unitString != null && !unitString.isEmpty()) {
         this.packageSizeValueUnitText.setUnits(unitString);
      }
   }

   private void setDensityUnitToUnitText(String unitString) {
      if (this.densityValueUnitText != null && unitString != null && !unitString.isEmpty()) {
         this.densityValueUnitText.setUnits(unitString);
      }
   }

   /**
    * make the Remove Button visible for the First Product if more than 1 Products set
    */
   private void addRemoveButtonToFirstProduct() {
      if (!productMixList.isEmpty()) {
         ProductMixElementHolder firstProduct = productMixList.get(0);
         if (firstProduct != null) {
            firstProduct.removeButton.setVisibility(VISIBLE);
         }
      }
   }

   /**
    * make the Remove Button invisible if less than 2 Products are set.
    */
   private void removeRemoveButtonFromFirstProduct() {
      if (!productMixList.isEmpty()) {
         ProductMixElementHolder firstProduct = productMixList.get(0);
         if (firstProduct != null) {
            firstProduct.removeButton.setVisibility(GONE);
         }
      }
   }

   /**
    * will call by the Remove button click
    * remove the selected Product from the View and from the List.
    * @param productElement view which will removed
    */
   private void removeProduct(ProductMixElementHolder productElement) {
      if (productElement != null) {
         if (!productMixList.isEmpty()) {
            if (!productMixList.remove(productElement)) {
               log.warn("Could not Remove " + productElement);
            }
            recipeDeleteList.add(productElement.currentRecipe);
            mixProductsLayout.removeView(productElement.elementView);
            updateProductViewList();
            updateApplicationRateTable();
            this.mixProductCategoryButton.resizeContent(false);
            if (productMixList.size() < 2) {
               removeRemoveButtonFromFirstProduct();
            }
         }
      }
   }

   /**
    * if an product would be removed the Product indexes will updated
    */
   private void updateProductViewList() {
      if (!productMixList.isEmpty()) {
         isOneProductMixSet = false;
         for (int i = 0; i < productMixList.size(); i++) {
            ProductMixElementHolder productElement = productMixList.get(i);
            productElement.productPickList.setHeaderText(getResources().getString(R.string.product_mix_title_product, i + 1));
            if (!isOneProductMixSet && productElement.productPickList.getSelectedItemPosition() >= 0) {
               isOneProductMixSet = true;
            }
         }
         validateRequiredData();
      }
   }

   /**
    * Helper Method to fill Pickerlist with the Products
    * @param picklistAdapter Adapter for the PickList
    * @param productList list with Product which will add to the PickListAdapter
    */
   private void fillPickListAdapterWithProducts(PickListAdapter picklistAdapter, ArrayList<Product> productList) {
      if (picklistAdapter != null && productList != null && !productList.isEmpty()) {
         for (int i = 0; i < productList.size(); i++) {
            picklistAdapter.add(new PickListItem(i, productList.get(i).getName()));
         }
      }
   }

   /**
    * filter product list by Form and return the filtered list
    * @param productList ArrayList with all products
    * @param form Productform which has to be filtered
    * @return a list with all product of the searched productform
    */
   private ArrayList<Product> filterProductList(ArrayList<Product> productList, ProductForm form) {
      ArrayList<Product> filteredProductList = new ArrayList<Product>();
      if (productList != null && form != null) {
         for (Product product : productList) {
            if (product != null && product.getForm() == form) {
               filteredProductList.add(product);
            }
         }
      }
      return filteredProductList;

   }

   /**
    * If an Product would be selected or removed, in Application Rates will show an overview about all used Products.
    * The Table Layout will show all Products and the Carrier
    * If no Product used the TableLayout will be invisible
    */
   private void updateApplicationRateTable() {
      if (productMixTable != null) {
         int childCounter = productMixTable.getChildCount();
         for (int i = 1; i < childCounter; i++) {
            productMixTable.removeViewAt(1);
         }
         int viewCounter = 1;

         //Add Carrier to table
         if (carrierProductHolder != null && carrierProductHolder.currentRecipe != null) {
            TableRow tableRow = createTableRow(carrierProductHolder.currentRecipe.getProduct());
            productMixTable.addView(tableRow, viewCounter++);
         }

         if (!productMixList.isEmpty()) {

            for (ProductMixElementHolder productElement : productMixList) {
               if (productElement.currentRecipe != null) {
                  if (productElement.currentRecipe.getProduct() != null) {
                     Product product = productElement.currentRecipe.getProduct();
                     TableRow tableRow = createTableRow(product);
                     if (tableRow != null) {
                        productMixTable.addView(tableRow, viewCounter++);
                     }
                  }
               }
            }
            if (productMixTable.getChildCount() > 2) {
               isOneProductMixSet = true;
               validateRequiredData();
            }
         }
         if (productMixTable.getChildCount() > 1) {
            applicationProductTableLayoutOverView.setVisibility(VISIBLE);
         }
         else {
            applicationProductTableLayoutOverView.setVisibility(GONE);
         }
      }
   }

   /**
    * create Tablerow with product data
    * @param product current product to extract the data into the several cells
    * @return created TableRow
    */
   private TableRow createTableRow(Product product) {
      TableRow tableRow = null;
      if (product != null) {
         tableRow = new TableRow(getContext());
         ProductUnits unit = ProductHelperMethods.retrieveProductRateUnits(product, measurementSystemProductOther);
         int tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell;
         if (productMixTable.getChildCount() - 1 % 2 == 1) {
            tableRowBackground = R.drawable.product_mix_dialog_application_rates_table_background_cell_gray;
         }
         tableRow.setGravity(Gravity.CENTER);

         LinearLayout productNameLayout = new LinearLayout(getContext());
         productNameLayout.setBackgroundResource(tableRowBackground);
         productNameLayout.setOrientation(LinearLayout.HORIZONTAL);
         productNameLayout.setGravity(Gravity.START | Gravity.CENTER);

         ImageView productTypeImage = new ImageView(getContext());
         productTypeImage.setMaxWidth(getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setMinimumWidth(getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setMaxHeight(getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setMinimumHeight(getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));
         productTypeImage.setBackgroundResource(getProductTypeImageId(product.getForm()));
         productNameLayout.addView(productTypeImage, getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height),
               getResources().getDimensionPixelOffset(R.dimen.product_mix_product_image_width_height));

         TextView productNameTextView = new TextView(getContext());
         productNameTextView.setText(product.getName());
         productNameTextView.setTextSize(getResources().getDimensionPixelSize(R.dimen.product_mix_dialog_application_rate_name_text_size));
         productNameTextView.setTextColor(getResources().getColor(R.color.defaultTextColor));
         productNameTextView.setPadding(10, 0, 0, 0);
         productNameTextView.setTypeface(null, Typeface.BOLD);
         productNameLayout.addView(productNameTextView);

         tableRow.addView(productNameLayout);

         TextView applicationRate1TextView = new TextView(getContext());
         applicationRate1TextView.setTextSize(getResources().getDimensionPixelSize(R.dimen.product_mix_dialog_application_rate_text_size));
         applicationRate1TextView.setTextColor(getResources().getColor(R.color.defaultTextColor));
         applicationRate1TextView.setTypeface(null, Typeface.BOLD);
         applicationRate1TextView.setGravity(Gravity.START | Gravity.CENTER);
         applicationRate1TextView.setEllipsize(TruncateAt.MIDDLE);
         applicationRate1TextView.setBackgroundResource(tableRowBackground);
         if (unit != null) {
            applicationRate1TextView.setText(String.format("   %.2f %s", product.getDefaultRate() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate1TextView.setText(String.format("   %.2f", product.getDefaultRate()));
         }
         tableRow.addView(applicationRate1TextView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

         TextView applicationRate2TextView = new TextView(getContext());
         applicationRate2TextView.setTextSize(getResources().getDimensionPixelSize(R.dimen.product_mix_dialog_application_rate_text_size));
         applicationRate2TextView.setTypeface(null, Typeface.BOLD);
         applicationRate2TextView.setGravity(Gravity.START | Gravity.CENTER);
         applicationRate2TextView.setEllipsize(TruncateAt.MIDDLE);
         applicationRate2TextView.setTextColor(getResources().getColor(R.color.defaultTextColor));
         applicationRate2TextView.setBackgroundResource(tableRowBackground);
         if (unit != null) {
            applicationRate2TextView.setText(String.format("   %.2f %s", product.getRate2() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate2TextView.setText(String.format("   %.2f", product.getRate2()));
         }

         tableRow.addView(applicationRate2TextView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
      }
      return tableRow;
   }

   /**
    * update all Application rate steppers with the new unit string
    * @param unitString new unit string
    */
   private void updateApplicationRatesView(String unitString) {
      applicationRate1Stepper.setParameters(BigDecimal.valueOf(MIN_STEPPER), BigDecimal.valueOf(MAX_STEPPER), BigDecimal.valueOf(STEP_SIZE),
            BigDecimal.valueOf(applicationRate1Stepper.getValue()), PRECISION, unitString);
      applicationRate2Stepper.setParameters(BigDecimal.valueOf(MIN_STEPPER), BigDecimal.valueOf(MAX_STEPPER), BigDecimal.valueOf(STEP_SIZE),
            BigDecimal.valueOf(applicationRate2Stepper.getValue()), PRECISION, unitString);
      applicationRateDeltaStepper.setParameters(BigDecimal.valueOf(MIN_STEPPER), BigDecimal.valueOf(MAX_STEPPER), BigDecimal.valueOf(STEP_SIZE),
            BigDecimal.valueOf(applicationRateDeltaStepper.getValue()), PRECISION, unitString);
      applicationRateMaxStepper.setParameters(BigDecimal.valueOf(MIN_STEPPER), BigDecimal.valueOf(MAX_STEPPER), BigDecimal.valueOf(STEP_SIZE),
            BigDecimal.valueOf(applicationRateMaxStepper.getValue()), PRECISION, unitString);
      applicationRateMinStepper.setParameters(BigDecimal.valueOf(MIN_STEPPER), BigDecimal.valueOf(MAX_STEPPER), BigDecimal.valueOf(STEP_SIZE),
            BigDecimal.valueOf(applicationRateMinStepper.getValue()), PRECISION, unitString);
   }

   private void calculateTotalAmountForProductMix() {
      float totalAmount = 0;

      UnitsToggleHolder carrierUnitHolder = (UnitsToggleHolder) carrierProductHolder.productUnit.getTag();
      if (carrierUnitHolder != null && carrierUnitHolder.currentChoice != null) {
         totalAmount += (carrierProductHolder.productAmount.getValue() / carrierUnitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
         ProductMixRecipe carrierRecipe = carrierProductHolder.currentRecipe;
         if (carrierRecipe != null && carrierRecipe.getProduct() != null) {
            Product carrierProduct = carrierRecipe.getProduct();
            if (carrierProduct != null) {
               for (ProductMixElementHolder productElement : productMixList) {
                  if (productElement.currentRecipe != null && productElement.currentRecipe.getProduct().getForm() == carrierProduct.getForm()) {
                     UnitsToggleHolder unitHolder = (UnitsToggleHolder) productElement.productUnit.getTag();
                     totalAmount += (productElement.productAmount.getValue() / unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
                  }
               }
            }
         }
      }
      this.totalAmount = totalAmount;
      calculateNewApplicationRate1PerProduct();
      calculateNewApplicationRate2PerProduct();
   }

   /**
    * If all required data will set, the "add" button will enable
    */
   private void validateRequiredData() {
      if (this.applicationRate1Stepper != null && this.applicationRate1Stepper.getValue() > 0 && isCarrierSet && isOneProductMixSet && isProductMixFormSet
            && !this.productMixTitleEditText.getText().toString().isEmpty() && this.carrierProductHolder != null && this.carrierProductHolder.productAmount.getValue() > 0) {
         this.setFirstButtonEnabled(true);
      }
      else {
         this.setFirstButtonEnabled(false);
      }
   }

   private static double calculateApplicationRate(double amount, double productMixRate, double totalAmount) {
      double result = amount / totalAmount;
      return productMixRate * result;
   }

   private static double getNewRate(double applicationRate, ProductMixElementHolder productElement, double totalAmount) {
      if (productElement != null) {
         UnitsToggleHolder amountUnitHolder = (UnitsToggleHolder) productElement.productUnit.getTag();
         if (amountUnitHolder != null && amountUnitHolder.currentChoice != null) {
            double amount = productElement.productAmount.getValue() / amountUnitHolder.currentChoice.getMultiplyFactorFromBaseUnits();
            return calculateApplicationRate(amount, applicationRate, totalAmount);
         }
      }
      return 0;
   }

   private void calculateNewApplicationRate1PerProduct() {
      UnitsToggleHolder productMixUnitHolder = (UnitsToggleHolder) applicationRatesUnitsPickList.getTag();
      if (productMixUnitHolder != null && productMixUnitHolder.currentChoice != null) {
         double applicationRate = applicationRate1Stepper.getValue() / productMixUnitHolder.currentChoice.getMultiplyFactorFromBaseUnits();
         if (carrierProductHolder.currentRecipe != null) {
            carrierProductHolder.currentRecipe.getProduct().setDefaultRate(getNewRate(applicationRate, carrierProductHolder, totalAmount));

            for (ProductMixElementHolder productElement : productMixList) {
               if (productElement != null && productElement.currentRecipe != null) {
                  double newRate = getNewRate(applicationRate, productElement, totalAmount);
                  productElement.currentRecipe.getProduct().setDefaultRate(newRate);
               }
            }
            updateApplicationRateTable();
         }
      }
      else {
         log.info("no current Choice");
      }
   }

   private void calculateNewApplicationRate2PerProduct() {
      UnitsToggleHolder productMixUnitHolder = (UnitsToggleHolder) applicationRatesUnitsPickList.getTag();
      if (productMixUnitHolder != null && productMixUnitHolder.currentChoice != null) {
         double applicationRate = applicationRate2Stepper.getValue() / productMixUnitHolder.currentChoice.getMultiplyFactorFromBaseUnits();
         if (carrierProductHolder.currentRecipe != null) {
            carrierProductHolder.currentRecipe.getProduct().setRate2(getNewRate(applicationRate, carrierProductHolder, totalAmount));
         }
         for (ProductMixElementHolder productElement : productMixList) {
            if (productElement.currentRecipe != null) {
               productElement.currentRecipe.getProduct().setRate2(getNewRate(applicationRate, productElement, totalAmount));
            }
         }
         updateApplicationRateTable();
      }
      else {
         log.info("no current Choice");
      }
   }

   /**
    * Helper Method to get the right Asset of the Producttype
    * @param productType the selected producttype
    * @return return the assset of the producttype if will found, otherwise use default the Seed Asset
    */
   private static int getProductTypeImageId(ProductForm productType) {
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

   /**
    * get Product from productlist by enter a Name
    * @param productName String product name
    * @return if product will found by name, return Product otherwise null
    */
   private Product getProductByName(String productName) {
      if (productList != null && !productList.isEmpty()) {
         for (Product product : this.productList) {
            if (product.getName().equalsIgnoreCase(productName)) {
               return product;
            }
         }
      }
      return null;
   }

   /**
    * Will Initialize the Save and Cancel button for the Dialog
    */
   private void initEventListener() {
      this.setOnButtonClickListener(new OnButtonClickListener() {
         @Override
         public void onButtonClick(DialogViewInterface dialogViewInterface, int which) {
            unregisterListener();
            switch (which) {
            case BUTTON_ADD:
               saveProductMixToPCM();
               ProductMixDialog.this.dismiss();
               break;

            case BUTTON_CANCEL:
               ProductMixDialog.this.cancel();
               log.warn("Cancel Product Mix Dialog, will not add or change");
               break;

            default:
               log.warn("Button not defined");
               break;
            }
         }
      });
   }

   private void unregisterListener() {
      try {
         vipService.unregister(TAG);
      }
      catch (RemoteException e) {
         log.error("failed");
      }
   }

   private class ProductMixElementHolder {
      public PickListEditable productPickList;
      public Button removeButton;
      public SegmentedToggleButtonGroupPickList productUnit;
      public StepperView productAmount;
      public ProductMixRecipe currentRecipe;
      public View elementView;

      public ProductMixElementHolder(View view) {
         if (view != null) {
            try {
               productPickList = (PickListEditable) view.findViewById(R.id.product_mix_product_left_picklist_product);
               removeButton = (Button) view.findViewById(R.id.product_mix_button_remove_product);
               productUnit = (SegmentedToggleButtonGroupPickList) view.findViewById(R.id.product_mix_segmentedtogglebuttongroup_product_unit);
               productAmount = (StepperView) view.findViewById(R.id.product_mix_stepperview_product_amount);
               elementView = view;
               productUnit.addEntry((long) 0, "m");
            }
            catch (Exception e) {
               log.error("error ", e);
            }
         }
      }
   }
}
