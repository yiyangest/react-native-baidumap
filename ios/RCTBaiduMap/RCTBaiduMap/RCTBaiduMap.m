//
//  RCTAMap.m
//  RCTAMap
//
//  Created by yiyang on 16/2/26.
//  Copyright © 2016年 creditease. All rights reserved.
//

#import "RCTBaiduMap.h"

#import "RCTEventDispatcher.h"
#import "RCTLog.h"
#import "RCTBaiduMapAnnotation.h"
#import "RCTBaiduMapOverlay.h"
#import "RCTUtils.h"

#import <BaiduMapAPI_Utils/BMKGeometry.h>

const CLLocationDegrees RCTBaiduMapDefaultSpan = 0.005;
const NSTimeInterval RCTBaiduMapRegionChangeObserveInterval = 0.1;
const CGFloat RCTBaiduMapZoomBoundBuffer = 0.01;

@implementation RCTBaiduMap
{
    UIView *_legalLabel;
    CLLocationManager *_locationManager;
    NSMutableArray<UIView *> *_reactSubviews;
}

- (instancetype)init
{
    if ((self = [super init])) {
        _hasStartedRendering = NO;
        _reactSubviews = [NSMutableArray new];
        
        for (UIView *subview in self.subviews) {
            if ([NSStringFromClass(subview.class) isEqualToString:@"MKAttributionLabel"]) {
                _legalLabel = subview;
                break;
            }
        }
    }
    return self;
}

- (void)dealloc
{
    [_regionChangeObserveTimer invalidate];
}

- (void)insertReactSubview:(UIView *)subview atIndex:(NSInteger)atIndex
{
    [_reactSubviews insertObject:subview atIndex:atIndex];
}

- (void)removeReactSubviews: (UIView *)subview
{
    [_reactSubviews removeObject:subview];
}

- (NSArray<UIView *> *)reactSubviews
{
    return _reactSubviews;
}

- (void)layoutSubviews
{
    [super layoutSubviews];
    
    if (_legalLabel) {
        dispatch_async(dispatch_get_main_queue(), ^{
            CGRect frame = _legalLabel.frame;
            if (_legalLabelInsets.left) {
                frame.origin.x = _legalLabelInsets.left;
            } else if (_legalLabelInsets.right) {
                frame.origin.x = self.frame.size.width - _legalLabelInsets.right - frame.size.width;
            }
            if (_legalLabelInsets.top) {
                frame.origin.y = _legalLabelInsets.top;
            } else if (_legalLabelInsets.bottom) {
                frame.origin.y = self.frame.size.height - _legalLabelInsets.bottom - frame.size.height;
            }
            _legalLabel.frame = frame;
        });
    }
}

#pragma mark - Accessors

- (void)setShowsUserLocation:(BOOL)showsUserLocation
{
    if (self.showsUserLocation != showsUserLocation) {
        if (showsUserLocation && !_locationManager) {
            _locationManager = [CLLocationManager new];
            if ([_locationManager respondsToSelector:@selector(requestWhenInUseAuthorization)]) {
                [_locationManager requestWhenInUseAuthorization];
            }
        }
        super.showsUserLocation = showsUserLocation;
    }
}

- (void)setRegion:(BMKCoordinateRegion)region animated:(BOOL)animated
{
    if (!CLLocationCoordinate2DIsValid(region.center)) {
        return;
    }
    
    if (!region.span.latitudeDelta) {
        region.span.latitudeDelta = self.region.span.latitudeDelta;
    }
    if (!region.span.longitudeDelta) {
        region.span.longitudeDelta = self.region.span.longitudeDelta;
    }
    
    [super setRegion:region animated:animated];
}

- (void)setAnnotations:(NSArray<RCTBaiduMapAnnotation *> *)annotations
{
    NSMutableArray<NSString *> *newAnnotationIDs = [NSMutableArray new];
    NSMutableArray<RCTBaiduMapAnnotation *> *annotationsToDelete = [NSMutableArray new];
    NSMutableArray<RCTBaiduMapAnnotation *> *annotationsToAdd = [NSMutableArray new];
    
    for (RCTBaiduMapAnnotation *annotation in annotations) {
        if (![annotation isKindOfClass:[RCTBaiduMapAnnotation class]]) {
            continue;
        }
        
        [newAnnotationIDs addObject:annotation.identifier];
        
        if (![_annotationIDs containsObject:annotation.identifier]) {
            [annotationsToAdd addObject:annotation];
        }
    }
    for (RCTBaiduMapAnnotation *annotation in self.annotations) {
        if (![annotation isKindOfClass:[RCTBaiduMapAnnotation class]]) {
            continue;
        }
        
        if (![newAnnotationIDs containsObject:annotation.identifier]) {
            [annotationsToDelete addObject:annotation];
        }
    }
    
    if (annotationsToDelete.count > 0) {
        [self removeAnnotations:(NSArray<id<BMKAnnotation>> *)annotationsToDelete];
    }
    
    if (annotationsToAdd.count > 0) {
        [self addAnnotations:(NSArray<id<BMKAnnotation>> *)annotationsToAdd];
    }
    
    self.annotationIDs = newAnnotationIDs;
    [self showAnnotations: self.annotations animated:YES];
    
}

- (void)setOverlays:(NSArray<RCTBaiduMapOverlay *> *)overlays
{
    NSMutableArray<NSString *> *newOverlayIDs = [NSMutableArray new];
    NSMutableArray<RCTBaiduMapOverlay *> *overlaysToDelete = [NSMutableArray new];
    NSMutableArray<RCTBaiduMapOverlay *> *overlaysToAdd = [NSMutableArray new];
    
    for (RCTBaiduMapOverlay *overlay in overlays) {
        if (![overlay isKindOfClass:[RCTBaiduMapOverlay class]]) {
            continue;
        }
        
        [newOverlayIDs addObject:overlay.identifier];
        
        if (![_overlayIDs containsObject:overlay.identifier]) {
            [overlaysToAdd addObject:overlay];
        }
    }
    
    for (RCTBaiduMapOverlay *overlay in self.overlays) {
        if (![overlay isKindOfClass:[RCTBaiduMapOverlay class]]) {
            continue;
        }
        
        if (![newOverlayIDs containsObject:overlay.identifier]) {
            [overlaysToDelete addObject:overlay];
        }
    }
    
    if (overlaysToDelete.count > 0) {
        [self removeOverlays:(NSArray<id<BMKOverlay>> *)overlaysToDelete];
    }
    if (overlaysToAdd.count > 0) {
        [self addOverlays:(NSArray<id<BMKOverlay>> *)overlaysToAdd];
    }
    
    self.overlayIDs = newOverlayIDs;
    BMKCoordinateRegion region;
    for (RCTBaiduMapOverlay *overlay in self.overlays) {
        NSUInteger count = overlay.pointCount;
        NSUInteger middle = count / 2;
        if (middle < count) {
            
            BMKMapPoint pt = overlay.points[middle];
            region = BMKCoordinateRegionMakeWithDistance(BMKCoordinateForMapPoint(pt), 20000, 20000);
        }
        
    }
    
    //    [self setVisibleMapRect:re animated:YES];
    [self setRegion:region animated:YES];
}


@end
