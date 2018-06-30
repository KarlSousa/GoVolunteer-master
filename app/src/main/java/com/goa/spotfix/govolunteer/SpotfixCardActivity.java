package com.goa.spotfix.govolunteer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class SpotfixCardActivity extends AppCompatActivity {

    private int zoomLevel = 12;

    private LatLng spotfixLocation;

    private Spotfix spotfix;

    private MapView mMapView;

    private GoogleMap mGoogleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotfix_card);

        mMapView = findViewById(R.id.spotfixMapView);
        ImageButton spotfixLocationButton = findViewById(R.id.spotfixLocationBtn);
        ImageButton zoomInButton = findViewById(R.id.zoomInBtn);
        ImageButton zoomOutButton = findViewById(R.id.zoomOutBtn);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            spotfix = (Spotfix) getIntent().getSerializableExtra("spotfix");
            mMapView.onCreate(savedInstanceState);
            mapIntent();
        } else {
            Toast.makeText(SpotfixCardActivity.this, "An error occured. Please install the app again and try this feature", Toast.LENGTH_SHORT).show();
            finish();
        }

        spotfixLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spotfixLocation, zoomLevel));
            }
        });

        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (zoomLevel < 21)
                    zoomLevel++;
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spotfixLocation, zoomLevel));
            }
        });

        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (zoomLevel > 1)
                    zoomLevel--;
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spotfixLocation, zoomLevel));
            }
        });
    }

    private void mapIntent() {
        mMapView.onResume();
        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap mMap) {
                mGoogleMap = mMap;
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                spotfixLocation = new LatLng(spotfix.getLatitude(), spotfix.getLongitude());
                mMap.addMarker(new MarkerOptions().position(spotfixLocation).title(spotfix.getPlaceName()));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spotfixLocation, zoomLevel));
            }
        });
        populateDataFromFirebase();
    }

    private void populateDataFromFirebase() {
        TextView spotfixAddress = findViewById(R.id.spotfixAddress);
        TextView spotfixDate = findViewById(R.id.spotfixDate);
        TextView spotfixTime = findViewById(R.id.spotfixTime);
        TextView spotfixPplReq = findViewById(R.id.spotfixPeopleReq);
        TextView spotfixPplJoin = findViewById(R.id.spotfixPeopleJoined);

        spotfixAddress.setText(spotfix.getAddress());
        spotfixDate.setText(spotfix.getDate());
        spotfixTime.setText(spotfix.getTime());
        spotfixPplReq.setText(String.valueOf(spotfix.getPeopleRequired()));
        spotfixPplJoin.setText(String.valueOf(spotfix.getNoOfPeopleJoined()));
    }
}