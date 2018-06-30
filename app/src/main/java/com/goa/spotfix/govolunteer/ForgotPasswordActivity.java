package com.goa.spotfix.govolunteer;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;


public class ForgotPasswordActivity extends AppCompatActivity {

    private String email;

    private EditText emailET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailET = findViewById(R.id.email);
        Button resetBtn = findViewById(R.id.reset_password);

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validate()) {
                    submitResetRequest();
                }
            }
        });
    }

    private boolean validate() {
        boolean valid = true;

        email = emailET.getText().toString().trim();

        if(email.isEmpty()) {
            emailET.setError("Email is required");
            emailET.requestFocus();
            valid = false;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailET.setError("Please enter a valid email");
            emailET.requestFocus();
            valid = false;
        }
        return valid;
    }

    private void submitResetRequest() {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Email has been sent successfully!", Toast.LENGTH_SHORT).show();
                            emailET.setText("");
                        }
                        else {
                            Toast.makeText(getApplicationContext(), "Could not send reset password request. Please check your internet connection and try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
