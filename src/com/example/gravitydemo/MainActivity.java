package com.example.gravitydemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private GravityView gravityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameLayout container = (FrameLayout) findViewById(R.id.container);
        gravityView = new GravityView(this);
        container.addView(gravityView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gravityView.start();
    }

    @Override
    protected void onPause() {
        gravityView.stop();
        super.onPause();
    }
}
