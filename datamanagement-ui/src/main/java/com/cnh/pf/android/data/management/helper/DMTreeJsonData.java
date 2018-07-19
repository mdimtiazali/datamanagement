package com.cnh.pf.android.data.management.helper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by F48523C on 6/28/2018.
 */
public class DMTreeJsonData {
   private String Title;
   private String DataType;
   private int FollowSource;
   private int Hidden;
   private List<DMTreeJsonData> Children = new ArrayList<DMTreeJsonData>();

   @Override
   public  String toString() {
      return getDataType() + "-" + getTitle();
   }

   public String getTitle() {
      return Title;
   }

   public void setTitle(String title) {
      Title = title;
   }

   public String getDataType() {
      return DataType;
   }

   public void setDataType(String dataType) {
      DataType = dataType;
   }

   public int getFollowSource() {
      return FollowSource;
   }

   public void setFollowSource(int followSource) {
      FollowSource = followSource;
   }

   public int getHidden() {
      return Hidden;
   }

   public void setHidden(int hidden) {
      Hidden = hidden;
   }

   public List<DMTreeJsonData> getChildren() {
      return Children;
   }

   public void setChildren(List<DMTreeJsonData> children) {
      Children = children;
   }
}
