package com.family.donghyunlee.family.timeline;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.family.donghyunlee.family.OnSingleClickListener;
import com.family.donghyunlee.family.Profile;
import com.family.donghyunlee.family.R;
import com.family.donghyunlee.family.data.IsCheck;
import com.family.donghyunlee.family.data.TimeLineItem;
import com.family.donghyunlee.family.data.TimelineCountItem;
import com.family.donghyunlee.family.data.User;
import com.family.donghyunlee.family.dialog.TimelineCardDialogFragment;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by DONGHYUNLEE on 2017-08-15.
 */
public class TimelineRecyclerAdapter extends RecyclerView.Adapter<TimelineRecyclerAdapter.TimelineViewHolder> {

    private static final String TAG = TimelineRecyclerAdapter.class.getSimpleName();
    private static final int WANT_TO_COMMENT_COUNT = 102;
    private Context mContext;
    private ArrayList<TimeLineItem> items;
    private LayoutInflater mInflater;
    private ArrayList<User> profile_items;

    // ViewHolder Type
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;
    public static final int TYPE_FOOTER = 2;

    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference profile_pathRef;
    private StorageReference timeline_pathRef;
    private String storageProfileFolder;
    private String storageTimelineFolder;

    private DatabaseReference likeReference;
    private DatabaseReference likeUserReference;

    ProfileRecyclerAdapter adapter;
    RecyclerView.LayoutManager layoutManager;


    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private ChildEventListener mChildEventListener;
    private DatabaseReference timelineReference;

    private String groupId;
    private List<String> mTimelineIds;
    //아이템 클릭시 실행 함수
    Activity activity;
    // like enable
    FragmentManager fragmentManager;

    public TimelineRecyclerAdapter(Activity activity, Context context, final ArrayList<TimeLineItem> items,
                                   ArrayList<User> profile_items, final String groupId, FragmentManager fragmentManager) {
        this.activity = activity;
        this.mContext = context;
        this.items = items;
        this.mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.profile_items = profile_items;
        this.groupId = groupId;
        this.fragmentManager = fragmentManager;
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        timelineReference = database.getReference().child("groups").child(groupId).child("timelineCard");
        mTimelineIds = new ArrayList<>();

        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());
                mTimelineIds.add(0, dataSnapshot.getKey());
                final TimeLineItem timeLineItem = dataSnapshot.getValue(TimeLineItem.class);
                String key = timeLineItem.getTimeline_key();
                likeUserReference = database.getReference().child("groups").child(groupId).child("likes")
                        .child("timelineCard").child(key).child("users").child(currentUser.getUid());
                likeUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        IsCheck ischeck = dataSnapshot.getValue(IsCheck.class);
                        if (ischeck == null){
                            Log.i(TAG, ">>>>>>>>>> 1");
                            timeLineItem.setIsCheck(false);
                            items.add(0, timeLineItem);
                            // TODO 수정 할 것! 해당 위치만 변경
                            notifyDataSetChanged();
                            return;
                        } else if (ischeck.getIsCheck()) {

                            Log.i(TAG, ">>>>>>>>>> 2");
                            timeLineItem.setIsCheck(true);
                            items.add(0, timeLineItem);
                            notifyDataSetChanged();
                            return;

                        } else if(!ischeck.getIsCheck()){

                            Log.i(TAG, ">>>>>>>>>> 3");
                            timeLineItem.setIsCheck(false);
                            items.add(0, timeLineItem);
                            notifyDataSetChanged();
                            return;
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                //notifyItemInserted(items.size() - 1); ??
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
//                Toast.makeText(mContext, "변경", Toast.LENGTH_SHORT).show();
                TimeLineItem newTimeLineItem = dataSnapshot.getValue(TimeLineItem.class);
                String timelineCardKey = dataSnapshot.getKey();

                // [START_EXCLUDE]
                int timelineIndex = mTimelineIds.indexOf(timelineCardKey);
                if (timelineIndex > -1) {

                    items.set(timelineIndex, newTimeLineItem);

                    notifyItemChanged(timelineIndex);
                } else {
                    Log.w(TAG, "onChildChanged:unknown_child:" + timelineCardKey);
                }
                // [END_EXCLUDE]

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                 String timelineCardKey = dataSnapshot.getKey();
                // [START_EXCLUDE]
                int timelineIndex = mTimelineIds.indexOf(timelineCardKey);
                if (timelineIndex > -1) {
                    // Remove data from the list
                    mTimelineIds.remove(timelineIndex);
                    items.remove(timelineIndex);
                    // Update the RecyclerView
                    // Log.d(TAG, ">>>>>      items: "  +items.get(timelineIndex).getTimeline_date() + "/ index:" + timelineIndex );
                    notifyDataSetChanged();
                   // notifyItemRemoved(timelineIndex + 1);
                } else {
                    Log.w(TAG, "onChildRemoved:unknown_child:" + timelineCardKey);
                }
                // [END_EXCLUDE]
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        timelineReference.addChildEventListener(mChildEventListener);
    }

    public class TimelineViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // holder 의 viewType
        int mViewType;
        // body 의 view components
        TextView timelineNickname;
        TextView timelineDate;
        ImageView timelineProfileImage;
        TextView timelineContent;
        TextView timelineCommentCnt;
        TextView timelineLikeCnt;
        ImageView timelineContentImage;
        ImageButton timelineLike;
        LinearLayout timelineLikeContainer;
        LinearLayout timelineCommentContainer;
        LinearLayout timelineContentContainer;
        ImageView timeline_smallconmment;
        // footer의 view
        ImageView timelineFooterIcon;

        // Header 의 view components
        RecyclerView rv_profile;
        TextView tlHeaderContainer;
        ImageButton tlHeaderAddimage;
        ImageButton timelineExpand;


        // viewType 에 따른 viewHolder 정의
        public TimelineViewHolder(View itemView, int viewType) {
            super(itemView);
            storage = FirebaseStorage.getInstance();
            storageProfileFolder = mContext.getResources().getString(R.string.storage_profiles_folder);
            storageTimelineFolder = mContext.getResources().getString(R.string.storage_timeline_folder);
            storageRef = storage.getReferenceFromUrl(mContext.getResources().getString(R.string.firebase_storage));
            mViewType = viewType;

            if (TYPE_HEADER == viewType) {
                rv_profile = (RecyclerView) itemView.findViewById(R.id.rv_profile);
                tlHeaderContainer = (TextView) itemView.findViewById(R.id.tl_header_container);
                tlHeaderContainer.setOnClickListener(this);
                tlHeaderAddimage = (ImageButton) itemView.findViewById(R.id.tl_header_addimage);
                tlHeaderAddimage.setOnClickListener(this);

            }
            if (TYPE_FOOTER == viewType) {
                timelineFooterIcon = (ImageView) itemView.findViewById(R.id.timelinefootericon);

            } else {
                timelineLike = (ImageButton) itemView.findViewById(R.id.timeline_like);
                timelineNickname = (TextView) itemView.findViewById(R.id.timeline_nickname);
                timelineDate = (TextView) itemView.findViewById(R.id.timeline_date);
                timelineProfileImage = (ImageView) itemView.findViewById(R.id.timeline_profile);
                timelineContent = (TextView) itemView.findViewById(R.id.timeline_content);
                timelineCommentCnt = (TextView) itemView.findViewById(R.id.timline_comment_cnt);
                timelineLikeCnt = (TextView) itemView.findViewById(R.id.timline_like_cnt);
                timelineContentImage = (ImageView) itemView.findViewById(R.id.timeline_contentimage);
                timelineLikeContainer = (LinearLayout) itemView.findViewById(R.id.timeline_like_container);
                timelineCommentContainer = (LinearLayout) itemView.findViewById(R.id.timeline_comment_container);
                timelineContentContainer = (LinearLayout) itemView.findViewById(R.id.timeline_content_container);
                timelineExpand = (ImageButton) itemView.findViewById(R.id.timeline_expand);
                timeline_smallconmment = (ImageView) itemView.findViewById(R.id.timeline_smallconmment);
            }

        }

        @Override
        public void onClick(View v) {
            Intent intent;

            switch (v.getId()) {

                case R.id.tl_header_container:
                    Activity origin = (Activity)mContext;
                    intent = new Intent(mContext, TimelineWrite.class);
                    intent.putExtra("FLAG", 0);
                    origin.startActivityForResult(intent, WANT_TO_COMMENT_COUNT);
                    break;

                case R.id.tl_header_addimage:

                    intent = new Intent(mContext, TimelineWrite.class);
                    intent.putExtra("FLAG", 1);
                    mContext.startActivity(intent);

                    break;
            }
        }
    }


    // header 여부 체크 메소드
    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    // header 여부 체크 메소드
    private boolean isPositionFooter(int position) {
        return position == items.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        } else if (isPositionFooter(position)) {
            return TYPE_FOOTER;
        } else
            return TYPE_ITEM;
    }

    // header, body 의 view 정의
    @Override
    public TimelineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {

            View headerView = mInflater.inflate(R.layout.timeline_header_item, parent, false);
            return new TimelineViewHolder(headerView, viewType);
        } else if (viewType == TYPE_FOOTER) {
            View footerView = LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_footer_item, parent, false);
            return new TimelineViewHolder(footerView, viewType);
        } else {

            View itemView = mInflater.inflate(R.layout.timeline_body_item, parent, false);
            return new TimelineViewHolder(itemView, viewType);
        }
    }

    // header, body 에 data bind
    @Override
    public void onBindViewHolder(TimelineViewHolder holder, final int position) {
        // 1. header data bind
        if (holder.mViewType == TYPE_HEADER) {
            bindHeaderItem(holder);
        } else if (holder.mViewType == TYPE_FOOTER) {
            Glide.with(mContext).load(R.drawable.ic_icon_font).crossFade().into(holder.timelineFooterIcon);
        }
        // 2. body data bind
        else {
            bindBodyItem(holder, position - 1);
        }

    }

    // head itemSet bind
    private void bindHeaderItem(TimelineViewHolder holder) {
        adapter = new ProfileRecyclerAdapter(mContext, profile_items, groupId, activity);
        layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        holder.rv_profile.setLayoutManager(layoutManager);
        holder.rv_profile.setAdapter(adapter);
        // Layout Manager 를 이용해 Horizontal Option 을 적용


    }

    // body itemSet bind
    private void bindBodyItem(final TimelineViewHolder holder, final int position) {

        // text data bind
        holder.timelineNickname.setText(items.get(position).getTimeline_nickName());
        holder.timelineDate.setText(items.get(position).getTimeline_date());
        holder.timelineContent.setText(items.get(position).getTimeline_content());
        holder.timelineCommentCnt.setText(String.format("%d", (items.get(position).getTimelineCountItem().getCommentCnt())));
        holder.timelineLikeCnt.setText(String.format("%d", (items.get(position).getTimelineCountItem().getLikeCnt())));

        profile_pathRef = storageRef.child(storageProfileFolder + "/" + items.get(position).getTimeline_profileImage());
        timeline_pathRef = storageRef.child(storageTimelineFolder + "/" + items.get(position).getTimeline_contentImage());
        // image data bind
        Glide.with(mContext).using(new FirebaseImageLoader()).load(profile_pathRef).centerCrop()
                .crossFade().bitmapTransform(new CropCircleTransformation(mContext)).into(holder.timelineProfileImage);

        // 버튼 set.
       if(items.get(position).getIsCheck() == false){
           Log.i(TAG, ">>>>>>>>>       false");
           holder.timelineLike.setSelected(false);

       } else{
           Log.i(TAG, ">>>>>>>>>       true");
           holder.timelineLike.setSelected(true);
       }

        if (!items.get(position).getTimeline_contentImage().equals("empty")) {
            holder.timelineContentImage.setVisibility(View.VISIBLE);


            Glide.with(mContext).using(new FirebaseImageLoader()).load(timeline_pathRef).centerCrop()
                    .crossFade().into(holder.timelineContentImage);
        } else {
            holder.timelineContentImage.setVisibility(View.GONE);
        }
        holder.timelineLike.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                changeLikeCount(holder, position);
            }
        });
        holder.timelineLikeContainer.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                changeLikeCount(holder, position);
            }
        });

        holder.timelineCommentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, Comment.class);
                intent.putExtra("TimelineItem", items.get(position));
                intent.putExtra("LikeCnt", items.get(position).getTimelineCountItem().getLikeCnt());
                intent.putExtra("CommentCnt", items.get(position).getTimelineCountItem().getCommentCnt());
                intent.putExtra("position", position);
                mContext.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in, R.anim.step_back);

            }
        });
        holder.timelineContentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, Comment.class);
                intent.putExtra("TimelineItem", items.get(position));
                intent.putExtra("LikeCnt", items.get(position).getTimelineCountItem().getLikeCnt());
                intent.putExtra("commentCnt", items.get(position).getTimelineCountItem().getCommentCnt());
                intent.putExtra("position", position);
                mContext.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in, R.anim.step_back);
            }
        });
        holder.timeline_smallconmment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, Comment.class);
                intent.putExtra("TimelineItem", items.get(position));
                intent.putExtra("LikeCnt", items.get(position).getTimelineCountItem().getLikeCnt());
                intent.putExtra("commentCnt", items.get(position).getTimelineCountItem().getCommentCnt());
                intent.putExtra("position", position);
                mContext.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in, R.anim.step_back);            }
        });

        holder.timelineExpand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimelineCardDialogFragment dialogFragment = TimelineCardDialogFragment.newInstance(items.get(position).getTimeline_key(),
                        items.get(position).getTimeline_contentImage());
                fragmentManager.beginTransaction().commit();
                dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, 0);
                dialogFragment.show(fragmentManager, "timelineCardDialog");
            }
        });
        holder.timelineProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, Profile.class);
                intent.putExtra("userId", items.get(position).getTimeline_userId());
                mContext.startActivity(intent);
                activity.overridePendingTransition(R.anim.y_slide_in, R.anim.y_step_back);
            }
        });

    }

    // isCheck => 사용자가 체크 했는지 확인하는 프래그
    // isSelect => 현재 좋아요 클릭.
    private void changeLikeCount(final TimelineViewHolder holder, final int position) {
        String key = items.get(position).getTimeline_key();

        IsCheck isCheck;
        likeUserReference = database.getReference().child("groups").child(groupId).child("likes")
                .child("timelineCard").child(key).child("users");

        likeReference = database.getReference().child("groups").child(groupId)
                .child("timelineCard").child(key).child("timelineCountItem");

        if (!items.get(position).getIsCheck()) { // 좋아요 클릭
            items.get(position).setIsCheck(true);
            // 개인 사용자 좋아요 클릭 설정
            Log.i(TAG, ">>>>>>>>>>>>>>>>>>>                 HERE" + items.get(position).getIsCheck());
            isCheck = new IsCheck(true);
            likeUserReference.child(currentUser.getUid()).setValue(isCheck).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    int cnt = Integer.parseInt(holder.timelineLikeCnt.getText().toString());
                    items.get(position).setIsCheck(true);
                    // 좋아요 카운트
                    holder.timelineLike.setSelected(true);
                    holder.timelineLikeCnt.setText(String.format("%d", ++cnt));
                }
            });
            likeReference.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    TimelineCountItem timelineCountItem = mutableData.getValue(TimelineCountItem.class);
                    if (timelineCountItem == null) {
                        return Transaction.success(mutableData);
                    } else {
                        Log.d(TAG, ">>>>>>>...... " + timelineCountItem.getLikeCnt());
                        int cnt = timelineCountItem.getLikeCnt();
                        timelineCountItem.setLikeCnt(++cnt);
                    }
                    mutableData.setValue(timelineCountItem);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    // Transaction completed
                    Log.d(TAG, "postTransaction:onComplete:" + databaseError);
                    IsCheck isCheck = new IsCheck(true);
                    likeUserReference.child(currentUser.getUid()).setValue(isCheck);

                }
            });
        } else { // 좋아요 클릭 해제

            items.get(position).setIsCheck(false);
            // 개인 사용자 좋아요 클릭 설정

            Log.i(TAG, ">>>>>>>>>>>>>>>>>>>                 HERE2" + items.get(position).getIsCheck());
            isCheck = new IsCheck(false);
            likeUserReference.child(currentUser.getUid()).setValue(isCheck).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    int cnt = Integer.parseInt(holder.timelineLikeCnt.getText().toString());
                    items.get(position).setIsCheck(false);
                    holder.timelineLike.setSelected(false);
                    holder.timelineLikeCnt.setText(String.format("%d", --cnt));
                }
            });

            likeReference.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    TimelineCountItem timelineCountItem = mutableData.getValue(TimelineCountItem.class);
                    if (timelineCountItem == null) {
                        return Transaction.success(mutableData);
                    } else {
                        Log.d(TAG, ">>>>>>>...... " + timelineCountItem.getLikeCnt());
                        int cnt = timelineCountItem.getLikeCnt();
                        timelineCountItem.setLikeCnt(--cnt);
                    }
                    mutableData.setValue(timelineCountItem);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    // Transaction completed
                    Log.d(TAG, "postTransaction:onComplete:" + databaseError);
                    IsCheck isCheck = new IsCheck(false);
                    likeUserReference.child(currentUser.getUid()).setValue(isCheck);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + 2; // header + body + footer;
    }

    public void setProfileItem(ArrayList<User> profile_items) {
        this.profile_items = profile_items;
        adapter.notifyDataSetChanged();
    }

    public void cleanupListener() {
        if (mChildEventListener != null) {
            timelineReference.removeEventListener(mChildEventListener);
        }
    }

}