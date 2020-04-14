package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.stripe.android.ApiResultCallback;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardInputWidget;
import com.example.cashappv2.EncryptDecrypt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class MakePayment extends AppCompatActivity {
    private DocumentSnapshot currentUserDetails;
    private double accessibleFunds;
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_make_payment);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final CardInputWidget cardInputWidget = findViewById(R.id.cardInputWidget);
        final TextView amount = findViewById(R.id.paymentAmountlbl);
        final TextView payee = findViewById(R.id.payeeUsernamelbl);
        final EditText inputPayee = findViewById(R.id.payeeUsernametxt);
        final Button confirm = findViewById(R.id.confirmbtn);
        final EditText inputAmount = findViewById(R.id.paymentAmounttxt);
        final TextView balance = findViewById(R.id.currentBalancelbl);
        db.document("Users/" + currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    currentUserDetails = task.getResult();
                    accessibleFunds = currentUserDetails.getDouble("Accessible Funds");
                    balance.setText(String.valueOf(accessibleFunds));
                }
            }
        });
        amount.setText("");
        payee.setText("");

        inputPayee.addTextChangedListener(new TextWatcher() {
              @Override
              public void beforeTextChanged(CharSequence s, int start, int count, int after) {

              }

              @Override
              public void onTextChanged(CharSequence s, int start, int before, int count) {

              }

              @Override
              public void afterTextChanged(Editable s) {
                    payee.setText(inputPayee.getText().toString());
              }
          });

        inputAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                amount.setText(inputAmount.getText().toString());
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Card card = cardInputWidget.getCard();
                db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            try {
                                RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(new BigDecimal(task.getResult().get("Public Key Mod").toString()).toBigIntegerExact(), new BigDecimal(task.getResult().get("Public Key Exp").toString()).toBigIntegerExact());
                                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                publicKey = keyFactory.generatePublic(rsaPublicKeySpec);
                                System.out.println(EncryptDecrypt.encrypt(currentUserDetails.get("Username").toString(),publicKey));
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                });

                String encryptedFrom;
                String encryptedTo;
                String encryptedAmount;
                String encryptedCardNumber;
                String encryptedCvc;

                Map<String, Object> payment = new HashMap<>();
                payment.put("From", currentUserDetails.get("Username").toString());
                payment.put("To", payee.getText().toString());
                payment.put("Amount", inputAmount.getText().toString());
                payment.put("Card Number", card.getNumber());
                payment.put("Cvc", card.getCvc());

                if (Double.valueOf(inputAmount.getText().toString()) <= accessibleFunds) {
                    Log.d("Input", inputAmount.getText().toString());
                    Log.d("Accessible Funds", String.valueOf(accessibleFunds));
                    db.collection("Pending Payments").add(payment).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Toast.makeText(MakePayment.this, "Success", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MakePayment.this, Home.class));
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MakePayment.this, "Payment Failed", Toast.LENGTH_SHORT).show();
                            Log.w("Failed", "Error adding document", e);
                        }
                    });
                }else{
                    Log.d("Payment Failed", "Insufficient Funds");
                    Toast.makeText(MakePayment.this, "Payment Failed, Insufficient Funds",Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}
