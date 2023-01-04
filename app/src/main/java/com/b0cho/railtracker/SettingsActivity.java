package com.b0cho.railtracker;


import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settingsFrameLayout, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @AndroidEntryPoint
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Inject
        ILocationProvider appLocationProvider;

        @ClearUserdataDialogBuilder
        @Inject
        AlertDialog.Builder clearUserdataDialogBuilder;

        @Override
        public void onViewCreated(@NonNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // adding listeners for clear data dialog
            // on confirm - clear application data
            clearUserdataDialogBuilder.setPositiveButton(getString(R.string.dialog_confirm_button), (dialogInterface, i) -> {
                // TODO: add listener

            });

            // on cancel - close dialog
            clearUserdataDialogBuilder.setNegativeButton(getString(R.string.dialog_cancel_button), (dialogInterface, i) -> dialogInterface.cancel());
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // setting listener for clear cache listener click -> open confirm/cancel dialog
            Preference clearUserdataPreference = Objects.requireNonNull(getPreferenceScreen().findPreference(getString(R.string.clear_userdata_key)));
            clearUserdataPreference.setOnPreferenceClickListener(preference -> {
                clearUserdataDialogBuilder.create().show();
                return true;
            });

            // Location timeout expiration
            Preference locationTimeoutPreference = Objects.requireNonNull(getPreferenceScreen().findPreference(getString(R.string.location_timeout_key)));
            locationTimeoutPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                final long longValue = Long.parseLong(newValue.toString());
                assert longValue > 0;
                appLocationProvider.setLocationTimeoutSec(longValue);
                return true;
            });
        }

        @Override
        public void onResume() {
            super.onResume();

            // disable location settings category if location permission is not granted
            PreferenceCategory locationGPSCat = Objects.requireNonNull(getPreferenceScreen().findPreference(getString(R.string.location_cat_key)));
            locationGPSCat.setEnabled(appLocationProvider.checkPermissions(requireContext().getApplicationContext()));
        }
    }
}