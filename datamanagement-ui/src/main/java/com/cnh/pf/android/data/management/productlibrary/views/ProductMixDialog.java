/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary.views;

import android.content.Context;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.persistence.PersistenceClient;
import com.cnh.android.pf.widget.controls.SegmentedToggleButtonGroupPickList;
import com.cnh.android.pf.widget.controls.SegmentedToggleButtonGroupPickList.SegmentedTogglePickListListener;
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.ProductMixHelper;
import com.cnh.android.pf.widget.utilities.ProductMixRecipeHelper;
import com.cnh.android.pf.widget.utilities.ProductNameValidator;
import com.cnh.android.pf.widget.utilities.UnitsToggleHolder;
import com.cnh.android.pf.widget.utilities.commands.ProductCommandParams;
import com.cnh.android.pf.widget.utilities.commands.ProductMixCommandParams;
import com.cnh.android.pf.widget.utilities.commands.SaveProductCommand;
import com.cnh.android.pf.widget.utilities.commands.SaveProductMixCommand;
import com.cnh.android.pf.widget.utilities.listeners.ClearFocusOnDoneOnEditorActionListener;
import com.cnh.android.pf.widget.utilities.listeners.ExtendedOnAdjustableBarChangedListener;
import com.cnh.android.pf.widget.utilities.listeners.GenericListener;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.android.pf.widget.view.DisabledOverlay.MODE;
import com.cnh.android.pf.widget.view.productdialogs.DialogActionType;
import com.cnh.android.pf.widget.view.productdialogs.DialogApplicationRateHandler;
import com.cnh.android.pf.widget.view.productdialogs.DialogApplicationRateHandlerListener;
import com.cnh.android.pf.widget.view.productdialogs.DialogDensityHandler;
import com.cnh.android.pf.widget.view.productdialogs.DialogHandlerListener;
import com.cnh.android.pf.widget.view.productdialogs.DialogPackageSizeHandler;
import com.cnh.android.pf.widget.view.productdialogs.DialogUsageAndCropTypeHandler;
import com.cnh.android.vip.aidl.IVIPListenerAIDL;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.vip.aidl.SimpleVIPListener;
import com.cnh.android.widget.Widget;
import com.cnh.android.widget.control.AbstractStepperView;
import com.cnh.android.widget.control.CategoryButtons;
import com.cnh.android.widget.control.CategoryButtons.CategoryButtonsEventListener;
import com.cnh.android.widget.control.InputField;
import com.cnh.android.widget.control.PickList;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListEditable;
import com.cnh.android.widget.control.PickListEditable.OnPickListItemActionListener;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.StepperView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.api.pvip.IPVIPServiceAIDL;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.MixType;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductDisplayItem;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductMix;
import com.cnh.pf.model.product.library.ProductMixRecipe;
import com.cnh.pf.model.product.library.ProductUnits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * DialogView to create ProductMixes
 *
 * @author helming
 * @since 29.10.2015
 */
public class ProductMixDialog extends DialogView implements DialogHandlerListener, DialogApplicationRateHandlerListener {

   private static final Logger log = LoggerFactory.getLogger(ProductMixDialog.class);
   private static final int BUTTON_ADD = BUTTON_FIRST;
   private static final int BUTTON_CANCEL = BUTTON_SECOND;
   private static final int MAX_STEPPER = 100000;
   private static final int MIN_STEPPER = 0;
   private static final int STEP_SIZE = 1;
   private static final int PRECISION = 2;

   private final String identifier = ProductMixDialog.class.getSimpleName() + System.identityHashCode(this);
   private IVIPServiceAIDL vipService;
   private IPVIPServiceAIDL pvipService;
   private ProductMix productMix;
   private List<ProductMix> productMixes;
   private DialogActionType actionType;
   private boolean isProductMixFormSet = false;
   private boolean isCarrierSet = false;
   private boolean isOneProductMixSet = false;
   private boolean isInitialized = false;
   private ProductMixCallBack callback;
   private List<Product> productList = null;
   private List<ProductUnits> productUnitsList = null;
   private LinearLayout mixProductsLayout;
   private CategoryButtons mixProductCategoryButton;
   private LinearLayout applicationProductTableLayoutOverView;
   private ProductForm productMixForm = ProductForm.GRANULAR;
   private ProductMixElementHolder carrierProductHolder;
   private double totalAmount = 0;
   private final ArrayList<ProductMixElementHolder> productMixElementHolderList = new ArrayList<ProductMixElementHolder>();
   private TableLayout productMixTable;
   private Button addMoreButton;
   private InputField productMixNameInputField;
   private PickList productMixFormPickList;
   private DisabledOverlay overlay;
   private boolean isAddNewProductOpen = false;
   private Map<Product, ApplicationRateTableFactory.ApplicationRateTableData> applicationRateTableDataMap = new HashMap<Product, ApplicationRateTableFactory.ApplicationRateTableData>();
   private ClearFocusOnDoneOnEditorActionListener clearFocusOnDoneOnEditorActionListener;
   private DialogApplicationRateHandler productDialogsApplicationRateHandler;
   private DialogDensityHandler dialogDensityHandler;
   private DialogPackageSizeHandler dialogPackageSizeHandler;
   private DialogUsageAndCropTypeHandler dialogUsageAndCropTypeHandler;
   private Widget.ErrorIndicator productMixNameErrorIndicator = Widget.ErrorIndicator.NONE;

   private CategoryButtonsEventListener eventListener = new CategoryButtonsEventListener() {
      @Override
      public void onCategoryButtonsBeforeExpand(CategoryButtons categoryButtons) {
         if (log.isTraceEnabled()) {
            log.trace("onCategoryButtonsBeforeExpand timemillis: {}", System.currentTimeMillis());
         }
         InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
         imm.hideSoftInputFromWindow(productMixNameInputField.getWindowToken(), 0);
      }

      @Override
      public void onCategoryButtonsAfterExpand(CategoryButtons categoryButtons) {
         if (log.isTraceEnabled()) {
            log.trace("onCategoryButtonsAfterExpand timemillis: {}", System.currentTimeMillis());
         }
      }

      @Override
      public void onCategoryButtonsBeforeCollapse(CategoryButtons categoryButtons) {
         if (log.isTraceEnabled()) {
            log.trace("onCategoryButtonsBeforeCollapse timemillis: {}", System.currentTimeMillis());
         }
      }

      @Override
      public void onCategoryButtonsAfterCollapse(CategoryButtons categoryButtons) {
         if (log.isTraceEnabled()) {
            log.trace("onCategoryButtonsAfterCollapse timemillis: {}", System.currentTimeMillis());
         }
      }
   };

   private IVIPListenerAIDL vipListener = new SimpleVIPListener() {
      @Override
      public void deliverProductList(final List<Product> products) throws RemoteException {
         new AsyncTask<Void, Void, List<Product>>() {

            @Override
            protected List<Product> doInBackground(Void... params) {
               if (products != null) {
                  List<Product> tempProductList = new ArrayList<Product>();
                  log.info("deliverProductList: {}", products.size());
                  for (Product product : products) {
                     if (product != null && product.getProductMixId() == 0) {
                        tempProductList.add(product);
                     }
                  }
                  return tempProductList;
               }
               return null;
            }

            @Override
            protected void onPostExecute(List<Product> products) {
               productList = products;
               if (productUnitsList != null && productList != null) {
                  log.info("loadProductMixToDialog by deliverProductList");
                  if (!isInitialized) {
                     initializeViews();
                     isInitialized = true;
                  }
                  else {
                     log.info("is Initialized");
                  }
                  if (actionType != DialogActionType.ADD) {
                     loadProductMixToDialog();
                  }
               }
            }
         }.execute();
      }

      @Override
      public void deliverProductUnitsList(final List<ProductUnits> productUnits) throws RemoteException {
         new AsyncTask<Void, Void, List<ProductUnits>>() {
            @Override
            protected List<ProductUnits> doInBackground(Void... voids) {
               return productUnits;
            }

            @Override
            protected void onPostExecute(List<ProductUnits> productUnits) {
               if (productUnits != null && !productUnits.isEmpty()) {
                  productUnitsList = productUnits;
                  if (productList != null) {
                     log.info("loadProductMixToDialog by deliverProductUnitsList");
                     if (!isInitialized) {
                        initializeViews();
                        isInitialized = true;
                     }
                     if (actionType != DialogActionType.ADD) {
                        loadProductMixToDialog();
                     }
                  }
               }
            }
         }.execute();
      }
   };

   public interface ProductMixCallBack {
      /**
       * after Creating ProductMix will call loadProductMix for the MainView
       * @param productMix new ProductMix
       */
      void loadProductMix(ProductMix productMix);
   }

   /**
    * Create ProductMixDialog for adding new Product Mix
    * @param context all resource information
    * @param vipService communication with database
    * @param callback callbackMethod
    */
   public ProductMixDialog(Context context, IVIPServiceAIDL vipService, ProductMixCallBack callback, List<ProductMix> productMixes) {
      this(context, DialogActionType.ADD, vipService, null, null, callback, productMixes);
   }

   public ProductMixDialog(Context context, IVIPServiceAIDL vipService, IPVIPServiceAIDL pvipService, ProductMixCallBack callback, List<ProductMix> productMixes) {
      this(context, DialogActionType.ADD, vipService, pvipService, null, callback, productMixes);
   }

   public ProductMixDialog(Context context, DialogActionType actionType, IVIPServiceAIDL vipService, IPVIPServiceAIDL pvipService, ProductMix productMix,
         ProductMixCallBack callBack, List<ProductMix> productMixes) {
      super(context);
      this.actionType = actionType;
      this.productMix = productMix;
      this.vipService = vipService;
      this.pvipService = pvipService;
      this.callback = callBack;
      this.productMixes = new ArrayList<ProductMix>(productMixes);
      if (vipService != null) {
         try {
            vipService.register(identifier, vipListener);
            vipService.requestProductList();
            vipService.requestProductUnitsList(null, null, null, null);
         }
         catch (RemoteException e) {
            log.error("can not register vipListener", e);
         }
         //update product name validator once
         ProductNameValidator.getInstance().update(this.vipService);
      }
      initEventListener();
   }

   @Override
   public DialogView setBodyView(int layoutResId) {
      long start = 0l;
      if (log.isTraceEnabled()) {
         start = System.currentTimeMillis();
      }
      super.setBodyView(layoutResId);
      initializeGUI();
      initializeProductFormPickList();
      if (actionType != DialogActionType.ADD) {
         isCarrierSet = true;
         isOneProductMixSet = true;
         isProductMixFormSet = true;
      }

      if (actionType == DialogActionType.COPY) {
         if (productMix != null && productMix.getProductMixParameters() != null) {
            productMixNameInputField.setText(String.format("%s -%s", productMix.getProductMixParameters().getName(), getContext().getString(R.string.copy)));
         }
      }

      if (productMix != null && (DialogActionType.COPY.equals(actionType) || DialogActionType.EDIT.equals(actionType))) {
         dialogUsageAndCropTypeHandler.setValuesToUi(productMix.getProductMixParameters());
      }

      if (log.isTraceEnabled()) {
         log.trace("setBodyView() duration {}", System.currentTimeMillis() - start);
      }
      return this;
   }

   /**
    * Initialize all Views and connect logic with the UI will be public because
    */
   private void initializeViews() {
      long initializeViewsStart = 0l;
      if (log.isTraceEnabled()) {
         initializeViewsStart = System.currentTimeMillis();
      }
      if (clearFocusOnDoneOnEditorActionListener == null) {
         clearFocusOnDoneOnEditorActionListener = new ClearFocusOnDoneOnEditorActionListener();
      }
      initializeMainView();
      initializeMixProductsView();
      initializeApplicationRatesView();
      initializeAdvancedView();
      isInitialized = true;
      overlay.setMode(MODE.HIDDEN);
      if (log.isTraceEnabled()) {
         log.trace("initializeViews duration {}", System.currentTimeMillis() - initializeViewsStart);
      }
   }

   private void initializeGUI() {
      overlay = (DisabledOverlay) this.findViewById(R.id.disabled_overlay);
      addMoreButton = (Button) this.findViewById(R.id.product_mix_dialog_mix_product_add_more);
      dialogUsageAndCropTypeHandler = new DialogUsageAndCropTypeHandler(this, this, pvipService);
      productDialogsApplicationRateHandler = new DialogApplicationRateHandler(this, this, actionType, this);
      dialogDensityHandler = new DialogDensityHandler(this, this);
      dialogPackageSizeHandler = new DialogPackageSizeHandler(this, this);
      productMixTable = (TableLayout) this.findViewById(R.id.product_mix_dialog_application_rates_table);
      productMixNameInputField = (InputField) this.findViewById(R.id.product_mix_name_input_field);
      productMixNameInputField.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_DONE);
      productMixFormPickList = (PickList) this.findViewById(R.id.product_mix_picklist_product_form);
      mixProductsLayout = (LinearLayout) this.findViewById(R.id.product_mix_product_list_view);
      mixProductCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_categorybuttons_mix_product_view);
      mixProductCategoryButton.setCategoryButtonsEventListener(eventListener);
      CategoryButtons applicationRatesCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_categorybuttons_application_rate_view);
      applicationRatesCategoryButton.setCategoryButtonsEventListener(eventListener);
      CategoryButtons advancedCategoryButton = (CategoryButtons) this.findViewById(R.id.product_mix_advanced_categorybuttons_view);
      advancedCategoryButton.setCategoryButtonsEventListener(eventListener);
      overlay.setMode(MODE.LOADING);
   }

   /**
    * Initialize the main view, load ProductFormPickList and create a new ProductMix if actiontype is add
    */
   private void initializeMainView() {

      if (this.actionType == DialogActionType.ADD) {
         this.productMix = new ProductMix();
      }
      if (productMixNameInputField != null) {
         productMixNameInputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
               updateSaveButtonState();
            }
         });
         productMixNameInputField.setOnEditorActionListener(clearFocusOnDoneOnEditorActionListener);
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
               carrierPickListAdapter.add(new PickListItem(form.getValue(), EnumValueToUiStringUtility.getUiStringForProductForm(form, getContext())));
            }
         }
         productMixFormPickList.setAdapter(carrierPickListAdapter);
         productMixFormPickList.sortAdapterData();
         productMixFormPickList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
               if (position < 0) {
                  position = ProductForm.GRANULAR.getValue();
               }
               productMixForm = ProductForm.values()[position];
               initializeApplicationRatesSegmentedToggleButtonGroup(productMixForm);
               if (productList == null) {
                  log.warn("ProductList is null");
               }
               else if (productList.isEmpty()) {
                  log.warn("ProductList is empty");
               }
               if (carrierProductHolder.getProductPickList() != null) {
                  PickListAdapter productListAdapter = new PickListAdapter(carrierProductHolder.getProductPickList(), getContext());
                  fillPickListAdapterWithProducts(productListAdapter, ProductHelperMethods.filterProductList(productList, productMixForm));
                  carrierProductHolder.getProductPickList().setAdapter(productListAdapter);
               }
               isProductMixFormSet = true;

               if (productMix != null) {
                  Product productMixParameters = productMix.getProductMixParameters();
                  if (productMixParameters != null) {
                     productMixParameters.setForm(productMixForm);
                     dialogUsageAndCropTypeHandler.setValuesToUi(productMixParameters);
                  }
                  else {
                     dialogUsageAndCropTypeHandler.setValuesToUi(productMixForm);
                  }
                  dialogPackageSizeHandler.setUnitsOptions(productMix.getProductMixParameters(), productMixForm, productUnitsList);
               }
               else {
                  dialogUsageAndCropTypeHandler.setValuesToUi(productMixForm);
               }
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
      long start = 0l;
      if (log.isTraceEnabled()) {
         start = System.currentTimeMillis();
      }
      log.debug("load product mix to dialog");
      Product productMixParameters = productMix.getProductMixParameters();
      //Add Product mix name and form to UI
      {
         productMixNameInputField.setText(productMixParameters.getName());
         productMixForm = productMixParameters.getForm();
         productMixFormPickList.setSelectionById(productMixForm.getValue());
      }
      //Add Carrier Data to UI
      {
         carrierProductHolder.setCurrentRecipe(new ProductMixRecipe(productMix.getProductCarrier()));
         if (actionType == DialogActionType.COPY) {
            carrierProductHolder.getCurrentRecipe().setId(PersistenceClient.NEW_OBJECT_ID);
         }
         List<Product> filteredProduct = ProductHelperMethods.filterProductList(productList, productMixForm);
         addDataToProductElement(carrierProductHolder.getCurrentRecipe(), carrierProductHolder, filteredProduct);
      }

      //Add Product Mix element Data to UI
      for (int i = 0; i < productMix.getRecipe().size(); i++) {
         ProductMixRecipe recipeElement = new ProductMixRecipe(productMix.getRecipe().get(i));
         if (actionType == DialogActionType.COPY) {
            recipeElement.setId(PersistenceClient.NEW_OBJECT_ID);
         }
         if (i > 0) {
            addProduct();
         }
         ProductMixElementHolder element = this.productMixElementHolderList.get(i);
         if (element != null) {
            addDataToProductElement(recipeElement, element, productList);
            element.setCurrentRecipe(recipeElement);
         }
      }

      //Add Application Rate Data to UI
      {
         productDialogsApplicationRateHandler.setUnitsOptions(productMixParameters, productMixForm, productUnitsList);
         ProductUnits rateUnit = productDialogsApplicationRateHandler.getSelectedRateUnits();
         if (rateUnit != null) {
            String rateUnitName = rateUnit.getName();
            if (rateUnitName != null) {
               productDialogsApplicationRateHandler.setStepperParameters(rateUnitName);
               productDialogsApplicationRateHandler.setValuesToUi(productMixParameters);
            }
            calculateTotalAmountForProductMix();
            calculateNewApplicationRatePerProduct(new Float(productDialogsApplicationRateHandler.getStepperRate1Value()),
                  new Float(productDialogsApplicationRateHandler.getStepperRate2Value()), rateUnit);
            updateApplicationRateTable();
         }
      }

      //Add Advanced Data to UI
      {
         initializeDensityUnits();
         dialogPackageSizeHandler.setValuesToUi(productMixParameters);
         dialogDensityHandler.setValuesToUi(productMixParameters);
         dialogUsageAndCropTypeHandler.setValuesToUi(productMixParameters);
      }
      if (actionType == DialogActionType.COPY) {
         if (productMix != null && productMix.getProductMixParameters() != null) {
            productMixNameInputField.setText(String.format("%s - %s", productMix.getProductMixParameters().getName(), getContext().getString(R.string.copy)));
         }
         updateSaveButtonState();
      }
      if (log.isTraceEnabled()) {
         log.trace("loadProductMixToDialog duration {}", System.currentTimeMillis() - start);
      }
   }

   private void addDataToProductElement(ProductMixRecipe recipeElement, ProductMixElementHolder element, List<Product> products) {
      if (recipeElement != null) {
         Product product = recipeElement.getProduct();
         if (product != null) {
            log.info("beforeSetUnitToSegmentedToggleButtonGroup");
            setUnitToSegmentedToggleButtonGroup(element.getProductUnit(), product.getForm(),
                  ProductHelperMethods.queryAmountMeasurementSystemForProductForm(product.getForm(), getContext()), ProductDisplayItem.AMOUNT);
            UnitsToggleHolder unitHolder = (UnitsToggleHolder) element.getProductUnit().getTag();
            if (unitHolder == null) {
               log.warn("unitHolder is null ");
               unitHolder = new UnitsToggleHolder();
            }
            unitHolder.currentChoice = ProductMixRecipeHelper.retrieveAmountUnitsFromProductMixRecipe(recipeElement, getContext());
            element.setCurrentRecipe(recipeElement);
            PickListAdapter productListAdapter = new PickListAdapter(element.getProductPickList(), getContext());
            fillPickListAdapterWithProducts(productListAdapter, products);
            element.getProductPickList().setAdapter(productListAdapter);
            if (products != null) {
               int productIndex = -1;
               for (Product tempProduct : products) {
                  if (product.getId() == tempProduct.getId()) {
                     productIndex = products.indexOf(tempProduct);
                     break;
                  }
               }
               element.getProductPickList().setSelectionByPosition(productIndex);
               if (unitHolder.currentChoice != null && unitHolder.unitChoices != null) {
                  element.getProductUnit().setEnabled(true);
                  int unitIndex = unitHolder.unitChoices.indexOf(unitHolder.currentChoice);
                  element.getProductUnit().setSelectionById((long) unitIndex);
                  element.getProductAmountStepper().setEnabled(true);
                  setUnitToStepper(element.getProductAmountStepper(), unitHolder.currentChoice.getName());

                  // the stepper is not able to handle NaN so it's need to be replaced with 0f.
                  double amountAsDouble = recipeElement.getAmount();
                  float amount = Double.isNaN(amountAsDouble) || Double.isInfinite(amountAsDouble) ? 0f : (float) amountAsDouble;
                  double multiplyFactorFromBaseUnitsAsDouble = unitHolder.currentChoice.getMultiplyFactorFromBaseUnits();
                  float multiplyFactorFromBaseUnits = Double.isNaN(multiplyFactorFromBaseUnitsAsDouble) || Double.isInfinite(multiplyFactorFromBaseUnitsAsDouble) ? 0f
                        : (float) multiplyFactorFromBaseUnitsAsDouble;
                  log.debug("addDataToProductElement - amount: {}, multiplyFactorFromBaseUnits: {}", amount, multiplyFactorFromBaseUnits);
                  element.getProductAmountStepper().setValue(amount * multiplyFactorFromBaseUnits);
               }
            }
         }
      }
   }

   private static void setUnitToStepper(StepperView stepper, String unit) {
      if (stepper != null) {
         stepper.setParameters(BigDecimal.valueOf(MIN_STEPPER), BigDecimal.valueOf(MAX_STEPPER), BigDecimal.valueOf(STEP_SIZE), BigDecimal.valueOf(stepper.getValue()), PRECISION,
               unit);
      }
   }

   /**
    * add new Product to the ProductViewList and fill the Product with needed Data
    */
   private void addProduct() {
      try {
         LinearLayout productElementLayout = (LinearLayout) inflate(getContext(), R.layout.product_mix_dialog_mix_products_element_view, null);
         productElementLayout.setPadding(0, 10, 0, 0);
         ProductMixElementHolder productElement = new ProductMixElementHolder(productElementLayout);
         initializeNewProduct(productElementLayout, productElement);
         if (mixProductsLayout != null) {
            mixProductsLayout.addView(productElementLayout);
            productMixElementHolderList.add(productElement);
            productElement.getProductUnit().setEnabled(false);
         }
      }
      catch (Exception e) {
         log.error("productElement could not created", e);
      }
   }

   /**
    * fill into Product pickerList all products.
    * @param productElementView the new product view which created in addProduct.
    */
   private void initializeNewProduct(final LinearLayout productElementView, final ProductMixElementHolder productElement) {
      final TextView productTitle = (TextView) productElementView.findViewById(R.id.product_mix_new_product_title);
      if (productTitle != null) {
         productTitle.setText(getResources().getString(R.string.product_mix_title_product, productMixElementHolderList.size() + 1));
      }

      if (productElement != null) {
         productElement.getRemoveButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
               removeProduct(productElement);
            }
         });

         if (productElement.getProductPickList() != null) {
            productElement.getProductPickList().setHeaderText(getResources().getString(R.string.product_mix_title_product, productMixElementHolderList.size() + 1));
            PickListAdapter productListAdapter = new PickListAdapter(productElement.getProductPickList(), getContext());
            fillPickListAdapterWithProducts(productListAdapter, productList);
            productElement.getProductPickList().setAdapter(productListAdapter);
            productElement.getProductPickList().setWidgetHandlesAddNewMode(false);
            createNewProductDialog(productElementView, productElement);
            productElement.getProductPickList().setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
                  productElement.getProductAmountStepper().setEnabled(true);
                  productElement.getProductUnit().setEnabled(true);
                  if (productElement.getProductPickList().getItemByPosition(position) != null) {
                     Product product = getProductByName(productElement.getProductPickList().getItemByPosition(position).getValue());
                     if (product != null) {
                        if (productElement.getCurrentRecipe() == null) {
                           productElement.setCurrentRecipe(new ProductMixRecipe());
                        }
                        productElement.getCurrentRecipe().setProduct(product);
                        productElement.getProductPickList().setTag(product);

                        setUnitToSegmentedToggleButtonGroup(productElement.getProductUnit(), product.getForm(),
                              ProductHelperMethods.queryAmountMeasurementSystemForProductForm(product.getForm(), getContext()), ProductDisplayItem.AMOUNT);
                        productElement.getProductUnit().setToggleListSelectionListener(new SegmentedTogglePickListListener() {
                           @Override
                           public void onToggleButtonCheckedChanged(RadioGroup radioGroup, int position) {
                              updateHolderAndStepper(position);
                           }

                           @Override
                           public void onListItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
                              updateHolderAndStepper(position);
                           }

                           private void updateHolderAndStepper(int position) {
                              UnitsToggleHolder unit = (UnitsToggleHolder) productElement.getProductUnit().getTag();
                              if (unit.unitChoices.size() > position) {
                                 unit.currentChoice = unit.unitChoices.get(position);
                              }
                              if (unit.currentChoice != null) {
                                 setUnitToStepper(productElement.getProductAmountStepper(), unit.currentChoice.getName());
                              }
                              calculateTotalAmountForProductMix();
                           }

                           @Override
                           public void onNoItemsSelected(AdapterView<?> adapterView) {

                           }
                        });
                        productElement.getProductAmountStepper().setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
                           @Override
                           public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double value, boolean fromUser) {
                              if (fromUser) {
                                 calculateTotalAmountForProductMix();
                              }
                           }
                        });
                     }
                  }
                  if (productMixElementHolderList.size() > 1) {
                     addRemoveButtonToFirstProduct();
                     productElement.getRemoveButton().setVisibility(VISIBLE);
                  }
                  calculateNewApplicationRatePerProduct(new Float(productDialogsApplicationRateHandler.getStepperRate1Value()),
                        new Float(productDialogsApplicationRateHandler.getStepperRate2Value()), productDialogsApplicationRateHandler.getSelectedRateUnits());
                  updateApplicationRateTable();
                  if (mixProductCategoryButton.isExpanded()) {
                     mixProductCategoryButton.resizeContent(false);
                  }
                  UnitsToggleHolder unit = (UnitsToggleHolder) productElement.getProductUnit().getTag();
                  if (unit != null && unit.currentChoice != null) {
                     setUnitToStepper(productElement.getProductAmountStepper(), unit.currentChoice.getName());
                  }
               }

               @Override
               public void onNothingSelected(AdapterView<?> adapterView) {
               }
            });
            // this is a guess - as long as there is no form for the product selected it is not possible to choose the correct measurement system
            setUnitToSegmentedToggleButtonGroup(productElement.getProductUnit(), productMixForm,
                  ProductHelperMethods.queryAmountMeasurementSystemForProductForm(productMixForm, getContext()), ProductDisplayItem.AMOUNT);
         }
      }
   }

   /**
    * Checks if the title for a new product is not empty and not null and if no other product is using this title.
    * @param title the new title to check
    * @return true if the title is valid false otherwise
    */
   private boolean isNewProductTitleValid(Editable title) {
      if (title == null || title.toString() == null) {
         return false;
      }
      String titleString = title.toString().trim();
      if (titleString.isEmpty()) {
         return false;
      }
      for (Product product : productList) {
         if (product.getName().equals(titleString)) {
            return false;
         }
      }
      return true;
   }

   private void createNewProductDialog(final LinearLayout productElementView, final ProductMixElementHolder productElement) {
      productElement.getProductPickList().setOnItemActionListener(new OnPickListItemActionListener() {
         private boolean isTitleValid = false;
         private boolean isFormSet = false;

         //instance initializer
         {
            productElement.getProductPickList().setAllowNewItemsCreation(!isAddNewProductOpen);
         }

         private void updateAddButtonState(Button saveButton) {
            if (saveButton != null) {
               saveButton.setEnabled(isTitleValid && isFormSet);
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
            enableAddNewProductButtons(false);
            final LinearLayout selectProductView = (LinearLayout) productElementView.findViewById(R.id.product_mix_product_left_select_product);
            selectProductView.setVisibility(GONE);
            final LinearLayout newProductView = (LinearLayout) productElementView.findViewById(R.id.product_mix_product_left_new_product);
            newProductView.setVisibility(VISIBLE);
            final EditText newProductEditText = (EditText) newProductView.findViewById(R.id.product_mix_product_edittext_add_new_product);
            newProductEditText.setText("");
            final PickList newProductFormPickList = (PickList) newProductView.findViewById(R.id.product_mix_product_picklist_product_type);
            final Button newProductButtonCancel = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_cancel);
            newProductButtonCancel.setOnClickListener(new OnClickListener() {
               @Override
               public void onClick(View view) {
                  selectProductView.setVisibility(VISIBLE);
                  newProductView.setVisibility(GONE);
                  enableAddNewProductButtons(true);
               }
            });
            final Button newProductButtonAdd = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_add);
            newProductButtonAdd.setEnabled(false);
            PickListAdapter productFormAdapter = new PickListAdapter(newProductFormPickList, getContext());
            for (ProductForm form : ProductForm.values()) {
               if (form != ProductForm.ANY) {
                  productFormAdapter.add(new PickListItem(form.getValue(), EnumValueToUiStringUtility.getUiStringForProductForm(form, getContext())));
               }
            }
            newProductFormPickList.setAdapter(productFormAdapter);
            newProductFormPickList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
               @Override
               public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l, boolean fromUser) {
                  isFormSet = true;
                  updateAddButtonState(newProductButtonAdd);
               }

               @Override
               public void onNothingSelected(AdapterView<?> adapterView) {

               }
            });
            newProductEditText.addTextChangedListener(new TextWatcher() {
               @Override
               public void beforeTextChanged(CharSequence s, int start, int count, int after) {
               }

               @Override
               public void onTextChanged(CharSequence s, int start, int before, int count) {
               }

               @Override
               public void afterTextChanged(Editable newTitle) {
                  isTitleValid = isNewProductTitleValid(newTitle);
                  updateAddButtonState(newProductButtonAdd);
               }
            });
            newProductButtonAdd.setOnClickListener(new OnClickListener() {
               @Override
               public void onClick(View view) {
                  log.debug("saving new product clicked");
                  Product newProduct = new Product();
                  newProduct.setName(newProductEditText.getText().toString());
                  newProduct.setForm(ProductForm.findByValue(newProductFormPickList.getSelectedItemPosition()));

                  if (productUnitsList != null && !productUnitsList.isEmpty()) {
                     MeasurementSystem measurementSystemForRates = ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(newProduct.getForm(), getContext());
                     ProductHelperMethods.bindProductRateUnits(newProduct,
                           ProductHelperMethods.filterUnitList(newProduct.getForm(), ProductDisplayItem.RATES, measurementSystemForRates, productUnitsList).get(0),
                           measurementSystemForRates);
                  }

                  ProductCommandParams params = new ProductCommandParams();
                  params.vipService = vipService;
                  params.product = newProduct;
                  new VIPAsyncTask<ProductCommandParams, Product>(params, new GenericListener<Product>() {
                     @Override
                     public void handleEvent(Product product) {
                        productList.add(product);
                        PickListAdapter adapter = productElement.getProductPickList().getAdapter();
                        adapter.add(new PickListItem(adapter.getCount() - 1, product.getName()));
                        productElement.getProductPickList().setAdapter(adapter);
                        productElement.getProductPickList().setSelectionByPosition(adapter.getCount() - 1);
                        selectProductView.setVisibility(VISIBLE);
                        newProductView.setVisibility(GONE);
                        enableAddNewProductButtons(true);
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
         holder.unitChoices = new ArrayList<ProductUnits>(ProductHelperMethods.filterUnitList(productForm, productDisplayItem, measurementSystem, this.productUnitsList));
         for (int i = 0; i < holder.unitChoices.size(); i++) {
            segmentedToggleButtonGroup.addEntry((long) i, holder.unitChoices.get(i).getName());
         }
         if (holder.unitChoices.size() > 0) {
            holder.currentChoice = holder.unitChoices.get(0);
         }
         segmentedToggleButtonGroup.setTag(holder);
         segmentedToggleButtonGroup.setSelectionById((long) 0);
      }
   }

   /**
    * Read all Data and save it to productMix
    */
   private void saveProductMixToPCM() {
      if (this.vipService != null) {
         Product productMixParameters;
         ProductMix tempProductMix;

         if (actionType == DialogActionType.EDIT) {
            tempProductMix = this.productMix;
            productMixParameters = tempProductMix.getProductMixParameters();
         }
         else if (actionType == DialogActionType.COPY) {
            tempProductMix = new ProductMix(this.productMix);
            tempProductMix.setId(PersistenceClient.NEW_OBJECT_ID);
            productMixParameters = new Product(tempProductMix.getProductMixParameters());
            productMixParameters.setId(PersistenceClient.NEW_OBJECT_ID);
         }
         else {
            tempProductMix = this.productMix;
            productMixParameters = new Product();
         }
         productMixParameters.setForm(this.productMixForm);
         dialogUsageAndCropTypeHandler.setValuesToProduct(productMixParameters);
         dialogPackageSizeHandler.setValuesToProduct(productMixParameters);
         dialogDensityHandler.setValuesToProduct(productMixParameters);
         productDialogsApplicationRateHandler.setValuesToProduct(productMixParameters);

         ProductUnits carrierUnit = null;
         if (carrierProductHolder.getProductPickList().getTag() != null) {
            UnitsToggleHolder holder = (UnitsToggleHolder) carrierProductHolder.getProductUnit().getTag();
            carrierUnit = holder.currentChoice;
         }

         ProductMixHelper.bindMixTotalUnits(tempProductMix, carrierUnit, ProductHelperMethods.queryAmountMeasurementSystemForProductForm(productMixForm, getContext()));
         ProductMixRecipeHelper.setAmountAndUnitsToProductMixRecipe(carrierProductHolder.getCurrentRecipe(), ((UnitsToggleHolder) carrierProductHolder.getProductUnit().getTag()).currentChoice,
               carrierProductHolder.getProductAmountStepper().getValue(), getContext());
         tempProductMix.setMixType(MixType.FORMULA);
         tempProductMix.setProductCarrier(carrierProductHolder.getCurrentRecipe());
         tempProductMix.setRecipe(createProductRecipeList());
         // this is a bit nasty here - because the productMix object to save is created in this method, there is no place to save the trimmed string in the TextWatcher...
         // if the product object would be created earlier we could save the trimmed string there.
         productMixParameters.setName(productMixNameInputField.getText().toString().trim());
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
    * create a Product recipe arrayList with all products from the productviewlist
    * @return arrayList with all products for the product mix
    */
   private List<ProductMixRecipe> createProductRecipeList() {
      List<ProductMixRecipe> productMixRecipeList = new ArrayList<ProductMixRecipe>();
      for (ProductMixElementHolder productElementHolder : productMixElementHolderList) {
         if (productElementHolder.getCurrentRecipe() != null) {
            ProductMixRecipe recipe = productElementHolder.getCurrentRecipe();
            ProductMixRecipeHelper.setAmountAndUnitsToProductMixRecipe(recipe, ((UnitsToggleHolder) productElementHolder.getProductUnit().getTag()).currentChoice,
                  productElementHolder.getProductAmountStepper().getValue(), getContext());
            productMixRecipeList.add(recipe);
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
      carrierProductHolder.getProductPickList().setHeaderText(R.string.product_mix_title_carrier);
      if (carrierProductHolder.getProductPickList() != null) {
         carrierProductHolder.getProductPickList().setWidgetHandlesAddNewMode(false);
         carrierProductHolder.getProductPickList().setOnItemActionListener(new OnProductPickListItemActionListener(productElement));
         carrierProductHolder.getProductPickList().setOnItemSelectedListener(new OnCarrierProductPickListItemSelectedListener());
      }
      if (actionType == DialogActionType.ADD) {
         setUnitToSegmentedToggleButtonGroup(carrierProductHolder.getProductUnit(), productMixForm,
               ProductHelperMethods.queryAmountMeasurementSystemForProductForm(productMixForm, getContext()), ProductDisplayItem.AMOUNT);
         carrierProductHolder.getProductUnit().setEnabled(false);
      }
      mixProductsLayout.addView(productElement);
   }

   private class OnCarrierProductPickListItemSelectedListener implements PickListEditable.OnItemSelectedListener {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
         carrierProductHolder.getProductAmountStepper().setOnAdjustableBarChangedListener(new ExtendedOnAdjustableBarChangedListener() {
            @Override
            public void onAdjustableBarChanged(AbstractStepperView abstractStepperView, double v, boolean fromUser) {
               if (fromUser) {
                  updateSaveButtonState();
                  calculateTotalAmountForProductMix();
               }
            }
         });
         carrierProductHolder.getProductAmountStepper().setEnabled(true);
         carrierProductHolder.getProductUnit().setEnabled(true);
         if (position >= 0) {
            Product product = getProductByName(carrierProductHolder.getProductPickList().getItemByPosition(position).getValue());
            carrierProductHolder.getProductPickList().setTag(product);
            if (carrierProductHolder.getCurrentRecipe() == null) {
               carrierProductHolder.setCurrentRecipe(new ProductMixRecipe());
            }
            carrierProductHolder.getCurrentRecipe().setProduct(product);
            if (product != null) {
               setUnitToSegmentedToggleButtonGroup(carrierProductHolder.getProductUnit(), product.getForm(),
                     ProductHelperMethods.queryAmountMeasurementSystemForProductForm(productMixForm, getContext()), ProductDisplayItem.AMOUNT);
            }
            carrierProductHolder.getProductUnit().setToggleListSelectionListener(new SegmentedTogglePickListListener() {
               @Override
               public void onToggleButtonCheckedChanged(RadioGroup radioGroup, int position) {
                  updateHolder(position);
               }

               @Override
               public void onListItemSelected(AdapterView<?> adapterView, View view, int position, long id, boolean fromUser) {
                  updateHolder(position);
               }

               private void updateHolder(int position) {
                  UnitsToggleHolder unit = (UnitsToggleHolder) carrierProductHolder.getProductUnit().getTag();
                  if (unit != null) {
                     if (unit.currentChoice != null) {
                        if (unit.unitChoices.size() > position) {
                           unit.currentChoice = unit.unitChoices.get(position);
                        }
                        setUnitToStepper(carrierProductHolder.getProductAmountStepper(), unit.currentChoice.getName());
                        carrierProductHolder.getProductUnit().setTag(unit);
                        calculateTotalAmountForProductMix();
                     }
                  }
               }

               @Override
               public void onNoItemsSelected(AdapterView<?> adapterView) {

               }
            });

            isCarrierSet = true;
            updateSaveButtonState();
            UnitsToggleHolder unit = (UnitsToggleHolder) carrierProductHolder.getProductUnit().getTag();
            if (unit != null && unit.currentChoice != null) {
               setUnitToStepper(carrierProductHolder.getProductAmountStepper(), unit.currentChoice.getName());
            }
            calculateNewApplicationRatePerProduct(new Float(productDialogsApplicationRateHandler.getStepperRate1Value()),
                  new Float(productDialogsApplicationRateHandler.getStepperRate2Value()), productDialogsApplicationRateHandler.getSelectedRateUnits());
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
      private boolean isTitleValid = false;
      private boolean isFormSet = false;

      private OnProductPickListItemActionListener(LinearLayout productElement) {
         this.productElement = productElement;
      }

      private void updateAddButtonState(Button saveButton) {
         if (saveButton != null) {
            saveButton.setEnabled(isTitleValid && isFormSet);
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
         enableAddNewProductButtons(false);
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
         final Button newProductButtonCancel = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_cancel);
         newProductButtonCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
               selectProductView.setVisibility(VISIBLE);
               newProductView.setVisibility(GONE);
               productMixFormPickList.setEnabled(true);
               enableAddNewProductButtons(true);
            }
         });
         final Button newProductButtonAdd = (Button) newProductView.findViewById(R.id.product_mix_product_left_button_add);
         newProductButtonAdd.setEnabled(false);
         PickListAdapter productFormAdapter = new PickListAdapter(newProductFormPickList, getContext());

         for (ProductForm form : ProductForm.values()) {
            if (form != ProductForm.ANY) {
               productFormAdapter.add(new PickListItem(form.getValue(), EnumValueToUiStringUtility.getUiStringForProductForm(form, getContext())));
            }
         }
         newProductFormPickList.setOnItemSelectedListener(new PickListEditable.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l, boolean fromUser) {
               isFormSet = true;
               updateAddButtonState(newProductButtonAdd);
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
         newProductEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable newTitle) {
               isTitleValid = isNewProductTitleValid(newTitle);
               updateAddButtonState(newProductButtonAdd);
            }
         });
         newProductButtonAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
               Product newProduct = new Product();
               newProduct.setName(newProductEditText.getText().toString());
               newProduct.setForm(ProductForm.findByValue(newProductFormPickList.getSelectedItemPosition()));
               MeasurementSystem measurementSystemForRates = ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(newProduct.getForm(), getContext());
               ProductHelperMethods.bindProductRateUnits(newProduct,
                     ProductHelperMethods.filterUnitList(newProduct.getForm(), ProductDisplayItem.RATES, measurementSystemForRates, productUnitsList).get(0),
                     measurementSystemForRates);
               ProductCommandParams params = new ProductCommandParams();
               params.vipService = vipService;
               params.product = newProduct;
               new VIPAsyncTask<ProductCommandParams, Product>(params, new GenericListener<Product>() {
                  @Override
                  public void handleEvent(Product product) {
                     // as side effect this sets the adapter of carrierProductHolder.productPickList ...
                     productMixFormPickList.setSelectionByPosition(newProductFormPickList.getSelectedItemPosition());
                     productMixFormPickList.setEnabled(true);
                     productList.add(product);
                     PickListAdapter adapter = carrierProductHolder.getProductPickList().getAdapter();
                     adapter.add(new PickListItem(adapter.getCount() - 1, product.getName()));
                     carrierProductHolder.getProductPickList().setSelectionByPosition(adapter.getCount() - 1);
                     selectProductView.setVisibility(VISIBLE);
                     newProductView.setVisibility(GONE);
                     enableAddNewProductButtons(true);
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
   }

   /**
    * will initialize the Mix Products segment.
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
      initializeApplicationRatesSegmentedToggleButtonGroup(productMixForm);
   }

   /**
    * initialize the Application Units in relation to the product form, fill the SegmentedToggleButtonGroup with Units to select
    * @param productForm the selected product from
    */
   private void initializeApplicationRatesSegmentedToggleButtonGroup(ProductForm productForm) {
      productDialogsApplicationRateHandler.setUnitsOptions(productMix.getProductMixParameters(), productForm, productUnitsList);
      productDialogsApplicationRateHandler.setStepperParameters(productDialogsApplicationRateHandler.getSelectedRateUnits().getName());
   }

   /**
    * Initialize the Advanced View
    */
   private void initializeAdvancedView() {
      dialogPackageSizeHandler.setUnitsOptions(productMix.getProductMixParameters(), productMixForm, productUnitsList);
      initializeDensityUnits();
   }

   /**
    * If new Carrier will select, reset the Carrier view, disable the stepper and the SegmentedToggleButtonGroupPickList.
    */
   private void resetCarrierProduct() {
      if (carrierProductHolder != null) {
         carrierProductHolder.getProductUnit().setEnabled(false);
         carrierProductHolder.getProductAmountStepper().setEnabled(false);
         carrierProductHolder.getProductAmountStepper().setValue(0);
         isCarrierSet = false;
         updateSaveButtonState();
      }
   }

   /**
    * InitializeDensityUnits.
    * If ProductForm is not Seed/Plant set Density units to toggle button and setup the unitText
    * if Product is Seed/Plant will remove the Density
    */
   private void initializeDensityUnits() {
      if (!(productMixForm == ProductForm.SEED || productMixForm == ProductForm.PLANT)) {
         dialogDensityHandler.setUnitDensityVisibility(View.GONE);
      }
      else {
         dialogDensityHandler.setUnitDensityVisibility(View.VISIBLE);
         dialogDensityHandler.setUnitDensityUnitsOptions(productMix.getProductMixParameters(), productMixForm, productUnitsList);
      }
      dialogDensityHandler.setProductDensityUnitsOptions(productMix.getProductMixParameters(), productMixForm, productUnitsList);
   }

   /**
    * make the Remove Button visible for the First Product if more than 1 Products set
    */
   private void addRemoveButtonToFirstProduct() {
      if (!productMixElementHolderList.isEmpty()) {
         ProductMixElementHolder firstProduct = productMixElementHolderList.get(0);
         if (firstProduct != null) {
            firstProduct.getRemoveButton().setVisibility(VISIBLE);
         }
      }
   }

   /**
    * make the Remove Button invisible if less than 2 Products are set.
    */
   private void removeRemoveButtonFromFirstProduct() {
      if (!productMixElementHolderList.isEmpty()) {
         ProductMixElementHolder firstProduct = productMixElementHolderList.get(0);
         if (firstProduct != null) {
            firstProduct.getRemoveButton().setVisibility(GONE);
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
         if (!productMixElementHolderList.isEmpty()) {
            if (!productMixElementHolderList.remove(productElement)) {
               log.warn("Could not Remove " + productElement);
            }
            ProductMixRecipe recipe = productElement.getCurrentRecipe();
            mixProductsLayout.removeView(productElement.getElementView());
            updateProductViewList();
            if (recipe != null) {
               applicationRateTableDataMap.remove(recipe.getProduct());
            }
            updateApplicationRateTable();
            this.mixProductCategoryButton.resizeContent(false);
            if (productMixElementHolderList.size() < 2) {
               removeRemoveButtonFromFirstProduct();
            }
         }
      }
   }

   /**
    * if an product would be removed the Product indexes will updated
    */
   private void updateProductViewList() {
      if (!productMixElementHolderList.isEmpty()) {
         isOneProductMixSet = false;
         for (int i = 0; i < productMixElementHolderList.size(); i++) {
            ProductMixElementHolder productElement = productMixElementHolderList.get(i);
            productElement.getProductPickList().setHeaderText(getResources().getString(R.string.product_mix_title_product, i + 1));
            if (!isOneProductMixSet && productElement.getProductPickList().getSelectedItemPosition() >= 0) {
               isOneProductMixSet = true;
            }
         }
         updateSaveButtonState();
      }
   }

   /**
    * Helper Method to fill Pickerlist with the Products
    * @param picklistAdapter Adapter for the PickList
    * @param productList list with Product which will add to the PickListAdapter
    */
   private void fillPickListAdapterWithProducts(PickListAdapter picklistAdapter, List<Product> productList) {
      if (picklistAdapter != null && productList != null && !productList.isEmpty()) {
         for (int i = 0; i < productList.size(); i++) {
            picklistAdapter.add(new PickListItem(i, productList.get(i).getName()));
         }
      }
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
         if (carrierProductHolder != null && carrierProductHolder.getCurrentRecipe() != null) {
            TableRow tableRow = ApplicationRateTableFactory.createTableRowForProductMixDialog(applicationRateTableDataMap.get(carrierProductHolder.getCurrentRecipe().getProduct()),
                  getContext(), productMixTable);
            if (tableRow != null) {
               productMixTable.addView(tableRow, viewCounter++);
            }
         }

         if (!productMixElementHolderList.isEmpty()) {
            for (ProductMixElementHolder productElement : productMixElementHolderList) {
               if (productElement.getCurrentRecipe() != null) {
                  if (productElement.getCurrentRecipe().getProduct() != null) {
                     Product product = productElement.getCurrentRecipe().getProduct();
                     TableRow tableRow = ApplicationRateTableFactory.createTableRowForProductMixDialog(applicationRateTableDataMap.get(product), getContext(), productMixTable);
                     if (tableRow != null) {
                        productMixTable.addView(tableRow, viewCounter++);
                     }
                  }
               }
            }
            if (productMixTable.getChildCount() > 2) {
               isOneProductMixSet = true;
               updateSaveButtonState();
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

   private void calculateTotalAmountForProductMix() {
      float totalAmount = 0;

      UnitsToggleHolder carrierUnitHolder = (UnitsToggleHolder) carrierProductHolder.getProductUnit().getTag();
      if (carrierUnitHolder != null && carrierUnitHolder.currentChoice != null) {
         totalAmount += (carrierProductHolder.getProductAmountStepper().getValue() / carrierUnitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
         ProductMixRecipe carrierRecipe = carrierProductHolder.getCurrentRecipe();
         if (carrierRecipe != null && carrierRecipe.getProduct() != null) {
            Product carrierProduct = carrierRecipe.getProduct();
            if (carrierProduct != null) {
               for (ProductMixElementHolder productElement : productMixElementHolderList) {
                  if (productElement.getCurrentRecipe() != null && productElement.getCurrentRecipe().getProduct().getForm() == carrierProduct.getForm()) {
                     UnitsToggleHolder unitHolder = (UnitsToggleHolder) productElement.getProductUnit().getTag();
                     totalAmount += (productElement.getProductAmountStepper().getValue() / unitHolder.currentChoice.getMultiplyFactorFromBaseUnits());
                  }
               }
            }
         }
      }
      this.totalAmount = totalAmount;
      calculateNewApplicationRatePerProduct(new Float(productDialogsApplicationRateHandler.getStepperRate1Value()),
            new Float(productDialogsApplicationRateHandler.getStepperRate2Value()), productDialogsApplicationRateHandler.getSelectedRateUnits());
      updateApplicationRateTable();
   }

   /**
    * This method prevents scrolling issues!
    * Whenever setErrorIndicator is called and the view it is called for is in focus,
    * it will be scrolled to the position of this view. Thus this method prevents
    * assigning the errorIndicator having the same one already assigned.
    * @param errorIndicator New error indicator state
    */
   private void setProductMixNameErrorIndicator(Widget.ErrorIndicator errorIndicator) {
      if (productMixNameErrorIndicator != errorIndicator) {
         productMixNameErrorIndicator = errorIndicator;
         productMixNameInputField.setErrorIndicator(errorIndicator);
      }
   }

   /**
    * If all required data will set, the "add" button will enable
    */
   @Override
   public void updateSaveButtonState() {
      log.debug("update updateSaveButtonState called");
      String productMixName = productMixNameInputField.getText().toString().trim();
      if (productMixName.isEmpty()) {
         //disallow empty names (no indicator)
         this.setFirstButtonEnabled(false);
         setProductMixNameErrorIndicator(Widget.ErrorIndicator.INVALID);
         return;
      }
      else {
         Integer productId = null;
         if (this.productMix != null && this.productMix.getProductMixParameters() != null) {
            productId = this.productMix.getProductMixParameters().getId();
         }
         if (!ProductNameValidator.getInstance().productNameIsUsable(productMixName, productId)) {
            //disallow non unique names
            this.setFirstButtonEnabled(false);
            setProductMixNameErrorIndicator(Widget.ErrorIndicator.NEEDS_CHECKING);
            return;
         }
         else {
            setProductMixNameErrorIndicator(Widget.ErrorIndicator.NONE);
         }
      }
      if (!dialogUsageAndCropTypeHandler.isValidItemSelected()) {
         this.setFirstButtonEnabled(false);
         return;
      }
      if (this.productDialogsApplicationRateHandler.getStepperRate1Value() > 0 && isCarrierSet && isOneProductMixSet && isProductMixFormSet && this.carrierProductHolder != null
            && this.carrierProductHolder.getProductAmountStepper().getValue() > 0) {
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
         UnitsToggleHolder amountUnitHolder = (UnitsToggleHolder) productElement.getProductUnit().getTag();
         if (amountUnitHolder != null && amountUnitHolder.currentChoice != null) {
            double amount = productElement.getProductAmountStepper().getValue() / amountUnitHolder.currentChoice.getMultiplyFactorFromBaseUnits();
            return calculateApplicationRate(amount, applicationRate, totalAmount);
         }
      }
      return 0;
   }

   @Override
   public void applicationRate1Changed(float rate1value, ProductUnits currentProductUnit) {
      calculateNewApplicationRatePerProduct(rate1value, null, currentProductUnit);
   }

   @Override
   public void applicationRate2Changed(float rate2value, ProductUnits currentProductUnit) {
      calculateNewApplicationRatePerProduct(null, rate2value, currentProductUnit);
   }

   @Override
   public void applicationRatesChanged(float rate1value, float rate2value, ProductUnits currentProductUnit) {
      calculateNewApplicationRatePerProduct(rate1value, rate2value, currentProductUnit);
   }

   @Override
   public void applicationDeltaRateChanged(final float deltaRatevalue, final ProductUnits productUnit) {
      //TODO: Need to store the delta rate.
   }

   /**
    * Calculates the application rates per product and updated the application rate table. The method is not made for setting booth parameters to false.
    * @param updateRate1 set true if rate1 values need to be updated
    * @param updateRate2 set true if rate2 values need to be updated
    */
   private void calculateNewApplicationRatePerProduct(Float updateRate1, Float updateRate2, ProductUnits currentProductUnit) {
      if (currentProductUnit != null) {
         if (carrierProductHolder.getCurrentRecipe() != null) {
            double applicationRate1 = Double.NaN;
            double applicationRate2 = Double.NaN;
            Product carrierProduct = carrierProductHolder.getCurrentRecipe().getProduct();
            ApplicationRateTableFactory.ApplicationRateTableData carrierApplicationRateTableData = getCreateAndPutApplicationRateTableData(carrierProduct);
            if (updateRate1 != null) {
               applicationRate1 = updateRate1 / currentProductUnit.getMultiplyFactorFromBaseUnits();
               carrierApplicationRateTableData.setDefaultRate(getNewRate(applicationRate1, carrierProductHolder, totalAmount));
            }
            if (updateRate2 != null) {
               applicationRate2 = updateRate2 / currentProductUnit.getMultiplyFactorFromBaseUnits();
               carrierApplicationRateTableData.setRate2(getNewRate(applicationRate2, carrierProductHolder, totalAmount));
            }
            for (ProductMixElementHolder productElement : productMixElementHolderList) {
               if (productElement != null && productElement.getCurrentRecipe() != null) {
                  Product product = productElement.getCurrentRecipe().getProduct();
                  ApplicationRateTableFactory.ApplicationRateTableData productApplicationRateTableData = getCreateAndPutApplicationRateTableData(product);
                  if (updateRate1 != null) {
                     productApplicationRateTableData.setDefaultRate(getNewRate(applicationRate1, productElement, totalAmount));
                  }
                  if (updateRate2 != null) {
                     productApplicationRateTableData.setRate2(getNewRate(applicationRate2, productElement, totalAmount));
                  }
               }
            }
         }
         updateApplicationRateTable();
      }
      else {
         log.info("no current Choice");
      }
   }

   /**
    * Gets the ApplicationRateTableData for the product or creates and adds it. Updates the name in ApplicationRateTableData.
    * @param product
    * @return the new or found ApplicationRateTableData
    */
   @Nonnull
   private ApplicationRateTableFactory.ApplicationRateTableData getCreateAndPutApplicationRateTableData(@Nonnull Product product) {
      ApplicationRateTableFactory.ApplicationRateTableData applicationRateTableData;
      if (applicationRateTableDataMap.containsKey(product)) {
         applicationRateTableData = applicationRateTableDataMap.get(product);
      }
      else {
         applicationRateTableData = new ApplicationRateTableFactory.ApplicationRateTableData();
         applicationRateTableDataMap.put(product, applicationRateTableData);
      }
      applicationRateTableData.setProductName(product.getName());
      applicationRateTableData.setProductForm(product.getForm());
      // defined in System Units Spec 5.2. that mass or volume should be used for application rates
      ProductUnits pu = ProductHelperMethods
              .retrieveProductRateUnits(product, ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(product.getForm(), getContext()));
      if (null != pu) {
         applicationRateTableData.setUnit(pu.deepCopy());
      }
      return applicationRateTableData;
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
         vipService.unregister(identifier);
      }
      catch (RemoteException e) {
         log.error("failed");
      }
   }

   private void enableAddNewProductButtons(boolean enable) {
      isAddNewProductOpen = !enable;
      for (ProductMixElementHolder productMixElementHolder : productMixElementHolderList) {
         productMixElementHolder.getProductPickList().setAllowNewItemsCreation(enable);
      }
      if (carrierProductHolder != null) {
         carrierProductHolder.getProductPickList().setAllowNewItemsCreation(enable);
      }
   }

   private class ProductMixElementHolder {
      private PickListEditable productPickList;
      private Button removeButton;
      private SegmentedToggleButtonGroupPickList productUnit;
      private StepperView productAmountStepper;
      private ProductMixRecipe currentRecipe;
      private View elementView;

      public ProductMixElementHolder(View view) {
         if (view != null) {
            try {
               setProductPickList((PickListEditable) view.findViewById(R.id.product_mix_product_left_picklist_product));
               setRemoveButton((Button) view.findViewById(R.id.product_mix_button_remove_product));
               setProductUnit((SegmentedToggleButtonGroupPickList) view.findViewById(R.id.product_mix_segmentedtogglebuttongroup_product_unit));
               setProductAmountStepper((StepperView) view.findViewById(R.id.product_mix_stepperview_product_amount));
               setElementView(view);
               getProductUnit().addEntry((long) 0, "m");
            }
            catch (Exception e) {
               log.error("error ", e);
            }
         }
      }

      public PickListEditable getProductPickList() {
         return productPickList;
      }

      public void setProductPickList(PickListEditable productPickList) {
         this.productPickList = productPickList;
      }

      public Button getRemoveButton() {
         return removeButton;
      }

      public void setRemoveButton(Button removeButton) {
         this.removeButton = removeButton;
      }

      public SegmentedToggleButtonGroupPickList getProductUnit() {
         return productUnit;
      }

      public void setProductUnit(SegmentedToggleButtonGroupPickList productUnit) {
         this.productUnit = productUnit;
      }

      public StepperView getProductAmountStepper() {
         return productAmountStepper;
      }

      public void setProductAmountStepper(StepperView productAmountStepper) {
         this.productAmountStepper = productAmountStepper;
      }

      public ProductMixRecipe getCurrentRecipe() {
         return currentRecipe;
      }

      public void setCurrentRecipe(ProductMixRecipe currentRecipe) {
         this.currentRecipe = currentRecipe;
      }

      public View getElementView() {
         return elementView;
      }

      public void setElementView(View elementView) {
         this.elementView = elementView;
      }
   }
}
