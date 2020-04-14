package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.widget.Toast.LENGTH_SHORT;

public class SignUp extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    RSAPublicKeySpec rsaPublicKeySpec;
    RSAPrivateKeySpec rsaPrivateKeySpec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mAuth = FirebaseAuth.getInstance();

        Button signUp = findViewById(R.id.signUpbtn);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.selectTab(tabs.getTabAt(1));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        startActivity(new Intent(SignUp.this, Login.class));
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


        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText firstName =findViewById(R.id.firstNametxt);
                final EditText lastName = findViewById(R.id.surnametxt);
                final EditText username = findViewById(R.id.usernametxt);
                final EditText email = findViewById(R.id.emailtxt);
                final EditText cardHoldersName = findViewById(R.id.cardHoldersNametxt);
                final EditText accountNumber = findViewById(R.id.accountNumbertxt);
                final EditText sortCode = findViewById(R.id.sortCodetxt);

                EditText Cpassword = findViewById(R.id.confirmPasswordtxt);
                EditText password = findViewById(R.id.passwordtxt);
                if (email.getText().toString().isEmpty()) {
                    email.setError("Please enter a valid email address");
                    email.requestFocus();
                } else if (password.getText().toString().isEmpty()||!Cpassword.getText().toString().equals(password.getText().toString())) {
                    password.setError("Please enter a valid password");
                    password.requestFocus();
                    Cpassword.requestFocus();
                } else {
                    mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("Made User", "createUserWithEmail:success");
                                Toast.makeText(getApplicationContext(), "Account Created", LENGTH_SHORT).show();

                                try{
                                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                                    keyPairGenerator.initialize(512);
                                    KeyPair keyPair = keyPairGenerator.generateKeyPair();
                                    PublicKey publicKey = keyPair.getPublic();
                                    PrivateKey privateKey = keyPair.getPrivate();
                                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                    rsaPublicKeySpec = keyFactory.getKeySpec(publicKey,RSAPublicKeySpec.class);
                                    rsaPrivateKeySpec = keyFactory.getKeySpec(privateKey,RSAPrivateKeySpec.class);
                                }catch(Exception e){
                                    System.out.println("Failed Creating Keys");
                                }


                                Map<String, Object> user = new HashMap<>();
                                user.put("First Name", firstName.getText().toString());
                                user.put("Last Name", lastName.getText().toString());
                                user.put("Username", username.getText().toString());
                                user.put("Email", email.getText().toString());
                                user.put("Account Holders Name", cardHoldersName.getText().toString());
                                user.put("Account Number", accountNumber.getText().toString());
                                user.put("Sort Code", sortCode.getText().toString());

                                Random rand = new Random();
                                user.put("Accessible Funds",(rand.nextInt(9500)+500));

                                user.put("Public Key Mod",rsaPublicKeySpec.getModulus());
                                //user.put("Public Key Exp",rsaPublicKeySpec.getPublicExponent());
                                user.put("Private Key Mod",rsaPrivateKeySpec.getModulus());
                                //user.put("Private Key Exp",rsaPrivateKeySpec.getPrivateExponent());

                                db.document("Users/"+email.getText().toString()).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.w("Success", "User Saved to db");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("Made User Failed", "createUserWithEmail:failure");
                                        Toast.makeText(SignUp.this, "Authentication failed.", LENGTH_SHORT).show();
                                    }
                                });
                                startActivity(new Intent(SignUp.this, Login.class));
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("Made User Failed", "createUserWithEmail:failure", task.getException());
                                Toast.makeText(SignUp.this, "Authentication failed.", LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}