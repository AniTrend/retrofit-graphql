package io.github.wax911.retgraph.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;

import io.github.wax911.retgraph.R;

public class AdvancedExampleActivity extends AppCompatActivity {

    private FrameLayout frameLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        bindViews();
    }

    private void bindViews() {
        frameLayout = findViewById(R.id.frame_content);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
