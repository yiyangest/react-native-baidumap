//
//  RCTConvert+AMapKit.h
//  RCTAMap
//
//  Created by yiyang on 16/2/29.
//  Copyright © 2016年 creditease. All rights reserved.
//

#import <BaiduMapAPI_Map/BMKMapComponent.h>

#import "RCTConvert.h"

@class RCTBaiduMapAnnotation;
@class RCTBaiduMapOverlay;

@interface RCTConvert (BaiduMapKit)

+ (BMKCoordinateSpan)BMKCoordinateSpan:(id)json;
+ (BMKCoordinateRegion)BMKCoordinateRegion:(id)json;
+ (BMKMapType)BMKMapType:(id)json;

+ (RCTBaiduMapAnnotation *)RCTBaiduMapAnnotation:(id)json;
+ (RCTBaiduMapOverlay *)RCTBaiduMapOverlay:(id)json;

+ (NSArray<RCTBaiduMapAnnotation *> *)RCTBaiduMapAnnotationArray:(id)json;
+ (NSArray<RCTBaiduMapOverlay *> *)RCTBaiduMapOverlayArray:(id)json;

+ (BMKLocationViewDisplayParam *)RCTBaiduMapLocationViewDisplayParam:(id)json;

@end
