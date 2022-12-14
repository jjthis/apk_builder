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

import android.util.Log;

import com.android.dex.Dex;
import com.android.dx.io.DexBuffer;
import com.android.dx.merge.CollisionPolicy;
import com.android.dx.merge.DexMerger;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.SealedApkException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Created by bclinthall on 4/15/16.
 */
public class ApkMaker {
    ApkMakerOptions options;
    ProjectFiles projectFiles;
    boolean verbose;
    PrintStream out = System.out;
    PrintStream err = System.err;
    int iPercent = 0;

    public ApkMaker(ApkMakerOptions options, ProjectFiles projectFiles, PrintStream out, PrintStream err) {
        this(options, projectFiles);
        this.out = out;
        this.err = err;
    }

    public ApkMaker(ApkMakerOptions options, ProjectFiles projectFiles) {
        this.options = options;
        this.projectFiles = projectFiles;
        //verbose = options.verbose;
        verbose = true;//I don't know why, but without the debug statements, the program fails.
    }

    /*
     //This may become useful if I can get jni working. 
     private void runAapt(File aapt, File androidManifest, File resourcesArsc, File androidJar, File resDir, File genDir, Callbacks callbacks) {
        //./aapt p -f -v -M precursors/AndroidManifest.xml -F precursors/resources.arsc -I android.jar -S precursors/res -J precursors/gen
        String aaptCommand = aapt.getAbsolutePath() + " package " +
                " -f " + //force overwrite existing files;
                " -v " +//verbose
                " -M " + androidManifest.getAbsolutePath() +
                " -F " + resourcesArsc.getAbsolutePath() +  //this is where aapt will output the resource file to go in apk
                " -I " + androidJar.getAbsolutePath() +
                " -S " + resDir.getAbsolutePath() +
                " -J " + genDir.getAbsolutePath();  //where to put R.java
        //exec(aaptCommand, callbacks, totAaptLines, "Running aapt");
        exec(aaptCommand);
    }*/

    public static String getMD5Checksum(File file) throws Exception {
        byte[] b = hash("MD5", new FileInputStream(file));
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    private static byte[] hash(String alg, InputStream in) throws Exception {
        MessageDigest md = MessageDigest.getInstance(alg);
        DigestInputStream dis = new DigestInputStream(new BufferedInputStream(in), md);
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                int readCount = dis.read(buffer);
                if (readCount < 0) {
                    break;
                }
            }
            return md.digest();
        } finally {
            in.close();
        }
    }

    public void makeApk(Callbacks callbacks) {
        //needed directories;
        File temp = new File(projectFiles.outputDir, projectFiles.apkName + "_tmp");
        temp.mkdir();
        File classesDir = new File(temp, "classes");
        classesDir.mkdir();
        //File to be made
        File resourcesArsc = freshFile(temp, "resources.arsc");
        File classesDex = freshFile(temp, "classes.dex");
        File apkUnsigned = new File(options.outputFile);

        //aapt package **--auto-add-overlay** -f -m -J
        // ./gen -S ./app/src/main/res **-S "path_to_prebuilts\prebuilts\devtools\extras\android\support\v7\appcompat\res\"**
        // -I "D:\ProgramInstall\Android\Android SDK\platforms\android-21\android.jar" -M ./app/src/main/AndroidManifest.xml
        //Files passed in
        //File aapt = options.aapt == null ? null : new File(options.aapt);
        if (options.aaptRunner == null) {
            options.aaptRunner = new AaptRunner() {
                @Override
                public boolean runAapt(File androidManifest, File resourcesArsc, File androidJar, File resDir, File genDir, Callbacks callbacks) {
                    String aaptCommand = options.aapt + " package " +
                            " -f " + //force overwrite existing files;
                            " -v " +//verbose
                            " -M " + androidManifest.getAbsolutePath() +
                            " -F " + resourcesArsc.getAbsolutePath() +  //this is where aapt will output the resource file to go in apk
                            " -I " + androidJar.getAbsolutePath() +
                            " -S " + resDir.getAbsolutePath() +
                            " -J " + genDir.getAbsolutePath();  //where to put R.java
                    //exec(aaptCommand, callbacks, totAaptLines, "Running aapt");
                    int val = exec(aaptCommand);
                    return val == 0;
                }
            };
        }

        File androidJar = new File(options.androidJar);
        File androidManifest = new File(projectFiles.androidManifest);
        File resDir = new File(projectFiles.resDir);
        File assetsDir = projectFiles.assetsDir == null ? null : new File(projectFiles.assetsDir);
        File libDir = projectFiles.libDir == null ? null : new File(projectFiles.libDir);
        File srcDir = new File(projectFiles.javaDir);

        callbacks.updateProgress("Running aapt", -1);
        //File rJava = new File(srcDir, "R.java");
        options.aaptRunner.runAapt(androidManifest, resourcesArsc, androidJar, resDir, srcDir, callbacks);
        //if (rJava.exists()) {
        //    javaFiles.add(rJava);
        //}

        callbacks.updateProgress("Compiling", -1);

        runCompiler(srcDir, temp, androidJar, classesDir);

        try {
            dexLibs();
        } catch (Exception e) {
            trace(e);
            callbacks.error("Exception. Unable to dex files.libs");
        }

        callbacks.updateProgress("Dexing, Building and Signing Apk, Final Steps", -1);
        try {
            runDex(classesDex, classesDir);
        } catch (IOException e) {
            trace(e);
            callbacks.error("IOException. Unable to dex files.");
            //deleteRecursive(temp);-----------------------------------------------------------
            return;
        }

        try {
            dexMerge();
        } catch (Exception e) {
            trace(e);
            callbacks.error("Exception. Unable to dex files.libs merge");
        }

        //Use sdklib to build apk.
        logd("Building Apk", verbose);

        try {
            com.android.sdklib.build.ApkBuilder apkBuilder = new com.android.sdklib.build.ApkBuilder(apkUnsigned, resourcesArsc, classesDex, null, out);
            if (assetsDir != null) {
                File[] assetFiles = assetsDir.listFiles();
                for (File assetFile : assetFiles) {
                    apkBuilder.addFile(assetFile, "assets/" + assetFile.getName());
                }
            }
            if (libDir != null) {
                apkBuilder.addNativeLibraries(libDir);

                for (File jarLib : libDir.listFiles()) {

                    // skip native libs in sub directories
                    if (!jarLib.isFile() || !(jarLib.getName().endsWith(".jar") || jarLib.getName().endsWith(".aar"))) {
                        continue;
                    }
                    apkBuilder.addResourcesFromJar(jarLib);
                }
            }//---------------------------------------------------------------------------------------------------------------------------------------------
            apkBuilder.setDebugMode(true);
            apkBuilder.sealApk();
        } catch (ApkCreationException e) {
            callbacks.error("Unable to make apk: " + e.getMessage());
        } catch (SealedApkException e) {
            callbacks.error("Unable to make apk: " + e.getMessage());
        } catch (DuplicateFileException e) {
            e.printStackTrace();
        }


        //cleanup
        logd("cleaning up", verbose);
        if (!apkUnsigned.exists()) {
            callbacks.error("Unable to build apk.");
            //deleteRecursive(temp);-----------------------------------------------------------------------
            return;
        }
        ///deleteRecursive(temp);--------------------------------------------------------------------------
        callbacks.done(apkUnsigned);
    }

    private void dexLibs() throws Exception {
        File libDir = projectFiles.libDir == null ? null : new File(projectFiles.libDir);
        File dexedLibDir = projectFiles.dexedLibDir == null ? null : new File(projectFiles.dexedLibDir);
        if (libDir == null || dexedLibDir == null) return;
        for (File jarLib : libDir.listFiles()) {

            // skip native libs in sub directories
            if (!jarLib.isFile() || !jarLib.getName().endsWith(".jar")) {
                continue;
            }

            // compare hash of jar contents to name of dexed version
            String md5 = getMD5Checksum(jarLib);

            // check if jar is pre-dexed
            File dexLib = new File(dexedLibDir, jarLib.getName().replace(".jar", "-" + md5 + ".jar"));
            System.out.println(dexLib.getName());
            if (!dexLib.exists()) {
                com.android.dx.command.dexer.Main
                        .main(new String[]{"--verbose", "--output=" + dexLib.getPath(), jarLib.getPath()});
            }
        }
    }

    private void dexMerge() throws Exception {
        File dexedLibDir = projectFiles.dexedLibDir == null ? null : new File(projectFiles.dexedLibDir);
        if (dexedLibDir == null) return;
        File temp = new File(projectFiles.outputDir, projectFiles.apkName + "_tmp");
        File classes = new File(temp, "classes.dex");
        for (File dexLib : dexedLibDir.listFiles()) {
//            Dex merged = new DexMerger(new Dex(classes), new Dex(dexLib), CollisionPolicy.FAIL).merge();
            DexBuffer merged = new DexMerger(new DexBuffer(classes),new DexBuffer(dexLib), CollisionPolicy.FAIL).merge();
            merged.writeTo(classes);
        }
    }

    public String getJavaClassPath() {
        File libDir = projectFiles.libDir == null ? null : new File(projectFiles.libDir);
        String strClassPath = "";
        if (libDir == null) return strClassPath;
        for (File jarLib : libDir.listFiles()) {
            if (jarLib.isFile() && (jarLib.getName().endsWith(".jar") || jarLib.getName().endsWith(".aar"))) {
                strClassPath += File.pathSeparator + jarLib.getPath();
            }
        }
        return strClassPath;
    }

/*		String[] compilerArgs = {
                "-classpath", srcDir.getAbsolutePath() +/* ":" + genDir.getAbsolutePath() + ***":" + classesDir.getAbsolutePath(),
                "-verbose",
                "-bootclasspath", androidJar.getAbsolutePath(),
                "-1.6",  //java version to use
                "-target", "1.6", //target java level
                "-proc:none", //perform compilation but do not run annotation processors
                // for some reason we get an error if we try to run annotation processors:
                // java.lang.NoClassDefFoundError: org.eclipse.jdt.internal.compiler.apt.dispatch.BatchProcessingEnvImpl
                "-d", classesDir.getAbsolutePath(), //where the result will be put - makes folders to match package path
                srcDir.getAbsolutePath()
        };
*/
//String[] compilerArgs = {
//        "-showversion", "-verbose", "-deprecation",
//        "-bootclasspath", androidJar.getAbsolutePath(),
//        "-1.5",  //java version to use
//        "-cp", getJavaClassPath(),
//        "-d", classesDir.getAbsolutePath(), //where the result will be put - makes folders to match package path
//        genDir.getAbsolutePath(),
//        srcDir.getAbsolutePath()
//};genDir.getAbsolutePath()
    private void runCompiler(File srcDir, File genDir, File androidJar, File classesDir/*, List<File> javaFiles*/) {

        String[] compilerArgs = {
                "-classpath", srcDir.getAbsolutePath() + ":" + getJavaClassPath() + ":" + classesDir.getAbsolutePath(),
                "-verbose",
                "-bootclasspath", androidJar.getAbsolutePath(),
                "-1.6",  //java version to use
                "-target", "1.6", //target java level
                "-proc:none", //perform compilation but do not run annotation processors
                // for some reason we get an error if we try to run annotation processors:
                // java.lang.NoClassDefFoundError: org.eclipse.jdt.internal.compiler.apt.dispatch.BatchProcessingEnvImpl
                "-d", classesDir.getAbsolutePath(), //where the result will be put - makes folders to match package path
                srcDir.getAbsolutePath()
        };
//        src $(find src -type f -name "*.java")

        logd("Compiling", verbose);
        boolean systemExitWhenFinished = false;//
        org.eclipse.jdt.internal.compiler.batch.Main compiler = new org.eclipse.jdt.internal.compiler.batch.Main(new PrintWriter(out), new PrintWriter(err){
            @Override
            public void println(String x) {
                super.println(x);
                //Logger.printing(x+"\n", null);
            }
        }, systemExitWhenFinished, null, null);
        if (compiler.compile(compilerArgs)) {
            logd("Compile Success", verbose);
        } else {
            logd("Compile Error", verbose);
        }

    }



    // appcompat class injection trying
    private void runDex(File classesDex, File classesDir) throws IOException {
        //run dex //for this one it's important that you be inside precursors/classes so the path to MainActivity.class matches the package name in the file.
        //java -cp /Users/bclinthall/AndroidStudioProjects/IconEffects/app/src/main/assets/dx.jar  com.android.dx.command.dexer.Main --output ../../precursors/MainActivity.dex com/theaetetuslabs/charlie/MainActivity.class
        //it might be best to run this as commandline
        String[] dexArgs = //"java -cp " + dx.getAbsolutePath() + "com.android.dx.command.dexer.Main" +
                {"--output", classesDex.getAbsolutePath(),
                        classesDir.getAbsolutePath()};
        logd("Dexing", verbose);
        com.android.dx.command.dexer.Main.main(dexArgs);

    }

    //StackOverflow: http://codereview.stackexchange.com/questions/8835/java-most-compact-way-to-print-inputstream-to-system-out
    public long copyStream(InputStream is, OutputStream os) {
        final int BUFFER_SIZE = 8192;

        byte[] buf = new byte[BUFFER_SIZE];
        long total = 0;
        int len = 0;
        try {
            while (-1 != (len = is.read(buf))) {
                os.write(buf, 0, len);
                total += len;
            }
        } catch (IOException ioe) {
            throw new RuntimeException("error reading stream", ioe);
        }
        return total;
    }

    private void readStream(String msg, InputStream stream) throws IOException {
        if (verbose) {
            out.append(msg);
            copyStream(stream, out);
        }
    }

    private void readStream(String msg, InputStream stream, Callbacks callbacks, int totalLines, String text) throws IOException {
        if (verbose) {
            out.println(msg);
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        IOException exception = null;
        float lineCt = 0;
        iPercent = 0;
        try {
            do {
                line = reader.readLine();
                if (verbose) {
                    out.println(line);
                }
                lineCt++;
                float percent = lineCt / totalLines;
                int newIPercent = (int) (100 * lineCt / totalLines);
                if (newIPercent != iPercent) {
                    callbacks.updateProgress(text, percent);
                    iPercent = newIPercent;
                }
            } while (line != null);
        } catch (IOException e) {
            exception = e;
        } finally {
            reader.close();
        }
        if (exception != null) {
            throw exception;
        }


    }

    private void readStreams(Process pp, String name) {
        if (verbose) {
            try {
                readStream("------InputStream, " + name + "\n", pp.getInputStream());
                readStream("------ErrorStream, " + name + "\n", pp.getErrorStream());
                pp.waitFor();
            } catch (IOException e) {
                trace(e);
            } catch (InterruptedException e) {
                trace(e);
            }
        }
    }

    private void readStreams(Process pp, String name, Callbacks callbacks, int totalLines, String text) {
        try {
            readStream("------InputStream, " + name + "\n", pp.getInputStream(), callbacks, totalLines, text);
            readStream("------ErrorStream, " + name + "\n", pp.getErrorStream());
            pp.waitFor();
        } catch (IOException e) {
            trace(e);
        } catch (InterruptedException e) {
            trace(e);
        }
    }

    public int exec(String command) {
        logd("exec: " + command, verbose);
        try {
            Process pp = Runtime.getRuntime().exec(command);
            readStreams(pp, command);
            return pp.exitValue();
        } catch (Exception e) {
            trace(e);
            return -1;
        }
    }

    private void exec(String command, Callbacks callbacks, int totalLines, String text) {
        logd("exec: " + text + command, verbose);
        try {
            Process pp = Runtime.getRuntime().exec(command);
            readStreams(pp, command, callbacks, totalLines, text);
        } catch (Exception e) {
            trace(e);
        }
    }

    private File freshFile(File dir, String name) {
        File f = new File(dir, name);
        if (f.exists()) {
            deleteRecursive(f);
        }
        return f;
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteRecursive(f);
            }
        } else {
            file.delete();
        }
    }

    void trace(Exception e) {
        Logger.trace(e, err);
    }

    void logd(String msg, boolean verbose) {
        Logger.logd(msg, verbose, out);
    }


    public interface Callbacks {
        void updateProgress(String msg, float percent);

        void error(String str);

        void done(File apk);
    }


    public interface AaptRunner {
        public boolean runAapt(File androidManifest, File resourcesArsc, File androidJar, File resDir, File genDir, Callbacks callbacks);
    }

    public static class ApkMakerOptions {
        public String aapt = "aapt";
        public AaptRunner aaptRunner;
        public String androidJar = "android.jar";
        public String projectDir;
        public String outputFile;
        public boolean verbose = false;

        {
            if (File.separator.equals("/")) {
                outputFile = "~/com.theaetetuslabs.apkmaker/newapk.apk";
            } else {
                outputFile = "%UserProfile%\\com.theaetetuslabs.apkmaker\\newapk.apk";
            }
        }

        void verifyOne(String path, String which) throws FileNotFoundException {
            if (!new File(path).exists()) {
                throw new FileNotFoundException("Listed " + which + ", " + path + " not found");
            }
        }

        void verifyAll() throws FileNotFoundException {
            verifyOne(projectDir, "project directory");
            verifyOne(androidJar, "android.jar");
            if (aaptRunner == null) {
                verifyOne(aapt, "aapt binary file");
            }
        }
    }

    static class ProjectFiles {
        String resDir;
        String javaDir;
        String assetsDir;
        String androidManifest;
        String outputDir;
        String apkName;
        String libDir;
        String dexedLibDir;

        public ProjectFiles(ApkMakerOptions options) throws Exception {
            setProjectDir(options.projectDir);
            setOutputDir(options.outputFile);
        }

        private String getFilePath(String label, File parent, String fileName) throws Exception {
            File file = new File(parent, fileName);
            if (!file.exists()) {
                throw new Exception(label + " does not exist in " + parent.getAbsolutePath());
            } else {
                return file.getAbsolutePath();
            }
        }

        private void setProjectDir(String projectDir) throws Exception {
            File projectDirFile = new File(projectDir);
            androidManifest = getFilePath("AndroidManifest.xml", projectDirFile, "AndroidManifest.xml");
            resDir = getFilePath("Directory \"res\"", projectDirFile, "res");
            File assetsDirFile = new File(projectDirFile, "assets");
            assetsDir = assetsDirFile.exists() ? assetsDirFile.getAbsolutePath() : null;
            File libDirFile = new File(projectDirFile, "libs");
            libDir = libDirFile.exists() ? libDirFile.getAbsolutePath() : null;
            File dexedLibDirFile = new File(projectDirFile, "dexedLibs");
            dexedLibDir = dexedLibDirFile.exists() ? dexedLibDirFile.getAbsolutePath() : null;
            javaDir = getFilePath("Directory \"java\"", projectDirFile, "java");
        }

        private void setOutputDir(String outputFile) throws Exception {
            File outputFileFile = new File(outputFile);
            File outputDirFile = new File(outputFile).getParentFile();
            if (!outputDirFile.canWrite()) {
                throw new Exception("Cannot write to output directory " + outputDirFile.getAbsolutePath());
            }
            outputDir = outputDirFile.getAbsolutePath();
            apkName = outputFileFile.getName();
        }
    }
}
