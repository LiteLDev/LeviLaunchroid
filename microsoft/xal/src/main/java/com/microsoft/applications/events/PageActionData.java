package com.microsoft.applications.events;

/**
 * @author <a href="https://github.com/RadiantByte">RadiantByte</a>
 */

public class PageActionData {
    public ActionType actionType;
    public String pageViewId;
    public String targetItemId = "";
    public String targetItemDataSourceName = "";
    public String targetItemDataSourceCategory = "";
    public String targetItemDataSourceCollection = "";
    public String targetItemLayoutContainer = "";
    public String destinationUri = "";
    public RawActionType rawActionType = RawActionType.Unspecified;
    public InputDeviceType inputDeviceType = InputDeviceType.Unspecified;
    public short targetItemLayoutRank = 0;

    public PageActionData(String str, ActionType actionType) {
        this.pageViewId = str;
        this.actionType = actionType;
    }
}