/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.service;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.Mediator;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.android.data.management.RoboModule;
import com.cnh.pf.android.data.management.fault.DMFaultHandler;
import com.cnh.pf.android.data.management.fault.FaultCode;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.session.CacheManager;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionContract;
import com.cnh.pf.android.data.management.session.SessionEventListener;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.android.data.management.session.StatusSender;
import com.cnh.pf.android.data.management.session.resolver.Resolver;
import com.cnh.pf.android.data.management.session.resolver.ResolverFactory;
import com.cnh.pf.android.data.management.session.task.CancelTask;
import com.cnh.pf.android.data.management.session.task.SessionOperationTask;
import com.cnh.pf.data.management.service.ServiceConstants;
import com.cnh.pf.datamng.Process.Result;
import com.cnh.pf.jgroups.ChannelModule;
import com.google.inject.name.Named;

import org.jgroups.Address;
import org.jgroups.Global;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import roboguice.RoboGuiceHelper;
import roboguice.service.RoboService;

/**
 * Service manages a DataManagementSession. Keeps one session alive until completion.
 * Notifies listeners of progress and provides state information to UI.
 * Accepts operations from UI, and relays them to destination datasource.
 * @author oscar.salazar@cnhind.com
 */
public class DataManagementService extends RoboService implements SharedPreferences.OnSharedPreferenceChangeListener, SessionNotifier {
   private static final Logger logger = LoggerFactory.getLogger(DataManagementService.class);

   public static final String ACTION_RESET_CACHE = "com.cnh.pf.data.management.RESET_CACHE";
   public static final String ACTION_CANCEL = "com.cnh.pf.data.management.CANCEL";

   public static final int KILL_STATUS_DELAY = 5000;

   @Inject
   private Mediator mediator;
   @Inject
   private DatasourceHelper dsHelper;
   @Named("global")
   @Inject
   private SharedPreferences prefs;
   @Inject
   private FormatManager formatManager;
   @Inject
   private DMFaultHandler faultHandler;
   @Inject
   private NotificationManager notifyManager;
   @Inject
   private UsbServiceManager usbServiceManager;
   @Inject
   private CacheManager cacheManager;

   // This is to maintain reference to current active session.
   private AtomicReference<Session> activeSessionRef = new AtomicReference<Session>();
   // This is to maintain reference to current active session view.
   private AtomicReference<SessionContract.SessionView> activeViewRef = new AtomicReference<SessionContract.SessionView>();

   /**
    * Setter for currently active session
    *
    * @param session    the session
    */
   public void setActiveSession(final Session session) {
      activeSessionRef.set(session);
   }

   /**
    * Getter for currently active session
    * @return  active session
    */
   public Session getActiveSession() {
      return activeSessionRef.get();
   }

   private boolean hasActiveSession() {
      if (getActiveSession() != null) {
         final Session session = getActiveSession();
         return SessionUtil.isDiscoveryTask(session) || SessionUtil.isPerformOperationsTask(session) || SessionUtil.isCalculateConflictsTask(session)
               || SessionUtil.isCalculateOperationsTask(session);
      }
      return false;
   }

   /**
    * Setter for currently active session view
    *
    * @param view session view
    */
   public void setActiveView(final SessionContract.SessionView view) {
      activeViewRef.set(view);
   }

   /**
    * Getter for currently active session view
    * @return  session view
    */
   public SessionContract.SessionView getActiveView() {
      return activeViewRef.get();
   }

   private List<SessionEventListener> sessionEventListeners = new CopyOnWriteArrayList<SessionEventListener>();
   private Handler handler = new Handler();
   private final IBinder localBinder = new LocalBinder();

   private boolean useInternalFileSystem = false;

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      logger.debug("onStartCommand {}", intent);

      if (intent == null) { //happens after restart when service is sticky
         return START_STICKY;
      }
      else if (intent.getAction().equals(ACTION_RESET_CACHE)) {
         if (cacheManager != null) {
            logger.trace("Cache is requested to reset by ACTION_RESET_CACHE action.");
            cacheManager.resetAll();
         }
         return START_STICKY;
      }

      if (Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) {
         notifyMediumUpdate();
      }
      else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction()) || Intent.ACTION_MEDIA_BAD_REMOVAL.equals(intent.getAction())
            || Intent.ACTION_MEDIA_REMOVED.equals(intent.getAction()) || Intent.ACTION_MEDIA_EJECT.equals(intent.getAction())) {
         logger.info("Media has been unmounted");
         if (hasActiveSession() && Intent.ACTION_MEDIA_BAD_REMOVAL.equals(intent.getAction())) {
            logger.debug("Bad USB removal during task");
            // If USB stick gets pulled out during the task (discovery, perform operation),
            // the current ongoing session needs to be cancelled and alert corresponding fault.
            final Session curSession = getActiveSession();
            logger.debug("Current active session: {}, {}", curSession.getType(), curSession.getAction());

            if (SessionUtil.isInProgress(curSession) && (curSession.getExtra() != null && curSession.getExtra().isUsbExtra())) {
               logger.debug("USB unplugged while interacting with datasources. Cancel the current session.");
               cancelSession(curSession);
               curSession.setResultCode(Result.ERROR);
               FaultCode faultCode = SessionUtil.isExportAction(curSession) ? FaultCode.USB_REMOVED_DURING_EXPORT : FaultCode.USB_REMOVED_DURING_IMPORT;
               DMFaultHandler.Fault fault = faultHandler.getFault(faultCode);
               fault.reset();
               fault.alert();
            }
            else if (SessionUtil.isImportAction(curSession) && (curSession.getObjectData() != null && curSession.getObjectData().size() > 0) && getActiveView() != null
                  && (SessionUtil.isDiscoveryTask(curSession) || SessionUtil.isCalculateConflictsTask(curSession))) {
               logger.debug("USB unplugged while waiting for the user response (discovery/conflict resolution).");
               curSession.setResultCode(Result.ERROR);
               notifySessionError(curSession, ErrorCode.USB_REMOVED);
               curSession.setType(Session.Type.DISCOVERY);
               DMFaultHandler.Fault fault = faultHandler.getFault(FaultCode.USB_REMOVED_DURING_IMPORT);
               fault.reset();
               fault.alert();
            }
         }

         notifyMediumUpdate();
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
      logger.debug("onCreate(): Create DM service");
      final Application app = getApplication();
      //Phoenix Workaround (phoenix sometimes cannot read the manifest)
      RoboGuiceHelper.help(app, new String[] { "com.cnh.pf.android.data.management", "com.cnh.pf.jgroups" }, new RoboModule(app), new ChannelModule(app));
      super.onCreate();
      try {
         prefs.registerOnSharedPreferenceChangeListener(this);
         faultHandler.setMediator(mediator);
         mediator.setProgressListener(pListener);

         new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  System.setProperty(Global.IPv4, "true");
                  mediator.start();
                  logger.debug("Mediator started, notify the listeners");
               }
               catch (Exception e) {
                  logger.error("Failure at starting Mediator: ", e);
               }
            }
         }).start();
      }
      catch (Exception e) {
         logger.error("error in onCreate", e);
      }
      usbServiceManager.connect();
   }

   @Override
   public void onDestroy() {
      logger.debug("onDestroy(): Destroy DM service");
      sendBroadcast(new Intent(ServiceConstants.ACTION_INTERNAL_DATA_STOP).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));

      sessionEventListeners.clear();
      prefs.unregisterOnSharedPreferenceChangeListener(this);

      // Stop USB datasources if there are any running
      usbServiceManager.stopAllDatasources();
      usbServiceManager.disconnect();
      new Thread(new Runnable() {
         @Override
         public void run() {
            mediator.close();
         }
      });
      super.onDestroy();
   }

   /**
    * Register a session event listener
    *
    * @param listener   listener to be registered
    */
   public void registerSessionEventListener(SessionEventListener listener) {
      if (!sessionEventListeners.contains(listener)) {
         sessionEventListeners.add(listener);
      }
   }

   /**
    * Unregister session event listener
    *
    * @param listener   listener to be unregistered
    */
   public void unregisterSessionEventListener(SessionEventListener listener) {
      sessionEventListeners.remove(listener);
   }

   /**
    * Execute a session.
    *
    * @param session    the session
    */
   private void executeSession(final Session session) {
      Resolver resolver = ResolverFactory.createResolver(dsHelper, session);

      try {
         if (resolver != null) {
            resolver.resolve(session);
         }
         else {
            logger.error("Unable to create proper Resolver for session {}.", session.getType());
            return;
         }

         SessionOperationTask task = new SessionOperationTask.TaskBuilder().mediator(mediator).session(session).notifier(this)
               .statusSender(new StatusSender(getApplicationContext())).formatManager(formatManager).faultHandler(faultHandler).build();

         if (task != null) {
            task.executeTask(session);
         }
         else {
            logger.error("No session task has been created.");
         }
      }
      catch (SessionException e) {
         logger.debug("Caught SessionException: {}", e.getErrorCode().toString());
         session.setResultCode(Result.ERROR);
         notifySessionError(session, e.getErrorCode());
      }
      catch (IllegalStateException e) {
         logger.error("Failure at executeSession(): {}", e);
         session.setResultCode(Result.ERROR);
         notifySessionError(session, ErrorCode.NO_DATA);
      }
   }

   /**
    * Process session object. Pre-configuration & session execution & notification will be processed.
    *
    * @param session    the session object.
    */
   public void processSession(final Session session) {
      logger.debug("Service.processSession: {}", session.getType());

      setActiveSession(session);
      // Use cached data only for DISCOVERY task here
      if (SessionUtil.isDiscoveryTask(session) && !SessionUtil.isImportAction(session) && cacheManager.cached(session)) {
         logger.trace("Found cached data for session: {}, {}", session.getAction(), session.getType());
         CacheManager.CacheItem cItem = cacheManager.retrieve(session);
         List<ObjectGraph> objectGraphs = cItem.getObjectGraph();
         List<Operation> operations = cItem.getOperations();

         // Set the state & result code to get passed the notifier
         session.setState(Session.State.COMPLETE);
         session.setResultCode(Result.SUCCESS);
         session.setObjectData(objectGraphs);
         session.setOperations(operations);
         notifySessionSuccessWithoutCaching(session);
         return;
      }

      if (SessionUtil.isImportAction(session) && SessionUtil.isPerformOperationsTask(session)) {
         // Clear cached discovery data for EXPORT & MANAGE. This is to defend any data change in
         // IMPORT process even if the process is not successfully finished.
         cacheManager.reset(Session.Action.EXPORT, Session.Type.DISCOVERY);
         cacheManager.reset(Session.Action.MANAGE, Session.Type.DISCOVERY);
      }

      if ((SessionUtil.isExportAction(session) && SessionUtil.isPerformOperationsTask(session)) || (SessionUtil.isImportAction(session) && SessionUtil.isDiscoveryTask(session))) {
         // Run USB service code in new thread to show UI change faster
         new Thread(new Runnable() {
            @Override
            public void run() {
               if (usbServiceManager.datasourceReady(session.getExtra())) {
                  logger.trace("Found datasource for {}", session.getExtra());
                  // For the usb datasource services, starting datasources is also considered to be
                  // 'SESSION IN PROGRESS'.
                  session.setState(Session.State.IN_PROGRESS);
                  usbServiceManager.stopAllDatasourceBeforeStart();
                  if (usbServiceManager.startUsbDatasource(session.getExtra(), SessionUtil.isExportAction(session))) {
                     executeSession(session);
                  }
                  else {
                     logger.debug("Unable to start datasource: {}, {}", session.getType(), session.getExtra());
                     session.setResultCode(Result.CANCEL);
                     session.setState(Session.State.COMPLETE);
                     notifySessionCancel(session);
                  }
               }
               else {
                  logger.error("No datasource has been found for {}", session.getExtra());
                  session.setResultCode(Result.ERROR);
                  session.setState(Session.State.COMPLETE);
                  notifySessionError(session, ErrorCode.INVALID_FORMAT);
               }
            }
         }).start();
      }
      else {
         executeSession(session);
      }
   }

   /**
    * Cancel a session operation.
    * @param session the session to cancel.
    */
   public void cancelSession(Session session) {
      logger.debug("cancelSession(): {}, {}", session.getType(), session.getAction());

      final Session activeSession = getActiveSession();
      if (activeSession != null && activeSession.getUuid().equals(session.getUuid())) {
         new CancelTask(mediator).executeTask(activeSession);
      }
   }

   /**
    * Return true if PCM is online. If one of PCM datasources (pfdsd, vipd) is connected, then PCM is
    * considered to be online.
    *
    * @return True if PCM is online
    */
   public boolean isPCMOnline() {
      List<Address> internalAddresses = dsHelper.getAddressesForLocation(Datasource.LocationType.PCM);
      return !internalAddresses.isEmpty();
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
         logger.debug("publishProgress({}, {}, {})", operation, progress, max);
         handler.post(new Runnable() {
            @Override
            public void run() {
               notifyProgressUpdate(operation, progress, max);
            }
         });
      }

      @Override
      public void onSecondaryProgressPublished(String secondaryOperation, int secondaryProgress, int secondaryMax) {
         logger.trace("onSecondaryProgressPublished()");
      }

      @Override
      public void onViewAccepted(View newView) {
         logger.debug("onViewAccepted {}", newView);
         dsHelper.updateView(newView, new DatasourceHelper.onConnectionChangeListener() {
            @Override
            public void onConnectionChange(Address[] left, Address[] join, boolean updateNeeded) {
               notifyChannelConnectionChange(updateNeeded);
            }
         });
      }
   };

   /**
    * Notify session success to registered callbacks without caching session data.
    *
    * @param session    the session object
    */
   private void notifySessionSuccessWithoutCaching(Session session) {
      logger.debug("notifySessionSuccessWithoutCaching()");
      for (SessionEventListener listener : this.sessionEventListeners) {
         listener.onSessionSuccess(session);
      }
   }

   /**
    * Notify session success to registered callbacks.
    *
    * @param session    the session object
    */
   public void notifySessionSuccess(Session session) {
      logger.debug("notifySessionSuccess()");
      // Cache session result so that it can be recycled later.
      if (SessionUtil.isComplete(session) && SessionUtil.isSuccessful(session)) {
         cacheManager.save(session);
      }

      notifySessionSuccessWithoutCaching(session);
   }

   /**
    * Notify session error to registered callbacks.
    *
    * @param session    the session object
    * @param errCode    error code
    */
   public void notifySessionError(final Session session, final ErrorCode errCode) {
      logger.debug("notifySessionError()");
      new Handler(Looper.getMainLooper()).post(new Runnable() {
         @Override
         public void run() {
            for (SessionEventListener listener : sessionEventListeners) {
               listener.onSessionError(session, errCode);
            }
         }
      });
   }

   /**
    * Notify session cancellation to registered callbacks.
    *
    * @param session    the session object
    */
   public void notifySessionCancel(Session session) {
      logger.debug("notifySessionCancel()");
      for (SessionEventListener listener : this.sessionEventListeners) {
         listener.onSessionCancelled(session);
      }
   }

   /**
    * Notify progress update during session execution.
    *
    * @param operation  the current operation
    * @param progress   current progress
    * @param max        total progress
    */
   public void notifyProgressUpdate(String operation, int progress, int max) {
      for (SessionEventListener listener : this.sessionEventListeners) {
         listener.onProgressUpdate(operation, progress, max);
      }
   }

   /**
    * Notify change in the datasource channel.
    *
    * @param updateNeeded  True if view update is required.
    */
   public void notifyChannelConnectionChange(final boolean updateNeeded) {
      logger.debug("notifyChannelConnectionChange()");
      if (updateNeeded) {
         // Cache needs to be reset since there is a change in datasource.
         logger.trace("Reset cache upon the channel connection change.");
         cacheManager.resetAll();
      }

      // Run the callback on the main UI thread to allow UI work.
      new Handler(Looper.getMainLooper()).post(new Runnable() {
         @Override
         public void run() {
            for (SessionEventListener listener : sessionEventListeners) {
               listener.onChannelConnectionChange(updateNeeded);
            }
         }
      });
   }

   /**
    * Notify medium update event (ie: USB mount/unmount/bad removal)
    */
   public void notifyMediumUpdate() {
      logger.debug("notifyMediumUpdate()");
      if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || Environment.getExternalStorageState().equals(Environment.MEDIA_UNMOUNTED)
            || Environment.getExternalStorageState().equals(Environment.MEDIA_BAD_REMOVAL)) {
         // Reset cached item for import process when USB mounting status changes.
         cacheManager.reset(Session.Action.IMPORT);
      }

      for (SessionEventListener listener : this.sessionEventListeners) {
         listener.onMediumUpdate();
      }
   }
}