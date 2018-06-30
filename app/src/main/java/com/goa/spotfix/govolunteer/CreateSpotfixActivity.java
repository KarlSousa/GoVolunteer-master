package com.goa.spotfix.govolunteer;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class CreateSpotfixActivity extends AppCompatActivity {


    private byte[] spotfixByteImage = null;
    static final int REQUEST_CAMERA = 1;
    static final int SELECT_IMAGE = 2;
    private static final int DATE_DIALOG_ID = 999;
    private int mYear = -1, mMonth = -1, mDay = -1, mHour = -1, mMinute = -1;
    private int noOfReqPeople;
    private static final String TAG = "CreateSpotfixActivity";
    private String userId;
    private String spotfixId;
    private String userChosenTask;
    private CharSequence placeName = null;
    private CharSequence placeAddress = null;
    private LatLng placeLatLng = null;

    private EditText noOfPeopleET;
    private EditText spotfixDateET;
    private EditText spotfixTimeET;
    private Button addSpotfixImageBtn;
    private Button addSpotfixBtn;
    private ImageView spotfixImage;
    private ProgressBar progressBarPB;
    private PlaceAutocompleteFragment placeET;

    private FirebaseStorage mImagesStorage;
    private DatabaseReference mSpotfixReference, mUsersReference, mGeoFireReference;
    private ValueEventListener mProfileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_spotfix);

        noOfPeopleET = findViewById(R.id.noOfReqPeopleET);
        spotfixDateET = findViewById(R.id.spotfixDateET);
        spotfixTimeET = findViewById(R.id.spotfixTimeET);
        addSpotfixImageBtn = findViewById(R.id.addSpotfixImageBtn);
        addSpotfixBtn = findViewById(R.id.addSpotfixButton);
        spotfixImage = findViewById(R.id.spotfixIV);
        progressBarPB = findViewById(R.id.progressBar);
        placeET = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        EditText etPlace = placeET.getView().findViewById(R.id.place_autocomplete_search_input);

        // Retrieve user id to fetch profile from Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Retrieve user id
        userId = currentUser.getUid();
        spotfixId = userId + UUID.randomUUID().toString().replace("-", "");

        // Initialize Firebase database and storage
        mUsersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        mSpotfixReference = FirebaseDatabase.getInstance().getReference().child("Spotfixes").child(spotfixId);
        mGeoFireReference = FirebaseDatabase.getInstance().getReference().child("Geofire");
        mImagesStorage = FirebaseStorage.getInstance();

        etPlace.setHint("Enter spotfix location");
        /*
         * The following code example shows setting an AutocompleteFilter on a PlaceAutocompleteFragment to
         * set a filter returning only results with a precise address.
         */
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .build();
        placeET.setFilter(typeFilter);

        placeET.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            @Override
            public void onPlaceSelected(Place place) {
                Log.i(TAG, "Place: " + place.getName());//get place details here
                placeName = place.getName();
                placeAddress = place.getAddress();
                placeLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {
                Log.i(TAG, "An error occurred: " + status);
                Toast.makeText(CreateSpotfixActivity.this, "Please specify a location with an exact address and ensure u have an active internet connection", Toast.LENGTH_SHORT).show();
            }
        });

        spotfixDateET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrentDateOnView();
                showDialog(DATE_DIALOG_ID);
            }
        });

        spotfixTimeET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                int localHour = c.get(Calendar.HOUR_OF_DAY);
                int localMinute = c.get(Calendar.MINUTE);

                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(CreateSpotfixActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                spotfixTimeET.setText(new StringBuilder().append(hourOfDay).append(":").append(minute));
                                mHour = hourOfDay;
                                mMinute = minute;
                            }
                        }, localHour, localMinute, false);
                timePickerDialog.show();

            }
        });

        addSpotfixImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        addSpotfixBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateData()) {
                    showProgressBar();
                    uploadImageToFirebase(spotfixByteImage);
                    hideProgressBar();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Remove profile value event listener
        if(mProfileListener != null)
            mUsersReference.removeEventListener(mProfileListener);
    }

    private boolean validateData() {

        if(spotfixByteImage == null) {
            Toast.makeText(CreateSpotfixActivity.this, "Please upload an image of the spotfix", Toast.LENGTH_SHORT).show();
            spotfixImage.requestFocus();
            return false;
        }

        if(placeName == null || placeAddress == null || placeLatLng == null) {
            Toast.makeText(CreateSpotfixActivity.this, "Please enter the location of the spotfix", Toast.LENGTH_SHORT).show();
            return false;
        }

        if((!TextUtils.isDigitsOnly(noOfPeopleET.getText())) || noOfPeopleET.getText().toString().trim().isEmpty()) {
            noOfPeopleET.setError("Number of people has to be numeric");
            noOfPeopleET.requestFocus();
            return false;
        }
        else {
            noOfReqPeople = Integer.parseInt(noOfPeopleET.getText().toString().trim());
        }

        if(mYear == -1 || mMonth == -1 || mDay == -1) {
            spotfixDateET.setError("Please select a date for the spotfix");
            spotfixDateET.requestFocus();
            return false;
        }

        if(mHour == -1 || mMinute == -1) {
            spotfixTimeET.setError("Please select a time for the spotfix");
            spotfixTimeET.requestFocus();
            return false;
        }

        return true;
    }

    // display current date
    private void setCurrentDateOnView() {

        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        // set current date into textview
        spotfixDateET.setText(new StringBuilder()
                // Month is 0 based, just add 1
                .append(mMonth + 1).append("-").append(mDay).append("-")
                .append(mYear));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DATE_DIALOG_ID:
                return new DatePickerDialog(CreateSpotfixActivity.this, datePickerListener, mYear,mMonth,
                        mDay){
                    @Override
                    public void onDateChanged(@NonNull DatePicker view, int year, int monthOfYear, int dayOfMonth)
                    {
                        if (year < mYear)
                            view.updateDate(mYear, mMonth, mDay);

                        if (monthOfYear < mMonth && year == mYear)
                            view.updateDate(mYear, mMonth, mDay);

                        if (dayOfMonth < mDay && year == mYear && monthOfYear == mMonth)
                            view.updateDate(mYear, mMonth, mDay);

                    }
                };
        }
        return null;
    }

    private DatePickerDialog.OnDateSetListener datePickerListener = new DatePickerDialog.OnDateSetListener() {

        // when dialog box is closed, below method will be called.
        public void onDateSet(DatePicker view, int selectedYear,
                              int selectedMonth, int selectedDay) {
            mYear = selectedYear;
            mMonth = selectedMonth;
            mDay = selectedDay;

            // set selected date into textview
            spotfixDateET.setText(new StringBuilder().append(mDay)
                    .append("-").append(mMonth + 1).append("-").append(mYear)
                    .append(" "));
        }
    };

    private void selectImage() {
        final CharSequence[] dialogItems = {"Take Photo", "Choose from Library", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Edit Spotfix Image!")
                .setItems(dialogItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int item) {
                        if(dialogItems[item].equals("Take Photo")) {
                            boolean cameraResult = MarshmellowPermissions.checkCameraPermission(CreateSpotfixActivity.this);
                            userChosenTask = "Take Photo";
                            if(cameraResult)
                                cameraIntent();
                            else
                                Toast.makeText(CreateSpotfixActivity.this, "Permission to read and write to external files is required to use this feature. Please enable it before trying again.", Toast.LENGTH_SHORT).show();
                        }
                        else if(dialogItems[item].equals("Choose from Library")) {
                            boolean storageResult = MarshmellowPermissions.checkExternalStoragePermission(CreateSpotfixActivity.this);
                            userChosenTask = "Choose from Library";
                            if(storageResult)
                                galleryIntent();
                            else
                                Toast.makeText(CreateSpotfixActivity.this, "Permission to read and write to external files is required to use this feature. Please enable it before trying again.", Toast.LENGTH_SHORT).show();
                        }
                        else if(dialogItems[item].equals("Cancel")) {
                            dialogInterface.dismiss();
                        }
                    }
                })
                .create()
                .show();
    }
    private void cameraIntent() {
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            Toast.makeText(CreateSpotfixActivity.this, "Camera not found. This feature cannot be used", Toast.LENGTH_SHORT).show();
        else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case MarshmellowPermissions.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChosenTask.equals("Choose from Library"))
                        galleryIntent();
                }
                else {
                    Toast.makeText(CreateSpotfixActivity.this, "These permissions are necessary to use this feature of the app. Please grant them and try again", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK) {
            if(requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
            else if(requestCode == SELECT_IMAGE)
                onSelectFromGalleryResult(data);
        }
    }

    private void onCaptureImageResult(@NonNull Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        if (thumbnail != null) {
            // Create BLOB and upload it to firebase storage as shown in Firebase documentation
            ByteArrayOutputStream boas = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, boas);
            spotfixByteImage = boas.toByteArray();

            Glide.with(CreateSpotfixActivity.this)
                    .load(spotfixByteImage)
                    .asBitmap()
                    .into(spotfixImage);
        }
        else {
            Log.e(TAG, "onCaptureImageResult() - thumbnail is null. Reason unknown. Image will have to be captured again");
            Toast.makeText(CreateSpotfixActivity.this, "Error while generating image. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSelectFromGalleryResult(@NonNull Intent data) {
        Bitmap thumbnail = null;
        try {
            thumbnail = MediaStore.Images.Media.getBitmap(CreateSpotfixActivity.this.getContentResolver(), data.getData());
        } catch(IOException ie) {
            Log.e(TAG, "onSelectFromGalleryResult:IOException occurred. Requested image could not be fetched.", ie.fillInStackTrace());
            Toast.makeText(CreateSpotfixActivity.this, "Requested image could not be fetched. Please try again", Toast.LENGTH_SHORT).show();
        }

        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        if (thumbnail != null) {
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, boas);
            spotfixByteImage = boas.toByteArray();

            Glide.with(CreateSpotfixActivity.this)
                    .load(spotfixByteImage)
                    .asBitmap()
                    .into(spotfixImage);
        }
        else {
            Log.e(TAG, "onSelectFromGalleryResult - profileImage is null. Reason unknown. Image will have to be fetched again");
            Toast.makeText(CreateSpotfixActivity.this, "An error occurred. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase(byte[] spotfixByteImage) {
        final String photoURL = "images/spotfixes/" + spotfixId;

        // Update photoURL in Realtime Database in Firebase
        mSpotfixReference.child("photoURL").setValue(photoURL);

        // Create reference to store image in Firebase Storage and upload image
        final StorageReference profilesReference = mImagesStorage.getReference().child(photoURL);
        UploadTask uploadTask = profilesReference.putBytes(spotfixByteImage);
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = Math.round((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                Toast.makeText(CreateSpotfixActivity.this, "Image is being uploaded: " + String.format(Locale.getDefault(), "%.0f", progress) + "% uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(CreateSpotfixActivity.this, "Upload has been paused", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Image upload failed", e.fillInStackTrace());
                Toast.makeText(CreateSpotfixActivity.this, "Image could not be uploaded. Please try again", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mImagesStorage.getReference().child(photoURL).getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
//                                Toast.makeText(CreateSpotfixActivity.this, "Image has been uploaded", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Loading of image failed after upload. Please do not re-upload", e.fillInStackTrace());
                            }
                        });
                Toast.makeText(CreateSpotfixActivity.this, "Image has been uploaded successfully!", Toast.LENGTH_SHORT).show();
                uploadSpotfixToFirebase();
            }
        });
    }

    private void showProgressBar() {
        progressBarPB.setVisibility(View.VISIBLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void hideProgressBar() {
        progressBarPB.setVisibility(View.GONE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void uploadSpotfixToFirebase() {
        double latitude = placeLatLng.latitude;
        double longitude = placeLatLng.longitude;
        String name = placeName.toString();
        String address = placeAddress.toString();
        String date = String.valueOf(mDay) + "-" + (String.valueOf(mMonth + 1)) + "-" + String.valueOf(mYear);
        String time = String.valueOf(mHour) + ":"  + String.valueOf(mMinute);

        mSpotfixReference.child("uploaderId").setValue(userId);
        mSpotfixReference.child("placeName").setValue(name);
        mSpotfixReference.child("address").setValue(address);
        mSpotfixReference.child("latitude").setValue(latitude);
        mSpotfixReference.child("longitude").setValue(longitude);
        mSpotfixReference.child("peopleRequired").setValue(noOfReqPeople);
        mSpotfixReference.child("noOfPeopleJoined").setValue(0);
        mSpotfixReference.child("date").setValue(date);
        mSpotfixReference.child("time").setValue(time);
        mSpotfixReference.child("spotfixId").setValue(spotfixId);

        GeoFire geoFire = new GeoFire(mGeoFireReference);
        geoFire.setLocation(spotfixId, new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if(error != null) {
                    Log.e(TAG, "There was an error in geofire.", error.toException());
                }
                else {
                    Log.d(TAG, "Geofire executed successfully");
                }
            }
        });

        ValueEventListener profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(user != null) {
                    mUsersReference.child("spotfixes_created").setValue(user.getSpotfixes_created() + 1);
                }
                else {
                    Log.e(TAG, "User object returned null. Check reason and fix it");
                   // Toast.makeText(CreateSpotfixActivity.this, "Could not update your profile.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled was called for some reason", databaseError.toException());
                Toast.makeText(CreateSpotfixActivity.this, "Profile updation operation was cancelled. Please contact the administrator", Toast.LENGTH_SHORT).show();
            }
        };
        mUsersReference.addListenerForSingleValueEvent(profileListener);
        mProfileListener = profileListener;

        Toast.makeText(CreateSpotfixActivity.this, "Spotfix has been uploaded successfully!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(CreateSpotfixActivity.this, MainActivity.class));
    }

}
