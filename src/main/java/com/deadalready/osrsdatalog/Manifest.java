package com.deadalready.osrsdatalog;

import lombok.Data;

@Data
public class Manifest
{
    final int version = -1;
    final int[] varbits;
    final int[] varps;

    // Constructor to initialize final arrays
    public Manifest(int[] varbits, int[] varps) {
        this.varbits = varbits;
        this.varps = varps;
    }
}