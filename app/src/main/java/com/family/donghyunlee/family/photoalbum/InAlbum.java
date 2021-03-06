package com.family.donghyunlee.family.photoalbum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.family.donghyunlee.family.PhotoSel;
import com.family.donghyunlee.family.R;
import com.family.donghyunlee.family.data.MemoryAlbum;
import com.family.donghyunlee.family.data.MemoryPhotoImage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.zanlabs.widget.infiniteviewpager.InfiniteViewPager;
import com.zanlabs.widget.infiniteviewpager.indicator.CirclePageIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import jp.wasabeef.glide.transformations.ColorFilterTransformation;

public class InAlbum extends AppCompatActivity {

    @BindView(R.id.inalbum_viewpager)
    InfiniteViewPager viewPager;
    @BindView(R.id.inalbum_indicator)
    CirclePageIndicator circlePageIndicator;
    @BindView(R.id.title_inalbum_photo)
    TextView titleInalbumPhoto;
    @BindView(R.id.img_album_photo)
    ImageView imgalbumPhoto;
    @BindView(R.id.inalbum_temptext)
    TextView inalbumTemptext;
    @BindView(R.id.inalbum_edit)
    ImageButton inalbumEdit;
    @BindView(R.id.fab_inalbum)
    FloatingActionButton fab;

    private static final String TAG = InAlbum.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;
    private static final int GET_INALBUM_PHOTOSEL_REQUESTCODE = 100;
    private static final int GET_ALBUM_PHOTOSEL_REQUESTCODE = 101;
    private static final int ALBUM = 0;
    private static final int INALBUM = 1;

    private String keyMemoryPhotoImage;
    private String filename;
    private String albumId;
    private String groupId;
    private String userId;

    private Uri inAlbum_uri;
    private Uri album_uri;
    private ArrayList<MemoryPhotoImage> items = new ArrayList<>();
    private InAlbumPagerAdapter pagerAdapter;
    private FirebaseStorage storage;
    private FirebaseDatabase database;
    private DatabaseReference groupReference;
    private DatabaseReference changeAlbumReference;
    private ValueEventListener changingListener;
    private DatabaseReference changeInalbumReference;

    @Override
    public void onStop() {
        super.onStop();
        if (viewPager != null)
            viewPager.stopAutoScroll();
        changeAlbumReference.removeEventListener(changingListener);
        cleanupListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fab.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.e("!!!!!!!!!!!!!!!!", "onDESTROY");

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inalbum);
        ButterKnife.bind(this);

        setInit();

    }
    @Override
    public void onStart() {
        super.onStart();
        if (viewPager != null)
            viewPager.startAutoScroll();

        // album 사진 가져오기
        albumItemGet();

    }
    public void cleanupListener() {
        if (changingListener != null) {
            changeInalbumReference.removeEventListener(changingListener);
        }
    }
    private void setInit() {
        if (Build.VERSION.SDK_INT >= 21) {   //상태바 색상 변경
            getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.main_color_dark_c));
        }

        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        groupReference = database.getReference("groups");

        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        // sharedreference에서 가져오기
        groupId = pref.getString("groupId", "");

        // Fragment에 받는 객체
        Intent intent = getIntent();
        titleInalbumPhoto.setText((String) intent.getSerializableExtra("TITLE"));
        albumId = (String) intent.getSerializableExtra("ALBUM_ID");
        int position = (int) intent.getSerializableExtra("ALBUM_POSITION");
        // Fragment에 보낼 번들
        Intent sendIntent = new Intent(this, MemoryFragment.class);
        Bundle bundle = new Bundle();
        bundle.putString("ALBUMID", albumId);
        bundle.putInt("ALBUMPOSITION", position);
        sendIntent.putExtras(bundle);
        setResult(RESULT_OK, sendIntent);
        // album 사진 child listener
        changeInalbumReference = database.getReference("groups").child(groupId).child(albumId)
                .child("imagePackage");
        Log.e("1111111111111","groupid: "+ groupId + "albumId: " + albumId);


        // pageradapter setting
        pagerAdapter = new InAlbumPagerAdapter(this, albumId, groupId);
        pagerAdapter.setDataList(items);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setAutoScrollTime(8000);              // 8초 msec
        circlePageIndicator.setViewPager(viewPager);
        inAlbumGet();
    }

    @OnClick(R.id.inalbum_back)
    void backClick(){
        // Transition 유지하며 finish()
        fab.hide();
        supportFinishAfterTransition();
    }

    @OnClick(R.id.fab_inalbum)
    void onFabClick(){
        // 사진 permission
        requestPermission();
        Intent intent = new Intent(InAlbum.this, PhotoSel.class);
        startActivityForResult(intent, GET_INALBUM_PHOTOSEL_REQUESTCODE);
    }
    @OnClick(R.id.inalbum_edit)
    void onEditClick(){
        requestPermission();
        Intent intent = new Intent(InAlbum.this, PhotoSel.class);
        startActivityForResult(intent, GET_ALBUM_PHOTOSEL_REQUESTCODE);
    }
    // 카메라부터 사진을 가지고옴.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {

            case GET_INALBUM_PHOTOSEL_REQUESTCODE:{
                if (resultCode == RESULT_OK) {
                    inAlbum_uri = data.getParcelableExtra("IMAGE_URI");
                    oneItemADD(INALBUM);
                }
                else{
                    Log.e(TAG, ">>>>> "+ "GET_INALBUM_PHOTOSEL_REQUESTCODE don't result ");
                }
                break;
            }
            case GET_ALBUM_PHOTOSEL_REQUESTCODE:{
                if (resultCode == RESULT_OK) {
                    // 앰범 사진 변경하기, 파이어베이스에 사진 업로드.
                    // 파이어베이스 데이터베이스에 추가 및 저장소 올리기. (앨범)
                    album_uri = data.getParcelableExtra("IMAGE_URI");
                    oneItemADD(ALBUM);

                }
                else{
                    Log.e(TAG, ">>>>> "+ "GET_ALBUM_PHOTOSEL_REQUESTCODE don't result ");
                }
                break;
            }
            default:
                break;
        }
    }
    // 실시간 데이터베이스에서 inAlbum의 path 가져온다.
    public void inAlbumGet(){
        groupReference = database.getReference("groups")
                .child(groupId).child("memoryPhoto").child(albumId).child("imagePackage");
        groupReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> child = dataSnapshot.getChildren().iterator();
                boolean isExistInAlbum = false;
                while (child.hasNext()) {
                    MemoryPhotoImage memoryPhotoImage = child.next().getValue(MemoryPhotoImage.class);
                    items.add(memoryPhotoImage);
                    pagerAdapter.notifyDataSetChanged();
                    isExistInAlbum = true;
                }
                if(!isExistInAlbum){
                    inalbumTemptext.setText(" ");
                } else{
                    inalbumTemptext.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // Album 사진을 가져오는 함수.
    public void albumItemGet(){
        // album 사진 child listener
        changeAlbumReference = database.getReference("groups").child(groupId).child("memoryPhoto").child(albumId);

        // IMAGE만 변경될 것임을 생각하고 album value에서 listener한다.
        changingListener =new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String mainImgPath;
                MemoryAlbum memoryAlbum = dataSnapshot.getValue(MemoryAlbum.class);
                Log.e(TAG,"albumId: " +albumId + "!! mamoryAlbum:  " +memoryAlbum);
                mainImgPath = memoryAlbum.getMainImgPath();
                if(mainImgPath.equals("empty") == true){
                    imgalbumPhoto.setImageResource(R.drawable.img_default);
                }else{
                    String storageAlbumFolder = getResources().getString(R.string.storage_albumimages_folder);
                    StorageReference storageRef = storage.getReferenceFromUrl(getApplicationContext().getString(R.string.firebase_storage));
                    //StorageReference pathRef = storageRef.child(storageAlbumFolder + "/" + mainImgPath);
                    String filepath = storageAlbumFolder + "/" + mainImgPath;

                    // TODO LOADING
                    storageRef.child(filepath).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // Got the download URL for 'users/me/profile.png'
                            Glide.with(getApplicationContext()).load(uri)
                                    .bitmapTransform(new ColorFilterTransformation(getApplicationContext(), Color.argb(80, 255, 255, 255)))
                                    .crossFade().into(imgalbumPhoto);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle any errors
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        changeAlbumReference.addValueEventListener(changingListener);
    }



    // 사진을 저장소 업로드 및 데이터베이스에 갱신
    private void oneItemADD(int what_album_flag) {

        if(what_album_flag == INALBUM){
            uploadFile(inAlbum_uri, INALBUM);
        }
        else if(what_album_flag == ALBUM){
            uploadFile(album_uri, ALBUM);
        }
    }

    // TODO 어싱크나 서비스를 쓰자.
    private void uploadFile(Uri filePath, int what_album_flag) {
        groupReference = database.getReference("groups");
        if (filePath != null) {
            // 파일이름 생성.
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(getResources().getString(R.string.date_format));
            Date now = new Date();
            filename = formatter.format(now) + getResources().getString(R.string.file_extension);
            final SweetAlertDialog pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
            if(what_album_flag == INALBUM){
                Log.e("!!!!!!!!!!", "what_album_flag: " + what_album_flag);
                StorageReference storageRef = storage.getReferenceFromUrl(getResources().getString(R.string.firebase_storage))
                        .child(getResources().getString(R.string.storage_memoryimages_folder) + "/" + filename);
                storageRef.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // 실시간 데이터베이스에 저장
                        pDialog.hide();
                        long now = System.currentTimeMillis();
                        Date date = new Date(now);
                        SimpleDateFormat CurDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일");

                        groupReference = groupReference.child(groupId)
                                .child("memoryPhoto").child(albumId).child("imagePackage");
                        keyMemoryPhotoImage = groupReference.push().getKey();
                        MemoryPhotoImage memoryPhotoImage = new MemoryPhotoImage(keyMemoryPhotoImage, CurDateFormat.format(date), filename);
                        groupReference.child(keyMemoryPhotoImage).setValue(memoryPhotoImage);
                        items.add(0, memoryPhotoImage);
                        pagerAdapter.notifyDataSetChanged();
                        showMessage(getResources().getString(R.string.uploading_successed));
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, ">>>>>>     onFailure");
                        showMessage(getResources().getString(R.string.uploading_successed));
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                        pDialog.setTitleText("Loading");
                        pDialog.setCancelable(false);
                        pDialog.show();
                    }
                });
            }
            else if(what_album_flag == ALBUM) {
                StorageReference storageRef = storage.getReferenceFromUrl(getResources().getString(R.string.firebase_storage))
                        .child(getResources().getString(R.string.storage_albumimages_folder) + "/" + filename);
                storageRef.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        pDialog.hide();
                        groupReference = groupReference.child(groupId)
                                .child("memoryPhoto").child(albumId).child("mainImgPath");
                        groupReference.setValue(filename);
                        showMessage(getResources().getString(R.string.uploading_successed));
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, ">>>>>>     onFailure");
                        showMessage(getResources().getString(R.string.uploading_successed));
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
                        pDialog.setTitleText("Loading");
                        pDialog.setCancelable(false);
                        pDialog.show();
                    }
                });
            }

        } else {
            Log.i(TAG, ">>>>>>      filePath is null");
        }
    }



    // Request Permission
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, ">>>>>리퀘스트 요청 Success     " + grantResults[0]);

                } else {
                    Log.d(TAG, ">>>>>리퀘스트 요청 Fail     " + grantResults[0]);
                }
                return;
            }
            // other case
        }
    }
    private void showMessage(String msg) {
        ViewGroup container = (ViewGroup) findViewById(R.id.snackbar_layout);
        Snackbar.make(container, msg, Snackbar.LENGTH_SHORT).show();
    }
}
