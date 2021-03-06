package com.family.donghyunlee.family.data;

import android.support.annotation.Keep;

/**
 * Created by DONGHYUNLEE on 2017-08-18.
 */
@Keep
public class CommentItem {

    private String commentKey;
    private String commentNickName;
    private String commentDate;
    private String commentContent;
    private String commentProfileImage;
    private String commentImage;
    private CommentCountItem commentCountItem;
    private String timelineKey;
    private boolean isCheck;
    public CommentItem(){

    }
    public CommentItem(String timelineKey, String commentKey, String commentImage, String commentNickName, String commentDate, String commentContent, CommentCountItem commentCountItem, String commentProfileImage) {
        this.commentKey = commentKey;
        this.timelineKey = timelineKey;
        this.commentNickName = commentNickName;
        this.commentDate = commentDate;
        this.commentContent = commentContent;
        this.commentCountItem = commentCountItem;
        this.commentProfileImage = commentProfileImage;
        this.commentImage = commentImage;
        isCheck = false;
    }

    public void setIsCheck(boolean isCheck) {
        this.isCheck = isCheck;
    }

    public void setCommentCountItem(CommentCountItem commentCountItem) {
        this.commentCountItem = commentCountItem;
    }

    public void setTimelineKey(String timelineKey) {
        this.timelineKey = timelineKey;
    }

    public void setCommentKey(String commentKey) {
        this.commentKey = commentKey;
    }

    public void setCommentNickName(String commentNickName) {
        this.commentNickName = commentNickName;
    }

    public void setCommentDate(String commentDate) {
        this.commentDate = commentDate;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }


    public void setCommentProfileImage(String commentProfileImage) {
        this.commentProfileImage = commentProfileImage;
    }

    public void setCommentImage(String commentImage) {
        this.commentImage = commentImage;
    }

    public String getTimelineKey() {
        return timelineKey;
    }

    public String getCommentKey() {
        return commentKey;
    }

    public String getCommentNickName() {
        return commentNickName;
    }

    public String getCommentDate() {
        return commentDate;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public CommentCountItem getCommentCountItem() {
        return commentCountItem;
    }

    public String getCommentProfileImage() {
        return commentProfileImage;
    }

    public String getCommentImage() {
        return commentImage;
    }

    public boolean getIsCheck() {
        return isCheck;
    }
}
