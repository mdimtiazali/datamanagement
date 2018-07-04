package com.cnh.pf.android.data.management.helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by F48523C on 6/28/2018.
 */
public class DMTreeJsonData {
   public String Title;
   public String DataType;
   public int FollowSource;
   public int Hidden;
   public List<DMTreeJsonData> Children = new ArrayList<DMTreeJsonData>();

   @Override
   public  String toString() {
      return DataType  + "-" + Title;
   }
}
