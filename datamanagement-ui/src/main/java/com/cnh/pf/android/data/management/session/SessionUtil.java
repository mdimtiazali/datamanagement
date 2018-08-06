/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import com.cnh.pf.datamng.Process;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import org.jgroups.Address;
import org.jgroups.util.UUID;

import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * This utility class provides static methods that query session state & types and etc.
 */
public class SessionUtil {

   private SessionUtil() {
      // This is to avoid code analysis warning.
   }

   /**
    * Return true if the session is in progress.
    *
    * @param session the session
    * @return  true if the session is in progress
    */
   public static boolean isInProgress(final Session session) {
      return session != null && (Session.State.IN_PROGRESS.equals(session.getState()));
   }

   /**
    * Return true if the session is complete.
    *
    * @param session the session
    * @return  true if the session is complete
    */
   public static boolean isComplete(final Session session) {
      return session != null && Session.State.COMPLETE.equals(session.getState());
   }

   /**
    * Return true if the session is waiting.
    *
    * @param session the session
    * @return  true if the session is waiting
    */
   public static boolean isWaiting(final Session session) {
      return session != null && Session.State.WAIT.equals(session.getState());
   }

   /**
    * Return true if the session pertains to EXPORT action
    *
    * @param session the session
    * @return  true if the session pertains to EXPORT action
    */
   public static boolean isExportAction(final Session session) {
      return session != null && Session.Action.EXPORT.equals(session.getAction());
   }

   /**
    * Return true if the session pertains to IMPORT action
    *
    * @param session the session
    * @return  true if the session pertains to IMPORT action
    */
   public static boolean isImportAction(final Session session) {
      return session != null && Session.Action.IMPORT.equals(session.getAction());
   }

   /**
    * Return true if the session pertains to MANAGE action
    *
    * @param session the session
    * @return  true if the session pertains to MANAGE action
    */
   public static boolean isManageAction(final Session session) {
      return session != null && Session.Action.MANAGE.equals(session.getAction());
   }

   /**
    * Return true if the current session type is DISCOVERY
    *
    * @param session the session
    * @return  true if the current session type is DISCOVERY
    */
   public static boolean isDiscoveryTask(final Session session) {
      return session != null && Session.Type.DISCOVERY.equals(session.getType());
   }

   /**
    * Return true if the current session type is PERFORM_OPERATIONS
    *
    * @param session the session
    * @return  true if the current session type is PERFORM_OPERATIONS
    */
   public static boolean isPerformOperationsTask(final Session session) {
      return session != null && Session.Type.PERFORM_OPERATIONS.equals(session.getType());
   }

   /**
    * Return true if the current session type is CALCULATE_CONFLICTS
    *
    * @param session the session
    * @return  true if the current session type is CALCULATE_CONFLICTS
    */
   public static boolean isCalculateConflictsTask(final Session session) {
      return session != null && Session.Type.CALCULATE_CONFLICTS.equals(session.getType());
   }

   /**
    * Return true if the current session type is CALCULATE_OPERATIONS
    *
    * @param session the session
    * @return  true if the current session type is CALCULATE_OPERATIONS
    */
   public static boolean isCalculateOperationsTask(final Session session) {
      return session != null && Session.Type.CALCULATE_OPERATIONS.equals(session.getType());
   }

   /**
    * Return true if the current session type is UPDATE
    *
    * @param session the session
    * @return  true if the current session type is UPDATE
    */
   public static boolean isUpdateTask(final Session session) {
      return session != null && Session.Type.UPDATE.equals(session.getType());
   }

   /**
    * Return true if the current session type is PASTE
    *
    * @param session the session
    * @return  true if the current session type is PASTE
    */
   public static boolean isPasteTask(final Session session) {
      return session != null && Session.Type.PASTE.equals(session.getType());
   }

   /**
    * Return true if the current session type is DELETE
    *
    * @param session the session
    * @return  true if the current session type is DELETE
    */
   public static boolean isDeleteTask(final Session session) {
      return session != null && Session.Type.DELETE.equals(session.getType());
   }

   /**
    * Return true if the session ends with success.
    *
    * @param session the session
    * @return  true if the session ends with success
    */
   public static boolean isSuccessful(final Session session) {
      return session != null && Process.Result.SUCCESS.equals(session.getResultCode());
   }

   /**
    * Return true if the session ends with error.
    *
    * @param session the session
    * @return  true if the session ends with error
    */
   public static boolean isErroneous(final Session session) {
      return session != null && Process.Result.ERROR.equals(session.getResultCode());
   }

   /**
    * Return true if the session gets cancelled.
    *
    * @param session the session
    * @return  true if the session gets cancelled
    */
   public static boolean isCancelled(final Session session) {
      return session != null && Process.Result.CANCEL.equals(session.getResultCode());
   }

   /**
    * Convert an array of Address objects to String
    *
    * @param addresses  array of Address
    * @return  converted string
    */
   public static String addressToString(Address[] addresses) {
      return Collections2.transform(Arrays.asList(addresses), new Function<Address, String>() {
         @Nullable
         @Override
         public String apply(@Nullable Address input) {
            return UUID.get(input);
         }
      }).toString();
   }
}
