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
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Application;
import android.content.Context;

import com.cnh.pf.android.data.management.R;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Class parses xml document which list all entities supported by data formats
 * @author oscar.salazar@cnhind.com
 */
@Singleton public class FormatManager {

   Context context;
   Map<String, Format> formatMap;

   private static String FORMAT = "format";
   private static String FORMAT_NAME = "name";
   public static String TYPE = "type";

   @Inject
   public FormatManager(Application context) {
      this.context = context;
      this.formatMap = null;
   }

   private Format parseFormat(XmlPullParser parser) throws IOException, XmlPullParserException {
      Format format = new Format();
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
      return formatMap.keySet();
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
               for (int idx = 0; idx < xpp.getAttributeCount(); idx++) {
                  if (xpp.getAttributeName(idx).equals(FORMAT_NAME)) {
                     formatMap.put(xpp.getAttributeValue(idx), parseFormat(xpp));
                  }
               }
            }
            eventType = xpp.next();
         }
      }
   }


   public static class Format {
      public List<String> includes = new ArrayList<String>();
      public List<String> excludes = new ArrayList<String>();
   }
}
