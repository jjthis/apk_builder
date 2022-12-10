/*
 * Copyright (C) 2016 B. Clint Hall
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lino.shiablsfmk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    public static final String MyWorkFolder = "sdcard/MakeApkProject/";
    /*
    For testing android_apkmaker.  Included in the assests directory of this project is a test.zip file.
    You may replace it with your own, as you see fit.  test.zip should include an AndroidManifest.xml,
    a res directory, a java directory, and (optionally) an assets directory.
    APP_PACKAGE_NAME should be set to the package name specified in the AndroidManifest.xml in test.zip.
     */
    public static StringBuilder texts = new StringBuilder("<------------log----------->\n");
    public static String APP_PACKAGE_NAME = "com.mycompany.myapp";
    public static String APP_NAME = "MyApp";
    public static String WorkingFile = "";
    ListView projectList;
    ArrayList<HashMap<String, String>> arrList;
    SimpleAdapter adapter;
    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            File file = new File(MyWorkFolder + "Temp/");
            if (!file.exists())
                file.mkdirs();
            file = new File(MyWorkFolder + ".nomedia");
            if(!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            refresh();
            adapter = new SimpleAdapter(
                    MainActivity.this, arrList, R.layout.projectitem,
                    new String[]{"name", "packname"},
                    new int[]{R.id.name, R.id.packname}
            ) {
                @Override
                public View getView(final int position, final View convertView, final ViewGroup parent) {
                    //Log.i("taf", "getView: " + position);
                    final View view = super.getView(position, convertView, parent);
                    final TextView compile = (TextView) view.findViewById(R.id.compile);
                    final TextView editinfo = (TextView) view.findViewById(R.id.edit_info);
                    final TextView delete = (TextView) view.findViewById(R.id.delete);
                    final TextView deleapp = (TextView) view.findViewById(R.id.dele_app);
                    final TextView editor = (TextView) view.findViewById(R.id.editor);
                    final TextView name = (TextView) view.findViewById(R.id.name);
                    final TextView packname = (TextView) view.findViewById(R.id.packname);


                    deleapp.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Uri uri = Uri.fromParts("package", packname.getText().toString(), null);
                            Intent delIntent = new Intent(Intent.ACTION_DELETE, uri);
                            startActivity(delIntent);
                        }
                    });
                    compile.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            APP_PACKAGE_NAME = packname.getText().toString();
                            APP_NAME = name.getText().toString();
                            Toast.makeText(MainActivity.this, name.getText() + " 컴파일 중입니다." + "\n알림을 확인하세요.", Toast.LENGTH_SHORT).show();
                            buildAndInstall();
                        }
                    });
                    editinfo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            createProjectDialog(name.getText().toString(), packname.getText().toString(), false);
                        }
                    });
                    editor.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            WorkingFile = MyWorkFolder + name.getText() + "/src/java/" +
                                    packname.getText().toString().replace(".", "/") + "/MainActivity.java";
                            Intent intent = new Intent(MainActivity.this, sourceEditor.class);
                            startActivity(intent);
                        }
                    });
                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("Delete Project");
                            dialog.setMessage("프로젝트를 삭제하시겠습니까?");
                            dialog.setNegativeButton("취소", null);
                            dialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    deleteFolder(MyWorkFolder + name.getText());
                                    refresh();
                                    Toast.makeText(MainActivity.this, "Deleted " + name.getText(), Toast.LENGTH_SHORT).show();
                                }
                            });
                            dialog.show();
                        }
                    });
                    return view;
                }
            };
            projectList.setAdapter(adapter);
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(MainActivity.this, "Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            finish();
        }
    };

    public static void deleteFolder(String path) {

        File folder = new File(path);
        try {
            if (folder.exists()) {
                File[] folder_list = folder.listFiles(); //파일리스트 얻어오기

                for (int i = 0; i < folder_list.length; i++) {
                    if (folder_list[i].isFile()) {
                        folder_list[i].delete();
                    } else {
                        deleteFolder(folder_list[i].getPath()); //재귀함수호출
                    }
                    folder_list[i].delete();
                }
                folder.delete(); //폴더 삭제
            }
        } catch (Exception e) {
            e.getStackTrace();
        }
    }

    public void refresh() {
        arrList.clear();
        File file = new File(MyWorkFolder);
        String[] files = file.list();

        for (String name : files) {
            File check = new File(MyWorkFolder + name + "/Build_Info.txt");
            if (check.exists()) {
                HashMap<String, String> named = new HashMap<String, String>();
                named.put("name", name);
                named.put("packname", readFile(check.getAbsolutePath()));
                //ArrayList에 추가합니다..
                arrList.add(named);
            }
        }
        try {
            adapter.notifyDataSetChanged();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String channelId = "com.lino.shiablsfmk";
        String channelName = "Make Apk";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setRationaleMessage("컴파일을 위해 파일 권한이 필요합니다.")
                .setDeniedMessage("거부하시면 제대로된 컴파일이 되지 않습니다.")
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(1);
        try{
            copyAsset();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("MakeApkApplication");
        toolbar.setTitleTextColor(Color.WHITE);
        toolbar.setBackgroundResource(R.color.colorPrimary);
        setSupportActionBar(toolbar);
        lay.addView(toolbar);

        arrList = new ArrayList<HashMap<String, String>>();

        projectList = new ListView(this);


        lay.addView(projectList);
        setContentView(lay);
    }
    File initDir(File parent, String childName){
        File dir = new File(parent, childName);
        if(!dir.exists()) dir.mkdir();
        return dir;
    }
    public void copyAsset()
            throws IOException {
            //파일인 경우
        File filesDir = this.getFilesDir();
        File buildDir = initDir(filesDir, "buildStuff");
        File androidJar = new File(buildDir, "android.jar");
        OutputStream out = new FileOutputStream(androidJar);
        InputStream in = getAssets().open("android.jar");
        // Copy the bits from instream to outstream
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    public void createProjectDialog() {
        createProjectDialog("", "", true);
    }

    @SuppressLint("SetTextI18n")
    public void createProjectDialog(final String name, final String packName, final boolean iscreate) {
        Context ctx = MainActivity.this;
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        LinearLayout layout = new android.widget.LinearLayout(ctx);
        layout.setOrientation(1);
        TextView loc1 = new android.widget.TextView(ctx);
        final ClearEditText loc2 = new ClearEditText(ctx);
        loc2.setSingleLine();
        loc1.setText("Name:");
        loc1.setTextSize(18);
        loc2.setHint("앱 이름을 입력하세요...");
        loc2.setText(name);
        layout.addView(loc1);
        layout.addView(loc2);
        TextView loc3 = new android.widget.TextView(ctx);
        final ClearEditText loc4 = new ClearEditText(ctx);
        loc4.setSingleLine();
        loc3.setText("Package Name:");
        loc3.setTextSize(18);
        loc4.setHint("패키지명을 입력하세요...");
        loc4.setText(packName);


        layout.addView(loc3);
        layout.addView(loc4);
        LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        param2.leftMargin = 40;
        param2.topMargin = 40;
        LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        param1.leftMargin = 40;
        param1.rightMargin = 40;

        final ClearEditText number = new ClearEditText(ctx);
        number.setSingleLine();
        number.setHint("버전 코드를 입력하세요...");
        number.setText("1");
        number.setLayoutParams(param1);

        TextView text = new TextView(this);
        text.setText("Version Code:");
        text.setTextSize(18);
        text.setLayoutParams(param1);

        final ClearEditText numberN = new ClearEditText(ctx);
        numberN.setSingleLine();
        numberN.setHint("버전 이름을 입력하세요...");
        numberN.setText("1.0");
        numberN.setLayoutParams(param1);

        TextView text2 = new TextView(this);
        text2.setText("Version Name:");
        text2.setTextSize(18);
        text2.setLayoutParams(param1);

        loc2.setLayoutParams(param1);
        loc3.setLayoutParams(param1);
        loc4.setLayoutParams(param1);
        loc1.setLayoutParams(param2);
        dialog.setView(layout);
        dialog.setTitle("Edit Info");
        dialog.setNegativeButton("취소", null);
        dialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String pack = loc4.getText().toString();
                String named = loc2.getText().toString();
                if (!named.matches("[0-9a-zA-Z가-힣ㄱ-ㅎㅏ-ㅣ]+")) {
                    createProjectDialog(named, pack, iscreate);
                    Toast.makeText(MainActivity.this, "앱 이름은 숫자, 영어, 한글만 가능합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pack.split("\\.").length < 2 || !pack.matches("[0-9a-zA-Z.]+") ||
                        pack.split("\\.").length != pack.split("\\.", -1).length) {
                    createProjectDialog(named, pack, iscreate);
                    Toast.makeText(MainActivity.this, "점이 하나 이상 들어가야 합니다" +
                            "\n숫자와 영어로 이루어져야 합니다." +
                            "\n점과 점사이에 아무것도 없으면 안됩니다.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (iscreate) {
                    if(!number.getText().toString().matches("[0-9]+")) {
                        createProjectDialog(named, pack, true);
                        Toast.makeText(MainActivity.this, "버전코드는 숫자만 가능 합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(!numberN.getText().toString().matches("[0-9a-zA-Z.\\-]+")){
                        createProjectDialog(named, pack, true);
                        Toast.makeText(MainActivity.this, "버전이름은 숫자, 영어, 한글, .-만 가능 합니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createProject(named, pack, number.getText().toString(), numberN.getText().toString());
                }
                else
                    moveProject(named, pack, name, packName);
            }
        });
        if(!iscreate)
            dialog.show();
        else{
            dialog.setTitle("Create new Project");
            layout.addView(text);
            layout.addView(number);
            layout.addView(text2);
            layout.addView(numberN);
            dialog.show();
        }

    }

    //    File mkDir = getDir("폴더", Activity.MODE_PRIVATE);
//    String path = mkDir.getAbsolutePath();
//
//    File file = new File(path + "/파일명");
//
//    FileOutputStream fos = new FileOutputStream(file, false);
//
//    Writer out = new OutputStreamWriter(fos, "EUC-KR");
//
//   out.write(sb2.toString());
//
//   out.close();


    //android:versionCode="1"
    //android:versionName="1.0"
    public void createProject(String name, String packName, String verCode, String verName) {
        //Build_Info.txt
        //strings.xml
        //MainActivity.java
        //AndroidManifest.xml
        String projectPath = MyWorkFolder + name;
        if (new File(projectPath).exists()) {
            Toast.makeText(MainActivity.this, "이미 존재하는 프로젝트입니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        unpackZip("app.zip", projectPath);
        writeFile(projectPath + "/Build_Info.txt", packName);
        writeFile(projectPath + "/src/res/values/strings.xml", "<resources>\n" +
                "    <string name=\"app_name\">" + name + "</string>\n" +
                "\n" +
                "</resources>");
        writeFile(projectPath + "/src/AndroidManifest.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    package=\"" + packName + "\"\n" +
                "    android:versionCode=\""+verCode+"\"\n" +
                "    android:versionName=\""+verName+"\">\n" +
                "    <uses-sdk android:minSdkVersion=\"16\"\n" +
                "              android:targetSdkVersion=\"26\"/>\n" +
                "    <application\n" +
                "        android:allowBackup=\"true\"\n" +
                "        android:icon=\"@drawable/ic_launcher\"\n" +
                "        android:label=\"@string/app_name\">\n" +
                "        <activity \n" +
                "        android:theme=\"@android:style/Theme.Light\"\n" +
                "        android:name=\".MainActivity\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.MAIN\" />\n" +
                "\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "    </application>\n" +
                "\n" +
                "</manifest>");
        File packagePath = new File(projectPath + "/src/java/" + packName.replace(".", "/"));
        packagePath.mkdirs();
        writeFile(packagePath.getAbsolutePath() + "/MainActivity.java", "package " + packName + ";\n" +
                "\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "\n" +
                "public class MainActivity extends Activity {\n" +
                "\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "    }\n" +
                "}");
        refresh();
        Toast.makeText(MainActivity.this, "Created Project", Toast.LENGTH_SHORT).show();
    }
    //Build_Info.txt
    //strings.xml
    //MainActivity.java
    //AndroidManifest.xml
    public void moveProject(String name, String packName, String oldName, String oldPackName) {
        if(new File(MyWorkFolder+name).exists()){
            Toast.makeText(this, "이미 존재하는 프로젝트명 입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        writeFile(MyWorkFolder+oldName+"/Build_Info.txt", packName);
        writeFile(MyWorkFolder+oldName+"/src/res/values/strings.xml",
                readFile(MyWorkFolder+oldName+"/src/res/values/strings.xml")
                        .replaceFirst("<string name=\"app_name\">.*?</string>",
                                "<string name=\"app_name\">" + name + "</string>" ));
        writeFile(MyWorkFolder+oldName + "/src/AndroidManifest.xml",
                readFile(MyWorkFolder+oldName + "/src/AndroidManifest.xml")
        .replaceFirst("package=\".*?\"", "package=\""+packName+"\""));
        new File(MyWorkFolder+oldName).renameTo(new File(MyWorkFolder+name));
        File ss = new File(MyWorkFolder+name+ "/src/java/"+packName.replace(".", "/"));

        new File(MyWorkFolder+name+ "/src/java/"+oldPackName.replace(".", "/"))
                .renameTo(new File(MyWorkFolder+ "Temp/TempJava"));

        File[] filel = new File(MyWorkFolder+"Temp/TempJava").listFiles();
        for(File file : filel){
            writeFile(file.getAbsolutePath(), readFile(file.getAbsolutePath()).replaceFirst("package .*?;", "package "+packName+";"));
        }
        deleteFolder(MyWorkFolder+name+ "/src/java/");

        ss.mkdirs();
        ss.delete();
        new File(MyWorkFolder+ "Temp/TempJava")
                .renameTo(new File(MyWorkFolder+name+ "/src/java/"+packName.replace(".", "/")));
        refresh();
        Toast.makeText(this, "Edit Complete", Toast.LENGTH_SHORT).show();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.createProject:
                createProjectDialog();
                return true;
            case R.id.log:
                Intent intent = new Intent(MainActivity.this, log.class);
                startActivity(intent);
                return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu1, menu);
        return true;
    }

    public void buildAndInstall() {
        Intent intent = new Intent(this, ApkMakerService.class);
        startService(intent);
    }


    public void writeFile(String path, String content) {
        BufferedOutputStream bs = null;
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            bs = new BufferedOutputStream(new FileOutputStream(path, false));
            bs.write(content.getBytes()); //Byte형으로만 넣을 수 있음
        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            try {
                assert bs != null;
                bs.close(); //반드시 닫는다.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String readFile(String path) {
        String result = "";
        try {
            // 바이트 단위로 파일읽기
            FileInputStream fileStream = null; // 파일 스트림
            fileStream = new FileInputStream(path);// 파일 스트림 생성
            //버퍼 선언
            byte[] readBuffer = new byte[fileStream.available()];
            while (fileStream.read(readBuffer) != -1) ;
            result += (new String(readBuffer));
            fileStream.close(); //스트림 닫기
        } catch (Exception e) {
            e.getStackTrace();
        }
        return result;
    }


    public boolean unpackZip(String pathToZip, String destPath) {
        new File(destPath).mkdirs();
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = getAssets().open(pathToZip);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null) {
                // zapis do souboru
                filename = ze.getName();

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(destPath, filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(new File(destPath, filename));

                // cteni zipu a zapis
                while ((count = zis.read(buffer)) != -1) {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
