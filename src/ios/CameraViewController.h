//
//  CameraViewController.h
//  HelloCordova
//
//  Created by WangAnnda on 2017/3/9.
//
//

#import <UIKit/UIKit.h>
#import "DeviceInfo.h"

@interface CameraViewController : UIViewController

@property UIView *m_playView;

@property int   m_nPreviewPort;
@property int   m_nPlaybackPort;
@property FILE *m_fp;
@property (nonatomic, retain) id m_playThreadID;
@property bool m_bThreadRun;
@property int m_lUserID;
@property int m_lRealPlayID;
@property int m_lPlaybackID;
@property bool m_bPreview;
@property bool m_bRecord;
@property bool m_bPTZL;
@property bool m_bVoiceTalk;

@property NSString *m_ip;
@property NSString *m_port;
@property NSString *m_user;
@property NSString *m_pwd;
@property int m_channel;
@property bool m_disableCtrl;

- (void) goback;

- (bool) validateValue:(DeviceInfo*)deviceInfo;
- (bool) isValidIP:(NSString *)ipStr;
- (void) startPlay;
- (void) stopPlay;

- (void) startPlayer;
- (void) playerPlay;

- (void) previewPlay:(int*)iPlayPort playView:(UIView*)playView;
- (void) stopPreviewPlay:(int*)iPlayPort;

@end
