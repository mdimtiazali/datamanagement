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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cnh.jgroups.Mediator;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * Provides Mediator implementation to all objects that depend on Mediator.
 * @author oscar.salazar@cnhind.com
 */
public class MediatorModule extends AbstractModule {

   private Application application;

   public MediatorModule(Application ctx) {
      this.application = ctx;
   }

   @Override
   protected void configure() {
      bind(Mediator.class).toProvider(new MediatorProvider(application)).in(Singleton.class);
   }

}
