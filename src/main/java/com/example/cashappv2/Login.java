package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;

public class Login extends AppCompatActivity {
    FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mFirebaseUser = mAuth.getCurrentUser();
        if (mFirebaseUser != null) {
            Toast.makeText(Login.this, "You are logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Login.this, Home.class));
        } else {
            Toast.makeText(Login.this, "Please Login", Toast.LENGTH_SHORT).show();
        }

        Button login = findViewById(R.id.loginbtn);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.selectTab(tabs.getTabAt(0));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 1:
                        startActivity(new Intent(Login.this, SignUp.class));
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText email = findViewById(R.id.emailtxt);
                EditText password = findViewById(R.id.passwordtxt);
                if (email.getText().toString().isEmpty()) {
                    email.setError("Please enter an email address");
                    email.requestFocus();
                } else if (password.getText().toString().isEmpty()) {
                    password.setError("Please enter a password");
                    password.requestFocus();
                } else if (email.getText().toString().isEmpty() && password.getText().toString().isEmpty()) {
                    Toast.makeText(Login.this, "Fields are empty", Toast.LENGTH_SHORT).show();
                } else if (!(email.getText().toString().isEmpty() && password.getText().toString().isEmpty())) {
                    mAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("successful login","successful login");
                                Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Login.this, Home.class));
                            } else {
                                Log.d("Login Failed", "Login Failed");
                                Toast.makeText(Login.this, "Login Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}