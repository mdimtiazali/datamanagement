/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */
package com.cnh.jgroups.data.service;

import android.os.Parcel;
import android.os.Parcelable;

import javax.annotation.Nonnull;
import java.util.List;

import com.cnh.jgroups.Datasource;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;

/**
 * Session class that keeps state of data management session. Persist current operation,
 * data selected but user, and conflict resolutions if applied.
 * @author oscar.salazar@cnhind.com
 */
public class DataManagementSession implements Parcelable {

   /** Original ObjectGraph data as delivered by discovery**/
   private List<ObjectGraph> objectData;
   /** User selected entities */
   private List<Operation> data;
   /** Current Session Operation*/
   private SessionOperation sessionOperation;
   /** Source Type {USB, INTERNAL...} */
   private Datasource.Source sourceType;
   /** Destination Type */
   private Datasource.Source destinationType;

   public enum SessionOperation {
      DISCOVERY(0), CALCULATTE_OPERATIONS(1), CALCULATE_CONFLICTS(2), PERFORM_OPERATIONS(3), INITIAL(4);

      private int value;

      private SessionOperation(int v) {
         this.value = v;
      }

      public int getValue() {
         return value;
      }

      public static SessionOperation fromValue(int value) {
         switch (value) {
         case 0:
            return DISCOVERY;
         case 1:
            return CALCULATTE_OPERATIONS;
         case 2:
            return CALCULATE_CONFLICTS;
         case 3:
            return PERFORM_OPERATIONS;
         case 4:
            return INITIAL;
         }
         return null;
      }
   }

   public DataManagementSession(@Nonnull Datasource.Source sourceType, @Nonnull Datasource.Source destinationType) {
      this.objectData = null;
      this.data = null;
      this.sessionOperation = SessionOperation.INITIAL;
      this.sourceType = sourceType;
      this.destinationType = destinationType;
   }

   public List<ObjectGraph> getObjectData() {
      return objectData;
   }

   public void setObjectData(List<ObjectGraph> objectData) {
      this.objectData = objectData;
   }

   public void setData(@Nonnull List<Operation> data) {
      this.data = data;
   }

   public List<Operation> getData() {
      return data;
   }

   public synchronized void setSessionOperation(SessionOperation sessionOperation) {
      this.sessionOperation = sessionOperation;
   }

   public synchronized SessionOperation getSessionOperation() {
      return sessionOperation;
   }

   public Datasource.Source getSourceType() {
      return sourceType;
   }

   public Datasource.Source getDestinationType() {
      return destinationType;
   }

   public static final android.os.Parcelable.Creator<DataManagementSession> CREATOR = new android.os.Parcelable.Creator<DataManagementSession>() {
      @Override
      public DataManagementSession[] newArray(int size) {
         return new DataManagementSession[size];
      }

      @Override
      public DataManagementSession createFromParcel(android.os.Parcel in) {
         return new DataManagementSession(in);
      }
   };

   @Override
   public int describeContents() {
      return 0;
   }

   public DataManagementSession(Parcel in) {
      in.readList(objectData, List.class.getClassLoader());
      in.readList(data, List.class.getClassLoader());
      this.sessionOperation = SessionOperation.fromValue(in.readInt());
      sourceType = Datasource.Source.values()[in.readInt()];
      destinationType = Datasource.Source.values()[in.readInt()];
   }

   @Override
   public void writeToParcel(Parcel out, int i) {
      out.writeList(objectData);
      out.writeList(data);
      out.writeInt(sessionOperation.getValue());
      out.writeInt(sourceType.ordinal());
      out.writeInt(destinationType.ordinal());
   }
}
