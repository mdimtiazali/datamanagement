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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.cnh.android.dialog.DialogViewInterface;
import com.cnh.android.dialog.TextDialogView;
import com.cnh.android.pf.widget.utilities.MathUtility;
import com.cnh.android.pf.widget.utilities.ProductHelperMethods;
import com.cnh.android.pf.widget.utilities.UnitUtility;
import com.cnh.android.pf.widget.utilities.UnitsSettings;
import com.cnh.android.pf.widget.utilities.commands.DeleteProductMixCommand;
import com.cnh.android.pf.widget.utilities.commands.ProductMixCommandParams;
import com.cnh.android.pf.widget.utilities.tasks.VIPAsyncTask;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.ProgressiveDisclosureView;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.android.data.management.productlibrary.utility.SearchableSortableExpandableListAdapter;
import com.cnh.pf.android.data.management.productlibrary.utility.UiHelper;
import com.cnh.pf.android.data.management.productlibrary.views.ProductMixDialog;
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
 *
 */
public final class ProductMixAdapter extends SearchableSortableExpandableListAdapter<ProductMix> {

   private static final Logger log = LoggerFactory.getLogger(ProductMixAdapter.class);
   private final Context context;
   private final TabActivity activity;
   private IVIPServiceAIDL vipService;
   private final Drawable arrowCloseDetails;
   private final Drawable arrowOpenDetails;

   // the measurement systems could be loaded instead by the adapter itself via the vipservice
   private MeasurementSystem volumeMeasurementSystem;
   private MeasurementSystem massMeasurementSystem;
   private final ProductLibraryFragment productLibraryFragment;

   /**
    * @param context the context of the adapter
    * @param productMixes the list of varieties which should be shown.
    * @param tabActivity the tabActivity
    * @param vipService the vipService
    * @param massMeasurementSystem the measurementSystem for mass
    * @param volumeMeasurementSystem the measurementSystem for volume
    * @param productLibraryFragment the productLibraryFragment this adapter is connected to
    */
   public ProductMixAdapter(final Context context, final List<ProductMix> productMixes, final TabActivity tabActivity, final IVIPServiceAIDL vipService,
         MeasurementSystem volumeMeasurementSystem, MeasurementSystem massMeasurementSystem, ProductLibraryFragment productLibraryFragment) {
      super();
      setItems(productMixes);
      this.context = context;
      this.activity = tabActivity;
      this.vipService = vipService;
      this.volumeMeasurementSystem = volumeMeasurementSystem;
      this.massMeasurementSystem = massMeasurementSystem;
      arrowCloseDetails = activity.getResources().getDrawable(R.drawable.arrow_down_expanded_productlist);
      arrowOpenDetails = activity.getResources().getDrawable(R.drawable.arrow_up_expanded_productlist);
      this.productLibraryFragment = productLibraryFragment;
   }

   /**
    * Setter for the vipService.
    * @param vipService the vipService
    */
   public void setVIPService(final IVIPServiceAIDL vipService) {
      this.vipService = vipService;
   }

    @Override
    public Filter getFilter() {
        Filter filter = super.getFilter();
        if (filter == null) {
            setFilter(new ProductMixAdapter.ProductMixFilter(this, activity, getItems()));
        }
        return super.getFilter();
    }

   /**
    * Add all ProductMixRecipes to the OverviewTable
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
    * @param amount productMix amount
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
      TableRow tableRow = new TableRow(context);
      if (product != null) {
         ProductUnits unit = ProductHelperMethods.retrieveProductRateUnits(product,
               ProductHelperMethods.getMeasurementSystemForProduct(product, volumeMeasurementSystem, massMeasurementSystem));
         tableRow.setGravity(Gravity.CENTER);
         TextView productNameTextView = new TextView(context);
         productNameTextView.setText(" " + product.getName());
         productNameTextView.setTextSize(activity.getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
         productNameTextView.setTextColor(activity.getResources().getColor(R.color.defaultTextColor));
         productNameTextView.setTypeface(null, Typeface.BOLD);
         productNameTextView.setBackgroundResource(R.drawable.product_mix_dialog_application_rates_table_background_cell);
         tableRow.addView(productNameTextView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

         TextView applicationRate1TextView = new TextView(context);
         applicationRate1TextView.setTextSize(activity.getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
         applicationRate1TextView.setTextColor(activity.getResources().getColor(R.color.defaultTextColor));
         applicationRate1TextView.setTypeface(null, Typeface.BOLD);
         applicationRate1TextView.setBackgroundResource(R.drawable.product_mix_dialog_application_rates_table_background_cell);
         if (unit != null) {
            applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate() * unit.getMultiplyFactorFromBaseUnits(), unit.getName()));
         }
         else {
            applicationRate1TextView.setText(String.format(" %.2f %s", product.getDefaultRate(), ""));
         }
         tableRow.addView(applicationRate1TextView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

         TextView applicationRate2TextView = new TextView(context);
         applicationRate2TextView.setTextSize(activity.getResources().getDimensionPixelSize(R.dimen.product_mix_overview_application_rate_text_size));
         applicationRate2TextView.setTextColor(activity.getResources().getColor(R.color.defaultTextColor));
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
            viewHolder.formText.setText(ProductLibraryFragment.friendlyName(parameters.getForm().name()));
         }
         else {
            viewHolder.formText.setText(ProductLibraryFragment.friendlyName(ProductForm.LIQUID.name()));
         }
         viewHolder.rateText.setText(UnitUtility.formatRateUnits(parameters, parameters.getDefaultRate()));
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
         viewHolder.appRate1Text.setText(UnitUtility.formatRateUnits(productMixParameter, productDetail.getProductMixParameters().getDefaultRate()));
         viewHolder.appRate2Text.setText(UnitUtility.formatRateUnits(productMixParameter, productDetail.getProductMixParameters().getRate2()));
         addProductsToTableLayout(viewHolder.productRecipeTable, viewHolder.productMix);
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
         ProductMixDialog editProductMixDialog = new ProductMixDialog(context, ProductMixDialog.ProductMixesDialogActionType.EDIT, vipService, productMixDetail,
               productLibraryFragment, getItems());
         editProductMixDialog.setFirstButtonText(activity.getResources().getString(R.string.save))
               .setSecondButtonText(activity.getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
               .setTitle(activity.getResources().getString(R.string.product_mix_title_dialog_edit_product_mix)).setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT)
               .setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

         activity.showModalPopup(editProductMixDialog);

         editProductMixDialog.setContentPaddings(ProductLibraryFragment.LEFT_RIGHT_MARGIN, ProductLibraryFragment.TOP_BOTTOM_MARGIN, ProductLibraryFragment.LEFT_RIGHT_MARGIN,
               ProductLibraryFragment.TOP_BOTTOM_MARGIN);
         editProductMixDialog.disableButtonFirst(true);
         editProductMixDialog.setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);
      }
   }

   private class OnCopyButtonClickListener implements View.OnClickListener {
      private final ProductMix productMixDetail;

      public OnCopyButtonClickListener(ProductMix productMixDetail) {
         this.productMixDetail = productMixDetail;
      }

      @Override
      public void onClick(View view) {
         ProductMixDialog copyProductMixDialog = new ProductMixDialog(context, ProductMixDialog.ProductMixesDialogActionType.COPY, vipService, productMixDetail,
               productLibraryFragment, getItems());
         copyProductMixDialog.setFirstButtonText(activity.getResources().getString(R.string.product_dialog_add_button))
               .setSecondButtonText(activity.getResources().getString(R.string.product_dialog_cancel_button)).showThirdButton(false).showThirdButton(false)
               .setTitle(activity.getResources().getString(R.string.product_mix_title_dialog_copy_product_mix)).setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT)
               .setBodyHeight(ProductLibraryFragment.DIALOG_HEIGHT).setBodyView(R.layout.product_mix_dialog);

         activity.showModalPopup(copyProductMixDialog);

         copyProductMixDialog.setContentPaddings(ProductLibraryFragment.LEFT_RIGHT_MARGIN, ProductLibraryFragment.TOP_BOTTOM_MARGIN, ProductLibraryFragment.LEFT_RIGHT_MARGIN,
               ProductLibraryFragment.TOP_BOTTOM_MARGIN);
         copyProductMixDialog.disableButtonFirst(true);
         copyProductMixDialog.setDialogWidth(ProductLibraryFragment.DIALOG_WIDTH);
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
    public class ProductMixFilter extends Filter {

        private List<ProductMix> fullProductList;
        private Context context;
        private ProductMixAdapter productMixAdapter;

        public ProductMixFilter(ProductMixAdapter adapter, Context cntext, List<ProductMix> fullList) {
            super();
            productMixAdapter = adapter;
            context = cntext;
            fullProductList = fullList;
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults results = new FilterResults();
            if (charSequence == null || charSequence.length() == 0) {
                results.values = fullProductList;
                results.count = fullProductList.size();
            }
            else {
                List<ProductMix> productMixList = productMixAdapter.getItems();
                // We perform filtering operation
                List<ProductMix> nProductList = new ArrayList<ProductMix>();

                for (ProductMix mix : productMixList) {
                    Product p = mix.getProductMixParameters();
                    final ProductUnits rateProductUnits = ProductHelperMethods.retrieveProductRateUnits(p, ProductHelperMethods.queryMeasurementSystem(context, UnitsSettings.VOLUME));
                    double rateUnitFactor = 1.0;
                    if (rateProductUnits != null && rateProductUnits.isSetMultiplyFactorFromBaseUnits()) {
                        rateUnitFactor = rateProductUnits.getMultiplyFactorFromBaseUnits();
                    }

                    if (p.getName().toUpperCase().contains(charSequence.toString().toUpperCase())
                            || (p.getForm() != null && p.getForm().name().toUpperCase().contains(charSequence.toString().toUpperCase()))
                            || (rateProductUnits != null && rateProductUnits.getName().toUpperCase().contains(charSequence.toString().toUpperCase()))
                            || (String.valueOf(MathUtility.getConvertedFromBase(p.getDefaultRate(), rateUnitFactor))).contains(charSequence.toString())) {
                        nProductList.add(mix);
                    }
                }

                results.values = nProductList;
                results.count = nProductList.size();

            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            // Now we have to inform the adapter about the new list filtered
            if (filterResults.count == 0)
                productMixAdapter.notifyDataSetInvalidated();
            else {
                productMixAdapter.setItems((List<ProductMix>) filterResults.values);
                productMixAdapter.notifyDataSetChanged();
            }
        }
    }
}