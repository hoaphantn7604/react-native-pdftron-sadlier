#import <Tools/Tools.h>

NS_ASSUME_NONNULL_BEGIN

@class RNTPTDocumentViewController;

@protocol RNTPTDocumentViewControllerDelegate <PTDocumentViewControllerDelegate>
@required

- (void)rnt_documentViewControllerDocumentLoaded:(PTDocumentViewController *)documentViewController;

- (void)rnt_documentViewControllerDidZoom:(PTDocumentViewController *)documentViewController;

- (BOOL)rnt_documentViewControllerIsTopToolbarEnabled:(PTDocumentViewController *)documentViewController;

- (BOOL)rnt_documentViewControllerShouldGoBackToPan:(PTDocumentViewController *)documentViewController;

- (void)rnt_documentViewController:(PTDocumentViewController *)documentViewController filterMenuItemsForAnnotationSelectionMenu:(UIMenuController *)menuController;

- (BOOL)rnt_documentViewController:(PTDocumentViewController *)documentViewController filterMenuItemsForAnnotationSelectionMenu:(UIMenuController *)menuController forAnnotation:(PTAnnot *)annot;

- (BOOL)rnt_documentViewController:(PTDocumentViewController *)documentViewController filterMenuItemsForLongPressMenu:(UIMenuController *)menuController;

- (void)rnt_documentViewControllerDidChange:(PTDocumentViewController *)documentViewController;

@end

@interface RNTPTDocumentViewController : PTDocumentViewController <PTOutlineViewControllerDelegate, PTBookmarkViewControllerDelegate, PTAnnotationViewControllerDelegate, UICollectionViewDelegate, UICollectionViewDelegateFlowLayout>

@property (nonatomic, weak, nullable) id<RNTPTDocumentViewControllerDelegate> delegate;

- (void)openCustomContentViewController;

@end

NS_ASSUME_NONNULL_END
