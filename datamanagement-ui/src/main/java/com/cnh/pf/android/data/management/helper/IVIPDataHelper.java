/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import com.cnh.pf.model.vip.vehimp.VehicleCurrent;

/**
 * Interface to easily access vehicle information across DM project
 * Majority of code brought in from VIPHandler in yield-monitor project
 * @Author: eric.fusinetti@cnhind.com
 */
public interface IVIPDataHelper {

    /**
     * This interface defines the listener methods that need to be supported when vehicle changes occur
     */
    interface OnVehicleChangedListener { //Add any new classes to this
        /**
         * Updates when the current vehicle is changed
         * @param vehicleCurrent
         */
        void onCurrentVehicleChanged(VehicleCurrent vehicleCurrent);
        //onVehicleMakeChanged
        //onVehicleTypeChanged
    }

   /**
    * Used to get make of the vehicle the display is connected to
    *
    * @param defaultString String to be returned if no valid make could be found
    * @return String device name or Unknown Device if vehicle parameters are not set
    */
   String getMakeOfVehicle(String defaultString);

    /**
     * Add listener for when vehicle changes occur
     * @param listener
     */
    void addOnVehicleChangedListener(OnVehicleChangedListener listener);

    /**
     * Remove listener for vehicle changes
     * @param listener
     */
    void removeOnVehicleChangedListener(OnVehicleChangedListener listener);
}
