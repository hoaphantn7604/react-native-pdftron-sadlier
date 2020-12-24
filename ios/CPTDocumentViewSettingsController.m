//
//  CPTDocumentViewSettingsController.m
//  RNPdftron
//
//  Created by Vien Nguyen Hai on 7/21/20.
//  Copyright © 2020 Facebook. All rights reserved.
//

#import "CPTDocumentViewSettingsController.h"

@interface CPTDocumentViewSettingsController ()

@end

@implementation CPTDocumentViewSettingsController

- (void)viewDidLoad {
    [super viewDidLoad];
        
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
}

#pragma mark - Table view data source

//- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
//#warning Incomplete implementation, return the number of sections
//    return 0;
//}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    if (section == 0) { // this section has the reflow/reader mode option
    // remove a row from this section only
    return [super tableView:tableView numberOfRowsInSection:section] - 1;
    }
    
    if (section == 1) {
        return 0;
    }
    // other sections don't need to be modified
    return [super tableView:tableView numberOfRowsInSection:section];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:@"Cell"];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"Cell"];
    }
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
    
    if (indexPath.section == 0 && indexPath.row == 2) {
        cell.textLabel.text = @"Spread View";
        cell.imageView.image = [UIImage imageNamed:@"cover_facing"];
        cell.accessoryType = self.settings.pagePresentationMode == 5 ? UITableViewCellAccessoryCheckmark : UITableViewCellAccessoryNone;
        return cell;
    }
    
    if (indexPath.section == 0 && indexPath.row == 3) {
    // this cell is no longer the reflow mode cell so we should return what would normally be the next cell (cell row index 4, vertical scrolling)
        return [super tableView:tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:4 inSection:indexPath.section]];
    }
    return [super tableView:tableView cellForRowAtIndexPath:indexPath];
}

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section {
    if (section == 1) {
        return 0;
    }
    return 44;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.section == 0 && indexPath.row == 1)
    {
        return 0;
    }

    return [super tableView:tableView heightForRowAtIndexPath:indexPath];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (indexPath.section == 0 && indexPath.row == 3)
    {
        return;
    }
    return [super tableView:tableView didSelectRowAtIndexPath:indexPath];
}

@end
