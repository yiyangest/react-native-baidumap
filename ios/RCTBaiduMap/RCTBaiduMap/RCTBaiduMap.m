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
#import <BaiduMapAPI_Location/BMKLocationService.h>

const CLLocationDegrees RCTBaiduMapDefaultSpan = 0.005;
const NSTimeInterval RCTBaiduMapRegionChangeObserveInterval = 0.1;
const CGFloat RCTBaiduMapZoomBoundBuffer = 0.01;

@interface RCTBaiduMap ()<BMKLocationServiceDelegate>

@end

@implementation RCTBaiduMap
{
    UIView *_legalLabel;
    CLLocationManager *_locationManager;
    BMKLocationService *_locationService;
    BMKLocationViewDisplayParam *_myLocationViewParam;
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
        if (showsUserLocation && !_locationService) {
            _locationService = [BMKLocationService new];
            _locationService.distanceFilter = 5;
            _locationService.delegate = self;
            [_locationService startUserLocationService];
        } else if (showsUserLocation) {
            [_locationService startUserLocationService];
        }else if (!showsUserLocation && _locationService) {
            [_locationService stopUserLocationService];
        }
        super.showsUserLocation = showsUserLocation;
    }
}

- (void)setFollowUserLocation:(BOOL)followUserLocation
{
    if (self.followUserLocation != followUserLocation) {
        if (followUserLocation) {
            self.userTrackingMode = BMKUserTrackingModeFollow;
        } else {
            self.userTrackingMode = BMKUserTrackingModeNone;
        }
        _followUserLocation = followUserLocation;
    }
}

- (void)setUserLocationViewParams:(BMKLocationViewDisplayParam *)userLocationViewParams
{
    if (self.userLocationViewParams != userLocationViewParams) {
        [self updateLocationViewWithParam:userLocationViewParams];
        _userLocationViewParams = userLocationViewParams;
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
    
    if (self.autoZoomToSpan) {
        [self zoomToSpan];
    }
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
    
    if (self.autoZoomToSpan) {
        [self zoomToSpan];
    }
}

- (void)zoomToSpan:(NSArray<RCTBaiduMapAnnotation *> *)annotations andOverlays:(NSArray<RCTBaiduMapOverlay *> *)overlays
{
    CLLocationDegrees minLat = 0.0;
    CLLocationDegrees maxLat = 0.0;
    CLLocationDegrees minLon = 0.0;
    CLLocationDegrees maxLon = 0.0;
    BOOL hasInitialized = NO;
    NSInteger index = 0;
    if (annotations != nil) {
        for (RCTBaiduMapAnnotation *annotation in annotations) {
            if (index == 0 && hasInitialized == NO) {
                minLat = maxLat = annotation.coordinate.latitude;
                minLon = maxLon = annotation.coordinate.longitude;
                hasInitialized = YES;
            } else {
                minLat = MIN(minLat, annotation.coordinate.latitude);
                minLon = MIN(minLon, annotation.coordinate.longitude);
                maxLat = MAX(maxLat, annotation.coordinate.latitude);
                maxLon = MAX(maxLon, annotation.coordinate.longitude);
            }
            index ++;
        }
    }
    index = 0;
    if (overlays != nil) {
        for (RCTBaiduMapOverlay *overlay in overlays) {
            for (NSInteger i = 0; i < overlay.pointCount; i++) {
                BMKMapPoint pt = overlay.points[i];
                CLLocationCoordinate2D coordinate = BMKCoordinateForMapPoint(pt);
                if (index == 0 && i == 0 && hasInitialized == NO) {
                    minLat = maxLat = coordinate.latitude;
                    minLon = maxLon = coordinate.longitude;
                    hasInitialized = YES;
                } else {
                    minLat = MIN(minLat, coordinate.latitude);
                    minLon = MIN(minLon, coordinate.longitude);
                    maxLat = MAX(maxLat, coordinate.latitude);
                    maxLon = MAX(maxLon, coordinate.longitude);
                }
            }
            index ++;
        }
    }
    
    if (hasInitialized) {
        CLLocationCoordinate2D center;
        center.latitude = (maxLat + minLat) * .5f;
        center.longitude = (minLon + maxLon) * .5f;
        BMKCoordinateSpan span = BMKCoordinateSpanMake(maxLat - minLat + 0.02, maxLon - minLon + 0.02);
        
        BMKCoordinateRegion region = BMKCoordinateRegionMake(center, span);
        
        [self setRegion:region animated:YES];
    }
}

- (void)zoomToSpan
{
    [self zoomToSpan:self.annotations andOverlays:self.overlays];
}

- (void)zoomToSpan:(NSArray<CLLocation *> *)locations
{
    if (locations == nil || locations.count == 0) {
        [self zoomToSpan];
    } else if (locations.count == 1) {
        CLLocation *onlyLocation = locations.firstObject;
        [self zoomToCenter:onlyLocation.coordinate];
    } else {
        CLLocationDegrees minLat = 0.0;
        CLLocationDegrees maxLat = 0.0;
        CLLocationDegrees minLon = 0.0;
        CLLocationDegrees maxLon = 0.0;
        NSInteger index = 0;
        for (CLLocation *location in locations) {
            if (index == 0) {
                minLat = maxLat = location.coordinate.latitude;
                minLon = maxLon = location.coordinate.longitude;
            } else {
                minLat = MIN(minLat, location.coordinate.latitude);
                minLon = MIN(minLon, location.coordinate.longitude);
                maxLat = MAX(maxLat, location.coordinate.latitude);
                maxLon = MAX(maxLon, location.coordinate.longitude);
            }
            index ++;
        }
        
        CLLocationCoordinate2D center;
        center.latitude = (maxLat + minLat) * .5f;
        center.longitude = (minLon + maxLon) * .5f;
        BMKCoordinateSpan span = BMKCoordinateSpanMake(maxLat - minLat + 0.02, maxLon - minLon + 0.02);
        
        BMKCoordinateRegion region = BMKCoordinateRegionMake(center, span);
        
        [self setRegion:region animated:YES];

    }
}

- (void)zoomToCenter:(CLLocationCoordinate2D)coordinate
{
    BMKMapStatus *newMapStatus = [BMKMapStatus new];
    newMapStatus.targetGeoPt = coordinate;
    newMapStatus.fLevel = 16;
    
    [self setMapStatus:newMapStatus withAnimation:YES];
}

#pragma mark - BMKLocationServiceDelegate

- (void)didUpdateBMKUserLocation:(BMKUserLocation *)userLocation
{
    if (self.showsUserLocation) {
        [self updateLocationData:userLocation];
    }
}

- (void)didUpdateUserHeading:(BMKUserLocation *)userLocation
{
    if (self.showsUserLocation) {
        [self updateLocationData:userLocation];
    }
}


@end
