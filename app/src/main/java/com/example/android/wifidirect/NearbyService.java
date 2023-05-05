package com.example.android.wifidirect;

import static com.example.android.wifidirect.NearbyShare.getTime1;
import static com.example.android.wifidirect.NearbyShare.timestamp;
import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.File;

public class NearbyService extends Service {
    private static final String TAG = "NearbyService";
    private static final Strategy STRATEGY = Strategy.P2P_POINT_TO_POINT;
    private static final String SERVICE_ID = "com.google.example.myapp";
    private static final String LOCAL_ENDPOINT_NAME = "[Phone] " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE;
    private static final String CHANNEL_ID = "channel";
    private static final int NOTIFICATION_ID = 101;
    public static final String BROADCAST_ACTION = "com.example.android.nearby.reports";
    private final IBinder binder = new LocalBinder();
    private ConnectionsClient connectionsClient;
    private final SimpleArrayMap<String, EndpointStatus> endpoints = new SimpleArrayMap<>();

    static String endTime;
    static File file;
    static File file1;
    static File targetFileName;
    static File targetFileName1;
    static File payloadFile1;
    static File payloadFile;
    static Payload filePayload;
    static EndpointStatus status;

    @Override
    public void onCreate() {

        super.onCreate();
        connectionsClient = Nearby.getConnectionsClient(this);
        startForeground(NOTIFICATION_ID, getNotification());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAdvertising();
        return START_NOT_STICKY;

    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /** The devices we've discovered near us. */
    public SimpleArrayMap<String, EndpointStatus> getStatus() {
        return endpoints;
    }

    class LocalBinder extends Binder {
        NearbyService getService()
        {
            return NearbyService.this;
        }
    }

    public void startAdvertising() {
        Log.d(TAG, "startAdvertising()");

        connectionsClient.startAdvertising(
                LOCAL_ENDPOINT_NAME, SERVICE_ID, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    public void startAdvertising1() {
        Log.d(TAG, "startAdvertising() 1");

        connectionsClient.startAdvertising(
                LOCAL_ENDPOINT_NAME, SERVICE_ID, connectionLifecycleCallback1,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());

    }


    public void startDiscovery() {
        Log.d(TAG, "startDiscovery()");

        connectionsClient.startDiscovery(
                SERVICE_ID, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());

    }


    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: endpointId =" + endpointId);
                    Log.i(TAG, "onEndpointFound: info.getEndpointName() =" + info.getEndpointName());
                    EndpointStatus status = new EndpointStatus();
                    status.setName(info.getEndpointName());
                    status.setStatus("found");
                    endpoints.put(endpointId, status);
                    reportConnectStatus();

                }


                @Override
                public void onEndpointLost(String endpointId) {
                    Log.i(TAG, "onEndpointLost: endpointId =" + endpointId);
                    endpoints.remove(endpointId);
                    reportConnectStatus();
                }
            };


//document
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.i(TAG, "onConnectionInitiated: endpointId =" + endpointId);
                    Log.i(TAG, "onConnectionInitiated: connectionInfo.getEndpointName() =" + connectionInfo.getEndpointName());
                    Log.i(TAG, "onConnectionInitiated:connectionInfo.isIncomingConnection() =" + connectionInfo.isIncomingConnection());
                    Log.i(TAG, " acceptConnection");


                    connectionsClient.acceptConnection(endpointId, payloadCallback);

                    status = new EndpointStatus();
                    status.setName(connectionInfo.getEndpointName());
                    status.setStatus("ConnectionInitiated");
                    endpoints.put(endpointId, status);
                    reportConnectStatus();

                }


                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i(TAG, "onConnectionResult: connection successful");
                        Log.i(TAG, "onConnectionResult: endpointId =" + endpointId);
                        endpoints.get(endpointId).setStatus("Connected");

                        connectionsClient.stopDiscovery();
                        reportConnectStatus();


                    } else {
                        Log.i(TAG, "onConnectionResult: connection failed");
                        endpoints.get(endpointId).setStatus("connection failed");
                        reportConnectStatus();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.i(TAG, "onDisconnected: endpointId =" + endpointId);
                    endpoints.get(endpointId).setStatus("Disconnected");
                    reportConnectStatus();
                }
            };



//image
    private final ConnectionLifecycleCallback connectionLifecycleCallback1 =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {

                    Log.i(TAG, "onConnectionInitiated: endpointId 1=" + endpointId);
                    Log.i(TAG, "onConnectionInitiated: connectionInfo.getEndpointName() 1=" + connectionInfo.getEndpointName());
                    Log.i(TAG, "onConnectionInitiated:connectionInfo.isIncomingConnection() 1=" + connectionInfo.isIncomingConnection());
                    Log.i(TAG, " acceptConnection1");

                    connectionsClient.acceptConnection(endpointId, payloadCallback1);

                    status = new EndpointStatus();
                    status.setName(connectionInfo.getEndpointName());
                    status.setStatus("ConnectionInitiated1");
                    endpoints.put(endpointId, status);
                    reportConnectStatus();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        Log.i(TAG, "onConnectionResult: connection successful 1");
                        Log.i(TAG, "onConnectionResult: endpointId 1=" + endpointId);
                        endpoints.get(endpointId).setStatus("Connected1");

                        connectionsClient.stopDiscovery();
                        reportConnectStatus();


                    } else {
                        Log.i(TAG, "onConnectionResult: connection failed1");
                        endpoints.get(endpointId).setStatus("connection failed1");
                        reportConnectStatus();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.i(TAG, "onDisconnected: endpointId 1=" + endpointId);
                    endpoints.get(endpointId).setStatus("Disconnected1");
                    reportConnectStatus();
                }
            };


    public void reportConnectStatus() {
        Intent localIntent = new Intent(BROADCAST_ACTION);
        LocalBroadcastManager.getInstance(NearbyService.this).sendBroadcast(localIntent);
    }

    public void sendStringPayload(String str) {
        connectionsClient.sendPayload(endpoints.keyAt(0), Payload.fromBytes(str.getBytes(UTF_8)));
    }


    public void sendFilePayload(Payload filePayload) {
        connectionsClient.sendPayload(endpoints.keyAt(0), filePayload);
    }


    public void disconnect(int position) {
        connectionsClient.disconnectFromEndpoint(endpoints.keyAt(position));
    }

    public void connect(int position) {
        if (position == 0) {
            connectionsClient.requestConnection(LOCAL_ENDPOINT_NAME, endpoints.keyAt(position), connectionLifecycleCallback);
        }
    }

    public void connect1(int position){
        if (position == 1) {
            connectionsClient.requestConnection(LOCAL_ENDPOINT_NAME, endpoints.keyAt(position), connectionLifecycleCallback1);
        }
    }

    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();


    //Payload 1: Document
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            Log.d(TAG, "onPayloadReceived, payload.getType() = " + payload.getType());
            if (payload.getType() == Payload.Type.BYTES) {
                String str = new String(payload.asBytes(), UTF_8);
            } else if (payload.getType() == Payload.Type.FILE) {
                incomingFilePayloads.put(payload.getId(), payload);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                Log.d(TAG, "PayloadTransferUpdate.Status.SUCCESS");
                long payloadId = update.getPayloadId();
                Payload payload = incomingFilePayloads.remove(payloadId);

                if (payload != null) {
                    Log.d(TAG, "payload.getType() " + payload.getType());
                    completedFilePayloads.put(payloadId, payload);


                    if (payload.getType() == Payload.Type.FILE)
                    {
                        processFilePayload1(payloadId);

                    }

                }
            }
        }

    };

    private void processFilePayload1(long payloadId) {
        Log.d(TAG, "processFilePayload1 ");
        filePayload = completedFilePayloads.get(payloadId);
        if (filePayload != null) {
            completedFilePayloads.remove(payloadId);
            file1 = getFile1(filePayload);
            showFile1(file1);
        }
    }

    private File getFile1(Payload filePayload) {
        payloadFile = filePayload.asFile().asJavaFile();
        String randomName = "nearby_shared-" + System.currentTimeMillis() + ".pdf";
        targetFileName = new File(payloadFile.getParentFile(), randomName);
        boolean result = payloadFile.renameTo(targetFileName);
        if (!result) Log.d(TAG, "renameTo failed  ");
        Log.d(TAG, "targetFileName =  " + targetFileName.toString());
        return targetFileName;
    }

    private void showFile1(File targetFileName) {

        Uri uri3 = FileProvider.getUriForFile(this,
                "com.example.android.wifidirect.fileprovider",
                targetFileName);

        String textUri3 = uri3.toString();

        Intent intent = new Intent(Intent.ACTION_VIEW);

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(textUri3.substring(textUri3.lastIndexOf('.') + 1));
        intent.setDataAndType(uri3, mimeType);
        //intent.setDataAndType(uri3, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);

        //Run Timestamp - Prints the end time of the image transferred in run screen
        endTime = getTime1();
        Log.d("END TIME: ", String.valueOf(endTime));
        //Timestamp of the received file
        timestamp.setText(endTime);
    }


    //Payload 2: Image
    private final PayloadCallback payloadCallback1 = new PayloadCallback() {

        @Override
        public void onPayloadReceived(String endpointId, Payload payload1) {
            Log.d(TAG, "onPayloadReceived, payload1.getType() = " + payload1.getType());
            if (payload1.getType() == Payload.Type.BYTES) {
                String str1 = new String(payload1.asBytes(), UTF_8);
            } else if (payload1.getType() == Payload.Type.FILE) {
                incomingFilePayloads.put(payload1.getId(), payload1);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update1) {

            if (update1.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                Log.d(TAG, "PayloadTransferUpdate.Status.SUCCESS");
                long payloadId = update1.getPayloadId();
                Payload payload1 = incomingFilePayloads.remove(payloadId);

                if (payload1 != null) {
                    Log.d(TAG, "payload1.getType() " + payload1.getType());
                    completedFilePayloads.put(payloadId, payload1);
                    if (payload1.getType() == Payload.Type.FILE)
                    {
                        processFilePayload(payloadId);
                    }

                }
            }
        }

    };

        private void processFilePayload(long payloadId) {
        Log.d(TAG, "processFilePayload ");
        filePayload = completedFilePayloads.get(payloadId);
        if (filePayload != null) {
            completedFilePayloads.remove(payloadId);
            file = getFile(filePayload);
            showFile(file);
        }
    }


    private File getFile(Payload filePayload) {
        payloadFile1 = filePayload.asFile().asJavaFile();
        String randomName1 = "nearby_shared-" + System.currentTimeMillis() + ".jpg";
        targetFileName1 = new File(payloadFile1.getParentFile(), randomName1);
        boolean result1 = payloadFile1.renameTo(targetFileName1);
        if (!result1) Log.d(TAG, "renameTo failed  ");
        Log.d(TAG, "targetFileName1 =  " + targetFileName1.toString());
        return targetFileName1;
    }


    private void showFile(File targetFileName1) {

        Uri uri2 = FileProvider.getUriForFile(this,
                "com.example.android.wifidirect.fileprovider",
                targetFileName1);

        String textUri2 = uri2.toString();

        Intent intent = new Intent(Intent.ACTION_VIEW);

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(textUri2.substring(textUri2.lastIndexOf('.') + 1));
        intent.setDataAndType(uri2, mimeType);

        //intent.setDataAndType(uri2, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);

        //Run Timestamp - Prints the end time of the image transferred in run screen
        endTime = getTime1();
        Log.d("END TIME: ", String.valueOf(endTime));
        //Timestamp of the received file
        timestamp.setText(endTime);
    }




    private Notification getNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {

            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT

            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nearby Service")
                .setContentText("Wi-Fi Direct")
                .setSmallIcon(R.drawable.icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle())
                .build();

    }

    public void stopDiscovery() {
        connectionsClient.stopDiscovery();
    }

    public void stopAdvertising() {
        connectionsClient.stopAdvertising();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        stopDiscovery();
        connectionsClient.stopAllEndpoints();
    }


}