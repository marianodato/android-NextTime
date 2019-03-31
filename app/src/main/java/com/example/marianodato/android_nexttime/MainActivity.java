package com.example.marianodato.android_nexttime;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

/**
 * Demonstrates how to create and remove geofences using the GeofencingApi. Uses an IntentService
 * to monitor geofence transitions and creates notifications whenever a device enters or exits
 * a geofence.
 * <p>
 * This sample requires a device's Location settings to be turned on. It also requires
 * the ACCESS_FINE_LOCATION permission, as specified in AndroidManifest.xml.
 * <p>
 */
public class MainActivity extends AppCompatActivity implements ServiceConnection{

    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean serviceBinded = false;
    private GeofenceService service;
    private String geofencingAction;
    private Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.w(TAG, intent.getStringExtra(Constants.KEY));
                Toast.makeText(MainActivity.this, intent.getStringExtra(Constants.KEY), Toast.LENGTH_SHORT).show();
                if (!intent.getStringExtra(Constants.KEY).equals(getString(R.string.geofences_added)) || !intent.getStringExtra(Constants.KEY).equals(getString(R.string.geofences_removed))){
                    if (serviceBinded) {
                        unbindService();
                        serviceBinded = false;
                    }
                }

            }
        }, new IntentFilter(Constants.FILTER));

    }

    private void unbindService() {
        unbindService(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            if (serviceBinded){
                String responseMessage = service.addSavedGeofences();
                if (responseMessage != null && responseMessage.equals(getString(R.string.insufficient_permissions))){
                    showSnackbar(responseMessage);
                }
            }else{
                Intent intent= new Intent(this, GeofenceService.class);
                bindService(intent, this, Context.BIND_AUTO_CREATE);
                geofencingAction = Constants.ADD;
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        GeofenceService.MyBinder b = (GeofenceService.MyBinder) binder;
        service = b.getService();
        Log.w(TAG,  "Connected to Service");
        String responseMessage;
        if(geofencingAction.equals(Constants.ADD)){
            responseMessage = service.addSavedGeofences();
            serviceBinded = true;
        }else{
            responseMessage = service.removeSavedGeofences();
            unbindService(this);
            serviceBinded = false;
        }
        if (responseMessage != null && responseMessage.equals(getString(R.string.insufficient_permissions))) {
            showSnackbar(responseMessage);
        }

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuRemoveGeofences:
                if (!checkPermissions()) {
                    requestPermissions();
                    return true;
                }
                if (serviceBinded) {
                    String responseMessage = service.removeSavedGeofences();
                    if (responseMessage != null && responseMessage.equals(getString(R.string.insufficient_permissions))) {
                        showSnackbar(responseMessage);
                    }
                    unbindService(this);
                    serviceBinded = false;
                }else{
                    Intent intent= new Intent(this, GeofenceService.class);
                    bindService(intent, this, Context.BIND_AUTO_CREATE);
                    geofencingAction = Constants.REMOVE;
                }
                break;
        }
        return true;
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    Constants.REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == Constants.REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.");
                if (serviceBinded){
                    String responseMessage = service.addSavedGeofences();
                    if (responseMessage != null && responseMessage.equals(getString(R.string.insufficient_permissions))){
                        showSnackbar(responseMessage);
                    }
                }else{
                    Intent intent= new Intent(this, GeofenceService.class);
                    bindService(intent, this, Context.BIND_AUTO_CREATE);
                    geofencingAction = Constants.ADD;
                }
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts(Constants.PACKAGE,
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
                if (serviceBinded) {
                    unbindService(this);
                    serviceBinded = false;
                }
            }
        }
    }
}
