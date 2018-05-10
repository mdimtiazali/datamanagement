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
import com.google.common.base.MoreObjects;

/**
 * A class that contains extra information to run a session and display items related to session.
 */
public class SessionExtra implements Parcelable {
   public static final int USB = 1;
   public static final int CLOUD = 2;
   public static final int DISPLAY = 3;

   public static final Creator<SessionExtra> CREATOR = new Creator<SessionExtra>() {
      @Override
      public SessionExtra[] newArray(int size) {
         return new SessionExtra[size];
      }

      @Override
      public SessionExtra createFromParcel(Parcel in) {
         return new SessionExtra(in);
      }
   };

   /** Import/Export format */
   private String format;
   /** Import/Export base path */
   private String basePath;
   /** Import/Export path */
   private String path;
   /** Extra type: USB, CLOUD, DISPLAY */
   private int type;
   /** Name string for display purpose */
   private String desc;
   /** Used to sort extra items in extra list */
   private int order;

   public SessionExtra(Parcel in) {
      this.format = in.readString();
      if (in.readInt() == 1) {
         this.basePath = in.readString();
      }
      if (in.readInt() == 1) {
         this.path = in.readString();
      }
      this.type = in.readInt();
      this.desc = in.readString();
      this.order = in.readInt();
   }

   @Override
   public int describeContents() {
      return 0;
   }

   @Override
   public void writeToParcel(Parcel out, int flags) {
      out.writeString(this.format);
      out.writeInt(basePath == null ? 0 : 1);
      if (basePath != null) {
         out.writeString(this.basePath);
      }
      out.writeInt(path == null ? 0 : 1);
      if (path != null) {
         out.writeString(this.path);
      }
      out.writeInt(this.type);
      out.writeString(this.desc);
      out.writeInt(this.order);
   }

   public SessionExtra(SessionExtra other) {
      this.type = other.type;
      this.format = other.format;
      this.path = other.path;
      this.basePath = other.basePath;
      this.desc = other.desc;
      this.order = other.order;
   }

   public SessionExtra(int type, String desc, int order) {
      this.format = null;
      this.path = null;
      this.basePath = null;
      this.type = type;
      this.desc = desc;
      this.order = order;
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
              .add("desc", desc)
              .add("type", type)
              .add("basepath", basePath)
              .add("path", path)
              .add("format", format)
              .add("order", order)
              .toString();
   }

   /**
    * get import/export format.  i.e. ISOXML, XSTREAM
    * @return
    */
   public String getFormat() {
      return format;
   }

   public void setFormat(String format) {
      this.format = format;
   }

   public String getPath() {
      return this.path;
   }

   public void setPath(String path) {
      this.path = path;
   }

   public String getBasePath() {
      return this.basePath;
   }

   public void setBasePath(String basePath) {
      this.basePath = basePath;
   }

   public String getDescription() {
      return desc;
   }

   public void setDescription(String desc) {
      this.desc = desc;
   }

   public int getOrder() {
      return order;
   }

   public void setOrder(int order) {
      this.order = order;
   }

   public boolean isUsbExtra() {
      return this.type == USB;
   }

   public boolean isCloudExtra() {
      return this.type == CLOUD;
   }

   public boolean isDisplayExtra() {
      return this.type == DISPLAY;
   }
}