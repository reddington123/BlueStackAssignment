package com.example.libarchitecture;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    private static final int REQUEST_CODE_PICK_FILE=1;
    private static final int PERMISSION_REQUEST_CODE=2;
    private static String TAG = MainActivity.class.getName();

    static RandomAccessFile libFile = null;
    private static final byte[] buffer = new byte[512];
    private static int endian;
    private static int type;
    /** The magic values for the ELF identification. */
    private static final byte[] ELFMAG = {
            (byte) 0x7F, (byte) 'E', (byte) 'L', (byte) 'F', };

    private static final int EI_NIDENT = 16; /* size of e_ident array*/

    private static final int EM_386 = 3;    /* x86 lib Architecture*/
    private static final int EM_MIPS = 8;   /* mips lib Architecture*/
    private static final int EM_ARM = 40;   /* armeabi-v7a lib Architecture*/
    private static final int EM_X86_64 = 62;    /* x86_64 lib Architecture */
    private static final int EM_AARCH64 = 183;  /* arm64-v8a lib Architecture */

    private static final int EI_CLASS = 4;
    private static final int ELFCLASSNONE = 0;  /* Invalid class */
    private static final int ELFCLASS32 = 1;    /*32 bit objects*/
    private static final int ELFCLASS64 = 2;    /*64 bit objects*/

    private static final int EI_DATA = 5;
    private static final int ELFDATANONE = 0;   /*Invalid data encoding*/
    private static final int ELFDATA2LSB = 1;   /*Little Endian*/
    private static final int ELFDATA2MSB = 2;   /*Big Endian*/

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK) {
        /*
        // Using Storage Access Framwork is in progress
        if (requestCode == PICKFILE_REQUEST_CODE
                && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
            }*/
            String path = Environment.getExternalStorageDirectory().getPath();
            path = path + "/Download";
            File directory = new File(path);
            File[] files = directory.listFiles();
            String[][] newArray = new String[files.length][2];
            int len = 0;
            for (int i = 0; i < files.length; i++) {
                try {
                    String filepath = path + "/" + files[i].getName();
                    libFile = new RandomAccessFile(filepath, "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    String abi = readLibHeader(libFile, files[i].getName());
                    if (abi.equals("x86") || abi.equals("x86_64") || abi.equals("armeabi-v7a") || abi.equals("arm64-v8a") || abi.equals("mips")) {
                        newArray[len][0] = files[i].getName();
                        newArray[len][1] = abi;
                        len = len + 1;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(len>0 && len!=newArray.length) {
                String[][] finalResult = new String[len][2];
                for (int i = 0; i < len; i++) {
                    finalResult[i][0] = newArray[i][0];
                    finalResult[i][1] = newArray[i][1];
                }
                mAdapter = new MyAdapter(finalResult, this);
                recyclerView.setAdapter(mAdapter);
            }else if(len>0 && len==newArray.length){
                mAdapter = new MyAdapter(newArray, this);
                recyclerView.setAdapter(mAdapter);
            }else{
                Toast.makeText(this,"No Shared library found in Download Folder",Toast.LENGTH_SHORT).show();
            }
        }
    }
    private static String readLibHeader(RandomAccessFile libFile, String content) throws IOException {
        libFile.seek(0);
        libFile.readFully(buffer,0,EI_NIDENT);

        //Checking the Magic ELF Format first 4 bytes must be 7f 45 4c 46
        if (buffer[0] != ELFMAG[0] || buffer[1] != ELFMAG[1] ||
                buffer[2] != ELFMAG[2] || buffer[3] != ELFMAG[3]) {
            return "Invalid file format";
            //throw new IllegalArgumentException("Invalid File format: " + libFile.toString());
        }
        int elfClass = buffer[EI_CLASS];
        if(elfClass!=ELFCLASS32 && elfClass!=ELFCLASS64){
            return "Invalid elfClass";
            //throw new IOException("Invalid ELF EI_CLASS: " + elfClass);
        }
        endian = buffer[EI_DATA];
        if (endian != ELFDATA2LSB && endian!=ELFDATA2MSB) {
            return "Invalid endian";
            //throw new IOException("Invalid ELF EI_DATA: " + endian);
        }
        type=readHalf();
        int e_machine = readHalf();
        if (e_machine != EM_386 && e_machine != EM_X86_64 &&
                e_machine != EM_AARCH64 && e_machine != EM_ARM &&
                e_machine != EM_MIPS) {
            return "Invalid e_machine :"+e_machine;
            //throw new IOException("Invalid ELF e_machine: " +e_machine);
        }
        // Reject the combinations which are unsupported
        if ((e_machine == EM_386 && elfClass != ELFCLASS32) ||
                (e_machine == EM_X86_64 && elfClass != ELFCLASS64) ||
                (e_machine == EM_AARCH64 && elfClass != ELFCLASS64) ||
                (e_machine == EM_ARM && elfClass != ELFCLASS32) ) {
                return "Invalid combination of e_machine :"+e_machine+" elfClass :"+elfClass;
            //throw new IOException("Invalid combination of e_machine = "+e_machine+" elfClass = "+elfClass);
        }
        return convertMachineNametoABI(e_machine);
    }
    private static String convertMachineNametoABI(int e_machine) {
        if(e_machine==EM_386){
            return "x86";
        }else if(e_machine==EM_MIPS){
            return "mips";
        }else if(e_machine==EM_ARM){
            return "armeabi-v7a";
        }else if(e_machine==EM_X86_64){
            return "x86_64";
        }else if(e_machine==EM_AARCH64){
            return "arm64-v8a";
        }else{
            return "Invalid abi";
        }
    }

    private static int readHalf() throws IOException {
        return (int) readX(2);
    }

    private static long readX(int byteCount) throws IOException {
        libFile.readFully(buffer, 0, byteCount);

        int answer = 0;
        if (endian == ELFDATA2LSB) {
            for (int i = byteCount - 1; i >= 0; i--) {
                answer = (answer << 8) | (buffer[i] & 0xff);
            }
        } else {
            final int N = byteCount - 1;
            for (int i = 0; i <= N; ++i) {
                answer = (answer << 8) | (buffer[i] & 0xff);
            }
        }
        return answer;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new MyAdapter(null, this);
        recyclerView.setAdapter(mAdapter);
    }
    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Read External Storage permission allows us to read files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"Permission Granted for reading local storage",Toast.LENGTH_SHORT).show();
            } else {
                 Toast.makeText(this,"Permission Denied for reading local storage",Toast.LENGTH_SHORT).show();
            }
            break;
        }
    }
    public void selectFolder(View v){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}