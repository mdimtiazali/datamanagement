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
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.TextDialogView;
import com.cnh.android.pf.widget.utilities.EnumValueToUiStringUtility;
import com.cnh.android.pf.widget.utilities.MathUtility;
import com.cnh.android.pf.widget.utilities.MeasurementSystemCache;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.UnitUtility;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.android.pf.widget.utilities.commands.DeleteProductMixCommand;
import com.cnh.android.pf.widget.utilities.commands.ProductMixCommandParams;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.pf.widget.view.productdialogs.DialogActionType;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressiveDisclosureView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.android.data.management.productlibrary.utility.UiHelper;
import com.cnh.pf.android.data.management.productlibrary.views.ApplicationRateTableFactory;
import com.cnh.pf.android.data.management.productlibrary.views.ProductMixDialog;
import com.cnh.pf.api.pvip.IPVIPServiceAIDL;
import com.cnh.pf.model.product.library.MeasurementSystem;
import com.cnh.pf.model.product.library.Product;
import com.cnh.pf.model.product.library.ProductForm;
import com.cnh.pf.model.product.library.ProductMix;
import com.cnh.pf.model.product.library.ProductMixRecipe;
import com.cnh.pf.model.product.library.ProductUnits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for ProductMix-ListView.
 * @author waldschmidt (original coppied from joorjitham or helmig)
 */
public final class ProductMixAdapter extends SearchableSortableExpandableListAdapter<ProductMix> {

   private static final Logger log = LoggerFactory.getLogger(ProductMixAdapter.class);
   private final Context context;
   private final TabActivity activity;
   private IVIPServiceAIDL vipService;
   private IPVIPServiceAIDL pvipService;
   private final Drawable arrowCloseDetails;
   private final Drawable arrowOpenDetails;

   // the measurement systems could be loaded instead by the adapter itself via the vipservice
   private final ProductLibraryFragment productLibraryFragment;
   private final MeasurementSystemCache measurementSystemCache;

   /**
    * @param context the context of the adapter
    * @param productMixes the list of varieties which should be shown.
    * @param tabActivity the tabActivity
    * @param vipService the vipService
    * @param productLibraryFragment the productLibraryFragment this adapter is connected to
    * @param measurementSystemCache the cache used to get data about the current {@link MeasurementSystem}s
    */
   public ProductMixAdapter(final Context context, final List<ProductMix> productMixes, final TabActivity tabActivity, final IVIPServiceAIDL vipService, final IPVIPServiceAIDL pvipService,
         final ProductLibraryFragment productLibraryFragment, final MeasurementSystemCache measurementSystemCache) {
      super(productMixes);
      this.context = context;
      this.activity = tabActivity;
      this.vipService = vipService;
      this.pvipService = pvipService;
      arrowCloseDetails = context.getResources().getDrawable(R.drawable.arrow_down_expanded_productlist);
      arrowOpenDetails = context.getResources().getDrawable(R.drawable.arrow_up_expanded_productlist);
      this.productLibraryFragment = productLibraryFragment;
      this.measurementSystemCache = measurementSystemCache;
   }

   /**
    * Setter for the vipService.
    * @param vipService the vipService
    */
   public void setVIPService(final IVIPServiceAIDL vipService) {
      this.vipService = vipService;
   }
   /**
    * Setter for the pvipService.
    * @param pvipService the pvipService
    */
   public void setPVIPService(final IPVIPServiceAIDL pvipService) {
      this.pvipService = pvipService;
   }

   @Override
   public Filter getFilter() {
      Filter filter = super.getFilter();
      if (filter == null) {
         setFilter(new ProductMixFilter());
      }
      return super.getFilter();
   }

   /**
    * Add all ProductMixRecipes to the OverviewTable
    * @param tableLayout the tableLayout of the ApplicationRatesTable
    * @param productMix the data for the table
    */
   private void addProductsToTableLayout(TableLayout tableLayout, ProductMix productMix) {
      if (tableLayout != null && productMix != null) {
         int childCounter = tableLayout.getChildCount();
         for (int i = 1; i < childCounter; i++) {
            tableLayout.removeViewAt(1);
         }
         int viewCounter = 1;
         ProductMixRecipe carrierRecipe = productMix.getProductCarrier();
         Product carrierProduct = carrierRecipe.getProduct();
         double productMixDefaultRate = productMix.getProductMixParameters().getDefaultRate();
         double productMixRate2 = productMix.getProductMixParameters().getRate2();
         double productMixTotalAmount = productMix.getMixTotalAmount();
         ApplicationRateTableFactory.ApplicationRateTableData carrierProductApplicationRateTableData = createApplicationRateTableData(carrierRecipe, carrierProduct, productMixDefaultRate,
               productMixRate2, productMixTotalAmount);
         TableRow carrierTableRow = ApplicationRateTableFactory.createTableRowForProductMixAdapter(carrierProductApplicationRateTableData, context, tableLayout);
         if (carrierTableRow != null) {
            tableLayout.addView(carrierTableRow, viewCounter++);
         }

         for (ProductMixRecipe recipeElement : productMix.getRecipe()) {
            Product product = recipeElement.getProduct();
            ApplicationRateTableFactory.ApplicationRateTableData productApplicationRateTableData = createApplicationRateTableData(recipeElement, product, productMixDefaultRate, productMixRate2,
                  productMixTotalAmount);
            TableRow productTableRow = ApplicationRateTableFactory.createTableRowForProductMixAdapter(productApplicationRateTableData, context, tableLayout);
            if (productTableRow != null) {
               tableLayout.addView(productTableRow, viewCounter++);
            }
         }
      }
   }

   /**
    * Creates an {@link com.cnh.pf.android.data.management.productlibrary.views.ApplicationRateTableFactory.ApplicationRateTableData} for a ApplicationRateTableRow
    * @param recipe the recipe of the {@link ProductMix}
    * @param product the {@link Product} we create the row for
    * @param productMixDefaultRate the default rate of the product mix (not of the product)
    * @param productMixRate2 the rate2 of the product mix (not of the product)
    * @param productMixTotalAmount the total amount of the product mix (not of the product)
    * @return the ApplicationRateTableData needed to create the table row for the product
    */
   private ApplicationRateTableFactory.ApplicationRateTableData createApplicationRateTableData(ProductMixRecipe recipe, Product product, double productMixDefaultRate,
         double productMixRate2, double productMixTotalAmount) {
      ApplicationRateTableFactory.ApplicationRateTableData carrierProductApplicationRateTableData = new ApplicationRateTableFactory.ApplicationRateTableData();
      carrierProductApplicationRateTableData.productName = product.getName();
      carrierProductApplicationRateTableData.defaultRate = calculateApplicationRate(recipe.getAmount(), productMixDefaultRate, productMixTotalAmount);
      carrierProductApplicationRateTableData.rate2 = calculateApplicationRate(recipe.getAmount(), productMixRate2, productMixTotalAmount);
      ProductUnits productRateUnits = ProductHelperMethods.retrieveProductRateUnits(product,
            ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(product.getForm(), measurementSystemCache));
      carrierProductApplicationRateTableData.unit = productRateUnits.deepCopy();
      return carrierProductApplicationRateTableData;
   }

   /**
    * Calculate the application rate for a {@link Product} using the rate for the complete {@link ProductMix} and the amount of a single {@link Product} and the complete
    * {@link ProductMix}.
    * @param amount the amount of a single the product
    * @param productMixRate the rate of the product mix
    * @param totalAmount the amount of the product mix
    * @return the application rate of single the product
    */
   private double calculateApplicationRate(double amount, double productMixRate, double totalAmount) {
      double result = amount / totalAmount;
      return productMixRate * result;
   }

   private void initGroupView(View view, ProductMix productDetail, boolean expanded, ViewGroup root, View.OnClickListener listener) {
      ProductMixGroupHolder viewHolder;
      if (view == null) {
         view = LayoutInflater.from(context).inflate(R.layout.product_mix_item, root, false);
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
            viewHolder.formText.setText(EnumValueToUiStringUtility.getUiStringForProductForm(parameters.getForm(), context));
         }
         else {
            // maybe "unknown" would be better - but form should be never null ...
            log.error("productForm was null - this should never happen" );
            viewHolder.formText.setText(EnumValueToUiStringUtility.getUiStringForProductForm(ProductForm.ANY, context));
         }
         viewHolder.rateText.setText(UnitUtility.formatRateUnits(parameters, parameters.getDefaultRate(),
                 ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(parameters.getForm(), measurementSystemCache)));
      }
      viewHolder.groupIndicator.setImageDrawable(expanded ? arrowOpenDetails : arrowCloseDetails);
      view.setOnClickListener(listener);
   }

   @Override
   public View getGroupView(final int groupId, boolean expanded, View view, final ViewGroup viewGroup) {
      ProductMix productDetail = getGroup(groupId);
      if (productDetail != null) {
         if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.product_mix_item, viewGroup, false);
            if (view != null) {
               view.setTag(new ProductMixGroupHolder(view));
            }
         }
         initGroupView(view, productDetail, expanded, viewGroup, new View.OnClickListener() {
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
                  ProgressiveDisclosureView productMixPanel = productLibraryFragment.getProductMixesPanel();
                  if (productMixPanel != null) {
                     productMixPanel.resizeContent(false);
                  }
               }
            }
         });
         UiHelper.setAlternatingTableItemBackground(context, groupId, view);
      }
      return view;
   }

   private void initChildView(View view, final ProductMix productDetail, ViewGroup root, View.OnClickListener editButtonClickListener, View.OnClickListener copyButtonClickListener,
         View.OnClickListener deleteButtonClickListener, View.OnClickListener alertButtonClickListener) {
      final ProductMixChildHolder viewHolder;

      if (view == null) {
         view = LayoutInflater.from(context).inflate(R.layout.product_mix_item_child_details, root, false);
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
         viewHolder.appRate1Text.setText(UnitUtility.formatRateUnits(productMixParameter, productMixParameter.getDefaultRate(),
                 ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(productMixParameter.getForm(), measurementSystemCache)));
         viewHolder.appRate2Text.setText(UnitUtility.formatRateUnits(productMixParameter, productMixParameter.getRate2(),
                 ProductHelperMethods.queryApplicationRateMeasurementSystemForProductForm(productMixParameter.getForm(), measurementSystemCache)));
         addProductsToTableLayout(viewHolder.productRecipeTable, viewHolder.productMix);
      }
      viewHolder.alertIcon.setOnClickListener(alertButtonClickListener);
      viewHolder.editButton.setOnClickListener(editButtonClickListener);
      viewHolder.copyButton.setOnClickListener(copyButtonClickListener);
      viewHolder.deleteButton.setOnClickListener(deleteButtonClickListener);
   }

   @Override
   public View getChildView(final int group, int child, boolean expanded, View view, ViewGroup viewGroup) {
      if (view == null) {
         view = LayoutInflater.from(context).inflate(R.layout.product_mix_item_child_details, viewGroup, false);
         view.setTag(new ProductMixChildHolder(view));
      }
      final ProductMix productMixDetail = getChild(group, child);
      final Product parameters = productMixDetail.getProductMixParameters();
      final ProductMixChildHolder productMixChildHolder = (ProductMixChildHolder) view.getTag();
      initChildView(view, productMixDetail, viewGroup, new OnEditButtonClickListener(productMixDetail), new OnCopyButtonClickListener(productMixDetail),
            new OnDeleteButtonClickListener(parameters, productMixChildHolder, productMixDetail), new OnAlertButtonClickListener(parameters, productMixDetail));
      return view;
   }

   //Perhaps consider rewriting findViewById references below with dependency injection
   @Override
   public void notifyDataSetChanged() {
      //only update if fragment is attached [filter class is executing publishResults when activity is closed]
      if (productLibraryFragment.isAdded()) {
         super.notifyDataSetChanged();
         ProgressiveDisclosureView productMixPanel = productLibraryFragment.getProductMixesPanel();
         if (productMixPanel != null) {
            productMixPanel.invalidate();
         }
      }
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

   private class OnEditButtonClickListener implements View.OnClickListener {
      private final ProductMix productMixDetail;

      public OnEditButtonClickListener(ProductMix productMixDetail) {
         this.productMixDetail = productMixDetail;
      }

      @Override
      public void onClick(View view) {
         ProductMixDialog editProductMixDialog = new ProductMixDialog(context, DialogActionType.EDIT, vipService, pvipService, productMixDetail,
               productLibraryFragment, getCopyOfUnfilteredItemList());
         editProductMixDialog.setFirstButtonText(activity.getResources().getString(R.string.save))
               .setSecondButtonText(activity.getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
               .setTitle(activity.getResources().getString(R.string.product_mix_title_dialog_edit_product_mix)).setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT)
               .setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

         activity.showModalPopup(editProductMixDialog);

         editProductMixDialog.setContentPaddings(ProductLibraryFragment.LEFT_RIGHT_MARGIN, ProductLibraryFragment.TOP_BOTTOM_MARGIN, ProductLibraryFragment.LEFT_RIGHT_MARGIN,
               ProductLibraryFragment.TOP_BOTTOM_MARGIN);
         editProductMixDialog.disableButtonFirst(true);
         editProductMixDialog.setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);
         editProductMixDialog.setId(R.id.edit_product_mix_dialog);
      }
   }

   private class OnCopyButtonClickListener implements View.OnClickListener {
      private final ProductMix productMixDetail;

      public OnCopyButtonClickListener(ProductMix productMixDetail) {
         this.productMixDetail = productMixDetail;
      }

      @Override
      public void onClick(View view) {
         ProductMixDialog copyProductMixDialog = new ProductMixDialog(context, DialogActionType.COPY, vipService, pvipService, productMixDetail,
               productLibraryFragment, new ArrayList<ProductMix>(getCopyOfUnfilteredItemList()));
         copyProductMixDialog.setFirstButtonText(activity.getResources().getString(R.string.product_dialog_add_button))
               .setSecondButtonText(activity.getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
               .setTitle(activity.getResources().getString(R.string.product_mix_title_dialog_copy_product_mix)).setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT)
               .setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

         activity.showModalPopup(copyProductMixDialog);

         copyProductMixDialog.setContentPaddings(ProductLibraryFragment.LEFT_RIGHT_MARGIN, ProductLibraryFragment.TOP_BOTTOM_MARGIN, ProductLibraryFragment.LEFT_RIGHT_MARGIN,
               ProductLibraryFragment.TOP_BOTTOM_MARGIN);
         copyProductMixDialog.disableButtonFirst(true);
         copyProductMixDialog.setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);
         copyProductMixDialog.setId(R.id.copy_product_mix_dialog);
      }
   }

   private class OnDeleteButtonClickListener implements View.OnClickListener {
      private final Product parameters;
      private final ProductMixChildHolder productMixChildHolder;
      private final ProductMix productMixDetail;

      public OnDeleteButtonClickListener(Product parameters, ProductMixChildHolder productMixChildHolder, ProductMix productMixDetail) {
         this.parameters = parameters;
         this.productMixChildHolder = productMixChildHolder;
         this.productMixDetail = productMixDetail;
      }

      @Override
      public void onClick(View v) {
         if (productLibraryFragment.validateDeleteProduct(parameters)) {
            productMixChildHolder.alertIcon.setVisibility(View.GONE);
            final TextDialogView deleteDialog = new TextDialogView(context);
            deleteDialog.setBodyText(activity.getString(R.string.delete_product_dialog_body_text));
            deleteDialog.setFirstButtonText(activity.getString(R.string.delete_dialog_confirm_button_text));
            deleteDialog.setSecondButtonText(activity.getString(R.string.cancel));
            deleteDialog.showThirdButton(false);
            deleteDialog.setOnButtonClickListener(new DialogViewInterface.OnButtonClickListener() {
               @Override
               public void onButtonClick(DialogViewInterface dialogViewInterface, int buttonNumber) {
                  switch (buttonNumber) {
                  case DialogViewInterface.BUTTON_FIRST:
                     // TODO: delete this when pcm really deletes objects
                     ProductMixAdapter.this.removeItem(productMixDetail);
                     ProductMixCommandParams params = new ProductMixCommandParams();
                     params.productMix = productMixDetail;
                     params.vipService = vipService;
                     new VIPAsyncTask<ProductMixCommandParams, ProductMix>(params, null).execute(new DeleteProductMixCommand());
                     break;
                  }
                  deleteDialog.dismiss();
               }
            });
            activity.showModalPopup(deleteDialog);
         }
         else {
            productMixChildHolder.alertIcon.setVisibility(View.VISIBLE);
         }
      }
   }

   private class OnAlertButtonClickListener implements View.OnClickListener {
      private final Product parameters;
      private final ProductMix productMixDetail;

      public OnAlertButtonClickListener(Product parameters, ProductMix productMixDetail) {
         this.parameters = parameters;
         this.productMixDetail = productMixDetail;
      }

      @Override
      public void onClick(View v) {
         log.debug("Alert button pressed for product - name: {}, id: {}", parameters.getName(), productMixDetail.getId());
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
    * Filters products, providing search functionality
    * @author joorjitham
    */
   public class ProductMixFilter extends Filter implements UpdateableFilter {

      private CharSequence lastUsedCharSequence;

      @Override
      public void updateFiltering(){
         if (lastUsedCharSequence != null){
            filter(lastUsedCharSequence);
         }
      }

      @Override
      protected FilterResults performFiltering(CharSequence charSequence) {
         this.lastUsedCharSequence = charSequence;
         final FilterResults results = new FilterResults();
         final List<ProductMix> copyOfOriginalList;
         synchronized (listsLock) {
            isFiltered = true;
            if (originalList == null) {
               originalList = new ArrayList<ProductMix>(filteredList);
            }
            copyOfOriginalList = new ArrayList<ProductMix>(originalList);
         }
         if (charSequence == null || charSequence.length() == 0) {
            results.values = copyOfOriginalList;
            results.count = copyOfOriginalList.size();
         }
         else {
            final List<ProductMix> newProductMixList = new ArrayList<ProductMix>();
            MeasurementSystem measurementSystem = measurementSystemCache.queryMeasurementSystem(UnitsSettings.VOLUME);
            for (ProductMix mix : copyOfOriginalList) {
               Product p = mix.getProductMixParameters();
               final ProductUnits rateProductUnits = ProductHelperMethods.retrieveProductRateUnits(p, measurementSystem);
               double rateUnitFactor = 1.0;
               if (rateProductUnits != null && rateProductUnits.isSetMultiplyFactorFromBaseUnits()) {
                  rateUnitFactor = rateProductUnits.getMultiplyFactorFromBaseUnits();
               }
               String upperCaseCharSequence = charSequence.toString().toUpperCase();
               if (p.getName().toUpperCase().contains(upperCaseCharSequence)
                     || (p.getForm() != null && EnumValueToUiStringUtility.getUiStringForProductForm(p.getForm(), context).toUpperCase().contains(upperCaseCharSequence))
                     || (rateProductUnits != null && rateProductUnits.getName().toUpperCase().contains(upperCaseCharSequence))
                     || (String.valueOf(MathUtility.getConvertedFromBase(p.getDefaultRate(), rateUnitFactor))).contains(charSequence.toString())) {
                  newProductMixList.add(mix);
               }
            }
            results.values = newProductMixList;
            results.count = newProductMixList.size();
         }
         return results;
      }

      @Override
      protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
         synchronized (listsLock){
            if (!isFiltered){
               // ui thread changed something in parallel during perform filtering. So the list filtered is out-of-date now
               updateFiltering();
               return;
            } else {
               filteredList = (List<ProductMix>) filterResults.values;
               // Now we have to inform the adapter about the new list filtered
               if (filterResults.count == 0)
                  ProductMixAdapter.this.notifyDataSetInvalidated();
               else {
                  ProductMixAdapter.this.notifyDataSetChanged();
               }
            }
         }
      }
   }
}