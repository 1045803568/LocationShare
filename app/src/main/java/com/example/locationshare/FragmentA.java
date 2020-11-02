package com.example.locationshare;

import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;

public class FragmentA extends FragmentBase {
    public static final LatLng CHENGDU = new LatLng(30.573095, 104.066143);// 成都市经纬度
    protected static CameraPosition cameraPosition;

    @Override
    LatLng getTarget() {
        return CHENGDU;
    }

    @Override
    CameraPosition getCameraPosition() {
        return cameraPosition;
    }

    @Override
    void setCameraPosition(CameraPosition cameraPosition) {
        FragmentA.cameraPosition = cameraPosition;
    }
}
