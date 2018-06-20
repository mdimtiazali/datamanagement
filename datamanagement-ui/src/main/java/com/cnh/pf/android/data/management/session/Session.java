/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session;

import android.os.Parcel;
import android.os.Parcelable;
import com.cnh.jgroups.ObjectGraph;
import com.cnh.jgroups.Operation;
import com.cnh.pf.datamng.Process;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.jgroups.Address;
import org.jgroups.util.RspList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Session class that keeps state of data management session. Persist current operation,
 * data selected but user, and conflict resolutions if applied.
 *
 * @author junsu.shin@cnhind.com
 */
public class Session implements Parcelable {
   private UUID uuid;
   /** Original ObjectGraph data as delivered by discovery**/
   private List<ObjectGraph> objectData;
   /** User selected entities */
   private List<Operation> operations;
   /** Current Session Operation*/
   private Type type;
   /** Source device*/
   private List<Address> sources;
   /** Target device*/
   private List<Address> destinations;
   /** Extra information for Export/Import */
   private SessionExtra extra;
   /** results */
   private RspList<Process> results;

   private Process.Result resultCode;

   /** flag for operation is in progress and timestamp for last operation complete*/
   private volatile long timestamp;

   private State state;

   private Action action;

   public enum Action {
      MANAGE(1),
      EXPORT(2),
      IMPORT(3),
      UNKNOWN(4);

      private int value;

      Action(int v) {
         this.value = v;
      }

      public int getValue() {
         return value;
      }

      public static Action fromValue(int value) {
         switch (value) {
            case 1:
               return MANAGE;
            case 2:
               return EXPORT;
            case 3:
               return IMPORT;
            case 4:
            default:
               return UNKNOWN;
         }
      }
   }

   public enum State {
      WAIT(0),
      IN_PROGRESS(1),
      COMPLETE(2);

      private int value;

      State(int v) {
         this.value = v;
      }

      public int getValue() {
         return value;
      }

      public static State fromValue(int value) {
         switch (value) {
            case 1:
               return IN_PROGRESS;
            case 2:
               return COMPLETE;
            case 0:
            default:
               return WAIT;
         }
      }
   }

   public enum Type {
      INITIAL(0),
      DISCOVERY(1),
      CALCULATE_OPERATIONS(2),
      CALCULATE_CONFLICTS(3),
      PERFORM_OPERATIONS(4),
      UPDATE(5),
      DELETE(6);


      private int value;

      Type(int v) {
         this.value = v;
      }

      public int getValue() {
         return value;
      }

      public static Type fromValue(int value) {
         switch (value) {
            case 1:
               return DISCOVERY;
            case 2:
               return CALCULATE_OPERATIONS;
            case 3:
               return CALCULATE_CONFLICTS;
            case 4:
               return PERFORM_OPERATIONS;
            case 5:
               return UPDATE;
            case 6:
               return DELETE;
            case 0:
            default:
               return INITIAL;
         }
      }
   }

   public static final Creator<Session> CREATOR = new Creator<Session>() {
      @Override
      public Session createFromParcel(Parcel source) {
         return new Session(source);
      }

      @Override
      public Session[] newArray(int size) {
         return new Session[size];
      }
   };

   @Override
   public int describeContents() {
      return 0;
   }

   @Override
   public void writeToParcel(Parcel out, int flags) {
      out.writeString(this.uuid.toString());
      out.writeTypedList(this.objectData);
      out.writeTypedList(this.operations);
      out.writeInt(this.type.getValue());
      out.writeInt(this.sources.size());
      for (Address src : this.sources) {
         out.writeSerializable(src);
      }
      out.writeInt(this.destinations.size());
      for (Address dest : this.destinations) {
         out.writeSerializable(dest);
      }
      out.writeInt(this.state.getValue());
      out.writeParcelable(extra, flags);
      out.writeInt(this.action.getValue());
   }

   public Session(Parcel in) {
      this.uuid = UUID.fromString(in.readString());
      this.objectData = new ArrayList<ObjectGraph>();
      in.readTypedList(this.objectData, ObjectGraph.CREATOR);
      this.operations = new ArrayList<Operation>();
      in.readTypedList(this.operations, Operation.CREATOR);
      this.type = Type.fromValue(in.readInt());
      int size = in.readInt();
      this.sources = new ArrayList<Address>();
      for (int i = 0; i < size; i++) {
         this.sources.add((Address)in.readSerializable());
      }
      size = in.readInt();
      this.destinations = new ArrayList<Address>();
      for (int i = 0; i < size; i++) {
         this.destinations.add((Address)in.readSerializable());
      }
      this.state = State.fromValue(in.readInt());
      this.extra = in.readParcelable(SessionExtra.class.getClassLoader());
      this.action = Action.fromValue(in.readInt());
   }

   public Session() {
      this.uuid = UUID.randomUUID();
      this.objectData = new ArrayList<ObjectGraph>();
      this.operations = new ArrayList<Operation>();
      this.type = Type.INITIAL;
      this.sources = new ArrayList<Address>();
      this.destinations = new ArrayList<Address>();
      this.state = State.WAIT;
      this.extra = null;
      this.action = Action.UNKNOWN;
   }

   public Session(Session other) {
      this.uuid = other.uuid;
      this.objectData = other.objectData;
      this.operations = other.operations;
      this.type = other.type;
      this.sources = other.sources;
      this.destinations = other.destinations;
      this.state = other.state;
      this.extra = other.extra;
      this.action = other.action;
   }

   public Session(List<Address> sources, List<Address> destinations) {
      this();

      this.sources = sources;
      this.destinations = destinations;
   }

   public UUID getUuid() {
      return uuid;
   }

   /**
    * Get timestamp
    * @return long
    */
   public long getTimestamp() {
      return timestamp;
   }
   /**
    * Set timestamp
    * @return
    */
   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }

   /**
    * Return original object tree as received by discovery
    * @return ObjectGraph tree
    */
   public List<ObjectGraph> getObjectData() {
      return objectData;
   }

   /**
    * Set the object tree returned by discovery from source datasource
    * @param objectData ObjectGraph tree
    */
   public void setObjectData(List<ObjectGraph> objectData) {
      this.objectData = objectData;
   }

   /**
    * Set the {@link Operation} for each Object selected by user for import/export
    * @param ops List of Operations
    */
   public void setOperations(List<Operation> ops) {
      this.operations = ops;
   }

   /**
    * Get list of Operations for all Objects user selected for import/export
    * @return
    */
   public List<Operation> getOperations() {
      return operations;
   }

   /**
    * Set the latest operation(state) that the DataManagementSession has completed
    * @param type
    */
   public synchronized void setType(Type type) {
      this.type = type;
   }

   /**
    * Get the latest operation that this session has completed
    * @return
    */
   public synchronized Type getType() {
      return type;
   }

   /**
    * Getter for session extra. The extra contains path, format for import/export process.
    * @return
    */
   public synchronized SessionExtra getExtra() {
      return this.extra;
   }

   /**
    * Setter for session extra.
    * @param extra
    */
   public synchronized void setExtra(SessionExtra extra) {
      this.extra = extra;
   }

   /**
    * Setter for session state
    * @param state
    */
   public synchronized void setState(State state) {
      this.state = state;
   }

   /**
    * Getter for session state
    * @return
    */
   public synchronized State getState() {
      return state;
   }

   /**
    * Setter for action that a session belongs to
    * @param action
    */
   public synchronized void setAction(Action action) {
      this.action = action;
   }

   /**
    * Getter for session action
    * @return
    */
   public synchronized Action getAction() {
      return this.action;
   }

   /**
    * Getter for source addresses to execute a session
    * @return
    */
   public List<Address> getSources() {
      return sources;
   }

   /**
    * Setter for source addresses to execute a session
    * @param sources list of addresses
    */
   public void setSources(List<Address> sources) {
      this.sources = sources;
   }

   /**
    * Getter for destination addresses to execute a session
    * @return
    */
   public List<Address> getDestinations() {
      return destinations;
   }

   /**
    * Setter for destination addresses to execute a session
    * @param destinations
    */
   public void setDestinations(List<Address> destinations) {
      this.destinations = destinations;
   }

   /**
    * Getter for session results
    * @return
    */
   public RspList<Process> getResults() {
      return results;
   }

   /**
    * Setter for session results
    * @param results
    */
   public void setResults(RspList<Process> results) {
      this.results = results;
   }

   /**
    * Getter for session result code
    * @return
    */
   public Process.Result getResultCode() {
      return resultCode;
   }

   /**
    * Setter for session result code
    * @param result
    */
   public void setResultCode(Process.Result result) {
      this.resultCode = result;
   }

   public String toString() {
      MoreObjects.ToStringHelper h = MoreObjects.toStringHelper(this)
              .add("UUID", uuid.toString())
              .add("operation", type.toString());
      if (objectData != null) h.add("objectData", objectData);
      if (operations != null) h.add("operations", operations);
      return h.toString();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (!(o instanceof Session))
         return false;
      Session that = (Session) o;
      return Objects.equal(uuid, that.uuid);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(uuid);
   }
}

