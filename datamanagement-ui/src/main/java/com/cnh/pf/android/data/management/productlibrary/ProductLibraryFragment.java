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
import android.os.AsyncTask;
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

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.pf.widget.controls.SearchInput;
import com.cnh.android.pf.widget.utilities.MeasurementSystemCache;
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
import com.cnh.pf.api.pvip.IPVIPServiceAIDL;
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
import com.cnh.pf.model.vip.vehimp.ImplementModel;
import com.cnh.pf.model.vip.vehimp.ImplementType;
import com.cnh.pf.model.vip.vehimp.Operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import roboguice.fragment.provided.RoboFragment;

/**
 * ProductLibraryFragment
 * Supplies tab interface for products (and eventually product mixes)
 * Provides add/edit product functionality
 * Created by joorjitham on 3/27/2015.
 */
public class ProductLibraryFragment extends RoboFragment implements ProductMixCallBack, MeasurementSystemCache.MeasurementSystemListener {
   private static final Logger log = LoggerFactory.getLogger(ProductLibraryFragment.class);
   private static final String PRODUCT_LIST_SIZE_QUERY = "SELECT COUNT(*) FROM Product WHERE productMixId = 0";
   private static final String PRODUCT_MIX_LIST_SIZE_QUERY = "SELECT COUNT(*) FROM Product WHERE productMixId != 0";
   private static final String VARIETY_LIST_SIZE_QUERY = "SELECT COUNT(*) FROM Variety";
   private static final String PRODUCT_LIST = "product list";
   private static final String PRODUCT_UNITS_LIST = "product units list";
   private static final String CURRENT_PRODUCT = "current product";
   public static final int LEFT_RIGHT_MARGIN = 42;
   public static final int TOP_BOTTOM_MARGIN = 1;
   public static final int DIALOG_WIDTH = 817;
   public static final int DIALOG_HEIGHT = 400;
   private final String identifier = ProductLibraryFragment.class.getSimpleName() + System.identityHashCode(this);
   private boolean isProductMixListSizeDelivered = false;
   private boolean isProductListSizeDelivered = false;
   private boolean isVarietyListSizeDelivered = false;
   private View productLibraryLayout;
   protected DisabledOverlay disabledOverlay;
   private SearchInput productSearch;
   private SearchInput productMixSearch;
   private RelativeLayout productEmptyView;
   private RelativeLayout productMixEmptyView;
   private ProgressiveDisclosureView productsPanel;
   private DisabledOverlay productListDisabledOverlay;
   private DisabledOverlay productMixListDisabledOverlay;

   // Varieties
   private List<Variety> varietyList;
   private ProgressiveDisclosureView varietiesPanel;
   private SearchInput varietiesSearch;
   private ListView varietiesListView;
   private VarietyAdapter varietyAdapter;
   private AbstractVarietyComparator varietyComparator;
   private boolean varietySortAscending;
   private AddOrEditVarietyDialog addVarietyDialog;
   private RelativeLayout varietiesListEmptyView;
   private DisabledOverlay varietiesListDisabledOverlay;

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
   private MeasurementSystemCache measurementSystemCache;
   private boolean productSortAscending;
   private boolean productMixSortAscending;
   private volatile boolean pcmConnected = false;
   private static final int WHAT_LOAD_PRODUCT_LIST_SIZE = 1;
   private static final int WHAT_LOAD_PRODUCT_LIST = 2;
   private static final int WHAT_LIST = 3;
   private static final int WHAT_CONFIG = 4;
   private static final int WHAT_PING = 5;
   private static final int WHAT_IMPLEMENT = 6;
   private static final int WHAT_LOAD_PRODUCT_MIX_LIST = 7;
   private static final int WHAT_LOAD_UNIT_LIST = 8;
   private static final int WHAT_GET_VARIETY_LIST = 9;
   private static final int WHAT_LOAD_PRODUCT_MIX_LIST_SIZE = 10;
   private static final int WHAT_LOAD_VARIETY_LIST_SIZE = 11;

   private IVIPServiceAIDL vipService;
   private IPVIPServiceAIDL pvipService;
   private IVIPListenerAIDL vipListener = new SimpleVIPListener() {

      @Override
      public void onError(String error) throws RemoteException {
         log.error("Remote error: " + error);
      }

      @Override
      public void onTableChange(TableChangeEvent action, String tableName, String id) throws RemoteException {
         log.debug("OnTableChange - Action:{}, tableName:{}, id:{}", action, tableName, id);
      }

      @Override
      public void onServerConnect() throws RemoteException {
         onPCMConnectionEstablished();

         log.debug("onServerConnect called");
         pcmConnected = true;
         // get only some small amount of data to show something
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST_SIZE).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST_SIZE).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_VARIETY_LIST_SIZE).sendToTarget();

         // get the bigger amounts of data
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_UNIT_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_GET_VARIETY_LIST).sendToTarget();
         vipService.requestImplementProductConfigCurrent();
      }

      @Override
      public void onServerDisconnect() throws RemoteException {
         onPCMConnectionLoss();
      }

      @Override
      public void deliverImplementCurrent(final ImplementCurrent newImplement) throws RemoteException {
         log.debug("deliverImplementCurrent");
         ProductLibraryFragment.this.currentImplement = newImplement.getImplement();
         vipCommunicationHandler.obtainMessage(WHAT_IMPLEMENT, 1, 0, null).sendToTarget();
         if (productAdapter != null && newImplement != null) {
            productAdapter.setCurrentImplement(ProductLibraryFragment.this.currentImplement);
         }
      }

      @Override
      public void deliverProductUnitsList(final List<ProductUnits> productUnits) {
         productUnitsList = productUnits;
         if (productAdapter != null && productUnits != null) {
            productAdapter.setProductUnits(productUnits);
         }
         //TODO: partition units list out into pieces here.
      }

      @Override
      public void deliverProductList(final List<Product> products) {
         log.debug("deliverProductList");
         new AsyncTask<Void, Void, List<Product>>() {

            @Override
            protected List<Product> doInBackground(Void... voids) {
               List<Product> tempProductList = new ArrayList<Product>();
               for (Product product : products) {
                  if (product != null && product.getProductMixId() == 0) {
                     tempProductList.add(product);
                  }
               }
               return tempProductList;
            }

            @Override
            protected void onPostExecute(List<Product> products) {
               super.onPostExecute(products);
               populateProducts(products);
            }
         }.execute();
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

   private void onPCMConnectionLoss() {
      log.debug("onPCMConnectionLoss called");
      if (pcmConnected) {
         pcmConnected = false;
         updateDisconnectedOverlay(pcmConnected);
         vipCommunicationHandler.obtainMessage(WHAT_PING).sendToTarget();
      }
      else {
         log.error("Invalid Connection lifecycle! Tried to invoke onServerDisconnect twice without connection!");
      }
   }

   private void onPCMConnectionEstablished() {
      log.debug("onPCMConnectionEstablished called");
      if (!pcmConnected) {
         pcmConnected = true;
         updateDisconnectedOverlay(pcmConnected);
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_UNIT_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_LOAD_PRODUCT_MIX_LIST).sendToTarget();
         vipCommunicationHandler.obtainMessage(WHAT_GET_VARIETY_LIST).sendToTarget();
         try {
            vipService.requestImplementProductConfigCurrent();
         }
         catch (RemoteException e) {
            log.error("Could not request ImplementProductConfigCurrent: ", e);
         }
      }
      else {
         log.error("Invalid Connection lifecycle! Tried to invoke onServerConnect twice without disconnect!");
      }
   }

   private void checkMode() {
      if (isProductMixListSizeDelivered && isProductListSizeDelivered && isVarietyListSizeDelivered) {
         disabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
         disabledOverlay.setVisibility(View.GONE);
      }
   }

   private void updateDisconnectedOverlay(final boolean connected) {
      getActivity().runOnUiThread(new Runnable() {
         @Override
         public void run() {
            log.debug("updating Disconnected Overlay to {}", connected);
            if (connected) {
               //connected
               disabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
            }
            else {
               //disconnected
               disabledOverlay.setMode(DisabledOverlay.MODE.DISCONNECTED);
            }
         }
      });
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
                  if(isAdded()) {
                     populateProductMixes(productMixList);
                  }
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
                  if(isAdded()) {
                     populateVarieties(newVarietyList);
                  }
               }
            }).execute(new GetVarietyListCommand());
            break;
         case WHAT_LOAD_PRODUCT_LIST_SIZE:
            log.debug("loading size of product list");
            new LoadListSizeAsyncTask(PRODUCT_LIST_SIZE_QUERY, Product.class.getName()) {

               @Override
               protected void onPostExecute(Long size) {
                  super.onPostExecute(size);
                  if (size > Integer.MAX_VALUE) {
                     log.error("number of products exceed the maximum this view can handle");
                     return;
                  }
                  populateProducts(size.intValue());
               }
            }.execute();
            break;
         case WHAT_LOAD_PRODUCT_MIX_LIST_SIZE:
            log.debug("loading size of product mix list");
            new LoadListSizeAsyncTask(PRODUCT_MIX_LIST_SIZE_QUERY, ProductMix.class.getName()) {

               @Override
               protected void onPostExecute(Long size) {
                  super.onPostExecute(size);
                  if (size > Integer.MAX_VALUE) {
                     log.error("number of products mixes exceed the maximum this view can handle");
                     return;
                  }
                  populateProductMixes(size.intValue());
               }
            }.execute();
            break;
         case WHAT_LOAD_VARIETY_LIST_SIZE:
            log.debug("loading size of variety list");
            new LoadListSizeAsyncTask(VARIETY_LIST_SIZE_QUERY, Variety.class.getName()) {

               @Override
               protected void onPostExecute(Long size) {
                  super.onPostExecute(size);
                  if (size > Integer.MAX_VALUE) {
                     log.error("number of varieties exceed the maximum this view can handle");
                     return;
                  }
                  populateVarieties(size.intValue());
               }
            }.execute();
            break;
         default:
            log.info("Unexpected default case in vip handler");
            break;
         }
      }
   };

   /**
    * Inflate and return the default product library view
    *
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
      productListDisabledOverlay = (DisabledOverlay) productsPanel.findViewById(R.id.products_overlay);
      productListDisabledOverlay.setMode(DisabledOverlay.MODE.LOADING);

      productMixesPanel = (ProgressiveDisclosureView) productLibraryLayout.findViewById(R.id.product_mix_panel);
      productMixesPanel.setAutoResizable(true);
      productMixSearch = (SearchInput) productMixesPanel.findViewById(R.id.product_mix_search);
      productMixSearch.setTextSize(getResources().getDimension(R.dimen.search_text_size));
      productMixEmptyView = (RelativeLayout) productMixesPanel.findViewById(R.id.product_mix_empty);
      productMixListView = (ExpandableListView) productMixesPanel.findViewById(R.id.product_mix_list);
      productMixListDisabledOverlay = (DisabledOverlay) productMixesPanel.findViewById(R.id.products_mixes_overlay);
      productMixListDisabledOverlay.setMode(DisabledOverlay.MODE.LOADING);

      varietiesPanel = (ProgressiveDisclosureView) productLibraryLayout.findViewById(R.id.variety_panel);
      varietiesPanel.setAutoResizable(true);
      varietiesSearch = (SearchInput) varietiesPanel.findViewById(R.id.variety_search);
      varietiesSearch.setTextSize(getResources().getDimension(R.dimen.search_text_size));
      varietiesListEmptyView = (RelativeLayout) varietiesPanel.findViewById(R.id.no_available_varieties);
      varietiesListView = (ListView) varietiesPanel.findViewById(R.id.varieties_list);
      varietiesListDisabledOverlay = (DisabledOverlay) varietiesPanel.findViewById(R.id.varieties_overlay);
      varietiesListDisabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);

      Button btnAddProduct = (Button) productsPanel.findViewById(R.id.add_product_button);
      btnAddProduct.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View view) {
            ProductDialog addProductDialog = new ProductDialog(getActivity().getApplicationContext(), vipService, pvipService, productUnitsList, new ProductDialog.productListCallback() {
               @Override
               public void productList(Product product) {
                  productList.add(product);
                  // TODO: directly before the save we need to change to loading - but currently this method gets called after the save.
                  // If we do this we can wait for the next deliver. We need to care about the deliverProductList which may disable loading too early ...
               }
            }, ProductLibraryFragment.this.currentImplement, productList);

            addProductDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_save_button))
                  .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false)
                  .setTitle(getResources().getString(R.string.product_dialog_add_tile)).setBodyHeight(DIALOG_HEIGHT);

            addProductDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
            addProductDialog.setDialogWidth(DIALOG_WIDTH);

            final TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addProductDialog);
         }
      });
      Button btnAddProductMix = (Button) productMixesPanel.findViewById(R.id.add_mix_button);
      btnAddProductMix.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            ProductMixDialog addProductMixDialog = new ProductMixDialog(getActivity().getApplicationContext(), vipService, pvipService, ProductLibraryFragment.this, productMixList);
            addProductMixDialog.setFirstButtonText(getResources().getString(R.string.product_dialog_add_button))
                  .setSecondButtonText(getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false)
                  .setTitle(getResources().getString(R.string.product_mix_title_dialog_add_product_mix)).setBodyHeight(DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

            addProductMixDialog.setContentPaddings(LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN, LEFT_RIGHT_MARGIN, TOP_BOTTOM_MARGIN);
            addProductMixDialog.disableButtonFirst(true);
            addProductMixDialog.setDialogWidth(DIALOG_WIDTH);
            addProductMixDialog.setId(R.id.add_product_mix_dialog);

            final TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addProductMixDialog);
         }
      });
      Button btnAddVariety = (Button) varietiesPanel.findViewById(R.id.variety_button_add);
      btnAddVariety.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            addVarietyDialog = new AddOrEditVarietyDialog(getActivity().getApplicationContext());
            addVarietyDialog.setFirstButtonText(getResources().getString(R.string.variety_dialog_save_button_text))
                  .setSecondButtonText(getResources().getString(R.string.variety_dialog_cancel_button_text))
                  .setTitle(getResources().getString(R.string.variety_add_dialog_title_text))
                  .setOnDismissListener(new DialogViewInterface.OnDismissListener() {
                     @Override
                     public void onDismiss(DialogViewInterface dialogViewInterface) {
                        ProductLibraryFragment.this.addVarietyDialog = null;
                     }
                  });
            addVarietyDialog.setActionType(AddOrEditVarietyDialog.VarietyDialogActionType.ADD);
            if (varietyList != null) {
               addVarietyDialog.setVarietyList(varietyList);
            }
            final TabActivity useModal = (DataManagementActivity) getActivity();
            useModal.showModalPopup(addVarietyDialog);
            if (addVarietyDialog != null) {
               addVarietyDialog.setVIPService(vipService);
            }
         }
      });
      return productLibraryLayout;
   }

   private void setProductPanelSubheading(int size) {
      productsPanel.setSubheading(getResources().getQuantityString(R.plurals.product_section_subheader_format, size, size));
   }

   private void setProductMixPanelSubheading(int size) {
      productMixesPanel.setSubheading(getResources().getQuantityString(R.plurals.product_mix_subheader_total_mix_format, size, size));
   }

   private void setVarietyPanelSubheading(int size) {
      varietiesPanel.setSubheading(getResources().getQuantityString(R.plurals.variety_section_subheader_format, size, size));
   }

   //TODO: Current Validate only Product not Product mixes
   public boolean validateDeleteProduct(Product product) {
      if (controllerProductConfigurationList == null || controllerProductConfigurationList.isEmpty() || currentImplement == null || currentImplement.getControllers() == null) {
         return true;
      }
      for (ControllerProductConfiguration controllerProductConfiguration : controllerProductConfigurationList) {
         // TODO: this will be null here if objects came from HQLQuery, but delete is a feature of rc15
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
      final Map<Class, ListHeaderSortView> sortButtons = new HashMap<Class, ListHeaderSortView>();
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
                  productMixComparator = new ProductMixNameSortComparator(getActivity());
                  break;
               case R.id.product_mix_header_product_form_sort:
                  productMixComparator = new ProductMixFormSortComparator(getActivity());
                  break;
               case R.id.product_mix_header_product_default_rate_sort:
                  productMixComparator = new ProductMixDefaultRateSortComparator(getActivity());
                  break;
               default:
                  productMixComparator = new ProductMixNameSortComparator(getActivity());
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
      productMixComparator = new ProductMixNameSortComparator(getActivity());
      productMixSortAscending = true;
      if (productMixAdapter != null) {
         productMixAdapter.sort(productMixComparator, productMixSortAscending);
      }
      ListHeaderSortView currentSortButton = sortButtons.get(ProductMixNameSortComparator.class);
      currentSortButton.setState(ListHeaderSortView.STATE_SORT_ASC);
   }

   private void initProductSortHeader() {
      final Map<Class, ListHeaderSortView> sortButtons = new HashMap<Class, ListHeaderSortView>();
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
               productComparator = new NameSortComparator(getActivity());
               break;
            case R.id.header_form:
               productComparator = new FormSortComparator(getActivity());
               break;
            case R.id.header_default_rate:
               productComparator = new DefaultRateSortComparator(getActivity());
               break;
            default:
               productComparator = new NameSortComparator(getActivity());
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
      productComparator = new NameSortComparator(getActivity());
      productSortAscending = true;
      if (productAdapter != null) {
         productAdapter.sort(productComparator, productSortAscending);
      }
      ListHeaderSortView currentSortButton = sortButtons.get(NameSortComparator.class);
      currentSortButton.setState(ListHeaderSortView.STATE_SORT_ASC);
   }

   private void initVarietySortHeader() {
      final Map<Class, ListHeaderSortView> sortButtons = new HashMap<Class, ListHeaderSortView>();
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
    *
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
      setProductPanelSubheading(productList == null ? 0 : productList.size());
      initProductSortHeader();
      setProductMixPanelSubheading(productMixList == null ? 0 : productMixList.size());
      initProductMixSortHeader();
      setVarietyPanelSubheading(varietyList == null ? 0 : varietyList.size());
      initVarietySortHeader();
   }

   private void populateVarieties(int size) {
      isVarietyListSizeDelivered = true;
      checkMode();
      setVarietyPanelSubheading(size);
   }

   /**
    * Populates the varieties to UI. Must be called from UI-Thread.
    *
    * @param varietyList the list of varieties to populate
    */
   private void populateVarieties(List<Variety> varietyList) {
      if (varietyList != null) {
         this.varietyList = new ArrayList<Variety>(varietyList);
         populateVarieties(varietyList.size());
         if (varietyAdapter == null) {
            varietyAdapter = new VarietyAdapter(getActivity().getApplicationContext(), varietyList, (TabActivity) getActivity(), vipService);
            varietiesSearch.setFilterable(varietyAdapter);
            if (varietyComparator != null) {
               varietyAdapter.sort(varietyComparator, varietySortAscending);
            }
         }
         else {
            varietiesSearch.setFilterable(varietyAdapter);
            varietyAdapter.setVarietyList(varietyList);
         }
         // Because android creates new views during onCreateView which is also called when the user switches the tab and
         // returns to this tab/fragment. The next lines have to be executed whether we create a new adapter or not.
         varietiesListView.setAdapter(varietyAdapter);
         varietiesSearch.addTextChangedListener(new SearchInputTextWatcher(varietiesSearch));
         if (varietiesPanel.isExpanded()) {
            varietiesPanel.resizeContent(false);
         }
         varietiesListDisabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
      }
   }

   private void populateProductMixes(int size) {
      isProductMixListSizeDelivered = true;
      checkMode();
      setProductMixPanelSubheading(size);
   }

   private void populateProductMixes(List<ProductMix> incomingProductMixList) {
      if (incomingProductMixList != null) {
         this.productMixList = new ArrayList<ProductMix>(incomingProductMixList);
         populateProductMixes(productMixList.size());
         if (productMixAdapter == null) {
            productMixAdapter = new ProductMixAdapter(getActivity().getApplicationContext(), incomingProductMixList, (TabActivity) getActivity(), vipService, pvipService,
                  this, measurementSystemCache);
            productMixSearch.setFilterable(productMixAdapter);
            if (productMixComparator != null) {
               productMixAdapter.sort(productMixComparator, productMixSortAscending);
            }
         }
         else {
            productMixSearch.setFilterable(productMixAdapter);
            productMixAdapter.setItems(incomingProductMixList);
         }
         // Because android creates new views during onCreateView which is also called when the user switches the tab and
         // returns to this tab/fragment. The next lines have to be executed whether we create a new adapter or not.
         productMixListView.setAdapter(productMixAdapter);
         productMixSearch.addTextChangedListener(new SearchInputTextWatcher(productMixSearch));
         if (productMixesPanel.isExpanded()) {
            productMixesPanel.resizeContent(false);
         }
         if (productList != null) { // this is needed for the product mix dialog - no extra check will be done to enable the edit + add button
            productMixListDisabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
         }
      }
   }

   private void populateProducts(int size) {
      isProductListSizeDelivered = true;
      checkMode();
      setProductPanelSubheading(size);
   }

   /**
    * Populate current product list
    */
   private void populateProducts(List<Product> incomingProductList) {
      if (incomingProductList != null) {
         productList = incomingProductList;
         populateProducts(productList.size());
         if (productListView != null && currentProduct != null && productListView.getSelectedItemId() != currentProduct.getId() && productListView.getSelectedId() > -1) {
            log.debug("Changing selection id. old: {}  new: {}", productListView.getSelectedItemId(), currentProduct.getId());
            currentProduct = productList.get((int) productListView.getSelectedId());
         }
         if (productAdapter == null) {
            productAdapter = new ProductAdapter(getActivity().getApplicationContext(), incomingProductList, (TabActivity) getActivity(), vipService, pvipService,
                  this, productUnitsList, currentImplement, measurementSystemCache);
            productSearch.setFilterable(productAdapter);
            if (productComparator != null) {
               productAdapter.sort(productComparator, productSortAscending);
            }
         }
         else {
            productSearch.setFilterable(productAdapter);
            productAdapter.setItems(productList);
         }
         // Because android creates new views during onCreateView which is also called when the user switches the tab and
         // returns to this tab/fragment. The next lines have to be executed whether we create a new adapter or not.
         if (productListView != null) {
            productListView.setAdapter(productAdapter);
         }
         productSearch.addTextChangedListener(new SearchInputTextWatcher(productSearch));
         if (currentProduct == null && productList.size() > 0) {
            currentProduct = productList.get(0);
         }
         productListDisabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
         if (productMixList != null) { // if product mix list was completed before but product list was missing product mix overlay needs to be hidden.
            productMixListDisabledOverlay.setMode(DisabledOverlay.MODE.HIDDEN);
         }
      }
   }

   /**
    * Populate current implement. Must be called from UI-Thread
    */
   private void populateImplement() {
      if (getActivity() != null && getActivity() instanceof DataManagementActivity && currentImplement != null && currentImplement.getName() != null) {
         ((DataManagementActivity) getActivity()).setTabActivitySubheaderRight(currentImplement.getName());
      }
   }

   /**
    * Restore UI from saved state
    *
    * @param savedInstanceState
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      log.debug("onCreate");
   }

   /**
    * Initialize adapters
    *
    * @param savedInstanceState
    */
   @Override
   public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      log.debug("onActivityCreated");
      List<String> measurementSystemParameter = new ArrayList<String>(4);
      measurementSystemParameter.add(UnitsSettings.VOLUME);
      measurementSystemParameter.add(UnitsSettings.MASS);
      measurementSystemParameter.add(UnitsSettings.OTHER);
      measurementSystemParameter.add(UnitsSettings.DENSITY);
      measurementSystemCache = new MeasurementSystemCache(measurementSystemParameter, getActivity(), this, false);
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
      updateDisconnectedOverlay(pcmConnected);
      // super on resume must be called before registerVIPService because this checks isResumed()
      super.onResume();
      measurementSystemCache.registerContentObservers();
      registerVIPService();
   }

   @Override
   public void onMeasurementSystemChanged(String measurementParameterName, MeasurementSystem measurementSystem) {
      if (productAdapter != null) {
         productAdapter.notifyDataSetChanged();
      }
   }


   private void registerVIPService() {
      // isResumed() needs to be checked if this is called via setVIPService() - if the method is not resumed objects may be null.
      if (vipService != null && isResumed()) {
         GetLightWeightImplementCurrentAsyncTask getLightWeightImplementCurrentAsyncTask = new GetLightWeightImplementCurrentAsyncTask();
         getLightWeightImplementCurrentAsyncTask.execute();
      }
      else {
         log.debug("cannot register vipListener - vipService == {}", vipService);
      }
   }

   /**
    * AsyncTask to get a light weight vehicle current which includes only parts of  the {@link ImplementCurrent}s data.
    */
   private final class GetLightWeightImplementCurrentAsyncTask extends AsyncTask<Void, Void, List> {

      // TODO: hint for rc15 when deleting products should be implemented - maybe controllers are needed than additionally
      //   private static String QUERY =
      //           "SELECT imp.name, type.operation, controller.id, controller.protocol " +
      //                   "FROM Implement imp " +
      //                   "LEFT OUTER JOIN imp.implementModel AS model " +
      //                   "LEFT OUTER JOIN imp.controllers AS controller " +
      //                   "LEFT OUTER JOIN model.implementType as type " +
      //                   "WHERE imp.id = (SELECT implement.id FROM ImplementCurrent ic WHERE ic.id = 1)";

      private final String IMPLEMENT_NAME_AND_TYPE_OP_QUERY = "SELECT imp.id, imp.name, type.operation FROM Implement imp " + "LEFT OUTER JOIN imp.implementModel AS model "
            + "LEFT OUTER JOIN model.implementType AS type " + "WHERE imp.id = (SELECT implement.id FROM ImplementCurrent ic WHERE ic.id = 1)";

      @Override
      protected void onPreExecute() {
         super.onPreExecute();
      }

      @Override
      protected List doInBackground(Void... nothing) {
         log.debug("starting thread for vipServiceListener registration");
         try {
            vipService.register(identifier, vipListener);
            // This is only used to check if a product can be deleted. Needs to be proved if this is necessary during
            // implementing deletion (planned in RC15).
            vipService.requestImplementProductConfigCurrent();

            final List implementNameAndTypeOp = vipService.genericQuery(IMPLEMENT_NAME_AND_TYPE_OP_QUERY);
            return implementNameAndTypeOp;
         }
         catch (RemoteException e) {
            log.error("vipService register failed: ", e);
         }
         return null;
      }

      @Override
      protected void onPostExecute(List list) {
         super.onPostExecute(list);
         if (list != null) {
            if (list.size() > 1) {
               log.warn("got more data then expected");
            }
            if (list.size() >= 1) {
               Object[] singleDatabaseLine = (Object[]) list.get(0);
               int implementId = (Integer) singleDatabaseLine[0];
               String implementName = (String) singleDatabaseLine[1];
               Operation implementTypeOperation = (Operation) singleDatabaseLine[2];
               log.debug("got implement id: {}, name: {},  and type: {} from pcm", implementId, implementName, implementTypeOperation);
               try {
                  vipListener.deliverImplementCurrent(createCurrentImplement(implementId, implementName, implementTypeOperation));
               }
               catch (RemoteException e) {
                  log.error("this should never happen because method was called locally");
               }
            }
         }
      }

      /**
       * Factory method to create the current implement with everything we need inside (so this objects contain only the given values)
       *
       * @param implementId
       * @param implementName
       * @param implementTypeOperation
       * @return the new implementCurrent with only the values gives as parameters
       */
      private ImplementCurrent createCurrentImplement(int implementId, String implementName, Operation implementTypeOperation) {
         Implement implement = new Implement();
         implement.setName(implementName);
         implement.setId(implementId);
         ImplementModel implementModel = new ImplementModel();
         ImplementType implementType = new ImplementType();
         implementType.setOperation(implementTypeOperation);
         implementModel.setImplementType(implementType);
         implement.setImplementModel(implementModel);
         ImplementCurrent implementCurrent = new ImplementCurrent();
         implementCurrent.setImplement(implement);
         return implementCurrent;
      }
   }

   ;

   @Override
   public void onPause() {
      log.debug("onPause called");
      super.onPause();
      unregisterVIPService();
      measurementSystemCache.unregisterContentObservers();
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
    * Set VIPService to listen to. Can be null if you want to unregister the current referenced service.
    *
    * @param vipService the vipService
    */
   public void setVipService(@Nullable IVIPServiceAIDL vipService) {
      log.debug("setVIPService called with {}", vipService);
      // unregister the old one
      unregisterVIPService();
      this.vipService = vipService;
      registerVIPService();
      if (varietyAdapter != null) {
         varietyAdapter.setVIPService(vipService);
      }
      if (addVarietyDialog != null) {
         addVarietyDialog.setVIPService(vipService);
      }
      if (productAdapter != null) {
         productAdapter.setVIPService(vipService);
      }
      if (productMixAdapter != null) {
         productMixAdapter.setVIPService(vipService);
      }
      if (this.vipService == null) {
         //vipService is disconnected
         if (pcmConnected) {
            //invoke disconnect action
            onPCMConnectionLoss();
            log.debug("invoked onPCMConnectionLoss!");
         }
      }
   }
   /**
    * Set pvipService to get pvip data. Can be null if you want to unregister the current referenced service.
    * @param pvipService the vipService
    */
   public void setPvipService(@Nullable IPVIPServiceAIDL pvipService) {
      log.debug("setPvipService called with {}", pvipService);
      // unregister the old one
      this.pvipService = pvipService;
      if (productMixAdapter != null){
         productMixAdapter.setPVIPService(pvipService);
      }
      if (productAdapter != null){
         productAdapter.setPVIPService(pvipService);
      }
   }

   @Override
   public void loadProductMix(ProductMix productMix) {
      // TODO: directly before the save we need to change to loading - but currently this method gets called after the save.
      // if we do this we can wait for the next deliver
      // we need to care about the deliverProductList which may disable loading too early ...
      // productMixListDisabledOverlay.setMode(DisabledOverlay.MODE.LOADING);;
   }

   /**
    * TextWatcher for SearchInputs to setClearIconEnabled state of the SearchInput
    */
   private class SearchInputTextWatcher implements TextWatcher {

      private final SearchInput searchInput;

      SearchInputTextWatcher(SearchInput searchInput) {
         this.searchInput = searchInput;
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

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
    *
    * @return the products mixes panel
    */
   public ProgressiveDisclosureView getProductMixesPanel() {
      return productMixesPanel;
   }

   /**
    * Getter for the products panel
    *
    * @return the product panel
    */
   public ProgressiveDisclosureView getProductsPanel() {
      return productsPanel;
   }

   private class LoadListSizeAsyncTask extends AsyncTask<Void, Void, Long> {

      private String listSizeQuery;
      private String type;

      LoadListSizeAsyncTask(String listSizeQuery, String type) {
         this.listSizeQuery = listSizeQuery;
         this.type = type;
      }

      @Override
      protected Long doInBackground(Void... params) {
         Long size = 0l;
         try {
            List list = vipService.genericQuery(listSizeQuery);
            if (list != null) {
               if (list.size() > 1) {
                  log.warn("got more data then expected");
               }
               if (list.size() >= 1) {
                  size = (Long) list.get(0);
                  log.debug("got {} {}s", size, type);
               }
            }
         }
         catch (RemoteException e) {
            log.error("failed to load number of {}s", type, e);
         }
         return size;
      }
   }
}
