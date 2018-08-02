/*
 * Copyright (C) 2018 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */
package com.cnh.pf.android.data.management.graph;

import com.cnh.jgroups.ObjectGraph;

public class GFFObject {
    private String name;
    private String type;
    private ObjectGraph dataObj;

    public GFFObject(String name, String type, ObjectGraph dataObj) {
        this.name = name;
        this.type = type;
        this.dataObj = dataObj;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ObjectGraph getDataObj() {
        return dataObj;
    }

    public void setDataObj(ObjectGraph dataObj) {
        this.dataObj = dataObj;
    }
}
