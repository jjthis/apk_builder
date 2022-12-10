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

package com.theaetetuslabs.java_apkmaker;

import android.widget.TextView;

import com.lino.shiablsfmk.MainActivity;
import com.lino.shiablsfmk.R;
import com.lino.shiablsfmk.log;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by bclinthall on 8/19/16.
 */
public class Logger{

    public static void trace(Exception e, boolean verbose, PrintStream err){
        if(verbose) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            printing(errors.toString(),err);
        }
    }
    public static void systemOutLog(String str){
        printing(str, System.out);
    }
    public static void trace(Exception e, PrintStream err){
        trace(e, true, err);
    }
    public static void logd(String msg, boolean verbose, PrintStream out){
        if(verbose){
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            printing(msg + " -- " + stack[2].toString(), out);
        }
    }
    public static void logd2(String msg, boolean verbose, PrintStream out){
        if(verbose){
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            printing( "<<<<<<<<<<", out);
            printing( msg , out);
            for(int i=0; i<stack.length; i++){
                printing( " -- "+i+": " + stack[i].toString(), out);
            }
            printing( ">>>>>>>>>>", out);
        }
    }

    public static void printing(String str, PrintStream out){
        MainActivity.texts.append("\n\n").append(str);
        if(out != null)
        out.println(str);
    }


}
