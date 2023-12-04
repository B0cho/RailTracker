package com.b0cho.railtracker.di;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.b0cho.railtracker.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Qualifier;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.FragmentComponent;
import dagger.hilt.android.qualifiers.ActivityContext;
import dagger.hilt.android.scopes.FragmentScoped;

@Module
@InstallIn(FragmentComponent.class)
public class SettingsActivityModule {
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ClearUserdataDialogBuilder {}

    @ClearUserdataDialogBuilder
    @Provides
    @FragmentScoped
    public AlertDialog.Builder clearUserDataDialogBuilder(@ActivityContext Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.clear_userdata_title));
        builder.setMessage(context.getString(R.string.clear_userdata_dialog_message));
        return builder;
    }
}
