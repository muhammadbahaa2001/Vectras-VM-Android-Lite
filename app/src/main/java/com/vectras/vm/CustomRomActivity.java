package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.MainRoms.AdapterMainRoms;
import com.vectras.vm.MainRoms.DataMainRoms;
import com.vectras.vm.logger.VectrasStatus;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CustomRomActivity extends AppCompatActivity {

    public TextInputEditText title, icon, drive, qemu;
    public Button addRomBtn;

    public ProgressBar loadingPb;

    public static CustomRomActivity activity;

    private boolean isFilled(TextInputEditText TXT) {
        if (TXT.getText().toString().trim().length() > 0)
            return true;
        else
            return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, 0, 0, "arch").setShortcut('3', 'c').setIcon(R.drawable.ic_arch).setShowAsAction(1);

        menu.add(1, 1, 1, "custom rom").setShortcut('3', 'c').setIcon(R.drawable.input_circle).setShowAsAction(1);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 0:
                startActivity(new Intent(activity, SetArchActivity.class));
                return true;
            case 1:
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 0);
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_rom);
        activity = this;
        loadingPb = findViewById(R.id.loadingPb);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle("Custom Rom");
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        title = findViewById(R.id.title);
        icon = findViewById(R.id.icon);
        drive = findViewById(R.id.drive);
        qemu = findViewById(R.id.qemu);
        TextInputLayout iconLayout = findViewById(R.id.iconField);
        TextInputLayout driveLayout = findViewById(R.id.driveField);
        TextView arch = findViewById(R.id.textArch);
        arch.setText(MainSettingsManager.getArch(this));
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/jpeg");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1001);
            }
        });
        iconLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/jpeg");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1001);
            }
        });
        drive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1002);
            }
        });
        driveLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");

                // Optionally, specify a URI for the file that should appear in the
                // system file picker when it loads.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                }

                startActivityForResult(intent, 1002);
            }
        });
        addRomBtn = findViewById(R.id.addRomBtn);
        addRomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String CREDENTIAL_SHARED_PREF = "settings_prefs";
                SharedPreferences credentials = activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = credentials.edit();
                editor.putBoolean("isFirstLaunch", Boolean.TRUE);
                editor.apply();
                loadingPb.setVisibility(View.VISIBLE);
                final File jsonFile = new File(AppConfig.maindirpath + "roms-data" + ".json");
                RomsJso obj = new RomsJso();
                if (jsonFile.exists()) {
                    try {
                        List<DataMainRoms> data = new ArrayList<>();
                        JSONArray jArray = null;
                        jArray = new JSONArray(FileUtils.readFromFile(MainActivity.activity, jsonFile));

                        try {
                            // Extract data from json and store into ArrayList as class objects
                            for (int i = 0; i < jArray.length(); i++) {
                                JSONObject json_data = jArray.getJSONObject(i);
                                DataMainRoms romsMainData = new DataMainRoms();
                                romsMainData.itemName = json_data.getString("imgName");
                                romsMainData.itemIcon = json_data.getString("imgIcon");
                                romsMainData.itemPath = json_data.getString("imgPath");
                                romsMainData.itemExtra = json_data.getString("imgExtra");
                                data.add(romsMainData);
                            }

                        } catch (JSONException e) {
                            Toast.makeText(MainActivity.activity, e.toString(), Toast.LENGTH_LONG).show();
                        }

                        JSONObject jsonObject = obj.makeJSONObject(title.getText().toString(), icon.getText().toString(), MainSettingsManager.getArch(activity), drive.getText().toString(), qemu.getText().toString());
                        jArray.put(jsonObject);
                        try {
                            Writer output = null;
                            output = new BufferedWriter(new FileWriter(jsonFile));
                            output.write(jArray.toString().replace("\\", "").replace("//", "/"));
                            output.close();
                        } catch (Exception e) {
                            UIUtils.toastLong(activity, e.toString());
                            loadingPb.setVisibility(View.GONE);
                        }
                    } catch (JSONException e) {
                        loadingPb.setVisibility(View.GONE);
                        throw new RuntimeException(e);
                    }
                } else {
                    JSONObject jsonObject = obj.makeJSONObject(title.getText().toString(), icon.getText().toString(), MainSettingsManager.getArch(activity), drive.getText().toString(), qemu.getText().toString());
                    JSONArray jsonArray = new JSONArray();
                    jsonArray.put(jsonObject);
                    try {
                        Writer output = null;
                        output = new BufferedWriter(new FileWriter(jsonFile));
                        output.write(jsonArray.toString().replace("\\", "").replace("//", "/"));
                        output.close();
                    } catch (Exception e) {
                        UIUtils.toastLong(activity, e.toString());
                    }
                    VectrasStatus.logInfo("Welcome to Vectras ♡");
                }
                finish();
                activity.startActivity(new Intent(activity, SplashActivity.class));
            }
        });
        TextView textName = findViewById(R.id.textName);
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textName.setText(title.getText().toString());

                if (isFilled(title) && isFilled(icon) && isFilled(drive))
                    addRomBtn.setEnabled(true);
                else
                    addRomBtn.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFilled(title) && isFilled(icon) && isFilled(drive))
                    addRomBtn.setEnabled(true);
                else
                    addRomBtn.setEnabled(false);
            }
        };
        title.addTextChangedListener(afterTextChangedListener);
        icon.addTextChangedListener(afterTextChangedListener);
        drive.addTextChangedListener(afterTextChangedListener);
        qemu.addTextChangedListener(afterTextChangedListener);
    }

    public static class RomsJso extends JSONObject {

        public JSONObject makeJSONObject(String imgName, String imgIcon, String imgArch, String imgPath, String imgExtra) {

            JSONObject obj = new JSONObject();

            try {
                obj.put("imgName", imgName);
                obj.put("imgIcon", imgIcon);
                obj.put("imgArch", imgArch);
                obj.put("imgPath", imgPath);
                obj.put("imgExtra", imgExtra);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return obj;
        }
    }

    byte[] data;

    public String getPath(Uri uri) {
        return FileUtils.getPath(this, uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent ReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, ReturnedIntent);

        LinearLayout custom = findViewById(R.id.custom);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            TextInputEditText icon = findViewById(R.id.icon);
            File selectedFilePath = new File(getPath(content_describer));
            ImageView ivIcon = findViewById(R.id.ivIcon);
            loadingPb.setVisibility(View.VISIBLE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileInputStream File = null;
                    Bitmap selectedImage = null;
                    try {
                        File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                        selectedImage = BitmapFactory.decodeStream(File);
                        Bitmap finalSelectedImage = selectedImage;
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                ivIcon.setImageBitmap(finalSelectedImage);
                            }
                        };
                        activity.runOnUiThread(runnable);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            try {
                                SaveImage(selectedImage, new File(AppConfig.maindirpath + "/icons/"), selectedFilePath.getName());
                            } finally {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingPb.setVisibility(View.GONE);
                                        icon.setText(AppConfig.maindirpath + "/icons/" + selectedFilePath.getName());
                                    }
                                };
                                activity.runOnUiThread(runnable);
                                File.close();
                            }
                        } catch (IOException e) {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loadingPb.setVisibility(View.GONE);
                                }
                            };
                            activity.runOnUiThread(runnable);
                            MainActivity.UIAlert("error", e.toString(), activity);
                        }

                    }
                }
            }).start();
        } else if (requestCode == 1002 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            drive.setText(AppConfig.maindirpath + selectedFilePath.getName());
            loadingPb.setVisibility(View.VISIBLE);
            custom.setVisibility(View.GONE);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FileInputStream File = null;
                    try {
                        File = (FileInputStream) getContentResolver().openInputStream(content_describer);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        try {
                            OutputStream out = new FileOutputStream(new File(AppConfig.maindirpath + selectedFilePath.getName()));
                            try {
                                // Transfer bytes from in to out
                                byte[] buf = new byte[1024];
                                int len;
                                while ((len = File.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                            } finally {
                                out.close();
                            }
                        } finally {
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    loadingPb.setVisibility(View.GONE);
                                    custom.setVisibility(View.VISIBLE);
                                    addRomBtn.setEnabled(isFilled(title) && isFilled(icon) && isFilled(drive));
                                }
                            };
                            activity.runOnUiThread(runnable);
                            File.close();
                        }
                    } catch (IOException e) {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                loadingPb.setVisibility(View.GONE);
                                custom.setVisibility(View.VISIBLE);
                                addRomBtn.setEnabled(isFilled(title) && isFilled(icon) && isFilled(drive));
                                MainActivity.UIAlert("error", e.toString(), activity);
                            }
                        };
                        activity.runOnUiThread(runnable);
                    }
                }
            }).start();
        } else if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = ReturnedIntent.getData();
            File selectedFilePath = new File(getPath(content_describer));
            if (selectedFilePath.getName().endsWith(".cvbi")) {

                loadingPb.setVisibility(View.VISIBLE);
                custom.setVisibility(View.GONE);
                Thread t = new Thread() {
                    public void run() {
                        FileInputStream zipFile = null;
                        try {
                            zipFile = (FileInputStream) getContentResolver().openInputStream(content_describer);
                            File targetDirectory = new File(AppConfig.maindirpath + selectedFilePath.getName().replace(".cvbi", ""));
                            ZipInputStream zis = null;
                            zis = new ZipInputStream(
                                    new BufferedInputStream(zipFile));
                            try {
                                ZipEntry ze;
                                int count;
                                byte[] buffer = new byte[8192];
                                while ((ze = zis.getNextEntry()) != null) {
                                    File file = new File(targetDirectory, ze.getName());
                                    File dir = ze.isDirectory() ? file : file.getParentFile();
                                    if (!dir.isDirectory() && !dir.mkdirs())
                                        throw new FileNotFoundException("Failed to ensure directory: " +
                                                dir.getAbsolutePath());
                                    if (ze.isDirectory())
                                        continue;
                                    try (FileOutputStream fout = new FileOutputStream(file)) {
                                        while ((count = zis.read(buffer)) != -1)
                                            fout.write(buffer, 0, count);
                                    }
                        /* if time should be restored as well
                        long time = ze.getTime();
                        if (time > 0)
                            file.setLastModified(time);
                        */
                                }
                            } catch (IOException e) {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        UIUtils.toastLong(activity, e.toString());
                                    }
                                };
                                activity.runOnUiThread(runnable);
                            } finally {
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        loadingPb.setVisibility(View.GONE);
                                        custom.setVisibility(View.VISIBLE);
                                        try {

                                            JSONObject jObj = new JSONObject(FileUtils.readFromFile(MainActivity.activity, new File(AppConfig.maindirpath
                                                    + selectedFilePath.getName().replace(".cvbi", "") + "/rom-data.json")));

                                            title.setText(jObj.getString("title"));
                                            icon.setText(AppConfig.maindirpath
                                                    + selectedFilePath.getName().replace(".cvbi", "") + "/" + jObj.getString("icon"));
                                            drive.setText(AppConfig.maindirpath
                                                    + selectedFilePath.getName().replace(".cvbi", "") + "/" + jObj.getString("drive"));
                                            qemu.setText(jObj.getString("qemu"));
                                            ImageView ivIcon = findViewById(R.id.ivIcon);
                                            Bitmap bmImg = BitmapFactory.decodeFile(AppConfig.maindirpath
                                                    + selectedFilePath.getName().replace(".cvbi", "") + "/" + jObj.getString("icon"));
                                            ivIcon.setImageBitmap(bmImg);
                                            MainActivity.UIAlert("DESCRIPTION", "rom by:\n" + jObj.getString("author") + "\n\n" + Html.fromHtml(jObj.getString("desc")), activity);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                };
                                activity.runOnUiThread(runnable);
                                try {
                                    zis.close();
                                } catch (IOException e) {
                                    UIUtils.toastLong(activity, e.toString());
                                    throw new RuntimeException(e);
                                }
                            }
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
                t.start();

            } else {
                MainActivity.UIAlert("File not supported", "please select cvbi vailed file to continue.", activity);
            }

        } else if (requestCode == 1000 && resultCode == RESULT_CANCELED) {
            finish();
        }
    }

    private static void SaveImage(Bitmap finalBitmap, File imgDir, String name) {
        File myDir = imgDir;
        myDir.mkdirs();

        String fname = name;
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        File lol = new File(AppConfig.maindirpath + drive.getText().toString());
        try {
            lol.delete();
        } catch (Exception e) {
        }
    }
}
