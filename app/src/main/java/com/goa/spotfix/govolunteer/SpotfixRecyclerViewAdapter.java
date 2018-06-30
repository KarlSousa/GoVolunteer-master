package com.goa.spotfix.govolunteer;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SpotfixRecyclerViewAdapter extends RecyclerView.Adapter {

    private List<Spotfix> cardList;
    private HashMap<String, String> joinedIds;

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {

        private String userId;

        private View view;
        private TextView spotfixLocationTV, noOfPeopleTV, dateTV, timeTV, noOfPeopleJoinedTV, spotfixId;
        private Button joinButton;
        private ImageView spotfixIV;

        private Spotfix spotfixForCardView;
        private FirebaseStorage mImagesStorage;
        private DatabaseReference mSpotfixReference, mUsersReference;

        RecyclerViewHolder(View view) {
            super(view);
            this.view = view;

            // Retrieve user id to fetch profile from Firebase
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            // Retrieve user id
            userId = currentUser.getUid();

            // Initialize Firebase database and storage
            mUsersReference = FirebaseDatabase.getInstance().getReference().child("Users").child(userId);

            CardView spotfixCardView = view.findViewById(R.id.spotfixCV);
            spotfixCardView.setCardElevation(20);
            spotfixCardView.setRadius(15);

            // Initialize other UI elements inside cardview
            spotfixIV = view.findViewById(R.id.imageView);
            spotfixLocationTV = view.findViewById(R.id.placeNameTV);
            noOfPeopleTV = view.findViewById(R.id.noOfReqPeopleTV);
            noOfPeopleJoinedTV = view.findViewById(R.id.noOfPeopleJoined);
            spotfixId = view.findViewById(R.id.spotfixId);
            joinButton = view.findViewById(R.id.joinBtn);
            dateTV = view.findViewById(R.id.dateTV);
            timeTV = view.findViewById(R.id.timeTV);

            joinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    int reqPeople = Integer.parseInt(noOfPeopleTV.getText().toString().trim());
                    final int noOfPeopleJoined = Integer.parseInt(noOfPeopleJoinedTV.getText().toString().trim());
                    final String id = spotfixId.getText().toString();
                    if(userId.equals(spotfixForCardView.getUploaderId())) {
                        joinButton.setEnabled(false);
                        joinButton.setBackgroundColor(Color.LTGRAY);
                        Toast.makeText(view.getContext(), "The uploader cannot join his/her own spotfix", Toast.LENGTH_SHORT).show();
                    }
                    else if(reqPeople == noOfPeopleJoined) {
                        joinButton.setEnabled(false);
                        joinButton.setBackgroundColor(Color.LTGRAY);
                        Toast.makeText(view.getContext(), "Sorry, enough people have already joined this spotfix!", Toast.LENGTH_SHORT).show();
                    }
                    else if(spotfixForCardView.getJoinedIds().containsKey(userId)) {
                        joinButton.setEnabled(false);
                        joinButton.setBackgroundColor(Color.LTGRAY);
                        Toast.makeText(view.getContext(), "You have already joined this spotfix", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        ValueEventListener profileListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                User user = dataSnapshot.getValue(User.class);
                                if(user != null) {
                                    joinButton.setEnabled(false);
                                    joinButton.setBackgroundColor(Color.LTGRAY);
                                    int newNoOfPeopleJoined = noOfPeopleJoined + 1;
                                    Date date = Calendar.getInstance().getTime();
                                    String strDate = new SimpleDateFormat("dd-mm-yyyy hh:mm:ss", Locale.ENGLISH).format(date);
                                    joinedIds.put(userId, strDate);
                                    mSpotfixReference = FirebaseDatabase.getInstance().getReference().child("Spotfixes").child(id);
                                    mSpotfixReference.child("noOfPeopleJoined").setValue(newNoOfPeopleJoined);
                                    mSpotfixReference.child("joinedIds").setValue(joinedIds);
                                    mUsersReference.child("spotfixes_attended").setValue(user.getSpotfixes_attented()+1);
                                    Toast.makeText(view.getContext(), "Well done! You have been added to the spotfix!", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    Log.e("RecyclerViewAdapter", "User object is null. Dont know why.");
                                    Toast.makeText(view.getContext(), "Invalid request. Please log in and try again", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(view.getContext(), "Could not connect to server. Please try again", Toast.LENGTH_SHORT).show();
                                Log.e("RecyclerViewAdapter", "onCancelled. Kuch tho jhol hai", databaseError.toException());
                            }
                        };
                        mUsersReference.addListenerForSingleValueEvent(profileListener);
                        mUsersReference.removeEventListener(profileListener);
                    }
                }
            });

            spotfixCardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Open new activity
                    Intent intent = new Intent(view.getContext(), SpotfixCardActivity.class);
                    intent.putExtra("spotfix", spotfixForCardView);
                    view.getContext().startActivity(intent);
                }
            });
        }

        void bindData(final Spotfix spotfix) {
            spotfixForCardView = spotfix;
            mImagesStorage = FirebaseStorage.getInstance();
            Glide.with(view.getContext()).load(R.drawable.prototype).into(spotfixIV);
            mImagesStorage.getReference().child(spotfix.getPhotoURL()).getDownloadUrl()
                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Glide.with(view.getContext()).load(uri).asBitmap().into(spotfixIV);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(view.getContext(), "Could not load image of spotfix", Toast.LENGTH_SHORT).show();
                        }
                    });
            spotfixLocationTV.setText(spotfix.getPlaceName());
            noOfPeopleTV.setText(String.format(Locale.getDefault(), "%d", spotfix.getPeopleRequired()));
            noOfPeopleJoinedTV.setText(String.format(Locale.getDefault(), "%d", spotfix.getNoOfPeopleJoined()));
            spotfixId.setText(spotfix.getSpotfixId());
            dateTV.setText(spotfix.getDate());
            timeTV.setText(spotfix.getTime());
            joinedIds = spotfix.getJoinedIds();
        }
    }

    SpotfixRecyclerViewAdapter(List<Spotfix> cardList) {
        this.cardList = cardList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_spotfix, parent, false);
        return new SpotfixRecyclerViewAdapter.RecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((RecyclerViewHolder) holder).bindData(cardList.get(position));

    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }
}
