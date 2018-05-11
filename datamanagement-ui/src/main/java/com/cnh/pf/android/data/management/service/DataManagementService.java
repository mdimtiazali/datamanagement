/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.service;

import static android.os.Environment.MEDIA_BAD_REMOVAL;

import static com.cnh.pf.android.data.management.helper.DatasourceHelper.getHostname;
import static com.cnh.pf.android.data.management.utility.UtilityHelper.NEGATIVE_BINARY_ERROR;
import static com.cnh.pf.data.management.service.ServiceConstants.ACTION_STOP;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;

import com.android.annotations.NonNull;
import com.cnh.android.status.Status;
import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.RoboModule;
import com.cnh.pf.android.data.management.connection.DataServiceConnectionImpl.ErrorEvent;
import com.cnh.pf.android.data.management.fault.DMFaultHandler;
import com.cnh.pf.android.data.management.fault.FaultCode;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.utility.UtilityHelper;
import com.cnh.pf.data.management.DataManagementSession;
import com.cnh.pf.data.management.aidl.IDataManagementListenerAIDL;
import com.cnh.pf.data.management.aidl.MediumDevice;
import com.cnh.pf.data.management.service.DatasourceContentClient;
import com.cnh.pf.data.management.service.DatasourceContract;
import com.cnh.pf.data.management.service.ServiceConstants;
import com.cnh.pf.datamng.Process;
import com.cnh.pf.datamng.Process.Result;
import com.cnh.pf.jgroups.ChannelModule;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.inject.name.Named;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.View;
import org.jgroups.util.Rsp;
import org.jgroups.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.inject.Inject;

import roboguice.RoboGuiceHelper;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import roboguice.inject.InjectResource;
import roboguice.service.RoboService;

/**
 * Service manages a DataManagementSession. Keeps one session alive until completion.
 * Notifies listeners of progress and provides state information to UI.
 * Accepts operations from UI, and relays them to destination datasource.
 * @author oscar.salazar@cnhind.com
 */
public class DataManagementService extends RoboService implements SharedPreferences.OnSharedPreferenceChangeListener {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementService.class);

   public static final String ACTION_CANCEL = "com.cnh.pf.data.management.CANCEL";

   private static final boolean SEND_NOTIFICATION = false;
   public static final int KILL_STATUS_DELAY = 5000;

   private @Inject Mediator mediator;
   private @Inject DatasourceHelper dsHelper;
   @Named(DefaultRoboModule.GLOBAL_EVENT_MANAGER_NAME)
   @Inject
   EventManager globalEventManager;
   @Named("global")
   @Inject
   private SharedPreferences prefs;

   @Inject
   private FormatManager formatManager;

   @Inject
   private DMFaultHandler faultHandler;

   @InjectResource(R.string.exporting_string)
   private String exporting;
   @InjectResource(R.string.importing_string)
   private String importing;
   @InjectResource(R.string.status_successful)
   private String statusSuccessful;
   @InjectResource(R.string.status_cancelled)
   private String statusCancelled;
   @InjectResource(R.string.status_cancelling)
   private String statusCancelling;
   @InjectResource(R.string.status_starting)
   private String statusStarting;

   @Inject
   NotificationManager notifyManager;

   boolean performCalled;
   // This is to keep track of a session's STATUS and send broadcast message to STATUS UI.
   private Map<DataManagementSession, Status> activeStatusUpdateSessions = new HashMap<DataManagementSession, Status>();
   // This is to maintain reference to current active session.
   private AtomicReference<DataManagementSession> currentSession = new AtomicReference<DataManagementSession>();

   public void setCurrentSession(final DataManagementSession session) {
      currentSession.set(session);
   }

   private DataManagementSession getCurrentSession() {
      return currentSession.get();
   }

   private boolean hasActiveSession() {
      if (currentSession.get() != null) {
         return currentSession.get().getSessionOperation() == DataManagementSession.SessionOperation.DISCOVERY
               || currentSession.get().getSessionOperation() == DataManagementSession.SessionOperation.PERFORM_OPERATIONS
               || currentSession.get().getSessionOperation() == DataManagementSession.SessionOperation.CALCULATE_CONFLICTS
               || currentSession.get().getSessionOperation() == DataManagementSession.SessionOperation.CALCULATE_OPERATIONS;
      }
      return false;
   }

   ConcurrentHashMap<String, IDataManagementListenerAIDL> listeners = new ConcurrentHashMap<String, IDataManagementListenerAIDL>();
   private Handler handler = new Handler();
   private final IBinder localBinder = new LocalBinder();

   /* Time to wait for USB Datasource to register if the usb has valid data*/
   private static int usbDelay = 6000;

   private BitmapDrawable statusDrawable;
   private static Status dataStatus;

   private boolean isScanningUsb;

   private boolean useInternalFileSystem = false;

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      logger.debug("onStartCommand {}", intent);

      if (intent == null) { //happens after restart when service is sticky
         return START_STICKY;
      }

      if (ACTION_CANCEL.equals(intent.getAction())) {
         cancel(intent.<DataManagementSession> getParcelableExtra("session"));
      }
      else if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
         sendMediumUpdateEvent();
         faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).reset();
         faultHandler.getFault(FaultCode.USB_REMOVED_DURING_IMPORT).reset();
         /*Uri mount = intent.getData();
         File mountFile = new File(mount.getPath());
         logger.info("Media has been mounted @{}: ", mountFile.getAbsolutePath());
         scan(mountFile);*/
      }
      else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction()) || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(intent.getAction())
            || Intent.ACTION_MEDIA_REMOVED.equals(intent.getAction()) || Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
         logger.info("Media has been unmounted");
         if (hasActiveSession() && Intent.ACTION_MEDIA_BAD_REMOVAL.equals(intent.getAction())) {
            logger.debug("Bad USB removal during task");
            // If USB stick gets pulled out during the task (discovery, perform operation),
            // the current ongoing session needs to be cancelled and alert corresponding fault.
            final DataManagementSession currentSession = getCurrentSession();
            logger.debug("Current active session: {}", currentSession);

            if (currentSession.isProgress() && (isUsbImport(currentSession) || isUsbExport(currentSession))) {
               logger.debug("USB unplugged while interacting with datasources. Cancel the current session.");
               cancel(currentSession);
               FaultCode faultCode = isUsbExport(currentSession) ? FaultCode.USB_REMOVED_DURING_EXPORT : FaultCode.USB_REMOVED_DURING_IMPORT;
               faultHandler.getFault(faultCode).alert();
            }
            else if (isUsbImport(currentSession) && currentSession.getObjectData() != null
                  && (currentSession.getSessionOperation() == DataManagementSession.SessionOperation.DISCOVERY
                        || currentSession.getSessionOperation() == DataManagementSession.SessionOperation.CALCULATE_CONFLICTS)) {
               logger.debug("USB unplugged while waiting for the user response (discovery/conflict resolution).");

               currentSession.setResult(Result.CANCEL);
               currentSession.setSessionOperation(DataManagementSession.SessionOperation.DISCOVERY);
               faultHandler.getFault(FaultCode.USB_REMOVED_DURING_IMPORT).alert();
            }
         }

         sendMediumUpdateEvent();
         removeUsbStatus();
      }
      return START_STICKY;
   }

   @Override
   public IBinder onBind(Intent intent) {
      logger.debug("onBind");
      //TODO distinguish between local and remote bind
      return localBinder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      logger.debug("onUnbind");
      return super.onUnbind(intent);
   }

   @Override
   public void onCreate() {
      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" }, new RoboModule(app), new ChannelModule(app));
      super.onCreate();
      statusDrawable = (BitmapDrawable) getResources().getDrawable(R.drawable.ic_usb);
      try {
         prefs.registerOnSharedPreferenceChangeListener(this);
         mediator.setProgressListener(pListener);
      }
      catch (Exception e) {
         logger.error("error in onCreate", e);
      }
   }

   @Override
   public void onDestroy() {
      stopUsbServices();
      sendBroadcast(new Intent(ServiceConstants.ACTION_INTERNAL_DATA_STOP).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
      removeUsbStatus();
      listeners.clear();
      prefs.unregisterOnSharedPreferenceChangeListener(this);
      new Thread(new Runnable() {
         @Override
         public void run() {
            mediator.close();
         }
      });
      super.onDestroy();
   }

   public DataManagementSession getSession() {
      if (!activeStatusUpdateSessions.isEmpty()) {
         return activeStatusUpdateSessions.keySet().iterator().next();
      }
      return null;
   }

   public void register(String name, IDataManagementListenerAIDL listener) {
      logger.debug("Register: " + name);
      if (listeners.isEmpty()) {
         if (!mediator.getChannel().isConnected()) {
            logger.debug("first listener comes in, do ConnectTask");
            new ConnectTask().execute();
         }
      }
      listeners.put(name, listener);
   }

   public void unregister(String name) {
      logger.debug("Unegister: " + name);
      listeners.remove(name);
   }

   protected void sendMediumUpdateEvent() {
      try {
         List<MediumDevice> mediums = getMediums();
         for (IDataManagementListenerAIDL listener : listeners.values()) {
            try {
               listener.onMediumsUpdated(mediums);
            }
            catch (RemoteException e) {
               logger.error("", e);
            }
         }
      }
      catch (Exception e) {
         logger.warn("Error sending medium update event, probably mediator isn't connected");
      }
   }

   /**
    * Scan USB stick for data.
    *
    * Starts all USB datasources and checks to see how many found data.
    * Passes all subfolders under the mountpoint.
    * @param mount
    */
   protected void scan(File mount) {
      logger.info("Checking for data");
      isScanningUsb = true;
      dataStatus = new Status(getResources().getString(R.string.usb_data_available), statusDrawable, getApplication().getPackageName());
      sendStatus(dataStatus, getResources().getString(R.string.usb_scanning));
      //Check if USB has any interesting data
      File[] folders = mount.listFiles(new FileFilter() {
         @Override
         public boolean accept(File file) {
            return file.isDirectory() && !file.getName().startsWith(".");
         }
      });
      String[] paths = new String[folders.length];
      for (int i = 0; i < folders.length; i++) {
         paths[i] = folders[i].getAbsolutePath();
      }
      //Launch USB datasource broadcast for every root folder on USB
      scanUsbServices(paths, false, null, new Runnable() {
         @Override
         public void run() {
            logger.info("Found USB data!");
            sendStatus(dataStatus, getResources().getString(R.string.usb_data_available));
            isScanningUsb = false;
         }
      }, new Runnable() {
         @Override
         public void run() {
            logger.info("No USB Data found");
            sendStatus(dataStatus, getResources().getString(R.string.usb_no_data_available));
            handler.postDelayed(new Runnable() {
               @Override
               public void run() {
                  removeStatus(dataStatus);
               }
            }, usbDelay);
            isScanningUsb = false;
         }
      });
   }

   protected void removeUsbStatus() {
      if (dataStatus != null) {
         sendBroadcast(new Intent(Status.ACTION_STATUS_REMOVE).putExtra(Status.ID, ParcelUuid.fromString(dataStatus.getID().toString())));
         dataStatus = null;
      }
   }

   public DataManagementSession processOperation(final DataManagementSession session, DataManagementSession.SessionOperation sessionOperation) {
      logger.debug("service.processOperation: {}", sessionOperation);
      session.setSessionOperation(sessionOperation);
      session.setProgress(true);
      session.setResult(null);
      setCurrentSession(session);
      if (sessionOperation.equals(DataManagementSession.SessionOperation.DISCOVERY)) {
         if (isUsbImport(session)) {
            if (session.getSource() != null && (session.getSource().getPath() != null) &&
               ((useInternalFileSystem) || (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)))) {
               logger.debug("Starting USB Datasource: getSource():{}", session.getSource());
               startUsbServices(new String[] { session.getSource().getPath().getPath() }, false, session.getFormat(), new Runnable() {
                  @Override
                  public void run() {
                     logger.debug("Running discovery");
                     new DiscoveryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
                  }
               }, new Runnable() {
                  @Override
                  public void run() {
                     logger.debug("Unable to start USB services for USB import");
                     session.setProgress(false);
                     session.setResult(Process.Result.NO_DATASOURCE);
                     globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_SOURCE_DATASOURCE));
                  }
               });
            }
            else {
               session.setProgress(false);
               session.setResult(Process.Result.NO_DATASOURCE);
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NEED_DATA_PATH));
            }
         }
         else {
            new DiscoveryTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
         }
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_OPERATIONS)) {
         new CalculateTargetsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.CALCULATE_CONFLICTS)) {
         new CalculateConflictsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
         performOperations(session);
      }
      else if (sessionOperation.equals(DataManagementSession.SessionOperation.UPDATE)) {
         updateOperations(session);
      }
      else if(sessionOperation.equals(DataManagementSession.SessionOperation.DELETE)){
         deleteOperations(session);
      }
      else {
         logger.error("Couldn't find op");
      }
      logger.debug("before return Datasource: getSource():{}", session.getSource());
      return session;
   }

   /**
    * Return whether an status-update required operation is currently executing.
    * @return true executing, false otherwise;
    */
   public boolean hasActiveStatusUpdateSession() {
      return !isScanningUsb && getActiveStatusUpdateSession() != null;
   }

   public DataManagementSession getActiveStatusUpdateSession() {
      for (Map.Entry<DataManagementSession, Status> entry : activeStatusUpdateSessions.entrySet()) {
         if (entry.getKey().getResult() == null && entry.getKey().getSessionOperation().equals(DataManagementSession.SessionOperation.PERFORM_OPERATIONS)) {
            return entry.getKey();
         }
      }
      return null;
   }

   /**
    * Cancel a session.
    * @param session the session to cancel.
    */
   public void cancel(DataManagementSession session) {
      new CancelTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
   }

   /**
    * Identify the data source locations of data sources connected to the display
    * Create an array list of devices
    * @return number of devices
    */
   public List<MediumDevice> getMediums() {
      List<MediumDevice> devs = new ArrayList<MediumDevice>();
      //temporarily always add usb for testing
      logger.debug("getDevices external storage state = {}", Environment.getExternalStorageState());

      // Checking if internal file system to be used for testing data management
      try {
         String fileStorage = UtilityHelper.getSharedPreferenceString(this, UtilityHelper.STORAGE_LOCATION_TYPE);
         String fileStorageLocation = UtilityHelper.getSharedPreferenceString(this, UtilityHelper.STORAGE_LOCATION);

         if ((fileStorage != null) && fileStorage.equals(UtilityHelper.STORAGE_LOCATION_INTERNAL)) {
            if ((fileStorageLocation != null) && !fileStorageLocation.isEmpty()) {
               File storageFolder = new File(fileStorageLocation);
               if ( storageFolder.exists() && storageFolder.canRead() && storageFolder.canWrite() ){
                  devs.add(new MediumDevice(Datasource.LocationType.USB_PHOENIX, storageFolder, UtilityHelper.STORAGE_LOCATION_USB));
                  devs.add(new MediumDevice(Datasource.LocationType.USB_HAWK, storageFolder, UtilityHelper.STORAGE_LOCATION_USB));
                  devs.add(new MediumDevice(Datasource.LocationType.USB_FRED, storageFolder, UtilityHelper.STORAGE_LOCATION_USB));
                  devs.add(new MediumDevice(Datasource.LocationType.USB_DESKTOP_SW, storageFolder, UtilityHelper.STORAGE_LOCATION_USB));
                  useInternalFileSystem = true;
                  logger.debug("using internal storage = {}", storageFolder);
               }
            }
         }
      }
      catch(Exception ex){
         logger.info("Unable to check if internal flash need to be used.", ex);
      }

      if ( (useInternalFileSystem == false) && (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) ){
         devs.add(new MediumDevice(Datasource.LocationType.USB_PHOENIX, Environment.getExternalStorageDirectory(),
            UtilityHelper.STORAGE_LOCATION_USB));
         devs.add(new MediumDevice(Datasource.LocationType.USB_HAWK, Environment.getExternalStorageDirectory(),
            UtilityHelper.STORAGE_LOCATION_USB));
         devs.add(new MediumDevice(Datasource.LocationType.USB_FRED, Environment.getExternalStorageDirectory(),
            UtilityHelper.STORAGE_LOCATION_USB));
         devs.add(new MediumDevice(Datasource.LocationType.USB_DESKTOP_SW, Environment.getExternalStorageDirectory(),
            UtilityHelper.STORAGE_LOCATION_USB));
      }

      String myHostname = getHostname(mediator.getAddress());
      logger.trace("My HOSTNAME {}", myHostname);
      if (Strings.isNullOrEmpty(myHostname)) {
         throw new IllegalStateException("No hostname for mediator connection");
      }
      for (Address addr : dsHelper.getAddressForSourceType(Datasource.LocationType.DISPLAY)) {
         String name = getHostname(addr);
         if (Strings.isNullOrEmpty(name)) {
            logger.warn("Datasource without machine name");
            continue;
         }
         logger.trace("Display hostname {}", name);
         if (myHostname.equals(name)) continue;
         devs.add(new MediumDevice(Datasource.LocationType.DISPLAY, addr, name));
      }
      return devs;
   }

   /**
    * Determine if the export destination is to  a USB device
    * @param session
    * @return
    */
   public static boolean isUsbExport(DataManagementSession session) {
      Datasource.LocationType[] sourceTypes = session.getSourceTypes();
      Datasource.LocationType destinationType = null;
      if (session.getDestination() != null) {
         destinationType = session.getDestination().getType();
      }
      else
         return false;

      return sourceTypes != null
              && Arrays.binarySearch(sourceTypes, Datasource.LocationType.PCM) > NEGATIVE_BINARY_ERROR
              && (destinationType.equals(Datasource.LocationType.USB_PHOENIX)
              || destinationType.equals(Datasource.LocationType.USB_FRED)
              || destinationType.equals(Datasource.LocationType.USB_HAWK)
              || destinationType.equals(Datasource.LocationType.USB_DESKTOP_SW));
   }

   public static boolean isUsbImport(DataManagementSession session) {
      return session.getSource() != null && session.getSource().getType().equals(Datasource.LocationType.USB_PHOENIX) && session.getDestinationTypes() != null
            && Arrays.binarySearch(session.getDestinationTypes(), Datasource.LocationType.PCM) > NEGATIVE_BINARY_ERROR;
   }

   /**
    * Return whether DISPLAY or INTERNAL data source is available
    * @return true if available, false otherwise;
    */
   public boolean hasLocalSources() {
      return dsHelper.getLocalDatasources(new Datasource.LocationType[] { Datasource.LocationType.DISPLAY, Datasource.LocationType.PCM }).size() > 0;
   }

   private void performOperations(final DataManagementSession session) {
      try {
         session.setResult(null);
         performCalled = false;
         Status status = new com.cnh.android.status.Status("", statusDrawable, getApplicationContext().getPackageName());
         activeStatusUpdateSessions.put(session, status);
         sendStatus(session, statusStarting);
         if (isUsbExport(session)) {
            File destinationFolder = new File(session.getDestination().getPath(), formatManager.getFormat(session.getFormat()).path);
            startUsbServices(new String[] { destinationFolder.getPath() }, true, session.getFormat(), new Runnable() {
               @Override
               public void run() {
                  if (!hasActiveStatusUpdateSession()) {
                     logger.debug("Operation cancelled before calling performOperations");
                     return; //if cancel was pressed before datasource started
                  }
                  performCalled = true;
                  new PerformOperationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
               }
            }, new Runnable() {
               @Override
               public void run() {
                  logger.debug("Unable to start USB services for USB export");
                  session.setProgress(false);
                  session.setResult(Process.Result.NO_DATASOURCE);
                  globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               }
            });
         }
         else if (isUsbImport(session) && (Environment.getExternalStorageState().equals(Environment.MEDIA_BAD_REMOVAL))) {
            logger.debug("Unable to continue PerformOperation for USB import");
            session.setProgress(false);
            session.setResult(Process.Result.NO_DATASOURCE);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_SOURCE_DATASOURCE));
         }
         else {
            performCalled = true;
            new PerformOperationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
         }
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }

   private void updateOperations(final DataManagementSession session) {
      try {
         session.setResult(null);
         new UpdateOperationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }
   private void deleteOperations(final DataManagementSession session) {
      try {
         session.setResult(null);
         new DeleteOperationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, session);
      }
      catch (Exception e) {
         logger.error("Could not find destination address for this type", e);
      }
   }

   private void scanUsbServices(String[] path, boolean create, String format, final Runnable onSuccess, final Runnable onFailure) {
      startUsbServices(path, create, format, ServiceConstants.ACTION_SCAN, onSuccess, onFailure);
   }

   private void startUsbServices(String[] path, boolean create, String format, final Runnable onSuccess, final Runnable onFailure) {
      startUsbServices(path, create, format, ServiceConstants.USB_ACTION_INTENT, onSuccess, onFailure);
   }

   private void startUsbServices(String[] path, boolean create, String format, String intent, final Runnable onSuccess, final Runnable onFailure) {
      logger.debug("Starting USB datasource");
      if (path.length > 0 && !Environment.getExternalStorageState().equals(MEDIA_BAD_REMOVAL)) {
         final DatasourceContentClient dClient = new DatasourceContentClient(this);
         getContentResolver().delete(DatasourceContract.Folder.CONTENT_URI, "", null);//clear to avoid previous error will block all following operation
         dClient.addFolderRequest(path);
         getContentResolver().registerContentObserver(DatasourceContract.Folder.CONTENT_URI, true, new ContentObserver(handler) {

            @Override
            public void onChange(boolean selfChange) {
               onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
               //check if heard back from all datasources
               if (dClient.isRequestProcessed()) {
                  logger.info("Got responses from all datasources, continuing. selfchange {}", selfChange);
                  getContentResolver().unregisterContentObserver(this);
                  handler.postDelayed(dClient.hasValid() ? onSuccess : onFailure, 2000);
                  getContentResolver().delete(DatasourceContract.Folder.CONTENT_URI, "", null);
               }
               else {
                  logger.info("Got response from a datasource {}", uri);
               }
            }
         });
         Intent i = new Intent(intent);
         i.putExtra(ServiceConstants.USB_PATH, path);
         i.putExtra(ServiceConstants.CREATE_PATH, create);
         if (format != null) {
            i.putExtra(ServiceConstants.FORMAT, format);
         }
         i.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
         getApplicationContext().sendBroadcast(i);
      }
      else { //if paths is empty
         handler.postDelayed(onFailure, 1000);
      }
   }

   public void stopUsbServices() {
      logger.debug("Stopping all USB datasources");
      getApplicationContext().sendBroadcast(new Intent(ACTION_STOP));
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      logger.info("Shared prefs changed {}.  Stopping DatamanagementService", key);
      stopSelf();
   }

   public class LocalBinder extends Binder {

      public DataManagementService getService() {
         return DataManagementService.this;
      }
   }

   private Mediator.ProgressListener pListener = new Mediator.ProgressListener() {
      @Override
      public void onProgressPublished(final String operation, final int progress, final int max) {
         logger.debug(String.format("publishProgress(%s, %d, %d)", operation, progress, max));
         handler.post(new Runnable() {
            @Override
            public void run() {
               for (IDataManagementListenerAIDL listener : listeners.values()) {
                  try {
                     listener.onProgressUpdated(operation, progress, max);
                  }
                  catch (RemoteException e) {
                     logger.error("", e);
                  }
               }
            }
         });
      }

      @Override
      public void onSecondaryProgressPublished(String secondaryOperation, int secondaryProgress, int secondaryMax) {
      }

      @Override
      public void onViewAccepted(View newView) {
         logger.debug("onViewAccepted {}", newView);
         try {
            dsHelper.setSourceMap(newView);
         }
         catch (Exception e) {
            logger.error("Error in updating sourceMap", e);
         }
      }
   };

   private class ConnectTask extends AsyncTask<Void, Void, Void> {

      @Override
      protected Void doInBackground(Void... params) {
         try {
            System.setProperty(Global.IPv4, "true");
            mediator.start();
            logger.debug("mediator started, notify the listeners");
            sendMediumUpdateEvent();
         }
         catch (Exception e) {
            logger.error("", e);
         }
         return null;
      }
   }

   static String addressToString(Address[] addresses) {
      if (addresses != null) {
         return Collections2.transform(Arrays.asList(addresses), new Function<Address, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Address input) {
               return UUID.get(input);
            }
         }).toString();
      }
      else {
         return "NULL";
      }
   }

   /**
    * Convert list of devices to array of addesses
    * @param devices
    * @return
    */
   private static Address[] getAddresses(List<MediumDevice> devices) throws NullPointerException {
      if (devices != null) {
         return Collections2.transform(devices, new Function<MediumDevice, Address>() {
            @Nullable
            @Override
            public Address apply(@Nullable MediumDevice input) {
               return input.getAddress();
            }
         }).toArray(new Address[devices.size()]);
      }
      else {
         return null;
      }
   }

   private class DiscoveryTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         DataManagementSession session = params[0];
         session.setProgress(true);
         try {
            resolveSources(session);
            Address[] addrs = getAddresses(session.getSources());
            logger.debug("Discovery for {}, address: {}", Arrays.toString(session.getSourceTypes()), addressToString(addrs));
            if (addrs == null || addrs.length > 0) {
               session.setObjectData(mediator.discovery(addrs));
               if (session.getObjectData() == null || session.getObjectData().isEmpty()) {
                  session.setProgress(false);
                  globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_DATA));
                  session.setResult(Result.NO_DATASOURCE);
               }
               else {
                  session.setResult(Process.Result.SUCCESS);
               }
            }
            else {
               logger.debug("No datasource given!");
               session.setProgress(false);
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_SOURCE_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.debug("error in discovery: ", e);
            session.setProgress(false);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.DISCOVERY_ERROR, Throwables.getRootCause(e).getMessage()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }
   }

   /**
    * Resolve source addresses (MediumDevice) in session
    * @param session    the data management session object
    * @return true if resolving addresses is successful
    */
   private boolean resolveSources(@NonNull DataManagementSession session) {
      if (session.getSources() == null && session.getSourceTypes() != null) { //export
         session.setSources(dsHelper.getLocalDatasources(session.getSourceTypes()));
      }
      else if (isUsbImport(session)) {
         if (session.getSource() == null) {  //This is needed for import, but causes export to fail.
            logger.debug("The source is null.");
            return false;
         }
         else if (session.getSource().getAddress() == null) { //This is needed for import, but causes export to fail.
            session.setSources(dsHelper.getLocalDatasources(session.getSource().getType()));
            session.getSource().setAddress(session.getFirstSource().getAddress());
         }
      }
      return true;
   }

   /**
    * Resolve target addresses (MediumDevice) in session
    * @param session    the data management session object
    * @return true if resolving addresses is successful
    */
   private boolean resolveTargets(@NonNull DataManagementSession session) {
      if (session.getDestinations() == null && session.getDestinationTypes() != null) { //resolve by type
         logger.debug("session.getTargets() is null and type isn't null");
         session.setDestinations(dsHelper.getLocalDatasources(session.getDestinationTypes()));
      }
      else if (session.getDestination() != null) { // if MediumDevice hasn't been resolved
         session.setDestinations(dsHelper.getLocalDatasources(session.getDestination().getType()));
         session.getDestination().setAddress(session.getDestination(0).getAddress()); //At this point, there is only one item in destinations
      }

      List<MediumDevice> destinations = session.getDestinations();
      List<MediumDevice> sources = session.getSources();
      if (destinations != null && sources != null) {
         if (destinations.size() > sources.size()) {
            logger.debug("resolveTargets: Destinations are greater than sources - Import");
            if (session.getDestination() == null) {
               session.setDestination(session.getDestination(0));
               session.setDestinations(dsHelper.getLocalDatasources(session.getDestination(0).getType()));
            }
         } else if (sources.size() > destinations.size()) {
            logger.debug("resolveTargets: Sources are greater than destination - Export");
            if(session.getDestination() == null){
               logger.debug("the Target is null");
               return false;
            }
         }
      }
      return true;
   }

   private class CalculateTargetsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Calculate Targets...");
         DataManagementSession session = params[0];
         session.setProgress(true);
         try {
            resolveTargets(session);
            Address[] addresses = getAddresses(session.getDestinations());
            logger.debug("Calculate targets to address: {}", addressToString(addresses));
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateOperations(session.getObjectData(), addresses));
               logger.debug("Got operation: {}", session.getData());
               session.setResult(Result.CANCEL.equals(session.getResult()) ? Result.CANCEL : Result.SUCCESS);
            }
            else {
               logger.warn("Skipping calculate targets");
               session.setProgress(false);
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception in CalculateTargets: ", e);
            session.setProgress(false);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.CALCULATE_TARGETS_ERROR, Throwables.getRootCause(e).getMessage()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }
   }

   private class CalculateConflictsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Calculate Conflicts...");
         DataManagementSession session = params[0];
         session.setProgress(true);
         try {
            Address[] addresses = getAddresses(session.getDestinations());
            if (addresses == null || addresses.length > 0) {
               session.setData(mediator.calculateConflicts(session.getData(), addresses));
               session.setResult(Result.CANCEL.equals(session.getResult()) ? Result.CANCEL : Result.SUCCESS);
            }
            else {
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception: ", e);
            session.setProgress(false);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.CALCULATE_CONFLICT_ERROR, Throwables.getRootCause(e).getMessage()));
            session.setResult(Process.Result.ERROR);
         }
         return session;
      }
   }

   private class PerformOperationsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Performing Operations...");
         DataManagementSession session = params[0];
         session.setProgress(true);
         try {
            if (!resolveSources(session) || !resolveTargets(session)) {
               logger.debug("Unable to resolve source/target addresses");
               session.setResult(Result.CANCEL);
               session.setProgress(false);
               return session;
            }
            if (session.getData() == null) {
               session.setData(new ArrayList<Operation>());
               for (ObjectGraph obj : session.getObjectData()) {
                  session.getData().add(new Operation(obj, null));
               }
            }
            Address[] addresses = getAddresses(session.getDestinations());
            if (addresses != null && addresses.length > 0) {
               session.setResults(mediator.performOperations(session.getData(), addresses));
               boolean hasCancelled = Result.CANCEL.equals(session.getResult());
               for (Rsp<Process> ret : session.getResults()) {
                  if (ret.hasException()) throw ret.getException();
                  if (ret.wasReceived() && ret.getValue() != null && ret.getValue().getResult() != null) {
                     hasCancelled |= Result.CANCEL.equals(ret.getValue().getResult());
                  }
                  else {//suspect/unreachable
                     session.setProgress(false);//possible fire will happen before view change
                     globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR));
                     session.setResult(Result.ERROR);
                  }
               }

               session.setResult(hasCancelled ? Result.CANCEL : Result.SUCCESS);
            }
            else {
               session.setProgress(false);
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception in PerformOperation:", e);
            session.setResult(Process.Result.ERROR);
            session.setProgress(false);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR, Throwables.getRootCause(e).toString()));

         }
         return session;
      }

      @Override
      protected void onPostExecute(DataManagementSession session) {
         logger.debug("PerformTask calling completeOperation");
         completeOperation(session);
         super.onPostExecute(session);
      }
   }

   private void completeOperation(final DataManagementSession session) {
      logger.info("perform ops finished {}", session.getResult().name());
      if (session.getResult().equals(Process.Result.ERROR)) {
         sendStatus(session, "Error");
      }
      else if (session.getResult().equals(Process.Result.CANCEL)) {
         sendStatus(session, statusCancelled);
      }
      else if (session.getResult().equals(Result.SUCCESS)) {
         sendStatus(session, statusSuccessful);
      }
      if (isUsbExport(session)) {//stop USB datasources after export
         new StopTask().execute(getAddresses(session.getDestinations()));
      }

      final Status status = activeStatusUpdateSessions.remove(session);
      handler.postDelayed(new Runnable() {
         @Override
         public void run() {
            if (status != null) {
               removeStatus(status);
            }
         }
      }, KILL_STATUS_DELAY);
   }

   private void sendStatus(DataManagementSession session, int res, Object... args) {
      if (args != null) {
         sendStatus(session, getResources().getString(res, args));
      }
      else {
         sendStatus(session, getResources().getString(res));
      }
   }

   private void sendStatus(DataManagementSession session, String message) {
      sendStatus(session, activeStatusUpdateSessions.get(session), message);
   }

   private void sendStatus(DataManagementSession session, Status status, String message) {
      if (status == null) return;
      sendStatus(status, new StringBuffer(isUsbImport(session) ? importing : exporting).append(" ").append(message).toString());
   }

   private void sendStatus(Status s, String msg) {
      if (s == null) return;
      s.setMessage(msg);
      removeStatus(s);
      sendBroadcast(new Intent(Status.ACTION_STATUS_DISPLAY).putExtra(Status.NAME, s));
   }

   private void removeStatus(Status s) {
      sendBroadcast(new Intent(Status.ACTION_STATUS_REMOVE).putExtra(Status.ID, ParcelUuid.fromString(s.getID().toString())));
   }

   private class CancelTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Cancelling...");
         DataManagementSession session = params[0];
         session.setProgress(true);
         try {
            Address[] targetAddresses = getAddresses(session.getDestinations());
            Address[] sourceAddresses = getAddresses(session.getSources());
            //if process already running tell it to cancel.
            if (sourceAddresses.length > 0 && performCalled) {
               logger.debug("Telling process to cancel");
               mediator.cancel(sourceAddresses);
               if (targetAddresses.length > 0) {
                  mediator.cancel(targetAddresses);
               }
            }
            else {
               logger.debug("Perform not called yet setting result manually to cancel");
               session.setResult(Process.Result.CANCEL);
            }
         }
         catch (Exception e) {
            logger.error("Send exception: ", e);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR, Throwables.getRootCause(e).getMessage()));
         }
         return session;
      }

      @Override
      protected void onPostExecute(DataManagementSession session) {
         //only call super and fire event if we caught the datasource before it started working.
         //otherwise the performOperations call itself will return the canceled status.
         if (Process.Result.CANCEL.equals(session.getResult()) && !performCalled) {
            logger.debug("CancelTask calling completeOperation");
            completeOperation(session);
         }
         else {
            sendStatus(session, statusCancelling);
         }
         session.setProgress(false);
      }
   }
   
   private class DeleteOperationsTask extends SessionOperationTask<Void>{
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Deleting Operations...");
         DataManagementSession session = params[0];
         session.setProgress(true);
         try {
            if (session.getData() == null && session.getObjectData() != null) {
               session.setData(new ArrayList<Operation>());
               for (ObjectGraph obj : session.getObjectData()) {
                  Operation operation = new Operation(obj, null);
                  operation.setStatus(Operation.Status.NOT_DONE);
                  session.getData().add(operation);
               }
            }

            session.setResults(mediator.deleteOperations(session.getData(), null));
            boolean error = false;
            for (Rsp<Process> ret : session.getResults()) {
               if (ret.hasException() || !ret.getValue().getResult().equals(Result.SUCCESS)){
                  if(ret.hasException()){
                     logger.error("exception occur in PCM when deleting;{}",ret.getException());
                  }
                  error = true;
                  break;
               }
            }
            session.setResult(error ? Result.ERROR : Result.SUCCESS);
         }
         catch (Throwable e) {
            logger.error("exception occur when deleting;{}",e);
            session.setResult(Result.ERROR);
         }
         return session;
      }
   }
   private class UpdateOperationsTask extends SessionOperationTask<Void> {
      @Override
      protected DataManagementSession doInBackground(DataManagementSession... params) {
         logger.debug("Update Operations...");
         DataManagementSession session = params[0];
         session.setProgress(true);
         try {
            if (session.getData().get(0).getData().getSource().size() > 0) {
               session.setResults(mediator.updateOperations(session.getData(), session.getData().get(0).getData().getSource()));
               boolean hasCancelled = Result.CANCEL.equals(session.getResult());
               for (Rsp<Process> ret : session.getResults()) {
                  if (ret.hasException()) throw ret.getException();
                  if (ret.wasReceived() && ret.getValue() != null && ret.getValue().getResult() != null) {
                     hasCancelled |= Result.CANCEL.equals(ret.getValue().getResult());
                  }
                  else {//suspect/unreachable
                     globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR));
                     session.setResult(Result.ERROR);
                  }
               }
               if (hasCancelled) {
                  session.setResult(Result.CANCEL);
               }
               else {
                  session.setResult(Result.SUCCESS);
               }
            }
            else {
               globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.NO_TARGET_DATASOURCE));
               session.setResult(Process.Result.NO_DATASOURCE);
            }
         }
         catch (Throwable e) {
            logger.error("Send exception in UpdateOperation:", e);
            session.setResult(Process.Result.ERROR);
            globalEventManager.fire(new ErrorEvent(session, ErrorEvent.DataError.PERFORM_ERROR, Throwables.getRootCause(e).toString()));

         }
         return session;
      }

      @Override
      protected void onPostExecute(DataManagementSession session) {
         logger.debug("PerformTask calling completeOperation");
         completeOperation(session);
         super.onPostExecute(session);
      }
   }

   private class StopTask extends AsyncTask<Address, Void, Void> {
      @Override
      protected Void doInBackground(Address... params) {
         logger.debug("Stopping...");
         try {
            mediator.stop(params);
         }
         catch (Exception e) {
            logger.error("Stop exception", e);
         }
         return null;
      }
   }

   private abstract class SessionOperationTask<Progress> extends AsyncTask<DataManagementSession, Progress, DataManagementSession> {
      @Override
      protected void onPostExecute(DataManagementSession session) {
         super.onPostExecute(session);
         session.setProgress(false);
         logger.debug("onPostExecute: {}", this.getClass().getSimpleName());
         if (session.getResult() != Result.ERROR) {
            for (IDataManagementListenerAIDL listener : listeners.values()) {
               try {
                  listener.onDataSessionUpdated(session);
               }
               catch (RemoteException e) {
                  logger.error("", e);
               }
            }
         }
      }
   }
}
