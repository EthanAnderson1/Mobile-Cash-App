package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;


public class Home extends AppCompatActivity {
    private DocumentSnapshot currentUserDetails;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    PublicKey publicKey;
    PrivateKey privateKey;
    private PaymentsClient mPaymentsClient;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private View mGooglePayButton;

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Button makePayment = findViewById(R.id.makePaymentbtn);
        final TextView currentBalance=findViewById(R.id.currentBalancelbl);
        final TextView username = findViewById(R.id.usernamelbl);
        final LinearLayout  pendingPayments = findViewById(R.id.pendingPayments);

        Wallet.WalletOptions walletOptions = new Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build();
        mPaymentsClient = Wallet.getPaymentsClient(this,walletOptions);
        mGooglePayButton = findViewById(R.id.googlepay_button);
        possiblyShowGooglePayButton();
        mGooglePayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        requestPayment(view);
                    }
                });

        db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    username.setText("Welcome "+task.getResult().get("Username").toString());
                    currentBalance.setText(decimalFormat.format(task.getResult().getDouble("Accessible Funds")));
                    currentUserDetails = task.getResult();
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

                    db.document(currentUserDetails.getReference().getPath()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                            try {
                                currentBalance.setText(decimalFormat.format(documentSnapshot.getDouble("Accessible Funds")));
                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    });

                    db.collection("Pending Payments").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()) {
                                List<DocumentSnapshot> docs = task.getResult().getDocuments();
                                LinearLayout linearLayout = findViewById(R.id.pendingPayments);
                                View child;
                                int i = 0;
                                for (final DocumentSnapshot doc : docs) {
                                    String payee = doc.get("From").toString();
                                    if (currentUserDetails.get("Username").toString().equals(payee)) {
                                        Log.d("success", "payment found");
                                        child = linearLayout.inflate(getApplicationContext(),R.layout.pending_payment,null);
                                        child.setId(10*i+6);
                                        pendingPayments.addView(child);
                                        TextView to = findViewById(R.id.fromlbl);
                                        to.setId(10*i+1);
                                        TextView amount = findViewById(R.id.amountlbl);
                                        amount.setId(10*i+2);
                                        TextView paymentId = findViewById(R.id.paymentId);
                                        paymentId.setId(10*i+3);
                                        to.setText("To: "+doc.get("To").toString());
                                        amount.setText("Amount: Encrypted");
                                        paymentId.setText("Payment Id: "+doc.getId());
                                        Button submit = findViewById(R.id.confirmbtn);
                                        ViewGroup viewGroup = (ViewGroup) submit.getParent();
                                        viewGroup.removeView(submit);
                                        Button reject = findViewById(R.id.rejectbtn);
                                        viewGroup = (ViewGroup) reject.getParent();
                                        viewGroup.removeView(reject);
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
        });



        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.selectTab(tabLayout.getTabAt(0));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 1:
                        startActivity(new Intent(Home.this, AcceptPayment.class));
                        finish();
                        break;

                    case 2:
                        startActivity(new Intent(Home.this, AccountSettings.class));
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

        makePayment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Home.this,MakePayment.class),107);
            }
        });

    }


    //checks if a google pay payment is possible if it isn't then the button wont be displayed
    private void possiblyShowGooglePayButton() {
        final Optional<JSONObject> isReadyToPayJson = PaymentHelper.getIsReadyToPayRequest();
        if (!isReadyToPayJson.isPresent()) {
            return;
        }
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString());
        if (request == null) {
            return;
        }
        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(this,
                new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            setGooglePayAvailable(task.getResult());
                        } else {
                            Log.w("isReadyToPay failed", task.getException());
                        }
                    }
                });
    }


    private void setGooglePayAvailable(boolean available) {
        if (available) {
            mGooglePayButton.setVisibility(View.VISIBLE);
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // value passed in AutoResolveHelper
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        handlePaymentSuccess(paymentData);
                        break;
                    case Activity.RESULT_CANCELED:
                        // Nothing to here normally - the user simply cancelled without selecting a
                        // payment method.
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        handleError(status.getStatusCode());
                        break;
                    default:
                        // Do nothing.
                }

                // Re-enables the Google Pay payment button.
                mGooglePayButton.setClickable(true);
                break;
        }
    }

    public void requestPayment(View view) {
        // Disables the button to prevent multiple clicks.
        mGooglePayButton.setClickable(false);

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        String price = "5";

        // TransactionInfo transaction = PaymentsUtil.createTransaction(price);
        Optional<JSONObject> paymentDataRequestJson = PaymentHelper.getPaymentDataRequest(price);

        if (!paymentDataRequestJson.isPresent()) {
            Log.d("error","not worked");
            return;
        }

        PaymentDataRequest request = PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString());

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            AutoResolveHelper.resolveTask(mPaymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
    }

    private void handleError(int statusCode) {
        Log.w("loadPaymentData failed", String.format("Error code: %d", statusCode));
    }

    private void handlePaymentSuccess(PaymentData paymentData) {
        String paymentInformation = paymentData.toJson();

        // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
        if (paymentInformation == null) {
            return;
        }
        JSONObject paymentMethodData;

        try {
            paymentMethodData = new JSONObject(paymentInformation).getJSONObject("paymentMethodData");
            // If the gateway is set to "example", no payment information is returned - instead, the
            // token will only consist of "examplePaymentMethodToken".
            if (paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("type")
                    .equals("PAYMENT_GATEWAY")
                    && paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token")
                    .equals("examplePaymentMethodToken")) {
                AlertDialog alertDialog =
                        new AlertDialog.Builder(this)
                                .setTitle("Warning")
                                .setMessage(
                                        "Gateway name set to \"example\" - please modify "
                                                + "Constants.java and replace it with your own gateway.")
                                .setPositiveButton("OK", null)
                                .create();
                alertDialog.show();
            }

            String billingName =
                    paymentMethodData.getJSONObject("info").getJSONObject("billingAddress").getString("name");
            Log.d("BillingName", billingName);
            //Toast.makeText(this, getString(R.string.payments_show_name, billingName), Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Payment Successful", Toast.LENGTH_LONG).show();
            db.document("Users/"+currentUser.getEmail()).update("Accessible Funds", FieldValue.increment(5));
            // Logging token string.
            Log.d("GooglePaymentToken", paymentMethodData.getJSONObject("tokenizationData").getString("token"));
        } catch (JSONException e) {
            Log.e("handlePaymentSuccess", "Error: " + e.toString());
            return;
        }
    }



}
