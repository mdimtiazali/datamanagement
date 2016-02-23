/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.helper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import com.google.inject.Guice;
import com.google.inject.HierarchyTraversalFilter;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import roboguice.RoboGuice;
import roboguice.config.DefaultRoboModule;
import roboguice.event.EventManager;
import roboguice.event.EventThread;
import roboguice.event.Observes;
import roboguice.event.eventListener.ObserverMethodListener;
import roboguice.event.eventListener.factory.EventListenerThreadingDecorator;

import static roboguice.RoboGuice.getInjector;

/**
 * @author kedzie
 */
public class ObservesTypeListener2 implements TypeListener {
   protected Provider<Context> contextProvider;
   protected EventListenerThreadingDecorator observerThreadingDecorator;
   private HierarchyTraversalFilter filter;

   public ObservesTypeListener2(Provider<Context> contextProvider, EventListenerThreadingDecorator observerThreadingDecorator) {
      this.contextProvider = contextProvider;
      this.observerThreadingDecorator = observerThreadingDecorator;
   }

   public <I> void hear(TypeLiteral<I> iTypeLiteral, TypeEncounter<I> iTypeEncounter) {
      if( filter == null ) {
         filter = Guice.createHierarchyTraversalFilter();
      } else {
         filter.reset();
      }
      Class<?> c = iTypeLiteral.getRawType();
      while( isWorthScanning(c)) {
         for (Method method : filter.getAllMethods(Observes.class.getName(), c)) {
            findContextObserver(method, iTypeEncounter);
         }
         for( Class<?> interfaceClass : c.getInterfaces()) {
            for (Method method : filter.getAllMethods(Observes.class.getName(), interfaceClass)) {
               findContextObserver(method, iTypeEncounter);
            }
         }
         c = c.getSuperclass();
      }
   }

   private boolean isWorthScanning(Class<?> c) {
      return filter.isWorthScanningForMethods(Observes.class.getName(), c);
   }

   protected <I> void findContextObserver(Method method, TypeEncounter<I> iTypeEncounter) {
      final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      for(int i = 0; i < parameterAnnotations.length; i++) {
         final List<Annotation> annotations = Arrays.asList(parameterAnnotations[i]);
         final Class<?> parameterType = method.getParameterTypes()[i];

         EventThread threadType = null;
         String name = null;

         for(Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Observes.class)) {
               threadType = ((Observes) annotation).value();
            } else if(annotation.annotationType().equals(com.google.inject.name.Named.class)) {
               name = ((com.google.inject.name.Named) annotation).value();
            }  else if(annotation.annotationType().equals(javax.inject.Named.class)) {
               name = ((javax.inject.Named) annotation).value();
            }
         }
         registerContextObserver(iTypeEncounter, method, parameterType, threadType, name);
      }
   }

   /**
    * Error checks the observed method and registers method with typeEncounter
    *
    * @param iTypeEncounter
    * @param method
    * @param parameterType
    * @param threadType
    * @param <I, T>
    */
   protected <I, T> void registerContextObserver(TypeEncounter<I> iTypeEncounter, Method method, Class<T> parameterType, @Nonnull EventThread threadType, @Nullable String name) {
      checkMethodParameters(method);
      Key<EventManager> key = name != null ?
            Key.get(EventManager.class, Names.named(name)) :
            Key.get(EventManager.class);
      Provider<EventManager> eventManagerProvider = RoboGuice.getInjector(contextProvider.get()).getProvider(key);
      iTypeEncounter.register(new ContextObserverMethodInjector<I, T>(eventManagerProvider, observerThreadingDecorator, method, parameterType,threadType));
   }

   /**
    * Error checking method, verifies that the method has the correct number of parameters.
    *
    * @param method
    */
   protected void checkMethodParameters(Method method) {
      if(method.getParameterTypes().length > 1)
         throw new RuntimeException("Annotation @Observes must only annotate one parameter," +
               " which must be the only parameter in the listener method.");
   }

   /**
    * Injection listener to handle the observation manager registration.
    *
    * @param <I>
    */
   public static class ContextObserverMethodInjector<I, T> implements InjectionListener<I> {
      protected EventListenerThreadingDecorator observerThreadingDecorator;
      protected Provider<EventManager> eventManagerProvider;
      protected Method method;
      protected Class<T> event;
      protected EventThread threadType;

      public ContextObserverMethodInjector(Provider<EventManager> eventManagerProvider,
            EventListenerThreadingDecorator observerThreadingDecorator,  Method method,
            Class<T> event, EventThread threadType) {
         this.observerThreadingDecorator = observerThreadingDecorator;
         this.eventManagerProvider = eventManagerProvider;
         this.method = method;
         this.event = event;
         this.threadType = threadType;
      }

      @SuppressWarnings({ "rawtypes", "unchecked" })
      public void afterInjection(I i) {
         eventManagerProvider.get().registerObserver( event, observerThreadingDecorator.decorate(threadType, new ObserverMethodListener(i, method)));
      }
   }
}
