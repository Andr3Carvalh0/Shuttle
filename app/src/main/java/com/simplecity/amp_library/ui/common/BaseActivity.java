package com.simplecity.amp_library.ui.common;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;
import com.afollestad.aesthetic.AestheticActivity;
import com.greysonparrelli.permiso.Permiso;
import com.simplecity.amp_library.playback.constants.InternalIntents;
import com.simplecity.amp_library.utils.MusicServiceConnectionUtils;
import com.simplecity.amp_library.utils.SettingsManager;
import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import javax.inject.Inject;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

public abstract class BaseActivity extends AestheticActivity implements
        HasSupportFragmentInjector,
        ServiceConnection {

    @Nullable
    private MusicServiceConnectionUtils.ServiceToken token;

    @Inject
    DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Inject
    SettingsManager settingsManager;

    @CallSuper
    protected void onCreate(final Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        Permiso.getInstance().setActivity(this);

        Permiso.getInstance().requestPermissions(new Permiso.IOnPermissionResult() {
            @Override
            public void onPermissionResult(Permiso.ResultSet resultSet) {
                if (resultSet.areAllPermissionsGranted()) {
                    bindService();
                } else {
                    Toast.makeText(BaseActivity.this, "Permission check failed", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onRationaleRequested(Permiso.IOnRationaleProvided callback, String... permissions) {
                callback.onRationaleProvided();
            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        keepScreenOn(settingsManager.keepScreenOn());
        super.onResume();

        if (token == null) {
            bindService();
        }

        Permiso.getInstance().setActivity(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        unbindService();
        super.onDestroy();
    }

    @Override
    public final AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentInjector;
    }

    void bindService() {
        MusicServiceConnectionUtils.bindToService(getLifecycle(), this, this, serviceToken -> token = serviceToken);
    }

    void unbindService() {
        if (token != null) {
            MusicServiceConnectionUtils.unbindFromService(token);
            token = null;
        }
    }

    @Override
    @CallSuper
    public void onServiceConnected(ComponentName name, IBinder service) {
        sendBroadcast(new Intent(InternalIntents.SERVICE_CONNECTED));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        unbindService();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Fix for issue on LG devices
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //Fix for issue on LG devices
        if (keyCode == KeyEvent.KEYCODE_MENU && "LGE".equalsIgnoreCase(Build.BRAND)) {
            openOptionsMenu();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void keepScreenOn(boolean on) {
        final Window window = getWindow();
        if (on) {
            window.addFlags(FLAG_KEEP_SCREEN_ON);
        } else {
            window.clearFlags(FLAG_KEEP_SCREEN_ON);
        }
    }

    protected abstract String screenName();
}