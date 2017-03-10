#import "CamCtrl.h"
#import "CameraViewController.h"
#import <Cordova/CDVPlugin.h>

@implementation CamCtrl

- (void)echo:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* echo = [command.arguments objectAtIndex:0];

    if(echo != nil && [echo length] > 0){
        pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK messageAsString: echo];
    }else{
        pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId: command.callbackId];
}

-(void)launch:(CDVInvokedUrlCommand*) command
{
    CDVPluginResult* pluginResult = nil;
    NSString* cameraIp = [command.arguments objectAtIndex: 0];
    NSString* cameraPort = [command.arguments objectAtIndex: 1];
    NSString* userName = [command.arguments objectAtIndex: 2];
    NSString* password = [command.arguments objectAtIndex: 3];
    NSString* tunnel = [command.arguments objectAtIndex: 4];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        CameraViewController *cameraController = [[CameraViewController alloc] init];
        [self.viewController presentViewController:cameraController animated:true completion:nil];
    });
    
    NSString* msg = [NSString stringWithFormat:@"%@, %@, %@, %@, %@", cameraIp, cameraPort,userName,password,tunnel];

    pluginResult = [CDVPluginResult resultWithStatus: CDVCommandStatus_OK messageAsString: msg];
    [self.commandDelegate sendPluginResult:pluginResult callbackId: command.callbackId];
}
@end