#import "RNTPTDocumentViewController.h"
#import "RNTPTDocumentView.h"

NS_ASSUME_NONNULL_BEGIN

@interface RNTPTDocumentViewController ()

@property (nonatomic) BOOL local;
@property (nonatomic) BOOL needsDocumentLoaded;
@property (nonatomic) BOOL needsRemoteDocumentLoaded;
@property (nonatomic) BOOL documentLoaded;


// My controller
@property (nonatomic) PTNavigationListsViewController* myNavigationListsViewController;

@end

NS_ASSUME_NONNULL_END

@implementation RNTPTDocumentViewController 

@dynamic delegate;

- (void)viewWillLayoutSubviews
{
    [super viewWillLayoutSubviews];
    
    if (self.needsDocumentLoaded) {
        self.needsDocumentLoaded = NO;
        self.needsRemoteDocumentLoaded = NO;
        self.documentLoaded = YES;
        
        if ([self.delegate respondsToSelector:@selector(rnt_documentViewControllerDocumentLoaded:)]) {
            [self.delegate rnt_documentViewControllerDocumentLoaded:self];
        }
    }
}

- (void)openCustomContentViewController
{
    @try {
        // Initialize a new navigation lists view controller.
        PTNavigationListsViewController* navigationListsViewController = [[PTNavigationListsViewController alloc] initWithToolManager:self.toolManager];
        
        // Initialize an annotation, outline, and bookmark view controller with a PTPDFViewCtrl instance.
        PTAnnotationViewController* annotationViewController = [[PTAnnotationViewController alloc] initWithPDFViewCtrl:self.pdfViewCtrl];
        annotationViewController.delegate = self;
        
        PTThumbnailsViewController *thumbnailsViewController = [[PTThumbnailsViewController alloc] initWithPDFViewCtrl:self.pdfViewCtrl];
        thumbnailsViewController.collectionView.delegate = self;
        thumbnailsViewController.tabBarItem.image = [UIImage imageNamed:@"toolbar-page"];
        thumbnailsViewController.editingEnabled = NO;

       

        PTOutlineViewController *outlineViewController = [[PTOutlineViewController alloc] initWithPDFViewCtrl:self.pdfViewCtrl];
        outlineViewController.delegate = self;
        outlineViewController.title = @"Table of content";

        PTBookmarkViewController *bookmarkViewController = [[PTBookmarkViewController alloc] initWithPDFViewCtrl:self.pdfViewCtrl];
        bookmarkViewController.delegate = self;

        // Set the array of child view controllers to display.
        navigationListsViewController.listViewControllers = @[outlineViewController, thumbnailsViewController, annotationViewController, bookmarkViewController];
        
        if ( UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad ){
            navigationListsViewController.modalPresentationStyle = UIModalPresentationFullScreen;
        }
        
        _myNavigationListsViewController = navigationListsViewController;
        
        [self presentViewController:navigationListsViewController animated:YES completion:nil];
    }
    @catch (NSException *exception) {
       return;
    }
}

- (void)openDocumentWithURL:(NSURL *)url password:(NSString *)password
{
    if ([url isFileURL]) {
        self.local = YES;
    } else {
        self.local = NO;
    }
    self.documentLoaded = NO;
    self.needsDocumentLoaded = NO;
    self.needsRemoteDocumentLoaded = NO;
    
    [super openDocumentWithURL:url password:password];
}

- (BOOL)isTopToolbarEnabled
{
    if ([self.delegate respondsToSelector:@selector(rnt_documentViewControllerIsTopToolbarEnabled:)]) {
        return [self.delegate rnt_documentViewControllerIsTopToolbarEnabled:self];
    }
    return YES;
}

- (void)setControlsHidden:(BOOL)hidden animated:(BOOL)animated
{
    if (!hidden && ![self isTopToolbarEnabled]){
        return;
    }
    
    [super setControlsHidden:hidden animated:animated];
}

#pragma mark - <PTToolManagerDelegate>

- (void)toolManagerToolChanged:(PTToolManager *)toolManager
{
    [super toolManagerToolChanged:toolManager];
    
    // If the top toolbar is disabled...
    if (![self isTopToolbarEnabled] &&
        // ...and the annotation toolbar is visible now...
        ![self isAnnotationToolbarHidden]) {
        // ...hide the toolbar.
        self.annotationToolbar.hidden = YES;
    }
}

- (BOOL)toolManager:(PTToolManager *)toolManager shouldShowMenu:(UIMenuController *)menuController forAnnotation:(PTAnnot *)annotation onPageNumber:(unsigned long)pageNumber
{
    BOOL result = [super toolManager:toolManager shouldShowMenu:menuController forAnnotation:annotation onPageNumber:pageNumber];
    if (!result) {
        return NO;
    }
    
    BOOL showMenu = YES;
    if (annotation) {
        if ([self.delegate respondsToSelector:@selector(rnt_documentViewController:
                                                        filterMenuItemsForAnnotationSelectionMenu:
                                                        forAnnotation:)]) {
            showMenu = [self.delegate rnt_documentViewController:self
                       filterMenuItemsForAnnotationSelectionMenu:menuController
                                                   forAnnotation:annotation];
        }
    } else {
        if ([self.delegate respondsToSelector:@selector(rnt_documentViewController:
                                                        filterMenuItemsForLongPressMenu:)]) {
            showMenu = [self.delegate rnt_documentViewController:self
                                 filterMenuItemsForLongPressMenu:menuController];
        }
    }
    
    return showMenu;
}

#pragma mark - <PTAnnotationToolbarDelegate>

- (BOOL)toolShouldGoBackToPan:(PTAnnotationToolbar *)annotationToolbar
{
    if ([self.delegate respondsToSelector:@selector(rnt_documentViewControllerShouldGoBackToPan:)]) {
        return [self.delegate rnt_documentViewControllerShouldGoBackToPan:self];
    }
    
    return [super toolShouldGoBackToPan:annotationToolbar];
}

#pragma mark - <PTPDFViewCtrlDelegate>

- (void)pdfViewCtrl:(PTPDFViewCtrl *)pdfViewCtrl onSetDoc:(PTPDFDoc *)doc
{
    [super pdfViewCtrl:pdfViewCtrl onSetDoc:doc];
    
    if (self.local && !self.documentLoaded) {
        self.needsDocumentLoaded = YES;
    }
    else if (!self.local && !self.documentLoaded && self.needsRemoteDocumentLoaded) {
        self.needsDocumentLoaded = YES;
    }
}

- (void)pdfViewCtrl:(PTPDFViewCtrl *)pdfViewCtrl downloadEventType:(PTDownloadedType)type pageNumber:(int)pageNum
{
    if (type == e_ptdownloadedtype_finished && !self.documentLoaded) {
        self.needsRemoteDocumentLoaded = YES;
    }
    
    [super pdfViewCtrl:pdfViewCtrl downloadEventType:type pageNumber:pageNum];
}

- (void)pdfViewCtrl:(PTPDFViewCtrl *)pdfViewCtrl pdfScrollViewDidZoom:(UIScrollView *)scrollView
{
    if ([self.delegate respondsToSelector:@selector(rnt_documentViewControllerDidZoom:)]) {
        [self.delegate rnt_documentViewControllerDidZoom:self];
    }
}

- (void)outlineViewControllerDidCancel:(PTOutlineViewController *)outlineViewController
{
    [outlineViewController dismissViewControllerAnimated:YES completion:nil];
}

- (void)annotationViewControllerDidCancel:(PTAnnotationViewController *)annotationViewController
{
    [annotationViewController dismissViewControllerAnimated:YES completion:nil];
}

- (void)bookmarkViewControllerDidCancel:(PTBookmarkViewController *)bookmarkViewController
{
    [bookmarkViewController dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - PTOutlineViewController's Delegate

- (void)outlineViewController:(PTOutlineViewController *)outlineViewController selectedBookmark:(NSDictionary *)bookmark
{
    [outlineViewController dismissViewControllerAnimated:YES completion:nil];
}

- (void)bookmarkViewController:(PTBookmarkViewController *)bookmarkViewController selectedBookmark:(NSDictionary *)bookmark
{
    [bookmarkViewController dismissViewControllerAnimated:YES completion:nil];
}

- (void)bookmarkViewController:(PTBookmarkViewController *)bookmarkViewController didAddBookmark:(PTUserBookmark *)bookmark
{
    if (self.delegate != nil)
    {
        [self.delegate rnt_documentViewControllerDidChange:self];
    }
}

- (void)bookmarkViewController:(PTBookmarkViewController *)bookmarkViewController didRemoveBookmark:(PTUserBookmark *)bookmark
{
    if (self.delegate != nil)
    {
        [self.delegate rnt_documentViewControllerDidChange:self];
    }
}


- (void)annotationViewController:(PTAnnotationViewController *)annotationViewController selectedAnnotaion: (NSDictionary *)anAnnotation
{
     [annotationViewController dismissViewControllerAnimated:YES completion:nil];
}

#pragma mark - UICollectionViewFlowLayout's Delegate

- (CGSize)collectionView:(UICollectionView *)collectionView layout:(UICollectionViewLayout* )collectionViewLayout referenceSizeForHeaderInSection:(NSInteger)section
{
    return CGSizeZero;
}

- (CGSize)collectionView:(UICollectionView* )collectionView layout:(UICollectionViewLayout* )collectionViewLayout sizeForItemAtIndexPath:(NSIndexPath *)indexPath
{
//    PTThumbnailsViewController *vc = _myNavigationListsViewController.listViewControllers[1];
//    vc.editingEnabled = NO;
    
    int numberOfCells = 6;
    
    if ( UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad ){
        numberOfCells = 6;
    }else{
        numberOfCells = 3;
    }
    
    float screenWidth = [[UIScreen mainScreen] bounds].size.width;
    float screenHeight = [[UIScreen mainScreen] bounds].size.height;

    float marginSize  = 10.0f;
    float itemWidth   = (MIN(screenWidth, screenHeight) - marginSize * 4) / numberOfCells;
    NSLog(@"CELL SIZE: %f", itemWidth);
    return CGSizeMake(itemWidth, 150);
}

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath* )indexPath
{
    
    if (_myNavigationListsViewController != nil) {
        
        NSNumber *parseInt = [NSNumber numberWithLong:indexPath.row];
        
        [self.pdfViewCtrl SetCurrentPage:parseInt.intValue + 1];

        [_myNavigationListsViewController dismissViewControllerAnimated:true completion:nil];
    }
}

@end
