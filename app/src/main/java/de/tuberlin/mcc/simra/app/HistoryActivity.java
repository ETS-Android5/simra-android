package de.tuberlin.mcc.simra.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static de.tuberlin.mcc.simra.app.Utils.checkForAnnotation;
import static de.tuberlin.mcc.simra.app.Utils.fileExists;
import static de.tuberlin.mcc.simra.app.Utils.getAppVersionNumber;
import static de.tuberlin.mcc.simra.app.Utils.lookUpBooleanSharedPrefs;
import static de.tuberlin.mcc.simra.app.Utils.lookUpIntSharedPrefs;
import static de.tuberlin.mcc.simra.app.Utils.overWriteFile;
import static de.tuberlin.mcc.simra.app.Utils.readContentFromFile;

public class HistoryActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "HistoryActivity_LOG";
    ImageButton backBtn;
    ImageButton helpBtn;
    TextView toolbarTxt;

    boolean exitWhenDone = false;
    String accGpsString = "";
    String pathToAccGpsFile = "";
    String date = "";
    int state = 0;
    String duration = "";
    String startTime = "";

    ListView listView;
    private File metaDataFile;
    String[] ridesArr;

    UploadService mBoundUploadService;

    boolean privacyAgreement = false;

    private BroadcastReceiver statusReceiver;
    private IntentFilter mIntent;

    BroadcastReceiver br;

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            StringBuilder sb = new StringBuilder();
            sb.append("Action: " + intent.getAction() + "\n");
            sb.append("Extra: " + intent.getStringExtra("data") + "\n");
            sb.append("URI: " + intent.toUri(Intent.URI_INTENT_SCHEME).toString() + "\n");
            String log = sb.toString();
            Log.d(TAG, "onReceive: " + log);
            Toast.makeText(context, log, Toast.LENGTH_LONG).show();
            refreshMyRides();
        }
    }



    /**
     * @TODO: When this Activity gets started automatically after the route recording is finished,
     * the route gets shown immediately by calling ShowRouteActivity.
     * Otherwise, this activity has to scan for saved rides (maybe as files in the internal storage
     * or as entries in sharedPreference) and display them in a list.
     * <p>
     * The user must be able to select a ride which should start the ShowRouteActivity with that ride.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_history);

        privacyAgreement = lookUpBooleanSharedPrefs("Privacy-Policy-Accepted", false, "simraPrefs", this);

        //  Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_history);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           finish();
                                       }
                                   }
        );

        /*
        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch(intent.getIntExtra("status", -1)) {
                    case 1:
                        Log.d(TAG, "BroadcastReceiver.onReceive() case 1");
                        break;
                    case 2:
                        Log.d(TAG, "BroadcastReceiver.onReceive() case 2");
                        break;
                    default:
                        Log.d(TAG, "BroadcastReceiver.onReceive() case default");
                }
            }
        };

        registerReceiver(statusReceiver, new IntentFilter("de.tuberlin.mcc.simra.app.GET_STATUS_INTENT"));
        */
        // activating the help Button
        /*helpBtn = findViewById(R.id.help_icon);
        helpBtn.setVisibility(View.VISIBLE);
        helpBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           Intent intent = new Intent (getApplicationContext(), HelpActivity.class);
                                           startActivity(intent);
                                       }
                                   }
        );*/

        listView = findViewById(R.id.listView);

        RelativeLayout justUploadButton = findViewById(R.id.justUpload);
        justUploadButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    justUploadButton.setElevation(0.0f);
                    justUploadButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    justUploadButton.setElevation(2 * HistoryActivity.this.getResources().getDisplayMetrics().density);
                    justUploadButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            }

        });
        justUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HistoryActivity.this, UploadService.class);
                startService(intent);
                Toast.makeText(HistoryActivity.this,getString(R.string.upload_started),Toast.LENGTH_SHORT);
                // bindService(intent, mUploadServiceConnection, Context.BIND_AUTO_CREATE);

            }
        });


        RelativeLayout uploadAndExitButton = findViewById(R.id.uploadAndExit);
        uploadAndExitButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    uploadAndExitButton.setElevation(0.0f);
                    uploadAndExitButton.setBackground(getDrawable(R.drawable.button_pressed));
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    uploadAndExitButton.setElevation(2 * HistoryActivity.this.getResources().getDisplayMetrics().density);
                    uploadAndExitButton.setBackground(getDrawable(R.drawable.button_unpressed));
                }
                return false;
            }

        });
        uploadAndExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitWhenDone = true;
                justUploadButton.performClick();
                HistoryActivity.this.moveTaskToBack(true);
            }
        });


        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Press immediately the button, if HistoryActivity was created automatically after the
        // recording of a route has finished
        if (getIntent().hasExtra("PathToAccGpsFile")) {
            startShowRouteWithSelectedRide();
        }

    }

    private void refreshMyRides() {
        ArrayList<String[]> metaDataLines = new ArrayList<>();


        if (fileExists("metaData.csv", this)) {

            metaDataFile = getFileStreamPath("metaData.csv");
            try {
                BufferedReader br = new BufferedReader(new FileReader(metaDataFile));
                // br.readLine() to skip the first line which contains the headers
                String line = br.readLine();
                line = br.readLine();

                while ((line = br.readLine()) != null) {
                    // Log.d(TAG, line);
                    metaDataLines.add(line.split(","));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //ridesArr = metaDataLines.toArray(new String[metaDataLines.size()]);
            ridesArr = new String[metaDataLines.size()];
            Log.d(TAG, "refreshMyRides(): metaDataLines: " + Arrays.deepToString(metaDataLines.toArray()));
            /*
            for (int i = 0; i < metaDataLines.size(); i++) {
                ridesArr[Integer.valueOf(metaDataLines.get(i)[0])] = listToTextShape(metaDataLines.get(i));
            }
            */
            Log.d(TAG, "ArrayList<String[]> metaDataLines: " + Arrays.deepToString(metaDataLines.toArray()));
            for (int i = 0; i < metaDataLines.size(); i++) {
                String[] metaDataLine = metaDataLines.get(i);
                Log.d(TAG, "String[] metaDataLine: " + Arrays.toString(metaDataLine));
                Log.d(TAG, "metaDataLines.size(): " + metaDataLines.size() + " metaDataLine[0]: " + metaDataLine[0]);
                Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
                ridesArr[((metaDataLines.size()) - i) - 1] = listToTextShape(metaDataLine);
                Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
            }

            Log.d(TAG, "ridesArr: " + Arrays.toString(ridesArr));
            ArrayList<String> stringArrayList = new ArrayList<>(Arrays.asList(ridesArr));
            MyArrayAdapter myAdapter = new MyArrayAdapter(this, R.layout.row, stringArrayList, metaDataLines);
            listView.setAdapter(myAdapter);

        } else {

            Log.d(TAG, "metaData.csv doesn't exists");

            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), (getString(R.string.noHistory)), Snackbar.LENGTH_LONG);
            snackbar.show();

        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        br = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("de.tuberlin.mcc.simra.app.MY_NOTIFICATION");
        this.registerReceiver(br, filter);
        refreshMyRides();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(br);
    }

    private String listToTextShape(String[] item) {
        Log.d(TAG, "listToTextShape item: " + Arrays.toString(item));
        String todo = getString(R.string.newRideInHistoryActivity);

        if (item[3].equals("1")) {
            todo = getString(R.string.rideAnnotatedInHistoryActivity);
        } else if (item[3].equals("2")) {
            todo = getString(R.string.rideUploadedInHistoryActivity);
        }

        long millis = Long.valueOf(item[2]) - Long.valueOf(item[1]);
        int minutes = Math.round((millis/1000/60));

        Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
        int day = localCalendar.get(Calendar.DATE);
        String month = String.valueOf(localCalendar.get(Calendar.MONTH) + 1);
        String year = String.valueOf(localCalendar.get(Calendar.YEAR)).substring(2,4);
        Date dt = new Date(Long.valueOf(item[1]));
        Locale locale = Resources.getSystem().getConfiguration().locale;
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm", locale);
        if (locale.equals(Locale.US)) {
            sdf = new SimpleDateFormat("hh:mm aa", locale);
        }
        String time = sdf.format(dt);
        if (month.length() < 2) {
           month = "0" + month;
        }

        String startDateOfRide = day + "." + month + "." + year + ", " + time + "h";
        if (locale.equals(Locale.US)) {
            startDateOfRide = month + "/" + day + "/" + year + ", " + time;
        }

        return "#" + item[0] + ";" + startDateOfRide + ";" + todo + ";" + minutes + ";" + item[3];
    }

    public void startShowRouteWithSelectedRide() {

        Log.d(TAG, "onClick()");

        // Checks if HistoryActivity was started by the user or by the app after a route
        // recording was finished
        if (getIntent().hasExtra("PathToAccGpsFile")) {
            // AccGpsString contains the accelerometer and location data as well as time data
            pathToAccGpsFile = getIntent().getStringExtra("PathToAccGpsFile");
            // TimeStamp is the duration of the ride in MS
            duration = getIntent().getStringExtra("Duration");
            // The time in which the ride started in ms from 1970
            startTime = getIntent().getStringExtra("StartTime");
            // State can be 0 for server processing not started, 1 for started and pending
            // and 2 for processed by server so the incidents can be annotated by the user
            state = getIntent().getIntExtra("State", 0);
        }
        // Log.d(TAG, "onCreate(): pathToAccGpsFile: " + pathToAccGpsFile + " date: " + date + " state: " + state);

        // Checks whether a ride was selected or not. Maybe it will be possible to select
        // multiple rides and push a button to send them all to the server to be analyzed
        if (accGpsString != null && startTime != "") {
            // Snackbar.make(view, getString(R.string.selectedRideInfo) + new Date(Long.valueOf(startTime)), Snackbar.LENGTH_LONG)
            //     .setAction("Action", null).show();
            // Start ShowRouteActivity with the selected Ride.
            Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
            intent.putExtra("PathToAccGpsFile", pathToAccGpsFile);
            intent.putExtra("Duration", duration);
            intent.putExtra("StartTime", startTime);
            intent.putExtra("State", state);
            startActivity(intent);
        } else {
            //Snackbar.make(view, getString(R.string.errorNoRideSelected) + new Date(Long.valueOf(startTime)), Snackbar.LENGTH_LONG)
            //      .setAction("Action", null).show();
        }


    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ServiceConnection for communicating with RecorderService
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private ServiceConnection mUploadServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            refreshMyRides();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected() called");
            UploadService.MyBinder myBinder = (UploadService.MyBinder) service;
            mBoundUploadService = myBinder.getService();
        }
    };

    public class MyArrayAdapter extends ArrayAdapter<String> {
        String TAG = "MyArrayAdapter_LOG";

        Context context;
        int layoutResourceId;
        ArrayList<String> stringArrayList = new ArrayList<String>();
        ArrayList<String[]> metaDataLines = new ArrayList<String[]>();

        public MyArrayAdapter(Context context, int layoutResourceId,
                              ArrayList<String> stringArrayList, ArrayList<String[]> metaDataLines ) {

            super(context, layoutResourceId, stringArrayList);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.stringArrayList = stringArrayList;
            this.metaDataLines = metaDataLines;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            Holder holder = null;
            Locale locale = Resources.getSystem().getConfiguration().locale;


            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new Holder();
                holder.rideDate = (TextView) row.findViewById(R.id.row_ride_date);
                if (locale.equals(Locale.US)) {
                    holder.rideDate.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 13));
                    row.findViewById(R.id.duration_relativeLayout).setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 10));
                }
                holder.duration = (TextView) row.findViewById(R.id.row_duration);
                holder.message = (TextView) row.findViewById(R.id.row_message);
                holder.btnDelete = (ImageButton) row.findViewById(R.id.button1);
                row.setTag(holder);
            } else {
                holder = (Holder) row.getTag();
            }
            Log.d(TAG, stringArrayList.get(position));
            String[] itemComponents = stringArrayList.get(position).split(";");
            holder.rideDate.setText(itemComponents[1]);
            holder.message.setText(itemComponents[2]);
            holder.duration.setText(itemComponents[3]);

            if (!itemComponents[4].equals("2")) {
                holder.btnDelete.setVisibility(View.VISIBLE);
            }


            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // gets the files in the directory
                    // lists all the files into an array
                    File[] dirFiles = getFilesDir().listFiles();
                    String clicked = (String) listView.getItemAtPosition(position);
                    clicked = clicked.replace("#", "").split(";")[0];
                    if (dirFiles.length != 0) {
                        // loops through the array of files, outputting the name to console
                        for (int i = 0; i < dirFiles.length; i++) {

                            String fileOutput = dirFiles[i].getName();


                            if (fileOutput.startsWith(clicked + "_")) {
                                // Start ShowRouteActivity with the selected Ride.
                                Intent intent = new Intent(HistoryActivity.this, ShowRouteActivity.class);
                                intent.putExtra("PathToAccGpsFile", dirFiles[i].getName());
                                // Log.d(TAG, "onClick() date: " + date);
                                intent.putExtra("Duration", String.valueOf(Long.valueOf(metaDataLines.get(position)[2]) - Long.valueOf(metaDataLines.get(position)[1])));
                                intent.putExtra("StartTime", metaDataLines.get(position)[2]);
                                intent.putExtra("State", Integer.valueOf(metaDataLines.get(position)[3]));
                                Log.d(TAG, "pathToAccGpsFile: " + dirFiles[i].getName());
                                Log.d(TAG, "Duration: " + String.valueOf(Long.valueOf(metaDataLines.get(position)[2]) - Long.valueOf(metaDataLines.get(position)[1])));
                                Log.d(TAG, "StartTime: " + metaDataLines.get(position)[2]);
                                Log.d(TAG, "State: " + metaDataLines.get(position)[3]);

                                startActivity(intent);
                            }
                        }
                    }
                }
            });

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Log.d(TAG,"Delete Button Clicked");
                    fireDeletePrompt(position, MyArrayAdapter.this);
                }
            });
            return row;
        }

        class Holder {
            TextView rideDate;
            TextView duration;
            TextView message;
            ImageButton btnDelete;
        }
    }

    public void fireDeletePrompt(int position, MyArrayAdapter arrayAdapter) {
        android.support.v7.app.AlertDialog.Builder alert = new android.support.v7.app.AlertDialog.Builder(HistoryActivity.this);
        alert.setTitle(getString(R.string.warning));
        alert.setMessage(getString(R.string.delete_file_warning));
        alert.setPositiveButton(R.string.delete_ride_approve, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                File[] dirFiles = getFilesDir().listFiles();
                Log.d(TAG, "btnDelete.onClick() dirFiles: " + Arrays.deepToString(dirFiles));
                String clicked = (String) listView.getItemAtPosition(position);
                Log.d(TAG, "btnDelete.onClick() clicked: " + clicked);
                clicked = clicked.replace("#", "").split(";")[0];
                if (dirFiles.length != 0) {
                    for (int i = 0; i < dirFiles.length; i++) {
                        File actualFile = dirFiles[i];
                        if (actualFile.getName().startsWith(clicked + "_") || actualFile.getName().startsWith("accEvents"+clicked)) {

                            /** don't delete the following line! */
                            Log.d(TAG, actualFile.getName() + " deleted: " + actualFile.delete());
                        }
                    }
                }
                String content = "";
                try (BufferedReader br = new BufferedReader(new FileReader(HistoryActivity.this.getFileStreamPath("metaData.csv")))) {
                    String line;

                    while ((line = br.readLine()) != null) {
                        if (!line.split(",")[0].equals(clicked)) {
                            content += line += System.lineSeparator();
                        }
                    }
                    overWriteFile(content,"metaData.csv", HistoryActivity.this);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                Toast.makeText(HistoryActivity.this,R.string.ride_deleted,Toast.LENGTH_SHORT).show();
                refreshMyRides();
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });
        alert.show();

    }
}
