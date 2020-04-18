package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.Button;
import android.widget.HorizontalScrollView;
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

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

public class AcceptPayment extends AppCompatActivity {
    String encryptedAmount;
    String encryptedCardNumber;
    String encryptedCvc;
    String fromStr;
    String amountStr;
    String cardNumber;
    String cvc;
    PrivateKey privateKey;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private DocumentSnapshot currentUserDetails;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_payment);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.selectTab(tabLayout.getTabAt(1));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        startActivity(new Intent(AcceptPayment.this, Home.class));
                        finish();
                        break;

                    case 2:
                        startActivity(new Intent(AcceptPayment.this, AccountSettings.class));
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

        //get current users details
        db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
            if(task.isSuccessful()){
                try{
                    currentUserDetails = task.getResult();
                    DocumentSnapshot doc = task.getResult();
                    String accountHoldersNamestr = "Account Holders Name - " + doc.getString("Account Holders Name");
                    String accountNumberstr = "Account Number - " + doc.getString("Account Number");
                    String sortCodestr = "Sort Code - " + doc.getString("Sort Code");
                    accountHoldersName.setText(accountHoldersNamestr);
                    accountNumber.setText(accountNumberstr);
                    sortCode.setText(sortCodestr);

                    //gets all payments
                    db.collection("Pending Payments").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()) {
                                List<DocumentSnapshot> docs = task.getResult().getDocuments();
                                LinearLayout linearLayout = findViewById(R.id.pendingPayments);
                                View child;
                                int i = 0;
                                //gets all payments to current user
                                for (final DocumentSnapshot doc : docs) {
                                    String payee = doc.get("To").toString();
                                    if (currentUserDetails.get("Username").toString().equals(payee)) {
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
                                        //get and decrypt the payment details
                                        fromStr = doc.get("From").toString();
                                        encryptedAmount = doc.get("Amount").toString();
                                        encryptedCardNumber = doc.get("Card Number").toString();
                                        encryptedCvc = doc.get("Cvc").toString();
                                        amountStr = PaymentHelper.decrypt(encryptedAmount.getBytes(),privateKey);
                                        cardNumber = PaymentHelper.decrypt(encryptedCardNumber.getBytes(),privateKey);
                                        cvc = PaymentHelper.decrypt(encryptedCvc.getBytes(),privateKey);
                                        Log.d("success", "payment found");

                                        //create visual representation
                                        child = linearLayout.inflate(getApplicationContext(),R.layout.pending_payment,null);
                                        child.setId(10*i+6);
                                        pendingPayments.addView(child);
                                        TextView from = findViewById(R.id.fromlbl);
                                        from.setId(10*i+1);
                                        TextView amount = findViewById(R.id.amountlbl);
                                        amount.setId(10*i+2);
                                        TextView paymentId = findViewById(R.id.paymentId);
                                        paymentId.setId(10*i+3);
                                        from.setText("From: "+ fromStr);
                                        amount.setText("Amount: " + amountStr);
                                        paymentId.setText("Payment Id: "+ doc.getId());
                                        Button submit = findViewById(R.id.confirmbtn);
                                        submit.setId(10*i+4);

                                        //accept payment
                                        submit.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Toast.makeText(AcceptPayment.this, "Payment Accepted", Toast.LENGTH_SHORT).show();
                                                HorizontalScrollView currentLayout = findViewById((v.getId())+2);
                                                final TextView paymentId = findViewById((v.getId())-1);
                                                final TextView amountToPay = findViewById((v.getId())-2);
                                                Log.d("Payment ID", paymentId.getText().toString());

                                                //find payment to accept
                                                db.document("Pending Payments/"+paymentId.getText().toString().substring(12)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @RequiresApi(api = Build.VERSION_CODES.O)
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if(task.isSuccessful()){
                                                            DocumentSnapshot doc = task.getResult();
                                                            Log.d("check if exists",String.valueOf(doc.exists()));
                                                            //get payment details
                                                            final String digitalSignature = doc.getString("Signature");
                                                            final String from = doc.getString("From");
                                                            final Double amountDbl = Double.valueOf(amountToPay.getText().toString().substring(8));
                                                            final String textPublicKey = doc.getString("Public Key");
                                                            try {
                                                                //verify digital signature
                                                                Signature signature = Signature.getInstance("SHA256withRSA");
                                                                Base64.Decoder decoder = Base64.getDecoder();
                                                                byte[] bytePublicKey = decoder.decode(textPublicKey);
                                                                X509EncodedKeySpec spec = new X509EncodedKeySpec(bytePublicKey);
                                                                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                                                PublicKey publicKey = keyFactory.generatePublic(spec);
                                                                signature.initVerify(publicKey);
                                                                byte[] byteDigitalSignature = decoder.decode(digitalSignature);
                                                                signature.update(from.getBytes());
                                                                System.out.println("Signature " + (signature.verify(byteDigitalSignature) ? "OK" : "Not OK"));
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }

                                                            //update current users money
                                                            db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                    if (task.isSuccessful()) {
                                                                        DocumentSnapshot doc = task.getResult();
                                                                        doc.getReference().update("Accessible Funds", doc.getDouble("Accessible Funds") + amountDbl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if (task.isSuccessful()) {
                                                                                    Log.d("Update Success", "Balance Updated");
                                                                                } else {
                                                                                    Log.d("Update Failed", "Error");
                                                                                }
                                                                            }
                                                                        });
                                                                        //remove payment record
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
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                                //remove view from interface
                                                LinearLayout payments = findViewById(R.id.pendingPayments);
                                                payments.removeView(currentLayout);
                                            }
                                        });

                                        Button reject = findViewById(R.id.rejectbtn);
                                        reject.setId(10*i+5);
                                        reject.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                //reject payment
                                                Toast.makeText(AcceptPayment.this, "Payment Rejected", Toast.LENGTH_SHORT).show();
                                                HorizontalScrollView currentLayout = findViewById((v.getId())+1);
                                                TextView paymentId = findViewById((v.getId())-2);
                                                final TextView amountToPay = findViewById((v.getId())-3);

                                                //find payment to reject
                                                db.document("Pending Payments/"+paymentId.getText().toString().substring(12)).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if(task.isSuccessful()){
                                                            db.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                    if(task.isSuccessful()){
                                                                        //find user who sent the payment
                                                                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                                                                        for(DocumentSnapshot doc : docs){
                                                                            //refund payment amount
                                                                            final Double amountDbl = Double.valueOf(amountToPay.getText().toString().substring(8));
                                                                            if(doc.get("Username").toString().equals(fromStr)){
                                                                                doc.getReference().update("Accessible Funds", doc.getDouble("Accessible Funds")+amountDbl).addOnCompleteListener(new OnCompleteListener<Void>() {
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
                                                                    }
                                                                }
                                                            });
                                                            //remove payment record
                                                            task.getResult().getReference().delete().addOnSuccessListener(new OnSuccessListener<Void>() {
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
                                                    }
                                                });
                                                //remove view from interface
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
                    Log.d("Check if running", "running");
                }catch(Exception e){
                    Log.d("Check if running", "Failed1");
                }
            }
            }
        });
    }
}
