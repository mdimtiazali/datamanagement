/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Application;
import android.content.Context;
import com.cnh.android.util.prefs.GlobalPreferences;
import com.cnh.pf.android.data.management.R;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Class parses xml document which list all entities supported by data formats
 * @author oscar.salazar@cnhind.com
 */
@Singleton public class FormatManager {
   private static final Logger log = LoggerFactory.getLogger(FormatManager.class);

   Context context;
   private GlobalPreferences globalPreferences;
   Map<String, Format> formatMap;

   private static String FORMAT = "format";
   private static String FORMAT_NAME = "name";
   private static String FORMAT_PCM = "pcm";
   private static String FORMAT_STANDALONE = "standalone";
   public static String TYPE = "type";

   @Inject
   public FormatManager(Application context, GlobalPreferences globalPreferences) {
      this.context = context;
      this.formatMap = null;
      this.globalPreferences = globalPreferences;
   }

   private Format parseFormat(XmlPullParser parser) throws IOException, XmlPullParserException {
      Format format = new Format();
      for (int idx = 0; idx < parser.getAttributeCount(); idx++) {
         if (parser.getAttributeName(idx).equals(FORMAT_NAME)) {
            format.name = parser.getAttributeValue(idx);
         }
         else if (parser.getAttributeName(idx).equals(FORMAT_PCM)) {
            format.pcmMode = Boolean.valueOf(parser.getAttributeValue(idx));
         }
         else if (parser.getAttributeName(idx).equals(FORMAT_STANDALONE)) {
            format.standaloneMode = Boolean.valueOf(parser.getAttributeValue(idx));
         }
      }
      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT && !(eventType == XmlPullParser.END_TAG && parser.getName().equals(FORMAT))) {
         if (eventType == XmlPullParser.START_TAG && parser.getName().equals(TYPE)) {
            if (parser.next() == XmlPullParser.TEXT) {
               String text = parser.getText();
               if(text.startsWith("!")) {
                  format.excludes.add(text.substring(1));
               } else {
                  format.includes.add(text);
               }
            }
         }
         eventType = parser.next();
      }
      return format;
   }

   /**
    * Check if type supported by this format
    */
   public boolean formatSupportsType(String format, String type) {
      if(!formatMap.containsKey(format)) return false;
      Format f = formatMap.get(format);

      if(!f.includes.isEmpty() && f.includes.contains(type))
         return true;

      if(!f.excludes.isEmpty() && !f.excludes.contains(type))
         return true;

      return false;
   }

   /**
    * Return list of all formats supported
    */
   public Set<String> getFormats() {
      Set<String> formats = new HashSet<String>(formatMap.size());
      boolean pcmMode = globalPreferences.hasPCM();
      for (Format format : formatMap.values()) {
         if ((format.standaloneMode && !pcmMode) || (format.pcmMode && pcmMode)) {
            formats.add(format.name);
         }
      }
      return formats;
   }

   /**
    * Parses xml and populates map
    * @throws IOException
    * @throws XmlPullParserException
    */
   public void parseXml() throws IOException, XmlPullParserException {
      if (formatMap == null) {
         formatMap = new HashMap<String, Format>();
         XmlPullParser xpp = context.getResources().getXml(R.xml.formats);
         int eventType = xpp.getEventType();
         while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xpp.getName().equals(FORMAT)) {
               Format f = parseFormat(xpp);
               formatMap.put(f.name, f);
            }
            eventType = xpp.next();
         }
      }
   }


   public static class Format {
      public String name;
      public boolean pcmMode;
      public boolean standaloneMode;
      public List<String> includes = new ArrayList<String>();
      public List<String> excludes = new ArrayList<String>();
   }
}
