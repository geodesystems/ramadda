/**
Copyright (c) 2008-2026 Geode Systems LLC
SPDX-License-Identifier: Apache-2.0
*/

package org.ramadda.data.record;

import org.ramadda.data.record.*;

import java.io.*;

public abstract class GeoRecord extends BaseRecord {

    public GeoRecord() {}

    public GeoRecord(RecordFile recordFile) {
        super(recordFile);
    }

    public GeoRecord(GeoRecord that) {
        super(that);
    }

    public GeoRecord(RecordFile recordFile, boolean bigEndian) {
        super(recordFile, bigEndian);
    }

    public abstract double getLatitude();

    public abstract double getLongitude();

    public abstract double getAltitude();

    public boolean isValidPosition() {
        if ((getLatitude() <= 90) && (getLatitude() >= -90)
                && (getLongitude() >= -180) && (getLongitude() <= 360)) {
            return true;
        }

        return false;
    }

    public boolean isValidAltitude() {
        return getAltitude() == getAltitude();
    }

    public boolean needsValidPosition() {
        return true;
    }

}
