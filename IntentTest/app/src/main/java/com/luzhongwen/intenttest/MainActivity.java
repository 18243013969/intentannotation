package com.luzhongwen.intenttest;

import android.content.Intent;
import android.os.Bundle;

import com.luzhongwen.intentannotation.Optional;
import com.luzhongwen.intentannotation.Required;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Required
    private String name;

    @Optional
    private int set;

    @Required
    private  ArrayList<CharSequence> sudents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivityBuilder builder = new MainActivityBuilder();
        builder.setName("")
                .setSet(10)
                .setSudents(new ArrayList<>())
                .startMainActivity(this);
    }
}
