package de.tuberlin.mcc.simra.app.subactivites;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import de.tuberlin.mcc.simra.app.R;
import de.tuberlin.mcc.simra.app.util.BaseActivity;
import pl.droidsonroids.gif.GifImageView;

import static de.tuberlin.mcc.simra.app.util.Utils.*;

public class SettingsActivity extends BaseActivity {

    // Log tag
    private static final String TAG = "SettingsActivity_LOG";
    ImageButton backBtn;
    TextView toolbarTxt;

    long privacyDuration;
    int privacyDistance;
    int child;
    int trailer;
    String unit;
    int dateFormat;
    int radmesserConnectionStatus;

    // Bike Type and Phone Location Items
    Spinner bikeTypeSpinner;
    Spinner phoneLocationSpinner;

    // Child and Trailer
    CheckBox childCheckBox;
    CheckBox trailerCheckBox;

    Switch unitSwitch;
    Switch dateFormatSwitch;
    Switch radmesserConnectionSwitch;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static int BLUETOOTH_SUCCESS = -1;
    private final static int CONNECTED = 2;
    private final static int CONNECTING = 1;
    private final static int NOT_CONNECTED = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        toolbarTxt = findViewById(R.id.toolbar_title);
        toolbarTxt.setText(R.string.title_activity_settings);

        backBtn = findViewById(R.id.back_button);
        backBtn.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View v) {
                                           finish();
                                       }
                                   }
        );

        // load unit and date format
        unit = lookUpSharedPrefs("Settings-Unit","m","simraPrefs",this);
        dateFormat = lookUpIntSharedPrefs("Settings-DateFormat",0,"simraPrefs",this);

        // Set switches
        unitSwitch = findViewById(R.id.unitSwitch);
        if (unit.equals("ft")) {
            unitSwitch.setChecked(true);
        }
        dateFormatSwitch = findViewById(R.id.dateFormatSwitch);
        if (dateFormat == 1) {
            dateFormatSwitch.setChecked(true);
        }
        unitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    unit = "ft";
                } else {
                    unit = "m";
                }
            }
        });
        dateFormatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    dateFormat = 1;
                } else {
                    dateFormat = 0;
                }
            }
        });
        // Set seekBars
        SeekBar durationSeekBar = (SeekBar) findViewById(R.id.privacyDurationSeekBar);
        SeekBar distanceSeekBar = (SeekBar) findViewById(R.id.privacyDistanceSeekBar);

        // Set textViews
        TextView durationTextView = (TextView) findViewById(R.id.privacyDurationSeekBarProgress);
        TextView distanceTextView = (TextView) findViewById(R.id.privacyDistanceSeekBarProgress);

        // Load the privacy option values
        privacyDuration = lookUpLongSharedPrefs("Privacy-Duration", 30, "simraPrefs", this);

        privacyDistance = lookUpIntSharedPrefs("Privacy-Distance", 30, "simraPrefs", this);
        if (unit.equals("ft")) {
            privacyDistance = (int) Math.round((lookUpIntSharedPrefs("Privacy-Distance", 30, "simraPrefs", this) * 3.28));
        }

        // Set the seekBars according to the privacy option values
        durationSeekBar.setProgress((int) privacyDuration);
        distanceSeekBar.setProgress(privacyDistance);

        // Set the textViews according to the values of the corresponding seekBars
        durationTextView.setText(durationSeekBar.getProgress() + "s/" + (durationSeekBar.getMax()) + "s");
        if (unit.equals("ft")) {
            distanceTextView.setText(Math.round(distanceSeekBar.getProgress() * 3.28) + unit + "/" + Math.round(distanceSeekBar.getMax() * 3.28) + unit);
        } else {
            distanceTextView.setText(distanceSeekBar.getProgress() + unit + "/" + distanceSeekBar.getMax() + unit);

        }

        // Create onSeekBarChangeListeners to change the corresponding options
        durationSeekBar.setOnSeekBarChangeListener(createOnSeekBarChangeListener(durationTextView, "s", "Privacy-Duration"));
        distanceSeekBar.setOnSeekBarChangeListener(createOnSeekBarChangeListener(distanceTextView, unit, "Privacy-Distance"));

        // Bike Type and Phone Location Spinners
        bikeTypeSpinner = findViewById(R.id.bikeTypeSpinner);
        phoneLocationSpinner = findViewById(R.id.locationTypeSpinner);

        childCheckBox = findViewById(R.id.childCheckBox);
        trailerCheckBox = findViewById(R.id.trailerCheckBox);

        // Load previous bikeType and phoneLocation settings
        int bikeType = lookUpIntSharedPrefs("Settings-BikeType", 0, "simraPrefs", this);
        int phoneLocation = lookUpIntSharedPrefs("Settings-PhoneLocation", 0, "simraPrefs", this);

        bikeTypeSpinner.setSelection(bikeType);
        phoneLocationSpinner.setSelection(phoneLocation);

        // Load previous child and trailer settings
        child = lookUpIntSharedPrefs("Settings-Child", 0, "simraPrefs", this);
        trailer = lookUpIntSharedPrefs("Settings-Trailer", 0, "simraPrefs", this);

        if (child == 1) {
            childCheckBox.setChecked(true);
        }
        childCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    child = 1;
                } else {
                    child = 0;
                }
            }
        });

        if (trailer == 1){
            trailerCheckBox.setChecked(true);
        }
        trailerCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    trailer = 1;
                } else {
                    trailer = 0;
                }
            }
        });



        TextView appVersionTextView = findViewById(R.id.appVersionTextView);
        appVersionTextView.setText("Version: " + getAppVersionNumber(this));

        // set radmesser connection
        radmesserConnectionStatus = lookUpIntSharedPrefs("RadmesserStatus", 0, "simraPrefs",this);
        radmesserConnectionSwitch = findViewById(R.id.radmesserSwitch);
        radmesserConnectionSwitch.setChecked(radmesserConnectionStatus > 0); // 0 not connected, 1 connecting, 2 connected
        radmesserConnectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                radmesserConnectionStatus = isChecked ? 1 : 0;
                setRadmesserStatus(radmesserConnectionStatus);
                if(BluetoothAdapter.getDefaultAdapter() == null){
                    // Device does not support Bluetooth
                }
                if (!BluetoothAdapter.getDefaultAdapter().isEnabled() && isChecked) {
                    enableBluetooth();
                }
                if(isChecked){
                    showTutorialDialog();
                }
            }
        });

    }

    private void setRadmesserStatus(int newStatus){
        writeIntToSharedPrefs("RadmesserStatus",newStatus,"simraPrefs", this);
    }

    private void showTutorialDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Verbindung mit Radmesser");
        alert.setMessage("\nBitte halten Sie Ihr Hand nah an den Abstandsensor für 3 Sekunden");

        LinearLayout gifLayout = new LinearLayout(this);
        LinearLayout.LayoutParams gifMargins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        GifImageView gif = new GifImageView(this);
        gif.setImageResource(R.drawable.tutorial);
        gif.setVisibility(View.VISIBLE);
        gifMargins.setMargins(50, 0, 50, 0);
        gif.setLayoutParams(gifMargins);
        gifLayout.addView(gif);
        alert.setView(gifLayout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        });
        alert.show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == BLUETOOTH_SUCCESS && requestCode == REQUEST_ENABLE_BT) {
            // Bluetooth successfully enabled, i dont know why the F**k is -1 instead a positive number
            radmesserConnectionSwitch.setChecked(true);
            setRadmesserStatus(CONNECTED);
            // LINUS Hier Vielleicht verbindung anfragen zu Radmesser

        }else{
            radmesserConnectionSwitch.setChecked(false);
            setRadmesserStatus(NOT_CONNECTED);
        }
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }



    // OnSeekBarChangeListener to update the corresponding value (privacy duration or distance)
    private SeekBar.OnSeekBarChangeListener createOnSeekBarChangeListener(TextView tV, String unit, String privacyOption) {
        SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (unit.equals("s")) {
                    if (progress < 3) {
                        seekBar.setProgress(3);
                    }
                }
                if (unit.equals("ft")) {
                    tV.setText(Math.round(seekBar.getProgress() * 3.28) + unit + "/" + Math.round(seekBar.getMax() * 3.28) + unit);

                } else {
                    tV.setText(seekBar.getProgress() + unit + "/" + seekBar.getMax() + unit);

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (unit.equals("s")) {
                    if (seekBar.getProgress() < 3) {
                        seekBar.setProgress(3);
                    }
                }
                if (unit.equals("ft")) {
                    tV.setText(Math.round(seekBar.getProgress() * 3.28) + unit + "/" + Math.round(seekBar.getMax() * 3.28) + unit);

                } else {
                    tV.setText(seekBar.getProgress() + unit + "/" + seekBar.getMax() + unit);

                }
                if (privacyOption.equals("Privacy-Duration")) {
                    privacyDuration = seekBar.getProgress();
                    //writeLongToSharedPrefs(privacyOption, (long) seekBar.getProgress(), "simraPrefs", SettingsActivity.this);
                } else if (privacyOption.equals("Privacy-Distance")) {
                    privacyDistance = seekBar.getProgress();
                    // writeIntToSharedPrefs(privacyOption, seekBar.getProgress(), "simraPrefs", SettingsActivity.this);
                } else {
                    Log.e(TAG, "onStopTrackingTouch unknown privacyOption: " + privacyOption);
                }
            }
        };

        return onSeekBarChangeListener;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
        writeLongToSharedPrefs("Privacy-Duration", privacyDuration, "simraPrefs", this);
        writeIntToSharedPrefs("Privacy-Distance", privacyDistance, "simraPrefs", this);
        writeIntToSharedPrefs("Settings-BikeType", bikeTypeSpinner.getSelectedItemPosition(), "simraPrefs", this);
        writeIntToSharedPrefs("Settings-PhoneLocation", phoneLocationSpinner.getSelectedItemPosition(), "simraPrefs", this);
        writeIntToSharedPrefs("Settings-Child", child, "simraPrefs", this);
        writeIntToSharedPrefs("Settings-Trailer", trailer, "simraPrefs", this);
        writeToSharedPrefs("Settings-Unit",unit,"simraPrefs",this);
        writeIntToSharedPrefs("Settings-DateFormat",dateFormat,"simraPrefs", this);
    }

}
