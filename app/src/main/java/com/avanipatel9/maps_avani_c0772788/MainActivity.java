package com.avanipatel9.maps_avani_c0772788;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener {

    private static  final String TAG = "MainActivity";

    private static final int ERROR_DIALOG_REQUEST = 9001;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;

    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private static final int POLYGON_SIDES = 4;

    Polyline line;
    Polygon shape;
    List<Marker> markersList = new ArrayList<>();
    List<Marker> distanceMarkers = new ArrayList<>();
    ArrayList<Polyline> polylinesList = new ArrayList<>();

    List<Marker> cityMarkers = new ArrayList<>();
    ArrayList<Character> letterList = new ArrayList<>();
    HashMap<LatLng, Character> markerLabelMap = new HashMap<>();

    private boolean mLocationPermissionGranted = false;

    private GoogleMap mMap;

    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isServicesOK()) {
            getLocationPermission();
        }
    }

    public  boolean isServicesOK() {
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if(available == ConnectionResult.SUCCESS) {
            // everything is OK
            Log.d(TAG, "isServicesOK: google play services is working");
            return true;
        }
        else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // an error occurred but we can resolve
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }
        else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void startUpdateLocations() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);

    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public String getMarkerDistance(Polyline polyline){
        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);


        double distance = distance(firstPoint.latitude,firstPoint.longitude,
                secondPoint.latitude,secondPoint.longitude);
        NumberFormat formatter = new DecimalFormat("#0.0");
        return formatter.format(distance) + " KM";
    }

    private void setMarker(LatLng latLng){

        Geocoder geoCoder = new Geocoder(this);
        Address address = null;

        try
        {
            List<Address> matches = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            address = (matches.isEmpty() ? null : matches.get(0));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        String title = "";
        String snippet = "";
        //Getting title and snippet
        ArrayList<String> titleString = new ArrayList<>();
        ArrayList<String> snippetString = new ArrayList<>();

        if(address != null){
            if(address.getSubThoroughfare() != null)
            {
                titleString.add(address.getSubThoroughfare());

            }
            if(address.getThoroughfare() != null)
            {

                titleString.add(address.getThoroughfare());

            }
            if(address.getPostalCode() != null)
            {

                titleString.add(address.getPostalCode());

            }
            if(titleString.isEmpty())
            {
                titleString.add("Unknown Location");
            }
            if(address.getLocality() != null)
            {
                snippetString.add(address.getLocality());

            }
            if(address.getAdminArea() != null)
            {
                snippetString.add(address.getAdminArea());
            }
        }
        //Building title string using TextUtils
        title = TextUtils.join(", ",titleString);
        title = (title.equals("") ? "  " : title);

        snippet = TextUtils.join(", ",snippetString);

        MarkerOptions options = new MarkerOptions().position(latLng)
                .draggable(true)
                .title(title)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .snippet(snippet);

        // check if there are already the same number of markers, we clear the map
        if (markersList.size() == POLYGON_SIDES)
        {
            clearMap();
        }

        Marker mm = mMap.addMarker(options);
        markersList.add(mm);

        if (markersList.size() == POLYGON_SIDES) {
            drawShape();
        }

        // Add city Label Marker
        Character cityLetters = 'A';
        Character[] arr = {'A','B','C','D'};
        for(Character letter: arr){
            if(letterList.contains(letter)){
                continue;
            }
            cityLetters = letter;
            break;
        }

        LatLng labelLatLng = new LatLng(latLng.latitude - 0.55,latLng.longitude);
        MarkerOptions optionsCityLabel = new MarkerOptions().position(labelLatLng)
                .draggable(false)
                .icon(displayText(cityLetters.toString()))
                .snippet(snippet);
        Marker letterMarker = mMap.addMarker(optionsCityLabel);

        cityMarkers.add(letterMarker);
        letterList.add(cityLetters);
        markerLabelMap.put(letterMarker.getPosition(),cityLetters);
    }

    //drawing polygon with 4 markers
    private void drawShape (){
        PolygonOptions options = new PolygonOptions()
                .fillColor(Color.argb(35, 0, 255, 0))
                .strokeColor(Color.RED);

        LatLng[] markersConvex = new LatLng[POLYGON_SIDES];
        for (int i = 0; i < POLYGON_SIDES; i++) {
            markersConvex[i] = new LatLng(markersList.get(i).getPosition().latitude,
                    markersList.get(i).getPosition().longitude);
        }

        Vector<LatLng> sortedLatLong = PointPlotter.convexHull(markersConvex, POLYGON_SIDES);

        // get sortedLatLong
        Vector<LatLng> sortedLatLong2 =  new Vector<>();

        // leftmost marker for convex hull formula
        int l = 0;
        for (int i = 0; i < markersList.size(); i++)
            if (markersList.get(i).getPosition().latitude < markersList.get(l).getPosition().latitude)
                l = i;
        //using counter clockwise rotation
        Marker currentMarker = markersList.get(l);
        sortedLatLong2.add(currentMarker.getPosition());
        while(sortedLatLong2.size() != POLYGON_SIDES){
            double minDistance = Double.MAX_VALUE;
            Marker nearestMarker  = null;
            for(Marker marker: markersList){
                if(sortedLatLong2.contains(marker.getPosition())){
                    continue;
                }

                double curDistance = distance(currentMarker.getPosition().latitude,
                        currentMarker.getPosition().longitude,
                        marker.getPosition().latitude,
                        marker.getPosition().longitude);

                if(curDistance < minDistance){
                    minDistance = curDistance;
                    nearestMarker = marker;
                }
            }

            if(nearestMarker != null){
                sortedLatLong2.add(nearestMarker.getPosition());
                currentMarker = nearestMarker;
            }
        }
        System.out.println(sortedLatLong);

        // add polygon as per convex hull lat long
        options.addAll(sortedLatLong);
        shape = mMap.addPolygon(options);
        shape.setClickable(true);

        // draw the polyline too
        LatLng[] polyLinePoints = new LatLng[sortedLatLong.size() + 1];
        int index = 0;
        for (LatLng x : sortedLatLong) {
            polyLinePoints[index] = x;

            index++;
            if (index == sortedLatLong.size()) {
                // at last add initial point
                polyLinePoints[index] = sortedLatLong.elementAt(0);
            }
        }
        for(int i =0 ; i<polyLinePoints.length -1 ; i++){

            LatLng[] tempArr = {polyLinePoints[i], polyLinePoints[i+1] };
            Polyline currentPolyline =  mMap.addPolyline(new PolylineOptions()
                    .clickable(true)
                    .add(tempArr)
                    .color(Color.RED));
            currentPolyline.setClickable(true);
            polylinesList.add(currentPolyline);
        }
    }

    private void clearMap() {
        for (Marker marker : markersList) {
            marker.remove();
        }
        markersList.clear();

        for(Polyline line: polylinesList){
            line.remove();
        }
        polylinesList.clear();

        shape.remove();
        shape = null;

        for (Marker marker : distanceMarkers) {
            marker.remove();
        }
        distanceMarkers.clear();

        for( Marker marker: cityMarkers){
            marker.remove();
        }
        cityMarkers.clear();
        letterList.clear();

    }

    public BitmapDescriptor displayText(String text) {

        Paint textPaint = new Paint();

        textPaint.setTextSize(48);
        float textWidth = textPaint.measureText(text);
        float textHeight = textPaint.getTextSize();
        int width = (int) (textWidth);
        int height = (int) (textHeight);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);

        canvas.translate(0, height);

        canvas.drawText(text, 0, 0, textPaint);
        return BitmapDescriptorFactory.fromBitmap(image);
    }

    public String getTotalDistance(ArrayList<Polyline> polylines){

        double totalDistance = 0;
        for(Polyline polyline : polylines){
            List<LatLng> points = polyline.getPoints();
            LatLng firstPoint = points.remove(0);
            LatLng secondPoint = points.remove(0);


            double distance = distance(firstPoint.latitude,firstPoint.longitude,
                    secondPoint.latitude,secondPoint.longitude);
            totalDistance += distance;

        }
        NumberFormat formatter = new DecimalFormat("#0.0");

        return formatter.format(totalDistance) + " KM";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i=0; i<grantResults.length; i++) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed.");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted.");
                    mLocationPermissionGranted = true;
                    //initialize map
                    initMap();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;

        mMap.setOnMapLongClickListener(this);
        mMap.setOnMapClickListener(this);
        mMap.setOnPolylineClickListener(this);
        mMap.setOnPolygonClickListener(this);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        startUpdateLocations();
        LatLng canadaCenterLatLong = new LatLng( 43.651070,-79.347015);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(canadaCenterLatLong, 5));

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                System.out.println("marker Clicked "+ marker.isInfoWindowShown());
                if(marker.isInfoWindowShown()){
                    marker.hideInfoWindow();
                }
                else{
                    marker.showInfoWindow();
                }
                return true;
            }
        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                if (markersList.size() == POLYGON_SIDES) {
                    for(Polyline line: polylinesList){
                        line.remove();
                    }
                    polylinesList.clear();

                    shape.remove();
                    shape = null;

                    for(Marker currMarker: distanceMarkers){
                        currMarker.remove();
                    }
                    distanceMarkers.clear();
                    drawShape();
                }
            }
        });

    }

    @Override
    public void onMapClick(LatLng latLng) {
        setMarker(latLng);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if(markersList.size() == 0){
            return;
        }
        double minDistance = Double.MAX_VALUE;
        Marker nearestMarker = null;

        for(Marker marker: markersList){
            double currDistance = distance(marker.getPosition().latitude,
                    marker.getPosition().longitude,
                    latLng.latitude,
                    latLng.longitude);
            if(currDistance < minDistance){
                minDistance = currDistance;
                nearestMarker = marker;
            }
        }

        if(nearestMarker != null){
            final Marker finalNearestMarker = nearestMarker;
            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);

            deleteDialog
                    .setTitle("Delete?")
                    .setMessage("Would you like to delete the marker?")

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                            finalNearestMarker.remove();
                            markersList.remove(finalNearestMarker);

                            letterList.remove(markerLabelMap.get(finalNearestMarker.getPosition()));
                            letterList.clear();
                            cityMarkers.clear();
                            markerLabelMap.remove(finalNearestMarker);
                            markerLabelMap.clear();

                            for(Polyline polyline: polylinesList){
                                polyline.remove();
                            }
                            polylinesList.clear();

                            if(shape != null){
                                shape.remove();
                                shape = null;
                            }

                            for(Marker currMarker: distanceMarkers){
                                currMarker.remove();
                            }
                            distanceMarkers.clear();

                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finalNearestMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker));

                        }
                    });
            AlertDialog dialog = deleteDialog.create();
            dialog.show();
        }
    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for(LatLng point: polygon.getPoints()){
            builder.include(point);
        }
        LatLng center = builder.build().getCenter();
        MarkerOptions options = new MarkerOptions().position(center)
                .draggable(true)
                .icon(displayText(getTotalDistance(polylinesList)));
        distanceMarkers.add(mMap.addMarker(options));
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        List<LatLng> points = polyline.getPoints();
        LatLng firstPoint = points.remove(0);
        LatLng secondPoint = points.remove(0);

        LatLng center = LatLngBounds.builder().include(firstPoint).include(secondPoint).build().getCenter();
        MarkerOptions options = new MarkerOptions().position(center)
                .draggable(true)
                .icon(displayText(getMarkerDistance(polyline)));
        distanceMarkers.add(mMap.addMarker(options));
    }

}