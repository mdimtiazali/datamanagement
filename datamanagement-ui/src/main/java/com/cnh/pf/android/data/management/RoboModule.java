/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management;

import javax.inject.Named;
import javax.inject.Singleton;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import com.cnh.jgroups.Mediator;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Roboguice module definition
 * @author kedzie
 */
public class RoboModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(RoboModule.class);

    public static final String GLOBAL_PREFERENCES_PACKAGE = "com.cnh.pf.android.preference";

    private Application application;

    public RoboModule(Application ctx) {
        this.application = ctx;
    }

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    @Named("global")
    @SuppressWarnings("deprecation")
    private SharedPreferences getPrefs() throws PackageManager.NameNotFoundException {
        return application.createPackageContext(GLOBAL_PREFERENCES_PACKAGE, Context.CONTEXT_IGNORE_SECURITY)
           .getSharedPreferences(GLOBAL_PREFERENCES_PACKAGE, Context.MODE_WORLD_READABLE);
    }

    @Provides
    public JChannel getChannel(@Named("global") SharedPreferences prefs) throws Exception {
        String config = prefs.getString("jgroups_config", "jgroups.xml");
        boolean gossip = config.equals("jgroups-tunnel.xml");
        boolean tcp = config.equals("jgroups-tcp.xml");
        if(gossip || tcp) {
            System.setProperty("jgroups.tunnel.gossip_router_hosts", String.format("%s[%s]",
               prefs.getString("jgroups_gossip_host", "10.0.0.11"),
               prefs.getString("jgroups_gossip_port", "12001")));
        }
        if(tcp) {
            String tcpExternalHost = prefs.getString("jgroups_external_host", "");
            String tcpExternalPort = prefs.getString("jgroups_external_port", "");
            if(!tcpExternalHost.isEmpty()) {
                System.setProperty(Global.EXTERNAL_ADDR, tcpExternalHost);
                if(!tcpExternalPort.isEmpty()) {
                    System.setProperty("jgroups.tcp.bind_port", tcpExternalPort);
                    System.setProperty(Global.EXTERNAL_PORT, tcpExternalPort);
                }
            }
        }
        logger.info("Using JGroups config {}", config);
        return new JChannel(config);
    }

    @Provides
    public Mediator getMediator(JChannel channel) {
        return new Mediator(channel, "Android");
    }
}
