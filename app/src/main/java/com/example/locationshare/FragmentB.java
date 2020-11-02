package com.example.locationshare;

import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;

public class FragmentB extends FragmentBase {
    public static final LatLng CHONGQIN= new LatLng(29.563761, 106.550464);// 成都市经纬度
    protected static CameraPosition cameraPosition;

    @Override
    LatLng getTarget() {
        return CHONGQIN;
    }


    CameraPosition getCameraPosition() {
        return cameraPosition;
    }

    @Override
    void setCameraPosition(CameraPosition cameraPosition) {
        FragmentA.cameraPosition = cameraPosition;
    }
}
