/*
 * Copyright (C) 2017 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.productlibrary.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.TextDialogView;
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.android.pf.widget.utilities.MathUtility;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.UiUtility;
import com.cnh.android.pf.widget.utilities.UnitUtility;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.android.pf.widget.utilities.commands.DeleteProductCommand;
import com.cnh.android.pf.widget.utilities.commands.ProductCommandParams;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.pf.widget.view.ProductDialog;
import com.cnh.android.pf.widget.view.productdialogs.DialogActionType;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressiveDisclosureView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.android.data.management.productlibrary.utility.UiHelper;
import com.cnh.pf.model.product.library.CNHPlanterFanData;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductUnits;
import com.cnh.pf.model.vip.vehimp.Implement;
import com.cnh.pf.units.unit_constantsConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for Product-ListView.
 * @author waldschmidt (original coppied from joorjitham or helmig)
 */
public final class ProductAdapter extends SearchableSortableExpandableListAdapter<Product> {

   private static final Logger log = LoggerFactory.getLogger(ProductAdapter.class);
   private final Context context;
   private final TabActivity activity;
   private IVIPServiceAIDL vipService;
   private final Drawable arrowCloseDetails;
   private final Drawable arrowOpenDetails;
   private final String unitRpm;
   private final String unitInH2O;
   private List<ProductUnits> productUnits;
   private Implement currentImplement;

   // the measurement systems could be loaded instead by the adapter itself via the vipservice
   private MeasurementSystem volumeMeasurementSystem;
   private MeasurementSystem massMeasurementSystem;
   private final ProductLibraryFragment productLibraryFragment;

   public ProductAdapter(final Context context, final List<Product> products, final TabActivity tabActivity, final IVIPServiceAIDL vipService,
         final MeasurementSystem volumeMeasurementSystem, final MeasurementSystem massMeasurementSystem, final ProductLibraryFragment productLibraryFragment,
         final List<ProductUnits> productUnits, final Implement currentImplement) {
      super(products);
      this.context = context;
      this.activity = tabActivity;
      this.vipService = vipService;
      this.volumeMeasurementSystem = volumeMeasurementSystem;
      this.massMeasurementSystem = massMeasurementSystem;
      arrowCloseDetails = context.getResources().getDrawable(R.drawable.arrow_down_expanded_productlist);
      arrowOpenDetails = context.getResources().getDrawable(R.drawable.arrow_up_expanded_productlist);
      this.unitInH2O = context.getString(R.string.unit_in_h2o);
      this.unitRpm = context.getString(R.string.unit_rpm);
      this.productLibraryFragment = productLibraryFragment;
      this.productUnits = productUnits;
      this.currentImplement = currentImplement;
   }

   /**
    * Setter for the vipService.
    * @param vipService the vipService
    */
   public void setVIPService(final IVIPServiceAIDL vipService) {
      this.vipService = vipService;
   }

   /**
    * Setter for the list of all productUnits
    * @param productUnits the productUnitsList
    */
   public void setProductUnits(List<ProductUnits> productUnits) {
      this.productUnits = productUnits;
   }

   /**
    * Setter for the currentImplement
    * @param currentImplement the currentImplement
    */
   public void setCurrentImplement(Implement currentImplement) {
      this.currentImplement = currentImplement;
   }

   @Override
   public Filter getFilter() {
      Filter filter = super.getFilter();
      if (filter == null) {
         setFilter(new ProductFilter());
      }
      return super.getFilter();
   }

   private void initGroupView(View view, Product productDetail, boolean expanded, ViewGroup root, View.OnClickListener listener) {
      ProductGroupHolder viewHolder;
      if (view == null) {
         view = LayoutInflater.from(context).inflate(R.layout.product_item, root, false);
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
         viewHolder.formText.setText(EnumValueToUiStringUtility.getUiStringForProductForm(productDetail.getForm(), context));
      }
      else {
         // maybe "unknown" would be better - but form should be never null ...
         log.error("productForm was null - this should never happen" );
         viewHolder.formText.setText(EnumValueToUiStringUtility.getUiStringForProductForm(ProductForm.ANY, context));
      }
      viewHolder.rateText.setText(UnitUtility.formatRateUnits(productDetail, productDetail.getDefaultRate(),
              ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(productDetail.getForm(), context)));
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
         view = LayoutInflater.from(context).inflate(R.layout.product_item, viewGroup, false);
         view.setTag(new ProductGroupHolder(view));
      }
      Product productDetail = getGroup(position);

      initGroupView(view, productDetail, expanded, viewGroup, new View.OnClickListener() {
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
            productLibraryFragment.getProductsPanel().resizeContent(false);
         }
      });
      UiHelper.setAlternatingTableItemBackground(context, position, view);
      return view;
   }

   private void initChildView(View view, final Product productDetail, ViewGroup root, View.OnClickListener copyButtonClickListener, View.OnClickListener editButtonClickListener,
         View.OnClickListener deleteButtonClickListener, View.OnClickListener alertButtonClickListener) {
      final ProductChildHolder viewHolder;

      if (view == null) {
         view = LayoutInflater.from(context).inflate(R.layout.product_item_child_details, root, false);
      }

      if (view.getTag() == null) {
         viewHolder = new ProductChildHolder(view);
         viewHolder.productHasImplements = false;
         if (productLibraryFragment.validateDeleteProduct(productDetail)) {
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
         MeasurementSystem rateMeasurementSystem = ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(productDetail.getForm(), context);
         viewHolder.appRate1Text.setText(UnitUtility.formatRateUnits(productDetail, productDetail.getDefaultRate(), rateMeasurementSystem));
         viewHolder.appRate2Text.setText(UnitUtility.formatRateUnits(productDetail, productDetail.getRate2(), rateMeasurementSystem));
         viewHolder.deltaRateText.setText(UnitUtility.formatRateUnits(productDetail, productDetail.getDeltaRate(), rateMeasurementSystem));
         viewHolder.minRateText.setText(UnitUtility.formatRateUnits(productDetail, productDetail.getMinRate(), rateMeasurementSystem));
         viewHolder.maxRateText.setText(UnitUtility.formatRateUnits(productDetail, productDetail.getMaxRate(), rateMeasurementSystem));
         viewHolder.packageText.setText(UnitUtility.formatPackageUnits(productDetail, productDetail.getPackageSize(),
               ProductHelperMethods.queryPageSizeMeasurementSystemForProductForm(viewHolder.product.getForm(), context)));
         viewHolder.densityText
               .setText(UnitUtility.formatDensityUnits(productDetail, productDetail.getDensity(), UnitsSettings.queryMeasurementSystem(context, UnitsSettings.DENSITY)));

         // FIXME: System spec says: "is only shown if CNHPlanter with vacuum is detected" so this should work like ProductDialog#updateFanSettingsVisibility
         // https://polarion.cnhind.com/polarion/#/project/pfhmidevdefects/workitem?id=pfhmi-dev-defects-5536
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

         if (productDetail.getForm() == ProductForm.SEED || productDetail.getForm() == ProductForm.PLANT) {
            viewHolder.unitDensityText
                  .setText(UnitUtility.formatUnitDensityUnits(productDetail, productDetail.getUnitDensity(), UnitsSettings.queryMeasurementSystem(context, UnitsSettings.DENSITY)));
            viewHolder.unitDensityContainer.setVisibility(View.VISIBLE);
         }
         else {
            viewHolder.unitDensityContainer.setVisibility(View.INVISIBLE);
         }

         LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewHolder.imageRow.getLayoutParams();
         if (!productLibraryFragment.validateDeleteProduct(productDetail)) {
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
         view = LayoutInflater.from(context).inflate(R.layout.product_item_child_details, viewGroup, false);
         view.setTag(new ProductChildHolder(view));
      }
      final ProductChildHolder productChildHolder = (ProductChildHolder) view.getTag();
      final Product productDetail = getChild(group, child);
      if (productLibraryFragment.validateDeleteProduct(productDetail)) {
         productChildHolder.alertIcon.setVisibility(View.GONE);
      }
      else {
         productChildHolder.alertIcon.setVisibility(View.VISIBLE);
      }
      initChildView(view, productDetail, viewGroup, new OnCopyButtonClickListener(productChildHolder), new OnEditButtonClickListener(productDetail),
            new OnDeleteButtonClickListener(productDetail, productChildHolder), new OnAlertButtonClickListener(productDetail));
      return view;
   }

   //Perhaps consider rewriting findViewById references below with dependency injection
   @Override
   public void notifyDataSetChanged() {
      //only update if fragment is attached [filter class is executing publishResults when activity is closed]
      if (productLibraryFragment.isAdded()) {
         super.notifyDataSetChanged();
         ProgressiveDisclosureView productsPanel = productLibraryFragment.getProductsPanel();
         if (productsPanel != null) {
            productsPanel.invalidate();
         }
      }
   }

   private class OnCopyButtonClickListener implements View.OnClickListener {
      private final ProductChildHolder productChildHolder;

      OnCopyButtonClickListener(ProductChildHolder productChildHolder) {
         this.productChildHolder = productChildHolder;
      }

      @Override
      public void onClick(View v) {
         final ProductDialog copyDialog;
         copyDialog = new ProductDialog(context, vipService, DialogActionType.COPY, productChildHolder.product, ProductAdapter.this.productUnits,
               // TODO: try to remove this callback stuff and replace it with updates after a deliver in ProductLibraryFragment
               new ProductDialog.productListCallback() {
                  @Override
                  public void productList(Product product) {
                  }
               }, ProductAdapter.this.currentImplement, getCopyOfUnfilteredItemList());
         copyDialog.setFirstButtonText(context.getResources().getString(R.string.product_dialog_save_button))
               .setSecondButtonText(context.getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
               .setTitle(context.getResources().getString(R.string.product_dialog_copy_title)).setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT);
         copyDialog.setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);
         activity.showModalPopup(copyDialog);
      }
   }

   private class OnEditButtonClickListener implements View.OnClickListener {
      private final Product productDetail;

      OnEditButtonClickListener(Product productDetail) {
         this.productDetail = productDetail;
      }

      @Override
      public void onClick(View v) {
         log.debug("Edit button pressed for product - name: {}, id: {}", productDetail.getName(), productDetail.getId());
         ProductDialog editDialog = new ProductDialog(context, vipService, DialogActionType.EDIT, productDetail, ProductAdapter.this.productUnits,
               new ProductDialog.productListCallback() {
                  // TODO: try to remove this callback stuff and replace it with updates after a deliver in ProductLibraryFragment
                  @Override
                  public void productList(Product product) {
                  }
               }, ProductAdapter.this.currentImplement, getCopyOfUnfilteredItemList());
         editDialog.setFirstButtonText(context.getResources().getString(R.string.product_dialog_save_button))
               .setSecondButtonText(context.getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false)
               .setTitle(context.getResources().getString(R.string.product_dialog_edit_title)).setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT);
         editDialog.setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);
         activity.showModalPopup(editDialog);
      }
   }

   private class OnDeleteButtonClickListener implements View.OnClickListener {
      private final Product productToDelete;
      private final ProductChildHolder productChildHolder;

      OnDeleteButtonClickListener(Product productToDelete, ProductChildHolder productChildHolder) {
         this.productToDelete = productToDelete;
         this.productChildHolder = productChildHolder;
      }

      @Override
      public void onClick(View v) {
         if (productLibraryFragment.validateDeleteProduct(productToDelete)) {
            productChildHolder.alertIcon.setVisibility(View.GONE);
            final TextDialogView deleteDialog = new TextDialogView(context);
            deleteDialog.setBodyText(context.getResources().getString(R.string.delete_product_dialog_body_text));
            deleteDialog.setFirstButtonText(context.getResources().getString(R.string.delete_dialog_confirm_button_text));
            deleteDialog.setSecondButtonText(context.getResources().getString(R.string.cancel));
            deleteDialog.showThirdButton(false);
            deleteDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialogViewInterface, int buttonNumber) {
                  if (buttonNumber == DialogViewInterface.BUTTON_FIRST) {
                     deleteProduct();
                  }
                  deleteDialog.dismiss();
               }
            });
            activity.showModalPopup(deleteDialog);
         }
         else {
            productChildHolder.alertIcon.setVisibility(View.VISIBLE);
         }
      }

      private void deleteProduct() {
         log.debug("Delete button pressed for product - name: {}, id: {}", productToDelete.getName(), productToDelete.getId());
         // TODO: delete this when pcm really deletes objects
         ProductAdapter.this.removeItem(productToDelete);
         ProductCommandParams params = new ProductCommandParams();
         params.product = productToDelete;
         params.vipService = vipService;
         new VIPAsyncTask<ProductCommandParams, Product>(params, null).execute(new DeleteProductCommand());
      }
   }

   private class OnAlertButtonClickListener implements View.OnClickListener {
      private final Product productDetail;

      OnAlertButtonClickListener(Product productDetail) {
         this.productDetail = productDetail;
      }

      @Override
      public void onClick(View v) {
         log.debug("Alert button pressed for product - name: {}, id: {}", productDetail.getName(), productDetail.getId());
         new AlertDialog.Builder(activity).setTitle(R.string.alert_title).setMessage(R.string.alert_in_use)
               .setPositiveButton(R.string.alert_dismiss, new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                  }
               }).show();
      }
   }

   /**
    * ProductGroupHolder
    * Wraps the outer "collapsed" view of a product list item
    */
   private class ProductGroupHolder {
      ImageView groupIndicator;
      TextView nameText;
      TextView formText;
      TextView rateText;
      Product product;

      /**
       * Construct new product group holder
       * @param view
       */
      ProductGroupHolder(View view) {
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
      TextView appRate1Text;
      TextView appRate2Text;
      TextView minRateText;
      TextView maxRateText;
      TextView deltaRateText;
      TextView packageText;
      TextView densityText;
      TextView unitDensityText;
      ImageView alertIcon;
      ImageButton editButton;
      ImageButton copyButton;
      ImageButton deleteButton;
      Product product;
      LinearLayout imageRow;
      boolean productHasImplements;
      List<View> fanRateContainers = null;
      TextView vacuumFanRateText = null;
      TextView vacuumFanDeltaText = null;
      TextView bulkFillFanRateText = null;
      TextView bulkFillFanDeltaText = null;
      View unitDensityContainer;

      ProductChildHolder(View view) {
         this.alertIcon = (ImageButton) view.findViewById(R.id.alert_icon);
         this.appRate1Text = (TextView) view.findViewById(R.id.app_rate1_text);
         this.appRate2Text = (TextView) view.findViewById(R.id.app_rate2_text);
         this.minRateText = (TextView) view.findViewById(R.id.min_rate_text);
         this.maxRateText = (TextView) view.findViewById(R.id.max_rate_text);
         this.deltaRateText = (TextView) view.findViewById(R.id.delta_rate_text);
         this.packageText = (TextView) view.findViewById(R.id.package_text);
         this.densityText = (TextView) view.findViewById(R.id.density_text);
         this.unitDensityText = (TextView) view.findViewById(R.id.unit_density_text);
         this.copyButton = (ImageButton) view.findViewById(R.id.copy_button);
         this.editButton = (ImageButton) view.findViewById(R.id.edit_button);
         this.deleteButton = (ImageButton) view.findViewById(R.id.delete_button);
         this.imageRow = (LinearLayout) view.findViewById(R.id.linear_layout_image_row);

         this.fanRateContainers = new ArrayList<View>(4);
         this.fanRateContainers.add(view.findViewById(R.id.vacuum_fan_rate_container));
         this.fanRateContainers.add(view.findViewById(R.id.vacuum_fan_delta_container));
         this.fanRateContainers.add(view.findViewById(R.id.bulk_fill_fan_rate_container));
         this.fanRateContainers.add(view.findViewById(R.id.bulk_fill_fan_delta_container));

         this.vacuumFanRateText = ((TextView) view.findViewById(R.id.vacuum_fan_rate_text));
         this.vacuumFanDeltaText = ((TextView) view.findViewById(R.id.vacuum_fan_delta_text));
         this.bulkFillFanRateText = ((TextView) view.findViewById(R.id.bulk_fill_fan_rate_text));
         this.bulkFillFanDeltaText = ((TextView) view.findViewById(R.id.bulk_fill_fan_delta_text));

         this.unitDensityContainer = view.findViewById(R.id.unit_density_container);
      }

      void setFanUiVisibility(boolean visible) {
         for (View container : fanRateContainers) {
            UiUtility.setVisible(container, visible);
         }
      }
   }

   /**
    * Filters products, providing search functionality
    * @author joorjitham
    */
   private class ProductFilter extends Filter implements UpdateableFilter {

      private CharSequence lastUsedCharSequence;

      @Override
      public void updateFiltering() {
         if (lastUsedCharSequence != null) {
            filter(lastUsedCharSequence);
         }
      }

      @Override
      protected FilterResults performFiltering(CharSequence charSequence) {
         this.lastUsedCharSequence = charSequence;
         final FilterResults results = new FilterResults();
         final ArrayList<Product> copyOfOriginalList;
         synchronized (listsLock) {
            isFiltered = true;
            if (originalList == null) {
               originalList = new ArrayList<Product>(filteredList);
            }
            copyOfOriginalList = new ArrayList<Product>(originalList);
         }
         if (charSequence == null || charSequence.length() == 0) {
            results.values = copyOfOriginalList;
            results.count = copyOfOriginalList.size();
         }
         else {
            final List<Product> newProductList = new ArrayList<Product>();
            for (Product p : copyOfOriginalList) {
               final ProductUnits rateProductUnits = ProductHelperMethods.retrieveProductRateUnits(p, ProductHelperMethods.queryMeasurementSystem(context, UnitsSettings.VOLUME));
               double rateUnitFactor = 1.0;
               if (rateProductUnits != null && rateProductUnits.isSetMultiplyFactorFromBaseUnits()) {
                  rateUnitFactor = rateProductUnits.getMultiplyFactorFromBaseUnits();
               }
               String upperCaseCharSequence = charSequence.toString().toUpperCase();
               if (p.getName().toUpperCase().contains(upperCaseCharSequence)
                     || (p.getForm() != null && EnumValueToUiStringUtility.getUiStringForProductForm(p.getForm(), context).toUpperCase().contains(upperCaseCharSequence))
                     || (rateProductUnits != null && rateProductUnits.getName().toUpperCase().contains(upperCaseCharSequence))
                     || (String.valueOf(MathUtility.getConvertedFromBase(p.getDefaultRate(), rateUnitFactor))).contains(charSequence.toString())) {
                  newProductList.add(p);
               }
            }
            results.values = newProductList;
            results.count = newProductList.size();
         }
         return results;
      }

      @Override
      protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
         synchronized (listsLock) {
            if (!isFiltered) {
               // ui thread changed something in parallel during perform filtering
               return;
            }
            else {
               filteredList = (List<Product>) filterResults.values;
               // Now we have to inform the adapter about the new list filtered
               if (filterResults.count == 0)
                  ProductAdapter.this.notifyDataSetInvalidated();
               else {
                  ProductAdapter.this.notifyDataSetChanged();
               }
            }
         }
      }
   }
}
