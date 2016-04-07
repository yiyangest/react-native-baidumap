package com.yiyang.reactnativebaidumap;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;

/**
 * Created by yiyang on 16/4/5.
 */
public class BaiduLocationListener implements BDLocationListener {

    private ReactLocationCallback mCallback;

    public BaiduLocationListener(LocationClient client, ReactLocationCallback callback) {
        this.mCallback = callback;
        if (client != null) {
            client.registerLocationListener(this);
        }
    }

    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        if (bdLocation == null) {
            Log.e("RNBaidumap", "receivedLocation is null!");
            return ;
        }
        Log.i("RNBaidumap", "received location: " + bdLocation.getLocType() + ", " + bdLocation.getLatitude() + ", " + bdLocation.getLongitude());
        int locateType = bdLocation.getLocType();
        if (locateType == BDLocation.TypeGpsLocation
                || locateType == BDLocation.TypeNetWorkLocation
                || locateType == BDLocation.TypeOffLineLocation) {
            if (this.mCallback != null) {
                this.mCallback.onSuccess(bdLocation);
            }
        } else {
            if (this.mCallback != null) {
                this.mCallback.onFailure(bdLocation);
            }
        }
    }

    public interface ReactLocationCallback {
        void onSuccess(BDLocation bdLocation);
        void onFailure(BDLocation bdLocation);
    }
}
