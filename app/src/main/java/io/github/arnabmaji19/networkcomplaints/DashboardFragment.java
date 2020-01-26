package io.github.arnabmaji19.networkcomplaints;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.github.arnabmaji19.networkcomplaints.api.DeviceReportAPI;
import io.github.arnabmaji19.networkcomplaints.model.DeviceReport;
import io.github.arnabmaji19.networkcomplaints.model.LocationData;
import io.github.arnabmaji19.networkcomplaints.model.SimInfo;
import io.github.arnabmaji19.networkcomplaints.util.LayoutToggler;
import io.github.arnabmaji19.networkcomplaints.util.LocationDataManager;
import io.github.arnabmaji19.networkcomplaints.util.PermissionsUtil;
import io.github.arnabmaji19.networkcomplaints.util.PhoneManager;
import io.github.arnabmaji19.networkcomplaints.util.Session;
import io.github.arnabmaji19.networkcomplaints.util.SimCardListAdapter;
import io.github.arnabmaji19.networkcomplaints.util.WaitDialog;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    private Activity activity;
    private ConstraintLayout permissionsLayout;
    private ConstraintLayout dashoardLayout;
    private ConstraintLayout localDataAlertLayout;
    private LinearLayout loadingLayout;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView postalCodeTextView;
    private TextView stateTextView;
    private Button grantPermissionsButton;
    private Button analyzeButton;
    private Button sendDataButton;
    private LayoutToggler layoutToggler;
    private WaitDialog waitDialog;
    private PermissionsUtil permissionsUtil;
    private RecyclerView simDetailsRecyclerView;
    private PhoneManager phoneManager;
    private LocationDataManager locationDataManager;
    private DeviceReport deviceReport;
    private DeviceReportAPI deviceReportAPI;

    public DashboardFragment(Activity activity) {
        this.activity = activity;
        this.permissionsUtil = new PermissionsUtil(activity, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        layoutToggler = new LayoutToggler();
        //set up PhoneManager
        TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        phoneManager = new PhoneManager(telephonyManager);
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        locationDataManager = new LocationDataManager(activity, locationManager);

        //link views
        grantPermissionsButton = view.findViewById(R.id.grantPermissionsButton);
        analyzeButton = view.findViewById(R.id.analyzeButton);
        sendDataButton = view.findViewById(R.id.sendDataButton);
        permissionsLayout = view.findViewById(R.id.permissionsLayout);
        dashoardLayout = view.findViewById(R.id.dashboardLayout);
        localDataAlertLayout = view.findViewById(R.id.localDatAlertLayout);
        loadingLayout = view.findViewById(R.id.laodingBar);
        latitudeTextView = view.findViewById(R.id.latitudeTextView);
        longitudeTextView = view.findViewById(R.id.longitudeTextView);
        postalCodeTextView = view.findViewById(R.id.postalCodeTextView);
        stateTextView = view.findViewById(R.id.stateTextView);
        simDetailsRecyclerView = view.findViewById(R.id.simCardListRecyclerView);
        simDetailsRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
        waitDialog = new WaitDialog(activity);
        //add layouts to layout toggler
        layoutToggler.addLayouts(permissionsLayout, dashoardLayout);

        if (permissionsUtil.areAllPermissionsGranted()) {
            layoutToggler.setVisible(localDataAlertLayout);
        } else {
            layoutToggler.setVisible(permissionsLayout);
        }

        grantPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                permissionsUtil.askPermissions();
            }
        });

        analyzeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (!locationDataManager.isGPSEnabled()) { //if gps is not turned on do not request location updates
                    Toast.makeText(activity.getBaseContext(), "Please turn on GPS", Toast.LENGTH_SHORT).show();
                    return;
                }
                loadingLayout.setVisibility(View.VISIBLE); //show the loading bar
                //get location details
                locationDataManager.requestCurrentLocation(new LocationDataManager.OnSuccessListener() {
                    @Override
                    public void onSuccess(Location location) {
                        Log.d(TAG, "onSuccess: " + location.getLongitude() + location.getLatitude());
                        loadingLayout.setVisibility(View.GONE); //hide the loading layout
                        //get sim card info list
                        List<SimInfo> simInfoList = phoneManager.getSimInfo(activity.getBaseContext());
                        SimCardListAdapter simCardListAdapter = new SimCardListAdapter(simInfoList);
                        simDetailsRecyclerView.setAdapter(simCardListAdapter);

                        //update location details
                        String postalCode = "Unknown";
                        String state = "Unknown";
                        Address address = locationDataManager.getAddress(location);
                        if (address != null) {
                            postalCode = address.getPostalCode();
                            postalCodeTextView.setText(postalCode);
                            state = address.getAdminArea();
                            stateTextView.setText(state);
                        }
                        String latitude = location.getLatitude() + "";
                        String longitude = location.getLongitude() + "";
                        latitudeTextView.setText(latitude);
                        longitudeTextView.setText(longitude);

                        LocationData locationData = new LocationData(latitude, longitude, postalCode, state);
                        deviceReport = new DeviceReport(locationData, simInfoList);


                        if (Session.getInstance().isSessionAvailable()) { //if session is available, configure api to send data
                            deviceReportAPI = new DeviceReportAPI(deviceReport);

                            deviceReportAPI.post(); //send post request
                        }
                        layoutToggler.setVisible(dashoardLayout); //show the dashboard layout
                    }
                });
            }
        });

        sendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Session.getInstance().isSessionAvailable()) { //if session is available send data
                    deviceReportAPI.post();
                    return;
                }

                //If session is not available, prompt the user to save device report
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder
                        .setTitle("Save Device Report")
                        .setMessage("Looks like you are in offline Mode." +
                                "\nWould you like to save device report?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //save device report for future use

                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        return view;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: ");
        if (requestCode == PermissionsUtil.PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (permissionsUtil.areAllPermissionsGranted()) {
                    layoutToggler.setVisible(dashoardLayout);
                }
            }
        }
    }
}
