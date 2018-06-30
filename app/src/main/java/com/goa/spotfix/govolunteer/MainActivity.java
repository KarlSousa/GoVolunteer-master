package com.goa.spotfix.govolunteer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 345;
    private static final String TAG = "MainActivity";
    private static ArrayList<Spotfix> cardList = new ArrayList<>();
    private static HashMap<String, Integer> idMap = new HashMap<>();

    private Location mLastLocation;

    private ProgressBar progressBarPB;
    private RecyclerView.Adapter mAdapter;

    private DatabaseReference mSpotfixReference;
    private DatabaseReference mGeofireReference;
    private ValueEventListener mSpotfixListener;

    private FusedLocationProviderClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase database and storage
        mSpotfixReference = FirebaseDatabase.getInstance().getReference().child("Spotfixes");
        mGeofireReference = FirebaseDatabase.getInstance().getReference().child("Geofire");

        // Initialize views
        RecyclerView mRecyclerView = findViewById(R.id.spotfixesRV);
        FloatingActionButton addSpotfixButton = findViewById(R.id.addSpotfixButton);
        progressBarPB = findViewById(R.id.progressBar);

        // use this setting to improve performance of RecyclerView whenever layout of view does not change when content changes
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        //specify an adapter
        mAdapter = new SpotfixRecyclerViewAdapter(cardList);
        mRecyclerView.setAdapter(mAdapter);

        addSpotfixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CreateSpotfixActivity.class));
            }
        });

        client = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        if(checkPermission())
            requestPermissions();
        else
            getLastLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(checkPermission())
            requestPermissions();
        else
            getLastLocation();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        client.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful() && task.getResult() != null) {
                            mLastLocation = task.getResult();
                            populateMainActivityUsingGeofire();
                        }
                        else {
                            Log.e(TAG, "getLastLocation():exception", task.getException());
                            showSnackbar(getString(R.string.no_location_detected));
                        }
                    }
                });
    }

    private void showSnackbar(final String text) {
        View container = findViewById(R.id.container);
        if(container != null)
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content), getText(mainTextStringId), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener)
                .show();
    }

    private void populateMainActivityUsingGeofire() {
        double currentLatitude = mLastLocation.getLatitude();
        double currentLongitude = mLastLocation.getLongitude();
        GeoFire geofire = new GeoFire(mGeofireReference);
        GeoQuery query = geofire.queryAtLocation(new GeoLocation(currentLatitude, currentLongitude), 3.0);

        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                showProgressBar();
                populateRecyclerView(key);
                hideProgressBar();
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                showProgressBar();
                populateRecyclerView(key);
                hideProgressBar();
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Error while loading data", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error detected in Geofire", error.toException());
            }
        });
    }

    private void populateRecyclerView(String key) {
        mSpotfixReference.child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int spotfixCardIndex = 0;
                boolean newSpotfix = true;
                Spotfix spotfix = dataSnapshot.getValue(Spotfix.class);
                if(spotfix != null) {
                    String spotfixId = spotfix.getSpotfixId();
                    if(idMap.containsKey(spotfixId)) {
                        newSpotfix = false;
                        spotfixCardIndex = idMap.get(spotfixId);
                    }
                    else
                        idMap.put(spotfixId, cardList.size());
                    if(!newSpotfix)
                        cardList.set(spotfixCardIndex, spotfix);
                    else
                        cardList.add(spotfix);

                    mAdapter.notifyDataSetChanged();
                }
                else {
                    Toast.makeText(MainActivity.this, "Spotfix could not be retrieved. Please check your internet connection", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Firebase database error", databaseError.toException());
            }
        });
    }

//
//    private void populateMainActivity() {
//        final ValueEventListener spotfixListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for (DataSnapshot child : dataSnapshot.getChildren()) {
//                    showProgressBar();
//                    String key = child.getKey();
//                    populateRecyclerView(key);
//                    hideProgressBar();
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(MainActivity.this, "Failed to load spotfixes", Toast.LENGTH_SHORT).show();
//            }
//        };
//        mSpotfixReference.addValueEventListener(spotfixListener);
//        mSpotfixListener = spotfixListener;
//    }

    @Override
    protected void onStop() {
        super.onStop();

        // Remove profile value event listener
        if(mSpotfixListener != null)
            mSpotfixReference.removeEventListener(mSpotfixListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_profile: {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                break;
            }
            case R.id.action_logout: {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    private void showProgressBar() {
        progressBarPB.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void hideProgressBar() {
        progressBarPB.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if(shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional content");

            showSnackbar(R.string.permission_rationale, android.R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startLocationPermissionRequest();
                }
            });
        }

        else {
            Log.i(TAG, "Requesting permission without showing permission rationale");

            startLocationPermissionRequest();
        }
    }

    @Override
    public void onRequestPermissionsResult(int resultCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult");
        switch(resultCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                }
                else {
                    showSnackbar(R.string.permission_denied_explanation, R.string.settings, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                            intent.setData(uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
                }
                break;
            }
        }
    }

}
