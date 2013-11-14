package com.mediaworx.intellij.opencmsplugin.entities;

import java.util.Date;

public class SyncFile extends SyncEntity {

    private Date lastChangeDate;

    public Date getLastChangeDate() {
        return (Date)lastChangeDate.clone();
    }

    public void setLastChangeDate(Date lastChangeDate) {
        this.lastChangeDate = (Date)lastChangeDate.clone();
    }
}