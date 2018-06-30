package com.goa.spotfix.govolunteer;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
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
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    static final int REQUEST_CAMERA = 1;
    static final int SELECT_IMAGE = 2;
    private String TAG = "ProfileActivity";
    private String userId;
    private String userChosenTask;

    private TextView createdSpotfixTV;
    private TextView attendedSpotfixTV;
    private TextView userEmailTV;
    private TextView userNameTV;
    private ImageView profilePictureIV;
    private ProgressBar progressBarPB;

    private FirebaseStorage mImagesStorage;
    private DatabaseReference mUsersReference;
    private ValueEventListener mProfileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Retrieve user id to fetch profile from Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Retrieve user id
        userId = currentUser.getUid();

        // Initialize Firebase database and storage
        mUsersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);
        mImagesStorage = FirebaseStorage.getInstance();

        // Initialize views
        createdSpotfixTV = findViewById(R.id.spotfixCreatedTV);
        attendedSpotfixTV = findViewById(R.id.spotfixAttendedTV);
        userEmailTV = findViewById(R.id.userEmailTV);
        userNameTV = findViewById(R.id.userNameTV);
        profilePictureIV = findViewById(R.id.profilePictureIV);
        progressBarPB = findViewById(R.id.progressBar);

        profilePictureIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        ValueEventListener profileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Get user profile information and update the views
                User user = dataSnapshot.getValue(User.class);
                if (user != null) {
                    userNameTV.setText(user.getName());
                    userEmailTV.setText(user.getEmail());
                    createdSpotfixTV.setText(String.format(Locale.getDefault(), "%d", user.getSpotfixes_created()));
                    attendedSpotfixTV.setText(String.format(Locale.getDefault(), "%d", user.getSpotfixes_attented()));
                    String photoURL = user.getPhotoURL();
                    if(photoURL.equals("")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                profilePictureIV.setImageResource(R.drawable.prototype);
                            }
                        });
                    }
                    else {
                        mImagesStorage.getReference().child(photoURL).getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Glide.with(ProfileActivity.this)
                                                .load(uri)
                                                .asBitmap()
                                                .centerCrop()
                                                .into(new BitmapImageViewTarget(profilePictureIV) {
                                                    @Override
                                                    protected void setResource(Bitmap resource) {
                                                        RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(getApplicationContext().getResources(), resource);
                                                        circularBitmapDrawable.setCircular(true);
                                                        profilePictureIV.setImageDrawable(circularBitmapDrawable);
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Loading of image failed after upload", e.fillInStackTrace());
                                        Toast.makeText(ProfileActivity.this, "Could not load profile picture. Please try again.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Profile data could not be retrieved. Please log in and try again", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onStart:onCancelled()", databaseError.toException());
                Toast.makeText(ProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
            }
        };
//
        mUsersReference.addListenerForSingleValueEvent(profileListener);
//
//        // Keep copy of post listener so we can remove it when app stops
        mProfileListener = profileListener;
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Remove profile value event listener
        if(mProfileListener != null)
            mUsersReference.removeEventListener(mProfileListener);
    }

    private void selectImage() {
        final CharSequence[] dialogItems = {"Take Photo", "Choose from Library", "Remove profile picture", "Cancel"};
        new AlertDialog.Builder(this)
                .setTitle("Edit Profile Picture!")
                .setItems(dialogItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int item) {
                        if(dialogItems[item].equals("Take Photo")) {
                            boolean cameraResult = MarshmellowPermissions.checkCameraPermission(ProfileActivity.this);
                            userChosenTask = "Take Photo";
                            if(cameraResult)
                                cameraIntent();
                        }
                        else if(dialogItems[item].equals("Choose from Library")) {
                            boolean storageResult = MarshmellowPermissions.checkExternalStoragePermission(ProfileActivity.this);
                            userChosenTask = "Choose from Library";
                            if(storageResult)
                                galleryIntent();
                        }
                        else if(dialogItems[item].equals("Remove profile picture")) {
                            userChosenTask = "Remove profile picture";
                            removeProfilePicture();
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
            Toast.makeText(ProfileActivity.this, "Camera feature was not found. This feature cannot be used", Toast.LENGTH_SHORT).show();
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

    private void removeProfilePicture() {
        StorageReference profileReference = mImagesStorage.getReference().child("images/profiles/" + userId);

        Log.e(TAG, String.valueOf(profileReference));
        profileReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                profilePictureIV.setImageResource(R.drawable.prototype);
                            }
                        });
                        mUsersReference.child("photoURL").setValue("");
                        Toast.makeText(ProfileActivity.this, "Profile picture has been removed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "removeProfilePicture: addOnFailureListener()", e.fillInStackTrace());
                        Toast.makeText(ProfileActivity.this, "Profile picture could not be removed. Please try again", Toast.LENGTH_SHORT).show();
                    }
                });
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
            byte[] byteImage = boas.toByteArray();

            uploadImageToFirebase(byteImage);
        }
        else {
            Log.e(TAG, "onCaptureImageResult() - thumbnail is null. Reason unknown. Image will have to be captured again");
            Toast.makeText(ProfileActivity.this, "Error while generating image. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSelectFromGalleryResult(@NonNull Intent data) {
        Bitmap profileImage = null;
        try {
            profileImage = MediaStore.Images.Media.getBitmap(ProfileActivity.this.getContentResolver(), data.getData());
        } catch(IOException ie) {
            Log.e(TAG, "onSelectFromGalleryResult:IOException occurred. Requested image could not be fetched.", ie.fillInStackTrace());
            Toast.makeText(ProfileActivity.this, "Requested image could not be fetched. Please try again", Toast.LENGTH_SHORT).show();
        }

        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        if (profileImage != null) {
            profileImage.compress(Bitmap.CompressFormat.PNG, 100, boas);
            byte[] byteImage = boas.toByteArray();
            uploadImageToFirebase(byteImage);
        }
        else {
            Log.e(TAG, "onSelectFromGalleryResult - profileImage is null. Reason unknown. Image will have to be fetched again");
            Toast.makeText(ProfileActivity.this, "An error occurred. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase(byte[] profileImage) {
        showProgressBar();
        final String photoURL = "images/profiles/" + userId;

        // Update photoURL in Realtime Database in Firebase
        mUsersReference.child("photoURL").setValue(photoURL);

        // Create reference to store image in Firebase Storage and upload image
        final StorageReference profilesReference = mImagesStorage.getReference().child(photoURL);
        UploadTask uploadTask = profilesReference.putBytes(profileImage);
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = Math.round((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                Toast.makeText(ProfileActivity.this, "Profile picture is being updated: " + String.format(Locale.getDefault(), "%.0f", progress) + "% uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                hideProgressBar();
                Toast.makeText(ProfileActivity.this, "Upload has been paused", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                hideProgressBar();
                Log.e(TAG, "Image upload failed", e.fillInStackTrace());
                Toast.makeText(ProfileActivity.this, "Profile picture could not be updated. Please try again", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                hideProgressBar();
                mImagesStorage.getReference().child(photoURL).getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(ProfileActivity.this)
                                        .load(uri)
                                        .asBitmap()
                                        .centerCrop()
                                        .into(new BitmapImageViewTarget(profilePictureIV) {
                                            @Override
                                            protected void setResource(Bitmap resource) {
                                                RoundedBitmapDrawable circularBitmapDrawable = RoundedBitmapDrawableFactory.create(getApplicationContext().getResources(), resource);
                                                circularBitmapDrawable.setCircular(true);
                                                profilePictureIV.setImageDrawable(circularBitmapDrawable);
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Loading of image failed after upload", e.fillInStackTrace());
                            }
                        });
                Toast.makeText(ProfileActivity.this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
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
}
