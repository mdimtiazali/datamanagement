/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 *
 */
package com.cnh.pf.android.data.management.session;

import com.cnh.pf.android.data.management.TestApp;
import com.cnh.pf.datamng.Process.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricMavenTestRunner;
import org.robolectric.annotation.Config;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Test Class for CacheManager.
 *
 * @author: Ranjith P.A
 */

@RunWith(RobolectricMavenTestRunner.class) @Config(manifest = "src/main/AndroidManifest.xml", application = TestApp.class) public class CacheManagerTest {

   private CacheManager cacheManager;
   private Session session;
   private CacheManager.CacheItem cacheItem;
   private Map<Session.Action, Map<Session.Type, CacheManager.CacheItem>> cache;

   @Before public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);

      cacheManager = new CacheManager();
      session = new Session();

      session.setAction(Session.Action.MANAGE);
      session.setType(Session.Type.DISCOVERY);
      session.setState(Session.State.COMPLETE);
      session.setResultCode(Result.SUCCESS);

      cache = new EnumMap<Session.Action, Map<Session.Type, CacheManager.CacheItem>>(Session.Action.class);

      Robolectric.getBackgroundThreadScheduler().pause();
      Robolectric.getForegroundThreadScheduler().pause();

   }

   @Test public void saveAndCacheTest() {

      Assert.assertNotNull(cacheManager);
      Assert.assertNotNull(session);
      cacheManager.save(session);
      Assert.assertTrue(cacheManager.cached(session));

   }

   @Test public void retrieveTest() {

      Assert.assertNotNull(cacheManager);
      Assert.assertNotNull(session);
      Assert.assertTrue(cacheManager.retrieve(session.getAction(), session.getType()) instanceof CacheManager.CacheItem);

   }

   @Test public void resetSessionTest() {

      Assert.assertNotNull(cacheManager);
      Assert.assertNotNull(session);
      cacheManager.save(session);
      cacheManager.reset(session);
      Assert.assertNull(cacheManager.getCache().get(session.getType()));

   }

   @Test public void resetActionTest() {

      Assert.assertNotNull(cacheManager);
      Assert.assertNotNull(session);
      cacheManager.save(session);
      cacheManager.reset(session.getAction());
      Assert.assertNull(cacheManager.getCache().get(session.getAction()));

   }

   @Test public void resetAllTest() {

      Assert.assertNotNull(cacheManager);
      Assert.assertNotNull(session);
      cacheManager.save(session);
      Assert.assertFalse(cacheManager.getCache().isEmpty());
      cacheManager.resetAll();
      Assert.assertEquals(cache, cacheManager.getCache());

   }

}
