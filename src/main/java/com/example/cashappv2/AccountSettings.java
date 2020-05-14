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
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;
import static com.google.firebase.auth.FirebaseAuth.getInstance;

public class AccountSettings extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = getInstance().getCurrentUser();
    DocumentSnapshot currentUserDetails;
    PrivateKey privateKey;
    String from;
    Double amount;
    List<DocumentSnapshot> pendingPayemtDocs;
    List<DocumentSnapshot> usersDocs;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    currentUserDetails = task.getResult();
                }
                try {
                    //get the current users private key
                    String textPrivateKey = currentUserDetails.get("Private Key").toString();
                    Base64.Decoder decoder = Base64.getDecoder();
                    byte[] bytePrivateKey = decoder.decode(textPrivateKey);
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytePrivateKey);
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    privateKey = keyFactory.generatePrivate(spec);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });

        final Button manageAccount = findViewById(R.id.manageAccountbtn);
        Button logout = findViewById(R.id.logoutbtn);
        Button deleteAccount = findViewById(R.id.deleteAccountbtn);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.selectTab(tabLayout.getTabAt(2));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        startActivity(new Intent(AccountSettings.this, Home.class));
                        finish();
                        break;
                    case 1:
                        startActivity(new Intent(AccountSettings.this, AcceptPayment.class));
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

        manageAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AccountSettings.this, AccountDetails.class));
            }
        });

        deleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get all payments
                db.collection("Pending Payments").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            pendingPayemtDocs = task.getResult().getDocuments();
                            //gets all payments to current user
                            db.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if(task.isSuccessful()){
                                        for (final DocumentSnapshot pendingPaymentDoc : pendingPayemtDocs) {
                                            String payee = pendingPaymentDoc.get("To").toString();
                                            if (currentUserDetails.get("Username").toString().equals(payee)) {
                                                amount = Double.valueOf(SecurityHelper.decrypt(pendingPaymentDoc.get("Amount").toString().getBytes(), privateKey));
                                                from = pendingPaymentDoc.get("From").toString();
                                                usersDocs = task.getResult().getDocuments();
                                                for (DocumentSnapshot userDoc : usersDocs) {
                                                    if (userDoc.get("Username").toString().equals(from)) {
                                                        userDoc.getReference().update("Accessible Funds", FieldValue.increment(amount)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Log.d("Update Success", "Balance Updated");
                                                                } else {
                                                                    Log.d("Update Failed", "Error");
                                                                }
                                                            }
                                                        });
                                                    }
                                                }

                                                pendingPaymentDoc.getReference().delete().addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(AccountSettings.this, "Error",LENGTH_SHORT).show();
                                                    }
                                                });

                                            }


                                        }



                                        db.document("Users/"+currentUser.getEmail()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){

                                                    Toast.makeText(AccountSettings.this, "User Data Deleted.", LENGTH_SHORT).show();


                                                    currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()){
                                                                startActivity(new Intent(AccountSettings.this, Login.class));
                                                                Toast.makeText(AccountSettings.this, "Account Deleted.", LENGTH_SHORT).show();
                                                                finish();
                                                            }else{
                                                                Toast.makeText(AccountSettings.this, "Failed to Delete Account.", LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                                }else{
                                                    Toast.makeText(AccountSettings.this, "Failed to Delete Account.", LENGTH_SHORT).show();
                                                }
                                            }
                                        });


                                    }else{
                                        Toast.makeText(AccountSettings.this, "Failed to Delete Account1.", LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }else{
                            Toast.makeText(AccountSettings.this, "Failed to Delete Account2.", LENGTH_SHORT).show();
                        }

                    }
                });

            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInstance().signOut();
                Toast.makeText(AccountSettings.this, "Signed Out", LENGTH_SHORT).show();
                startActivity(new Intent(AccountSettings.this, Login.class));
                finish();
            }
        });
    }
}
