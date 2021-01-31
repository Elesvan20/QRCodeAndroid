package com.example.qrscanner;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.icu.text.SymbolTable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidmads.library.qrgenearator.QRGSaver;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.WriterException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {
    private CodeScanner mCodeScanner;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    int number = 0;
    StringBuffer encoded_string;
    String image_name;
    Bitmap bitmap;
    Bitmap bitmapDatabase;
    File file;
    Uri file_uri;
    File imageFile;
    ArrayList<String> QRCodes = new ArrayList<>();
    String savePath = Environment.getExternalStorageDirectory().getPath() + "/QR/";
    byte[] array;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        mCodeScanner = new CodeScanner(this, scannerView);
        mCodeScanner.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull final Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
                        Display display = manager.getDefaultDisplay();
                        Point point = new Point();
                        display.getSize(point);
                        int width = point.x;
                        int height = point.y;
                        int smallerDimension = width < height ? width : height;
                        smallerDimension = smallerDimension * 3 / 4;

                        QRGEncoder qrgEncoder = new QRGEncoder(result.getText(), null, QRGContents.Type.TEXT, smallerDimension);
                        Bitmap bitmap = null;
                        onPause();

                        try {
                            bitmap = qrgEncoder.encodeAsBitmap();
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                        image_name =  "QRCode" + number + ".png";
                        file = new File(savePath + File.separator + image_name);
                        file_uri = Uri.fromFile(file);

                        //QRCodes.add(imageFile.getAbsolutePath());
                       //Toast.makeText(getApplicationContext(), imageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

                        boolean save;
                        try {
                            save = QRGSaver.save(savePath, "QRCode" + number, bitmap, QRGContents.ImageType.IMAGE_PNG);
                            String result = save ? "Image Saved" : "Image Not Saved";
                            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                        } catch (WriterException e) {
                            e.printStackTrace();
                        }
                        new Encode_image().execute();

                        number++;
                        onResume();
                    }
                });
            }
        });
    }

    private class Encode_image extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            bitmapDatabase = BitmapFactory.decodeFile(file_uri.getPath());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmapDatabase.compress(Bitmap.CompressFormat.PNG,100,stream);

            array = stream.toByteArray();
            encoded_string = new StringBuffer(Base64.encodeToString(array,0));

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            makeRequest();
        }
    }

    private void makeRequest(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, "http://127.0.0.1:8080/qrcodes/connection.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println("WE GOT RESPONSE :"+response);
                        //Toast.makeText(getApplicationContext(), "Response: "+response, Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(getApplicationContext(), "ERROR RESPONSE2", Toast.LENGTH_LONG).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                HashMap<String,String> map = new HashMap<>();
                map.put("encoded_string",encoded_string.toString());
                map.put("image_name",image_name);
                /*System.out.println("========================GET PARAMS DEBUG===================\n");
                System.out.println("Encoded string " +encoded_string.toString());
                System.out.println("Image Name " +image_name);*/

                return map;
            }
        };
        requestQueue.add(request);

    }


    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }

}