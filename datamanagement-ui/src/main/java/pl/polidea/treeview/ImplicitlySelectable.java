/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package pl.polidea.treeview;

/**
 * @author kedzie
 */
public interface ImplicitlySelectable {

      void setImplicitlySelected(boolean selected);
      boolean isImplicitlySelected();

      void setSupported(boolean supported);
      boolean isSupported();
}
