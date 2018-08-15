/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.misc;

import java.io.File;

/**
 * This class supports files with icons
 *
 * @author krueger
 * @since 28.06.2018
 */
public class IconizedFile {

   public final static int INVALID_RESSOURCE_ID = 0;

   private File file;
   private int iconResourceId = INVALID_RESSOURCE_ID;

   /**
    * Constructor for files without an attached icon resource id
    * @param file File to be iconized
    */
   public IconizedFile(File file) {
      this(file, INVALID_RESSOURCE_ID);
   }

   /**
    * Constructor for files with an attached icon resource id
    * @param file File to be iconized
    * @param iconResourceId Resource-id from the icon the file should be iconized with
    */
   public IconizedFile(File file, int iconResourceId) {
      this.file = file;
      this.iconResourceId = iconResourceId;
   }

   /**
    * Returns weather the iconized file has an icon or not
    * @return True if an icon is attached, false otherwise
    */
   public boolean hasIcon() {
      return iconResourceId != INVALID_RESSOURCE_ID;
   }

   /**
    * Returns the attached file
    * @return File that should be iconized
    */
   public File getFile() {
      return file;
   }

   /**
    * Sets the file to be iconized
    * @param file File to be iconized
    */
   public void setFile(File file) {
      this.file = file;
   }

   /**
    * Returns the icon resource id to be used as a icon for the given file
    * @return Icon Resource Id for iconization
    */
   public int getIconResourceId() {
      return iconResourceId;
   }

   /**
    * Sets the icon to be used for iconization
    * @param iconResourceId Resource Id to be used for iconization
    */
   public void setIconResourceId(int iconResourceId) {
      this.iconResourceId = iconResourceId;
   }
}
