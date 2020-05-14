package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static android.widget.Toast.LENGTH_SHORT;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AccountDetails extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    DocumentSnapshot currentUserDetails;
    PublicKey publicKey;
    Base64.Decoder decoder = Base64.getDecoder();
    Base64.Encoder encoder = Base64.getEncoder();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);

        TextView title = findViewById(R.id.addBankAccountlbl);
        title.setText("Update Bank Details");
        title = findViewById(R.id.inputDetailslbl);
        title.setText("Update Details");
        LinearLayout email = findViewById(R.id.emailll);
        email.setVisibility(View.GONE);
        LinearLayout username = findViewById(R.id.usernamell);
        username.setVisibility(View.GONE);

        final EditText firstName = findViewById(R.id.firstNametxt);
        final EditText lastName = findViewById(R.id.surnametxt);
        final EditText cardHoldersName = findViewById(R.id.cardHoldersNametxt);
        final EditText accountNumber = findViewById(R.id.accountNumbertxt);
        final EditText sortCode = findViewById(R.id.sortCodetxt);
        final EditText Cpassword = findViewById(R.id.confirmPasswordtxt);
        final EditText password = findViewById(R.id.passwordtxt);

        db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    currentUserDetails = task.getResult();
                    try {
                        //get the current users private key
                        String textPublicKey = currentUserDetails.get("Public Key").toString();
                        byte[] bytePublicKey = decoder.decode(textPublicKey);
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytePublicKey);
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        publicKey = keyFactory.generatePublic(spec);

                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });

        Button update = findViewById(R.id.update);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!firstName.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("First Name", encoder.encodeToString(SecurityHelper.encrypt(firstName.getText().toString(),publicKey)));
                    Log.d("Success", "Details Updated");
                    Toast.makeText(getApplicationContext(), "Details Updated", LENGTH_SHORT).show();
                    finish();
                }
                if(!lastName.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Last Name", encoder.encodeToString(SecurityHelper.encrypt(lastName.getText().toString(),publicKey)));
                    Log.d("Success", "Details Updated");
                    Toast.makeText(getApplicationContext(), "Details Updated", LENGTH_SHORT).show();
                    finish();
                }
                if(!cardHoldersName.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Account Holders Name", encoder.encodeToString(SecurityHelper.encrypt(cardHoldersName.getText().toString(),publicKey)));
                    Log.d("Success", "Details Updated");
                    Toast.makeText(getApplicationContext(), "Details Updated", LENGTH_SHORT).show();
                }
                if(!accountNumber.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Account Number", encoder.encodeToString(SecurityHelper.encrypt(accountNumber.getText().toString(),publicKey)));
                    Log.d("Success", "Details Updated");
                    Toast.makeText(getApplicationContext(), "Details Updated", LENGTH_SHORT).show();
                    finish();
                }
                if(!sortCode.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Sort Code", encoder.encodeToString(SecurityHelper.encrypt(sortCode.getText().toString(),publicKey)));
                    Log.d("Success", "Details Updated");
                    Toast.makeText(getApplicationContext(), "Details Updated", LENGTH_SHORT).show();
                    finish();
                }
                if(!password.getText().toString().isEmpty()) {
                    if (Cpassword.getText().toString().equals(password.getText().toString())) {
                        currentUser.updatePassword(password.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d("Success", "Password Updated");
                                    Log.d("Success", "Details Updated");
                                    Toast.makeText(getApplicationContext(), "Details Updated", LENGTH_SHORT).show();
                                    finish();
                                }else{
                                    Log.d("Fail", "Password Not Updated");
                                }
                            }
                        });
                    }else{
                        password.setError("Please enter a valid password");
                        password.requestFocus();
                        password.setError("Please enter a valid password");
                        Cpassword.requestFocus();
                    }
                }
            }
        });
    }
}
