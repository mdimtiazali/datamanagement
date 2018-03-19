/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package pl.polidea.treeview;

import java.util.Comparator;

/**
 * Interface to add sortable feature to the tree state manager
 * @Author: junsu.shin@cnhind.com
 */
public interface SortableChildrenTree<T> {
    void sortChildren(Comparator<? super InMemoryTreeNode<T>> comparator);
}
