/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import com.cnh.android.vip.aidl.IVIPServiceAIDL;
import com.cnh.pf.android.data.management.R;
import com.cnh.pf.model.vip.vehimp.Vehicle;
import com.cnh.pf.model.vip.vehimp.VehicleCurrent;
import com.cnh.pf.model.vip.vehimp.VehicleMake;
import com.cnh.pf.model.vip.vehimp.VehicleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static javax.swing.UIManager.getString;

/**
 * Class for handling needed vehicle information to be used across the DM project
 */
public class VIPDataHandler implements IVIPDataHelper {
    private static final Logger logger = LoggerFactory.getLogger(VIPDataHandler.class);

    private IVIPServiceAIDL vipService = null;
    private volatile VehicleCurrent currentVehicle = null;
    private List<OnVehicleChangedListener> listeners;

    public VIPDataHandler() {
        listeners = new ArrayList<OnVehicleChangedListener>();
    }

    /**
     * Used to get make of the vehicle
     *
     * @return String make of vehicle or Unknown Vehicle if vehicle parameters are not set
     */
    @Override
    public String getMakeOfVehicle() {
        if (currentVehicle != null) {
            Vehicle vehicle = currentVehicle.getVehicle();
            if (vehicle != null) {
                VehicleModel vehicleModel = vehicle.getVehicleModel();
                if (vehicleModel != null) {
                    VehicleMake vehicleMake = vehicleModel.getVehicleMake();
                    if (vehicleMake != null) {
                        return vehicleMake.getMake();
                    }
                }
            }
        }
        return getString(R.string.unknown_vehicle);
    }

    @Override
    public synchronized void addOnVehicleChangedListener(OnVehicleChangedListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void removeOnVehicleChangedListener(OnVehicleChangedListener listener) {
        listeners.remove(listener);
    }

    private synchronized void notifyOnVehicleChanged(VehicleCurrent currentVehicle) {
        for (OnVehicleChangedListener listener : listeners) {
            listener.onCurrentVehicleChanged(currentVehicle);
        }
    }

    /**
     * Update current vehicle and notify of vehicle change
     * @param currentVehicle
     */
    public synchronized void updateCurrentVehicle(VehicleCurrent currentVehicle) {
        logger.debug("vipDataHandler.updateCurrentVehicle(vehicleCurrent) {}", currentVehicle);
        if(this.currentVehicle != currentVehicle) {
            this.currentVehicle = currentVehicle;

            notifyOnVehicleChanged(currentVehicle);
        }
    }

    /**
     * Setting of VIP Service
     * @param vipService
     */
    public void setVipService(IVIPServiceAIDL vipService) {
        this.vipService = vipService;
    }
}
