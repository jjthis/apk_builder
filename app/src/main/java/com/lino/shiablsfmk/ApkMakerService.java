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

import android.app.AlertDialog.Builder;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.theaetetuslabs.android_apkmaker.AndroidApkMaker;
import com.theaetetuslabs.android_apkmaker.AndroidApkMaker.AfterInstallDialogAdder;
import com.theaetetuslabs.android_apkmaker.InstallActivity;

import org.zeroturnaround.zip.ZipUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * Created by bclinthall on 9/20/16.
 */
//aapt package **--auto-add-overlay** -f -m -J
// ./gen -S ./app/src/main/res **-S "path_to_prebuilts\prebuilts\devtools\extras\android\support\v7\appcompat\res\"**
// -I "D:\ProgramInstall\Android\Android SDK\platforms\android-21\android.jar" -M ./app/src/main/AndroidManifest.xml
public class ApkMakerService extends IntentService {
    //For use with the IntentService(String name) constructor.  Important for debugging only.
    public static final String SERVICE_NAME = "com.theaetetuslabs.apkmakertester.ApkMakerService";
    public static final String TAG = "ApkMakerService";

    public ApkMakerService() {
        super(SERVICE_NAME);
    }

    public static void moveAsset(Context context, String assetName, File destDir) throws IOException {
        File dest = new File(destDir, assetName);
        try {
            //InputStream assetIn = context.getAssets().open(assetName);
            InputStream assetIn = new FileInputStream(MainActivity.MyWorkFolder + "Temp/" + assetName);
            dest.createNewFile();
            int length = 0;
            byte[] buffer = new byte[4096];
            FileOutputStream rawOut = new FileOutputStream(dest);
            while ((length = assetIn.read(buffer)) > 0) {
                rawOut.write(buffer, 0, length);
            }
            rawOut.close();
            assetIn.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not move " + assetName + "!");
            e.printStackTrace();
            throw new IOException(e);
        }

    }

    //modified from http://stackoverflow.com/a/10997886/3000692
    public static boolean unpackZip(String pathToZip, String destPath) {
        new File(destPath).mkdirs();
        InputStream is;
        ZipInputStream zis;
        try {
            String filename;
            is = new FileInputStream(pathToZip);
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

    @Override
    protected void onHandleIntent(Intent intent) {
        final NotificationManager mNotifyManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, "com.lino.shiablsfmk");
        mBuilder.setContentTitle("앱 빌드 중")
                .setContentText("시작 하는 중")
                .setSmallIcon(R.drawable.ic_build);
        startForeground(AndroidApkMaker.ONGOING_NOTIFICATION_ID, mBuilder.build());
        final File filesDir = getFilesDir();
        Log.i(TAG, "addToAfterInstallDialog: delete Cache");
        deleteFolder(filesDir+"/test_project");

        try {
            deleteFolder(MainActivity.MyWorkFolder + MainActivity.APP_NAME + "/build");
            copyDirectory(new File(filesDir+"/buildStuff/apks")
                    ,new File(MainActivity.MyWorkFolder + MainActivity.APP_NAME + "/build"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        deleteFolder(filesDir+"/buildStuff/apks");
        //clearing


        try {
            //zipDir(MainActivity.MyWorkFolder+MainActivity.APP_NAME+"/src", MainActivity.MyWorkFolder+"Temp/test.zip");
            //고장 다른거 찾으셈
            ZipUtil.pack(new File(MainActivity.MyWorkFolder + MainActivity.APP_NAME + "/src"),
                    new File(MainActivity.MyWorkFolder + "Temp/test.zip"));
            moveAsset(this, "test.zip", filesDir);
            new File(MainActivity.MyWorkFolder + "Temp/test.zip").delete();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
//        try {
//            moveAsset(this, "test.zip", filesDir);
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
        File projectDir = new File(filesDir, "test_project");
        unpackZip(new File(getFilesDir(), "test.zip").getAbsolutePath(), projectDir.getAbsolutePath());
        //unpackZipAss("AppCompat.jar", getFilesDir().getAbsolutePath()+"buildStuff/classes");


        AfterInstallDialogAdder adder = new AfterInstallDialogAdder() {
            @Override
            public void addToAfterInstallDialog(Builder builder, final InstallActivity installActivity) {
                builder.setMessage("설치 완료.");
            }
        };
        try {
            new AndroidApkMaker(
                    this,
                    mNotifyManager,
                    mBuilder)
                    .make(MainActivity.APP_NAME + " apk",
                            MainActivity.APP_PACKAGE_NAME,
                            projectDir.getAbsolutePath(),
                            /*verbose */ true,
                            adder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //핵심
    }

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

    public void copyDirectory(File sourcelocation , File targetdirectory)
            throws IOException {
        //디렉토리인 경우
        if (sourcelocation.isDirectory()) {
            //복사될 Directory가 없으면 만듭니다.
            if (!targetdirectory.exists()) {
                targetdirectory.mkdirs();
            }

            String[] children = sourcelocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourcelocation, children[i]),
                        new File(targetdirectory, children[i]));
            }
        } else {
            //파일인 경우
            InputStream in = new FileInputStream(sourcelocation);
            OutputStream out = new FileOutputStream(targetdirectory);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public boolean unpackZipAss(String pathToZip, String destPath) {
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
