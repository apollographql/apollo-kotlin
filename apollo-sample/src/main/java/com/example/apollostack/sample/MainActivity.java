package com.example.apollostack.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import generatedIR.DroidDetails;

public class MainActivity extends AppCompatActivity {
  private final DroidDetails droidDetails = new DroidDetails();

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }
}
