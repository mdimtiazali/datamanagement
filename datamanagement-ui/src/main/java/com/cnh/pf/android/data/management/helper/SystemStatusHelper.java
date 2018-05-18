/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.helper;

import com.cnh.pf.signal.Consumer;
import com.cnh.pf.signal.OnConnectionChangeListener;
import com.cnh.pf.signal.Signal;
import com.cnh.pf.signal.SignalUri;
import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
/**
 * to provide monitor the working and engage status
 * @author Lifeng Liang
 */
public class SystemStatusHelper implements OnConnectionChangeListener, Consumer.Listener{
    private static final Logger logger = LoggerFactory.getLogger(SystemStatusHelper.class);
    private Consumer consumer;
    private List<Listener> listeners = new LinkedList<Listener>();
    private boolean isWorking = false;
    private boolean isEngage = false;

    public interface Listener{
        /**
         * listener for when status change
         * @param status true for accessible for dm and false for no
         */
        void onSystemStatus(boolean status);
    }
    @Inject
    @Named("daemon")
    private HostAndPort signalDeamonAddress;

    @Override
    public void onConnectionChanged(boolean b) {
        logger.debug("Consumer connectted: {}", b);
    }

    /**
     * constructor
     *@param listener listener for callback
     */
    public SystemStatusHelper(Listener listener) {
        this.listeners.add(listener);
        listener.onSystemStatus(isDmAccessable());
    }

    @Override
    public void onUpdate(String s, @Nonnull Signal.Header header, @Nullable Message message) {
        synchronized (this){
            if(s.equals(SignalUri.WORK_SYSTEM)){
                Signal.SystemWork systemWork = (Signal.SystemWork) message;
                isWorking = systemWork.getState()== Signal.WorkState.IN_WORK;
                logger.debug("Work system update : {}",isWorking);
            }
            else if(s.equals(SignalUri.GUIDANCE_MODULARGUIDANCE_ENGAGEMANAGER_ENGAGED)){
                Signal.Boolean engageState = (Signal.Boolean) message;
                isEngage = engageState.getValue();
                logger.debug("Engage update : {}", isEngage);
            }
            for(Listener l: listeners){
                l.onSystemStatus(isDmAccessable());
            }
        }
    }

    private boolean isDmAccessable(){
        synchronized (this){
            return !(isEngage || isWorking);
        }
    }
    /**
     * start the system status monitor.
     *
     */
    public void start(){
        if(consumer != null) {
            logger.debug("start()");
            consumer = new Consumer(signalDeamonAddress);
            consumer.start();
        }

    }
    /**
     * subscribe to listen to system work and engage channel
     *
     */
    public void subscribe(){
        if(consumer != null){
            logger.debug("subscribe()");
            consumer.subscribe(SignalUri.WORK_SYSTEM, this);
            consumer.subscribe(SignalUri.GUIDANCE_MODULARGUIDANCE_ENGAGEMANAGER_ENGAGED, this);
        }
    }
    /**
     * unsubscribe to listen to system work and engage channel
     *
     */
    public void unsubscribe(){
        if(consumer != null){
            logger.debug("unsubscribe()");
            consumer.unsubscribe(SignalUri.WORK_SYSTEM);
            consumer.unsubscribe(SignalUri.GUIDANCE_MODULARGUIDANCE_ENGAGEMANAGER_ENGAGED);
        }
    }
    /**
     * stop the system status monitor.
     *
     */
    public void stop(){
        if(consumer != null){
            consumer.close();
            consumer = null;
        }
    }
    /**
     * set callback to the system status monitor, so it will callback when there is status change
     *@param listener listener for callback
     */
    public void setListeners(Listener listener){
        synchronized (this) {
            listeners.add(listener);
        }
        listener.onSystemStatus(isDmAccessable());
    }
}
