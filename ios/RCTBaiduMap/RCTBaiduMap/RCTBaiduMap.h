//
//  RCTAMap.h
//  RCTAMap
//
//  Created by yiyang on 16/2/26.
//  Copyright © 2016年 creditease. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <BaiduMapAPI_Base/BMKBaseComponent.h>
#import <BaiduMapAPI_Map/BMKMapComponent.h>


#import "RCTConvert+BaiduMapKit.h"
#import "RCTComponent.h"
#import "RCTBaiduMapAnnotation.h"
#import "RCTBaiduMapOverlay.h"

RCT_EXTERN const CLLocationDegrees RCTBaiduMapDefaultSpan;
RCT_EXTERN const NSTimeInterval RCTBaiduMapRegionChangeObserveInterval;
RCT_EXTERN const CGFloat RCTBaiduMapZoomBoundBuffer;

@interface RCTBaiduMap : BMKMapView

@property (nonatomic, assign) BOOL followUserLocation;
@property (nonatomic, assign) BOOL hasStartedRendering;
@property (nonatomic, assign) BOOL autoZoomToSpan;
@property (nonatomic, strong) BMKLocationViewDisplayParam *userLocationViewParams;
@property (nonatomic, assign) CGFloat minDelta;
@property (nonatomic, assign) CGFloat maxDelta;
@property (nonatomic, assign) UIEdgeInsets legalLabelInsets;
@property (nonatomic, strong) NSTimer *regionChangeObserveTimer;
@property (nonatomic, copy) NSArray<NSString *> *annotationIDs;
@property (nonatomic, copy) NSArray<NSString *> *overlayIDs;

@property (nonatomic, copy) RCTBubblingEventBlock onChange;
@property (nonatomic, copy) RCTBubblingEventBlock onPress;
@property (nonatomic, copy) RCTBubblingEventBlock onAnnotationDragStateChange;
@property (nonatomic, copy) RCTBubblingEventBlock onAnnotationFocus;
@property (nonatomic, copy) RCTBubblingEventBlock onAnnotationBlur;

- (void)setAnnotations:(NSArray<RCTBaiduMapAnnotation *> *)annotations;
- (void)setOverlays:(NSArray<RCTBaiduMapOverlay *> *)overlays;

- (void)zoomToSpan;
- (void)zoomToSpan:(NSArray<RCTBaiduMapAnnotation *> *)annotations andOverlays:(NSArray<RCTBaiduMapOverlay *> *)overlays;
- (void)zoomToSpan:(NSArray<CLLocation *> *)locations;

@end
