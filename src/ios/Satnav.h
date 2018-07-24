//
//  Satnav.h
//  BSAPP
//
//  Created by I-smartnet on 2017/3/27.
//  Copyright © 2017年 chiefchain. All rights reserved.
//

#import <Cordova/CDV.h>

@interface Satnav : CDVPlugin {
    // Member variables go here.
}

- (void)showMap:(CDVInvokedUrlCommand*)command;
@end
