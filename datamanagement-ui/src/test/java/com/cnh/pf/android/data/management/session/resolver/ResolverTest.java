/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.session.resolver;

import com.cnh.jgroups.Datasource;
import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.android.data.management.helper.DatasourceHelper;
import com.cnh.pf.android.data.management.session.Session;
import com.cnh.pf.android.data.management.session.SessionException;
import com.cnh.pf.android.data.management.session.SessionExtra;
import org.jgroups.Address;
import org.jgroups.util.ExtendedUUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test session resolver for each session task
 */
@RunWith(RobolectricMavenTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class)
public class ResolverTest {
   @Mock
   DatasourceHelper dsHelper;
   List<Address> sources = new ArrayList<Address>();
   List<Address> destinations = new ArrayList<Address>();

   @Before
   public void setUp() {
      MockitoAnnotations.initMocks(this);
      sources.add(new ExtendedUUID());
      destinations.add(new ExtendedUUID());
   }

   /**
    * Perform the null test for resolver and check if the resolver can resolve without exception.
    *
    * @param resolver   session resolver
    * @param session    session
    */
   protected void checkResolver(Resolver resolver, Session session) {
      assertTrue(resolver != null);
      Exception ex = null;
      try {
         resolver.resolve(session);
      }
      catch (SessionException e) {
         ex = e;
      }
      assertTrue(ex == null);
   }

   @Test
   public void testDiscoveryResolve() {
      Session session = new Session();
      session.setType(Session.Type.DISCOVERY);

      // Make sure MANAGE discovery session gets resolved
      session.setAction(Session.Action.MANAGE);
      when(dsHelper.getAddressesForLocation(Datasource.LocationType.PCM)).thenReturn(sources);

      Resolver manageResolver = ResolverFactory.createResolver(dsHelper, session);
      checkResolver(manageResolver, session);

      // Make sure EXPORT discovery session gets resolved
      session.setAction(Session.Action.EXPORT);
      Resolver exportResolver = ResolverFactory.createResolver(dsHelper, session);
      checkResolver(exportResolver, session);

      // Make sure IMPORT discovery session gets resolved
      session.setAction(Session.Action.IMPORT);
      when(dsHelper.getAddressesForLocation(Datasource.LocationType.USB)).thenReturn(sources);

      Resolver importResolver = ResolverFactory.createResolver(dsHelper, session);
      checkResolver(importResolver, session);
   }

   @Test
   public void testPerformOperationsResolve() {
      SessionExtra exportExtra = new SessionExtra(SessionExtra.USB, "test extra", 1);
      exportExtra.setFormat("PF Database");

      Session session = new Session();
      session.setType(Session.Type.PERFORM_OPERATIONS);
      session.setAction(Session.Action.EXPORT);
      session.setExtra(exportExtra);

      when(dsHelper.getAddressesForLocation(Datasource.LocationType.PCM)).thenReturn(sources);
      when(dsHelper.getAddressesForLocation(Datasource.LocationType.USB)).thenReturn(destinations);

      // Make sure session for EXPORT, PERFORM_OPERATIONS gets resolved.
      Resolver exportResolver = ResolverFactory.createResolver(dsHelper, session);
      checkResolver(exportResolver, session);

      session.setAction(Session.Action.IMPORT);
      session.setExtra(exportExtra);

      when(dsHelper.getAddressesForLocation(Datasource.LocationType.PCM)).thenReturn(destinations);
      when(dsHelper.getAddressesForLocation(Datasource.LocationType.USB)).thenReturn(sources);

      // Make sure session for IMPORT, PERFORM_OPERATIONS gets resolved.
      Resolver importResolver = ResolverFactory.createResolver(dsHelper, session);
      checkResolver(importResolver, session);
   }

   @Test
   public void testCalculateConflistsResolve() {
      Session session = new Session();
      session.setType(Session.Type.CALCULATE_CONFLICTS);
      session.setAction(Session.Action.IMPORT);

      when(dsHelper.getAddressesForLocation(Datasource.LocationType.PCM)).thenReturn(destinations);

      // Make sure session for IMPORT, CALCULATE_CONFLICTS gets resolved.
      Resolver importResolver = ResolverFactory.createResolver(dsHelper, session);
      checkResolver(importResolver, session);
   }

   @Test
   public void testCalculateOperationsResolve() {
      Session session = new Session();
      session.setType(Session.Type.CALCULATE_OPERATIONS);
      session.setAction(Session.Action.IMPORT);

      when(dsHelper.getAddressesForLocation(Datasource.LocationType.PCM)).thenReturn(destinations);

      // Make sure session for IMPORT, CALCULATE_OPERATIONS gets resolved.
      Resolver importResolver = ResolverFactory.createResolver(dsHelper, session);
      checkResolver(importResolver, session);
   }
}
