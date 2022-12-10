package com.lino.shiablsfmk;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class sourceEditor extends AppCompatActivity {

    ArrayAdapter<String> modeAdapter;
    String dir;
    String[] stringArray;
    TextView path, loads;
    LineNumberedEditText editText;
    ListView list;
    CheckBox checkBox;
    ClearEditText editTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawerlay);
        Toolbar toolbar = findViewById(R.id.toolbar);
        dir = MainActivity.WorkingFile.replace("/MainActivity.java", "");
        setSupportActionBar(toolbar);

        editText = findViewById(R.id.sourceEditor);
        loads = findViewById(R.id.loadtext);
        readFile(MainActivity.WorkingFile, editText,loads);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                writeFile(MainActivity.WorkingFile, editText.getText().toString());
                Toast.makeText(sourceEditor.this, "저장 완료", Toast.LENGTH_SHORT).show();
            }
        });
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.app_name, R.string.app_name);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size);


//        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
//        View headerLayout =
//                navigationView.inflateHeaderView(R.layout.drawmain);

        final LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        param2.leftMargin = 40;
        param2.rightMargin = 40;
        param2.topMargin = 40;
        final LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        param1.leftMargin = 40;
        param1.rightMargin = 40;

        TextView createF = findViewById(R.id.newfile);
        path = findViewById(R.id.Path);
        createF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout lay = new LinearLayout(sourceEditor.this);
                lay.setOrientation(1);


                checkBox = new CheckBox(sourceEditor.this);
                checkBox.setChecked(true);
                checkBox.setText("File");
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(b)
                            checkBox.setText("File");
                        else checkBox.setText("Folder");
                    }
                });
                checkBox.setLayoutParams(param2);
                editTexts = new ClearEditText(sourceEditor.this);
                editTexts.setHint("이름을 입력하세요...");
                editTexts.setSingleLine();
                editTexts.setLayoutParams(param1);
                lay.addView(checkBox);
                lay.addView(editTexts);
                AlertDialog.Builder dialog = new AlertDialog.Builder(sourceEditor.this);
                dialog.setTitle("New File/Folder");
                dialog.setView(lay);
                dialog.setNegativeButton("취소", null);
                dialog.setPositiveButton("만들기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String name = editTexts.getText().toString();
                        if (!name.matches("[0-9a-zA-Z가-힣ㄱ-ㅎㅏ-ㅣ.]+")) {
                            Toast.makeText(sourceEditor.this, "이름은 숫자, 영어, 한글 .만 가능합니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File makef = new File(dir+"/"+name);
                        if(checkBox.isChecked()){
                            try {
                                makef.createNewFile();
                                MainActivity.WorkingFile = dir + "/" + name;
                                readFile(MainActivity.WorkingFile, editText, loads);
                                drawer.closeDrawers();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{
                            makef.mkdir();
                        }
                        stringArray = new File(dir).list();
                        modeAdapter = new ArrayAdapter<String>(sourceEditor.this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                        modeAdapter.notifyDataSetChanged();
                        list.setAdapter(modeAdapter);
                    }
                });
                dialog.show();
            }
        });
        final TextView updir = findViewById(R.id.updir);
        updir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(dir.equals("sdcard")){
                    Toast.makeText(sourceEditor.this, "sdcard 입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                dir = new File(dir).getParent();
                stringArray = new File(dir).list();
                modeAdapter = new ArrayAdapter<String>(sourceEditor.this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                modeAdapter.notifyDataSetChanged();
                list.setAdapter(modeAdapter);
                path.setText("Path: " + dir);
            }
        });
        list = findViewById(R.id.lists);
        stringArray = new File(dir).list();
        modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
        list.setAdapter(modeAdapter);
        path.setText("Path: " + dir);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (new File(dir + "/" + stringArray[i]).isDirectory()) {
                    dir = new File(dir, stringArray[i]).getAbsolutePath();
                    stringArray = new File(dir).list();
                    modeAdapter = new ArrayAdapter<String>(sourceEditor.this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                    modeAdapter.notifyDataSetChanged();
                    list.setAdapter(modeAdapter);
                    path.setText("Path: " + dir);
                } else {
                    MainActivity.WorkingFile = dir + "/" + stringArray[i];
                    readFile(MainActivity.WorkingFile, editText, loads);
                    drawer.closeDrawers();
                }
            }
        });

        list.setAdapter(modeAdapter);
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                final LinearLayout lay = new LinearLayout(sourceEditor.this);
                lay.setOrientation(1);
                Button btn = new Button(sourceEditor.this);
                btn.setText("Delete");
                btn.setTransformationMethod(null);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        deleteFolder(dir + "/" + stringArray[i]);
                        Toast.makeText(sourceEditor.this, "Deleted " + stringArray[i], Toast.LENGTH_SHORT).show();
                        stringArray = new File(dir).list();
                        modeAdapter = new ArrayAdapter<String>(sourceEditor.this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                        modeAdapter.notifyDataSetChanged();
                        list.setAdapter(modeAdapter);
                        return;
                    }
                });
                btn.setLayoutParams(param2);
                lay.addView(btn);

                Button btn2 = new Button(sourceEditor.this);
                btn2.setText("Rename/Move");
                btn2.setTransformationMethod(null);
                btn2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LinearLayout lays = new LinearLayout(sourceEditor.this);
                        lays.setOrientation(1);
                        final ClearEditText number = new ClearEditText(sourceEditor.this);
                        number.setSingleLine();
                        number.setHint("바꿀이름을 입력하세요...");
                        number.setText(dir + "/" + "rename.txt");
                        number.setLayoutParams(param2);
                        lays.addView(number);
                        AlertDialog.Builder dialog = new AlertDialog.Builder(sourceEditor.this);
                        dialog.setTitle("New File/Folder");
                        dialog.setView(lays);
                        dialog.setNegativeButton("취소", null);
                        dialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int iii) {
                                if(new File(number.getText().toString()).exists()){
                                    Toast.makeText(sourceEditor.this, "이미 있는 이름이네요 ㅎㅎ.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                new File(dir + "/" + stringArray[i]).renameTo(new File(number.getText().toString()));
                                Toast.makeText(sourceEditor.this, "Renamed", Toast.LENGTH_SHORT).show();
                                stringArray = new File(dir).list();
                                modeAdapter = new ArrayAdapter<String>(sourceEditor.this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                                modeAdapter.notifyDataSetChanged();
                                list.setAdapter(modeAdapter);
                                return;
                            }
                        });
                        dialog.show();
                    }
                });
                btn2.setLayoutParams(param1);
                lay.addView(btn2);


                AlertDialog.Builder dialog = new AlertDialog.Builder(sourceEditor.this);
                dialog.setTitle("Select Option");
                dialog.setView(lay);
                dialog.setNegativeButton("닫기", null);
                dialog.show();
                return true;
            }
        });
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

    public void readFile(final String path, final EditText edit, final TextView text) {
        edit.setText("");
        text.setText("로딩중!");
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new FileReader(path));

                    try {
                        String line = br.readLine();

                        while (line != null) {
                            Thread.sleep(10);
                            final String finalLine = line;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    edit.append(finalLine + "\n");
                                }
                            });
                            line = br.readLine();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        br.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text.setText("로딩완료!");
                    }
                });
            }
        }).start();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
//            AlertDialog.Builder dialog = new AlertDialog.Builder(sourceEditor.this);
//            dialog.setTitle("Save File");
//            dialog.setMessage("파일을 저장하시겠습니까?");
//            dialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    finish();
//                }
//            });
//            dialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    writeFile(MainActivity.WorkingFile, editText.getText().toString());
//                    Toast.makeText(sourceEditor.this, "Saved ", Toast.LENGTH_SHORT).show();
//                    finish();
//                }
//            });
//            dialog.show();
            super.onBackPressed();
        }
    }

    public static void deleteFolder(String path) {

        File folder = new File(path);
        if(folder.isFile()){
            folder.delete();
            return;
        }
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


}
