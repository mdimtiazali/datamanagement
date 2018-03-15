package com.cnh.pf.android.data.management.utility;

import android.app.Fragment;
import android.content.res.Resources;
import com.cnh.jgroups.Datasource;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.data.management.aidl.MediumDevice;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static com.cnh.pf.model.constants.stringsConstants.BRAND_CASE_IH;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_FLEXICOIL;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_NEW_HOLLAND;
import static com.cnh.pf.model.constants.stringsConstants.BRAND_STEYR;


/**
 * Class for utility type public methods or constants to used across DM project
 */
public class UtilityHelper {
    public static final int NEGATIVE_BINARY_ERROR = -1;
    /**
     * Map for returning the correct strings based on the vehicle make and data sources available
     */
    public static final Map<VehicleBrand, Map<Datasource.LocationType, Integer>> destinationNamesHashMap;
    static {
        destinationNamesHashMap = new HashMap<VehicleBrand, Map<Datasource.LocationType, Integer>>();

        // Create Case hashmap and add it to destination map
        // USB List
        Map<Datasource.LocationType, Integer> tempMap = new HashMap<Datasource.LocationType, Integer>();
        tempMap.put(Datasource.LocationType.USB_PHOENIX, R.string.case_pro1200_usb);
        tempMap.put(Datasource.LocationType.USB_HAWK, R.string.case_pro800_usb);
        tempMap.put(Datasource.LocationType.USB_FRED, R.string.case_pro700_usb);
        tempMap.put(Datasource.LocationType.USB_DESKTOP_SW, R.string.case_desktopsw_usb);
        // Cloud List items
        tempMap.put(Datasource.LocationType.CLOUD_OUTBOX, R.string.case_cloud_connect_outbox);
        tempMap.put(Datasource.LocationType.CLOUD_INBOX, R.string.case_cloud_connect_inbox);
        // Display and PCM List items
        tempMap.put(Datasource.LocationType.DISPLAY, R.string.case_display);
        tempMap.put(Datasource.LocationType.PCM, R.string.case_internal);
        destinationNamesHashMap.put(VehicleBrand.CASE, tempMap);

        // Create New Holland hashmap and add it to destination map
        // USB List
        tempMap = new HashMap<Datasource.LocationType, Integer>();
        tempMap.put(Datasource.LocationType.USB_PHOENIX, R.string.new_holland_intelli12_usb);
        tempMap.put(Datasource.LocationType.USB_HAWK, R.string.new_holland_intelli8_usb);
        tempMap.put(Datasource.LocationType.USB_FRED, R.string.new_holland_intelli4_usb);
        tempMap.put(Datasource.LocationType.USB_DESKTOP_SW, R.string.new_holland_desktopsw_usb);
        // Cloud List
        tempMap.put(Datasource.LocationType.CLOUD_OUTBOX, R.string.new_holland_cloud_connect_outbox);
        tempMap.put(Datasource.LocationType.CLOUD_INBOX, R.string.new_holland_cloud_connect_inbox);
        // Display and PCM List items
        tempMap.put(Datasource.LocationType.DISPLAY, R.string.new_holland_display);
        tempMap.put(Datasource.LocationType.PCM, R.string.new_holland_internal);
        destinationNamesHashMap.put(VehicleBrand.NEW_HOLLAND, tempMap);

        // Create Steyr hashmap and add it to destination map
        // Steyr USB List
        tempMap = new HashMap<Datasource.LocationType, Integer>();
        tempMap.put(Datasource.LocationType.USB_PHOENIX, R.string.steyr_12_usb);
        tempMap.put(Datasource.LocationType.USB_HAWK, R.string.steyr_8_usb);
        tempMap.put(Datasource.LocationType.USB_FRED, R.string.steyr_4_usb);
        tempMap.put(Datasource.LocationType.USB_DESKTOP_SW, R.string.steyr_desktopsw_usb);
        // Cloud List
        tempMap.put(Datasource.LocationType.CLOUD_OUTBOX, R.string.steyr_cloud_connect_outbox);
        tempMap.put(Datasource.LocationType.CLOUD_INBOX, R.string.steyr_cloud_connect_inbox);
        // Display and PCM List items
        tempMap.put(Datasource.LocationType.DISPLAY, R.string.steyr_display);
        tempMap.put(Datasource.LocationType.PCM, R.string.steyr_internal);
        destinationNamesHashMap.put(VehicleBrand.STEYR, tempMap);

        // Create Flexicoil hashmap and add it to destination map
        // USB List
        tempMap = new HashMap<Datasource.LocationType, Integer>();
        // Cloud List
        destinationNamesHashMap.put(VehicleBrand.FLEXICOIL, tempMap);

        // Create Generic Tractor hashmap and add it to destination map
        // USB List
        tempMap = new HashMap<Datasource.LocationType, Integer>();
        tempMap.put(Datasource.LocationType.USB_PHOENIX, R.string.generic_tractor_12_usb);
        tempMap.put(Datasource.LocationType.USB_HAWK, R.string.generic_tractor_8_usb);
        tempMap.put(Datasource.LocationType.USB_FRED, R.string.generic_tractor_4_usb);
        tempMap.put(Datasource.LocationType.USB_DESKTOP_SW, R.string.generic_tractor_desktopsw_usb);
        // Cloud List
        tempMap.put(Datasource.LocationType.CLOUD_OUTBOX, R.string.generic_tractor_cloud_connect_outbox);
        tempMap.put(Datasource.LocationType.CLOUD_INBOX, R.string.generic_tractor_cloud_connect_inbox);
        // Display and PCM List items
        tempMap.put(Datasource.LocationType.DISPLAY, R.string.generic_tractor_display);
        tempMap.put(Datasource.LocationType.PCM, R.string.generic_tractor_internal);
        destinationNamesHashMap.put(VehicleBrand.GENERIC_TRACTOR, tempMap);

        // Create Generic Combine hashmap and add it to destination map
        // Generic Combine USB List
        tempMap = new HashMap<Datasource.LocationType, Integer>();

        // Generic Combine Cloud List
        tempMap = new HashMap<Datasource.LocationType, Integer>();
        destinationNamesHashMap.put(VehicleBrand.GENERIC_COMBINE, tempMap);
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
     * Returns the device ID and string based on the vehicle make and devices available
     * @param vehicleBrand
     * @param devices
     * @return hashmap of Datasource Location types and strings associated with the vehicle brand
     */
    public static Map<Datasource.LocationType, String> getListOfDestinations(String vehicleBrand, List<MediumDevice> devices, Resources resources) {
        Map<Datasource.LocationType, String> destinationStrings = new HashMap<Datasource.LocationType, String>();

        Map<Datasource.LocationType, Integer> destinationMap = destinationNamesHashMap.get(UtilityHelper.VehicleBrand.getValue(vehicleBrand));
        if (null != destinationMap && !destinationMap.isEmpty()) {
            for (MediumDevice ds : devices) {
                if (destinationMap.containsKey(ds.getType())){
                    destinationStrings.put(ds.getType(), resources.getString(destinationMap.get(ds.getType())));
                }

            }
        }
        return destinationStrings;
    }
}
