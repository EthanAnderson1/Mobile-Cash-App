package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


import java.security.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.Random;

import static android.widget.Toast.LENGTH_SHORT;

@RequiresApi(api = Build.VERSION_CODES.O)
public class SignUp extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    PublicKey publicKey;
    String textPublicKey;
    String textPrivateKey;
    Base64.Encoder encoder = Base64.getEncoder();
    List<String> takenUsernames = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mAuth = FirebaseAuth.getInstance();

        Button signUp = findViewById(R.id.signUpbtn);
        takenUsernames.add("a");
        db.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
           @Override
           public void onComplete(@NonNull Task<QuerySnapshot> task) {
               List<DocumentSnapshot> docs = task.getResult().getDocuments();
               for(DocumentSnapshot doc:docs){
                   takenUsernames.add(doc.get("Username").toString());
               }
           }
        });

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.selectTab(tabs.getTabAt(1));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        startActivity(new Intent(SignUp.this, Login.class));
                        finish();
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

                } else if(takenUsernames.contains(username.getText().toString())){
                    username.setError("Username is already taken");
                    email.requestFocus();
                } else {
                    mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("Made User", "createUserWithEmail:success");
                                Toast.makeText(getApplicationContext(), "Account Created", LENGTH_SHORT).show();

                                try{
                                    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                                    keyPairGenerator.initialize(1024);
                                    KeyPair keyPair = keyPairGenerator.generateKeyPair();
                                    publicKey = keyPair.getPublic();
                                    Key privateKey = keyPair.getPrivate();
                                    Base64.Encoder encoder = Base64.getEncoder();
                                    textPublicKey =encoder.encodeToString(publicKey.getEncoded());
                                    textPrivateKey = encoder.encodeToString(privateKey.getEncoded());

                                  /* FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(fileName+".key", Context.MODE_PRIVATE);
                                   fileOutputStream.write(privateKey.getEncoded());
                                   System.out.println("Private key format: " + privateKey.getFormat());
                                   fileOutputStream = getApplicationContext().openFileOutput(fileName+".pub", Context.MODE_PRIVATE);
                                   fileOutputStream.write(publicKey.getEncoded());
                                   System.out.println("Public key format: " + publicKey.getFormat());
*/
                                }catch(Exception e){
                                    e.printStackTrace();
                                    //System.out.println("Failed Creating Keys");
                                }


                                Map<String, Object> user = new HashMap<>();
                                user.put("First Name", encoder.encodeToString(SecurityHelper.encrypt(firstName.getText().toString(), publicKey)));
                                user.put("Last Name", encoder.encodeToString(SecurityHelper.encrypt(lastName.getText().toString(), publicKey)));
                                user.put("Username", username.getText().toString());
                                user.put("Email", encoder.encodeToString(SecurityHelper.encrypt(email.getText().toString(), publicKey)));
                                user.put("Account Holders Name", encoder.encodeToString(SecurityHelper.encrypt(cardHoldersName.getText().toString(), publicKey)));
                                user.put("Account Number", encoder.encodeToString(SecurityHelper.encrypt(accountNumber.getText().toString(), publicKey)));
                                user.put("Sort Code", encoder.encodeToString(SecurityHelper.encrypt(sortCode.getText().toString(), publicKey)));
                                user.put("Accessible Funds",(0));
                                user.put("Public Key", textPublicKey);
                                user.put("Private Key", textPrivateKey);

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
