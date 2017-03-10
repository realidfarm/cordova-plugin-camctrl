#import <Cordova/CDVPlugin.h>

@interface CamCtrl : CDVPlugin

- (void)echo:(CDVInvokedUrlCommand*) command;

- (void)launch:(CDVInvokedUrlCommand*) command;

@end