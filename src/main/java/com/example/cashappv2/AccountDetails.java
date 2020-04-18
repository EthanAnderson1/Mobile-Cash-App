package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


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
import com.google.firebase.firestore.FirebaseFirestore;

import static android.widget.Toast.LENGTH_SHORT;

public class AccountDetails extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
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

        Button update = findViewById(R.id.update);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Success", "Details Updated");
                Toast.makeText(getApplicationContext(), "Details Updated", LENGTH_SHORT).show();
                if(!firstName.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("First Name", firstName.getText().toString());
                }
                if(!lastName.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Last Name", lastName.getText().toString());
                }
                if(!cardHoldersName.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Account Holders Name", cardHoldersName.getText().toString());
                }
                if(!accountNumber.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Account Number", accountNumber.getText().toString());
                }
                if(!sortCode.getText().toString().isEmpty()) {
                    db.document("Users/" + currentUser.getEmail()).update("Sort Code", sortCode.getText().toString());
                }
                if(!password.getText().toString().isEmpty()) {
                    if (Cpassword.getText().toString().equals(password.getText().toString())) {
                        currentUser.updatePassword(password.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d("Success", "Password Updated");
                                }else{
                                    Log.d("Fail", "Password Not Updated");
                                }
                            }
                        });
                    }else{
                        password.setError("Please enter a valid password");
                        password.requestFocus();
                        Cpassword.requestFocus();
                    }
                }
                finish();
            }
        });
    }
}
