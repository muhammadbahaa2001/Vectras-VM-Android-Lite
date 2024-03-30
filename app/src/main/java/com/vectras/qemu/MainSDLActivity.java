package com.vectras.qemu;

import android.annotation.SuppressLint;
import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.vectras.qemu.utils.FileUtils;
import com.vectras.qemu.utils.Machine;
import com.vectras.qemu.utils.QmpClient;
import com.vectras.vm.Fragment.ControlersOptionsFragment;
import com.vectras.vm.Fragment.LoggerDialogFragment;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.adapter.LogsAdapter;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.widgets.JoystickView;

import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import org.json.JSONObject;
import org.libsdl.app.SDLActivity;
import org.libsdl.app.SDLControllerManager;
import org.libsdl.app.SDLSurface;

/**
 * SDL Activity
 */
public class MainSDLActivity extends SDLActivity {
    public static final String TAG = "MainSDLActivity";

    public static MainSDLActivity activity;

    public static final int KEYBOARD = 10000;
    public static final int QUIT = 10001;
    public static final int HELP = 10002;

    private boolean monitorMode = false;
    private boolean mouseOn = false;
    private Object lockTime = new Object();
    private boolean timeQuit = false;
    private boolean once = true;
    private boolean zoomable = false;
    private String status = null;

    public static int vm_width;
    public static int vm_height;


    private Thread timeListenerThread;

    private ProgressDialog progDialog;
    protected static ViewGroup mMainLayout;

    public String cd_iso_path = null;

    // HDD
    public String hda_img_path = null;
    public String hdb_img_path = null;
    public String hdc_img_path = null;
    public String hdd_img_path = null;

    public String fda_img_path = null;
    public String fdb_img_path = null;
    public String cpu = null;

    // Default Settings
    public int memory = 128;
    public String bootdevice = null;

    // net
    public String net_cfg = "None";
    public int nic_num = 1;
    public String vga_type = "std";
    public String hd_cache = "default";
    public String nic_driver = null;
    public String soundcard = null;
    public String lib = "liblimbo.so";
    public String lib_path = null;
    public String snapshot_name = "limbo";
    public int disableacpi = 0;
    public int disablehpet = 0;
    public int disabletsc = 0;
    public int enableqmp = 0;
    public int enablevnc = 0;
    public String vnc_passwd = null;
    public int vnc_allow_external = 0;
    public String qemu_dev = null;
    public String qemu_dev_value = null;

    public String dns_addr = null;
    public int restart = 0;

    // This is what SDL runs in. It invokes SDL_main(), eventually
    private static Thread mSDLThread;

    // EGL private objects
    private static EGLContext mEGLContext;
    private static EGLSurface mEGLSurface;
    private static EGLDisplay mEGLDisplay;
    private static EGLConfig mEGLConfig;
    private static int mGLMajor, mGLMinor;


    private static Activity activity1;

    // public static void showTextInput(int x, int y, int w, int h) {
    // // Transfer the task to the main thread as a Runnable
    // // mSingleton.commandHandler.post(new ShowTextInputHandler(x, y, w, h));
    // }

    public static void singleClick(final MotionEvent event, final int pointer_id) {

        Thread t = new Thread(new Runnable() {
            public void run() {
                // Log.d("SDL", "Mouse Single Click");
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    // Log.v("singletap", "Could not sleep");
                }
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_DOWN, 1, 0, 0);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    // Log.v("singletap", "Could not sleep");
                }
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 1, 0, 0);
            }
        });
        t.start();
    }

    public void setParams() {

        if (MainActivity.vmexecutor == null) {
            return;
        }
        memory = MainActivity.vmexecutor.memory;
        vga_type = MainActivity.vmexecutor.vga_type;
        hd_cache = MainActivity.vmexecutor.hd_cache;
        snapshot_name = MainActivity.vmexecutor.snapshot_name;
        disableacpi = MainActivity.vmexecutor.disableacpi;
        disablehpet = MainActivity.vmexecutor.disablehpet;
        disabletsc = MainActivity.vmexecutor.disabletsc;
        enableqmp = MainActivity.vmexecutor.enableqmp;
        enablevnc = MainActivity.vmexecutor.enablevnc;

        if (MainActivity.vmexecutor.cpu.endsWith("(64Bit)")) {
            cpu = MainActivity.vmexecutor.cpu.split(" ")[0];
        } else {
            cpu = MainActivity.vmexecutor.cpu;
        }

        if (MainActivity.vmexecutor.cd_iso_path == null || MainActivity.vmexecutor.cd_iso_path.equals("None")) {
            cd_iso_path = null;
        } else {
            cd_iso_path = MainActivity.vmexecutor.cd_iso_path;
        }
        if (MainActivity.vmexecutor.hda_img_path == null || MainActivity.vmexecutor.hda_img_path.equals("None")) {
            hda_img_path = null;
        } else {
            hda_img_path = MainActivity.vmexecutor.hda_img_path;
        }

        if (MainActivity.vmexecutor.hdb_img_path == null || MainActivity.vmexecutor.hdb_img_path.equals("None")) {
            hdb_img_path = null;
        } else {
            hdb_img_path = MainActivity.vmexecutor.hdb_img_path;
        }

        if (MainActivity.vmexecutor.hdc_img_path == null || MainActivity.vmexecutor.hdc_img_path.equals("None")) {
            hdc_img_path = null;
        } else {
            hdc_img_path = MainActivity.vmexecutor.hdc_img_path;
        }

        if (MainActivity.vmexecutor.hdd_img_path == null || MainActivity.vmexecutor.hdd_img_path.equals("None")) {
            hdd_img_path = null;
        } else {
            hdd_img_path = MainActivity.vmexecutor.hdd_img_path;
        }

        if (MainActivity.vmexecutor.fda_img_path == null || MainActivity.vmexecutor.fda_img_path.equals("None")) {
            fda_img_path = null;
        } else {
            fda_img_path = MainActivity.vmexecutor.fda_img_path;
        }

        if (MainActivity.vmexecutor.fdb_img_path == null || MainActivity.vmexecutor.fdb_img_path.equals("None")) {
            fdb_img_path = null;
        } else {
            fdb_img_path = MainActivity.vmexecutor.fdb_img_path;
        }
        if (MainActivity.vmexecutor.bootdevice == null) {
            bootdevice = null;
        } else if (MainActivity.vmexecutor.bootdevice.equals("Default")) {
            bootdevice = null;
        } else if (MainActivity.vmexecutor.bootdevice.equals("CD Rom")) {
            bootdevice = "d";
        } else if (MainActivity.vmexecutor.bootdevice.equals("Floppy")) {
            bootdevice = "a";
        } else if (MainActivity.vmexecutor.bootdevice.equals("Hard Disk")) {
            bootdevice = "c";
        }

        if (MainActivity.vmexecutor.net_cfg == null || MainActivity.vmexecutor.net_cfg.equals("None")) {
            net_cfg = "none";
            nic_driver = null;
        } else if (MainActivity.vmexecutor.net_cfg.equals("User")) {
            net_cfg = "user";
            nic_driver = MainActivity.vmexecutor.nic_card;
        }

        soundcard = MainActivity.vmexecutor.sound_card;

    }

    public static void delayKey(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void sendCtrlAltKey(int code) {
        delayKey(100);
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
        delayKey(100);
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ALT_LEFT);
        delayKey(100);
        if (code >= 0) {
            SDLActivity.onNativeKeyDown(code);
            delayKey(100);
            SDLActivity.onNativeKeyUp(code);
            delayKey(100);
        }
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
        delayKey(100);
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
    }

    public void stopTimeListener() {
        Log.v("SaveVM", "Stopping Listener");
        synchronized (this.lockTime) {
            this.timeQuit = true;
            this.lockTime.notifyAll();
        }
    }

    public void onDestroy() {

        // Now wait for the SDL thread to quit
        Log.v("MainSDL", "Waiting for SDL thread to quit");
        if (mSDLThread != null) {
            try {
                mSDLThread.join();
            } catch (Exception e) {
                Log.v("SDL", "Problem stopping thread: " + e);
            }
            mSDLThread = null;

            Log.v("SDL", "Finished waiting for SDL thread");
        }
        this.stopTimeListener();

        MainActivity.vmexecutor.doStopVM(0);
        super.onDestroy();
    }


    public void checkStatus() {
        while (timeQuit != true) {
            MainActivity.VMStatus status = Machine.checkSaveVMStatus(activity);
            Log.v(TAG, "Status: " + status);
            if (status == MainActivity.VMStatus.Unknown
                    || status == MainActivity.VMStatus.Completed
                    || status == MainActivity.VMStatus.Failed
            ) {
                Log.v("Inside", "Saving state is done: " + status);
                stopTimeListener();
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Log.w("SaveVM", "Interrupted");
            }
        }
        Log.v("SaveVM", "Save state complete");

    }


    public void startTimeListener() {
        this.stopTimeListener();
        timeQuit = false;
        try {
            Log.v("Listener", "Time Listener Started...");
            checkStatus();
            synchronized (lockTime) {
                while (timeQuit == false) {
                    lockTime.wait();
                }
                lockTime.notifyAll();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.v("SaveVM", "Time listener thread error: " + ex.getMessage());
        }
        Log.v("Listener", "Time listener thread exited...");

    }

    public static boolean toggleKeyboardFlag = true;

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Log.v("Limbo", "Inside Options Check");
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.itemReset) {
            Machine.resetVM(activity);
        } else if (item.getItemId() == R.id.itemShutdown) {
            UIUtils.hideKeyboard(this, mSurface);
            Machine.stopVM(activity);
        } else if (item.getItemId() == R.id.itemMouse) {
            onMouseMode();
        } else if (item.getItemId() == this.KEYBOARD || item.getItemId() == R.id.itemKeyboard) {
            //XXX: need to post after delay to work correctly
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    toggleKeyboardFlag = UIUtils.onKeyboard(activity, toggleKeyboardFlag, mSurface);
                }
            }, 200);

        } else if (item.getItemId() == R.id.itemMonitor) {
            if (this.monitorMode) {
                this.onVMConsole();
            } else {
                this.onMonitor();
            }
        } else if (item.getItemId() == R.id.itemVolume) {
            this.onSelectMenuVol();
        } else if (item.getItemId() == R.id.itemSaveState) {
            this.promptPause(activity);
        } else if (item.getItemId() == R.id.itemSaveSnapshot) {
            //TODO:
            //this.promptStateName(activity);
        } else if (item.getItemId() == R.id.itemFitToScreen) {
            onFitToScreen();
        } else if (item.getItemId() == R.id.itemStretchToScreen) {
            onStretchToScreen();
        } else if (item.getItemId() == R.id.itemZoomIn) {
            this.setZoomIn();
        } else if (item.getItemId() == R.id.itemZoomOut) {
            this.setZoomOut();
        } else if (item.getItemId() == R.id.itemCtrlAltDel) {
            this.onCtrlAltDel();
        } else if (item.getItemId() == R.id.itemCtrlC) {
            this.onCtrlC();
        } else if (item.getItemId() == R.id.itemOneToOne) {
            this.onNormalScreen();
        } else if (item.getItemId() == R.id.itemZoomable) {
            this.setZoomable();
        } else if (item.getItemId() == this.QUIT) {
        } else if (item.getItemId() == R.id.itemHelp) {

        } else if (item.getItemId() == R.id.itemHideToolbar) {
            this.onHideToolbar();
        } else if (item.getItemId() == R.id.itemDisplay) {
            this.onSelectMenuSDLDisplay();
        } else if (item.getItemId() == R.id.itemViewLog) {
            this.onViewLog();
        }
        // this.canvas.requestFocus();


        this.invalidateOptionsMenu();
        return true;
    }

    public void onViewLog() {
        FileUtils.viewVectrasLog(this);
    }

    public void onHideToolbar() {
        ActionBar bar = this.getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
    }


    public void onMouseMode() {

        String[] items = {"Trackpad Mouse (Phone)",
                "Bluetooth/USB Mouse (Desktop mode)", //Physical mouse for Chromebook, Android x86 PC, or Bluetooth Mouse
        };
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this, R.style.MainDialogTheme);
        mBuilder.setTitle("Mouse Mode");
        mBuilder.setSingleChoiceItems(items, Config.mouseMode.ordinal(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case 0:
                        setUIModeMobile(true);
                        break;
                    case 1:
                        promptSetUIModeDesktop(false);
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = mBuilder.create();
        alertDialog.show();

    }

    public boolean checkVMResolutionFits() {
        int width = mLayout.getWidth();
        int height = mLayout.getHeight();
        ActionBar bar = activity.getSupportActionBar();

        if (!MainSettingsManager.getAlwaysShowMenuToolbar(MainSDLActivity.this)
                && bar != null && bar.isShowing()) {
            height += bar.getHeight();
        }

        if (vm_width < width && vm_height < height)
            return true;

        return false;
    }

    public void calibration() {
        //XXX: No need to calibrate for SDL trackpad.
    }

    private void setUIModeMobile(boolean fitToScreen) {

        try {
            UIUtils.setOrientation(this);
            MotionEvent a = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);

            //TODO: needed?
            //MainSDLActivity.singleClick(a, 0);
            Config.mouseMode = Config.MouseMode.Trackpad;
            MainSettingsManager.setDesktopMode(this, false);
            MainActivity.vmexecutor.setRelativeMouseMode(1);
            if (Config.showToast)
                UIUtils.toastShort(this.getApplicationContext(), "Trackpad Enabled");
            if (fitToScreen)
                onFitToScreen();
            else
                onNormalScreen();
            calibration();
            invalidateOptionsMenu();
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }

    }

    private void promptSetUIModeDesktop(final boolean mouseMethodAlt) {


        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Desktop Mode");
        String desktopInstructions = this.getString(R.string.desktopInstructions);
        if (!checkVMResolutionFits()) {
            String resolutionWarning = "Warning: MainActivity.vmexecutor resolution "
                    + vm_width + "x" + vm_height +
                    " is too high for Desktop Mode. " +
                    "Scaling will be used and Mouse Alignment will not be accurate. " +
                    "Reduce display resolution within the Guest OS for better experience.\n\n";
            desktopInstructions = resolutionWarning + desktopInstructions;
        }
        alertDialog.setMessage(desktopInstructions);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setUIModeDesktop();
                alertDialog.dismiss();
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();

    }

    private void setUIModeDesktop() {

        try {
            MotionEvent a = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0, 0, 0);

            //TODO: needed?
            //MainSDLActivity.singleClick(a, 0);

            //TODO: not needed?
            //SDLActivity.onNativeMouseReset(0, 0, MotionEvent.ACTION_MOVE, 0, 0, 0);
            //SDLActivity.onNativeMouseReset(0, 0, MotionEvent.ACTION_MOVE, vm_width, vm_height, 0);

            Config.mouseMode = Config.MouseMode.External;
            MainSettingsManager.setDesktopMode(this, true);
            MainActivity.vmexecutor.setRelativeMouseMode(0);
            if (Config.showToast)
                UIUtils.toastShort(MainSDLActivity.this, "External Mouse Enabled");
            onNormalScreen();
            calibration();
            invalidateOptionsMenu();
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }
    }

    private void onCtrlAltDel() {

        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_RIGHT);
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ALT_RIGHT);
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_FORWARD_DEL);
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_FORWARD_DEL);
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ALT_RIGHT);
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_RIGHT);
    }

    private void onCtrlC() {

        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_RIGHT);
        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_C);
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_C);
        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_RIGHT);
    }


    //TODO: not working
    private void onStretchToScreen() {


        new Thread(new Runnable() {
            public void run() {
                Log.d(TAG, "onStretchToScreen");
                screenMode = SDLScreenMode.Fullscreen;
                sendCtrlAltKey(KeyEvent.KEYCODE_F); // not working
                if (Config.showToast)
                    UIUtils.toastShort(activity, "Resizing, Please Wait");
                resize(null);

            }
        }).start();

    }

    private void onFitToScreen() {
        try {
            UIUtils.setOrientation(this);
            ActionBar bar = MainSDLActivity.this.getSupportActionBar();
            if (bar != null && !MainSettingsManager.getAlwaysShowMenuToolbar(this)) {
                bar.hide();
            }
            new Thread(new Runnable() {
                public void run() {
                    Log.d(TAG, "onFitToScreen");
                    screenMode = SDLScreenMode.FitToScreen;
                    if (Config.showToast)
                        UIUtils.toastShort(activity, "Resizing, Please Wait");
                    resize(null);

                }
            }).start();
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }

    }

    private void onNormalScreen() {
        try {
            ActionBar bar = MainSDLActivity.this.getSupportActionBar();
            if (bar != null && !MainSettingsManager.getAlwaysShowMenuToolbar(this)) {
                bar.hide();
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            new Thread(new Runnable() {
                public void run() {
                    Log.d(TAG, "onNormalScreen");
                    screenMode = SDLScreenMode.Normal;
                    if (Config.showToast)
                        UIUtils.toastShort(activity, "Resizing, Please Wait");
                    resize(null);

                }
            }).start();
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }

    }

    public void resize(final Configuration newConfig) {

        //XXX: flag so no mouse events are processed
        isResizing = true;

        //XXX: This is needed so Nougat+ devices will update their layout
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ((MainSDLSurface) mSurface).getHolder().setFixedSize(1, 1);
                setLayout(newConfig);

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((MainSDLSurface) mSurface).doResize(false, newConfig);
                    }
                }, 1000);
            }
        });

    }

    private void setZoomIn() {

        new Thread(new Runnable() {
            public void run() {
                screenMode = SDLScreenMode.Normal;
                sendCtrlAltKey(KeyEvent.KEYCODE_4);
            }
        }).start();

    }

    private void setZoomOut() {


        new Thread(new Runnable() {
            public void run() {
                screenMode = SDLScreenMode.Normal;
                sendCtrlAltKey(KeyEvent.KEYCODE_3);

            }
        }).start();

    }

    private void setZoomable() {

        zoomable = true;

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        return this.setupMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        return this.setupMenu(menu);
    }

    public boolean setupMenu(Menu menu) {
        // Log.v("Limbo", "Inside Options Created");
        getMenuInflater().inflate(R.menu.sdlactivitymenu, menu);

        int maxMenuItemsShown = 4;
        int actionShow = MenuItemCompat.SHOW_AS_ACTION_IF_ROOM;
        if (UIUtils.isLandscapeOrientation(this)) {
            maxMenuItemsShown = 6;
            actionShow = MenuItemCompat.SHOW_AS_ACTION_ALWAYS;
        }

        // if (vncCanvas.scaling != null) {
        // menu.findItem(vncCanvas.scaling.getId()).setChecked(true);
        // }

        // Remove snapshots for now
        menu.removeItem(menu.findItem(R.id.itemSaveSnapshot).getItemId());

        // Remove Monitor console for SDL2 it creates 2 SDL windows and SDL for
        // android supports only 1
        menu.removeItem(menu.findItem(R.id.itemMonitor).getItemId());

        // Remove scaling for now
        menu.removeItem(menu.findItem(R.id.itemScaling).getItemId());

        // Remove external mouse for now
        menu.removeItem(menu.findItem(R.id.itemExternalMouse).getItemId());
        //menu.removeItem(menu.findItem(R.id.itemUIMode).getItemId());

        menu.removeItem(menu.findItem(R.id.itemCtrlAltDel).getItemId());
        menu.removeItem(menu.findItem(R.id.itemCtrlC).getItemId());

        if (MainSettingsManager.getAlwaysShowMenuToolbar(activity) || Config.mouseMode == Config.MouseMode.External) {
            menu.removeItem(menu.findItem(R.id.itemHideToolbar).getItemId());
            maxMenuItemsShown--;
        }

        if (soundcard == null || soundcard.equals("None")) {
            menu.removeItem(menu.findItem(R.id.itemVolume).getItemId());
            maxMenuItemsShown--;
        }


        for (int i = 0; i < menu.size() && i < maxMenuItemsShown; i++) {
            MenuItemCompat.setShowAsAction(menu.getItem(i), actionShow);
        }

        return true;

    }

    private void onMonitor() {
        new Thread(new Runnable() {
            public void run() {
                monitorMode = true;
                // final KeyEvent altDown = new KeyEvent(downTime, eventTime,
                // KeyEvent.ACTION_DOWN,
                // KeyEvent.KEYCODE_2, 1, KeyEvent.META_ALT_LEFT_ON);
                sendCtrlAltKey(KeyEvent.KEYCODE_2);
                // sendCtrlAltKey(altDown);
                Log.v("Limbo", "Monitor On");
            }
        }).start();

    }

    private void onVMConsole() {
        monitorMode = false;
        sendCtrlAltKey(KeyEvent.KEYCODE_1);
    }


    // FIXME: We need this to able to catch complex characters strings like
    // grave and send it as text
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE && event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN) {
            sendText(event.getCharacters().toString());
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            FrameLayout l = findViewById(R.id.mainControl);
            if (l.getVisibility() == View.VISIBLE) {
                l.setVisibility(View.GONE);
            } else {
                l.setVisibility(View.VISIBLE);
            }
            return true;
        } if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // We emulate right click with volume down
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
                rightClick(e, 0);
            }
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
            // We emulate left click with volume up
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
                leftClick(e, 0);
            }
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }

    }

    private static void sendText(String string) {

        // Log.v("sendText", string);
        KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        KeyEvent[] keyEvents = keyCharacterMap.getEvents(string.toCharArray());
        if (keyEvents != null)
            for (int i = 0; i < keyEvents.length; i++) {

                if (keyEvents[i].getAction() == KeyEvent.ACTION_DOWN) {
                    // Log.v("sendText", "Up: " + keyEvents[i].getKeyCode());
                    SDLActivity.onNativeKeyDown(keyEvents[i].getKeyCode());
                } else if (keyEvents[i].getAction() == KeyEvent.ACTION_UP) {
                    // Log.v("sendText", "Down: " + keyEvents[i].getKeyCode());
                    SDLActivity.onNativeKeyUp(keyEvents[i].getKeyCode());
                }
            }
    }


    String[] functionsArray = {"F1", "F2", "F3", "F4",
            "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"};

    View view;
    private final String CREDENTIAL_SHARED_PREF = "settings_prefs";
    private LogsAdapter mLogAdapter;
    private RecyclerView logList;
    private Timer _timer = new Timer();
    private TimerTask t;
    public boolean ctrlClicked = false;
    public boolean altClicked = false;
    public static LinearLayout desktop;
    public static LinearLayout gamepad;

    // Setup
    @SuppressLint("UseCompatLoadingForDrawables")
    protected void onCreate(Bundle savedInstanceState) {
        // Log.v("SDL", "onCreate()");
        activity = this;

        if (MainSettingsManager.getFullscreen(this))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setupVolume();

        mSingleton = this;

        Log.v("SDL", "Max Mem = " + Runtime.getRuntime().maxMemory());

        this.activity1 = this;

        // So we can call stuff from static callbacks
        mSingleton = this;

        createUI(0, 0);

        UIUtils.showHints(this);

        this.resumeVM();

        UIUtils.setOrientation(this);


        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        onFitToScreen();
        desktop = findViewById(R.id.desktop);
        gamepad = findViewById(R.id.gamepad);

        if (Objects.equals(MainSettingsManager.getControlMode(activity), "D")) {
            desktop.setVisibility(View.VISIBLE);
            gamepad.setVisibility(View.GONE);
        } else if (Objects.equals(MainSettingsManager.getControlMode(activity), "G")) {
            desktop.setVisibility(View.GONE);
            gamepad.setVisibility(View.VISIBLE);
        } else if (Objects.equals(MainSettingsManager.getControlMode(activity), "H")) {
            desktop.setVisibility(View.GONE);
            gamepad.setVisibility(View.GONE);
        }


        ImageButton shutdownBtn = findViewById(R.id.shutdownBtn);
        ImageButton settingBtn = findViewById(R.id.btnSettings);
        ImageButton keyboardBtn = findViewById(R.id.kbdBtn);
        ImageButton controllersBtn = findViewById(R.id.btnMode);
        ImageButton upBtn = findViewById(R.id.upBtn);
        ImageButton leftBtn = findViewById(R.id.leftBtn);
        ImageButton downBtn = findViewById(R.id.downBtn);
        ImageButton rightBtn = findViewById(R.id.rightBtn);
        ImageButton enterBtn = findViewById(R.id.enterBtn);
        ImageButton escBtn = findViewById(R.id.escBtn);
        ImageButton ctrlBtn = findViewById(R.id.ctrlBtn);
        ImageButton altBtn = findViewById(R.id.altBtn);
        ImageButton delBtn = findViewById(R.id.delBtn);
        ImageButton qmpBtn = findViewById(R.id.btnQmp);
        ImageButton btnLogs = findViewById(R.id.btnLogs);
        Button eBtn = findViewById(R.id.eBtn);
        Button rBtn = findViewById(R.id.rBtn);
        Button qBtn = findViewById(R.id.qBtn);
        Button xBtn = findViewById(R.id.xBtn);
        ImageButton ctrlGameBtn = findViewById(R.id.ctrlGameBtn);
        Button spaceBtn = findViewById(R.id.spaceBtn);
        Button tabGameBtn = findViewById(R.id.tabGameBtn);
        Button tabBtn = findViewById(R.id.tabBtn);
        ImageButton upGameBtn = findViewById(R.id.upGameBtn);
        ImageButton downGameBtn = findViewById(R.id.downGameBtn);
        ImageButton leftGameBtn = findViewById(R.id.leftGameBtn);
        ImageButton rightGameBtn = findViewById(R.id.rightGameBtn);
        ImageButton enterGameBtn = findViewById(R.id.enterGameBtn);
        upGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_UP);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        leftGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_LEFT);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_LEFT);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        downGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_DOWN);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        rightGameBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        int loop =150;
        JoystickView joystick = (JoystickView) findViewById(R.id.joyStick);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                // do whatever you want
                if (angle > 0) {
                    if (angle < 30) {
                        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
                        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT);
                    } else if (angle > 30) {
                        if (angle < 60) {
                            SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP_RIGHT);
                            SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_UP_RIGHT);
                        } else if (angle > 60) {
                            if (angle < 120) {
                                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP);
                                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_UP);
                            } else if (angle > 120) {
                                if (angle < 150) {
                                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP_LEFT);
                                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_UP_LEFT);
                                } else if (angle > 150) {
                                    if (angle < 210) {
                                        SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_LEFT);
                                        SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_LEFT);
                                    } else if (angle > 210) {
                                        if (angle < 240) {
                                            SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN_LEFT);
                                            SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_DOWN_LEFT);
                                        } else if (angle > 240) {
                                            if (angle < 300) {
                                                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN);
                                                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_DOWN);
                                            } else if (angle > 300) {
                                                if (angle < 330) {
                                                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT);
                                                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT);
                                                } else if (angle > 330) {
                                                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
                                                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, loop);
        tabBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_TAB);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_TAB);
            }
        });
        tabGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_TAB);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_TAB);
            }
        });
        eBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_E);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_E);
            }
        });
        rBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_R);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_R);
            }
        });
        qBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_Q);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_Q);
            }
        });
        xBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_X);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_X);
            }
        });
        ctrlGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
            }
        });
        spaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_SPACE);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_SPACE);
            }
        });
        btnLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                // Create and show the dialog.
                LoggerDialogFragment newFragment = new LoggerDialogFragment();
                newFragment.show(ft, "Logger");
            }
        });
        shutdownBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Machine.stopVM(activity);
            }
        });
        keyboardBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toggleKeyboardFlag = UIUtils.onKeyboard(activity, toggleKeyboardFlag, mSurface);
                    }
                }, 200);
            }
        });
        controllersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                // Create and show the dialog.
                ControlersOptionsFragment newFragment = new ControlersOptionsFragment();
                newFragment.show(ft, "Controllers");
            }
        });
        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog alertDialog = new Dialog(activity, R.style.MainDialogTheme);
                alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                alertDialog.setContentView(R.layout.dialog_setting);
                alertDialog.show();
            }
        });
        upBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_UP);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        leftBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_LEFT);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_LEFT);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        downBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_DOWN);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        rightBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
                    v.animate().scaleXBy(-0.2f).setDuration(200).start();
                    v.animate().scaleYBy(-0.2f).setDuration(200).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT);
                    v.animate().cancel();
                    v.animate().scaleX(1f).setDuration(500).start();
                    v.animate().scaleY(1f).setDuration(200).start();
                    return true;
                }
                return false;
            }
        });
        escBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE);
            }
        });
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ENTER);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ENTER);
            }
        });
        enterGameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ENTER);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ENTER);
            }
        });
        ctrlBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (!ctrlClicked) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
                    ctrlBtn.setBackground(getResources().getDrawable(R.drawable.controls_button2));
                    ctrlClicked = true;
                } else {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
                    ctrlBtn.setBackground(getResources().getDrawable(R.drawable.controls_button1));
                    ctrlClicked = false;
                }
            }
        });
        altBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (!altClicked) {
                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ALT_LEFT);
                    altBtn.setBackground(getResources().getDrawable(R.drawable.controls_button2));
                    altClicked = true;
                } else {
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ALT_LEFT);
                    altBtn.setBackground(getResources().getDrawable(R.drawable.controls_button1));
                    altClicked = false;
                }
            }
        });
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DEL);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_DEL);
            }
        });
        qmpBtn.setVisibility(View.GONE);
        if (monitorMode) {
            qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_terminal_24));
        } else {
            qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_computer_24));
        }
        Button rightClickBtn = findViewById(R.id.rightClickBtn);
        Button middleClickBtn = findViewById(R.id.middleBtn);
        Button leftClickBtn = findViewById(R.id.leftClickBtn);
        ImageButton winBtn = findViewById(R.id.winBtn);

        rightClickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
                rightClick(e, 0);
            }
        });
        middleClickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
                middleClick(e, 0);
            }
        });
        leftClickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);
                leftClick(e, 0);
            }
        });
        winBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
                delayKey(100);
                SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE);
                delayKey(100);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
                SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_ESCAPE);
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.container_function, functionsArray);

        ListView listView = findViewById(R.id.functions);
        listView.setAdapter(adapter);
        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    keyDownUp(KeyEvent.KEYCODE_F1);
                } else if (position == 1) {
                    keyDownUp(KeyEvent.KEYCODE_F2);
                } else if (position == 2) {
                    keyDownUp(KeyEvent.KEYCODE_F3);
                } else if (position == 3) {
                    keyDownUp(KeyEvent.KEYCODE_F4);
                } else if (position == 4) {
                    keyDownUp(KeyEvent.KEYCODE_F5);
                } else if (position == 5) {
                    keyDownUp(KeyEvent.KEYCODE_F6);
                } else if (position == 6) {
                    keyDownUp(KeyEvent.KEYCODE_F7);
                } else if (position == 7) {
                    keyDownUp(KeyEvent.KEYCODE_F8);
                } else if (position == 8) {
                    keyDownUp(KeyEvent.KEYCODE_F9);
                } else if (position == 9) {
                    keyDownUp(KeyEvent.KEYCODE_F10);
                } else if (position == 10) {
                    keyDownUp(KeyEvent.KEYCODE_F11);
                } else if (position == 11) {
                    keyDownUp(KeyEvent.KEYCODE_F12);
                }
            }
        });*/

    }


    private void createUI(int w, int h) {
        mSurface = new MainSDLSurface(this);

        int width = w;
        int height = h;
        if (width == 0) {
            width = RelativeLayout.LayoutParams.WRAP_CONTENT;
        }
        if (height == 0) {
            height = RelativeLayout.LayoutParams.WRAP_CONTENT;
        }

        setContentView(R.layout.activity_sdl);

        //TODO:
        mLayout = (RelativeLayout) activity.findViewById(R.id.sdl_layout);
        mMainLayout = (LinearLayout) activity.findViewById(R.id.main_layout);

        RelativeLayout mLayout = (RelativeLayout) findViewById(R.id.sdl);
        RelativeLayout.LayoutParams surfaceParams = new RelativeLayout.LayoutParams(width, height);
        surfaceParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        mLayout.addView(mSurface, surfaceParams);

    }

    protected void onPause() {
        Log.v("SDL", "onPause()");
        MainService.updateServiceNotification("Vectras VM Suspended");
        super.onPause();

    }


    public void onSelectMenuVol() {

        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Volume");

        LinearLayout.LayoutParams volParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout t = createVolumePanel();
        t.setLayoutParams(volParams);

        ScrollView s = new ScrollView(activity);
        s.addView(t);
        alertDialog.setView(s);
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });
        alertDialog.show();

    }

    public LinearLayout createVolumePanel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams volparams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

        SeekBar vol = new SeekBar(this);

        int volume = 0;

        //TODO:
        vol.setMax(maxVolume);
        volume = getCurrentVolume();

        vol.setProgress(volume);
        vol.setLayoutParams(volparams);

        ((SeekBar) vol).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar s, int progress, boolean touch) {
                //TODO:
                setVolume(progress);
            }

            public void onStartTrackingTouch(SeekBar arg0) {

            }

            public void onStopTrackingTouch(SeekBar arg0) {

            }
        });

        layout.addView(vol);

        return layout;

    }

    protected void onResume() {
        Log.v("SDL", "onResume()");
        MainService.updateServiceNotification("Vectras VM Running");
        super.onResume();
    }

    // Messages from the SDLMain thread
    static int COMMAND_CHANGE_TITLE = 1;
    static int COMMAND_SAVEVM = 2;

    public void loadLibraries() {
        //XXX: override for the specific arch
    }


    public void promptPause(final Activity activity) {

        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Pause VM");
        TextView stateView = new TextView(activity);
        stateView.setText("This make take a while depending on the RAM size used");
        stateView.setPadding(20, 20, 20, 20);
        alertDialog.setView(stateView);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Pause", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                onPauseVM();
                return;
            }
        });
        alertDialog.show();
    }

    private void onPauseVM() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                // Delete any previous state file
                if (MainActivity.vmexecutor.save_state_name != null) {
                    File file = new File(MainActivity.vmexecutor.save_state_name);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                if (Config.showToast)
                    UIUtils.toastShort(getApplicationContext(), "Please wait while saving VM State");
                MainActivity.vmexecutor.current_fd = MainActivity.vmexecutor.get_fd(MainActivity.vmexecutor.save_state_name);

                String uri = "fd:" + MainActivity.vmexecutor.current_fd;
                String command = QmpClient.stop();
                String msg = QmpClient.sendCommand(command);
                command = QmpClient.migrate(false, false, uri);
                msg = QmpClient.sendCommand(command);
                if (msg != null) {
                    processMigrationResponse(msg);
                }

                // XXX: Instead we poll to see if migration is complete
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        VMListener a = new VMListener();
                        a.execute();
                    }
                }, 0);
            }
        });
        t.start();

    }

    private void processMigrationResponse(String response) {
        String errorStr = null;
        try {
            JSONObject object = new JSONObject(response);
            errorStr = object.getString("error");
        } catch (Exception ex) {
            if (Config.debug)
                ex.printStackTrace();
        }
        if (errorStr != null) {
            String descStr = null;

            try {
                JSONObject descObj = new JSONObject(errorStr);
                descStr = descObj.getString("desc");
            } catch (Exception ex) {
                if (Config.debug)
                    ex.printStackTrace();
            }
            final String descStr1 = descStr;

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Machine.pausedErrorVM(activity, descStr1);
                }
            }, 100);

        }

    }

    private class VMListener extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... arg0) {
            startTimeListener();
            return null;
        }

        @Override
        protected void onPostExecute(Void test) {

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean res = false;
        if (Config.mouseMode == Config.MouseMode.External) {
            return res;
        }
        //TODO:
        res = ((MainSDLSurface) this.mSurface).onTouchProcess(this.mSurface, event);
        res = ((MainSDLSurface) this.mSurface).onTouchEventProcess(event);
        return true;
    }

    private void resumeVM() {
        if (MainActivity.vmexecutor == null) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (MainActivity.vmexecutor.paused == 1) {

                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MainVNCActivity.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    MainActivity.vmexecutor.paused = 0;

                    String command = QmpClient.cont();
                    String msg = QmpClient.sendCommand(command);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (Config.mouseMode == Config.MouseMode.External)
                                setUIModeDesktop();
                            else
                                setUIModeMobile(screenMode == SDLScreenMode.FitToScreen);
                        }
                    }, 500);
                }
            }
        });
        t.start();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.invalidateOptionsMenu();
    }

    public void onSelectMenuSDLDisplay() {

        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle("Display");

        LinearLayout.LayoutParams volParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        LinearLayout t = createSDLDisplayPanel();
        t.setLayoutParams(volParams);

        ScrollView s = new ScrollView(activity);
        s.addView(t);
        alertDialog.setView(s);
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                alertDialog.cancel();
            }
        });
        alertDialog.show();

    }


    public LinearLayout createSDLDisplayPanel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int currRate = getCurrentSDLRefreshRate();

        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        Button displayMode = new Button(this);

        displayMode.setText("Display Mode");
        displayMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onDisplayMode();
            }
        });
        buttonsLayout.addView(displayMode);
        layout.addView(buttonsLayout);

        final TextView value = new TextView(this);
        value.setText("Idle Refresh Rate: " + currRate + " Hz");
        layout.addView(value);
        value.setLayoutParams(params);

        SeekBar rate = new SeekBar(this);
        rate.setMax(Config.MAX_DISPLAY_REFRESH_RATE);

        rate.setProgress(currRate);
        rate.setLayoutParams(params);

        ((SeekBar) rate).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar s, int progress, boolean touch) {
                value.setText("Idle Refresh Rate: " + (progress + 1) + " Hz");
            }

            public void onStartTrackingTouch(SeekBar arg0) {

            }

            public void onStopTrackingTouch(SeekBar arg0) {
                int progress = arg0.getProgress() + 1;
                int refreshMs = 1000 / progress;
                Log.v(TAG, "Changing idle refresh rate: (ms)" + refreshMs);
                MainActivity.vmexecutor.setsdlrefreshrate(refreshMs);
            }
        });


        layout.addView(rate);

        return layout;

    }

    public int getCurrentSDLRefreshRate() {
        return 1000 / MainActivity.vmexecutor.getsdlrefreshrate();
    }


    private void onDisplayMode() {

        String[] items = {
                "Normal (One-To-One)",
                "Fit To Screen"
//                ,"Stretch To Screen" //Stretched
        };
        int currentScaleType = 0;
        if (screenMode == SDLScreenMode.FitToScreen) {
            currentScaleType = 1;
        } else if (screenMode == SDLScreenMode.Fullscreen)
            currentScaleType = 2;

        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("Display Mode");
        mBuilder.setSingleChoiceItems(items, currentScaleType, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case 0:
                        onNormalScreen();
                        break;
                    case 1:
                        if (Config.mouseMode == Config.MouseMode.External) {
                            UIUtils.toastShort(MainSDLActivity.this, "Fit to Screen Disabled under Desktop Mode");
                            dialog.dismiss();
                            return;
                        }
                        onFitToScreen();
                        break;
                    case 2:
                        if (Config.mouseMode == Config.MouseMode.External) {
                            UIUtils.toastShort(MainSDLActivity.this, "Stretch Screen Disabled under Desktop Mode");
                            dialog.dismiss();
                            return;
                        }
                        onStretchToScreen();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });
        final AlertDialog alertDialog = mBuilder.create();
        alertDialog.show();

    }


    @Override
    protected synchronized void runSDLMain() {

        //We go through the vm executor
        MainActivity.startvm(this, Config.UI_SDL);

        //XXX: we hold the thread because SDLActivity will exit
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void onVMResolutionChanged(int w, int h) {
        boolean refreshDisplay = false;

        if (w != vm_width || h != vm_height)
            refreshDisplay = true;
        vm_width = w;
        vm_height = h;

        Log.v(TAG, "VM resolution changed to " + vm_width + "x" + vm_height);


        if (refreshDisplay) {
            activity.resize(null);
        }

    }

    public static boolean isResizing = false;

    public enum SDLScreenMode {
        Normal,
        FitToScreen,
        Fullscreen //fullscreen not implemented yet
    }

    public SDLScreenMode screenMode = SDLScreenMode.FitToScreen;

    private void setLayout(Configuration newConfig) {

        boolean isLanscape =
                (newConfig != null && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
                        || UIUtils.isLandscapeOrientation(this);

        View vnc_canvas_layout = (View) this.findViewById(R.id.sdl_layout);
        RelativeLayout.LayoutParams vnc_canvas_layout_params = null;
        //normal 1-1
        if (screenMode == SDLScreenMode.Normal) {
            if (isLanscape) {
                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
//                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_IN_PARENT);
                vnc_canvas_layout_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_HORIZONTAL);

            } else {
                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                vnc_canvas_layout_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            }
        } else {
            //fittoscreen
            if (isLanscape) {
                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_IN_PARENT);
            } else {

                vnc_canvas_layout_params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                vnc_canvas_layout_params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                vnc_canvas_layout_params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            }
        }
        vnc_canvas_layout.setLayoutParams(vnc_canvas_layout_params);

        this.invalidateOptionsMenu();
    }

    public class MainSDLSurface extends ExSDLSurface implements View.OnKeyListener, View.OnTouchListener {

        public boolean initialized = false;

        public MainSDLSurface(Context context) {
            super(context);
            setOnKeyListener(this);
            setOnTouchListener(this);
            gestureDetector = new GestureDetector(activity, new GestureListener());
            setOnGenericMotionListener(new SDLGenericMotionListener_API12());
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width, int height) {
            super.surfaceChanged(holder, format, width, height);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_CTRL_LEFT);
                    SDLActivity.onNativeKeyUp(KeyEvent.KEYCODE_CTRL_LEFT);
                }
            }, 500);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            super.surfaceCreated(holder);

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (Config.mouseMode == Config.MouseMode.External)
                        setUIModeDesktop();
                    else
                        setUIModeMobile(screenMode == SDLScreenMode.FitToScreen);
                }
            }, 1000);
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            Log.d(TAG, "Configuration changed");
            resize(newConfig);
        }


        public synchronized void doResize(boolean reverse, final Configuration newConfig) {
            //XXX: notify the UI not to process mouse motion
            isResizing = true;
            Log.v(TAG, "Resizing Display");
            Display display = SDLActivity.mSingleton.getWindowManager().getDefaultDisplay();
            int height = 0;
            int width = 0;

            Point size = new Point();
            display.getSize(size);
            int screen_width = size.x;
            int screen_height = size.y;

            final ActionBar bar = ((SDLActivity) activity).getSupportActionBar();

            if (MainSDLActivity.mLayout != null) {
                width = MainSDLActivity.mLayout.getWidth();
                height = MainSDLActivity.mLayout.getHeight();
            }

            //native resolution for use with external mouse
            if (screenMode != SDLScreenMode.Fullscreen && screenMode != SDLScreenMode.FitToScreen) {
                width = MainSDLActivity.vm_width;
                height = MainSDLActivity.vm_height;
            }

            if (reverse) {
                int temp = width;
                width = height;
                height = temp;
            }

            boolean portrait = SDLActivity.mSingleton.getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

            if (portrait) {
                if (Config.mouseMode != Config.MouseMode.External) {
                    int height_n = (int) (width / (MainSDLActivity.vm_width / (float) MainSDLActivity.vm_height));
                    Log.d(TAG, "Resizing portrait: " + width + " x " + height_n);
                    getHolder().setFixedSize(width, height_n);
                }
            } else {
                if ((screenMode == SDLScreenMode.Fullscreen || screenMode == SDLScreenMode.FitToScreen)
                        && !MainSettingsManager.getAlwaysShowMenuToolbar(MainSDLActivity.this)
                        && bar != null && bar.isShowing()) {
                    height += bar.getHeight();
                }
                Log.d(TAG, "Resizing landscape: " + width + " x " + height);
                getHolder().setFixedSize(width, height);
            }
            initialized = true;

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    isResizing = false;
                }
            }, 1000);


        }

        // XXX: SDL is missing some key codes in sdl2-keymap.h
        // So we create them with a Shift Modifier
        private boolean handleMissingKeys(int keyCode, int action) {

            int keyCodeTmp = keyCode;
            switch (keyCode) {
                case 77:
                    keyCodeTmp = 9;
                    break;
                case 81:
                    keyCodeTmp = 70;
                    break;
                case 17:
                    keyCodeTmp = 15;
                    break;
                case 18:
                    keyCodeTmp = 10;
                    break;
                default:
                    return false;

            }
            if (action == KeyEvent.ACTION_DOWN) {
                SDLActivity.onNativeKeyDown(59);
                SDLActivity.onNativeKeyDown(keyCodeTmp);
            } else {
                SDLActivity.onNativeKeyUp(59);
                SDLActivity.onNativeKeyUp(keyCodeTmp);
            }
            return true;

        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if ((keyCode == KeyEvent.KEYCODE_BACK) || (keyCode == KeyEvent.KEYCODE_FORWARD)) {
                // dismiss android back and forward keys
                return true;
            } else if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
                return false;
            } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
                //Log.v("SDL", "key down: " + keyCode);
                if (!handleMissingKeys(keyCode, event.getAction()))
                    SDLActivity.onNativeKeyDown(keyCode);
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                //Log.v("SDL", "key up: " + keyCode);
                if (!handleMissingKeys(keyCode, event.getAction()))
                    SDLActivity.onNativeKeyUp(keyCode);
                return true;
            } else {
                return super.onKey(v, keyCode, event);
            }

        }

        // Touch events
        public boolean onTouchProcess(View v, MotionEvent event) {
            int action = event.getAction();
            float x = event.getX(0);
            float y = event.getY(0);
            float p = event.getPressure(0);

            int relative = Config.mouseMode == Config.MouseMode.External ? 0 : 1;

            int sdlMouseButton = 0;
            if (event.getButtonState() == MotionEvent.BUTTON_PRIMARY)
                sdlMouseButton = Config.SDL_MOUSE_LEFT;
            else if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY)
                sdlMouseButton = Config.SDL_MOUSE_RIGHT;
            else if (event.getButtonState() == MotionEvent.BUTTON_TERTIARY)
                sdlMouseButton = Config.SDL_MOUSE_MIDDLE;


            if (event.getAction() == MotionEvent.ACTION_MOVE) {

                if (mouseUp) {
                    old_x = x;
                    old_y = y;
                    mouseUp = false;
                }
                if (action == MotionEvent.ACTION_MOVE) {
                    if (Config.mouseMode == Config.MouseMode.External) {
                        //Log.d("SDL", "onTouch Absolute Move by=" + action + ", X,Y=" + (x) + "," + (y) + " P=" + p);
                        MainActivity.vmexecutor.onVectrasMouse(0, MotionEvent.ACTION_MOVE, 0, x, y);
                    } else {
                        //Log.d("SDL", "onTouch Relative Moving by=" + action + ", X,Y=" + (x -
//                            old_x) + "," + (y - old_y) + " P=" + p);
                        MainActivity.vmexecutor.onVectrasMouse(0, MotionEvent.ACTION_MOVE, 1, (x - old_x) * sensitivity_mult, (y - old_y) * sensitivity_mult);
                    }

                }
                // save current
                old_x = x;
                old_y = y;

            } else if (event.getAction() == event.ACTION_UP) {
                //Log.d("SDL", "onTouch Up: " + sdlMouseButton);
                //XXX: it seems that the Button state is not available when Button up so
                //  we should release all mouse buttons to be safe since we don't know which one fired the event
                if (sdlMouseButton == Config.SDL_MOUSE_MIDDLE
                        || sdlMouseButton == Config.SDL_MOUSE_RIGHT
                ) {
                    MainActivity.vmexecutor.onVectrasMouse(sdlMouseButton, MotionEvent.ACTION_UP, relative, x, y);
                } else if (sdlMouseButton != 0) {
                    MainActivity.vmexecutor.onVectrasMouse(sdlMouseButton, MotionEvent.ACTION_UP, relative, x, y);
                } else { // if we don't have inforamtion about which button we can make some guesses

                    //Or only the last one pressed
                    if (lastMouseButtonDown > 0) {
                        if (lastMouseButtonDown == Config.SDL_MOUSE_MIDDLE
                                || lastMouseButtonDown == Config.SDL_MOUSE_RIGHT
                        ) {
                            MainActivity.vmexecutor.onVectrasMouse(lastMouseButtonDown, MotionEvent.ACTION_UP, relative, x, y);
                        } else
                            MainActivity.vmexecutor.onVectrasMouse(lastMouseButtonDown, MotionEvent.ACTION_UP, relative, x, y);
                    } else {
                        //ALl buttons
                        if (Config.mouseMode == Config.MouseMode.Trackpad) {
                            MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 1, 0, 0);
                        } else if (Config.mouseMode == Config.MouseMode.External) {
                            MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 0, x, y);
                            MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_RIGHT, MotionEvent.ACTION_UP, 0, x, y);
                            MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_MIDDLE, MotionEvent.ACTION_UP, 0, x, y);
                        }
                    }
                }
                lastMouseButtonDown = -1;
                mouseUp = true;
            } else if (event.getAction() == event.ACTION_DOWN
                    && Config.mouseMode == Config.MouseMode.External
            ) {

                //XXX: Some touch events for touchscreen mode are primary so we force left mouse button
                if (sdlMouseButton == 0 && MotionEvent.TOOL_TYPE_FINGER == event.getToolType(0)) {
                    sdlMouseButton = Config.SDL_MOUSE_LEFT;
                }

                MainActivity.vmexecutor.onVectrasMouse(sdlMouseButton, MotionEvent.ACTION_DOWN, relative, x, y);
                lastMouseButtonDown = sdlMouseButton;
            }
            return true;
        }

        public boolean onTouch(View v, MotionEvent event) {
            boolean res = false;
            if (Config.mouseMode == Config.MouseMode.External) {
                res = onTouchProcess(v, event);
                res = onTouchEventProcess(event);
            }
            return res;
        }

        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }

        public boolean onTouchEventProcess(MotionEvent event) {
            // Log.v("onTouchEvent",
            // "Action=" + event.getAction() + ", X,Y=" + event.getX() + ","
            // + event.getY() + " P=" + event.getPressure());
            // MK
            if (event.getAction() == MotionEvent.ACTION_CANCEL)
                return true;

            if (!firstTouch) {
                firstTouch = true;
            }
            if (event.getPointerCount() > 1) {

                // XXX: Limbo Legacy enable Right Click with 2 finger touch
                // Log.v("Right Click",
                // "Action=" + event.getAction() + ", X,Y=" + event.getX()
                // + "," + event.getY() + " P=" + event.getPressure());
                // rightClick(event);
                return true;
            } else
                return gestureDetector.onTouchEvent(event);
        }
    }

    public AudioManager am;
    protected int maxVolume;

    protected void setupVolume() {
        if (am == null) {
            am = (AudioManager) mSingleton.getSystemService(Context.AUDIO_SERVICE);
            maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
    }

    public void setVolume(int volume) {
        if (am != null)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    protected int getCurrentVolume() {
        int volumeTmp = 0;
        if (am != null)
            volumeTmp = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        return volumeTmp;
    }


    //XXX: We want to suspend only when app is calling onPause()
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

    }

    public boolean rightClick(final MotionEvent e, final int i) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Log.d("SDL", "Mouse Right Click");
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_RIGHT, MotionEvent.ACTION_DOWN, 1, -1, -1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
//					Log.v("SDLSurface", "Interrupted: " + ex);
                }
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_RIGHT, MotionEvent.ACTION_UP, 1, -1, -1);
            }
        });
        t.start();
        return true;

    }

    public boolean leftClick(final MotionEvent e, final int i) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Log.d("SDL", "Mouse Left Click");
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_DOWN, 1, -1, -1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
//					Log.v("SDLSurface", "Interrupted: " + ex);
                }
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 1, -1, -1);
            }
        });
        t.start();
        return true;

    }

    public boolean middleClick(final MotionEvent e, final int i) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Log.d("SDL", "Mouse Middle Click");
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_MIDDLE, MotionEvent.ACTION_DOWN, 1, -1, -1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
//                    Log.v("SDLSurface", "Interrupted: " + ex);
                }
                MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_MIDDLE, MotionEvent.ACTION_UP, 1, -1, -1);
            }
        });
        t.start();
        return true;

    }

    private void doubleClick(final MotionEvent event, final int pointer_id) {

        Thread t = new Thread(new Runnable() {
            public void run() {
                //Log.d("SDL", "Mouse Double Click");
                for (int i = 0; i < 2; i++) {
                    MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_DOWN, 1, 0, 0);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        // Log.v("doubletap", "Could not sleep");
                    }
                    MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_UP, 1, 0, 0);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        // Log.v("doubletap", "Could not sleep");
                    }
                }
            }
        });
        t.start();
    }


    int lastMouseButtonDown = -1;
    public float old_x = 0;
    public float old_y = 0;
    private boolean mouseUp = true;
    private float sensitivity_mult = (float) 1.0;
    private boolean firstTouch = false;


    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            // Log.v("onDown", "Action=" + event.getAction() + ", X,Y=" + event.getX()
            // + "," + event.getY() + " P=" + event.getPressure());
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            // Log.d("SDL", "Long Press Action=" + event.getAction() + ", X,Y="
            // + event.getX() + "," + event.getY() + " P="
            // + event.getPressure());
            if (Config.mouseMode == Config.MouseMode.External)
                return;

            if (Config.enableDragOnLongPress)
                dragPointer(event);
        }

        public boolean onSingleTapConfirmed(MotionEvent event) {
            float x1 = event.getX();
            float y1 = event.getY();

            if (Config.mouseMode == Config.MouseMode.External)
                return true;

//			 Log.d("onSingleTapConfirmed", "Tapped at: (" + x1 + "," + y1 +
//			 ")");

            for (int i = 0; i < event.getPointerCount(); i++) {
                int action = event.getAction();
                float x = event.getX(i);
                float y = event.getY(i);
                float p = event.getPressure(i);

                //Log.v("onSingleTapConfirmed", "Action=" + action + ", X,Y=" + x + "," + y + " P=" + p);
                if (event.getAction() == event.ACTION_DOWN
                        && MotionEvent.TOOL_TYPE_FINGER == event.getToolType(0)) {
                    //Log.d("SDL", "onTouch Down: " + event.getButtonState());
                    MainSDLActivity.singleClick(event, i);
                }
            }
            return true;

        }

        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent event) {
//			Log.d("onDoubleTap", "Tapped at: (" + event.getX() + "," + event.getY() + ")");

            if (Config.mouseMode == Config.MouseMode.External
                //&& MotionEvent.TOOL_TYPE_MOUSE == event.getToolType(0)
            )
                return true;

            if (!Config.enableDragOnLongPress)
                processDoubleTap(event);
            else
                doubleClick(event, 0);

            return true;
        }
    }

    private void dragPointer(MotionEvent event) {

        MainActivity.vmexecutor.onVectrasMouse(Config.SDL_MOUSE_LEFT, MotionEvent.ACTION_DOWN, 1, 0, 0);
        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (v.hasVibrator()) {
            v.vibrate(100);
        }
    }

    private void processDoubleTap(final MotionEvent event) {

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                if (!mouseUp) {
                    dragPointer(event);
                } else {
                    // Log.v("onDoubleTap", "Action=" + action + ", X,Y=" + x + "," + y + " P=" + p);
                    doubleClick(event, 0);
                }

            }
        });
        t.start();

    }

    class SDLGenericMotionListener_API12 implements View.OnGenericMotionListener {
        private MainSDLSurface mSurface;

        @Override
        public boolean onGenericMotion(View v, MotionEvent event) {
            float x, y;
            int action;

            switch (event.getSource()) {
                case InputDevice.SOURCE_JOYSTICK:
                case InputDevice.SOURCE_GAMEPAD:
                case InputDevice.SOURCE_DPAD:
                    SDLControllerManager.handleJoystickMotionEvent(event);
                    return true;

                case InputDevice.SOURCE_MOUSE:
                    if (Config.mouseMode == Config.MouseMode.Trackpad)
                        break;

                    action = event.getActionMasked();
//                    Log.d("SDL", "onGenericMotion, action = " + action + "," + event.getX() + ", " + event.getY());
                    switch (action) {
                        case MotionEvent.ACTION_SCROLL:
                            x = event.getAxisValue(MotionEvent.AXIS_HSCROLL, 0);
                            y = event.getAxisValue(MotionEvent.AXIS_VSCROLL, 0);
//                            Log.d("SDL", "Mouse Scroll: " + x + "," + y);
                            MainActivity.vmexecutor.onVectrasMouse(0, action, 0, x, y);
                            return true;

                        case MotionEvent.ACTION_HOVER_MOVE:
                            if (Config.processMouseHistoricalEvents) {
                                final int historySize = event.getHistorySize();
                                for (int h = 0; h < historySize; h++) {
                                    float ex = event.getHistoricalX(h);
                                    float ey = event.getHistoricalY(h);
                                    float ep = event.getHistoricalPressure(h);
                                    processHoverMouse(ex, ey, ep, action);
                                }
                            }

                            float ex = event.getX();
                            float ey = event.getY();
                            float ep = event.getPressure();
                            processHoverMouse(ex, ey, ep, action);
                            return true;

                        case MotionEvent.ACTION_UP:

                        default:
                            break;
                    }
                    break;

                default:
                    break;
            }

            // Event was not managed
            return false;
        }

        private void processHoverMouse(float x, float y, float p, int action) {


            if (Config.mouseMode == Config.MouseMode.External) {
                //Log.d("SDL", "Mouse Hover: " + x + "," + y);
                MainActivity.vmexecutor.onVectrasMouse(0, action, 0, x, y);
            }
        }

    }

    GestureDetector gestureDetector;

}
