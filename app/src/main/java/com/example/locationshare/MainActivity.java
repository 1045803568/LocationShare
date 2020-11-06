package com.example.locationshare;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity implements LocationSource,
        AMapLocationListener, ServiceConnection {
    private AMap aMap;
    private MapView mapView;
    private OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private Send send=null;

    private TextView mLocationErrText;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private boolean mFirstFix = false;
    private boolean mFirstFix2 = false;
    private double socketIndex;//套接字本地客户端标志
    private double otherSocketIndex;//套接字外客户端标志
    private static double lat,lng;//实时定位的经纬度
    private static double accuracy;//实时定位精确度
    private double direct;//实时定位的方向角
    private String otherLocateData;//外地客户端传输的数据
    private BitmapDescriptor bitmap;
    private static double olat,olng;//外地客户端的经纬度
    private double odirect;//外地客户端定位的方向角
    private static double oaccuracy;//实时定位精确度
    private Marker mLocMarker1,mLocMarker2;
    private SensorEventHelper mSensorHelper;
    private Circle mCircle1;
    private Circle mCircle2;
    public static final String LOCATION_MARKER_FLAG = "mylocation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 不显示程序的标题栏
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写

        init();
        Intent intent = new Intent(MainActivity.this, Send.class);
        bindService(intent, MainActivity.this, Context.BIND_AUTO_CREATE);
        socketIndex = Math.random();// 用随机数初始化本地客户端的标志

    }

    /**
     * 初始化
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        mSensorHelper = new SensorEventHelper(this);
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
        mLocationErrText = (TextView) findViewById(R.id.location_errInfo_text);
        mLocationErrText.setVisibility(View.GONE);
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
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
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (mSensorHelper != null) {
            mSensorHelper.registerSensorListener();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorHelper != null) {
            mSensorHelper.unRegisterSensorListener();
            mSensorHelper.setCurrentMarker(null);
            mSensorHelper = null;
        }
        mapView.onPause();
        deactivate();
        mFirstFix = false;
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocMarker1 != null) {
            mLocMarker1.destroy();
        }
        mapView.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }

    /**
     * 定位信息初始化
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            mLocationOption.setHttpTimeOut(20000);  // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();

        }
    }
    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {//定位初始化成功
            if (amapLocation != null
                    && amapLocation.getErrorCode() == 0) {//编码为0 定位成功
                mLocationErrText.setVisibility(View.GONE);
                //获取经纬度、方向角
                lat = amapLocation.getLatitude();
                lng = amapLocation.getLongitude();
                direct = amapLocation.getBearing();
                accuracy = amapLocation.getAccuracy();
                //Toast.makeText(this, "lat"+lat +"    lng"+lng+"    direct"+direct, Toast.LENGTH_SHORT).show();
                System.out.println("lat"+lat +"    lng"+lng+"    direct"+direct+"    accuracy"+accuracy);
                LatLng location = new LatLng(lat,lng);//根据经纬度生成位置信息

                if (!mFirstFix) {//第一次定位
                    mFirstFix = true;
                    addCircle(location, accuracy);//添加定位精度圆
                    addMarker(location);//添加定位图标
                    mSensorHelper.setCurrentMarker(mLocMarker1);//定位图标旋转
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location,18));//锁定在定位处
                } else {
                    mCircle1.setCenter(location);
                    mCircle1.setRadius(accuracy);
                    mLocMarker1.setPosition(location);
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(location));
                }
                //发送数据
                if(send!=null){
                    ;Thread th= new Thread(new Runnable(){
                        @Override
                        public void run() {

                            send.bd.sendData(socketIndex + ":" + lat + ":" + lng + ":" + direct +":" + accuracy );
                        }
                    });
                    th.start();
                }
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
                mLocationErrText.setVisibility(View.VISIBLE);
                mLocationErrText.setText(errText);
            }
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
        mCircle1 = aMap.addCircle(options);
    }
    private void addCircle2(LatLng latlng, double radius) {
        CircleOptions options2 = new CircleOptions();
        options2.strokeWidth(1f);
        options2.fillColor(FILL_COLOR);
        options2.strokeColor(STROKE_COLOR);
        options2.center(latlng);
        options2.radius(radius);
        mCircle2 = aMap.addCircle(options2);
    }

    private void addMarker(LatLng latlng) {
        if (mLocMarker1!= null) {
            return;
        }
        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.navi_map_gps_locked)));
        options.anchor(0.5f, 0.5f);
        options.position(latlng);
        mLocMarker1 = aMap.addMarker(options);
        mLocMarker1.setTitle(LOCATION_MARKER_FLAG);
    }
    private void addMarker2(LatLng latlng) {
        if (mLocMarker2 != null) {
            return;
        }
        MarkerOptions options2 = new MarkerOptions();
        options2.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(),
                R.mipmap.navi_map_gps_locked2)));
        options2.anchor(0.5f, 0.5f);
        options2.position(latlng);
        mLocMarker2 = aMap.addMarker(options2);
        mLocMarker2.setTitle(LOCATION_MARKER_FLAG);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        //导入菜单布局
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        //创建菜单项的点击事件
        switch (item.getItemId()) {
            case R.id.member:
                Toast.makeText(this, "点击了成员列表", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this,MemberInfo.class);
                break;
            case R.id.membermanage:
                Toast.makeText(this, "点击了成员管理", Toast.LENGTH_SHORT).show();
                break;
            case R.id.set:
                Toast.makeText(this, "点击了设置", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        System.out.println("进入到了IBINDER");
        send = new Send();
        System.out.println("Service连接成功");
        send.setCallBack(new Send.ICallBack(){
            @Override
            public void onDateChange(String data) {// 直接使用一个新创建的线程来执行UI线程的资源的话，是不行的，是android的
                // 安全机制，UI线程是不允许其他辅助线程来修改其资源的；此处需要Handler。
                Message message = new Message();
                Bundle bundle = new Bundle();
                System.out.println("data"+data);
                bundle.putString("data", data);
                message.setData(bundle);
                handler.sendMessage(message);
            }
        });

    }
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            otherLocateData = msg.getData().getString("data");
            System.out.println("收到的其它客户端" + otherLocateData);
            // 解析其它客户端的定位数据：
            Pattern overallPattern = Pattern
                    .compile("\\d+\\.\\d+:\\d+\\.\\d+:\\d+\\.\\d+:[-]?\\d+\\.\\d+:\\d+\\.\\d");
            Matcher matcher = overallPattern.matcher(otherLocateData);
           // Pattern pattern = Pattern.compile("([-]?\\d+[.]?\\d{0,20})(:)");
 //           System.out.println(matcher);
                String[] d = otherLocateData.split(":");
                //Matcher littleMatcher = pattern.matcher(matcher.group());
                otherSocketIndex = Double.parseDouble(d[0]);
                olat = Double.parseDouble(d[1]);
                olng = Double.parseDouble(d[2]);
                odirect = Double.parseDouble(d[3]);
                oaccuracy = Double.parseDouble(d[4]);
                if(otherSocketIndex!=socketIndex){
                    LatLng olocation = new LatLng(olat,olng);//根据经纬度生成位置信息
                    if (!mFirstFix2) {//第一次定位
                        mFirstFix2 = true;
                        addCircle2(olocation, oaccuracy);//添加定位精度圆
                        addMarker2(olocation);//添加定位图标
                        mSensorHelper.setCurrentMarker(mLocMarker2);//定位图标旋转
                    } else {
                        mCircle2.setCenter(olocation);
                        mCircle2.setRadius(oaccuracy);
                        mLocMarker2.setPosition(olocation);
                    }
                }
//
                System.out.println("收到的其它客户端" + otherSocketIndex + " " + olat + " " + olng +  " " + odirect+" " + oaccuracy);
            }

    };
    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}