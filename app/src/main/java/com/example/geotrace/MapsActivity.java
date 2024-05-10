package com.example.geotrace;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Declare the map name
    private GoogleMap mMap;

    // Timer variable declarations
    private TextView timerTextView;
    private int secondsPassed = 0;
    private Handler handler = new Handler();
    private Runnable runnable;

    // Fused location client
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    // Polyline and polygon related variables
    private PolylineOptions polylineOptions;
    private Polyline polyline;
    private ArrayList<LatLng> userLocations = new ArrayList<>();
    private Button saveLocationButton;
    private static final double POINTS_PER_SQUARE_METER = 0.5;

    // Radius detection variables
    private LatLng originalLocation;
    private static final double RADIUS_THRESHOLD = 50;

    // Media player variables
    private MediaPlayer mediaPlayer;

    // Create the map view, initialise timer and initialise the fused locator method.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        saveLocationButton = findViewById(R.id.saveLocationButton);
        saveLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentLocation();
            }
        });
        Button drawPolylineButton = findViewById(R.id.drawPolylineButton);
        drawPolylineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawPolygon();
            }
        });

        timerTextView = findViewById(R.id.timerTextView);
        //Display the map within the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize MediaPlayer with background music file, set it to loop  and start playing
        mediaPlayer = MediaPlayer.create(this, R.raw.backgroundmusic);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void saveCurrentLocation() {
        if (ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(
                                    location.getLatitude(),
                                    location.getLongitude());
                            userLocations.add(currentLocation);
                            originalLocation = currentLocation;

                            // Added markers in order to see that the add point button is
                            // functioning correctly -  remove for submission
                            //mMap.addMarker(new MarkerOptions().position(currentLocation).title("Saved Location"));

                            if (polyline != null) {
                                polyline.remove();
                            }

                            // Create PolylineOptions to define the polylines visual properties
                            polylineOptions = new PolylineOptions();
                            polylineOptions.color(Color.BLUE);
                            polylineOptions.width(10);

                            // Add each point in userLocations to the polyline
                            for (LatLng latLng : userLocations) {
                                polylineOptions.add(latLng);
                            }

                            // Add the polyline to the map
                            polyline = mMap.addPolyline(polylineOptions);
                            // Display a toast message indicating that the location is saved for testing purposes
                            //Toast.makeText(MapsActivity.this, "Location saved" + location, Toast.LENGTH_LONG).show();
                        } else {
                            // If location is null, display a message indicating that location is not available for testing purposes
                            Toast.makeText(MapsActivity.this, "Location not available", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    // Method to create a polyline between lat and long coordinates saved into the array
    // if there are 3 or fewer points, gives a toast message to display the encountered error
    //
    // The drawPolyline() method checks if the user's latest location
    // is within the radius threshold of the original location. If it is, it will draw a
    // polygon using the stored locations instead of a polyline.
    // Otherwise, it will draw a polyline as before.

    private void drawPolygon() {
        if (userLocations.size() < 3) {
            Toast.makeText(this, "At least three locations are required to draw a shape.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the current location is within the radius of the original location,
        if (originalLocation != null && isWithinRadius(originalLocation, userLocations.get(userLocations.size() - 1),
                RADIUS_THRESHOLD)) {
            if (polyline != null) {
                polyline.remove();
            }

            // Create PolygonOptions object to define polygon properties
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.strokeColor(Color.RED);
            polygonOptions.fillColor(Color.argb(128, 255, 0, 0)); // Set fill color of the polygon

            // Add each point in userLocations to the polygon
            for (LatLng latLng : userLocations) {
                polygonOptions.add(latLng);
            }

            mMap.addPolygon(polygonOptions);

            double area = calculateArea(userLocations);

            mMap.clear();

            double pointsAwarded = calculatePoints(userLocations);

            TextView scoreTextView = findViewById(R.id.scoreTextView);
            scoreTextView.setText(String.format("Score: %.2f", pointsAwarded));

            Toast.makeText(this, "Points awarded: " + pointsAwarded, Toast.LENGTH_SHORT).show();

            userLocations.clear();
        } else {
            Toast.makeText(this, "An error has occurred when drawing the polygon", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to calculate the area of a polygon
    private double calculateArea(ArrayList<LatLng> points) {
        List<LatLng> mapPoints = new ArrayList<>();
        for (LatLng point : points) {
            mapPoints.add(new com.google.android.gms.maps.model.LatLng(point.latitude, point.longitude));
        }
        PolygonOptions polygonOptions = new PolygonOptions();
        for (com.google.android.gms.maps.model.LatLng latLng : mapPoints) {
            polygonOptions.add(latLng);
        }
        Polygon polygon = mMap.addPolygon(polygonOptions);
        return SphericalUtil.computeArea(polygon.getPoints());
    }

    // Method to award points based on the area of the shape
    // Method to calculate the points awarded based on the area of the drawn shape
    private double calculatePoints(ArrayList<LatLng> points) {
        double area = calculateArea(points);
        double pointsAwarded = area * POINTS_PER_SQUARE_METER;
        pointsAwarded = Math.round(pointsAwarded * 100.0) / 100.0;
        return pointsAwarded;
    }


    // Method to check if a set point is within a certain radius of another point
    private boolean isWithinRadius(LatLng center, LatLng point, double radius) {
        double distance = SphericalUtil.computeDistanceBetween(center, point);
        return distance <= radius;
    }

    // Call the map api, manifest declarations grant permission for the application to find users location.
    // Start the location updates.
    // Begin timer counting up
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        mMap.setMyLocationEnabled(true);

        startLocationUpdates();

        startTimer();

    }

    protected void onPause() {
        super.onPause();
        // Pause or stop the MediaPlayer when the activity is paused
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        // Check if the screen is locked and stop the background music if it is
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            mediaPlayer.pause();
        }
    }

    // Fix for the problem of the music continuing after the screen is locked or
    // the applicatoin is minimised. Call onResume to check if media player has been
    // initiated, was playing, and if it has, resume the track

    protected void onResume() {
        super.onResume();
        // Check if the MediaPlayer was playing before the activity was paused
        // If it was playing, resume playing the music
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000); // 5 seconds interval

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    LatLng currentLocation = new LatLng(
                            location.getLatitude(),
                            location.getLongitude());

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(currentLocation)
                            .zoom(17)
                            .tilt(45) // angle (in degrees)
                            .build();

                    // Update the camera view
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void startTimer() {
        runnable = new Runnable() {
            @Override
            public void run() {
                secondsPassed++;
                int hours = secondsPassed / 3600;
                int minutes = (secondsPassed % 3600) / 60;
                int seconds = secondsPassed % 60;
                timerTextView.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable); // Start the timer runnable
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the callback to prevent memory leaks
        handler.removeCallbacks(runnable);

        stopLocationUpdates();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
