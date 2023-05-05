/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.wifidirect;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;
import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    protected static final int CHOOSE_FILE_RESULT_CODE1 = 21;

    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    private View mContentView = null;

    static String startTime;
    static String endTime;

    static Uri uri;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);

        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true

                );
                ((DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        //Button - Launch Gallery
        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);

                    }
                });
        //Button - Launch Documents
        mContentView.findViewById(R.id.btn_start_client1).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // Allow user to pick a document
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("application/pdf");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE1);
                    }
                }
        );


        return mContentView;
    }

    //Getting and setting Time
    public static String getTime() {

        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSS Z", Locale.getDefault()).
                format(new Date());

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CHOOSE_FILE_RESULT_CODE: {
                // User has picked an image. Transfer it to group owner i.e peer using
                // FileTransferService.
                uri = data.getData();


                ImageView imageView1 = mContentView.findViewById(R.id.imageView1);
                imageView1.setImageURI(uri);

                TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
                statusText.setText("Sending: " + uri);
                Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);


                //Timestamp
                TextView timestamp = mContentView.findViewById(R.id.tStamp);
                timestamp.setText(getTime());
                //Run Timestamp - Prints in run screen
                startTime = getTime();
                Log.d("START TIME: ", String.valueOf(startTime));


                Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                getActivity().startService(serviceIntent);
            }
            break;

            case CHOOSE_FILE_RESULT_CODE1: {
                // User has picked a document. Transfer it to group owner i.e peer using
                // FileTransferService.
                uri = data.getData();

                PDFView pdfView = mContentView.findViewById(R.id.pdfView);
                pdfView.fromUri(uri);

                TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
                statusText.setText("Sending: " + uri);
                Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);


                //Timestamp
                TextView timestamp1 = mContentView.findViewById(R.id.tStamp);
                timestamp1.setText(getTime());
                //Run Timestamp - Prints in run screen
                startTime = getTime();
                Log.d("START TIME: ", String.valueOf(startTime));


                Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        info.groupOwnerAddress.getHostAddress());
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                getActivity().startService(serviceIntent);
            }

            break;
        }


    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {


            new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text),
                    mContentView.findViewById(R.id.tStamp)).execute();

            new FileServerAsyncTask1(getActivity(),mContentView.findViewById(R.id.status_text),
                    mContentView.findViewById(R.id.tStamp)).execute();


            //document
            mContentView.findViewById(R.id.btn_start_client1).setVisibility(View.GONE);

            //image
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
            mContentView.findViewById(R.id.imageView1).setVisibility(View.GONE);



        }
        else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.

            //document
             mContentView.findViewById(R.id.btn_start_client1).setVisibility(View.VISIBLE);

            //image
             mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
             mContentView.findViewById(R.id.imageView1).setVisibility(View.VISIBLE);



            ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
                    .getString(R.string.client_text));

        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);

    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);

        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);

        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);

        view = (TextView) mContentView.findViewById(R.id.tStamp);
        view.setText("dd-MM-yyyy HH:mm:ss:SSS Z");

        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */

    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        private TextView timestamp;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask(Context context, View statusText, View timestamp) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.timestamp = (TextView) timestamp;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {

                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");


                File f = new File(context.getExternalFilesDir("received"), "wifip2pshared-" + System.currentTimeMillis() + ".pdf");

                File dirs = new File(f.getParent());

                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());

                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));

                serverSocket.close();
                return f.getAbsolutePath();

            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);

                File recvFile = new File(result);
                Uri fileUri = FileProvider.getUriForFile(
                        context,
                        "com.example.android.wifidirect.fileprovider",
                        recvFile);

                Intent intent = new Intent(Intent.ACTION_VIEW);

                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(result.substring(result.lastIndexOf('.') + 1));
                intent.setDataAndType(fileUri, mimeType);

                //intent.setDataAndType(fileUri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);


                //Run Timestamp - Prints the end time of the image transferred in run screen
                endTime = getTime();
                Log.d("END TIME: ", String.valueOf(endTime));

                //Timestamp of the received file
                timestamp.setText(endTime);

            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");

        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }




    public static class FileServerAsyncTask1 extends AsyncTask<Void, Void, String> {

        private Context context;
        private TextView statusText;
        private TextView timestamp;

        /**
         * @param context
         * @param statusText
         */
        public FileServerAsyncTask1(Context context, View statusText, View timestamp) {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.timestamp = (TextView) timestamp;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {

                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");


                File g = new File(context.getExternalFilesDir("received"), "wifip2pshared-"
                        + System.currentTimeMillis() + ".jpg");

                File dirs = new File(g.getParent());

                if (!dirs.exists())
                    dirs.mkdirs();
                g.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + g.toString());

                InputStream inputstream = client.getInputStream();
                copyFile1(inputstream, new FileOutputStream(g));

                serverSocket.close();
                return g.getAbsolutePath();

            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result1) {
            if (result1 != null) {
                statusText.setText("File copied - " + result1);

                File recvFile = new File(result1);
                Uri fileUri1 =


                        FileProvider.getUriForFile(
                        context,
                        "com.example.android.wifidirect.fileprovider",
                        recvFile);

                Intent intent = new Intent(Intent.ACTION_VIEW);

                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(result1.substring(result1.lastIndexOf('.') + 1));
                intent.setDataAndType(fileUri1, mimeType);

                //intent.setDataAndType(fileUri1, "image/*");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);


                //Run Timestamp - Prints the end time of the image transferred in run screen
                endTime = getTime();
                Log.d("END TIME: ", String.valueOf(endTime));

                //Timestamp of the received file
                timestamp.setText(endTime);

            }

        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");

        }

    }

    public static boolean copyFile1(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
}
