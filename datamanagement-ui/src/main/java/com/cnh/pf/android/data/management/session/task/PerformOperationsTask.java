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
import com.cnh.pf.util.ZipHelper;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.protocols.FILE_FRAG2;
import org.jgroups.util.Rsp;
import org.jgroups.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
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
   private final SessionNotifier notifier;
   private final String tempPath = UtilityHelper.CommonPaths.PATH_TMP.getPathString();

   protected String   filename;

   public PerformOperationsTask(@Nonnull Mediator mediator, @Nonnull SessionNotifier notifier, @Nonnull DMFaultHandler faultHandler, @Nonnull FormatManager formatManager, StatusSender statusSender) {
      super(mediator, notifier);
      this.faultHandler = faultHandler;
      this.formatManager = formatManager;
      this.statusSender = statusSender;
      this.notifier = notifier;
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
            Process.Result resultCode = session.getResultCode();
            boolean hasCancelled = Process.Result.CANCEL.equals(resultCode);
            boolean hasError = Process.Result.ERROR.equals(resultCode);

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

            if (hasCancelled && !hasError) {
               if (!Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
                  session.setResultCode(Process.Result.ERROR);
                  throw new SessionException(ErrorCode.USB_REMOVED);
               }
               else {
                  session.setResultCode(Process.Result.CANCEL);
               }
            }
            else {
               SessionExtra extra = session.getExtra();

               if(extra.isCloudExtra()) {
                  moveFilesToPCM(session, getCloudDestination(addresses));
               }
               else {
                  moveFilesToUSB(session, extra);
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
         if (!Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            throw new SessionException(ErrorCode.USB_REMOVED);
         }
         else {
            throw new SessionException(ErrorCode.PERFORM_ERROR);
         }
      }
   }

   private org.jgroups.Address getCloudDestination(Address[] addresses) {
      for (Address addr : addresses) {
         logger.info("cloud destination: {}", addr.toString());
         if (addr.toString().contains("CLOUD")) {
            return addr;
         }
      }
      return null;
   }

   private void moveFilesToPCM(@Nonnull Session session, Address cloudAddress) throws IOException, SessionException {

      final String PATH_TMP_CLOUD = UtilityHelper.CommonPaths.PATH_TMP_CLOUD.getPathString();

      final String TASKDATA_FOLDER = UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + "TASKDATA" +
                                 UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();

      final File zippedCloudDir = new File(PATH_TMP_CLOUD + TASKDATA_FOLDER);
      File tmpFolder = new File(tempPath);

      if (tmpFolder.exists()) {

         FileUtils.deleteQuietly(zippedCloudDir);

         logger.info("moving cloud files from: {} to {}{}", tempPath, PATH_TMP_CLOUD, TASKDATA_FOLDER);
         try {
            FileUtils.moveDirectory(tmpFolder, zippedCloudDir);
            session.setResultCode(Process.Result.SUCCESS);
         }
         catch (IOException e) {
            /* move failed, clear temporary source folder */
            if (!UtilityHelper.deleteRecursively(tmpFolder)) {
               logger.error("failing to delete contents of temporary folder:{}", tmpFolder.getPath());
            }
            session.setResultCode(Process.Result.ERROR);
         }
      } else {
         logger.info("temporary folder doesn't exist.");
         session.setResultCode(Process.Result.ERROR);
      }

      if (SessionUtil.isErroneous(session)) {
       throw new SessionException(ErrorCode.PERFORM_ERROR);
      }

      /* zip up temporary cloud export directory */
      final String ZIP_EXT = ".zip";
      final String CLOUD_FILENAME = String.format("CLOUD.%s", new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
      final String zippedCloudFileName = PATH_TMP_CLOUD + File.separator + CLOUD_FILENAME + ZIP_EXT;
      logger.debug("GET FILE zipping {} as {}", PATH_TMP_CLOUD + TASKDATA_FOLDER, zippedCloudFileName);

      String suffixFilter = "*";
      new ZipHelper().zipDirectory(new File(PATH_TMP_CLOUD), zippedCloudFileName, suffixFilter);

      /* send file */
      if (null != cloudAddress) {
         getMediator().sendFile(cloudAddress, zippedCloudFileName);
         session.setResultCode(Process.Result.SUCCESS);
      }
      else {
         session.setResultCode(Process.Result.ERROR);
      }

      File tempCloudRoot = new File(PATH_TMP_CLOUD);
      FileUtils.deleteQuietly(tempCloudRoot);

      if (SessionUtil.isErroneous(session)) {
         throw new SessionException(ErrorCode.PERFORM_ERROR);
      }
   }

   private void moveFilesToUSB(@Nonnull Session session, @Nonnull SessionExtra extra) throws SessionException {
      if (extra != null && extra.isUsbExtra() && SessionUtil.isExportAction(session)) {
         if (!Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            if (!extra.isUseInternalFileSystem()) {
            logger.info("USB is not mounted, so throw error.");
            session.setResultCode(Process.Result.ERROR);
            notifier.notifySessionError(session, ErrorCode.USB_REMOVED);
            throw new SessionException(ErrorCode.USB_REMOVED);
            }
         }

         boolean moveWasSuccessfull = false;
         File tmpFolder = new File(tempPath);

         if (extra.isUseInternalFileSystem()) {
            moveWasSuccessfull = moveFilesToInternalFlash(session.getExtra());
            if (moveWasSuccessfull) {
               session.setResultCode(Process.Result.SUCCESS);
            } else {
               session.setResultCode(Process.Result.ERROR);
               throw new SessionException(ErrorCode.PERFORM_ERROR);
            }
         } else {
            final String USB_EXPORT_PATH = UtilityHelper.CommonPaths.PATH_USB_PORT.getPathString();

            UtilityHelper.MediumVariant mediumVariant = UtilityHelper.MediumVariant.fromValue(extra.getOrder());
            final String ISOXML_FOLDER;
            if (null != mediumVariant && UtilityHelper.MediumVariant.USB_FRED.equals(mediumVariant)) {
               ISOXML_FOLDER = UtilityHelper.CommonPaths.PATH_USB_FRED.getPathString() + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() +
                       formatManager.getFormat(UtilityHelper.CommonFormats.ISOXMLFORMAT.getName()).getPath() + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();
            } else {
               ISOXML_FOLDER = UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() +
                       formatManager.getFormat(UtilityHelper.CommonFormats.ISOXMLFORMAT.getName()).getPath() + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();
            }

            if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED) && tmpFolder.exists()) {

               if (extra.getFormat().equals(UtilityHelper.CommonFormats.ISOXMLFORMAT.getName())) {
                  logger.info("start moving files from: {} to {}{}", tempPath, USB_EXPORT_PATH, ISOXML_FOLDER);
                  moveWasSuccessfull = moveFiles(tempPath, USB_EXPORT_PATH, ISOXML_FOLDER);
               } else {
                  logger.info("Unknown destination on export");
               }

               logger.info("finished moving files");

               session.setResultCode((moveWasSuccessfull) ? Process.Result.SUCCESS : Process.Result.ERROR);
            } else {
               logger.info("Either USB is not mounted or temporary folder doesn't exist.");
               session.setResultCode(Process.Result.ERROR);
               notifier.notifySessionError(session, ErrorCode.USB_REMOVED);
               throw new SessionException(ErrorCode.USB_REMOVED);
            }
         }

         if (tmpFolder.exists()) {
            if (!UtilityHelper.deleteRecursively(tmpFolder)) {
               logger.error("unable to delete temporary folder:{}", tmpFolder.getPath());
            }
         }

         if (SessionUtil.isErroneous(session)) {
            throw new SessionException(ErrorCode.PERFORM_ERROR);
         }
      } else if (extra != null && extra.isUsbExtra() && SessionUtil.isImportAction(session)) {
         if ( (!Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) && (!extra.isUseInternalFileSystem()) ) {
            logger.info("USB is not mounted, so throw error.");
            session.setResultCode(Process.Result.ERROR);
            notifier.notifySessionError(session, ErrorCode.USB_REMOVED);
            throw new SessionException(ErrorCode.USB_REMOVED);
         } else {
            session.setResultCode(Process.Result.SUCCESS);
         }
      } else {
         session.setResultCode(Process.Result.SUCCESS);
      }
   }

   @Override
   protected void onPostExecute(Session session) {
      if (statusSender != null) {
         boolean exporting = SessionUtil.isExportAction(session);

         if (SessionUtil.isErroneous(session)) {
            statusSender.sendStatus("Error", exporting);
         }
         else if (SessionUtil.isCancelled(session)) {
            statusSender.sendCancelledStatus(exporting);
         }
         else if (SessionUtil.isSuccessful(session)) {
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

      try {
         File source = new File(sourceDir);
         ArrayDeque<File> fileList = new ArrayDeque<File>();

         File dest = new File(destRoot + destDir);
         long exportSize = 0;

         if (null != source && source.exists()) {

            File[] paths = source.listFiles();
            if (null != paths) {
               for (File path : paths) {
                  fileList.push(path);
                  exportSize += path.length();
               }
            }
            logger.debug("Exporting source: {}", source.getPath());
            logger.debug("Exporting destination: {}", dest.getPath());

            StatFs stat = new StatFs(destRoot);
            long bytesFree = stat.getAvailableBytes();

            final long SAFETY_BUFFER_BYTE = (long)(2 * 1024 * 1024); // 2MByte safety buffer to prevent crashes caused by logging

            boolean isMounted = Environment.getExternalStorageState().equals(MEDIA_MOUNTED);

            if (exportSize == 0) {
               logger.info("No source file found");
            }
            else if (bytesFree < (exportSize + SAFETY_BUFFER_BYTE)) {
               // reset before showing again
               faultHandler.getFault(FaultCode.USB_NOT_ENOUGH_MEMORY).reset();
               faultHandler.getFault(FaultCode.USB_NOT_ENOUGH_MEMORY).alert();
               logger.info("Not enough space on USB stick");
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
                  if(!copy.renameTo(new File(String.format("%s.%s", dest.getAbsolutePath(), new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(dest.lastModified())))))) {
                     logger.info("Failed Renaming {} with last modified date suffix", dest.getAbsolutePath());
                  }
               }

               if (!dest.mkdirs()) {
                  logger.info("Failed to create directory");
               }

               String root = source.toString();
               File next;
               File destination;
               File parent;
               logger.info("Start copy {} files", fileList.size());

               retValue = true;
               while (!fileList.isEmpty()) {
                  next = fileList.pop();
                  logger.info("Copy: {}", next.getPath());
                  if (next.isFile()) {
                     destination = new File(String.format("%s%s", dest, next.toString().substring(root.length())));
                     parent = destination.getCanonicalFile().getParentFile();
                     if (!parent.exists()) {
                        retValue &= parent.mkdirs();
                     }
                     Files.move(next, destination);
                  }
                  else if (next.isDirectory()) {
                     if (null != next.listFiles()) {
                        for (File file : next.listFiles()) {
                           fileList.push(file);
                        }
                     }
                     else {
                        retValue = false;
                     }
                  }
               }
               logger.info("Finished copy files");
            }
         }
         else {
            logger.error("No source folder found");
         }
      }
      catch (Exception e) {
         if (!Environment.getExternalStorageState().equals(MEDIA_MOUNTED)) {
            faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).reset();
            faultHandler.getFault(FaultCode.USB_REMOVED_DURING_EXPORT).alert();
         }
         logger.error("Aborted moving files to USB, caused by:", e);
         retValue = false;
      }
      return retValue;
   }

   private boolean moveFilesToInternalFlash(SessionExtra extra) {
      boolean moveStatus = false;
      File source = new File(tempPath);
      File dest = new File(extra.getPath());
      final String ISOXML_FOLDER = UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + formatManager.getFormat(UtilityHelper.CommonFormats.ISOXMLFORMAT.getName()).getPath()
         + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString();

      if (dest.exists()) {
         logger.info("Renaming existing export destination folder");
         File copy = new File(dest.getAbsolutePath());
         if(!copy.renameTo(new File(String.format("%s.%s", dest.getAbsolutePath(), new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(dest.lastModified())))))) {
            logger.info("Failed Renaming {} with last modified date suffix", dest.getAbsolutePath());
         }
      }
      dest.mkdirs();

      try {
         File[] fileList = source.listFiles();
         if (fileList != null) {
            logger.info("start moving {} files", fileList.length);
            moveFileorFolders(source.getPath(), extra.getPath());
         }
         logger.info("finished moving files");
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

   void moveFileorFolders(String srcFolder, String destFolder) throws IOException {
      File src = new File(srcFolder);
      File dest = new File(destFolder);
      if(src.isDirectory()) {
         String files[] = src.list();
         for (String file: files) {
            String srcFile = srcFolder + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + file;
            moveFileorFolders(srcFile, destFolder);
         }
      }
      else{
         String destFile = destFolder + UtilityHelper.CommonPaths.PATH_DESIGNATOR.getPathString() + src.getName();
         Files.move(src, new File(destFile));
      }
   }
}