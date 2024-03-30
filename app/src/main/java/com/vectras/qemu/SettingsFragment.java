package com.vectras.qemu;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.vectras.qemu.utils.Machine;
import com.vectras.vm.R;
import com.vectras.vm.SplashActivity;

public class SettingsFragment extends PreferenceFragmentCompat {

    private Handler mHandler;
    public SharedPreferences mPref;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        SharedPreferences.OnSharedPreferenceChangeListener listener;
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                switch (key) {

                    case "modeNight":
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent startActivity = new Intent(getContext(), SplashActivity.class);
                                int pendingIntentId = 123456;
                                PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                                AlarmManager mgr = (AlarmManager) MainSettingsManager.activity.getSystemService(Context.ALARM_SERVICE);
                                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);

                                System.exit(0);
                            }
                        }, 300);

                        getActivity().finish();
                        break;
                    case "customMemory":
                        if (prefs.getBoolean("customMemory", false))
                            findPreference("memory").setEnabled(true);
                        else
                            findPreference("memory").setEnabled(false);
                        break;
                    case "MTTCG":
                        if (prefs.getBoolean("MTTCG", false)) {
                            findPreference("cpuNum").setEnabled(false);
                            MainSettingsManager.setCpuCores(getContext(), 1);
                        } else {
                            findPreference("cpuNum").setEnabled(true);
                        }
                        break;
                }
            }
        };

        mPref = getPreferenceManager().getDefaultSharedPreferences(getContext());
        if (mPref != null) {
            mPref.registerOnSharedPreferenceChangeListener(listener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPref.getBoolean("customMemory", false))
            findPreference("memory").setEnabled(true);
        else
            findPreference("memory").setEnabled(false);

        if (mPref.getBoolean("MTTCG", false)) {
            findPreference("cpuNum").setEnabled(false);
        } else {
            findPreference("cpuNum").setEnabled(true);
        }
    }
}
