//
//  RNTPTDocumentViewManager.h
//  RNPdftron
//
//  Copyright © 2018 PDFTron. All rights reserved.
//

#import "RNTPTDocumentView.h"

#import <React/RCTViewManager.h>

@interface RNTPTDocumentViewManager : RCTViewManager <RNTPTDocumentViewDelegate>

@property (nonatomic, strong) NSMutableDictionary<NSNumber *, RNTPTDocumentView *> * _Nonnull documentViews;

- (void)setToolModeForDocumentViewTag:(NSNumber *_Nonnull)tag toolMode:(NSString *_Nonnull)toolMode;

- (BOOL)commitToolForDocumentViewTag:(NSNumber *_Nonnull)tag;

- (int)getPageCountForDocumentViewTag:(NSNumber *_Nonnull)tag;

- (NSString *_Nonnull)exportAnnotationsForDocumentViewTag:(NSNumber *_Nonnull)tag options:(NSDictionary *_Nonnull)options;
- (NSString *_Nonnull)exportBookmarksDocumentViewTag:(NSNumber *_Nonnull)tag;
- (void)importBookmarksDocumentViewTag:(NSNumber *_Nonnull)tag bookmark:(NSString *_Nonnull) bookmark;
- (NSString *_Nonnull)toTextForDocumentViewTag:(NSNumber *_Nonnull)tag number:(int)number;
- (void)importAnnotationsForDocumentViewTag:(NSNumber *_Nonnull)tag xfdf:(NSString *_Nonnull)xfdfString;

- (void)flattenAnnotationsForDocumentViewTag:(NSNumber *_Nonnull)tag formsOnly:(BOOL)formsOnly;

- (void)saveDocumentForDocumentViewTag:(NSNumber *_Nonnull)tag completionHandler:(void (^_Nullable)(NSString * _Nullable filePath))completionHandler;

- (void)setFlagForFieldsForDocumentViewTag:(NSNumber *_Nullable)tag forFields:(NSArray<NSString *> *_Nonnull)fields setFlag:(PTFieldFlag)flag toValue:(BOOL)value;

- (void)setValueForFieldsForDocumentViewTag:(NSNumber *_Nullable)tag map:(NSDictionary<NSString *, id> *_Nullable)map;

- (void)importAnnotationCommandForDocumentViewTag:(NSNumber *_Nullable)tag xfdfCommand:(NSString *_Nullable)xfdfCommand initialLoad:(BOOL)initialLoad;

- (void)pageLabelViewTag:(NSNumber *_Nullable)tag mapping:(NSString *_Nullable)mapping;

@end
