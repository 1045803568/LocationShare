package com.example.locationshare;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.List;

public  class FragmentBase extends Fragment implements LocationSource, AMapLocationListener{
    private static final String TAG = "";
    private TextureMapView textureMapView;
    private AMap aMap;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;

    private TextView mLocationErrText;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private boolean mFirstFix = false;
    private boolean isFirst = true;
    private Marker mLocMarker;
    private SensorEventHelper mSensorHelper;
    private Circle mCircle;
    public static final String LOCATION_MARKER_FLAG = "mylocation";
    private static double lat,lng;
    private double latitude,longitude;//接收共享位置的经纬度
    private List<Marker> list;//存放共享位置的list

    private boolean isClick = false;
    private Marker marker,markerOwner;
    public FragmentBase() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_fragment, container, false);
        initview(savedInstanceState,view);
        Log.i(TAG, "onCreateView: 1");
        return view;

    }



    private void initview( Bundle savedInstanceState,View view) {
        textureMapView = (TextureMapView) view.findViewById(R.id.map);
        textureMapView.onCreate(savedInstanceState);
        if (aMap==null) {
            Log.i(TAG, "initview: 1");
            aMap = textureMapView.getMap();
            setUpMap();
        }
        Log.i(TAG, "initview: 2");
        mSensorHelper = new SensorEventHelper(this.getActivity());
        if (mSensorHelper!=null) {
            mSensorHelper.registerSensorListener();
        }

    }

    protected void setUpMap(){
        Log.i(TAG, "setUpMap: 开始定位监听了");

        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onResume() {
        super.onResume();
        textureMapView.onResume();
        if(mSensorHelper!=null){
            mSensorHelper.registerSensorListener();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onPause() {
        super.onPause();
        if(mSensorHelper!=null){
            mSensorHelper.unRegisterSensorListener();
            mSensorHelper.setCurrentMarker(null);
            mSensorHelper = null;
        }
        textureMapView.onPause();
        deactivate();
        mFirstFix = false;
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        textureMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onDestroy() {
        //setCameraPosition(aMap.getCameraPosition());
        super.onDestroy();
        if(mLocMarker!=null){
            mLocMarker.destroy();
        }
        textureMapView.onDestroy();
        if(null!=mlocationClient){
            mlocationClient.onDestroy();
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {
                mLocationErrText.setVisibility(View.GONE);
                LatLng location = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
                Log.i(TAG, "onLocationChanged: 开始调用传感器");
                if (!mFirstFix){
                    mFirstFix = true;
                    addCircle(location, amapLocation.getAccuracy());//添加定位精度圆
                    addMarker(location);//添加定位图标
                    mSensorHelper.setCurrentMarker(mLocMarker);//定位图标旋转
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,18));
                }
                lat = amapLocation.getLatitude();
                lng = amapLocation.getLongitude();

                Log.i(TAG, "onLocationChanged: "+lat);
                Log.i(TAG, "onLocationChanged: "+lng);
                if (isClick){
                    if (markerOwner != null ){
                        markerOwner.remove();//每次定位发生改变的时候,把自己的marker先移除再添加
                    }
                    markerOwner = (Marker)aMap.addMarker((help_add_icon(new LatLng(lat,lng), R.mipmap.navi_map_gps_locked)));
                }
            } else {

                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
                mLocationErrText.setVisibility(View.VISIBLE);
                mLocationErrText.setText(errText);
            }
        }
        Toast.makeText(this.getContext(), "定位回调", Toast.LENGTH_LONG).show();

    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this.getActivity());
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            mLocationOption.setInterval(1000);// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
            Toast.makeText(this.getContext(), "激活定位", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    private void addCircle(LatLng latlng, double radius) {
        CircleOptions options = new CircleOptions();
        options.strokeWidth(1f);
        options.fillColor(FILL_COLOR);
        options.strokeColor(STROKE_COLOR);
        options.center(latlng);
        options.radius(radius);
        mCircle = aMap.addCircle(options);
    }

    private void addMarker(LatLng latlng) {
        if (mLocMarker != null) {
            return;
        }
        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.navi_map_gps_locked)));
        options.anchor(0.5f, 0.5f);
        options.position(latlng);
        mLocMarker = aMap.addMarker(options);
        mLocMarker.setTitle(LOCATION_MARKER_FLAG);
    }
    public static double getLat(){
        return  lat;
    }
    public static double getLng(){
        return lng;
    }
    /**
     * 添加所接收到的共享位置信息
     * @param jsonArray
     */
    public void allLatLng(JSONArray jsonArray){
        try{
            if (list.size() != 0){
                Remove(list);
            }
            for (int i = 0;i<jsonArray.length();i++){
                JSONObject jsonObject = new JSONObject(jsonArray.get(i).toString());
                latitude = jsonObject.getDouble("lat");
                longitude = jsonObject.getDouble("lng");
                LatLng latLng = new LatLng(latitude,longitude);
                marker = (Marker) (aMap.addMarker(help_add_icon(latLng, R.mipmap.navi_map_gps_locked)));
                list.add(marker);
            }
        }catch (Exception e){
            Log.i("130","解析出错"+e.getMessage());
        }
    }
    /**
     * 手机上显示共享位置的图标
     * @param latLng
     * @param id
     * @return
     */
    public static MarkerOptions help_add_icon(LatLng latLng,int id){
        MarkerOptions markOptiopns = new MarkerOptions().position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(id));
        return markOptiopns;
    }

    /**
     * 移除
     * @param list
     */
    public static void Remove(List<Marker> list){
        if (list != null) {
            for (Marker marker : list) {
                marker.remove();
            }
        }
    }

}
