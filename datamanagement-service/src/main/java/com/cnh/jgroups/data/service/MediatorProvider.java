/*
 *  Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.jgroups.data.service;

import android.app.Application;

import org.jgroups.JChannel;

import com.cnh.jgroups.Mediator;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * {@link Mediator} handles all communication to datasources. It queries datasources for
 * conflicts, targets, discovery, and perform operations. This call provides Mediator instance
 * to any class depending on Mediator.
 * @author oscar.salazar@cnhind.com
 */
public class MediatorProvider implements Provider<Mediator> {

   @Inject Provider<JChannel> channelProvider;

   Application application;

   public MediatorProvider(Application application) {
      this.application = application;
   }

   @Override
   public Mediator get() {
      try {
         return new Mediator(channelProvider.get(), "DataManagerService");
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      return null;
   }
}
