package com.yiyang.reactnativebaidumap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.Uri;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeHolder;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;

/**
 * Created by yiyang on 16/4/7.
 */
public class ReactMapMyLocationConfiguration {
    private Context mContext;
    private MyLocationConfiguration mConfiguration;
    private boolean showAccuracyCircle;
    private ConfigurationUpdateListener mConfigurationUpdateListener;

    public static BitmapDescriptor defaultIcon = BitmapDescriptorFactory.fromResource(R.drawable.location);


    private BitmapDescriptor iconBitmapDescriptor;
    private final DraweeHolder mLogoHolder;
    private DataSource<CloseableReference<CloseableImage>> dataSource;

    private final ControllerListener<ImageInfo> mLogoControllerListener =
            new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    CloseableReference<CloseableImage> imageReference = null;
                    try {
                        imageReference = dataSource.getResult();
                        if (imageReference != null) {
                            CloseableImage image = imageReference.get();
                            if (image != null && image instanceof CloseableStaticBitmap) {
                                CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap)image;
                                Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
                                if (bitmap != null) {
                                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                                    iconBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
                                }
                            }
                        }
                    } finally {
                        dataSource.close();
                        if (imageReference != null) {
                            CloseableReference.closeSafely(imageReference);
                        }
                    }
                    update();
                }
            };

    public ReactMapMyLocationConfiguration(Context context) {
        this.mContext = context;
        mLogoHolder = DraweeHolder.create(createDraweeHierarchy(), null);
        mLogoHolder.onAttach();
    }

    public void buildConfiguration(ReadableMap map) {
        if (map == null) {
            mConfiguration = null;
        } else {
            int accuracyCircleFillColor = 4521984;
            int accuracyCircleStrokeColor = 4653056;
            if (map.hasKey("image")) {
                if (map.getType("image") == ReadableType.Map) {
                    String imgUri = map.getMap("image").getString("uri");
                    if (imgUri != null && imgUri.length() > 0) {
                        if (imgUri.startsWith("http://") || imgUri.startsWith("https://")) {
                            ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imgUri)).build();
                            ImagePipeline imagePipeline = Fresco.getImagePipeline();
                            dataSource = imagePipeline.fetchDecodedImage(imageRequest,this);
                            DraweeController controller = Fresco.newDraweeControllerBuilder()
                                    .setImageRequest(imageRequest)
                                    .setControllerListener(mLogoControllerListener)
                                    .setOldController(mLogoHolder.getController())
                                    .build();
                            mLogoHolder.setController(controller);
                        } else {
                            iconBitmapDescriptor = getBitmapDescriptorByName(imgUri);
                        }
                    }
                } else if (map.getType("image") == ReadableType.String) {
                    iconBitmapDescriptor = getBitmapDescriptorByName(map.getString("image"));
                }
            }

            if (map.hasKey("accuracyCircleFillColor") && map.getType("accuracyCircleFillColor") == ReadableType.Number) {
                accuracyCircleFillColor = map.getInt("accuracyCircleFillColor");
            }

            if (map.hasKey("accuracyCircleStrokeColor") && map.getType("accuracyCircleStrokeColor") == ReadableType.Number) {
                accuracyCircleStrokeColor = map.getInt("accuracyCircleStrokeColor");
            }

            if (map.hasKey("showAccuracyCircle") && map.getType("showAccuracyCircle") == ReadableType.Boolean) {
                this.showAccuracyCircle = map.getBoolean("showAccuracyCircle");
            }

            mConfiguration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, getIcon(), accuracyCircleFillColor, accuracyCircleStrokeColor);
        }
    }

    private GenericDraweeHierarchy createDraweeHierarchy() {
        return new GenericDraweeHierarchyBuilder(this.mContext.getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .setFadeDuration(0)
                .build();
    }

    private int getDrawableResourceByName(String name) {
        return this.mContext.getResources().getIdentifier(name, "drawable", this.mContext.getPackageName());
    }

    private BitmapDescriptor getBitmapDescriptorByName(String name) {
        return BitmapDescriptorFactory.fromResource(getDrawableResourceByName(name));
    }

    private BitmapDescriptor getIcon() {
        if (iconBitmapDescriptor != null) {
            return iconBitmapDescriptor;
        } else {
            return null;
        }
    }

    public void update() {
        if (this.mConfiguration != null) {
            MyLocationConfiguration newConfiguration = new MyLocationConfiguration(this.mConfiguration.locationMode, this.mConfiguration.enableDirection, getIcon());
            newConfiguration.accuracyCircleFillColor = this.mConfiguration.accuracyCircleFillColor;
            newConfiguration.accuracyCircleStrokeColor = this.mConfiguration.accuracyCircleStrokeColor;

            this.mConfiguration = newConfiguration;
            if (mConfigurationUpdateListener != null) {
                mConfigurationUpdateListener.onConfigurationUpdate(this);
            }
        }
    }

    public void setConfigurationUpdateListener(ConfigurationUpdateListener listener) {
        this.mConfigurationUpdateListener = listener;
    }

    public MyLocationConfiguration getConfiguration() {
        return this.mConfiguration;
    }

    public boolean isShowAccuracyCircle() {
        return showAccuracyCircle;
    }

    public interface ConfigurationUpdateListener {
        void onConfigurationUpdate(ReactMapMyLocationConfiguration configuration);
    }
}
