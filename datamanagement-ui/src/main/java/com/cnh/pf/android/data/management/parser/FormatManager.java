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
   Map<String, List<String>> formatMap;

   private static String FORMAT = "format";
   private static String FORMAT_NAME = "name";
   public static String TYPE = "type";

   @Inject
   public FormatManager(Application context) {
      this.context = context;
      this.formatMap = null;
   }

   /**
    * Returns supported types by this format
    * @return List of supported types
    */
   public List<String> getFormat(String format) throws XmlPullParserException, IOException {
      return formatMap.containsKey(format) ? formatMap.get(format) : new ArrayList<String>();
   }

   private List<String> parseFormat(XmlPullParser parser) throws IOException, XmlPullParserException {
      List<String> types = new ArrayList<String>();
      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT || (eventType == XmlPullParser.END_TAG && !parser.getName().equals(FORMAT))) {
         if (eventType == XmlPullParser.START_TAG && parser.getName().equals(TYPE)) {
            if (parser.next() == XmlPullParser.TEXT) {
               types.add(parser.getText());
            }
         }
         eventType = parser.next();
      }
      return types;
   }

   /**
    * Check if type supported by this format
    */
   public boolean formatSupportsType(String format, String type) {
      return formatMap.containsKey(format) && formatMap.get(format).contains(type);
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
         formatMap = new HashMap<String, List<String>>();
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
}
