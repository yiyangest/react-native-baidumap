//
//  RCTAMapOverlay.m
//  RCTAMap
//
//  Created by yiyang on 16/2/29.
//  Copyright © 2016年 creditease. All rights reserved.
//

#import "RCTBaiduMapOverlay.h"

@implementation RCTBaiduMapOverlay

+ (instancetype)polylineWithCoordinates:(CLLocationCoordinate2D *)coordinates count:(NSInteger)count
{
    RCTBaiduMapOverlay * overlay = [[RCTBaiduMapOverlay alloc] init];
    [overlay setPolylineWithCoordinates:coordinates count:count];
    return overlay;
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect {
    // Drawing code
}
*/

@end
