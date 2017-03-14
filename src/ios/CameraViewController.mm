//
//  CameraViewController.m
//
//  Created by sunht on 2017/3/9.
//
//

#import "CameraViewController.h"
#import "hcnetsdk.h"
#import "DeviceInfo.h"
#import "HikDec.h"
#import "IOSPlayM4.h"
#import "Preview.h"


@implementation CameraViewController

@synthesize m_playView;

@synthesize m_nPreviewPort;
@synthesize m_nPlaybackPort;
@synthesize m_fp;
@synthesize m_playThreadID;
@synthesize m_bThreadRun;
@synthesize m_lUserID;
@synthesize m_lRealPlayID;
@synthesize m_lPlaybackID;
@synthesize m_bPreview;
@synthesize m_bRecord;
@synthesize m_bPTZL;
@synthesize m_bVoiceTalk;


@synthesize m_ip;
@synthesize m_port;
@synthesize m_user;
@synthesize m_pwd;
@synthesize m_channel;
@synthesize m_disableCtrl;

CameraViewController *g_pController = NULL;

int g_iStartChan = 0;
int g_iPreviewChanNum = 0;
bool g_bDecode = true;
bool g_isFullScreen = false;
float g_screenW = 0;
float g_screenH = 0;


- (void)viewDidLoad {
    [super viewDidLoad];
    
    // m_ip = @"58.214.29.198"; //'58.214.29.198','8000','admin','gy123456','1'
    // m_port = @"8000";
    // m_user = @"admin";
    // m_pwd = @"gy123456";
    // m_channel = 1;
    
    
    m_lUserID = -1;
    m_lRealPlayID = -1;
    m_lPlaybackID = -1;
    m_nPreviewPort = -1;
    m_nPlaybackPort = -1;
    m_bRecord = false;
    m_bPTZL = false;
    
    
    
    [self.view setBackgroundColor: [UIColor whiteColor]];
    
    [self initView];
    
    g_pController = self;
}

- (void) viewDidAppear:(BOOL)animated{
    
    NSLog(@"view did appear...");
    if([self login]){
        NSLog(@"login success..");
        [self startPreview];
        
    }else{
        NSLog(@"login failed..");
    }
}

- (void)viewDidUnload
{
    
    if (m_lRealPlayID != -1)
    {
        NET_DVR_StopRealPlay(m_lRealPlayID);
        m_lRealPlayID = -1;
    }
    
    if(m_lPlaybackID != -1)
    {
        NET_DVR_StopPlayBack(m_lPlaybackID);
        m_lPlaybackID = -1;
    }
    
    if(m_lUserID != -1)
    {
        NET_DVR_Logout(m_lUserID);
        NET_DVR_Cleanup();
        m_lUserID = -1;
    }
}


-(void) initView{
    
    CGRect statuRect = [[UIApplication sharedApplication] statusBarFrame];
    float statuOffset = statuRect.size.height;
    
    
    CGRect rect = [[UIScreen mainScreen] bounds];
    g_screenW = rect.size.width;
    g_screenH = rect.size.height;
    
    //nav bar
    UIView *navView = [[UIView alloc]initWithFrame:CGRectMake(0, statuOffset, g_screenW, 44)];
    [navView setBackgroundColor:[UIColor colorWithRed:45/255.0 green:204/255.0 blue:134/255.0 alpha:1]];
    [self.view addSubview:navView];
    
    UILabel *lblTitle = [[UILabel alloc] initWithFrame:CGRectMake((g_screenW-100)/2, 6, 100, 32)];
    [lblTitle setText:@"视频查看"];
    [lblTitle setTextColor:[UIColor whiteColor]];
    [lblTitle setTextAlignment:NSTextAlignmentCenter];
    [lblTitle setFont:[UIFont systemFontOfSize:16]];
    [navView addSubview:lblTitle];
    
    UIButton *btnBack = [UIButton buttonWithType:UIButtonTypeCustom];
    btnBack.frame = CGRectMake(10, 6, 32, 32);
    [btnBack.imageView setContentMode:UIViewContentModeScaleAspectFit];
    [btnBack addTarget:self action:@selector(goback) forControlEvents:UIControlEventTouchUpInside];
    [btnBack setImage:[UIImage imageNamed:@"ic_back"] forState:UIControlStateNormal];
    [navView addSubview:btnBack];
    
    UIButton *btnFullscreen = [UIButton buttonWithType:UIButtonTypeCustom];
    btnFullscreen.frame = CGRectMake(g_screenW-42, 6, 32, 32);
    [btnFullscreen.imageView setContentMode:UIViewContentModeScaleAspectFit];
    [btnFullscreen setImage:[UIImage imageNamed:@"ic_fullscreen"] forState:UIControlStateNormal];
    [btnFullscreen addTarget:self action:@selector(enterFullscreen) forControlEvents:UIControlEventTouchUpInside];
    [navView addSubview:btnFullscreen];
    
    
    //ctrl view
    UIView *m_ctrlView = [[UIView alloc] initWithFrame:CGRectMake(0, 220 + 44 + statuOffset, g_screenW, 210)];
    [self.view addSubview:m_ctrlView];
    
    float dside = 150;
    
    float loffset = (g_screenW - 250)/2.0;
    
    
    UIView *directionView = [[UIView alloc] initWithFrame:CGRectMake(loffset, 10, dside, dside)];
    [m_ctrlView addSubview:directionView];
    
    UIImageView *ctrlBG = [[UIImageView alloc] initWithFrame:CGRectMake(0, 0, dside, dside)];
    [ctrlBG setImage:[UIImage imageNamed:@"ctrl_bg"]];
    [directionView addSubview:ctrlBG];
    
    //btn up
    UIButton *btnUp = [UIButton buttonWithType:UIButtonTypeCustom];
    btnUp.frame = CGRectMake(57, 7, 36, 36);
    [btnUp.imageView setContentMode:UIViewContentModeScaleAspectFit];
    [btnUp addTarget:self action:@selector(startTiltUp) forControlEvents:UIControlEventTouchDown];
    [btnUp addTarget:self action:@selector(stopTiltUp) forControlEvents:UIControlEventTouchUpInside];
    [btnUp setImage:[UIImage imageNamed:@"ic_up"] forState:UIControlStateNormal];
    [btnUp setImage:[UIImage imageNamed:@"ic_up_on"] forState:UIControlStateHighlighted];
    [directionView addSubview:btnUp];
    
    //btn left
    UIButton *btnLeft = [UIButton buttonWithType:UIButtonTypeCustom];
    btnLeft.frame = CGRectMake(7, 57, 36, 36);
    [btnLeft.imageView setContentMode:UIViewContentModeScaleAspectFit];
    [btnLeft addTarget:self action:@selector(startPanLeft) forControlEvents:UIControlEventTouchDown];
    [btnLeft addTarget:self action:@selector(stopPanRight) forControlEvents:UIControlEventTouchUpInside];
    [btnLeft setImage:[UIImage imageNamed:@"ic_left"] forState:UIControlStateNormal];
    [btnLeft setImage:[UIImage imageNamed:@"ic_left_on"] forState:UIControlStateHighlighted];
    [directionView addSubview:btnLeft];
    
    //btn right
    UIButton *btnRight = [UIButton buttonWithType:UIButtonTypeCustom];
    btnRight.frame = CGRectMake(107, 57, 36, 36);
    [btnRight.imageView setContentMode:UIViewContentModeScaleAspectFit];
    [btnRight addTarget:self action:@selector(startPanRight) forControlEvents:UIControlEventTouchDown];
    [btnRight addTarget:self action:@selector(stopPanRight) forControlEvents:UIControlEventTouchUpInside];
    [btnRight setImage:[UIImage imageNamed:@"ic_right"] forState:UIControlStateNormal];
    [btnRight setImage:[UIImage imageNamed:@"ic_right_on"] forState:UIControlStateHighlighted];
    [directionView addSubview:btnRight];
    
    //btn down
    UIButton *btnDown = [UIButton buttonWithType:UIButtonTypeCustom];
    btnDown.frame = CGRectMake(57, 107, 36, 36);
    [btnDown.imageView setContentMode:UIViewContentModeScaleAspectFit];
    [btnDown addTarget:self action:@selector(startTiltDown) forControlEvents:UIControlEventTouchDown];
    [btnDown addTarget:self action:@selector(stopTiltDown) forControlEvents:UIControlEventTouchUpInside];
    [btnDown setImage:[UIImage imageNamed:@"ic_down"] forState:UIControlStateNormal];
    [btnDown setImage:[UIImage imageNamed:@"ic_down_on"] forState:UIControlStateHighlighted];
    [directionView addSubview:btnDown];
    
    //btn zoom in
    UIButton *btnZoomIn = [UIButton buttonWithType:UIButtonTypeCustom];
    btnZoomIn.frame = CGRectMake(loffset + 198, 10, 48, 48);
    [btnZoomIn addTarget:self action:@selector(startZoomIn) forControlEvents:UIControlEventTouchDown];
    [btnZoomIn addTarget:self action:@selector(stopZoomIn) forControlEvents:UIControlEventTouchUpInside];
    [btnZoomIn setBackgroundImage:[UIImage imageNamed:@"ic_increase"] forState:UIControlStateNormal];
    [btnZoomIn setBackgroundImage:[UIImage imageNamed:@"ic_increase_on"] forState:UIControlStateHighlighted];
    [m_ctrlView addSubview:btnZoomIn];
    
    //btn zoom out
    UIButton *btnZoomOut = [UIButton buttonWithType:UIButtonTypeCustom];
    btnZoomOut.frame = CGRectMake(loffset + 198, 112, 48, 48);
    [btnZoomOut addTarget:self action:@selector(startZoomOut) forControlEvents:UIControlEventTouchDown];
    [btnZoomOut addTarget:self action:@selector(stopZoomOut) forControlEvents:UIControlEventTouchUpInside];
    [btnZoomOut setBackgroundImage:[UIImage imageNamed:@"ic_reduce"] forState:UIControlStateNormal];
    [btnZoomOut setBackgroundImage:[UIImage imageNamed:@"ic_reduce_on"] forState:UIControlStateHighlighted];
    [m_ctrlView addSubview:btnZoomOut];
    
    
    //play view
    m_playView = [[UIView alloc]initWithFrame:CGRectMake(0, 44+statuOffset, g_screenW, 210)];
    [m_playView setBackgroundColor:[UIColor blackColor]];
    UITapGestureRecognizer *recognizer = [[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(exitFullscreen)];
    recognizer.numberOfTapsRequired = 2;
    [m_playView addGestureRecognizer:recognizer];
    [self.view addSubview:m_playView];
    
}

-(void) exitFullscreen{
    if(g_isFullScreen){
        if ([[UIDevice currentDevice] respondsToSelector:@selector(setOrientation:)]) {
            SEL selector = NSSelectorFromString(@"setOrientation:");
            NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[UIDevice instanceMethodSignatureForSelector:selector]];
            [invocation setSelector:selector];
            [invocation setTarget:[UIDevice currentDevice]];
            int val = UIInterfaceOrientationPortrait;
            [invocation setArgument:&val atIndex:2];
            [invocation invoke];
            
            m_playView.frame = CGRectMake(0, 64, g_screenW, 210);
            g_isFullScreen = false;
        }
        
        UIView *lblHint = [self.view viewWithTag:171];
        if (lblHint != nil){
            [lblHint removeFromSuperview];
        }
    }
}

-(void) enterFullscreen{
    if ([[UIDevice currentDevice] respondsToSelector:@selector(setOrientation:)]) {
        SEL selector = NSSelectorFromString(@"setOrientation:");
        NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[UIDevice instanceMethodSignatureForSelector:selector]];
        [invocation setSelector:selector];
        [invocation setTarget:[UIDevice currentDevice]];
        int val = UIInterfaceOrientationLandscapeRight;
        [invocation setArgument:&val atIndex:2];
        [invocation invoke];
        
        m_playView.frame = CGRectMake(0, 0, g_screenH, g_screenW);
        g_isFullScreen = true;
        
        UILabel *lblHint = [[UILabel alloc]initWithFrame:CGRectMake((g_screenH-180)/2, (g_screenW-40)/2, 180, 40)];
        lblHint.tag = 171;
        [lblHint setText:@"双击屏幕退出全屏"];
        [lblHint setTextAlignment:NSTextAlignmentCenter];
        [lblHint setBackgroundColor:[UIColor colorWithRed:100/255.0 green:100/255.0 blue:100/255.0 alpha:1]];
        [lblHint setTextColor:[UIColor whiteColor]];
        [lblHint setFont:[UIFont systemFontOfSize:18]];
        [self.view addSubview:lblHint];
        
        [UIView animateWithDuration:2.0 delay:0 options:UIViewAnimationOptionCurveLinear animations:^{
            [lblHint setAlpha:0];
        } completion:nil];
        
    }
}

- (CGAffineTransform) getTransform:(UIInterfaceOrientation)orientation{
    if (orientation == UIInterfaceOrientationLandscapeLeft) {
        return CGAffineTransformMakeRotation(M_PI*1.5);
    } else if (orientation == UIInterfaceOrientationLandscapeRight) {
        return CGAffineTransformMakeRotation(M_PI/2);
    } else if (orientation == UIInterfaceOrientationPortraitUpsideDown) {
        return CGAffineTransformMakeRotation(-M_PI);
    } else {
        return CGAffineTransformIdentity;
    }
}

- (void) startTiltDown{
    NSLog(@"start TILT_DOWN");
    [self doPTZCtrl:TILT_DOWN withFlag:0];
}

- (void) stopTiltDown{
    NSLog(@"stop TILT_DOWN");
    [self doPTZCtrl:TILT_DOWN withFlag:1];
}

- (void) startTiltUp{
    NSLog(@"start TILT_UP");
    [self doPTZCtrl:TILT_UP withFlag:0];
}

- (void) stopTiltUp{
    NSLog(@"stop TILT_UP");
    [self doPTZCtrl:TILT_UP withFlag:1];
}


- (void) startPanRight{
    NSLog(@"start PAN_RIGHT");
    [self doPTZCtrl:PAN_RIGHT withFlag:0];
}

- (void) stopPanRight{
    NSLog(@"stop PAN_RIGHT");
    [self doPTZCtrl:PAN_RIGHT withFlag:1];
}

- (void) startPanLeft{
    NSLog(@"start PAN_LEFT");
    [self doPTZCtrl:PAN_LEFT withFlag:0];
}

- (void) stopPanLeft{
    NSLog(@"stop PAN_LEFT");
    [self doPTZCtrl:PAN_LEFT withFlag:1];
}

- (void) startZoomIn{
    NSLog(@"start ZOOM_IN");
    [self doPTZCtrl:ZOOM_IN withFlag:0];
}

- (void) stopZoomIn{
    NSLog(@"stop ZOOM_IN");
    [self doPTZCtrl:ZOOM_IN withFlag:1];
}

- (void) startZoomOut{
    NSLog(@"start ZOOM_OUT");
    [self doPTZCtrl:ZOOM_OUT withFlag:0];
}

- (void) stopZoomOut{
    NSLog(@"stop ZOOM_OUT");
    [self doPTZCtrl:ZOOM_OUT withFlag:1];
}

- (void) doPTZCtrl:(uint)cmd withFlag:(uint)flag{
    if (m_lUserID < 0) {
        NSLog(@"Please logon a device first!");
        return;
    }

    if(m_disableCtrl){
         UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:@"没有权限"
                              message:@"您没有控制摄像头的权限"
                              delegate:nil
                              cancelButtonTitle:@"确定"
                              otherButtonTitles:nil];
        [alert show];
        return;
    }
    
    if(!NET_DVR_PTZControl_Other(m_lUserID, g_iStartChan + m_channel, cmd, flag))
    {
        NSLog(@"exec ptz ctrl failed with[%d]", NET_DVR_GetLastError());
    }
    else
    {
        NSLog(@"exex ptz ctrl succ");
    }
}


- (BOOL) login{
    NSLog(@"login");
    // init
    BOOL bRet = NET_DVR_Init();
    if (!bRet)
    {
        NSLog(@"NET_DVR_Init failed");
    }
    NET_DVR_SetExceptionCallBack_V30(0, NULL, g_fExceptionCallBack, NULL);
    NSString *documentPath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    const char* pDir = [documentPath UTF8String];
    NET_DVR_SetLogToFile(3, (char*)pDir, true);
    if (m_lUserID == -1)
    {
        //  Get value
        NSString * iP = m_ip;
        NSString * port = m_port;
        NSString * usrName = m_user;
        NSString * password = m_pwd;
        
        //        iP=@"192.168.1.245";
        //        port=@"8000";
        //        usrName=@"admin";
        //        password=@"skyinno1";
        
        DeviceInfo *deviceInfo = [[DeviceInfo alloc] init];
        deviceInfo.chDeviceAddr = iP;
        deviceInfo.nDevicePort = [port intValue];
        deviceInfo.chLoginName = usrName;
        deviceInfo.chPassWord = password;
        
        // check valid
        if (![self validateValue:deviceInfo])
        {
            return false;
        }
        
        // device login
        NET_DVR_DEVICEINFO_V30 logindeviceInfo = {0};
        
        // encode type
        NSStringEncoding enc = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000);
        m_lUserID = NET_DVR_Login_V30((char*)[deviceInfo.chDeviceAddr UTF8String],
                                      deviceInfo.nDevicePort,
                                      (char*)[deviceInfo.chLoginName cStringUsingEncoding:enc],
                                      (char*)[deviceInfo.chPassWord UTF8String],
                                      &logindeviceInfo);
        
        printf("iP:%s\n", (char*)[deviceInfo.chDeviceAddr UTF8String]);
        printf("Port:%d\n", deviceInfo.nDevicePort);
        printf("UsrName:%s\n", (char*)[deviceInfo.chLoginName cStringUsingEncoding:enc]);
        printf("Password:%s\n", (char*)[deviceInfo.chPassWord UTF8String]);
        
        // login on failed
        if (m_lUserID == -1)
        {
            UIAlertView *alert = [[UIAlertView alloc]
                                  initWithTitle:kWarningTitle
                                  message:kLoginDeviceFailMsg
                                  delegate:nil
                                  cancelButtonTitle:kWarningConfirmButton
                                  otherButtonTitles:nil];
            [alert show];
            return false;
        }
        
        if(logindeviceInfo.byChanNum > 0)
        {
            g_iStartChan = logindeviceInfo.byStartChan;
            g_iPreviewChanNum = logindeviceInfo.byChanNum;
            
            NSLog(@"g_iStartChan:%zi",g_iStartChan);
            
        }
        else if(logindeviceInfo.byIPChanNum > 0)
        {
            g_iStartChan = logindeviceInfo.byStartDChan;
            g_iPreviewChanNum = logindeviceInfo.byIPChanNum + logindeviceInfo.byHighDChanNum * 256;
        }
        
        
        return true;
        
    }
    
    return false;

}

-(void) logout{
    if(m_lUserID != -1){
        NET_DVR_Logout(m_lUserID);
        NET_DVR_Cleanup();
        m_lUserID = -1;
    }
}

- (void) startPreview{
    m_lRealPlayID = startPreview(m_lUserID, g_iStartChan + m_channel, m_playView, 0);
    m_bPreview = true;
}

-(void) stopPreview{
    if(m_bPreview){
        stopPreview(0);
        m_bPreview = false;
    }
}

- (void) previewPlay:(int*)iPlayPort playView:(UIView*)playView
{
    m_nPreviewPort = *iPlayPort;
    int iRet = PlayM4_Play(*iPlayPort, (__bridge void*) playView);
    PlayM4_PlaySound(*iPlayPort);
    if (iRet != 1)
    {
        NSLog(@"PlayM4_Play fail");
        [self stopPreviewPlay: iPlayPort];
        return;
    }
}
- (void)stopPreviewPlay:(int*)iPlayPort
{
    PlayM4_StopSound();
    if (!PlayM4_Stop(*iPlayPort))
    {
        NSLog(@"PlayM4_Stop failed");
    }
    if(!PlayM4_CloseStream(*iPlayPort))
    {
        NSLog(@"PlayM4_CloseStream failed");
    }
    if (!PlayM4_FreePort(*iPlayPort))
    {
        NSLog(@"PlayM4_FreePort failed");
    }
    *iPlayPort = -1;
}


- (void) goback{
    
    [self stopPreview];
    [self logout];
    
    [self dismissViewControllerAnimated:true completion:nil];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}


/*******************************************************************************
 Function:			validateValue
 Description:		check valid
 Input:				deviceInfo － device info
 Output:
 Return:			true-valid;false-invalid
 *******************************************************************************/
- (bool) validateValue:(DeviceInfo *)deviceInfo
{
    // check device address
    if ([deviceInfo.chDeviceAddr compare:@""] == NSOrderedSame)
    {
        UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:kWarningTitle
                              message:kDeviceAddrEmptyMsg
                              delegate:nil
                              cancelButtonTitle:kWarningConfirmButton
                              otherButtonTitles:nil];
        [alert show];
        
        return false;
    }
    
    // check length of device address
    if ([deviceInfo.chDeviceAddr lengthOfBytesUsingEncoding:NSUTF8StringEncoding] > 32)
    {
        UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:kWarningTitle
                              message:kDeviceAddrTooLongerMsg
                              delegate:nil
                              cancelButtonTitle:kWarningConfirmButton
                              otherButtonTitles:nil];
        [alert show];
        
        return false;
    }
    
    // whether valid ip
    if (![self isValidIP:deviceInfo.chDeviceAddr])
    {
        UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:kWarningTitle
                              message:kDeviceAddrInvalidMsg
                              delegate:nil
                              cancelButtonTitle:kWarningConfirmButton
                              otherButtonTitles:nil];
        [alert show];
        
        return false;
    }
    
    // check port
    if (deviceInfo.nDevicePort == 0)
    {
        UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:kWarningTitle
                              message:kDevicePortEmptyMsg
                              delegate:nil
                              cancelButtonTitle:kWarningConfirmButton
                              otherButtonTitles:nil];
        [alert show];
        
        return false;
    }
    
    // check username
    if ([deviceInfo.chLoginName compare:@""] == NSOrderedSame)
    {
        UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:kWarningTitle
                              message:kDeviceUserNameEmptyMsg
                              delegate:nil
                              cancelButtonTitle:kWarningConfirmButton
                              otherButtonTitles:nil];
        [alert show];
        return false;
    }
    
    // check username length
    if ([deviceInfo.chLoginName lengthOfBytesUsingEncoding:NSUTF8StringEncoding] > 64)
    {
        UIAlertView *alert = [[UIAlertView alloc] 
                              initWithTitle:kWarningTitle
                              message:kDeviceUserNameTooLongerMsg
                              delegate:nil 
                              cancelButtonTitle:kWarningConfirmButton
                              otherButtonTitles:nil];	
        [alert show];
        return false;
    }
    
    // check password length
    if ([deviceInfo.chPassWord lengthOfBytesUsingEncoding:NSUTF8StringEncoding] > 16)
    {
        UIAlertView *alert = [[UIAlertView alloc] 
                              initWithTitle:kWarningTitle
                              message:kDevicePasswordTooLongerMsg
                              delegate:nil 
                              cancelButtonTitle:kWarningConfirmButton
                              otherButtonTitles:nil];	
        [alert show];
        return false;
    }
    
    return true;
}


/*******************************************************************************
 Function:			isValidIP
 Description:		check ip
 Input:				ipStr － IP address
 Output:
 Return:			true-valid,false-invalid
 *******************************************************************************/
- (bool)isValidIP:(NSString *)ipStr
{
    const char* ip = [ipStr cStringUsingEncoding:NSUTF8StringEncoding];
    
    // check invalid char
    int temp = 0;
    for (int i = 0; i < strlen(ip); i++)
    {
        // <1 or > 9,invalid char
        temp = (int)ip[i];
        if ((temp >= 48 && temp <= 57) || temp == 46)
        {
            continue;
        }
        else
        {
            return false;
        }
    }
    
    int n;
    unsigned int a, b, c, d;
    if(strlen(ip) <= 15 &&
       sscanf(ip, "%3u.%3u.%3u.%3u%n", &a, &b, &c, &d, &n) >= 4
       && n == static_cast<int>(strlen(ip)))
    {
        return (a > 0 && a <= 255 && b <= 255 && c <= 255 && d <= 255 && d > 0) || (a == 0 && b== 0 && c == 0 && d == 0);
    }
    return false;
}



//playback callback function
void fPlayDataCallBack_V40(LONG lPlayHandle, DWORD dwDataType, BYTE *pBuffer,DWORD dwBufSize,void *pUser)
{
    CameraViewController *pDemo = (__bridge CameraViewController*)pUser;
    int i = 0;
    switch (dwDataType)
    {
        case NET_DVR_SYSHEAD:
            if (dwBufSize > 0 && pDemo->m_nPlaybackPort == -1)
            {
                if(PlayM4_GetPort(&pDemo->m_nPlaybackPort) != 1)
                {
                    NSLog(@"PlayM4_GetPort failed:%d",  NET_DVR_GetLastError());
                    break;
                }
                if (!PlayM4_SetStreamOpenMode(pDemo->m_nPlaybackPort, STREAME_FILE))
                {
                    break;
                }
                if (!PlayM4_OpenStream(pDemo->m_nPlaybackPort, pBuffer , dwBufSize, 2*1024*1024))
                {
                    break;
                }
                pDemo->m_bPreview = 0;
                [pDemo startPlayer];
            }
            break;
        default:
            if (dwBufSize > 0 && pDemo->m_nPlaybackPort != -1)
            {
                for(i = 0; i < 4000; i++)
                {
                    if(PlayM4_InputData(pDemo->m_nPlaybackPort, pBuffer, dwBufSize))
                    {
                        break;
                    }
                    usleep(10*1000);
                }
            }
            break;
    }
}


//start player
- (void) startPlayer
{
    [self performSelectorOnMainThread:@selector(playerPlay)
                           withObject:nil
                        waitUntilDone:NO];
}

//play,the function PlayM4_Play must be called in main thread
- (void) playerPlay
{
    int nRet = 0;
    if(m_bPreview)
    {
        nRet = PlayM4_Play(m_nPreviewPort, (__bridge void*)m_playView);
        PlayM4_PlaySound(m_nPreviewPort);
    }
    else
    {
        nRet = PlayM4_Play(m_nPlaybackPort, (__bridge void*)m_playView);
        PlayM4_PlaySound(m_nPlaybackPort);
    }
    if (nRet != 1)
    {
        NSLog(@"PlayM4_Play fail");
        [self stopPlay];
        return;
    }
}


//stop preview
-(void) stopPlay
{
    if (m_lRealPlayID != -1)
    {
        NET_DVR_StopRealPlay(m_lRealPlayID);
        m_lRealPlayID = -1;
    }
    
    if(m_nPreviewPort >= 0)
    {
        if(!PlayM4_StopSound())
        {
            NSLog(@"PlayM4_StopSound failed");
        }
        if (!PlayM4_Stop(m_nPreviewPort))
        {
            NSLog(@"PlayM4_Stop failed");
        }
        if(!PlayM4_CloseStream(m_nPreviewPort))
        {
            NSLog(@"PlayM4_CloseStream failed");
        }
        if (!PlayM4_FreePort(m_nPreviewPort))
        {
            NSLog(@"PlayM4_FreePort failed");
        }
        m_nPreviewPort = -1;
    }
}



void g_fExceptionCallBack(DWORD dwType, LONG lUserID, LONG lHandle, void *pUser)
{
    NSLog(@"g_fExceptionCallBack Type[0x%x], UserID[%d], Handle[%d]", dwType, lUserID, lHandle);
}

@end
