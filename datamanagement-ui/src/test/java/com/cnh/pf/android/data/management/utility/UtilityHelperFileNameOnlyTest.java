/*
 *  Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 *  This software contains proprietary information of CNH Industrial NV. Neither
 *  receipt nor possession thereof confers any right to reproduce, use, or
 *  disclose in whole or in part any such information without written
 *  authorization from CNH Industrial NV.
 *
 */

package com.cnh.pf.android.data.management.utility;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class UtilityHelperFileNameOnlyTest {

   @Parameterized.Parameter
   public String input;
   @Parameterized.Parameter(1)
   public String expected;

   @Parameterized.Parameters
   public static Collection<String[]> data() {
      return Arrays.asList(new String[][] {
         {
            null,
            null
         },
         {
            "",
            null
         },
         {
            "Test" + File.separator + "Test.xml",
            "Test.xml"
         },
         {
            "Test" + File.separator + "Test" + File.separator + "Test2.xml",
            "Test2.xml"
         },
         {
            "" + File.separator + "Test" + File.separator + "Test3.xml",
            "Test3.xml"
         }
      });
   }

   @Test
   public void testFilenameOnlyValid() {
      assertThat(UtilityHelper.filenameOnly(input), is(expected));
   }

}