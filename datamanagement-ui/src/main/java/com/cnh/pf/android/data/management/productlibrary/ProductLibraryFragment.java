/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.cnh.android.pf.widget.controls.SearchInput;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.android.pf.widget.utilities.commands.GetVarietyListCommand;
import com.cnh.android.pf.widget.utilities.commands.LoadProductMixListCommand;
import com.cnh.android.pf.widget.utilities.listeners.GenericListener;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.pf.widget.view.DisabledOverlay;
import com.cnh.android.pf.widget.view.ProductDialog;
import com.cnh.android.vip.aidl.IVIPListenerAIDL;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.vip.aidl.SimpleVIPListener;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressiveDisclosureView;
import com.cnh.pf.android.data.management.DataManagementActivity;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.adapter.ProductAdapter;
import com.cnh.pf.android.data.management.productlibrary.adapter.ProductMixAdapter;
import com.cnh.pf.android.data.management.productlibrary.adapter.VarietyAdapter;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.AbstractProductComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.AbstractProductMixComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.AbstractVarietyComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.DefaultRateSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.FormSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.NameSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.ProductMixDefaultRateSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.ProductMixFormSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.ProductMixNameSortComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.VarietyByCropTypeComparator;
import com.cnh.pf.android.data.management.productlibrary.utility.sorts.VarietyByNameComparator;
import com.cnh.pf.android.data.management.productlibrary.views.AddOrEditVarietyDialog;
import com.cnh.pf.android.data.management.productlibrary.views.ListHeaderSortView;
import com.cnh.pf.android.data.management.productlibrary.views.NestedExpandableListView;
import com.cnh.pf.android.data.management.productlibrary.views.ProductMixDialog;
import com.cnh.pf.android.data.management.productlibrary.views.ProductMixDialog.ProductMixCallBack;
import com.cnh.pf.model.TableChangeEvent;
import com.cnh.pf.model.product.configuration.ControllerProductConfiguration;
import com.cnh.pf.model.product.configuration.DriveProductConfiguration;
import com.cnh.pf.model.product.configuration.ImplementProductConfig;
import com.cnh.pf.model.product.configuration.Variety;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductMix;
import com.cnh.pf.model.product.library.ProductUnits;
import com.cnh.pf.model.vip.vehimp.Implement;
import com.cnh.pf.model.vip.vehimp.ImplementCurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import roboguice.fragment.provided.RoboFragment;

import javax.annotation.Nullable;

/**
 * ProductLibraryFragment
 * Supplies tab interface for products (and eventually product mixes)
 * Provides add/edit product functionality
 * Created by joorjitham on 3/27/2015.
 */
public class ProductLibraryFragment extends RoboFragment implements ProductMixCallBack {
   private static final Logger log = LoggerFactory.getLogger(ProductLibraryFragment.class);
   private final String identifier = ProductLibraryFragment.class.getSimpleName() + System.identityHashCode(this);
   private static final String PRODUCT_LIST = "product list";
   private static final String PRODUCT_UNITS_LIST = "product units list";
   private static final String CURRENT_PRODUCT = "current product";
   public static final int LEFT_RIGHT_MARGIN = 42;
   public static final int TOP_BOTTOM_MARGIN = 1;
   public static final int DIALOG_WIDTH = 817;
   public static final int DIALOG_HEIGHT = 400;
   private boolean isProductMixListDelivered = false;
   private boolean isProductListDelivered = false;
   private boolean isVarietyListDelivered = false;
   private View productLibraryLayout;
   private ProductDialog addProductDialog;
   private ProductMixDialog addProductMixDialog;
   protected DisabledOverlay disabledOverlay;
   private SearchInput productSearch;
   private SearchInput productMixSearch;
   private RelativeLayout productEmptyView;
   private RelativeLayout productMixEmptyView;
   private ProgressiveDisclosureView productsPanel;

   // Varieties
   private ArrayList<Variety> varietyList;
   private ProgressiveDisclosureView varietiesPanel;
   private SearchInput varietiesSearch;
   private ListView varietiesListView;
   private VarietyAdapter varietyAdapter;
   private AbstractVarietyComparator varietyComparator;
   private boolean varietySortAscending;
   private AddOrEditVarietyDialog addVarietyDialog;
   private RelativeLayout varietiesListEmptyView;

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
         log.debug("OnTableChange - Action:{}, tableName:{}, id:{}", action, tableName, id);
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
         log.debug("deliverImplementCurrent");
         ProductLibraryFragment.this.currentImplement = newImplement.getImplement();
         vipCommunicationHandler.obtainMessage(WHAT_IMPLEMENT, 1, 0, null).sendToTarget();
         if (productAdapter != null && newImplement != null){
            productAdapter.setCurrentImplement(ProductLibraryFragment.this.currentImplement);
         }
      }

      @Override
      public void deliverProductUnitsList(final List<ProductUnits> productUnits) {
         productUnitsList = productUnits;
         if (productAdapter != null && productUnits != null){
            productAdapter.setProductUnits(productUnits);
         }
         //TODO: partition units list out into pieces here.
      }

      @Override
      public void deliverProductList(final List<Product> products) {
         log.debug("deliverProductList");
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

      @Override
      public void deliverVarietyList(final List<Variety> newVarietyList) throws RemoteException {
         log.debug("deliverVarietyList");
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               populateVarieties(newVarietyList);
            }
         });
      }

      @Override
      public void deliverProductMixList(final List<ProductMix> newProductMixList) throws RemoteException {
         log.debug("deliverProductMixList");
         getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
               if (newProductMixList != null) {
                  populateProductMixes(newProductMixList);
               }
            }
         });
      }
   };

   private void checkMode() {
      if (isProductMixListDelivered && isProductListDelivered && isVarietyListDelivered) {
         disabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
         disabledOverlay.setVisibility(View.GONE);
      }
   }

   private Handler vipCommunicationHandler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
         case WHAT_LOAD_PRODUCT_MIX_LIST:
            log.debug("Loading Product Mix list...");
            new VIPAsyncTask<IVIPServiceAIDL, List<ProductMix>>(vipService, new GenericListener<List<ProductMix>>() {
               @Override
               public void handleEvent(List<ProductMix> productMixList) {
                  populateProductMixes(productMixList);
               }
            }).execute(new LoadProductMixListCommand());
            break;
         case WHAT_LOAD_PRODUCT_LIST:
            log.debug("Loading Product list...");
            disabledOverlay.setVisibility(View.VISIBLE);
            disabledOverlay.setMode(DisabledOverlay.MODE.LOADING);
            try {
               vipService.requestProductList();
            }
            catch (RemoteException ex) {
               log.error("Error in WHAT_LOAD_PRODUCT_LIST handler", ex);
            }
            break;
         case WHAT_LOAD_UNIT_LIST:
            log.debug("Loading Units list...");
            try {
               //query all ProductUnits
               vipService.requestProductUnitsList(null, null, null, null);
            }
            catch (RemoteException ex) {
               log.error("Error in WHAT_LOAD_UNIT_LIST handler", ex);
            }
            volumeMeasurementSystem = ProductHelperMethods.queryMeasurementSystem(getActivity(), UnitsSettings.VOLUME);
            massMeasurementSystem = ProductHelperMethods.queryMeasurementSystem(getActivity(), UnitsSettings.MASS);
            break;
         case WHAT_IMPLEMENT:
            log.debug("Loading current implement...");
            populateImplement();
            break;
         case WHAT_PING:
            disabledOverlay.setVisibility(View.VISIBLE);
            disabledOverlay.setMode(pcmConnected ? DisabledOverlay.MODE.LOADING : DisabledOverlay.MODE.DISCONNECTED);
            break;
         case WHAT_GET_VARIETY_LIST:
            log.debug("Loading Variety list");
            new VIPAsyncTask<IVIPServiceAIDL, List<Variety>>(vipService, new GenericListener<List<Variety>>() {
               @Override
               public void handleEvent(final List<Variety> newVarietyList) {
                  log.debug("got variety list: {}", newVarietyList);
                  populateVarieties(newVarietyList);
               }
            }).execute(new GetVarietyListCommand());
            break;
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
   private static String toTitleCase(String input) {
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
    * // FIXME: using enum names for ui is a bug - see
    *  https://polarion.cnhind.com/polarion/#/project/pfhmidevdefects/workitem?id=pfhmi-dev-defects-3034
    *
    * Makes ENUM_NAMES into friendlier Enum Names
    * @param input
    * @return converted string
    * @deprecated never use see https://polarion.cnhind.com/polarion/#/project/pfhmidevdefects/workitem?id=pfhmi-dev-defects-3034
    */
   public static String friendlyName(String input) {
      String spaced = input.replace("_", " ");
      return toTitleCase(spaced);
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
      log.debug("onCreateView");
      Resources resources = this.getResources();
      productLibraryLayout = inflater.inflate(R.layout.product_library, container, false);
      disabledOverlay = (DisabledOverlay) productLibraryLayout.findViewById(R.id.disabled_overlay);

      productsPanel = (ProgressiveDisclosureView) productLibraryLayout.findViewById(R.id.products_panel);
      productsPanel.setAutoResizable(true);
      productSearch = (SearchInput) productsPanel.findViewById(R.id.product_search);
      productSearch.setTextSize(getResources().getDimension(R.dimen.search_text_size));
      productEmptyView = (RelativeLayout) productsPanel.findViewById(R.id.product_empty);
      productListView = (NestedExpandableListView) productsPanel.findViewById(R.id.product_list);

      productMixesPanel = (ProgressiveDisclosureView) productLibraryLayout.findViewById(R.id.product_mix_panel);
      productMixesPanel.setAutoResizable(true);
      productMixSearch = (SearchInput) productMixesPanel.findViewById(R.id.product_mix_search);
      productMixSearch.setTextSize(getResources().getDimension(R.dimen.search_text_size));
      productMixEmptyView = (RelativeLayout) productMixesPanel.findViewById(R.id.product_mix_empty);
      productMixListView = (ExpandableListView) productMixesPanel.findViewById(R.id.product_mix_list);

      varietiesPanel = (ProgressiveDisclosureView) productLibraryLayout.findViewById(R.id.variety_panel);
      varietiesPanel.setAutoResizable(true);
      varietiesSearch = (SearchInput) varietiesPanel.findViewById(R.id.variety_search);
      varietiesSearch.setTextSize(getResources().getDimension(R.dimen.search_text_size));
      varietiesListEmptyView = (RelativeLayout) varietiesPanel.findViewById(R.id.no_avaiblable_varieties);
      varietiesListView = (ListView) varietiesPanel.findViewById(R.id.varieties_list);

      Button btnAddProduct = (Button) productsPanel.findViewById(R.id.add_product_button);
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
            final TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addProductDialog);

            addProductDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
            addProductDialog.disableButtonFirst(true);
            addProductDialog.setDialogWidth(DIALOG_WIDTH);
         }
      });
      Button btnAddProductMix = (Button) productMixesPanel.findViewById(R.id.add_mix_button);
      btnAddProductMix.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            addProductMixDialog = new ProductMixDialog(getActivity().getApplicationContext(), vipService, ProductLibraryFragment.this, productMixList);
            addProductMixDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_add_button))
                  .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false)
                  .setTitle(getResources().getString(R.string.product_mix_title_dialog_add_product_mix)).setBodyHeight(DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

            final TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addProductMixDialog);

            addProductMixDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
            addProductMixDialog.disableButtonFirst(true);
            addProductMixDialog.setDialogWidth(DIALOG_WIDTH);
         }
      });
      Button btnAddVariety = (Button) varietiesPanel.findViewById(R.id.variety_button_add);
      btnAddVariety.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            addVarietyDialog = new AddOrEditVarietyDialog(getActivity().getApplicationContext());
            addVarietyDialog.setFirstButtonText(getResources().getString(R.string.variety_dialog_save_button_text))
                  .setSecondButtonText(getResources().getString(R.string.variety_dialog_cancel_button_text))
                  .showThirdButton(false).setTitle(getResources().getString(R.string.variety_add_dialog_title_text))
                  .setBodyHeight(DIALOG_HEIGHT).setBodyView(R.layout.variety_add_or_edit_dialog).setDialogWidth(DIALOG_WIDTH);

            addVarietyDialog.setActionType(AddOrEditVarietyDialog.VarietyDialogActionType.ADD);
            if (varietyList != null){
               addVarietyDialog.setVarietyList(varietyList);
            }
            addVarietyDialog.setVIPService(vipService);
            final TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addVarietyDialog);
         }
      });
      return productLibraryLayout;
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
   public boolean validateDeleteProduct(Product product) {
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
      sortButtons.put(ProductMixNameSortComparator.class, (ListHeaderSortView) productLibraryLayout.findViewById(R.id.product_mix_header_product_name_sort));
      sortButtons.put(ProductMixFormSortComparator.class, (ListHeaderSortView) productLibraryLayout.findViewById(R.id.product_mix_header_product_form_sort));
      sortButtons.put(ProductMixDefaultRateSortComparator.class, (ListHeaderSortView) productLibraryLayout.findViewById(R.id.product_mix_header_product_default_rate_sort));
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
      sortButtons.put(NameSortComparator.class, (ListHeaderSortView) productLibraryLayout.findViewById(R.id.header_name));
      sortButtons.put(FormSortComparator.class, (ListHeaderSortView) productLibraryLayout.findViewById(R.id.header_form));
      sortButtons.put(DefaultRateSortComparator.class, (ListHeaderSortView) productLibraryLayout.findViewById(R.id.header_default_rate));
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

   private void initVarietySortHeader(){
      final HashMap<Class, ListHeaderSortView> sortButtons = new HashMap<Class, ListHeaderSortView>();
      sortButtons.put(VarietyByNameComparator.class, (ListHeaderSortView) varietiesPanel.findViewById(R.id.varieties_list_header_name));
      sortButtons.put(VarietyByCropTypeComparator.class, (ListHeaderSortView) varietiesPanel.findViewById(R.id.varieties_list_header_crop_type));
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
               case R.id.varieties_list_header_name:
                  varietyComparator = new VarietyByNameComparator(getActivity().getApplicationContext());
                  break;
               case R.id.varieties_list_header_crop_type:
                  varietyComparator = new VarietyByCropTypeComparator(getActivity().getApplicationContext());
                  break;
               default:
                  varietyComparator = new VarietyByNameComparator(getActivity().getApplicationContext());
                  break;
            }
            varietySortAscending = (button.getState() == ListHeaderSortView.STATE_SORT_ASC);
            if (varietyAdapter != null) {
               varietyAdapter.sort(varietyComparator, varietySortAscending);
            }
         }
      };
      for (ListHeaderSortView sortButton : sortButtons.values()) {
         sortButton.setOnClickListener(sortClickHandler);
      }

      //Setup initial sort
      varietyComparator = new VarietyByNameComparator(getActivity().getApplicationContext());
      varietySortAscending = true;
      if (varietyAdapter != null) {
         varietyAdapter.sort(varietyComparator, varietySortAscending);
      }
      ListHeaderSortView currentSortButton = sortButtons.get(VarietyByNameComparator.class);
      currentSortButton.setState(ListHeaderSortView.STATE_SORT_ASC);
   }

   /**
    * Set initial state of product library view with overlays and empty messages as needed.
    * @param view
    * @param savedInstanceState
    */
   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      log.debug("onViewCreated");
      super.onViewCreated(view, savedInstanceState);
      productListView.setEmptyView(productEmptyView);
      productEmptyView.setVisibility(View.GONE);
      productMixListView.setEmptyView(productMixEmptyView);
      productMixEmptyView.setVisibility(View.GONE);
      varietiesListView.setEmptyView(varietiesListEmptyView);
      varietiesListEmptyView.setVisibility(View.GONE);
      setProductPanelSubheading();
      initProductSortHeader();
      setProductMixPanelSubheading();
      initProductMixSortHeader();
      setVarietyPanelSubheading();
      initVarietySortHeader();
   }

   private synchronized void populateVarieties(List<Variety> varietyList){
      if (varietyList != null){
         this.varietyList = new ArrayList<Variety>(varietyList);
         isVarietyListDelivered = true;
         checkMode();
         setVarietyPanelSubheading();
         if (varietyAdapter == null) {
            varietyAdapter = new VarietyAdapter(getActivity().getApplicationContext(), varietyList, (TabActivity) getActivity(), vipService);
         }
         else {
            varietyAdapter.setVarietyList(varietyList);
            // the new varietyList needs to be filtered with the old filter input
            varietyAdapter.getFilter().filter(varietiesSearch.getText());
         }
         // Because android creates new views during onCreateView which is also called when the user switches the tab and
         // returns to this tab/fragment. The next lines have to be executed whether we create a new adapter or not.
         varietiesListView.setAdapter(varietyAdapter);
         varietiesSearch.setFilterable(varietyAdapter);
         varietiesSearch.addTextChangedListener(new SearchInputTextWatcher(varietiesSearch));
         if (varietyComparator != null) {
            varietyAdapter.sort(varietyComparator, varietySortAscending);
         }
         if (varietiesPanel.isExpanded()) {
            varietiesPanel.resizeContent(false);
         }
      }
   }

   private synchronized void populateProductMixes(List<ProductMix> incomingProductMixList) {
      if (incomingProductMixList != null) {
         this.productMixList = new ArrayList<ProductMix>(incomingProductMixList);
         isProductMixListDelivered = true;
         checkMode();
         setProductMixPanelSubheading();
         if (productMixAdapter == null) {
            productMixAdapter = new ProductMixAdapter(getActivity().getApplicationContext(), incomingProductMixList, (TabActivity) getActivity(), vipService, volumeMeasurementSystem,
                  massMeasurementSystem, this);
         }
         else {
            productMixAdapter.setItems(incomingProductMixList);
         }
         // Because android creates new views during onCreateView which is also called when the user switches the tab and
         // returns to this tab/fragment. The next lines have to be executed whether we create a new adapter or not.
         productMixListView.setAdapter(productMixAdapter);
         productMixSearch.setFilterable(productMixAdapter);
         productMixSearch.addTextChangedListener(new SearchInputTextWatcher(productMixSearch));
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
      if (incomingProductList != null) {
         productList = incomingProductList;
         isProductListDelivered = true;
         checkMode();
         setProductPanelSubheading();
         if (productListView != null && currentProduct != null && productListView.getSelectedItemId() != currentProduct.getId() && productListView.getSelectedId() > -1) {
            log.debug("Changing selection id. old: {}  new: {}", productListView.getSelectedItemId(), currentProduct.getId());
            currentProduct = productList.get((int) productListView.getSelectedId());
         }
         if (productAdapter == null) {
            productAdapter = new ProductAdapter(getActivity().getApplicationContext(), incomingProductList, (TabActivity) getActivity(), vipService, volumeMeasurementSystem,
                    massMeasurementSystem, this, productUnitsList, currentImplement);
         }
         else {
            productAdapter.setItems(productList);
         }
         if (productListView != null) {
            productListView.setAdapter(productAdapter);
         }
         // Because android creates new views during onCreateView which is also called when the user switches the tab and
         // returns to this tab/fragment. The next lines have to be executed whether we create a new adapter or not.
         productSearch.setFilterable(productAdapter);
         productSearch.addTextChangedListener(new SearchInputTextWatcher(productSearch));
         if (currentProduct == null && productList.size() > 0) {
            currentProduct = productList.get(0);
         }
         if (productComparator != null) {
            productAdapter.sort(productComparator, productSortAscending);
         }
      }
   }

   /**
    * Populate current implement
    */
   private synchronized void populateImplement() {
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
      log.debug("onCreate");

      if (savedInstanceState != null) {
         productList = savedInstanceState.getParcelableArrayList(PRODUCT_LIST);
         currentProduct = savedInstanceState.getParcelable(CURRENT_PRODUCT);
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
      log.debug("onActivityCreated");
   }

   /**
    * Resume operation
    * Register to backend services
    * Reset UI as needed
    */
   @Override
   public void onStart() {
      super.onStart();
      log.debug("onStart");

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
      log.debug("onResume called");

      // super on resume must be called before registerVIPService because this checks isResumed()
      super.onResume();
      registerVIPService();
   }

   private void registerVIPService() {
      // isResumed() needs to be checked if this is called via setVIPService() - if the method is not resumed objects may be null.
      if (vipService != null && isResumed()) {
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
         log.debug("cannot register vipListener - vipService == {}", vipService);
      }
   }


   @Override
   public void onPause() {
      log.debug("onPause called");
      super.onPause();
      unregisterVIPService();
   }

   /**
    * unregister VIPService
    */
   private void unregisterVIPService() {
      log.debug("unregister vip service {}", vipService);
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
      log.debug("onStop");
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
      log.debug("onSaveInstanceState");
      outState.putParcelable(CURRENT_PRODUCT, currentProduct);
      outState.putParcelableArrayList(PRODUCT_LIST, (ArrayList<Product>) productList);
      outState.putParcelableArrayList(PRODUCT_UNITS_LIST, (ArrayList<ProductUnits>) productUnitsList);
      // TODO: Workitem #4187: Determine, if instance state handling for varieties in necessary here?
   }

   /**
    * Set VIPService to listen to. Can be null if you want to unregister the current referenced service.
    * @param vipService the vipService
    */
   public void setVipService(@Nullable IVIPServiceAIDL vipService) {
      log.debug("setVIPService called with {}", vipService);
      // unregister the old one
      unregisterVIPService();
      this.vipService = vipService;
      registerVIPService();
      if (varietyAdapter != null){
         varietyAdapter.setVIPService(vipService);
      }
      if(addVarietyDialog != null){
         addVarietyDialog.setVIPService(vipService);
      }
      if (productAdapter != null){
         productAdapter.setVIPService(vipService);
      }
      if (productMixAdapter != null){
         productMixAdapter.setVIPService(vipService);
      }
   }

   @Override
   public void loadProductMix(ProductMix productMix) {
      vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST).sendToTarget();
   }

   /**
    * TextWatcher for SearchInputs to setClearIconEnabled state of the SearchInput
    */
   private class SearchInputTextWatcher implements TextWatcher {

      private final SearchInput searchInput;

      SearchInputTextWatcher(SearchInput searchInput){
         this.searchInput = searchInput;
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s) {
         if (s.toString().isEmpty()) {
            searchInput.setClearIconEnabled(false);
         }
         else {
            searchInput.setClearIconEnabled(true);
         }
      }
   }

   /**
    * Getter for the product mixes panel
    * @return the products mixes panel
    */
   public ProgressiveDisclosureView getProductMixesPanel() {
      return productMixesPanel;
   }

   /**
    * Getter for the products panel
    * @return the product panel
    */
   public ProgressiveDisclosureView getProductsPanel() {
      return productsPanel;
   }
}