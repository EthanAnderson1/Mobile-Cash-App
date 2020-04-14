package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.QuerySnapshot;
import com.google.firestore.v1.Document;


import java.util.List;

public class AcceptPayment extends AppCompatActivity {

    private DocumentSnapshot currentUserDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_payment);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.selectTab(tabLayout.getTabAt(1));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        startActivity(new Intent(AcceptPayment.this, Home.class));
                        break;
                    case 2:
                        startActivity(new Intent(AcceptPayment.this, Logs.class));
                        break;
                    case 3:
                        startActivity(new Intent(AcceptPayment.this, AccountSettings.class));
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

        Button changeBankDetails = findViewById(R.id.changeBankDetails);
        changeBankDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AcceptPayment.this, AccountDetails.class));
            }
        });

        final TextView accountHoldersName = findViewById(R.id.accountHoldersNamelbl);
        final TextView accountNumber = findViewById(R.id.accountNumberlbl);
        final TextView sortCode = findViewById(R.id.sortCodelbl);
        final LinearLayout  pendingPayments = findViewById(R.id.pendingPayments);

        db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    try{
                        DocumentSnapshot doc = task.getResult();
                        String accountHoldersNamestr = "Account Holders Name - " + doc.getString("Account Holders Name");
                        String accountNumberstr = "Account Number - " + doc.getString("Account Number");
                        String sortCodestr = "Sort Code - " + doc.getString("Sort Code");
                        accountHoldersName.setText(accountHoldersNamestr);
                        accountNumber.setText(accountNumberstr);
                        sortCode.setText(sortCodestr);
                        Log.d("Check if running", "running");
                    }catch(Exception e){
                        Log.d("Check if running", "Failed1");
                    }
                }
            }
        });

        db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                   currentUserDetails = task.getResult();
                }
            }
        });

        db.collection("Pending Payments").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    List<DocumentSnapshot> docs = task.getResult().getDocuments();
                    LinearLayout linearLayout = findViewById(R.id.pendingPayments);
                    LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
                    View child;
                    int i = 0;
                    for (final DocumentSnapshot doc : docs) {
                        if (currentUserDetails.getString("Username").equals(doc.getString("To"))) {
                            Log.d("success", "payment found");
                            child = linearLayout.inflate(getApplicationContext(),R.layout.pending_payment,null);
                            child.setId(10*i+6);
                            pendingPayments.addView(child);
                            TextView from = findViewById(R.id.fromlbl);
                            from.setId(10*i+1);
                            TextView amount = findViewById(R.id.amountlbl);
                            amount.setId(10*i+2);
                            TextView paymentId = findViewById(R.id.paymentId);
                            paymentId.setId(10*i+3);
                            from.setText("From: " + doc.getString("From"));
                            amount.setText("Amount: " + doc.getString("Amount"));
                            paymentId.setText(doc.getId());
                            Button submit = findViewById(R.id.confirmbtn);
                            submit.setId(10*i+4);
                            submit.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(AcceptPayment.this, "Payment Accepted", Toast.LENGTH_SHORT).show();
                                    LinearLayout currentLayout = findViewById((v.getId())+2);
                                    final TextView paymentId = findViewById((v.getId())-1);
                                    Log.d("Payment ID", paymentId.getText().toString());
                                    db.collection("Pending Payments").document(paymentId.getText().toString()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if(task.isSuccessful()){
                                                DocumentSnapshot doc = task.getResult();
                                                Log.d("check if exists",String.valueOf(doc.exists()));
                                                final String to = doc.getString("To");
                                                final String from = doc.getString("From");
                                                final Double amount = Double.valueOf(doc.get("Amount").toString());
                                                Log.d("Check if working",doc.getString("To"));
                                                Log.d("Check if working",doc.getString("From"));
                                                Log.d("Check if working",Double.valueOf(doc.get("Amount").toString()).toString());
                                                db.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                        if(task.isSuccessful()){
                                                            List<DocumentSnapshot> docs = task.getResult().getDocuments();
                                                            Log.d("Check if working","1");
                                                            for(DocumentSnapshot doc : docs){
                                                                Log.d("Check if working","2");
                                                                if(doc.get("Username").toString().equals(from)){
                                                                    Log.d("Check if working","3");
                                                                    doc.getReference().update("Accessible Funds", doc.getDouble("Accessible Funds")-amount).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                Log.d("Update Success", "Balance Updated");
                                                                            }else{
                                                                                Log.d("Update Failed", "Error");
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                                if(doc.getString("Username").equals(to)){
                                                                    doc.getReference().update("Accessible Funds", doc.getDouble("Accessible Funds")+amount).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                Log.d("Update Success", "Balance Updated");
                                                                            }else{
                                                                                Log.d("Update Failed", "Error");
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        }
                                                        db.collection("Pending Payments").document(paymentId.getText().toString()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.d("Delete Doc","Success");
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.d("Delete Doc","Fail");
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        }
                                    });

                                    LinearLayout payments = findViewById(R.id.pendingPayments);
                                    payments.removeView(currentLayout);
                                }
                            });

                            Button reject = findViewById(R.id.rejectbtn);
                            reject.setId(10*i+5);
                            reject.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(AcceptPayment.this, "Payment Rejected", Toast.LENGTH_SHORT).show();
                                    LinearLayout currentLayout = findViewById((v.getId())+1);
                                    TextView paymentId = findViewById((v.getId())-2);
                                    db.collection("Pending Payments").document(paymentId.getText().toString().substring(12)).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d("Delete Doc","Success");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.d("Delete Doc","Fail");
                                        }
                                    });
                                    LinearLayout payments = findViewById(R.id.pendingPayments);
                                    payments.removeView(currentLayout);
                                }
                            });
                            i++;
                        }else{
                            Log.d("Failed", "payment not found");
                        }
                    }
                }else{
                    Log.d("Failed", "payment not found");
                }
            }
        });
    }
}
