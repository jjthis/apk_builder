package com.lino.shiablsfmk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class log extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loglayout);
        final TextView textView = findViewById(R.id.texts);
        Button btn1 = findViewById(R.id.save);
        Button btn2 = findViewById(R.id.clear);
        Button btn3 = findViewById(R.id.refresh);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeFile(MainActivity.MyWorkFolder+"log.txt", MainActivity.texts+"\n\n\n\n");
                Toast.makeText(log.this, "Saved", Toast.LENGTH_SHORT).show();
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.texts = new StringBuilder("<------------log----------->\n");
                textView.setText(MainActivity.texts);
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText(MainActivity.texts);
                Toast.makeText(log.this, "Cleared", Toast.LENGTH_SHORT).show();
            }
        });
        textView.setText(MainActivity.texts);
        textView.setTextIsSelectable(true);
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
            bs = new BufferedOutputStream(new FileOutputStream(path, true));
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
}
