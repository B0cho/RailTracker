package com.b0cho.railtracker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainActivityVM extends AndroidViewModel {
    @Inject
    public MainActivityVM(
            @NonNull Application application) {
        super(application);
    }
}
