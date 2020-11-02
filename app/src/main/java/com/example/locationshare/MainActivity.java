package com.example.locationshare;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends FragmentActivity implements OnClickListener {

    private FragmentA fragmentA;
    private FragmentB fragmentB;
    private FragmentTransaction fragmentTransaction;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.activity_main);

        initView();
        initFragment();

    }

    private void initFragment() {
        fragmentA = new FragmentA();
        fragmentB = new FragmentB();
        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.content_container, fragmentA, fragmentA.getClass().getName());
        fragmentTransaction.commit();
    }

    private void initView() {
        findViewById(R.id.tab1).setOnClickListener(this);
        findViewById(R.id.tab2).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tab1 :
                replaceFragment(fragmentA);
                break;
            case R.id.tab2 :
                replaceFragment(fragmentB);
                break;
    }
}

    private void replaceFragment(Fragment fragment) {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom);
        fragmentTransaction.replace(R.id.content_container, fragment, fragment.getClass().getName());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
//    private void hideFragment(FragmentTransaction fragmentTransaction) {
//        if(fragmentA != null){
//            fragmentTransaction.hide(fragmentA);
//        }
//        if(fragmentB != null){
//            fragmentTransaction.hide(fragmentB);
//        }
//    }

}