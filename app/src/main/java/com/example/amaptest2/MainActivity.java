package com.example.amaptest2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MultiPointOverlayOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.ServiceSettings;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationSource,
        AMapLocationListener, AMap.OnCameraChangeListener, PoiSearch.OnPoiSearchListener{
    MapView mapView;
    ListView mapList;

    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";
    public static final String KEY_DES = "des";


    private AMapLocationClient mLocationClient;
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocation aMapLocation;
    private LatLng latlng;
    private String city = null;
    private AMap aMap;
    private Marker locationMarker; // 选择的点
    private String deepType = "";// poi搜索类型
    private PoiSearch.Query query;// Poi查询条件类
    private PoiSearch poiSearch;
    private PoiResult poiResult; // poi返回的结果
    //        private PoiOverlay poiOverlay;// poi图层
    private List<PoiItem> poiItems;// poi数据
    private List<Marker> listMarkers;//地点图标
    private PoiSearch_adapter adapter; //普通的 ListView 的 adapter，需根据需要自行编写

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MapsInitializer.updatePrivacyShow(this,true,true);
        MapsInitializer.updatePrivacyAgree(this,true);

        mapView = findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapList = findViewById(R.id.map_list);
        applyForRight();
        //init();
    }
    //申请权限
    private void applyForRight() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,
            }, 1);
        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "拒绝访问", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                }
                init();
        }
    }
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnCameraChangeListener(this);
            setUpMap();
//                doSearchQuery();
        }
        deepType = "010000";//这里以餐饮为例
    }

    //-------- 定位 Start ------

    private void setUpMap() {
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mLocationClient.setLocationListener( this);
            //设置为高精度定位模式
            mLocationOption.setOnceLocation(true);
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mLocationClient.setLocationOption(mLocationOption);
            mLocationClient.startLocation();
        }
        // 自定义系统定位小蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.local_icon));// 设置小蓝点的图标
        myLocationStyle.strokeColor(Color.BLACK);// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 180));// 设置圆形的填充颜色
        myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setLocationSource( this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        doSearchQuery();
    }


    /**
     * 开始进行poi搜索
     */
    protected void doSearchQuery() {
//            aMap.setOnMapClickListener(null);// 进行poi搜索时清除掉地图点击事件
        query = new PoiSearch.Query("火锅", "", city);// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        query.setPageSize(20);// 设置每页最多返回多少条poiitem
        query.setPageNum(1);// 设置查第一页
//        getLatlon(city);
        if (latlng != null) {
            LatLonPoint lp = new LatLonPoint(latlng.latitude, latlng.longitude);
            try {
                poiSearch = new PoiSearch(this, query);
            } catch (AMapException e) {
                e.printStackTrace();
            }
//            MarkerOptions markerOption = new MarkerOptions().position(latlng)
//                    .draggable(false)
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_local));
//            //将数据添加到地图上
//            Marker marker = aMap.addMarker(markerOption);

            poiSearch.setOnPoiSearchListener( this);
            poiSearch.setBound(new PoiSearch.SearchBound(lp, 10000));
            poiSearch.searchPOIAsyn();// 异步搜索
        }
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                // 显示我的位置
                mListener.onLocationChanged(aMapLocation);
                //设置第一次焦点中心
                latlng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 14), 1000, null);

                city = aMapLocation.getProvince();
                doSearchQuery();
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener listener) {
        mListener = listener;
        mLocationClient.startLocation();
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        aMap.clear();
//        MarkerOptions markerOption = new MarkerOptions().position(latlng)
//                .draggable(false)
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_local));
//        //将数据添加到地图上
//        aMap.addMarker(markerOption);
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        latlng = cameraPosition.target;
        aMap.clear();
        MarkerOptions markerOption = new MarkerOptions().position(latlng)
                .draggable(false)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_background));
        //将数据添加到地图上
        aMap.addMarker(markerOption);
        doSearchQuery();
    }

    public void addMakerOption(List list) {
        for (int i = 0; i < list.size(); i++) {
            MarkerOptions markerOption = new MarkerOptions().position(latlng)
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_background));
            //将数据添加到地图上
//            aMap.addMarker(markerOption);
            listMarkers.add(aMap.addMarker(markerOption));
        }
        listMarkers.get(0).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_background));
    }


    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    poiResult = result;
                    poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始
                    Log.d("conttte", poiItems.toString());
                    List<SuggestionCity> suggestionCities = poiResult
                            .getSearchSuggestionCitys();
                    if (poiItems != null && poiItems.size() > 0) {
                        adapter = new PoiSearch_adapter(this, poiItems);
                        mapList.setAdapter(adapter);
                        mapList.setOnItemClickListener(new mOnItemClickListener());
                        /*注释掉高德原有的PoiOverlay*/
//                        PoiOverlay poiOverlay = new PoiOverlay(aMap, poiItems);
//                        poiOverlay = new MyOverlay(aMap, poiItems);
//                        poiOverlay.removeFromMap();
//                        poiOverlay.addToMap();
//                        poiOverlay.zoomToSpan();
                        addMakerOption(poiItems);
                    }
                } else {
                    Log.d("wjg", "无结果");
                }
            }
        } else if (rCode == 27) {
            Log.d("errnet", "error_network");
        } else if (rCode == 32) {
            Log.d("errkey", "error_key");
        } else {
            Log.d("erroth", "error_other：" + rCode);
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    //-------- 定位 End ------

    @Override
    protected void onResume() {
        super.onResume();
        mLocationClient.startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationClient.stopLocation();
    }

    @Override
    protected void onDestroy() {
        mLocationClient.onDestroy();
        super.onDestroy();
    }

    private class mOnItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            latlng = new LatLng(poiItems.get(position).getLatLonPoint().getLatitude(), poiItems.get(position).getLatLonPoint().getLongitude());
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17), 1000, null);
            aMap.clear();
//            MarkerOptions markerOption = new MarkerOptions().position(latlng)
//                    .draggable(false)
//                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_local));
//            //将数据添加到地图上
//            aMap.addMarker(markerOption);
//            Intent intent = new Intent();
//            Log.d("itemclick", String.valueOf(poiItems.get(position).getLatLonPoint().getLatitude()));
//            intent.putExtra(KEY_LAT, poiItems.get(position).getLatLonPoint().getLatitude());
//            intent.putExtra(KEY_LNG, poiItems.get(position).getLatLonPoint().getLongitude());
//            intent.putExtra(KEY_DES, poiItems.get(position).getTitle());
//            setResult(RESULT_OK, intent);
//            finish();
        }
    }

}