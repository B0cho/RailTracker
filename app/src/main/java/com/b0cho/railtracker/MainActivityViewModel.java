package com.b0cho.railtracker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainActivityViewModel extends AndroidViewModel {
    @Inject
    public MainActivityViewModel(
            @NonNull Application application) {
        super(application);
    }
}
