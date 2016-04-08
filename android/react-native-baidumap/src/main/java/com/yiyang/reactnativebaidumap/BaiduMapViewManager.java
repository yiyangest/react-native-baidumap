package com.yiyang.reactnativebaidumap;

import android.content.Context;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.util.Log;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yiyang on 16/3/1.
 */
public class BaiduMapViewManager extends SimpleViewManager<MapView> {
    public static final String RCT_CLASS = "RCTBaiduMap";

    public static final int COMMAND_ZOOM_TO_LOCS = 1;

    private ReactMapView mMapView;

    private Context mContext;

    private boolean isMapLoaded;


    @Override
    public String getName() {
        return RCT_CLASS;
    }

    @Override
    protected MapView createViewInstance(ThemedReactContext themedReactContext) {
        SDKInitializer.initialize(themedReactContext.getApplicationContext());
        MapView view = new MapView(themedReactContext);
        mMapView = new ReactMapView(view);
        view.getMap().setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                BaiduMapViewManager.this.isMapLoaded = true;
                mMapView.onMapLoaded();
            }
        });
        this.mContext = themedReactContext;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        return view;
    }

    public ReactMapView getMapView() {
        return mMapView;
    }

    @ReactProp(name="showsUserLocation", defaultBoolean = false)
    public void showsUserLocation(MapView mapView, Boolean show) {
        mMapView.setShowsUserLocation(show);
    }

    @ReactProp(name="showsCompass", defaultBoolean = false)
    public void showsCompass(MapView mapView, Boolean show) {
        mapView.getMap().getUiSettings().setCompassEnabled(show);
    }

    @ReactProp(name="zoomEnabled", defaultBoolean = true)
    public void setZoomEnabled(MapView mapView, Boolean enable) {
        mapView.getMap().getUiSettings().setZoomGesturesEnabled(enable);
    }

    @ReactProp(name="rotateEnabled", defaultBoolean = true)
    public void setRotateEnabled(MapView mapView, Boolean enable) {
//        mapView.getMap().getUiSettings().setRotateGesturesEnabled(enable);
    }

    @ReactProp(name="pitchEnabled", defaultBoolean = false)
    public void setTiltGestureEnabled(MapView mapView, Boolean enable) {
//        mapView.getMap().getUiSettings().setTiltGesturesEnabled(enable);
    }

    @ReactProp(name="scrollEnabled", defaultBoolean = false)
    public void setScrollEnabled(MapView mapView, Boolean enable) {
        mapView.getMap().getUiSettings().setScrollGesturesEnabled(enable);
    }

    @ReactProp(name="autoZoomToSpan", defaultBoolean = false)
    public void setAutoZoomToSpan(MapView mapView, Boolean enable) {
        this.getMapView().setAutoZoomToSpan(enable);
    }

    @ReactProp(name = "mapType", defaultInt = BaiduMap.MAP_TYPE_NORMAL)
    public void setMapType(MapView mapView, int mapType) {
        mapView.getMap().setMapType(mapType);
    }

    @ReactProp(name = "annotations")
    public void setAnnotations(MapView mapView, @Nullable ReadableArray value) throws Exception{
        if (value == null || value.size() == 0) {
            Log.e(RCT_CLASS, "Error: no annotation");
            return;
        }

        List<ReactMapMarker> markers = new ArrayList<ReactMapMarker>();
        int size = value.size();
        for (int i = 0; i < size; i++) {
            ReadableMap annotation = value.getMap(i);
            ReactMapMarker marker = new ReactMapMarker(this.mContext);
            marker.buildMarker(annotation);
            markers.add(marker);
        }

        getMapView().setMarker(markers);

        if (this.isMapLoaded && this.mMapView.isAutoZoomToSpan()) {
            this.mMapView.zoomToSpan();
        }

    }

    @ReactProp(name = "overlays")
    public void setOverlays(MapView mapView, @Nullable ReadableArray value) throws Exception{
        if (value == null || value.size() == 0) {
            return;
        }

        List<ReactMapOverlay> overlays = new ArrayList<ReactMapOverlay>();
        int size = value.size();
        for(int i = 0; i < size; i++) {
            ReadableMap overlay = value.getMap(i);
            ReactMapOverlay polyline = new ReactMapOverlay(overlay);
            if (polyline.getOptions() != null && polyline.getOptions().getPoints() != null && polyline.getOptions().getPoints().size() > 1) {
                overlays.add(polyline);
            }
        }

        getMapView().setOverlays(overlays);


        if (this.isMapLoaded && this.mMapView.isAutoZoomToSpan()) {
            this.mMapView.zoomToSpan();
        }
    }

    @ReactProp(name = "region")
    public void setRegion(MapView mapView, @Nullable ReadableMap center) {
        if (center != null) {
            double latitude = center.getDouble("latitude");
            double longitude = center.getDouble("longitude");
            MapStatus mapStatus = new MapStatus.Builder()
                    .target(new LatLng(latitude, longitude))
                    .build();
            mapView.getMap().animateMapStatus(MapStatusUpdateFactory.newMapStatus(mapStatus));
        }
    }

    @ReactProp(name = "userLocationViewParams")
    public void setUserLocationViewParams(MapView mapView, @Nullable ReadableMap params) {
        ReactMapMyLocationConfiguration configuration = new ReactMapMyLocationConfiguration(this.mContext);
        configuration.buildConfiguration(params);
        this.mMapView.setConfiguration(configuration);
    }

    @Override
    public void receiveCommand(MapView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_ZOOM_TO_LOCS:
                List<LatLng> positions = new ArrayList<LatLng>();
                if (args != null && args.size() > 0) {
                    if (args.getType(0) == ReadableType.Array) {
                        ReadableArray points = args.getArray(0);
                        positions = retreiveLocsFromArray(points);
                    }
                }
                this.zoomToLatLngs(root, positions);
                break;
            default:
                break;
        }
    }

    @javax.annotation.Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of("zoomToLocs", COMMAND_ZOOM_TO_LOCS);
    }

    private void zoomToCenter(MapView mapView, LatLng center) {
        mapView.getMap().animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(center, 18));
    }

    private void zoomToLatLngs(MapView mapView, List<LatLng> array) {
        if (array == null || array.size() == 0) {
            this.getMapView().zoomToSpan();
        } else if (array.size() == 1) {
            this.zoomToCenter(mapView, array.get(0));
        } else {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng item :
                    array) {
                builder.include(item);
            }
            mapView.getMap().animateMapStatus(MapStatusUpdateFactory.newLatLngBounds(builder.build()));
        }
    }

    private static List<LatLng> retreiveLocsFromArray(ReadableArray array) {
        List<LatLng> results = new ArrayList<LatLng>();
        if (array != null && array.size() > 0) {
            int size = array.size();
            for (int i = 0; i < size; i++) {
                if (array.getType(i) != ReadableType.Map && array.getType(i) != ReadableType.Array) {
                    return new ArrayList<LatLng>();
                }
                if (array.getType(i) == ReadableType.Array) {
                    ReadableArray onePointArray = array.getArray(i);
                    if (onePointArray != null && onePointArray.size() == 2) {
                        Double latitude = extractDouble(onePointArray, 0);
                        Double longitude = extractDouble(onePointArray, 1);
                        if (latitude != null && longitude != null) {
                            results.add(new LatLng(latitude, longitude));
                        }
                    }
                } else {
                    ReadableMap onePointMap = array.getMap(i);
                    if (onePointMap != null && onePointMap.hasKey("latitude") && onePointMap.hasKey("longitude")) {
                        Double latitude = extractDouble(onePointMap, "latitude");
                        Double longitude = extractDouble(onePointMap, "longitude");
                        if (latitude != null && longitude != null) {
                            results.add(new LatLng(latitude, longitude));
                        }
                    }
                }
            }
        }

        return results;
    }

    private static Double extractDouble(ReadableArray onePointArray, int arrayIndex) {
        if (arrayIndex >= onePointArray.size() || arrayIndex < 0) {
            return null;
        }
        Double latitude = null;
        if (onePointArray.getType(arrayIndex) == ReadableType.Number) {
            latitude = onePointArray.getDouble(arrayIndex);
        } else if (onePointArray.getType(arrayIndex) == ReadableType.String) {
            latitude = Double.valueOf(onePointArray.getString(arrayIndex));
        }

        return latitude;
    }

    private static Double extractDouble(ReadableMap onePointMap, String mapKey) {
        if (onePointMap == null || !onePointMap.hasKey(mapKey)) {
            return null;
        }
        Double result = null;
        if (onePointMap.getType(mapKey) == ReadableType.Number) {
            result = onePointMap.getDouble(mapKey);
        } else if (onePointMap.getType(mapKey) == ReadableType.String) {
            result = Double.valueOf(onePointMap.getString(mapKey));
        }

        return result;
    }
}
