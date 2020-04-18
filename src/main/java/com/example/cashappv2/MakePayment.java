package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
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
import com.stripe.android.model.Card;
import com.stripe.android.view.CardInputWidget;


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MakePayment extends AppCompatActivity {
    private DocumentSnapshot currentUserDetails;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private double accessibleFunds;
    String textDigitalSignature;
    String textPublicKey;
    String encryptedAmount;
    String encryptedCardNumber;
    String encryptedCvc;
    String from;
    String to;
    String amountStr;
    String cardNumber;
    String cvc;
    Map<String, Object> payment = new HashMap<>();
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
                    balance.setText(decimalFormat.format(accessibleFunds));
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
                amount.setText(decimalFormat.format(Double.valueOf(inputAmount.getText().toString())));
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            try {
                                //create signature
                                Base64.Decoder decoder = Base64.getDecoder();
                                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                String textPrivateKey = task.getResult().get("Private Key").toString();
                                textPublicKey = task.getResult().get("Public Key").toString();
                                byte[] bytesPrivateKey = decoder.decode(textPrivateKey);
                                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytesPrivateKey);
                                PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
                                Signature signature = Signature.getInstance("SHA256withRSA");
                                signature.initSign(privateKey);
                                String data = task.getResult().get("Username").toString();
                                byte[] byteData = data.getBytes();
                                signature.update(byteData);
                                byte[] digitalSignature = signature.sign();
                                final Base64.Encoder encoder = Base64.getEncoder();
                                textDigitalSignature = encoder.encodeToString(digitalSignature);

                                from=task.getResult().get("Username").toString();
                                Card card = cardInputWidget.getCard();
                                amountStr = inputAmount.getText().toString();
                                cardNumber = card.getNumber();
                                cvc = card.getCvc();

                                db.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                                        try {
                                            //find payee
                                            Boolean userFound = false;
                                            for(final DocumentSnapshot doc : docs){
                                                if(doc.get("Username").toString().equals(payee.getText().toString())){
                                                    userFound = true;
                                                    //encrypt payment
                                                    Base64.Decoder decoder = Base64.getDecoder();
                                                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                                                    to = doc.get("Username").toString();
                                                    textPublicKey = doc.get("Public Key").toString();
                                                    byte[] bytePublicKey = decoder.decode(textPublicKey);
                                                    X509EncodedKeySpec spec = new X509EncodedKeySpec(bytePublicKey);
                                                    PublicKey publicKey = keyFactory.generatePublic(spec);
                                                    encryptedAmount = encoder.encodeToString(PaymentHelper.encrypt(amountStr,publicKey));
                                                    encryptedCardNumber = encoder.encodeToString(PaymentHelper.encrypt(cardNumber,publicKey));
                                                    encryptedCvc = encoder.encodeToString(PaymentHelper.encrypt(cvc,publicKey));

                                                    //create Payment object
                                                    payment.put("Amount", encryptedAmount);
                                                    payment.put("Card Number", encryptedCardNumber);
                                                    payment.put("Cvc", encryptedCvc);
                                                    payment.put("Signature", textDigitalSignature);
                                                    payment.put("Public Key", textPublicKey);
                                                    payment.put("From", from);
                                                    payment.put("To",to);
                                                }
                                            }
                                            if(!userFound){
                                                inputPayee.requestFocus();
                                                Toast.makeText(MakePayment.this, "User not found",Toast.LENGTH_SHORT).show();
                                            }else{
                                                //send payment
                                                if (Double.valueOf(inputAmount.getText().toString()) <= accessibleFunds&&!inputAmount.getText().toString().isEmpty()) {
                                                    Log.d("Input", inputAmount.getText().toString());
                                                    Log.d("Accessible Funds", String.valueOf(accessibleFunds));
                                                    System.out.println("Payment Empty - "+payment.isEmpty());
                                                    db.document("Users/"+currentUser.getEmail()).update("Accessible Funds", accessibleFunds- Double.valueOf(inputAmount.getText().toString())).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()){
                                                                db.collection("Pending Payments").add(payment).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                                    @Override
                                                                    public void onSuccess(DocumentReference documentReference) {
                                                                        Toast.makeText(MakePayment.this, "Success", Toast.LENGTH_SHORT).show();
                                                                        finishActivity(107);
                                                                        startActivity(new Intent(MakePayment.this, Home.class));
                                                                        finish();
                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Toast.makeText(MakePayment.this, "Payment Failed", Toast.LENGTH_SHORT).show();
                                                                        Log.w("Failed", "Error adding document", e);
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });

                                                }else{
                                                    inputAmount.requestFocus();
                                                    Log.d("Payment Failed", "Insufficient Funds");
                                                    Toast.makeText(MakePayment.this, "Payment Failed, Insufficient Funds",Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }catch(Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }
}
