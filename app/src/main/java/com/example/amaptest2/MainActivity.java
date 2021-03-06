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
    private Marker locationMarker; // ????????????
    private String deepType = "";// poi????????????
    private PoiSearch.Query query;// Poi???????????????
    private PoiSearch poiSearch;
    private PoiResult poiResult; // poi???????????????
    //        private PoiOverlay poiOverlay;// poi??????
    private List<PoiItem> poiItems;// poi??????
    private List<Marker> listMarkers;//????????????
    private PoiSearch_adapter adapter; //????????? ListView ??? adapter??????????????????????????????

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
    //????????????
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
                            Toast.makeText(this, "????????????", Toast.LENGTH_SHORT).show();
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
        deepType = "010000";//?????????????????????
    }

    //-------- ?????? Start ------

    private void setUpMap() {
        if (mLocationClient == null) {
            mLocationClient = new AMapLocationClient(getApplicationContext());
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            //??????????????????
            mLocationClient.setLocationListener( this);
            //??????????????????????????????
            mLocationOption.setOnceLocation(true);
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //??????????????????
            mLocationClient.setLocationOption(mLocationOption);
            mLocationClient.startLocation();
        }
        // ??????????????????????????????
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.local_icon));// ????????????????????????
        myLocationStyle.strokeColor(Color.BLACK);// ???????????????????????????
        myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 180));// ???????????????????????????
        myLocationStyle.strokeWidth(1.0f);// ???????????????????????????
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setLocationSource( this);// ??????????????????
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// ????????????????????????????????????
        aMap.setMyLocationEnabled(true);// ?????????true??????????????????????????????????????????false??????????????????????????????????????????????????????false
        doSearchQuery();
    }


    /**
     * ????????????poi??????
     */
    protected void doSearchQuery() {
//            aMap.setOnMapClickListener(null);// ??????poi????????????????????????????????????
        query = new PoiSearch.Query("??????", "", city);// ????????????????????????????????????????????????????????????poi????????????????????????????????????poi??????????????????????????????????????????
        query.setPageSize(20);// ?????????????????????????????????poiitem
        query.setPageNum(1);// ??????????????????
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
//            //???????????????????????????
//            Marker marker = aMap.addMarker(markerOption);

            poiSearch.setOnPoiSearchListener( this);
            poiSearch.setBound(new PoiSearch.SearchBound(lp, 10000));
            poiSearch.searchPOIAsyn();// ????????????
        }
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                // ??????????????????
                mListener.onLocationChanged(aMapLocation);
                //???????????????????????????
                latlng = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 14), 1000, null);

                city = aMapLocation.getProvince();
                doSearchQuery();
            } else {
                String errText = "????????????," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
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
//        //???????????????????????????
//        aMap.addMarker(markerOption);
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        latlng = cameraPosition.target;
        aMap.clear();
        MarkerOptions markerOption = new MarkerOptions().position(latlng)
                .draggable(false)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_background));
        //???????????????????????????
        aMap.addMarker(markerOption);
        doSearchQuery();
    }

    public void addMakerOption(List list) {
        for (int i = 0; i < list.size(); i++) {
            MarkerOptions markerOption = new MarkerOptions().position(latlng)
                    .draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_background));
            //???????????????????????????
//            aMap.addMarker(markerOption);
            listMarkers.add(aMap.addMarker(markerOption));
        }
        listMarkers.get(0).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_background));
    }


    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// ??????poi?????????
                if (result.getQuery().equals(query)) {// ??????????????????
                    poiResult = result;
                    poiItems = poiResult.getPois();// ??????????????????poiitem????????????????????????0??????
                    Log.d("conttte", poiItems.toString());
                    List<SuggestionCity> suggestionCities = poiResult
                            .getSearchSuggestionCitys();
                    if (poiItems != null && poiItems.size() > 0) {
                        adapter = new PoiSearch_adapter(this, poiItems);
                        mapList.setAdapter(adapter);
                        mapList.setOnItemClickListener(new mOnItemClickListener());
                        /*????????????????????????PoiOverlay*/
//                        PoiOverlay poiOverlay = new PoiOverlay(aMap, poiItems);
//                        poiOverlay = new MyOverlay(aMap, poiItems);
//                        poiOverlay.removeFromMap();
//                        poiOverlay.addToMap();
//                        poiOverlay.zoomToSpan();
                        addMakerOption(poiItems);
                    }
                } else {
                    Log.d("wjg", "?????????");
                }
            }
        } else if (rCode == 27) {
            Log.d("errnet", "error_network");
        } else if (rCode == 32) {
            Log.d("errkey", "error_key");
        } else {
            Log.d("erroth", "error_other???" + rCode);
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    //-------- ?????? End ------

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
//            //???????????????????????????
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