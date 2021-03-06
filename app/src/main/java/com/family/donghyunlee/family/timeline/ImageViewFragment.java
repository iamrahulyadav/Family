package com.family.donghyunlee.family.timeline;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.family.donghyunlee.family.R;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by DONGHYUNLEE on 2017-08-18.
 */

public class ImageViewFragment extends Fragment {

    private static final String TAG = ImageViewFragment.class.getSimpleName();
    @BindView(R.id.imageview_image)
    ImageView imageViewImage;


    private Uri uri;
    private String filePath;
    public static ImageViewFragment newInstance(String filePath) {

        ImageViewFragment imageViewFragment = new ImageViewFragment();
        Bundle args = new Bundle();
        args.putString("filePath", filePath);
        imageViewFragment.setArguments(args);

        return imageViewFragment;
    }

    private OnImageViewFragmentListener mOnFragmentListener;

    public interface OnImageViewFragmentListener{
        void onReceivedData(Uri uri);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getActivity() != null && getActivity() instanceof OnImageViewFragmentListener){
            mOnFragmentListener = (OnImageViewFragmentListener) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_imageview, container, false);
        ButterKnife.bind(this, v);
        filePath = getArguments().getString("filePath");
        uri = Uri.fromFile(new File(filePath));
        if(mOnFragmentListener != null){
            mOnFragmentListener.onReceivedData(uri);
        }
        Glide.with(getActivity()).load(uri).centerCrop().crossFade().into(imageViewImage);

        return v;
    }

    @OnClick(R.id.imageview_delete)
    void onDelteClick(){
        ((Comment) getActivity()).setIsChecked(0); // 0은 프래그먼트가 죽을 떄
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction().remove(ImageViewFragment.this).commit();
        fragmentManager.popBackStack();
    }



}
