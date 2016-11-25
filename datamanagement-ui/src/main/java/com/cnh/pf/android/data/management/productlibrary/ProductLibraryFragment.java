/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.productlibrary;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.cnh.android.dialog.DialogView;
import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.DialogViewInterface.OnButtonClickListener;
import com.cnh.android.dialog.TextDialogView;
import com.cnh.android.pf.widget.controls.SearchInput;
import com.cnh.android.pf.widget.controls.SegmentedToggleButtonGroupPickList;
import com.cnh.android.pf.widget.utilities.MathUtility;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.UiUtility;
import com.cnh.android.pf.widget.utilities.UnitsQueryHolder;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.android.pf.widget.utilities.commands.DeleteProductCommand;
import com.cnh.android.pf.widget.utilities.commands.DeleteProductMixCommand;
import com.cnh.android.pf.widget.utilities.commands.GetVarietyListCommand;
import com.cnh.android.pf.widget.utilities.commands.LoadProductMixListCommand;
import com.cnh.android.pf.widget.utilities.commands.ProductCommandParams;
import com.cnh.android.pf.widget.utilities.commands.ProductMixCommandParams;
import com.cnh.android.pf.widget.utilities.listeners.GenericListener;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.android.pf.widget.view.ProductDialog;
import com.cnh.android.vip.aidl.IVIPListenerAIDL;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.vip.aidl.SimpleVIPListener;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.PickListAdapter;
import com.cnh.android.widget.control.PickListItem;
import com.cnh.android.widget.control.ProgressiveDisclosureView;
import com.cnh.pf.android.data.management.DataManagementActivity;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.utility.ProductLibraryAdapter;
import com.cnh.pf.android.data.management.productlibrary.utility.filters.ProductFilter;
import com.cnh.pf.android.data.management.productlibrary.utility.filters.ProductMixFilter;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.AbstractProductComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.AbstractProductMixComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.DefaultRateSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.FormSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.NameSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.ProductMixDefaultRateSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.ProductMixFormSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.ProductMixNameSortComparator;
import com.cnh.pf.android.data.management.productlibrary.views.ListHeaderSortView;
import com.cnh.pf.android.data.management.productlibrary.views.NestedExpandableListView;
import com.cnh.pf.android.data.management.productlibrary.views.ProductMixDialog;
import com.cnh.pf.android.data.management.productlibrary.views.ProductMixDialog.ProductMixesDialogActionType;
import com.cnh.pf.android.data.management.productlibrary.views.ProductMixDialog.productMixCallBack;
import com.cnh.pf.model.TableChangeEvent;
import com.cnh.pf.model.product.configuration.ControllerProductConfiguration;
import com.cnh.pf.model.product.configuration.DriveProductConfiguration;
import com.cnh.pf.model.product.configuration.ImplementProductConfig;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.library.CNHPlanterFanData;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductDisplayItem;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductMix;
import com.cnh.pf.model.product.library.ProductMixRecipe;
import com.cnh.pf.model.product.library.ProductUnits;
import com.cnh.pf.model.product.library.ProductUsage;
import com.cnh.pf.model.vip.vehimp.Implement;
import com.cnh.pf.model.vip.vehimp.ImplementCurrent;
import com.cnh.pf.units.unit_constantsConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import roboguice.fragment.provided.RoboFragment;

/**
 * ProductLibraryFragment
 * Supplies tab interface for products (and eventually product mixes)
 * Provides add/edit product functionality
 * Created by joorjitham on 3/27/2015.
 */

public class ProductLibraryFragment extends RoboFragment {
   private static final String TAG = ProductLibraryFragment.class.getSimpleName();
   private static final boolean DEBUG = false;
   private static final Logger log = LoggerFactory.getLogger(ProductLibraryFragment.class);
   private final String identifier = ProductLibraryFragment.class.getSimpleName() + System.identityHashCode(this);
   private static final String CROP_TYPE_GENERIC = "GENERIC";
   private static final String CORN_PRODUCT_CROP_TYPE_NAME = "CORN";
   private static final String WHEAT_PRODUCT_CROP_TYPE_NAME = "WHEAT";
   private static final String SOYBEANS_PRODUCT_CROP_TYPE_NAME = "SOYBEANS";
   private static final String PRODUCT_LIST = "product list";
   private static final String PRODUCT_UNITS_LIST = "product units list";
   private static final String CURRENT_PRODCUCT = "current product";
   private static final int LEFT_RIGHT_MARGIN = 42;
   private static final int TOP_BOTTOM_MARGIN = 1;
   private static final int DIALOG_WIDTH = 817;
   private static final int DIALOG_HEIGHT = 400;
   private boolean isProductMixListDelivered = false;
   private boolean isProductListDelivered = false;
   private View productLibrary;
   private ProductDialog addProductDialog;
   private ProductMixDialog addProductMixDialog;
   private RelativeLayout tabView;
   protected DisabledOverlay disabled;
   private Drawable arrowCloseDetails;
   private Drawable arrowOpenDetails;
   private Drawable alertIcon;
   private SearchInput productSearch;
   private SearchInput productMixSearch;
   private RelativeLayout productEmptyView;
   private RelativeLayout productMixEmptyView;
   private ProgressiveDisclosureView productsPanel;

   private List<Variety> varietyList;
   private ProgressiveDisclosureView varietiesPanel;

   //Product Mixes
   private ProgressiveDisclosureView productMixesPanel;
   private NestedExpandableListView productListView;
   private ExpandableListView productMixListView;
   private Implement currentImplement;
   private Product currentProduct;
   private List<Product> productList;
   private List<ProductUnits> productUnitsList;
   private List<ProductMix> productMixList;
   private List<ControllerProductConfiguration> controllerProductConfigurationList;
   private ProductAdapter productAdapter;
   private ProductMixAdapter productMixAdapter;
   private AbstractProductComparator productComparator;
   private AbstractProductMixComparator productMixComparator;
   //Products can be measured in either volume or mass
   private MeasurementSystem volumeMeasurementSystem;
   private MeasurementSystem massMeasurementSystem;
   private boolean productSortAscending;
   private boolean productMixSortAscending;
   private String unitRpm = "";
   private String unitInH2O = "";
   private volatile boolean pcmConnected = false;
   private static final int WHAT_LOAD_PRODUCT_LIST = 2;
   private static final int WHAT_LIST = 3;
   private static final int WHAT_CONFIG = 4;
   private static final int WHAT_PING = 5;
   private static final int WHAT_IMPLEMENT = 6;
   private static final int WHAT_LOAD_PRODUCT_MIX_LIST = 7;
   private static final int WHAT_LOAD_UNIT_LIST = 8;
   private static final int WHAT_GET_VARIETY_LIST = 9;
   private IVIPServiceAIDL vipService;
   private IVIPListenerAIDL vipListener = new SimpleVIPListener() {

      @Override
      public void onError(String error) throws RemoteException {
         log.error("Remote error: " + error);
      }

      @Override
      public void onTableChange(TableChangeEvent action, String tableName, String id) throws RemoteException {
         if (DEBUG) {
            log.debug("OnTableChange - Action: " + action + " tableName: " + tableName + " id: " + id);
         }
         if (tableName.equals("com.cnh.pf.model.product.library.Product")) {
            vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST).sendToTarget();
         }
      }

      @Override
      public void onServerConnect() throws RemoteException {
         log.debug("onServerConnect called");
         pcmConnected = true;
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_UNIT_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_GET_VARIETY_LIST).sendToTarget();
         vipService.requestImplementProductConfigCurrent();
      }

      @Override
      public void onServerDisconnect() throws RemoteException {
         pcmConnected = false;
         vipCommunicationHandler.obtainMessage(WHAT_PING).sendToTarget();
      }

      @Override
      public void deliverImplementCurrent(final ImplementCurrent newImplement) throws RemoteException {
         if (DEBUG) {
            log.debug("deliverImplementCurrent");
         }
         ProductLibraryFragment.this.currentImplement = newImplement.getImplement();
         vipCommunicationHandler.obtainMessage(WHAT_IMPLEMENT, 1, 0, null).sendToTarget();
      }

      @Override
      public void deliverProductUnitsList(final List<ProductUnits> productUnits) {
         productUnitsList = productUnits;
         //TODO: partition units list out into pieces here.
      }

      @Override
      public void deliverProductList(final List<Product> products) {
         if (DEBUG) {
            log.debug("deliverProductList");
         }
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               isProductListDelivered = true;
               checkMode();
               List<Product> tempProductList = new ArrayList<Product>();
               for (Product product : products) {
                  if (product != null && product.getProductMixId() == 0) {
                     tempProductList.add(product);
                  }
               }
               populateProducts(tempProductList);
            }
         });
      }

      @Override
      public void deliverImplementProductConfig(ImplementProductConfig config) throws RemoteException {
         if (config != null) {
            controllerProductConfigurationList = config.getControllers();
         }
      }
   };

   private void checkMode() {
      if (isProductMixListDelivered && isProductListDelivered) {
         disabled.setMode(DisabledOverlay.MODE.HIDDEN);
         disabled.setVisibility(View.GONE);
      }
   }

   private Handler vipCommunicationHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
         case WHAT_LOAD_PRODUCT_MIX_LIST:
            new VIPAsyncTask<IVIPServiceAIDL, List<ProductMix>>(vipService, new GenericListener<List<ProductMix>>() {
               @Override
               public void handleEvent(List<ProductMix> productMixList) {
                  populateProductMixes(productMixList);
               }
            }).execute(new LoadProductMixListCommand());

            break;

         case WHAT_LOAD_PRODUCT_LIST:
            if (DEBUG) {
               log.debug("Loading Product list...");
            }
            disabled.setVisibility(View.VISIBLE);
            disabled.setMode(DisabledOverlay.MODE.LOADING);
            try {
               vipService.requestProductList();
            }
            catch (RemoteException ex) {
               log.error(TAG, "Error in WHAT_LOAD_PRODUCT_LIST handler", ex);
            }
            break;
         case WHAT_LOAD_UNIT_LIST:
            if (DEBUG) {
               log.debug("Loading Units list...");
            }
            try {
               //query all ProductUnits
               vipService.requestProductUnitsList(null, null, null, null);
            }
            catch (RemoteException ex) {
               log.error(TAG, "Error in WHAT_LOAD_UNIT_LIST handler", ex);
            }
            volumeMeasurementSystem = ProductHelperMethods.queryMeasurementSystem(getActivity(), UnitsSettings.VOLUME);
            massMeasurementSystem = ProductHelperMethods.queryMeasurementSystem(getActivity(), UnitsSettings.MASS);
            break;
         case WHAT_IMPLEMENT:
            if (DEBUG) {
               log.debug("Loading current implement...");
            }
            populateImplement(msg.arg1 == 1);
            break;
         case WHAT_PING:
            disabled.setVisibility(View.VISIBLE);
            disabled.setMode(pcmConnected ? DisabledOverlay.MODE.LOADING : DisabledOverlay.MODE.DISCONNECTED);
            break;
         case WHAT_GET_VARIETY_LIST:
            log.debug("Loading Variety list");

            new VIPAsyncTask<IVIPServiceAIDL, List<Variety>>(vipService, new GenericListener<List<Variety>>() {
               @Override
               public void handleEvent(List<Variety> varietyList) {
                  log.debug("got variety list:" + varietyList);
                  populateVarieties(varietyList);
               }
            }).execute(new GetVarietyListCommand());

         default:
            log.info("Unexpected default case in vip handler");
            break;
         }

      }
   };

   /**
    * Convert string to Title Case
    * @param input
    * @return string
    */
   private String toTitleCase(String input) {
      StringBuilder titleCase = new StringBuilder();
      boolean nextTitleCase = true;

      for (char c : input.toCharArray()) {
         if (Character.isSpaceChar(c)) {
            nextTitleCase = true;
         }
         else if (nextTitleCase) {
            c = Character.toTitleCase(c);
            nextTitleCase = false;
         }
         else {
            c = Character.toLowerCase(c);
         }

         titleCase.append(c);
      }

      return titleCase.toString();
   }

   /**
    * Makes ENUM_NAMES into friendlier Enum Names
    * @param input
    * @return converted string
    */
   private String friendlyName(String input) {
      String spaced = input.replace("_", " ");
      String capped = toTitleCase(spaced);
      return capped;
   }

   /**
    * Populate picklist adapter for forms dropdown on Add/Edit Product Dialog
    */
   private void populateForms(PickListAdapter formPickListAdapter) {
      for (ProductForm form : ProductForm.values()) {
         if (form != ProductForm.ANY) {
            formPickListAdapter.add(new PickListItem(form.getValue(), friendlyName(form.name())));
         }
      }
   }

   /**
    * Populate picklist adapter for usage dropdown on Add/Edit Product Dialog
    */
   private void populateUsages(PickListAdapter usagePickListAdapter) {
      for (ProductUsage usage : ProductUsage.values()) {
         usagePickListAdapter.add(new PickListItem(usage.getValue(), friendlyName(usage.name())));
      }
   }

   /**
    * Populate picklist adapter for crops dropdown on Add/Edit Product Dialog
    */
   private void populateCrops(PickListAdapter cropPickListAdapter) {
      String[] crops = { CROP_TYPE_GENERIC, CORN_PRODUCT_CROP_TYPE_NAME, WHEAT_PRODUCT_CROP_TYPE_NAME, SOYBEANS_PRODUCT_CROP_TYPE_NAME };
      int i = 0;
      for (String crop : crops) {
         cropPickListAdapter.add(new PickListItem(i++, friendlyName(crop)));
      }
   }

   private List<ProductUnits> filterUnitList(ProductForm productForm, ProductDisplayItem displayItem, MeasurementSystem measurementSystem) {
      List<ProductUnits> filteredList = new ArrayList<ProductUnits>();
      for (ProductUnits p : productUnitsList) {
         if (p.getForm() == productForm && p.getDisplayItem() == displayItem && p.getMeasurementSystem() == measurementSystem) {
            filteredList.add(p);
         }
      }
      return filteredList;
   }

   /**
    * Fill UnitsQueryHolder with data from ProductUnits table on pcm
    * @param targetUnitsQuery
    * @param dialog
    * @param chosenForm
    * @param measurementSystem
    */
   private void retrieveUnitInfo(UnitsQueryHolder targetUnitsQuery, DialogView dialog, ProductForm chosenForm, MeasurementSystem measurementSystem) {
      //possible optimizations include caching results and doing work in background thread
      targetUnitsQuery.resultParent = dialog;
      targetUnitsQuery.productForm = chosenForm;
      targetUnitsQuery.measurementSystem = measurementSystem;

      targetUnitsQuery.toggleMapProductDisplayItem.clear();
      targetUnitsQuery.productDisplayItemMapUnits.clear();

      targetUnitsQuery.toggleMapProductDisplayItem.put((SegmentedToggleButtonGroupPickList) targetUnitsQuery.resultParent.findViewById(R.id.product_units_toggle),
            ProductDisplayItem.RATES);
      List<ProductUnits> ratesList = filterUnitList(targetUnitsQuery.productForm, ProductDisplayItem.RATES, targetUnitsQuery.measurementSystem);
      targetUnitsQuery.productDisplayItemMapUnits.put(ProductDisplayItem.RATES, ratesList);

      targetUnitsQuery.toggleMapProductDisplayItem.put((SegmentedToggleButtonGroupPickList) targetUnitsQuery.resultParent.findViewById(R.id.product_density_toggle),
            ProductDisplayItem.DENSITY);
      List<ProductUnits> densityList = filterUnitList(ProductForm.ANY, ProductDisplayItem.DENSITY, targetUnitsQuery.measurementSystem);
      targetUnitsQuery.productDisplayItemMapUnits.put(ProductDisplayItem.DENSITY, densityList);

      targetUnitsQuery.toggleMapProductDisplayItem.put((SegmentedToggleButtonGroupPickList) targetUnitsQuery.resultParent.findViewById(R.id.product_package_toggle),
            ProductDisplayItem.PACKAGE_SIZE);
      List<ProductUnits> packageSizeList = filterUnitList(targetUnitsQuery.productForm, ProductDisplayItem.PACKAGE_SIZE, targetUnitsQuery.measurementSystem);
      targetUnitsQuery.productDisplayItemMapUnits.put(ProductDisplayItem.PACKAGE_SIZE, packageSizeList);

      targetUnitsQuery.toggleMapProductDisplayItem.put((SegmentedToggleButtonGroupPickList) targetUnitsQuery.resultParent.findViewById(R.id.product_unit_density_toggle),
            ProductDisplayItem.UNIT_DENSITY);
      List<ProductUnits> unitDensityList = filterUnitList(targetUnitsQuery.productForm, ProductDisplayItem.UNIT_DENSITY, targetUnitsQuery.measurementSystem);
      targetUnitsQuery.productDisplayItemMapUnits.put(ProductDisplayItem.UNIT_DENSITY, unitDensityList);

      ProductHelperMethods.populateUnitToggles(targetUnitsQuery);
   }

   /**
    * Inflate and return the default product library view
    * @param inflater
    * @param container
    * @param savedInstanceState
    * @return the default product library view
    */
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      Resources resources = this.getResources();
      this.unitInH2O = resources.getString(R.string.unit_in_h2o);
      this.unitRpm = resources.getString(R.string.unit_rpm);

      productLibrary = inflater.inflate(R.layout.product_library, container, false);

      disabled = (DisabledOverlay) productLibrary.findViewById(R.id.disabled_overlay);
      tabView = (RelativeLayout) getActivity().findViewById(R.id.tab_activity_root);
      productSearch = (SearchInput) productLibrary.findViewById(R.id.product_search);
      productSearch.setTextSize(getResources().getDimension(R.dimen.product_search_text));

      productMixSearch = (SearchInput) productLibrary.findViewById(R.id.product_mix_search);
      productMixSearch.setTextSize(getResources().getDimension(R.dimen.product_search_text));

      productEmptyView = (RelativeLayout) productLibrary.findViewById(R.id.product_empty);
      productMixEmptyView = (RelativeLayout) productLibrary.findViewById(R.id.product_mix_empty);

      productsPanel = (ProgressiveDisclosureView) productLibrary.findViewById(R.id.products_panel);
      productsPanel.setAutoResizable(true);
      productMixesPanel = (ProgressiveDisclosureView) productLibrary.findViewById(R.id.product_mix_panel);
      productMixesPanel.setAutoResizable(true);
      varietiesPanel = (ProgressiveDisclosureView) productLibrary.findViewById(R.id.variety_panel);
      varietiesPanel.setAutoResizable(true);

      productListView = (NestedExpandableListView) productLibrary.findViewById(R.id.product_list);
      productMixListView = (ExpandableListView) productLibrary.findViewById(R.id.product_mix_list);

      Button btnAddProduct = (Button) productLibrary.findViewById(R.id.add_product_button);
      btnAddProduct.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View view) {
            addProductDialog = new ProductDialog(getActivity().getApplicationContext(), vipService, productUnitsList, new ProductDialog.productListCallback() {
               @Override
               public void productList(Product product) {
                  productList.add(product);
                  vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST).sendToTarget();
               }
            }, ProductLibraryFragment.this.currentImplement, productList);

            addProductDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_save_button))
                  .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false)
                  .setTitle(getResources().getString(R.string.product_dialog_add_tile)).setBodyHeight(DIALOG_HEIGHT);
            TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addProductDialog);

            addProductDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
            addProductDialog.disableButtonFirst(true);
            addProductDialog.setDialogWidth(DIALOG_WIDTH);
         }
      });
      Button btnAddProductMix = (Button) productLibrary.findViewById(R.id.add_mix_button);
      btnAddProductMix.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            addProductMixDialog = new ProductMixDialog(getActivity().getApplicationContext(), vipService, new productMixCallBack() {
               @Override
               public void productMix(ProductMix productMix) {
                  vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST).sendToTarget();
               }
            });
            addProductMixDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_add_button))
                  .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
                  .setTitle(getResources().getString(R.string.product_mix_title_dialog_add_product_mix)).setBodyHeight(DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

            TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addProductMixDialog);

            addProductMixDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
            addProductMixDialog.disableButtonFirst(true);
            addProductMixDialog.setDialogWidth(DIALOG_WIDTH);
         }
      });
      arrowCloseDetails = getResources().getDrawable(R.drawable.arrow_down_expanded_productlist);
      arrowOpenDetails = getResources().getDrawable(R.drawable.arrow_up_expanded_productlist);
      alertIcon = getResources().getDrawable(R.drawable.ic_fault_medium_badge);
      return productLibrary;
   }

   private void setProductPanelSubheading(){
      if (this.productList != null) {
         productsPanel.setSubheading(getResources().getQuantityString(
               R.plurals.product_section_subheader_format, this.productList.size(), this.productList.size())
         );
      }
      else {
         productsPanel.setSubheading(getResources().getQuantityString(R.plurals.product_section_subheader_format, 0));
      }
   }

   private void setProductMixPanelSubheading() {
      if (this.productMixList != null) {
         productMixesPanel.setSubheading(getResources().getQuantityString(
               R.plurals.product_mix_subheader_total_mix_format, this.productMixList.size(), this.productMixList.size())
         );
      }
      else {
         productMixesPanel.setSubheading(getResources().getQuantityString(R.plurals.product_mix_subheader_total_mix_format, 0));
      }
   }

   private void setVarietyPanelSubheading(){
      if (this.varietyList != null) {
         varietiesPanel.setSubheading(getResources().getQuantityString(
               R.plurals.variety_section_subheader_format, this.varietyList.size(), this.varietyList.size())
         );
      }
      else {
         varietiesPanel.setSubheading(getResources().getQuantityString(R.plurals.variety_section_subheader_format, 0));
      }
   }

   //TODO: Current Validate only Product not Product mixes
   private boolean validateDeleteProduct(Product product) {
      if (controllerProductConfigurationList == null || controllerProductConfigurationList.isEmpty()) {
         return true;
      }
      for (ControllerProductConfiguration controllerProductConfiguration : controllerProductConfigurationList) {
         if (controllerProductConfiguration != null && controllerProductConfiguration.getController() != null) {
            if (currentImplement.getControllers().contains(controllerProductConfiguration.getController())) {
               for (DriveProductConfiguration driveProductConfiguration : controllerProductConfiguration.getDrives()) {
                  if (driveProductConfiguration != null && driveProductConfiguration.getProduct() != null) {
                     if (driveProductConfiguration.getProduct().getId() == product.getId()) {
                        return false;
                     }
                  }
               }
            }
         }
      }
      return true;
   }

   private void initProductMixSortHeader() {
      final HashMap<Class, ListHeaderSortView> sortButtons = new HashMap<Class, ListHeaderSortView>();
      sortButtons.put(ProductMixNameSortComparator.class, (ListHeaderSortView) productLibrary.findViewById(R.id.product_mix_header_product_name_sort));
      sortButtons.put(ProductMixFormSortComparator.class, (ListHeaderSortView) productLibrary.findViewById(R.id.product_mix_header_product_form_sort));
      sortButtons.put(ProductMixDefaultRateSortComparator.class, (ListHeaderSortView) productLibrary.findViewById(R.id.product_mix_header_product_default_rate_sort));
      OnClickListener sortClickHandler = new OnClickListener() {
         @Override
         public void onClick(View v) {
            for (ListHeaderSortView sortButton : sortButtons.values()) {
               if (sortButton != v) {
                  sortButton.setState(ListHeaderSortView.STATE_NO_SORT);
               }
            }
            if (v instanceof ListHeaderSortView) {
               ListHeaderSortView button = (ListHeaderSortView) v;

               switch (v.getId()) {
               case R.id.product_mix_header_product_name_sort:
                  productMixComparator = new ProductMixNameSortComparator();
                  break;
               case R.id.product_mix_header_product_form_sort:
                  productMixComparator = new ProductMixFormSortComparator();
                  break;
               case R.id.product_mix_header_product_default_rate_sort:
                  productMixComparator = new ProductMixDefaultRateSortComparator();
                  break;
               default:
                  productMixComparator = new ProductMixNameSortComparator();
                  break;
               }
               productMixSortAscending = (button.getState() == ListHeaderSortView.STATE_SORT_ASC);
               if (productMixAdapter != null) {
                  productMixAdapter.sort(productMixComparator, productMixSortAscending);
               }
            }
         }
      };
      for (ListHeaderSortView sortButton : sortButtons.values()) {
         sortButton.setOnClickListener(sortClickHandler);
      }

      //Setup initial sort
      productMixComparator = new ProductMixNameSortComparator();
      productMixSortAscending = true;
      if (productMixAdapter != null) {
         productMixAdapter.sort(productMixComparator, productMixSortAscending);
      }
      ListHeaderSortView currentSortButton = sortButtons.get(ProductMixNameSortComparator.class);
      currentSortButton.setState(ListHeaderSortView.STATE_SORT_ASC);
   }

   private void initProductSortHeader() {
      final HashMap<Class, ListHeaderSortView> sortButtons = new HashMap<Class, ListHeaderSortView>();
      sortButtons.put(NameSortComparator.class, (ListHeaderSortView) productLibrary.findViewById(R.id.header_name));
      sortButtons.put(FormSortComparator.class, (ListHeaderSortView) productLibrary.findViewById(R.id.header_form));
      sortButtons.put(DefaultRateSortComparator.class, (ListHeaderSortView) productLibrary.findViewById(R.id.header_default_rate));
      OnClickListener sortClickHandler = new OnClickListener() {
         @Override
         public void onClick(View v) {
            for (ListHeaderSortView sortButton : sortButtons.values()) {
               if (sortButton != v) {
                  sortButton.setState(ListHeaderSortView.STATE_NO_SORT);
               }
            }
            ListHeaderSortView button = (ListHeaderSortView) v;

            switch (v.getId()) {
            case R.id.header_name:
               productComparator = new NameSortComparator();
               break;
            case R.id.header_form:
               productComparator = new FormSortComparator();
               break;
            case R.id.header_default_rate:
               productComparator = new DefaultRateSortComparator();
               break;
            default:
               productComparator = new NameSortComparator();
               break;
            }
            productSortAscending = (button.getState() == ListHeaderSortView.STATE_SORT_ASC);
            if (productAdapter != null) {
               productAdapter.sort(productComparator, productSortAscending);
            }
         }
      };
      for (ListHeaderSortView sortButton : sortButtons.values()) {
         sortButton.setOnClickListener(sortClickHandler);
      }

      //Setup initial sort
      productComparator = new NameSortComparator();
      productSortAscending = true;
      if (productAdapter != null) {
         productAdapter.sort(productComparator, productSortAscending);
      }
      ListHeaderSortView currentSortButton = sortButtons.get(NameSortComparator.class);
      currentSortButton.setState(ListHeaderSortView.STATE_SORT_ASC);
   }

   /**
    * Set initial state of product library view with overlays and empty messages as needed.
    * @param view
    * @param savedInstanceState
    */
   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      productListView.setEmptyView(productEmptyView);
      productEmptyView.setVisibility(View.GONE);
      productMixListView.setEmptyView(productMixEmptyView);
      productEmptyView.setVisibility(View.GONE);
      initProductSortHeader();
      setProductMixPanelSubheading();
      initProductMixSortHeader();
      setVarietyPanelSubheading();
   }

   private synchronized void populateVarieties(List<Variety> varietyList){
      if (varietyList != null){
         this.varietyList = varietyList;
         setVarietyPanelSubheading();
      }
   }

   private synchronized void populateProductMixes(List<ProductMix> incomingProductMixList) {
      if (incomingProductMixList != null) {
         this.productMixList = incomingProductMixList;
         isProductMixListDelivered = true;
         checkMode();
         setProductMixPanelSubheading();
         productMixAdapter = new ProductMixAdapter();
         productMixAdapter.setItems(incomingProductMixList);
         productMixListView.setAdapter(productMixAdapter);
         productMixSearch.setSearchableContainer(productMixAdapter);
         productMixSearch.addTextChangedListener(new TextWatcherSceleton() {
            @Override
            public void afterTextChanged(Editable s) {
               if (s.toString().isEmpty()) {
                  productMixSearch.setClearIconEnabled(false);
               }
               else {
                  productMixSearch.setClearIconEnabled(true);
               }
            }
         });
         productMixAdapter.setFilter(new ProductMixFilter(productMixAdapter, getActivity(), incomingProductMixList));
         productMixAdapter.notifyDataSetChanged();
         if (productMixComparator != null) {
            productMixAdapter.sort(productMixComparator, productMixSortAscending);
         }
         if (productMixesPanel.isExpanded()) {
            productMixesPanel.resizeContent(false);
         }
      }
   }

   /**
    * Populate current product list
    */
   private synchronized void populateProducts(List<Product> incomingProductList) {
      productList = incomingProductList;
      //TODO review this method and double check if refresh param is neccessary
      if (productListView != null && currentProduct != null && productListView.getSelectedItemId() != currentProduct.getId() && productListView.getSelectedId() > -1) {
         if (DEBUG) {
            log.debug(String.format("Changing selection id. old: %d  new: %d", productListView.getSelectedItemId(), currentProduct.getId()));
         }
         currentProduct = productList.get((int) productListView.getSelectedId());
      }
      productAdapter = new ProductAdapter();
      productAdapter.setItems(productList);
      if (productListView != null) {
         productListView.setAdapter(productAdapter);
      }
      productSearch.setSearchableContainer(productAdapter);
      productSearch.addTextChangedListener(new TextWatcherSceleton() {
         @Override
         public void afterTextChanged(Editable s) {
            if (s.toString().isEmpty()) {
               productSearch.setClearIconEnabled(false);
            }
            else {
               productSearch.setClearIconEnabled(true);
            }
         }
      });

      productAdapter.setFilter(new ProductFilter(productAdapter, getActivity(), productList));
      productAdapter.notifyDataSetChanged();
      if (currentProduct == null && productList.size() > 0) {
         currentProduct = productList.get(0);
      }
      if (productComparator != null) {
         productAdapter.sort(productComparator, productSortAscending);
      }
   }

   /**
    * Populate current implement
    * @param refresh whether or not to select a different adapter, or keep the state
    */
   private synchronized void populateImplement(boolean refresh) {
      if (getActivity() != null && getActivity() instanceof DataManagementActivity && currentImplement != null && currentImplement.getName() != null) {
         ((DataManagementActivity) getActivity()).setTabActivitySubheaderRight(currentImplement.getName());
      }
   }

   /**
    * Restore UI from saved state
    * @param savedInstanceState
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      if (DEBUG) {
         log.debug(TAG, "onCreate");
      }

      if (savedInstanceState != null) {
         productList = savedInstanceState.getParcelableArrayList(PRODUCT_LIST);
         currentProduct = savedInstanceState.getParcelable(CURRENT_PRODCUCT);
         productUnitsList = savedInstanceState.getParcelableArrayList(PRODUCT_UNITS_LIST);
      }

   }

   /**
    * Initialize adapters
    * @param savedInstanceState
    */
   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      if (DEBUG) {
         log.debug(TAG, "onActivityCreated");
      }
   }

   /**
    * Resume operation
    * Register to backend services
    * Reset UI as needed
    */
   @Override
   public void onStart() {
      super.onStart();
      if (DEBUG) {
         log.debug(TAG, "onStart");
      }

      //UI bug? causes the previously opened ProgressiveDisclosureView to contract when returning to the fragment
      if (productsPanel != null) {
         productsPanel.setInnerPaddingLeft(2);
         productsPanel.setInnerPaddingRight(2);
         productsPanel.collapse();
      }
      if (productMixesPanel != null) {
         productMixesPanel.setInnerPaddingLeft(2);
         productMixesPanel.setInnerPaddingRight(2);
         productMixesPanel.collapse();
      }
      if (varietiesPanel != null) {
         varietiesPanel.setInnerPaddingLeft(2);
         varietiesPanel.setInnerPaddingRight(2);
         varietiesPanel.collapse();
      }
   }

   @Override
   public void onResume() {
      log.info("onResume called");
      super.onResume();
      if (vipService != null) {
         new Thread() {
            @Override
            public synchronized void start() {
               log.debug("starting thread for vipServiceListener registration");
               try {
                  vipService.register(identifier, vipListener);
                  vipService.requestImplementCurrent();
                  vipService.requestImplementProductConfigCurrent();
                  log.debug("vipService registered");
               }
               catch (RemoteException e) {
                  log.error("vipService register failed: ", e);
               }
            }
         }.start();
      }
      else {
         log.debug("cannot register vipListener - vipService == " + vipService);
      }
   }

   @Override
   public void onPause() {
      super.onPause();
      unregisterVIPService();
   }

   /**
    * unregister VIPService
    */
   public void unregisterVIPService() {
      if (vipService != null) {
         try {
            vipService.unregister(identifier);
         }
         catch (RemoteException e) {
            log.error("Error", e);
         }
      }
   }

   /**
    * Unregister from backend services
    */
   @Override
   public void onStop() {
      if (DEBUG) {
         log.debug(TAG, "onStop");
      }
      productSearch.setText("");
      productSearch.setClearIconEnabled(false);
      super.onStop();

   }

   /**
    * Persist UI state for later retrieval
    * @param outState
    */
   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      if (DEBUG) {
         log.debug(TAG, "onSaveInstanceState");
      }
      outState.putParcelable(CURRENT_PRODCUCT, currentProduct);
      outState.putParcelableArrayList(PRODUCT_LIST, (ArrayList<Product>) productList);
      outState.putParcelableArrayList(PRODUCT_UNITS_LIST, (ArrayList<ProductUnits>) productUnitsList);
   }

   /**
    * Retrieve rate unit values formatted properly per product settings
    * @param product
    * @param value
    * @return appropriate formatted string for the unit
    */
   public String formatRateUnits(Product product, double value) {
      String result = "";
      boolean unitFound = false;
      if (product.getRateDisplayUnitsImperial() != null) {
         result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getRateDisplayUnitsImperial().getMultiplyFactorFromBaseUnits()));
         if (product.getRateDisplayUnitsImperial().getName() != null) {
            result = result + " " + product.getRateDisplayUnitsImperial().getName();
            unitFound = true;
         }
      }
      else if (product.getRateDisplayUnitsMetric() != null) {
         result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getRateDisplayUnitsMetric().getMultiplyFactorFromBaseUnits()));
         if (product.getRateDisplayUnitsMetric().getName() != null) {
            result = result + " " + product.getRateDisplayUnitsMetric().getName();
            unitFound = true;
         }
      }

      else if (product.getRateDisplayUnitsUSA() != null) {
         result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getRateDisplayUnitsUSA().getMultiplyFactorFromBaseUnits()));
         if (product.getRateDisplayUnitsUSA().getName() != null) {
            result = result + " " + product.getRateDisplayUnitsUSA().getName();
            unitFound = true;
         }
      }

      if (!unitFound) {
         result = String.format("%1.2f", value);
      }

      return result;
   }

   /**
    * Retrieve package unit values formatted properly per product settings
    * @param product
    * @param value
    * @param unit
    * @return appropriate formatted string for the unit
    */
   public String formatPackageUnits(Product product, double value, MeasurementSystem unit) {
      String result = "";
      switch (unit) {
      case IMPERIAL:
         if (product.getPackageSizeUnitsImperial() != null) {
            result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getPackageSizeUnitsImperial().getMultiplyFactorFromBaseUnits()));
            if (product.getPackageSizeUnitsImperial().getName() != null) {
               result = result + " " + product.getPackageSizeUnitsImperial().getName();
            }
         }
         else {
            result = String.format("%1.2f", value);
         }
         break;
      case METRIC:
         if (product.getPackageSizeUnitsMetric() != null) {
            result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getPackageSizeUnitsMetric().getMultiplyFactorFromBaseUnits()));
            if (product.getPackageSizeUnitsMetric().getName() != null) {
               result = result + " " + product.getPackageSizeUnitsMetric().getName();
            }
         }
         else {
            result = String.format("%1.2f", value);
         }
         break;
      case USA:
         if (product.getPackageSizeUnitsUSA() != null) {
            result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getPackageSizeUnitsUSA().getMultiplyFactorFromBaseUnits()));
            if (product.getPackageSizeUnitsUSA().getName() != null) {
               result = result + " " + product.getPackageSizeUnitsUSA().getName();
            }
         }
         else {
            result = String.format("%1.2f", value);
         }
         break;
      default:
         result = String.format("%1.2f", value);
         break;
      }
      return result;
   }

   /**
    * set VIPService to listen
    * @param vipService
    */
   public void setVipService(IVIPServiceAIDL vipService) {
      this.vipService = vipService;
   }

   /**
    * Retrieve density unit values formatted properly per product settings
    * @param product
    * @param value
    * @param unit
    * @return appropriate formatted string for the unit
    */
   public String formatDensityUnits(Product product, double value, MeasurementSystem unit) {
      String result = "";
      switch (unit) {
      case IMPERIAL:
         if (product.getDensityUnitsImperial() != null) {
            result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getDensityUnitsImperial().getMultiplyFactorFromBaseUnits()));
            if (product.getDensityUnitsImperial().getName() != null) {
               result = result + " " + product.getDensityUnitsImperial().getName();
            }
         }
         else {
            result = String.format("%1.2f", value);
         }
         break;
      case METRIC:
         if (product.getDensityUnitsMetric() != null) {
            result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getDensityUnitsMetric().getMultiplyFactorFromBaseUnits()));
            if (product.getDensityUnitsMetric().getName() != null) {
               result = result + " " + product.getDensityUnitsMetric().getName();
            }
         }
         else {
            result = String.format("%1.2f", value);
         }
         break;
      case USA:
         if (product.getDensityUnitsUSA() != null) {
            result = String.format("%2.2f", MathUtility.getConvertedFromBase(value, product.getDensityUnitsUSA().getMultiplyFactorFromBaseUnits()));
            if (product.getDensityUnitsUSA().getName() != null) {
               result = result + " " + product.getDensityUnitsUSA().getName();
            }
         }
         else {
            result = String.format("%1.2f", value);
         }
         break;
      default:
         result = String.format("%1.2f", value);
         break;
      }
      return result;
   }

   private static class ProductMixGroupHolder {
      public TextView nameText;
      public TextView formText;
      public TextView rateText;
      public ImageView groupIndicator;
      public ProductMix productMix;

      public ProductMixGroupHolder(View view) {
         if (view != null) {
            this.nameText = (TextView) view.findViewById(R.id.product_mix_name_text);
            this.formText = (TextView) view.findViewById(R.id.product_mix_form_text);
            this.rateText = (TextView) view.findViewById(R.id.product_mix_rate_text);
            this.groupIndicator = (ImageView) view.findViewById(R.id.product_mix_group_indicator);
         }
      }
   }

   private static class ProductMixChildHolder {
      public TextView appRate1Text;
      public TextView appRate2Text;
      public ImageButton editButton;
      public ImageButton copyButton;
      public ImageButton deleteButton;
      public TableLayout productRecipeTable;
      public ImageView alertIcon;
      public ProductMix productMix;

      public ProductMixChildHolder(View view) {
         if (view != null) {
            this.appRate1Text = (TextView) view.findViewById(R.id.app_rate1_text);
            this.appRate2Text = (TextView) view.findViewById(R.id.app_rate2_text);
            this.productRecipeTable = (TableLayout) view.findViewById(R.id.product_mix_recipe_list);
            this.editButton = (ImageButton) view.findViewById(R.id.edit_button);
            this.copyButton = (ImageButton) view.findViewById(R.id.copy_button);
            this.deleteButton = (ImageButton) view.findViewById(R.id.delete_button);
            this.alertIcon = (ImageView) view.findViewById(R.id.alert_icon);
         }
      }
   }

   public class ProductMixAdapter extends ProductLibraryAdapter<ProductMix> {

      private View inflateView(int id, ViewGroup root) {
         if (ProductLibraryFragment.this.getActivity() != null && ProductLibraryFragment.this.getActivity().getLayoutInflater() != null) {
            return ProductLibraryFragment.this.getActivity().getLayoutInflater().inflate(id, root, false);
         }
         else {
            return null;
         }
      }

      /**
       * Add all ProductMixRecipes to the Overviewtable
       * @param tableLayout
       * @param productMix
       */
      private void addProductsToTableLayout(TableLayout tableLayout, ProductMix productMix) {
         if (tableLayout != null && productMix != null) {
            int childCounter = tableLayout.getChildCount();
            for (int i = 1; i < childCounter; i++) {
               tableLayout.removeViewAt(1);
            }
            int viewCounter = 1;
            Product carrierProduct = productMix.getProductCarrier().getProduct();
            double defaultRate = productMix.getProductMixParameters().getDefaultRate();
            double rate2 = productMix.getProductMixParameters().getRate2();
            double totalAmount = productMix.getMixTotalAmount();
            carrierProduct.setDefaultRate(calculateApplicationRate(productMix.getProductCarrier().getAmount(), defaultRate, totalAmount));
            carrierProduct.setRate2(calculateApplicationRate(productMix.getProductCarrier().getAmount(), rate2, totalAmount));
            tableLayout.addView(createTableRow(carrierProduct), viewCounter++);

            for (ProductMixRecipe recipeElement : productMix.getRecipe()) {
               Product product = recipeElement.getProduct();
               product.setDefaultRate(calculateApplicationRate(recipeElement.getAmount(), defaultRate, totalAmount));
               product.setRate2(calculateApplicationRate(recipeElement.getAmount(), rate2, totalAmount));
               tableLayout.addView(createTableRow(product), viewCounter++);
            }
         }
      }

      /**
       * Calculate the Application Rate for an ProductMix Element
       * @param amount productmix amount
       * @param productMixRate the
       * @param totalAmount
       * @return
       */
      private double calculateApplicationRate(double amount, double productMixRate, double totalAmount) {
         double result = amount / totalAmount;
         return productMixRate * result;
      }

      /**
       * create Tablerow with product data
       * @param product current product to extract the data into the several cells
       * @return created TableRow
       */
      private TableRow createTableRow(Product product) {
         TableRow tableRow = new TableRow(getActivity().getApplicationContext());
         if (product != null) {
            ProductUnits unit = getApplicationRateProductUnit(product, getProductUnitMeasurementSystem(product, volumeMeasurementSystem, massMeasurementSystem));

            tableRow.setGravity(Gravity.CENTER);
            TextView productNameTextView = new TextView(getActivity().getApplicationContext());
            productNameTextView.setText(" " + product.getName());
            productNameTextView.setTextSize(getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
            productNameTextView.setTextColor(getResources().getColor(R.color.defaultTextColor));
            productNameTextView.setTypeface(null, Typeface.BOLD);
            productNameTextView.setBackgroundResource(R.drawable.product_mix_dialog_application_rates_table_background_cell);
            tableRow.addView(productNameTextView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

            TextView applicationRate1TextView = new TextView(getActivity().getApplicationContext());
            applicationRate1TextView.setTextSize(getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
            applicationRate1TextView.setTextColor(getResources().getColor(R.color.defaultTextColor));
            applicationRate1TextView.setTypeface(null, Typeface.BOLD);
            applicationRate1TextView.setBackgroundResource(R.drawable.product_mix_dialog_application_rates_table_background_cell);
            if (unit != null) {
               applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
            }
            else {
               applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate(), ""));
            }
            tableRow.addView(applicationRate1TextView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

            TextView applicationRate2TextView = new TextView(getActivity().getApplicationContext());
            applicationRate2TextView.setTextSize(getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
            applicationRate2TextView.setTextColor(getResources().getColor(R.color.defaultTextColor));
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

      private void initGroupView(View view, ProductMix productDetail, boolean expanded, ViewGroup root, OnClickListener listener) {
         ProductMixGroupHolder viewHolder;
         if (view == null) {
            view = inflateView(R.layout.product_mix_item, root);
         }
         if (view.getTag() == null) {
            viewHolder = new ProductMixGroupHolder(view);
            view.setTag(viewHolder);
         }
         else {
            viewHolder = (ProductMixGroupHolder) view.getTag();
         }

         viewHolder.productMix = productDetail;
         Product parameters = viewHolder.productMix.getProductMixParameters();
         if (parameters != null) {
            viewHolder.nameText.setText(parameters.getName());
            if (parameters.getForm() != null) {
               viewHolder.formText.setText(friendlyName(parameters.getForm().name()));
            }
            else {
               viewHolder.formText.setText(friendlyName(ProductForm.LIQUID.name()));
            }

            viewHolder.rateText.setText(formatRateUnits(parameters, parameters.getDefaultRate()));
         }
         viewHolder.groupIndicator.setImageDrawable(expanded ? arrowOpenDetails : arrowCloseDetails);
         view.setOnClickListener(listener);
      }

      /**
       * Retrieve the outer "collapsed" view of a product list item
       * @param groupId
       * @param expanded
       * @param view
       * @param viewGroup
       * @return outer view of a product list item
       */
      public View getGroupView(final int groupId, boolean expanded, View view, final ViewGroup viewGroup) {
         ProductMix productDetail = getGroup(groupId);
         if (productDetail != null) {
            if (view == null) {
               view = inflateView(R.layout.product_mix_item, viewGroup);
               if (view != null) {
                  view.setTag(new ProductMixGroupHolder(view));
               }
            }
            initGroupView(view, productDetail, expanded, viewGroup, new OnClickListener() {
               @Override
               public void onClick(View v) {
                  if (viewGroup != null) {
                     ExpandableListView listView = (ExpandableListView) viewGroup;
                     for (int i = 0; i < getGroupCount(); i++) {
                        if (i != groupId) {
                           listView.collapseGroup(i);
                        }
                     }
                     if (listView.isGroupExpanded(groupId)) {
                        listView.collapseGroup(groupId);
                     }
                     else {
                        listView.expandGroup(groupId, true);
                     }
                     productMixesPanel.resizeContent(false);

                  }
               }
            });
            if (groupId > 0 && groupId % 2 != 0) {
               view.setBackgroundColor(getResources().getColor(R.color.odd_rows));
            }
            else {
               view.setBackgroundColor(getResources().getColor(R.color.even_rows));
            }
         }
         return view;
      }

      private void initChildView(View view, final ProductMix productDetail, ViewGroup root, OnClickListener editButtonClickListener, OnClickListener copyButtonClickListener,
            OnClickListener deleteButtonClickListener, OnClickListener alertButtonClickListener) {
         final ProductMixChildHolder viewHolder;

         if (view == null) {
            view = inflateView(R.layout.product_mix_item_child_details, root);
         }

         if (view.getTag() == null) {
            viewHolder = new ProductMixChildHolder(view);
            viewHolder.productMix = productDetail;
            view.setTag(viewHolder);
         }
         else {
            viewHolder = (ProductMixChildHolder) view.getTag();
         }

         viewHolder.productMix = productDetail;
         if (viewHolder.productMix != null) {
            Product productMixParameter = viewHolder.productMix.getProductMixParameters();
            viewHolder.appRate1Text.setText(formatRateUnits(productMixParameter, productDetail.getProductMixParameters().getDefaultRate()));
            viewHolder.appRate2Text.setText(formatRateUnits(productMixParameter, productDetail.getProductMixParameters().getRate2()));
            addProductsToTableLayout(viewHolder.productRecipeTable, viewHolder.productMix);
         }
         viewHolder.editButton.setOnClickListener(editButtonClickListener);
         viewHolder.copyButton.setOnClickListener(copyButtonClickListener);
         viewHolder.deleteButton.setOnClickListener(deleteButtonClickListener);
      }

      /**
       * Retrieve the "expanded" view of a product list item
       * @param group
       * @param child
       * @param expanded
       * @param view
       * @param viewGroup
       * @return fully expanded product list item view
       */
      public View getChildView(final int group, int child, boolean expanded, View view, ViewGroup viewGroup) {
         if (view == null) {
            view = inflateView(R.layout.product_mix_item_child_details, viewGroup);
            view.setTag(new ProductMixChildHolder(view));
         }
         final ProductMix productMixDetail = getChild(group, child);
         final Product parameters = productMixDetail.getProductMixParameters();
         final ProductMixChildHolder productMixChildHolder = (ProductMixChildHolder) view.getTag();
         initChildView(view, productMixDetail, viewGroup, new OnClickListener() {
            @Override
            public void onClick(View view) {
               ProductMixDialog editProductMixDialog = new ProductMixDialog(getActivity().getApplicationContext(), ProductMixesDialogActionType.EDIT, vipService, productMixDetail,
                     new productMixCallBack() {
                        @Override
                        public void productMix(ProductMix productMix) {
                           vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST).sendToTarget();
                        }
                     });
               editProductMixDialog.setFirstButtonText(getResources().getString(R.string.save)).setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button))
                     .showThirdButton(false).showThirdButton(false).setTitle(getResources().getString(R.string.product_mix_title_dialog_edit_product_mix))
                     .setBodyHeight(DIALOG_HEIGHT).setBodyHeight(DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

               TabActivity useModal = (DataManagementActivity) getActivity();
               useModal.showModalPopup(editProductMixDialog);

               editProductMixDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
               editProductMixDialog.disableButtonFirst(true);
               editProductMixDialog.setDialogWidth(DIALOG_WIDTH);
            }
         }, new OnClickListener() {
            @Override
            public void onClick(View view) {
               ProductMixDialog copyProductMixDialog = new ProductMixDialog(getActivity().getApplicationContext(), ProductMixesDialogActionType.COPY, vipService, productMixDetail,
                     new productMixCallBack() {
                        @Override
                        public void productMix(ProductMix productMix) {
                           vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST).sendToTarget();

                        }
                     });
               copyProductMixDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_add_button))
                     .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
                     .setTitle(getResources().getString(R.string.product_mix_title_dialog_copy_product_mix)).setBodyHeight(DIALOG_HEIGHT).setBodyHeight(DIALOG_HEIGHT)
                     .setBodyView(R.layout.product_mix_dialog);

               TabActivity useModal = (DataManagementActivity) getActivity();
               useModal.showModalPopup(copyProductMixDialog);

               copyProductMixDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
               copyProductMixDialog.disableButtonFirst(true);
               copyProductMixDialog.setDialogWidth(DIALOG_WIDTH);
            }
         }, new OnClickListener() {
            @Override
            public void onClick(View v) {
               if (validateDeleteProduct(parameters)) {
                  productMixChildHolder.alertIcon.setVisibility(View.GONE);
                  final TextDialogView deleteDialog = new TextDialogView(getActivity().getApplicationContext());
                  deleteDialog.setBodyText(getString(R.string.delete_product_dialog_body_text));
                  deleteDialog.setFirstButtonText(getString(R.string.delete_dialog_confirm_button_text));
                  deleteDialog.setSecondButtonText(getString(R.string.cancel));
                  deleteDialog.showThirdButton(false);
                  deleteDialog.setOnButtonClickListener(new OnButtonClickListener() {
                     @Override
                     public void onButtonClick(DialogViewInterface dialogViewInterface, int buttonNumber) {
                        switch (buttonNumber) {
                        case DialogViewInterface.BUTTON_FIRST:
                           ProductMixCommandParams params = new ProductMixCommandParams();
                           params.productMix = productMixDetail;
                           params.vipService = vipService;
                           new VIPAsyncTask<ProductMixCommandParams, ProductMix>(params, null).execute(new DeleteProductMixCommand());
                           break;
                        }
                        deleteDialog.dismiss();
                     }
                  });
                  TabActivity useModal = (DataManagementActivity) getActivity();
                  useModal.showModalPopup(deleteDialog);
               }
               else {
                  productMixChildHolder.alertIcon.setVisibility(View.VISIBLE);
               }
            }
         }, new OnClickListener() {
            @Override
            public void onClick(View v) {
               if (DEBUG) {
                  log.debug("Alert button pressed for product: " + parameters.getName() + " " + productMixDetail.getId());
               }
               new AlertDialog.Builder(getActivity()).setTitle(R.string.alert_title).setMessage(R.string.alert_in_use)
                     .setPositiveButton(R.string.alert_dismiss, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                           dialog.dismiss();
                        }
                     }).show();
            }
         });

         return view;
      }

      //Perhaps consider rewriting findViewById references below with dependency injection
      @Override
      public void notifyDataSetChanged() {
         //only update if fragment is attached [filter class is executing publishResults when activity is closed]
         if (isAdded()) {
            super.notifyDataSetChanged();
            if (productMixesPanel == null) {
               productMixesPanel = (ProgressiveDisclosureView) productLibrary.findViewById(R.id.product_mix_panel);
            }
            if (productMixesPanel != null) {
               setProductMixPanelSubheading();
               productMixesPanel.invalidate();
            }
         }
      }
   }

   /**
    * ProductGroupHolder
    * Wraps the outer "collapsed" view of a product list item
    */
   private class ProductGroupHolder {
      public ImageView groupIndicator;
      public TextView nameText;
      public TextView formText;
      public TextView rateText;
      public Product product;

      /**
       * Construct new product group holder
       * @param view
       */
      public ProductGroupHolder(View view) {
         this.nameText = ((TextView) view.findViewById(R.id.name_text));
         this.formText = ((TextView) view.findViewById(R.id.form_text));
         this.rateText = ((TextView) view.findViewById(R.id.rate_text));
         this.groupIndicator = ((ImageView) view.findViewById(R.id.group_indicator));
      }
   }

   /**
    * ProductChildHolder
    * Wraps the inner "expanded" view of a product list item
    */
   private class ProductChildHolder {
      public TextView appRate1Text;
      public TextView appRate2Text;
      public TextView minRateText;
      public TextView maxRateText;
      public TextView deltaRateText;
      public TextView packageText;
      public TextView densityText;
      public ImageView alertIcon;
      public ImageButton editButton;
      public ImageButton copyButton;
      public ImageButton deleteButton;
      public Product product;
      public LinearLayout imageRow;
      public boolean productHasImplements;
      public List<View> fanRateContainers = null;
      public TextView vacuumFanRateText = null;
      public TextView vacuumFanDeltaText = null;
      public TextView bulkFillFanRateText = null;
      public TextView bulkFillFanDeltaText = null;

      public ProductChildHolder(View view) {
         this.alertIcon = ((ImageButton) view.findViewById(R.id.alert_icon));
         this.appRate1Text = ((TextView) view.findViewById(R.id.app_rate1_text));
         this.appRate2Text = ((TextView) view.findViewById(R.id.app_rate2_text));
         this.minRateText = ((TextView) view.findViewById(R.id.min_rate_text));
         this.maxRateText = ((TextView) view.findViewById(R.id.max_rate_text));
         this.deltaRateText = ((TextView) view.findViewById(R.id.delta_rate_text));
         this.packageText = ((TextView) view.findViewById(R.id.package_text));
         this.densityText = ((TextView) view.findViewById(R.id.density_text));
         this.copyButton = ((ImageButton) view.findViewById(R.id.copy_button));
         this.editButton = ((ImageButton) view.findViewById(R.id.edit_button));
         this.deleteButton = ((ImageButton) view.findViewById(R.id.delete_button));
         this.imageRow = ((LinearLayout) view.findViewById(R.id.linear_layout_image_row));

         this.fanRateContainers = new ArrayList<View>(4);
         this.fanRateContainers.add(view.findViewById(R.id.vacuum_fan_rate_container));
         this.fanRateContainers.add(view.findViewById(R.id.vacuum_fan_delta_container));
         this.fanRateContainers.add(view.findViewById(R.id.bulk_fill_fan_rate_container));
         this.fanRateContainers.add(view.findViewById(R.id.bulk_fill_fan_delta_container));

         this.vacuumFanRateText = ((TextView) view.findViewById(R.id.vacuum_fan_rate_text));
         this.vacuumFanDeltaText = ((TextView) view.findViewById(R.id.vacuum_fan_delta_text));
         this.bulkFillFanRateText = ((TextView) view.findViewById(R.id.bulk_fill_fan_rate_text));
         this.bulkFillFanDeltaText = ((TextView) view.findViewById(R.id.bulk_fill_fan_delta_text));
      }

      void setFanUiVisibility(boolean visible) {
         for (View container : fanRateContainers) {
            UiUtility.setVisible(container, visible);
         }
      }
   }

   public class ProductAdapter extends ProductLibraryAdapter<Product> {

      private View inflateView(int id, ViewGroup root) {
         if (ProductLibraryFragment.this.getActivity() != null && ProductLibraryFragment.this.getActivity().getLayoutInflater() != null) {
            return ProductLibraryFragment.this.getActivity().getLayoutInflater().inflate(id, root, false);
         }
         else {
            return null;
         }
      }

      //Perhaps consider rewriting findViewById references below with dependency injection
      @Override
      public void notifyDataSetChanged() {
         //only update if fragment is attached [filter class is executing publishResults when activity is closed]
         if (isAdded()) {
            super.notifyDataSetChanged();
            if (productsPanel == null) {
               productsPanel = (ProgressiveDisclosureView) productLibrary.findViewById(R.id.products_panel);
            }
            if (productsPanel != null) {
               setProductPanelSubheading();
               productsPanel.invalidate();
            }
         }
      }

      private void initGroupView(View view, Product productDetail, boolean expanded, ViewGroup root, OnClickListener listener) {
         ProductGroupHolder viewHolder;
         if (view == null) {
            view = inflateView(R.layout.product_item, root);
         }
         if (view.getTag() == null) {
            viewHolder = new ProductGroupHolder(view);
            view.setTag(viewHolder);
         }
         else {
            viewHolder = (ProductGroupHolder) view.getTag();
         }

         viewHolder.product = productDetail;

         viewHolder.nameText.setText(productDetail.getName());
         if (productDetail.getForm() != null) {
            viewHolder.formText.setText(friendlyName(productDetail.getForm().name()));
         }
         else {
            viewHolder.formText.setText(friendlyName(ProductForm.LIQUID.name()));
         }
         String rateUnitSuffix;
         if (productDetail.getRateDisplayUnitsUSA() != null && productDetail.getRateDisplayUnitsUSA().getName() != null) {
            rateUnitSuffix = " " + productDetail.getRateDisplayUnitsUSA().getName();
         }
         else {
            rateUnitSuffix = "";
         }
         viewHolder.rateText.setText(formatRateUnits(productDetail, productDetail.getDefaultRate()));

         viewHolder.groupIndicator.setImageDrawable(expanded ? arrowOpenDetails : arrowCloseDetails);
         view.setOnClickListener(listener);
      }

      /**
       * Retrieve the outer "collapsed" view of a product list item
       * @param position
       * @param expanded
       * @param view
       * @param viewGroup
       * @return outer view of a product list item
       */
      public View getGroupView(final int position, boolean expanded, View view, final ViewGroup viewGroup) {
         if (view == null) {
            view = inflateView(R.layout.product_item, viewGroup);
            view.setTag(new ProductGroupHolder(view));
         }
         Product productDetail = getGroup(position);

         initGroupView(view, productDetail, expanded, viewGroup, new OnClickListener() {
            @Override
            public void onClick(View v) {
               ExpandableListView listView = (ExpandableListView) viewGroup;
               for (int i = 0; i < getGroupCount(); i++) {
                  if (i != position) {
                     listView.collapseGroup(i);
                  }
               }
               if (listView.isGroupExpanded(position)) {
                  listView.collapseGroup(position);
               }
               else {
                  listView.expandGroup(position, true);
               }
               productsPanel.resizeContent(false);
            }
         });

         if (position > 0 && position % 2 != 0) {
            view.setBackgroundColor(getResources().getColor(R.color.odd_rows));
         }
         else {
            view.setBackgroundColor(getResources().getColor(R.color.even_rows));
         }

         return view;
      }

      private void initChildView(View view, final Product productDetail, ViewGroup root, OnClickListener copyButtonClickListener, OnClickListener editButtonClickListener,
            OnClickListener deleteButtonClickListener, OnClickListener alertButtonClickListener) {
         final ProductChildHolder viewHolder;

         if (view == null) {
            view = inflateView(R.layout.product_item_child_details, root);
         }

         if (view.getTag() == null) {
            viewHolder = new ProductChildHolder(view);
            viewHolder.productHasImplements = false;
            if (validateDeleteProduct(productDetail)) {
               viewHolder.alertIcon.setVisibility(View.GONE);
            }
            else {
               viewHolder.alertIcon.setVisibility(View.VISIBLE);
            }
            view.setTag(viewHolder);
         }
         else {
            viewHolder = (ProductChildHolder) view.getTag();
         }

         viewHolder.product = productDetail;

         if (viewHolder.product != null) {
            MeasurementSystem measurementSystem = getProductUnitMeasurementSystem(viewHolder.product, volumeMeasurementSystem, massMeasurementSystem);
            viewHolder.appRate1Text.setText(formatRateUnits(productDetail, productDetail.getDefaultRate()));
            viewHolder.appRate2Text.setText(formatRateUnits(productDetail, productDetail.getRate2()));
            viewHolder.deltaRateText.setText(formatRateUnits(productDetail, productDetail.getDeltaRate()));
            viewHolder.minRateText.setText(formatRateUnits(productDetail, productDetail.getMinRate()));
            viewHolder.maxRateText.setText(formatRateUnits(productDetail, productDetail.getMaxRate()));
            viewHolder.packageText.setText(formatPackageUnits(productDetail, productDetail.getPackageSize(), measurementSystem));
            viewHolder.densityText.setText(formatDensityUnits(productDetail, productDetail.getDensity(), measurementSystem));

            CNHPlanterFanData cnhPlanterFanData = productDetail.getCnhPlanterFanData();

            if (productDetail.getForm() == ProductForm.SEED && cnhPlanterFanData != null) {

               double vacuumUiRate = MathUtility.getConvertedFromBase(cnhPlanterFanData.getVacuumFanDefaultRate1(), unit_constantsConstants.in_H2O_PER_kPa);
               double vacuumUiDelta = MathUtility.getConvertedFromBase(cnhPlanterFanData.getVacuumFanDeltaRate(), unit_constantsConstants.in_H2O_PER_kPa);
               viewHolder.vacuumFanRateText.setText(UiUtility.getValueAsString(vacuumUiRate, 1) + " " + unitInH2O);
               viewHolder.vacuumFanDeltaText.setText(UiUtility.getValueAsString(vacuumUiDelta, 1) + " " + unitInH2O);
               viewHolder.bulkFillFanRateText.setText(UiUtility.getValueAsString(cnhPlanterFanData.getBulkFillFanDefaultRate1(), 0) + " " + unitRpm);
               viewHolder.bulkFillFanDeltaText.setText(UiUtility.getValueAsString(cnhPlanterFanData.getBulkFillFanDeltaRate(), 0) + " " + unitRpm);
               viewHolder.setFanUiVisibility(true);
            }
            else {
               viewHolder.setFanUiVisibility(false);
            }

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.imageRow.getLayoutParams();
            if (!validateDeleteProduct(productDetail)) {
               viewHolder.alertIcon.setImageResource(R.drawable.ic_needs_checking);
               viewHolder.alertIcon.setVisibility(View.VISIBLE);
               params.topMargin = 11;
               viewHolder.imageRow.setLayoutParams(params);
            }
            else {
               viewHolder.alertIcon.setImageResource(R.drawable.ic_needs_checking);
               viewHolder.alertIcon.setVisibility(View.GONE);
               params.topMargin = 33;
               viewHolder.imageRow.setLayoutParams(params);
            }
         }
         viewHolder.alertIcon.setOnClickListener(alertButtonClickListener);
         viewHolder.editButton.setOnClickListener(editButtonClickListener);
         viewHolder.copyButton.setOnClickListener(copyButtonClickListener);
         viewHolder.deleteButton.setOnClickListener(deleteButtonClickListener);
      }

      /**
       * Retrieve the "expanded" view of a product list item
       * @param group
       * @param child
       * @param expanded
       * @param view
       * @param viewGroup
       * @return fully expanded product list item view
       */
      public View getChildView(final int group, int child, boolean expanded, View view, ViewGroup viewGroup) {

         if (view == null) {
            view = inflateView(R.layout.product_item_child_details, viewGroup);
            view.setTag(new ProductChildHolder(view));
         }
         final ProductChildHolder productChildHolder = (ProductChildHolder) view.getTag();
         final Product productDetail = getChild(group, child);
         if (validateDeleteProduct(productDetail)) {
            productChildHolder.alertIcon.setVisibility(View.GONE);
         }
         else {
            productChildHolder.alertIcon.setVisibility(View.VISIBLE);
         }
         initChildView(view, productDetail, viewGroup, new OnClickListener() {
            @Override
            public void onClick(View v) {
               final ProductDialog copyDialog;

               copyDialog = new ProductDialog(getActivity().getApplicationContext(), vipService, ProductDialog.DialogActionType.COPY, productChildHolder.product, productUnitsList,
                     new ProductDialog.productListCallback() {
                        @Override
                        public void productList(Product product) {
                           productList.add(product);
                           vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST).sendToTarget();
                        }
                     }, ProductLibraryFragment.this.currentImplement, productList);
               copyDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_save_button))
                     .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
                     .setTitle(getResources().getString(R.string.product_dialog_copy_title)).setBodyHeight(DIALOG_HEIGHT);

               TabActivity useModal = (DataManagementActivity) getActivity();
               useModal.showModalPopup(copyDialog);

               copyDialog.setDialogWidth(DIALOG_WIDTH);

               copyDialog.disableButtonFirst(true);

            }
         }, new OnClickListener() {
            @Override
            public void onClick(View v) {
               if (DEBUG) {
                  log.debug("Edit button pressed for product: " + productDetail.getName() + " " + productDetail.getId());
               }

               ProductDialog editDialog;

               editDialog = new ProductDialog(getActivity().getApplicationContext(), vipService, ProductDialog.DialogActionType.EDIT, productDetail, productUnitsList,
                     new ProductDialog.productListCallback() {

                        @Override
                        public void productList(Product product) {
                           vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST).sendToTarget();
                        }
                     }, ProductLibraryFragment.this.currentImplement, productList);
               editDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_save_button))
                     .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false)
                     .setTitle(getResources().getString(R.string.product_dialog_edit_title)).setBodyHeight(DIALOG_HEIGHT);

               TabActivity useModal = (DataManagementActivity) getActivity();
               useModal.showModalPopup(editDialog);

               editDialog.setDialogWidth(DIALOG_WIDTH);

               editDialog.disableButtonFirst(true);

            }
         }, new OnClickListener() {
            @Override
            public void onClick(View v) {
               if (validateDeleteProduct(productDetail)) {
                  productChildHolder.alertIcon.setVisibility(View.GONE);
                  final TextDialogView deleteDialog = new TextDialogView(getActivity().getApplicationContext());
                  deleteDialog.setBodyText(getString(R.string.delete_product_dialog_body_text));
                  deleteDialog.setFirstButtonText(getString(R.string.delete_dialog_confirm_button_text));
                  deleteDialog.setSecondButtonText(getString(R.string.cancel));
                  deleteDialog.showThirdButton(false);
                  deleteDialog.setOnButtonClickListener(new OnButtonClickListener() {
                     @Override
                     public void onButtonClick(DialogViewInterface dialogViewInterface, int buttonNumber) {
                        if (buttonNumber == DialogViewInterface.BUTTON_FIRST) {
                           deleteProduct();
                        }
                        deleteDialog.dismiss();
                     }
                  });
                  TabActivity useModal = (DataManagementActivity) getActivity();
                  useModal.showModalPopup(deleteDialog);
               }
               else {
                  productChildHolder.alertIcon.setVisibility(View.VISIBLE);
               }
            }

            private void deleteProduct() {
               if (DEBUG) {
                  log.debug("Delete button pressed for product: " + productDetail.getName() + " " + productDetail.getId());
               }
               productList.remove(productDetail);
               ProductCommandParams params = new ProductCommandParams();
               params.product = productDetail;
               params.vipService = vipService;
               new VIPAsyncTask<ProductCommandParams, Product>(params, new GenericListener<Product>() {
                  @Override
                  public void handleEvent(Product param) {
                     if (param != null) {
                        productAdapter.notifyDataSetChanged();
                        productListView.invalidate();
                     }
                  }
               }).execute(new DeleteProductCommand());
            }
         }, new OnClickListener() {
            @Override
            public void onClick(View v) {
               if (DEBUG) {
                  log.debug("Alert button pressed for product: " + productDetail.getName() + " " + productDetail.getId());
               }
               new AlertDialog.Builder(getActivity()).setTitle(R.string.alert_title).setMessage(R.string.alert_in_use)
                     .setPositiveButton(R.string.alert_dismiss, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                           dialog.dismiss();
                        }
                     }).show();
            }
         });
         return view;
      }
   }

   public static class TextWatcherSceleton implements TextWatcher {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {

      }
   }
}
