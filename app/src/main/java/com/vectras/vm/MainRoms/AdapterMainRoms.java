package com.vectras.vm.MainRoms;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.vectras.qemu.Config;
import com.vectras.vm.AppConfig;
import com.vectras.vm.Fragment.HomeFragment;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.utils.UIUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AdapterMainRoms extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private LayoutInflater inflater;
    public List<DataMainRoms> data = Collections.emptyList();
    int currentPos = 0;
    private int mSelectedItem = -1;
    public static Dialog d;

    // create constructor to innitilize context and data sent from MainActivity
    public AdapterMainRoms(Context context, List<DataMainRoms> data) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.data = data;
    }

    // Inflate the layout when viewholder created
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.container_main_roms, parent, false);
        MyHolder holder = new MyHolder(view);
        return holder;
    }

    public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

    // Bind data
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

        // Get current position of item in recyclerview to bind data and assign values from list
        final MyHolder myHolder = (MyHolder) holder;
        final DataMainRoms current = data.get(position);
        myHolder.textName.setText(current.itemName);
        myHolder.textArch.setText(current.itemArch);
        Bitmap bmImg = BitmapFactory.decodeFile(current.itemIcon);
        myHolder.ivIcon.setImageBitmap(bmImg);
        myHolder.optionsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                d = new Dialog(MainActivity.activity);
                d.setTitle(current.itemName);
                d.setContentView(R.layout.rom_options_dialog);
                d.setCancelable(true);
                TextView qemu = d.findViewById(R.id.qemu);
                qemu.setText(current.itemExtra);
                TextInputEditText drv1 = d.findViewById(R.id.drive1);
                drv1.setText(current.itemDrv1);
                drv1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        MainActivity.curRomName = current.itemName;

                        // Optionally, specify a URI for the file that should appear in the
                        // system file picker when it loads.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                        }

                        MainActivity.activity.startActivityForResult(intent, 122);
                    }
                });
                TextInputLayout drv1fld = d.findViewById(R.id.drive1Field);
                drv1fld.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        MainActivity.curRomName = current.itemName;

                        // Optionally, specify a URI for the file that should appear in the
                        // system file picker when it loads.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                        }

                        MainActivity.activity.startActivityForResult(intent, 122);
                    }
                });
                Button saveRomBtn = d.findViewById(R.id.saveRomBtn);
                saveRomBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final File jsonFile = new File(AppConfig.maindirpath + "roms-data" + ".json");
                        current.itemDrv1 = drv1.getText().toString();
                        current.itemExtra = qemu.getText().toString();
                        try {
                            JSONObject jObj = HomeFragment.jArray.getJSONObject(position);
                            jObj.put("imgDrv1", drv1.getText().toString());
                            jObj.put("imgExtra", qemu.getText().toString());
                            HomeFragment.jArray.put(position, jObj);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Writer output = null;
                            output = new BufferedWriter(new FileWriter(jsonFile));
                            output.write(HomeFragment.jArray.toString());
                            output.close();
                        } catch (Exception e) {
                            UIUtils.toastLong(MainActivity.activity, e.toString());
                        } finally {
                            d.dismiss();
                            HomeFragment.loadDataVbi();
                        }
                    }
                });
                Button exportRomBtn = d.findViewById(R.id.exportRomBtn);
                exportRomBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final File jsonFile = new File(MainActivity.activity.getExternalFilesDir("data") + "/rom-data.json");
                        AlertDialog ad;
                        ad = new AlertDialog.Builder(MainActivity.activity).create();
                        ad.setTitle("Export Rom");
                        ad.setMessage("Are you sure?");
                        final TextInputLayout Description = new TextInputLayout(MainActivity.activity);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        Description.setHint("DESCRIPTION (html supported)");
                        Description.setLayoutParams(lp);
                        Description.setPadding(10, 10, 10, 10);
                        final TextInputEditText DescriptionET = new TextInputEditText(MainActivity.activity);
                        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        DescriptionET.setLayoutParams(lp2);
                        Description.addView(DescriptionET);
                        DescriptionET.setText("null");
                        DescriptionET.setInputType(InputType.TYPE_CLASS_TEXT);
                        DescriptionET.setSelectAllOnFocus(true);
                        ad.setView(Description);
                        ad.setButton(Dialog.BUTTON_POSITIVE, "EXPORT", (dialog, which) -> {
                            RomJson obj = new RomJson();
                            JSONObject jsonObject = obj.makeJSONObject(current.itemName, current.itemArch, FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), DescriptionET.getText().toString(), new File(current.itemIcon).getName(), new File(current.itemPath).getName(), qemu.getText().toString());

                            try {
                                Writer output = null;
                                output = new BufferedWriter(new FileWriter(jsonFile));
                                output.write(jsonObject.toString().replace("\\", "").replace("//", "/"));
                                output.close();
                            } catch (Exception e) {
                            }
                            SharedPreferences credentials = MainActivity.activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);

                            ProgressDialog progressDialog = new ProgressDialog(MainActivity.activity);
                            progressDialog.setTitle("Compressing CVBI");
                            progressDialog.setMessage("Please wait...");
                            progressDialog.setCancelable(false);
                            progressDialog.show(); // Showing Progress Dialog
                            Thread t = new Thread() {
                                public void run() {
                                    try {
                                        ZipEntrySource[] addedEntries = new ZipEntrySource[]{
                                                new FileSource("/" + new File(current.itemPath).getName(), new File(current.itemPath)),
                                                new FileSource("/" + new File(current.itemIcon).getName(), new File(current.itemIcon)),
                                                new FileSource("/" + new File(current.itemDrv1).getName(), new File(current.itemDrv1)),
                                                new FileSource("/" + new File(MainActivity.activity.getExternalFilesDir("data") + "/rom-data.json").getName(), new File(MainActivity.activity.getExternalFilesDir("data") + "/rom-data.json"))
                                        };
                                        ZipUtil.pack(addedEntries, new File(AppConfig.datadirpath(MainActivity.activity) + "/cvbi/" + current.itemName + ".cvbi"));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Runnable runnable = new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.cancel(); // cancelling Dialog.
                                                MainActivity.UIAlert("ERROR!", e.toString(), MainActivity.activity);
                                            }
                                        };
                                        MainActivity.activity.runOnUiThread(runnable);
                                    } finally {
                                        Runnable runnable = new Runnable() {
                                            @Override
                                            public void run() {
                                                progressDialog.cancel(); // cancelling Dialog.}
                                                MainActivity.UIAlert("DONE!", AppConfig.datadirpath(MainActivity.activity) + "/cvbi/" + current.itemName + ".cvbi", MainActivity.activity);
                                            }
                                        };
                                        MainActivity.activity.runOnUiThread(runnable);
                                    }
                                }
                            };
                            t.start();
                            return;
                        });
                        ad.setButton(Dialog.BUTTON_NEGATIVE, "CLOSE", (dialog, which) -> {
                            return;
                        });
                        ad.show();
                    }
                });
                d.show();
            }
        });

        myHolder.cdRoms.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                MainActivity.setupNativeLibs();
                Config.hda_path = current.itemPath;
                Config.extra_params = current.itemExtra;
                Thread thread = new Thread(new Runnable() {
                    public void run() {
                        if (!Config.loadNativeLibsEarly && !Config.loadNativeLibsMainThread) {
                            MainActivity.setupNativeLibs();
                        }
                        MainActivity.onStartButton();
                    }
                });
                thread.setPriority(Thread.MIN_PRIORITY);
                thread.start();

            }
        });
        myHolder.cdRoms.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog ad;
                ad = new AlertDialog.Builder(MainActivity.activity, R.style.MainDialogTheme).create();
                ad.setTitle("Remove " + current.itemName);
                ad.setMessage("Are you sure?");
                ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE " + current.itemName, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(current.itemPath);
                        File file2 = new File(current.itemDrv1);
                        try {
                            file.delete();
                        } catch (Exception e) {
                            UIUtils.toastLong(MainActivity.activity, e.toString());
                        } finally {
                        }
                        try {
                            file2.delete();
                        } catch (Exception e) {
                            UIUtils.toastLong(MainActivity.activity, e.toString());
                        } finally {
                        }
                        HomeFragment.mMainAdapter = new AdapterMainRoms(MainActivity.activity, HomeFragment.data);
                        HomeFragment.data.remove(position);
                        HomeFragment.mRVMainRoms.setAdapter(HomeFragment.mMainAdapter);
                        HomeFragment.mRVMainRoms.setLayoutManager(new GridLayoutManager(MainActivity.activity, 2));
                        HomeFragment.jArray.remove(position);
                        try {
                            Writer output = null;
                            File jsonFile = new File(AppConfig.maindirpath + "roms-data" + ".json");
                            output = new BufferedWriter(new FileWriter(jsonFile));
                            output.write(HomeFragment.jArray.toString());
                            output.close();
                        } catch (Exception e) {
                            UIUtils.toastLong(MainActivity.activity, e.toString());
                        }
                        UIUtils.toastLong(MainActivity.activity, current.itemName + " are removed successfully!");
                        return;
                    }
                });
                ad.setButton(Dialog.BUTTON_POSITIVE, "CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
                ad.show();
                return false;
            }
        });
    }

    public class RomJson extends JSONObject {

        public JSONObject makeJSONObject(String imgName, String imgArch, String imgAuthor, String imgDesc, String imgIcon, String imgDrive, String imgQemu) {

            JSONObject obj = new JSONObject();

            try {
                obj.put("title", imgName);
                obj.put("arch", imgArch);
                obj.put("author", imgAuthor);
                obj.put("desc", imgDesc);
                obj.put("kernel", "windows");
                obj.put("icon", imgIcon);
                obj.put("drive", imgDrive);
                obj.put("qemu", imgQemu);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return obj;
        }
    }

    private void showDialog(String title, String path, String pathIcon) {

        final Dialog dialog = new Dialog(MainActivity.activity, R.style.MainDialogTheme);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.rom_options_layout);

        LinearLayout removeLayout = dialog.findViewById(R.id.layoutRemove);

        removeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog ad;
                ad = new AlertDialog.Builder(MainActivity.activity, R.style.MainDialogTheme).create();
                ad.setTitle("Remove " + title);
                ad.setMessage("Are you sure?");
                ad.setButton(Dialog.BUTTON_NEGATIVE, "REMOVE " + title, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(path);
                        try {
                            file.delete();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                        HomeFragment.mMainAdapter = new AdapterMainRoms(MainActivity.activity, HomeFragment.data);
                        HomeFragment.data.remove(currentPos);
                        HomeFragment.mRVMainRoms.setAdapter(HomeFragment.mMainAdapter);
                        HomeFragment.mRVMainRoms.setLayoutManager(new GridLayoutManager(MainActivity.activity, 2));
                        HomeFragment.jArray.remove(currentPos);
                        try {
                            Writer output = null;
                            File jsonFile = new File(AppConfig.maindirpath + "roms-data" + ".json");
                            output = new BufferedWriter(new FileWriter(jsonFile));
                            output.write(HomeFragment.jArray.toString().replace("\\", "").replace("//", "/"));
                            output.close();
                        } catch (Exception e) {
                            UIUtils.toastLong(MainActivity.activity, e.toString());
                        }
                        UIUtils.toastLong(MainActivity.activity, title + " are removed successfully!");
                        return;
                    }
                });
                ad.setButton(Dialog.BUTTON_POSITIVE, "CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });
                ad.show();
                dialog.dismiss();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);

    }

    // return total item from List
    @Override
    public int getItemCount() {
        return data.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {

        CardView cdRoms;
        TextView textName, textArch;
        ImageView ivIcon;
        ImageButton optionsBtn;

        // create constructor to get widget reference
        public MyHolder(View itemView) {
            super(itemView);
            cdRoms = (CardView) itemView.findViewById(R.id.cdItem);
            textName = (TextView) itemView.findViewById(R.id.textName);
            textArch = (TextView) itemView.findViewById(R.id.textArch);
            ivIcon = (ImageView) itemView.findViewById(R.id.ivIcon);
            optionsBtn = (ImageButton) itemView.findViewById(R.id.optionsButton);
        }

    }

}
