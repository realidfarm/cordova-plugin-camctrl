/*******************************************************************************
 CopyRight:			Copyright 2009 hikvision. All rights reserved.
 FileName:			DeviceInfo.m
 Description:		device info
 Author:
 Data:				2009/08/25
 Modification History:
 *******************************************************************************/

#import "DeviceInfo.h"

@implementation DeviceInfo
@synthesize nDeviceID;
@synthesize chDeviceName;
@synthesize chDeviceAddr;
@synthesize nDevicePort;
@synthesize chLoginName;
@synthesize chPassWord;
@synthesize nChannelNum;


- (id)init
{
    return self;
}
@end
