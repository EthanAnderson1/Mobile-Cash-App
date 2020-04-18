package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.List;

import static com.google.firebase.auth.FirebaseAuth.getInstance;

public class Home extends AppCompatActivity {
    private DocumentSnapshot currentUserDetails;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        Button makePayment = findViewById(R.id.makePaymentbtn);
        final TextView currentBalance=findViewById(R.id.currentBalancelbl);
        final TextView username = findViewById(R.id.usernamelbl);
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final LinearLayout  pendingPayments = findViewById(R.id.pendingPayments);

        db.document("Users/"+currentUser.getEmail()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    username.setText("Welcome "+task.getResult().get("Username").toString());
                    currentBalance.setText(decimalFormat.format(task.getResult().getDouble("Accessible Funds")));
                    currentUserDetails = task.getResult();


                    db.collection("Pending Payments").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()) {
                                List<DocumentSnapshot> docs = task.getResult().getDocuments();
                                LinearLayout linearLayout = findViewById(R.id.pendingPayments);
                                LayoutInflater layoutInflater = LayoutInflater.from(getApplicationContext());
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
}
