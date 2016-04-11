package com.yiyang.reactnativebaidumap;


import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.SystemClock;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yiyang on 16/4/11.
 */
public class ReactMapLocationModule extends ReactContextBaseJavaModule {

    private LocationClient mClient;

    @Override
    public String getName() {
        return "KKLocationObserver";
    }

    BaiduLocationListener.ReactLocationCallback mLocationCallback = new BaiduLocationListener.ReactLocationCallback() {
        @Override
        public void onSuccess(BDLocation bdLocation) {
            getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("kkLocationDidChange", locationToMap(bdLocation));
        }

        @Override
        public void onFailure(BDLocation bdLocation) {
            emitError("unable to locate, locType = " + bdLocation.getLocType());
        }
    };

    public ReactMapLocationModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @ReactMethod
    public void getCurrentPosition(ReadableMap options, final Callback success, Callback error) {
        LocationClientOption option = defaultOption();
        LocationClient client = new LocationClient(getReactApplicationContext().getApplicationContext(), option);
        BDLocation lastLocation = client.getLastKnownLocation();
        if (lastLocation != null) {
            Double locationTime = convertLocationTime(lastLocation);
            if (locationTime != null && (SystemClock.currentTimeMillis() - locationTime < 1000)) {

                success.invoke(locationToMap(lastLocation));
                return;
            }
        }
        new SingleUpdateRequest(client, success, error).invoke();
    }

    @ReactMethod
    public void startObserving(ReadableMap options) {
        LocationClientOption option = defaultOption();

        if (mClient == null) {
            mClient = new LocationClient(getReactApplicationContext().getApplicationContext(), option);
            new BaiduLocationListener(mClient, mLocationCallback);
        } else {
            mClient.setLocOption(option);
        }

        if (!mClient.isStarted()) {
            mClient.start();
        }
    }

    @ReactMethod
    public void stopObserving() {
        if (mClient != null) {
            mClient.stop();
        }
    }

    public static LocationClientOption defaultOption() {
        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setOpenGps(true);
        option.setScanSpan(30 * 1000);
        return option;
    }

    private static WritableMap locationToMap(BDLocation location) {
        if (location == null) {
            return null;
        }
        WritableMap map = Arguments.createMap();
        WritableMap coords = Arguments.createMap();
        coords.putDouble("latitude", location.getLatitude());
        coords.putDouble("longitude", location.getLongitude());
        coords.putDouble("accuracy", location.getRadius());
        coords.putDouble("heading", location.getDirection());
        map.putMap("coords", coords);
        map.putDouble("timestamp", convertLocationTime(location));

        return map;
    }

    private void emitError(String error) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("kkLocationError", error);
    }

    private static Double convertLocationTime(BDLocation location) {
        if (location == null || location.getTime() == null || location.getTime().length() == 0) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Double timestamp = null;
        try {
            Date date = format.parse(location.getTime());
            timestamp = (double)date.getTime();

        } catch(Exception e) {

        }
        return timestamp;
    }

    private static class SingleUpdateRequest {
        private final Callback mSuccess;
        private final Callback mError;
        private final LocationClient mClient;

        private final BaiduLocationListener mListenr;

        private final BaiduLocationListener.ReactLocationCallback mCallback = new BaiduLocationListener.ReactLocationCallback() {
            @Override
            public void onSuccess(BDLocation bdLocation) {
                mSuccess.invoke(locationToMap(bdLocation));
                mClient.unRegisterLocationListener(mListenr);
                mClient.stop();
            }

            @Override
            public void onFailure(BDLocation bdLocation) {
                mError.invoke("定位失败: " + bdLocation.getLocType());
                mClient.unRegisterLocationListener(mListenr);
                mClient.stop();
            }
        };

        private SingleUpdateRequest(LocationClient client, Callback success, Callback error) {
            this.mClient = client;
            mSuccess = success;
            mError = error;
            mListenr = new BaiduLocationListener(client, mCallback);
        }

        public void invoke() {
            if (mClient == null) {
                return ;
            }
            if (mClient.isStarted()) {
                mClient.requestLocation();
            } else {
                mClient.start();
            }
        }
    }
}
