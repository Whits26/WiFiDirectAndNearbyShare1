package com.example.android.wifidirect;

import static com.example.android.wifidirect.NearbyService.BROADCAST_ACTION;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.nearby.connection.Payload;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class NearbyShare extends Activity {
    public static final String TAG = "nearby";

    static String startTime;
    static TextView timestamp;
    Button Osfbc;
    Button Osfbc1;
    Button eft;

    RecyclerView recyclerView;

    private static final int REQUEST_OPEN_DOCUMENT = 20;
    private static final int REQUEST_OPEN_DOCUMENT1 = 30;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final String LOCAL_ENDPOINT_NAME = Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE;
    private TextView tvBytesReceived;


    private NearbyService mService;
    private boolean mBound = false;
    private MyAdapter mAdapter;


    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_nearby_share);

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }

        timestamp = findViewById(R.id.timeStamp);
        TextView tvLocalEndpointName = findViewById(R.id.local_endpoint_name);
        tvLocalEndpointName.setText(getString(R.string.local_endpoint_name, LOCAL_ENDPOINT_NAME));
        tvBytesReceived = findViewById(R.id.bytes_received);

        recyclerView = findViewById(R.id.my_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new MyAdapter(this);
        recyclerView.setAdapter(mAdapter);

        Osfbc = findViewById(R.id.onSendFileButtonClicked);
        Osfbc1 = findViewById(R.id.onSendFileButtonClicked1);
        eft = findViewById(R.id.endFileTransfer);

        Osfbc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_OPEN_DOCUMENT);
            }
        });

        Osfbc1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                startActivityForResult(intent, REQUEST_OPEN_DOCUMENT1);
            }
        });

        eft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.onDestroy();
                recyclerView.clearOnChildAttachStateChangeListeners();
                mService.startAdvertising1();
            }
        });

        mAdapter.setOnItemClickListener(new MyAdapter.ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (position == 0) {
                    connect(position);
                    Toast.makeText(NearbyShare.this, "item " + position + "clicked", Toast.LENGTH_SHORT).show();
                }else {
                    connect1(position);
                    Toast.makeText(NearbyShare.this, "item " + position + "clicked", Toast.LENGTH_SHORT).show();

                }
            }

        });

        //discoverer and receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(NearbyShare.this, NearbyService.class));
        } else {
            startService(new Intent(NearbyShare.this, NearbyService.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NearbyService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            NearbyService.LocalBinder binder = (NearbyService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mAdapter.setmDataset(mService.getStatus());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (mBound) mService.reportConnectStatus();
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, intentFilter);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mAdapter.setmDataset(mService.getStatus());
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    public void onSendBytesButtonClicked(View view) {
        String str = String.valueOf(new Random().nextInt(100));
        mService.sendStringPayload(str);
    }


    //Getting and setting Time
    public static String getTime1() {

        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS Z", Locale.getDefault()).
                format(new Date());

    }

    //image
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_OPEN_DOCUMENT
                && resultCode == Activity.RESULT_OK
                && resultData != null) {

            Uri uri2 = resultData.getData();

            ImageView imageView2 = findViewById(R.id.imageV1);
            findViewById(R.id.imageV1).setVisibility(View.VISIBLE);
            imageView2.setImageURI(uri2);

            Log.d(TAG, "uri img = " + uri2);
            Payload filePayload;
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri2, "r");
                filePayload = Payload.fromFile(pfd);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found", e);
                return;
            }

            //Timestamp
            timestamp.setText(getTime1());
            //Run Timestamp - Prints in run screen
            startTime = getTime1();
            Log.d("START TIME: ", String.valueOf(startTime));


            mService.sendFilePayload(filePayload);

        }

        // document
        if (requestCode == REQUEST_OPEN_DOCUMENT1 && resultCode == Activity.RESULT_OK && resultData != null) {

            Uri uri3 = resultData.getData();

            Log.d(TAG, "uri doc = " + uri3);
            Payload filePayload;
            try {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri3, "r");
                filePayload = Payload.fromFile(pfd);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found", e);
                return;
            }

            //Timestamp
            timestamp.setText(getTime1());
            //Run Timestamp - Prints in run screen
            startTime = getTime1();
            Log.d("START TIME: ", String.valueOf(startTime));


            mService.sendFilePayload(filePayload);

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) unbindService(connection);
        mBound = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    public void connect(int position) {
        mService.connect(position);
    }

    public void connect1(int position) {
        mService.connect1(position);
    }

    public void disconnect(int position) {
        mService.disconnect(position);
    }


    public void onStartButtonClick(View view) {
        mService.startDiscovery();
    }


    public void onStopButtonClick(View view) {
        finish();
        mService.onDestroy();
        System.exit(0);
    }


}


