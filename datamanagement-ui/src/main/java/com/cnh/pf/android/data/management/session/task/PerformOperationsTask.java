/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.task;

import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import com.cnh.jgroups.Mediator;
import com.cnh.pf.android.data.management.fault.DMFaultHandler;
import com.cnh.pf.android.data.management.fault.FaultCode;
import com.cnh.pf.android.data.management.parser.FormatManager;
import com.cnh.pf.android.data.management.session.ErrorCode;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionExtra;
import com.cnh.pf.android.data.management.session.SessionNotifier;
import com.cnh.pf.android.data.management.session.SessionUtil;
import com.cnh.pf.android.data.management.session.StatusSender;
import com.cnh.pf.android.data.management.utility.UtilityHelper;
import com.cnh.pf.datamng.Process;
import com.google.common.io.Files;
import org.jgroups.Address;
import org.jgroups.util.Rsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Session task class to execute PERFORM_OPERATIONS session
 *
 * @author: junsu.shin@cnhind.com
 */
public class PerformOperationsTask extends SessionOperationTask<Void> {
   private static final Logger logger = LoggerFactory.getLogger(PerformOperationsTask.class);
   public static final int KILL_STATUS_DELAY = 5000;
   private final StatusSender statusSender;
   private final DMFaultHandler faultHandler;
   private final FormatManager formatManager;
   private final String tempPath = UtilityHelper.CommonPaths.PATH_TMP.getPathString();

   public PerformOperationsTask(@Nonnull Mediator mediator, @Nonnull SessionNotifier notifier, @Nonnull DMFaultHandler faultHandler, @Nonnull FormatManager formatManager, StatusSender statusSender) {
      super(mediator, notifier);
      this.faultHandler = faultHandler;
      this.formatManager = formatManager;
      this.statusSender = statusSender;
   }

   @Override
   protected void processSession(@Nonnull Session session) throws SessionException {
      logger.debug("{}:processSession()", this.getClass().getSimpleName());
      try {
         if (statusSender != null) {
            statusSender.sendStartingStatus(SessionUtil.isExportAction(session));
         }
         Address[] addresses = session.getDestinations().toArray(new Address[0]);
         if (addresses != null && addresses.length > 0) {
            logger.debug("PerformOperations-Dst Addresses: {}", SessionUtil.addressToString(addresses));

            session.setResults(getMediator().performOperations(session.getOperations(), addresses));
            boolean hasCancelled = Process.Result.CANCEL.equals(session.getResultCode());

            for (Rsp<Process> ret : session.getResults()) {
               if (ret.hasException()) throw ret.getException();
               if (ret.wasReceived() && ret.getValue() != null && ret.getValue().getResult() != null) {
                  hasCancelled |= Process.Result.CANCEL.equals(ret.getValue().getResult());
               }
               else {
                  logger.debug("Either the session results have no value or the value itself is null. ({})", session.getAction());
                  session.setResultCode(Process.Result.ERROR);
                  throw new SessionException(ErrorCode.PERFORM_ERROR);
               }
            }

            if (hasCancelled) {
               session.setResultCode(Process.Result.CANCEL);
            }
            else {
               SessionExtra extra = session.getExtra();
               if (extra != null && extra.isUsbExtra() && SessionUtil.isExportAction(session)) {
                  boolean moveWasSuccessfull = false;
                  File tmpFolder = new File(tempPath);

                  if (extra.isUseInternalFileSystem()) {
                     moveWasSuccessfull = moveFilesToInternalFlash(session.getExtra());
                     if (moveWasSuccessfull) {
                        session.setResultCode(Process.Result.SUCCESS);
                     }
                     else {
                        session.setResultCode(Process.Result.ERROR);
                        throw new SessionException(ErrorCode.PERFORM_ERROR);
                     }
                  }
                  else {
                     final String USB_EXPORT_PATH = UtilityHelper.CommonPaths.PATH_USB_PORT.getPathString();

                     final String PFDATABASE_FOLDER =
                        UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + formatManager.getFormat(UtilityHelper.CommonFormats.PFDATABASEFORMAT.getName()).path + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();

                     final String ISOXML_FOLDER =
                        UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + formatManager.getFormat(UtilityHelper.CommonFormats.ISOXMLFORMAT.getName()).path + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();

                     if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED) && tmpFolder.exists()) {

                        if (extra.getFormat().equals(UtilityHelper.CommonFormats.PFDATABASEFORMAT.getName())) {
                           logger.info("start moving files from: {} to {}{}", tempPath, USB_EXPORT_PATH, PFDATABASE_FOLDER);
                           moveWasSuccessfull = moveFiles(tempPath, USB_EXPORT_PATH, PFDATABASE_FOLDER);
                        }
                        else if (extra.getFormat().equals(UtilityHelper.CommonFormats.ISOXMLFORMAT.getName())) {
                           logger.info("start moving files from: {} to {}{}", tempPath, USB_EXPORT_PATH, ISOXML_FOLDER);
                           moveWasSuccessfull = moveFiles(tempPath, USB_EXPORT_PATH, ISOXML_FOLDER);
                        }
                        else {
                           logger.info("Unknown destination on export");
                        }

                        logger.info("finished moving files");

                        session.setResultCode( (moveWasSuccessfull) ? Process.Result.SUCCESS : Process.Result.ERROR);
                     }
                     else {
                        logger.info("Either USB is not mounted or temporary folder doesn't exist.");
                        faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).reset();
                        faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).alert();
                        session.setResultCode(Process.Result.ERROR);
                     }
                  }

                  if (tmpFolder.exists()) {
                     if (!UtilityHelper.deleteRecursively(tmpFolder)){
                        logger.error("unable to delete temporary folder:{}", tmpFolder.getPath());
                     }
                  }

                  if (SessionUtil.isErroneous(session)) {
                     throw new SessionException(ErrorCode.PERFORM_ERROR);
                  }
               }
               else {
                  session.setResultCode(Process.Result.SUCCESS);
               }
            }
         }
         else {
            session.setResultCode(Process.Result.ERROR);
            throw new SessionException(ErrorCode.NO_DESTINATION_DATASOURCE);
         }
      }
      catch (Throwable e) {
         logger.error("Exception in PERFORM_OPERATIONS: ", e);
         session.setResultCode(Process.Result.ERROR);
         throw new SessionException(ErrorCode.PERFORM_ERROR);
      }
   }

   @Override
   protected void onPostExecute(Session session) {
      if (statusSender != null) {
         boolean exporting = SessionUtil.isExportAction(session);

         if (SessionUtil.isErroneous(session)) {
            statusSender.sendStatus("Error", exporting);
         } else if (SessionUtil.isCancelled(session)) {
            statusSender.sendCancelledStatus(exporting);
         } else if (SessionUtil.isSuccessful(session)) {
            statusSender.sendSuccessfulStatus(exporting);
         }

         new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
               statusSender.removeStatus();
            }
         }, KILL_STATUS_DELAY);
      }

      // Call super.onPostExecute() after finishing status notification.
      // Session data get resets in the super.onPostExecute() call.
      super.onPostExecute(session);
   }

   private boolean moveFiles(String sourceDir, String destRoot, String destDir) {

      boolean retValue = false;

      File source = new File(sourceDir);
      ArrayDeque<File> fileList = new ArrayDeque<File>();

      File dest = new File(destRoot + destDir);
      long exportSize = 0;

      if ((null != source) && source.exists()) {

         File[] paths = source.listFiles();
         if (null != paths) {
            for (File path : paths) {
               fileList.push(path);
               exportSize += path.length();
            }
         }
         logger.debug("exporting source: ", source.getPath());
         logger.debug("exporting destination: ", dest.getPath());

         StatFs stat = new StatFs(destRoot);
         long bytesFree = (long) stat.getAvailableBlocksLong() * (long) stat.getBlockCount();

         boolean isMounted = Environment.getExternalStorageState().equals(MEDIA_MOUNTED);

         if (exportSize == 0) {
            logger.info("no source file found");
         }
         else if (bytesFree < exportSize) {
            // reset before showing again
            faultHandler.getFault(FaultCode.USB_NOT_ENOUGH_MEMORY).reset();
            faultHandler.getFault(FaultCode.USB_NOT_ENOUGH_MEMORY).alert();
            logger.info("not enough space on USB stick");
         }
         else if (!isMounted) {
            // if the USB does not exist anymore, drop a USB removed alert
            faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).reset();
            faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).alert();
            logger.info("USB stick is not mounted or has been removed");
         }
         else {
            // check, if the folder on the USB stick is still existing and rename the old folder
            if (dest.exists()) {
               logger.info("Renaming existing export destination folder");
               File copy = new File(dest.getAbsolutePath());
               copy.renameTo(new File(String.format("%s.%s", dest.getAbsolutePath(), new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(dest.lastModified())))));
            }
            dest.mkdirs();

            String root;

            try {
               root = source.toString();
               File[] temppath = source.listFiles();
               File next;
               File destination;
               File parent;
               logger.info("start copy {} files", fileList.size());

               File destFolder = new File(destRoot);

               while (!fileList.isEmpty()) {
                  next = fileList.pop();
                  logger.info("copy:{}", next.getPath());
                  if (next.isFile()) {
                     destination = new File(String.format("%s%s", dest, next.toString().substring(root.length())));
                     parent = destination.getCanonicalFile().getParentFile();
                     if (!parent.exists()) {
                        parent.mkdirs();
                     }
                     Files.move(next, destination);
                     retValue = true;
                  }
                  else if (next.isDirectory()) {
                     if (null != next.listFiles()) {
                        for (File file : next.listFiles()) {
                           fileList.push(file);
                        }
                        retValue = true;
                     }
                  }
               }
               logger.info("finished copy files");
            }
            catch (Exception e) {
               if (!Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
                  faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).reset();
                  faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).alert();
               }
               logger.error("", e);
            }
         }
      }
      else {
         logger.error("no source folder found");
      }
      return retValue;
   }

   private boolean moveFilesToInternalFlash(SessionExtra extra) {
      boolean moveStatus = false;
      File source = new File(tempPath);
      File dest = new File(extra.getPath());
      final String PFDATABASE_FOLDER =
         UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + formatManager.getFormat(UtilityHelper.CommonFormats.PFDATABASEFORMAT.getName()).path +
            UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();
      final String ISOXML_FOLDER = UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + formatManager.getFormat(UtilityHelper.CommonFormats.ISOXMLFORMAT.getName()).path
         + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();

      if (dest.exists()) {
         logger.info("Renaming existing export destination folder");
         File copy = new File(dest.getAbsolutePath());
         copy.renameTo(new File(String.format("%s.%s", dest.getAbsolutePath(), new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(dest.lastModified())))));
      }
      dest.mkdirs();

      try {
         File[] fileList = source.listFiles();
         if (fileList != null) {
            logger.info("start copy {} files", fileList.length);
            copyFileorFolders(source.getPath(), extra.getPath());
         }
         logger.info("finished copy files");
         moveStatus = true;
      }
      catch (Exception e) {
         if (!Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).reset();
            faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).alert();
         }
         logger.error("", e);
      }
      return moveStatus;
   }

   void copyFileorFolders(String srcFolder, String destFolder) throws IOException {
      File src = new File(srcFolder);
      File dest = new File(destFolder);
      if(src.isDirectory()) {
         String files[] = src.list();
         for (String file: files) {
            String srcFile = srcFolder + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + file;
            copyFileorFolders(srcFile, destFolder);
         }
      }
      else{
         String destFile = destFolder + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + src.getName();
         Files.move(src, new File(destFile));
      }
   }
}