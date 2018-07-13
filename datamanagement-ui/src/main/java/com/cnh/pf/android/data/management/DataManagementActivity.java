/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.view.View;

import com.cnh.android.pf.libwidgetutil.listener.ConnectedVipListener;
import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.android.util.prefs.GlobalPreferencesNotAvailableException;
import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.android.widget.activity.TabActivity;
import com.cnh.android.widget.control.TabActivityListeners;
import com.cnh.android.widget.control.TabActivityTab;
import com.cnh.pf.android.data.management.helper.VIPDataHandler;
import com.cnh.pf.android.data.management.productlibrary.ProductLibraryFragment;
import com.cnh.pf.android.data.management.service.DataManagementService;
import com.cnh.pf.android.data.management.session.SessionManager;
import com.cnh.pf.android.data.management.utility.UtilityHelper;
import com.cnh.pf.api.pvip.ConnectedPVIPListener;
import com.cnh.pf.api.pvip.IPVIPServiceAIDL;
import com.cnh.pf.data.management.service.ServiceConstants;
import com.cnh.pf.jgroups.ChannelModule;
import com.cnh.pf.model.TableChangeEvent;
import com.cnh.pf.model.vip.vehimp.Vehicle;
import com.cnh.pf.model.vip.vehimp.VehicleCurrent;
import com.cnh.pf.model.vip.vehimp.VehicleDeviceClass;
import com.cnh.pf.model.vip.vehimp.VehicleMake;
import com.cnh.pf.model.vip.vehimp.VehicleModel;
import com.cnh.pf.model.vip.vehimp.VehicleType;
import com.google.inject.Key;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.RoboGuiceHelper;
import roboguice.activity.event.OnPauseEvent;
import roboguice.activity.event.OnResumeEvent;
import roboguice.event.EventManager;
import roboguice.util.RoboContext;

/**
 * Data Management Tab Activity
 * Contains test UI, will later include Data Management, Data Sync, Import & Export
 */
public class DataManagementActivity extends TabActivity implements RoboContext {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementActivity.class);

   private static final String TAG = DataManagementActivity.class.getName();
   private static final String PACKAGE_FILE_SYSTEM = "FILE_SYSTEM";
   private static final String PACKAGE_FILE_SYSTEM_LOCATION = "FILE_SYSTEM_LOCATION";
   private static final String PACKAGE_DS_PERF_FLAG = "DATASOURCE_PERF_FLAG";
   public static final String PRODUCT_LIBRARY_TAB = "product_library_tab";

   protected HashMap<Key<?>, Object> scopedObjects = new HashMap<Key<?>, Object>();
   protected EventManager eventManager;
   private VIPDataHandler vipDataHandler;
   private WeakReference<ManageFragment> manageFragmentWeakReference;
   private WeakReference<ImportFragment> importFragmentWeakReference;
   private WeakReference<ExportFragment> exportFragmentWeakReference;
   private WeakReference<ProductLibraryFragment> productLibraryFragmentWeakReference;
   private TabActivityTab productLibraryTab = null;
   private boolean calledProductLibraryViaShortcut = false;
   protected SessionManager sessionManager;

   private DataManagementConnectedVipListener connectedVipListener;
   private DataManagementConnectedPvipListener connectedPvipListener;

   public DataManagementActivity() {
      vipDataHandler = new VIPDataHandler();
   }

   private class DataManagementTabListener implements TabActivityListeners.TabListener {
      private final Activity a;
      private final Fragment fragment;

      public DataManagementTabListener(Fragment fragment, Activity a) {
         this.fragment = fragment;
         this.a = a;

         // Create a place to store internal state within fragment for fragment switching.
         // Fragment.onSaveInstanceState is NOT called upon tab (fragment) switch.
         this.fragment.setArguments(new Bundle());
      }

      /**
       * Returns the current ProductLibraryFragment for testing/injection
       *
       * @return
       */
      public ProductLibraryFragment getProductLibraryFragment() {
         return productLibraryFragmentWeakReference != null ? productLibraryFragmentWeakReference.get() : null;
      }

      @Override
      public void onTabUnselected(TabActivityTab tab) {
         a.getFragmentManager().beginTransaction().detach(fragment).commit();
      }

      @Override
      public void onTabSelected(TabActivityTab tab) {
         Fragment existing = a.getFragmentManager().findFragmentByTag(tab.getTabId());
         final FragmentTransaction ft = a.getFragmentManager().beginTransaction();
         if (existing != null) {
            ft.attach(existing);
         }
         else {
            ft.add(R.id.fragment_content, fragment, tab.getTabId());
         }
         ft.commit();
      }

      @Override
      public void onTabClickable(TabActivityTab tabActivityTab, boolean b) {

      }

      @Override
      public void onTabGrayedout(TabActivityTab tabActivityTab, boolean b) {

      }

      @Override
      public void onTabDisabled(TabActivityTab tab) {

      }

      @Override
      public void onTabEnabled(TabActivityTab tab) {

      }

      @Override
      public void onTabHidden(TabActivityTab tab) {

      }

      @Override
      public void onTabShown(TabActivityTab tab) {

      }

      @Override
      public void onTabAdded(TabActivityTab tab) {

      }

      @Override
      public void onTabRemoved(TabActivityTab tab) {
         fragment.getArguments().clear();
         a.getFragmentManager().beginTransaction().remove(fragment).commit();
      }
   }

   private class DataManagementConnectedVipListener extends ConnectedVipListener {

      DataManagementConnectedVipListener(Context context) {
         super(context);
      }

      @Override
      public void onConnectionStatusChanged(boolean connected) {
         if (connected) {
            //VIP service is connected
            logger.debug("DataManagementVIPListener is connected");
            IVIPServiceAIDL vipService = this.getService();
            if (null != vipService) {
               GetLightWeightVehicleCurrentAsynTask getLightWeightVehicleCurrentAsynTask = new GetLightWeightVehicleCurrentAsynTask();
               getLightWeightVehicleCurrentAsynTask.execute();
               if (null != vipDataHandler) {
                  logger.debug("onServerConnect gets current vehicle");
                  vipDataHandler.setVipService(vipService);
               }
               if (null != productLibraryFragmentWeakReference) {
                  ProductLibraryFragment productLibraryFragment = productLibraryFragmentWeakReference.get();
                  if (null != productLibraryFragment) {
                     logger.debug("onServiceConnected called - productLibraryFragment != null");
                     productLibraryFragment.setVipService(vipService); //TODO: - Use VipDataHandler instead of vipService
                  }
               }

            }
         }
         else {
            //VIP service is disconnected
            logger.debug("DataManagementVIPListener is disconnected");
            if (productLibraryFragmentWeakReference != null) {
               ProductLibraryFragment productLibraryFragment = productLibraryFragmentWeakReference.get();
               if (productLibraryFragment != null) {
                  productLibraryFragment.setVipService(null);
               }
            }
            if (null != vipDataHandler) {
               vipDataHandler.setVipService(null);
            }
         }
      }

      /**
       * Check if the given vehicle is a combine
       * @param vehicle Vehicle to be checked to be a combine or not
       * @return true if the given vehicle is a combine
       */
      private boolean vehicleIsCombine(Vehicle vehicle) {
         if (vehicle != null) {
            VehicleModel vehicleModel = vehicle.getVehicleModel();
            if (vehicleModel != null) {
               VehicleType vehicleType = vehicleModel.getVehicleType();
               if (vehicleType != null) {
                  VehicleDeviceClass vehicleDeviceClass = vehicleType.getName();
                  boolean vehicleIsCombine = VehicleDeviceClass.COMBINE.equals(vehicleDeviceClass) || VehicleDeviceClass.GENERIC_COMBINE.equals(vehicleDeviceClass);
                  logger.debug("vehicleIsCombine?: {}", vehicleIsCombine);
                  return vehicleIsCombine;
               }
               else {
                  logger.error("Could not get Vehicle Type - VehicleType is null!");
               }
            }
            else {
               logger.error("Could not get Vehicle Type - VehicleModel is null!");
            }
         }
         else {
            logger.error("Could not get Vehicle Type - Vehicle is null!");
         }
         return false;
      }

      @Override
      public void deliverVehicleCurrent(final VehicleCurrent vehicleCurrent) throws RemoteException {
         // Take care if you change something here - the vehicle current may have only parts of the data from database inside.
         // look inside this: GetLightWeightVehicleCurrentAsynTask for detailed information.
         if (hasPCM() && (vehicleCurrent == null || !vehicleIsCombine(vehicleCurrent.getVehicle()))) {
            //add tab, if not present
            runOnUiThread(new Runnable() {
               @Override
               public void run() {
                  if (productLibraryTab == null) {
                     ProductLibraryFragment productLibraryFragment = new ProductLibraryFragment();
                     productLibraryFragmentWeakReference = new WeakReference<ProductLibraryFragment>(productLibraryFragment);
                     productLibraryTab = new TabActivityTab(R.string.tab_product_library, R.drawable.tab_product_library_selector, PRODUCT_LIBRARY_TAB,
                           new DataManagementTabListener(productLibraryFragmentWeakReference.get(), DataManagementActivity.this));
                     addTab(productLibraryTab);
                     IVIPServiceAIDL vipService = null;
                     if (connectedVipListener != null) {
                        vipService = connectedVipListener.getService();

                     }
                     productLibraryFragment.setVipService(vipService);
                     IPVIPServiceAIDL pvipService = null;
                     if (connectedPvipListener != null) {
                        pvipService = connectedPvipListener.getVipService();
                     }
                     productLibraryFragment.setPvipService(pvipService);
                  }
                  if (null != productLibraryTab && productLibraryTab.isHidden()) {
                     logger.debug("Showing productLibraryTab");
                     // TabActivity is not handling that for an already shown tab nothing should happen during the call of the method.
                     // Also it deselects the tab always which leads to a bug here. So only call showTab if it is hidden.
                     // https://polarion.cnhind.com/polarion/#/project/Core_Display_PDS/workitem?id=COREPDS-1458
                     showTab(productLibraryTab);
                  }
                  if (calledProductLibraryViaShortcut) {
                     logger.debug("executing shortcut action / jumping to product library");
                     setSelectedTab(productLibraryTab);
                     calledProductLibraryViaShortcut = false; // we don't need this value anymore during the life of this activity.
                  }
                  if (null != vipDataHandler) {
                     logger.debug("onServerConnect gets current vehicle");

                     if (null != vehicleCurrent) {
                        vipDataHandler.updateCurrentVehicle(vehicleCurrent);
                     }
                  }
               }
            });
         }
         else {
            //remove tab, if it is set
            if (null != productLibraryTab) {
               runOnUiThread(new Runnable() {

                  @Override
                  public void run() {
                     //check, if ProductLibraryFragment is shown / Tab is selected
                     ProductLibraryFragment productLibraryFragment = productLibraryFragmentWeakReference.get();
                     if (null != productLibraryFragment && productLibraryFragment.isVisible()) {
                        //select different fragment
                        selectTabAtPosition(0);
                     }
                     logger.debug("Hiding productLibraryTab");
                     hideTab(productLibraryTab);
                  }
               });

            }
         }
      }

      @Override
      public void onTableChange(final TableChangeEvent action, final String tableName, final String id) throws RemoteException {
         //current vehicle changed
         if ((null != action) && (null != tableName) && (null != id)) {
            if (tableName.equals(VehicleCurrent.class.getName())) {
               GetLightWeightVehicleCurrentAsynTask getLightWeightVehicleCurrentAsynTask = new GetLightWeightVehicleCurrentAsynTask();
               getLightWeightVehicleCurrentAsynTask.execute();
            }
         }
      }
   };

   /**
   * AsyncTask to get a light weight vehicle current which includes only {@link VehicleDeviceClass} data.
   */
   private class GetLightWeightVehicleCurrentAsynTask extends AsyncTask<Void, Void, List> {

      // Remark: must be Vehicle_full instead of Vehicle because there is a entity defined in hbm.xml
      // and so HQL uses the entity name instead of the class name.
      private final String CURRENT_VEHICLE_DEVICE_CLASS_QUERY = "SELECT type.name, theMake.make FROM Vehicle_full v " + "LEFT OUTER JOIN v.vehicleModel AS model "
            + "LEFT OUTER JOIN model.vehicleType AS type " + "LEFT OUTER JOIN model.vehicleMake AS theMake "
            + "WHERE v.id = (SELECT vehicle.id FROM VehicleCurrent vc WHERE vc.id = 1)";

      @Override
      protected List doInBackground(Void... nothing) {
         try {
            if (connectedVipListener != null && connectedVipListener.getService() != null) {
               List vehicleList = connectedVipListener.getService().genericQuery(CURRENT_VEHICLE_DEVICE_CLASS_QUERY);
               return vehicleList;
            }
            else {
               logger.error("Could query for LightWeightVehicleCurrent in GetLightWeightVehicleCurrentAsynTask since vip listener is not running properly!");
            }
         }
         catch (RemoteException e) {
            logger.error("failed to load vehicle current", e);
         }
         catch (NullPointerException e) {
            logger.error("failed to load vehicle current", e);
         }
         return null;
      }

      @Override
      protected void onPostExecute(List list) {
         super.onPostExecute(list);
         if (list != null) {
            if (list.size() > 1) {
               logger.warn("got more data then expected");
            }
            if (list.size() >= 1) {
               Object[] singleDatabaseLine = (Object[]) list.get(0);
               VehicleDeviceClass vehicleDeviceClass = (VehicleDeviceClass) singleDatabaseLine[0];
               String vehMake = (String) singleDatabaseLine[1];
               logger.debug("got vehicle with device class: {}", vehicleDeviceClass);
               if (connectedVipListener != null) {
                  try {
                     connectedVipListener.deliverVehicleCurrent(createVehicleCurrent(vehicleDeviceClass, vehMake));
                  }
                  catch (RemoteException e) {
                     logger.error("this should never happen because method was called locally");
                  }
               }
               else {
                  logger.error("Could not deliver vehicle current since vip listener is not running properly!");
               }

            }
         }
      }

      /**
       * factory method to create a vehicle current with only vehicle device class as a single value inside
       * @param vehicleDeviceClass
       * @return the vehicle current
       */
      private VehicleCurrent createVehicleCurrent(VehicleDeviceClass vehicleDeviceClass, String vehMake) {
         VehicleCurrent vehicleCurrent = new VehicleCurrent();
         Vehicle vehicle = new Vehicle();
         VehicleModel vehicleModel = new VehicleModel();
         VehicleMake vehicleMake = new VehicleMake();
         VehicleType vehicleType = new VehicleType();
         vehicleCurrent.setVehicle(vehicle);
         vehicle.setVehicleModel(vehicleModel);
         vehicleModel.setVehicleMake(vehicleMake);
         vehicleModel.setVehicleType(vehicleType);
         vehicleType.setName(vehicleDeviceClass);
         vehicleMake.setMake(vehMake);
         return vehicleCurrent;
      }

   }

   private TabActivityTab createActivityTab(BaseDataFragment fragment, int titleRes, int drawableRes, String tabId) {
      return new TabActivityTab(titleRes, drawableRes, tabId, new DataManagementTabListener(fragment, this));
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      logger.debug("onCreate called");

      try {
         ApplicationInfo appInfo = getPackageManager().getApplicationInfo(this.getPackageName(), PackageManager.GET_META_DATA);
         Bundle bundle = appInfo.metaData;
         String storageType = bundle.getString(PACKAGE_FILE_SYSTEM);
         String storageLocation = bundle.getString(PACKAGE_FILE_SYSTEM_LOCATION);
         if ((storageType != null) && (storageLocation != null)) {
            UtilityHelper.setPreference(UtilityHelper.STORAGE_LOCATION_TYPE, storageType, this);
            UtilityHelper.setPreference(UtilityHelper.STORAGE_LOCATION, storageLocation, this);
         }
         String dsPerfFlag = bundle.getString(PACKAGE_DS_PERF_FLAG);
         BaseDataFragment.dsPerfFlag = false;
         if((dsPerfFlag != null) && dsPerfFlag.equals("Enabled")) {
            BaseDataFragment.dsPerfFlag = true;
         }
      }
      catch (NameNotFoundException e) {
         logger.info("PackageManager namesNameNotFoundException ", e);
      }
      catch (Exception e) {
         logger.info("Unable to read package data ", e);
      }

      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" }, new RoboModule(app), new ChannelModule(app));
      super.onCreate(savedInstanceState);

      connectedVipListener = new DataManagementConnectedVipListener(getApplicationContext());
      connectedPvipListener = new DataManagementConnectedPvipListener(getApplicationContext());

      eventManager = RoboGuice.getInjector(this).getInstance(EventManager.class);
      sessionManager = RoboGuice.getInjector(this).getInstance(SessionManager.class);

      // Create Manage Tab
      ManageFragment manageFragment = new ManageFragment();
      manageFragmentWeakReference = new WeakReference<ManageFragment>(manageFragment);
      manageFragment.setSessionManager(sessionManager);
      TabActivityTab managementTab = createActivityTab(manageFragment, R.string.tab_management, R.drawable.tab_management_selector, getResources().getString(R.string.tab_management));
      addTab(managementTab);

      // Create Import tab
      ImportFragment importFragment = new ImportFragment();
      importFragmentWeakReference = new WeakReference<ImportFragment>(importFragment);
      importFragment.setSessionManager(sessionManager);
      TabActivityTab importTab = createActivityTab(importFragment, R.string.tab_import, R.drawable.tab_import_selector, getResources().getString(R.string.tab_import));
      addTab(importTab);

      // Create Export tab
      ExportFragment exportFragment = new ExportFragment();
      exportFragmentWeakReference = new WeakReference<ExportFragment>(exportFragment);
      exportFragment.setVipDataHandler(vipDataHandler);
      exportFragment.setSessionManager(sessionManager);
      TabActivityTab exportTab = createActivityTab(exportFragment, R.string.tab_export, R.drawable.tab_export_selector, getResources().getString(R.string.tab_export));
      addTab(exportTab);

      productLibraryFragmentWeakReference = new WeakReference<ProductLibraryFragment>(new ProductLibraryFragment());
      productLibraryTab = new TabActivityTab(R.string.tab_product_library, R.drawable.tab_product_library_selector, getResources().getString(R.string.tab_product_library),
            new DataManagementTabListener(productLibraryFragmentWeakReference.get(), this));
      addTab(productLibraryTab);
      hideTab(productLibraryTab);

      setTabActivityTitle(getString(R.string.app_name));
      selectTabAtPosition(0);

      // It is not possible to jump to the ProductLibraryFragment/Tab at this time. So it's necessary to save
      // if the activity was called via shortcut to jump to the tab as soon as it is ready.
      Intent currentIntent = getIntent();
      // is there any const we can use instead of this string?
      if (currentIntent.hasExtra("com.cnh.android.shortcut.extra.SHORTCUT")) {
         Bundle extras = currentIntent.getExtras();
         logger.debug("Data Management Activity was called via shortcut ", extras.toString());
         String tabId = getTabId(extras);
         if (PRODUCT_LIBRARY_TAB.equals(tabId)) {
            logger.debug("Product Library was called via shortcut");
            calledProductLibraryViaShortcut = true;
            extras.remove(PRODUCT_LIBRARY_TAB);
         }
      }
   }

   /**
    * Check if the configuration includes a pcm
    * @return true if the configuration includes a pcm
    */
   private boolean hasPCM() {
      //pref_key_pcm_standalone
      boolean hasPCM = false;
      try {
         GlobalPreferences globalPreferences = new GlobalPreferences(this);
         hasPCM = globalPreferences.hasPCM();
      }
      catch (GlobalPreferencesNotAvailableException e) {
         if (logger.isWarnEnabled()) {
            logger.warn("global preferences not available - guess that is has pcm? :" + hasPCM, e);
         }
      }
      logger.debug("hasPCM?: {}", hasPCM);
      return hasPCM;
   }

   @Override
   public Map<Key<?>, Object> getScopedObjectMap() {
      return scopedObjects;
   }

   /**
    * @return true if name begins with a lowercase character (indicating a package) and it doesn't start with com.android
    */
   protected static boolean shouldInjectOnCreateView(String name) {
      return Character.isLowerCase(name.charAt(0)) && !name.startsWith("com.android") && !name.equals("fragment");
   }

   @Override
   public View onCreateView(String name, Context context, AttributeSet attrs) {
      if (shouldInjectOnCreateView(name)) return injectOnCreateView(name, context, attrs);

      return super.onCreateView(name, context, attrs);
   }

   @Override
   public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
      if (shouldInjectOnCreateView(name)) return injectOnCreateView(name, context, attrs);

      return super.onCreateView(parent, name, context, attrs);
   }

   protected static View injectOnCreateView(String name, Context context, AttributeSet attrs) {
      try {
         final Constructor<?> constructor = Class.forName(name).getConstructor(Context.class, AttributeSet.class);
         final View view = (View) constructor.newInstance(context, attrs);
         RoboGuice.getInjector(context).injectMembers(view);
         RoboGuice.getInjector(context).injectViewMembers(view);
         return view;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private class DataManagementConnectedPvipListener extends ConnectedPVIPListener {

      DataManagementConnectedPvipListener(Context context) {
         super(context);
      }

      @Override
      public void onConnectionChange(boolean connected) {
         IPVIPServiceAIDL pvipService = this.getVipService();
         if (connected) {
            if (pvipService != null && productLibraryFragmentWeakReference != null) {
               ProductLibraryFragment productLibraryFragment = productLibraryFragmentWeakReference.get();
               if (productLibraryFragment != null) {
                  logger.debug("onConnectionChange(connected) called - productLibraryFragment != null, set Pvip service");
                  productLibraryFragment.setPvipService(pvipService);
               }
            }
         }
         else {
            logger.debug("onConnectionChange(disconnected) for pvip service called");
            if (pvipService != null && productLibraryFragmentWeakReference != null) {
               ProductLibraryFragment productLibraryFragment = productLibraryFragmentWeakReference.get();
               if (productLibraryFragment != null) {
                  productLibraryFragment.setPvipService(null);
               }
            }
         }
      }
   }

   @Override
   public void onResume() {
      logger.debug("onResume called");
      super.onResume();
      eventManager.fire(new OnResumeEvent(this));
      sessionManager.connect();
      logger.debug("Sending INTERNAL_DATA broadcast");
      sendBroadcast(new Intent(ServiceConstants.ACTION_INTERNAL_DATA).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
      if (connectedVipListener != null) {
         connectedVipListener.connect();
      }
      else {
         logger.error("Could not connect connectedVipListener since it is not initialized!");
      }
      if (connectedPvipListener != null) {
         connectedPvipListener.connect();
      }
      else {
         logger.error("Could not connect connectedPvipListener since it is not initialized!");
      }
   }

   @Override
   protected void onPause() {
      logger.debug("onPause called");
      super.onPause();
      // Send Intent to DM service to reset cached data before closing the app
      Intent intent = new Intent(this, DataManagementService.class);
      intent.setAction(DataManagementService.ACTION_RESET_CACHE);
      startService(intent);

      eventManager.fire(new OnPauseEvent(this));

      if (connectedVipListener != null) {
         connectedVipListener.disconnect();
      }
      else {
         logger.error("Could not disconnect connectedVipListener since it is not initialized!");
      }
      if (connectedPvipListener != null) {
         connectedPvipListener.disconnect();
      }
      else {
         logger.error("Could not disconnect connectedPvipListener since it is not initialized!");
      }

      sessionManager.disconnect();
   }

   @Override
   protected void onDestroy() {
      logger.debug("onDestroy called");
      super.onDestroy();

      vipDataHandler = null;
      scopedObjects = null;
      eventManager = null;
      productLibraryFragmentWeakReference = null;
      productLibraryTab = null;
      connectedVipListener = null;
      connectedPvipListener = null;
   }
}
