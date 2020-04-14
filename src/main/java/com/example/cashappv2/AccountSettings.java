package com.example.cashappv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.widget.Toast.LENGTH_SHORT;
import static com.google.firebase.auth.FirebaseAuth.getInstance;

public class AccountSettings extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        Button manageAccount = findViewById(R.id.manageAccountbtn);
        Button logout = findViewById(R.id.logoutbtn);
        Button deleteAccount = findViewById(R.id.deleteAccountbtn);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.selectTab(tabLayout.getTabAt(3));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        startActivity(new Intent(AccountSettings.this, Home.class));
                        break;
                    case 1:
                        startActivity(new Intent(AccountSettings.this, AcceptPayment.class));
                        break;
                    case 2:
                        startActivity(new Intent(AccountSettings.this, Logs.class));
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
                FirebaseUser currentUser = getInstance().getCurrentUser();
                db.document("Users/"+currentUser.getEmail()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(AccountSettings.this, "User Data Deleted.", LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(AccountSettings.this, "Failed to Delete Account.", LENGTH_SHORT).show();
                        }
                    }
                });
                currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            startActivity(new Intent(AccountSettings.this, Login.class));
                            Toast.makeText(AccountSettings.this, "Account Deleted.", LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(AccountSettings.this, "Failed to Delete Account.", LENGTH_SHORT).show();
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
            }
        });
    }
}
