/********* Satnav.m Cordova Plugin Implementation *******/

#include "Satnav.h"

#import "TQLocationConverter.h"

#import <MapKit/MapKit.h>

@interface Satnav()<UIActionSheetDelegate>
@property (nonatomic, strong) NSDictionary *coordinateInfo;
@end

@implementation Satnav

- (void)showMap:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    
    self.coordinateInfo = [command.arguments objectAtIndex:0];
    
    if ([self.coordinateInfo isEqual:[NSNull null]]) {
        [self failWithCallbackID:command.callbackId withMessage:@"参数格式错误"];
        return ;
    }
    
    // check required parameters
    NSArray *requiredParams;
    if ([self.coordinateInfo objectForKey:@"target"])
    {
        requiredParams = @[@"target", @"lat", @"log"];
    }
    else
    {
        requiredParams = @[@"lat", @"log"];
    }
    
    for (NSString *key in requiredParams)
    {
        if (![self.coordinateInfo objectForKey:key])
        {
            [self failWithCallbackID:command.callbackId withMessage:@"参数格式错误"];
            return ;
        }
    }
    
    [self gotoAction];


    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"导航成功"];
    

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void)gotoAction{
    
    NSArray *appListArr = [self checkHasOwnApp];
    
    NSString *sheetTitle = @"选择导航地图";
    if ([self.coordinateInfo objectForKey:@"target"]) {
        sheetTitle = [NSString stringWithFormat:@"导航到 %@",[self.coordinateInfo objectForKey:@"target"]];
    }
    UIActionSheet *sheet = [[UIActionSheet alloc] initWithTitle:sheetTitle delegate:self cancelButtonTitle:@"取消" destructiveButtonTitle:nil otherButtonTitles:@"苹果地图", nil];
    
    for (NSString *mapType in appListArr) {
        [sheet addButtonWithTitle:mapType];
    }
    sheet.actionSheetStyle = UIActionSheetStyleBlackOpaque;
    [sheet showInView:[self topViewController].view];
}



-(NSArray *)checkHasOwnApp{
    NSArray *mapSchemeArr = @[@"comgooglemaps://",@"iosamap://navi",@"baidumap://map/"];
    
    NSMutableArray *appListArr = [NSMutableArray array];
    
    for (int i = 0; i < [mapSchemeArr count]; i++) {
        if ([[UIApplication sharedApplication] canOpenURL:[NSURL URLWithString:[NSString stringWithFormat:@"%@",[mapSchemeArr objectAtIndex:i]]]]) {
            if (i == 0) {
                [appListArr addObject:@"google地图"];
            }else if (i == 1){
                [appListArr addObject:@"高德地图"];
            }else if (i == 2){
                [appListArr addObject:@"百度地图"];
            }
        }
    }
    return appListArr;
}



- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex {
    
    NSString *btnTitle = [actionSheet buttonTitleAtIndex:buttonIndex];
    NSString *lat = [self.coordinateInfo objectForKey:@"lat"];
    NSString *log = [self.coordinateInfo objectForKey:@"log"];
    CLLocationCoordinate2D coords = CLLocationCoordinate2DMake([lat floatValue],[log floatValue]);//纬度，经度
    if ([btnTitle isEqualToString:@"苹果地图"]) {
        
       
        CLLocationCoordinate2D destination = [TQLocationConverter transformFromBaiduToGCJ:coords];
        MKMapItem *currentLocation = [MKMapItem mapItemForCurrentLocation];
        MKMapItem *toLocation = [[MKMapItem alloc] initWithPlacemark:[[MKPlacemark alloc] initWithCoordinate:destination addressDictionary:nil]];
        if ([self.coordinateInfo objectForKey:@"target"]) {
            toLocation.name = [self.coordinateInfo objectForKey:@"target"];
        }
        
        
        NSArray *items = @[currentLocation,toLocation];
        NSMutableDictionary *options = [NSMutableDictionary dictionary];
        options[MKLaunchOptionsDirectionsModeKey] = MKLaunchOptionsDirectionsModeDriving;
        options[MKLaunchOptionsMapTypeKey] =  [NSNumber numberWithInteger:MKMapTypeStandard];
        options[MKLaunchOptionsShowsTrafficKey] = @YES;
        [MKMapItem openMapsWithItems:items launchOptions:options];
    }
    
    if ([btnTitle isEqualToString:@"google地图"]) {
        CLLocationCoordinate2D destination = [TQLocationConverter transformFromBaiduToGCJ:coords];
        NSString *urlString = [[NSString stringWithFormat:@"comgooglemaps://?x-source=%@&x-success=%@&saddr=&daddr=%f,%f&directionsmode=driving",@"发吧",@"",destination.latitude, destination.longitude] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:urlString]];
    }
    
    if ([btnTitle isEqualToString:@"高德地图"]) {
        CLLocationCoordinate2D destination = [TQLocationConverter transformFromBaiduToGCJ:coords];
        NSString *urlString = [[NSString stringWithFormat:@"iosamap://navi?sourceApplication=%@&backScheme=%@&poiname=%@&lat=%f&lon=%f&dev=0&style=2",@"发吧",@"",[self.coordinateInfo objectForKey:@"target"],destination.latitude, destination.longitude] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:urlString]];
    }
    
    if ([btnTitle isEqualToString:@"百度地图"]){
        //NSString *stringURL = [NSString stringWithFormat:@"baidumap://map/direction?origin=%.8f,%.8f&destination=%.8f,%.8f&&mode=driving",self.position.userLocation.location.coordinate.latitude,self.position.userLocation.location.coordinate.longitude,self.coordinate.latitude,self.coordinate.longitude];
        NSString *stringURL = [[NSString stringWithFormat:@"baidumap://map/direction?origin={{我的位置}}&destination=latlng:%f,%f|name=%@&mode=driving&coord_type=bd09ll",coords.latitude,coords.longitude,[self.coordinateInfo objectForKey:@"target"]] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        NSURL *url = [NSURL URLWithString:stringURL];
        [[UIApplication sharedApplication] openURL:url];
    }
}



//获取当前ViewController
- (UIViewController *)topViewController {
    UIViewController *resultVC;
    resultVC = [self _topViewController:[[UIApplication sharedApplication].keyWindow rootViewController]];
    while (resultVC.presentedViewController) {
        resultVC = [self _topViewController:resultVC.presentedViewController];
    }
    return resultVC;
}

- (UIViewController *)_topViewController:(UIViewController *)vc {
    if ([vc isKindOfClass:[UINavigationController class]]) {
        return [self _topViewController:[(UINavigationController *)vc topViewController]];
    } else if ([vc isKindOfClass:[UITabBarController class]]) {
        return [self _topViewController:[(UITabBarController *)vc selectedViewController]];
    } else {
        return vc;
    }
    return nil;
}



- (void)failWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

@end
