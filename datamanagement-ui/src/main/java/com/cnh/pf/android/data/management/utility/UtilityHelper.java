package com.cnh.pf.android.data.management.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import com.cnh.jgroups.Datasource;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.android.data.management.session.SessionExtra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;


import static com.cnh.pf.model.constants.stringsConstants.BRAND_CASE_IH;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_FLEXICOIL;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_NEW_HOLLAND;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_STEYR;


/**
 * Class for utility type public methods or constants to used across DM project
 */
public class UtilityHelper {
   private static final Logger logger = LoggerFactory.getLogger(UtilityHelper.class);
   public static final String STORAGE_LOCATION_TYPE = "com.cnhi.datamanagement.storagelocationtype";
   public static final String STORAGE_LOCATION = "com.cnhi.datamanagement.storagelocation";
   public static final String STORAGE_LOCATION_USB = "USB";
   public static final String STORAGE_LOCATION_INTERNAL = "INTERNAL_FLASH";
   public static final int NEGATIVE_BINARY_ERROR = -1;
   public static final int MAX_TREE_SELECTIONS_FOR_DEFAULT_TEXT_SIZE = 999;
   public static final int POPOVER_DEFAULT_WIDTH = 550;
   public static final int EXPORT_DEST_POPOVER_DEFAULT_HEIGHT = 370;
   public static final int EXPORT_FORMAT_POPOVER_DEFAULT_HEIGHT = 455;
   /**
    * Map for returning the correct strings based on the vehicle make and data sources available
    */
   public static final Map<VehicleBrand, Map<MediumVariant, Integer>> destinationNamesHashMap;
   static {
      destinationNamesHashMap = new EnumMap<VehicleBrand, Map<MediumVariant, Integer>>(VehicleBrand.class);
      /**
       * Map for returning the correct strings based on the vehicle make and data sources available
       */
      // Create Case hashmap and add it to destination map
      // USB List
      Map<MediumVariant, Integer> tempMap = new EnumMap<MediumVariant, Integer>(MediumVariant.class);
      tempMap.put(MediumVariant.USB_PHOENIX, R.string.case_pro1200_usb);
      tempMap.put(MediumVariant.USB_HAWK, R.string.case_pro800_usb);
      tempMap.put(MediumVariant.USB_FRED, R.string.case_pro700_usb);
      tempMap.put(MediumVariant.USB_DESKTOP_SW, R.string.case_desktopsw_usb);
      // Cloud List items
      tempMap.put(MediumVariant.CLOUD_OUTBOX, R.string.case_cloud_connect_outbox);
      tempMap.put(MediumVariant.CLOUD_INBOX, R.string.case_cloud_connect_inbox);
      destinationNamesHashMap.put(VehicleBrand.CASE, tempMap);

      // Create New Holland hashmap and add it to destination map
      // USB List
      tempMap = new EnumMap<MediumVariant, Integer>(MediumVariant.class);
      tempMap.put(MediumVariant.USB_PHOENIX, R.string.new_holland_intelli12_usb);
      tempMap.put(MediumVariant.USB_HAWK, R.string.new_holland_intelli8_usb);
      tempMap.put(MediumVariant.USB_FRED, R.string.new_holland_intelli4_usb);
      tempMap.put(MediumVariant.USB_DESKTOP_SW, R.string.new_holland_desktopsw_usb);
      // Cloud List
      tempMap.put(MediumVariant.CLOUD_OUTBOX, R.string.new_holland_cloud_connect_outbox);
      tempMap.put(MediumVariant.CLOUD_INBOX, R.string.new_holland_cloud_connect_inbox);
      destinationNamesHashMap.put(VehicleBrand.NEW_HOLLAND, tempMap);

      // Create Steyr hashmap and add it to destination map
      // Steyr USB List
      tempMap = new EnumMap<MediumVariant, Integer>(MediumVariant.class);
      tempMap.put(MediumVariant.USB_PHOENIX, R.string.steyr_12_usb);
      tempMap.put(MediumVariant.USB_HAWK, R.string.steyr_8_usb);
      tempMap.put(MediumVariant.USB_FRED, R.string.steyr_4_usb);
      tempMap.put(MediumVariant.USB_DESKTOP_SW, R.string.steyr_desktopsw_usb);
      // Cloud List
      tempMap.put(MediumVariant.CLOUD_OUTBOX, R.string.steyr_cloud_connect_outbox);
      tempMap.put(MediumVariant.CLOUD_INBOX, R.string.steyr_cloud_connect_inbox);
      destinationNamesHashMap.put(VehicleBrand.STEYR, tempMap);

      // Create Flexicoil hashmap and add it to destination map
      // USB List
      tempMap = new EnumMap<MediumVariant, Integer>(MediumVariant.class);
      // Cloud List
      destinationNamesHashMap.put(VehicleBrand.FLEXICOIL, tempMap);

      // Create Generic Tractor hashmap and add it to destination map
      // USB List
      tempMap = new EnumMap<MediumVariant, Integer>(MediumVariant.class);
      tempMap.put(MediumVariant.USB_PHOENIX, R.string.generic_tractor_12_usb);
      tempMap.put(MediumVariant.USB_HAWK, R.string.generic_tractor_8_usb);
      tempMap.put(MediumVariant.USB_FRED, R.string.generic_tractor_4_usb);
      tempMap.put(MediumVariant.USB_DESKTOP_SW, R.string.generic_tractor_desktopsw_usb);
      // Cloud List
      tempMap.put(MediumVariant.CLOUD_OUTBOX, R.string.generic_tractor_cloud_connect_outbox);
      tempMap.put(MediumVariant.CLOUD_INBOX, R.string.generic_tractor_cloud_connect_inbox);
      destinationNamesHashMap.put(VehicleBrand.GENERIC_TRACTOR, tempMap);

      // Create Generic Combine hashmap and add it to destination map
      // Generic Combine USB List
      tempMap = new EnumMap<MediumVariant, Integer>(MediumVariant.class);

      // Generic Combine Cloud List
      tempMap = new EnumMap<MediumVariant, Integer>(MediumVariant.class);
      destinationNamesHashMap.put(VehicleBrand.GENERIC_COMBINE, tempMap);
   }

   /**
    * Enum for medium variant
    */
   public enum MediumVariant {
      USB_PHOENIX(1, SessionExtra.USB),
      USB_HAWK(2, SessionExtra.USB),
      USB_FRED(3, SessionExtra.USB),
      USB_DESKTOP_SW(4, SessionExtra.USB),
      CLOUD_OUTBOX(5, SessionExtra.CLOUD),
      CLOUD_INBOX(6, SessionExtra.CLOUD);

      private int extraType;
      private int value;

      MediumVariant(int v, int extraType) {
         this.value = v;
         this.extraType = extraType;
      }

      public int getValue() {
         return value;
      }

      public int getExtraType() {
         return this.extraType;
      }

      public static MediumVariant fromValue(int value) {
         switch (value) {
            case 1:
               return USB_PHOENIX;
            case 2:
               return USB_HAWK;
            case 3:
               return USB_FRED;
            case 4:
               return USB_DESKTOP_SW;
            case 5:
               return CLOUD_OUTBOX;
            case 6:
               return CLOUD_INBOX;
         }
         return null;
      }
   }

   /**
    * Enum for returning the vehicle brand
    */
   public enum VehicleBrand {
      CASE (BRAND_CASE_IH),
      NEW_HOLLAND (BRAND_NEW_HOLLAND),
      STEYR (BRAND_STEYR),                //TODO: Add correct strings once available
      FLEXICOIL (BRAND_FLEXICOIL),        //TODO: Add correct strings once available
      GENERIC_TRACTOR ("GENERICTRACTOR"), //TODO: Add correct strings once available
      GENERIC_COMBINE ("GENERICCOMBINE")  //TODO: Add correct strings once available
      ;

      private final String brandCode;

      VehicleBrand(String code) {
         this.brandCode = code;
      }

      /**
       * Returns the brand code
       * @return brandCode
       */
      public String getBrandCode() {
         return this.brandCode;
      }

      /**
       * Returns the brand code for the brand string that is passed
       * @param string
       * @return brand code value
       */
      public static VehicleBrand getValue(String string) {
         VehicleBrand ret = null;
         for (VehicleBrand value : VehicleBrand.values()) {
            if (value.getBrandCode().equals(string)) {
               ret = value;
               break;
            }
         }
         if (null != ret)
            return ret;
         else
            return GENERIC_TRACTOR; //Return Generic Tractor strings if vehicle brand isn't found
      }
   }

    /**
     * Returns string ID based on the vehicle make and devices available
     * @param vehicleBrand
     * @return map of medium types and string id associated with the vehicle brand
     */
   public static Map<MediumVariant, Integer> getMediumVariantMap(String vehicleBrand) {
      Map<MediumVariant, Integer> map;
      map = destinationNamesHashMap.get(UtilityHelper.VehicleBrand.getValue(vehicleBrand));
      if (map == null) {
         map = new EnumMap<MediumVariant, Integer>(MediumVariant.class);
      }

      return map;
   }

   /**
    * Set the build preference
    * @param key - key where value is to be stored in shared pref
    * @param value - value to be stored must bee bool, string or int or nothing will be stored.
    * @param ctx - activity context
    */
   public static void setPreference(String key, Object value, Context ctx) {
      try {
         // get shared perference for saving datamanagement file storage
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
         if (prefs != null && key != null) {
            if (value instanceof Boolean) {
               prefs.edit().putBoolean(key, (Boolean) value).apply();
            }
            else if (value instanceof String) {
               prefs.edit().putString(key, (String) value).apply();
            }
            else if (value instanceof Integer) {
               prefs.edit().putInt(key, (Integer) value).apply();
            }
            else {
               logger.warn("cannot add key/value pair to prefs key {}", key);
            }
         }
         else {
            logger.warn("shared prefs null");
         }
      }
      catch (NullPointerException ex) {
         logger.error("NPE", ex);
      }
   }

    /**
     * Gets the shared preference string
     * @param context the context of the preferences
     * @param key the key
     * @return the value
     */
   public static String getSharedPreferenceString(Context context, String key) {
      String sharedStringValue = "";
      if(context != null) {
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
         if (prefs != null) {
            try {
               sharedStringValue = prefs.getString(key, "");
            }
            catch(ClassCastException ex){
               logger.warn("ClassCastException", ex);
            }
         }
         else {
            logger.warn("shared prefs null");
         }
      }
      else {
         logger.warn("context is null");
      }
      return sharedStringValue;
   }

    /**
     * enum for common used paths
     */
    public enum CommonPaths{
        PATH_DESIGNATOR("/"),
        PATH_USB_PORT("./storage/usb1"),
        PATH_TMP("./tmp/data/");
        //TODO move this folder to global access for isoservice, datamanagement and vipdmg

        private final String path;

        CommonPaths(String path){
            this.path = path;
        }

        /**
         * returns the specified path as String
         * @return
         */
        public String getPathString(){
            return this.path;
        }
    }

    public enum CommonFormats{
        PFDATABASEFORMAT("PF Database"),
        ISOXMLFORMAT("ISOXML");

        private final String name;

        CommonFormats(String name){
            this.name = name;
        }

        /**
         * returns the specified name of the format
         * @return
         */
        public String getName() {
            return name;
        }
    }

   /**
    * Extract only file name & extension from full path string
    *
    * @param fullPath   full path string
    * @return           string containing file name & extension
    */
    public static String filenameOnly(String fullPath) {
       int lastPath = fullPath.lastIndexOf(File.separator);
       if (lastPath < 0) {
          return null;
       }
       return fullPath.substring(lastPath + 1);
    }
}
