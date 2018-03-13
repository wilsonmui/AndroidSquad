package edu.ucsb.cs48.lookup;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/*
This activity displays the current user's list of contacts. Should provide methods to pull up
contact info and remove them from contacts list.
Uses Contacts_Adapter to display RecyclerView
 */
public class ContactsPageActivity extends AppCompatActivity implements View.OnClickListener {

    FirebaseAuth mAuth;
    ArrayList<String> found_contacts = new ArrayList<>();
    EditText search;
    ArrayList<String> contacts = new ArrayList<>();
    RecyclerView contacts_list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(this, SignInPageActivity.class));
        }

        setContentView(R.layout.activity_contacts_page);
        findViewById(R.id.back_button).setOnClickListener(this);
        findViewById(R.id.search_button).setOnClickListener(this);
        search = findViewById(R.id.search);



        contacts_list = (RecyclerView) findViewById(R.id.contacts_list);
        contacts_list.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        contacts_list.setLayoutManager(llm);

        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        loadContacts(currentFirebaseUser.getUid(), contacts_list);
    }

    public void loadContacts(final String uid, final RecyclerView contacts_list) {

        mReadDataOnce("network", uid, new OnGetDataListener() {
            @Override
            public void onStart() {
                //DO SOME THING WHEN START GET DATA HERE
            }

            @Override
            public void onSuccess(DataSnapshot data) {

                for (DataSnapshot contactsDs : data.getChildren()) {
                    contacts.add(contactsDs.getValue().toString());
                }

                Contacts_Adapter ca = new Contacts_Adapter(contacts);

                contacts_list.setAdapter(ca);
            }


            @Override
            public void onFailed(DatabaseError databaseError) {
                System.out.println("-- Get User Contacts Failed --");
            }
        });
    }

    public void mReadDataOnce(String child, String innerChild, final OnGetDataListener listener) {
        listener.onStart();
        FirebaseDatabase.getInstance().getReference().child(child).child(innerChild).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listener.onSuccess(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                listener.onFailed(databaseError);
            }
        });
    }

    //update adapter with found users
    public void updateAdapter(String search){
        //search contacts
        for(int i = 0; i < contacts.size(); i++){
            if (contacts.get(i).toLowerCase().contains(search.toLowerCase())){
                found_contacts.add(contacts.get(i));
            }
        }

        Contacts_Adapter ca = new Contacts_Adapter(found_contacts);
        contacts_list.setAdapter(ca);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.back_button:
                startActivity(new Intent(this, HomePageActivity.class));
                break;
            case R.id.search_button:
                updateAdapter(search.toString());
                break;
        }
    }


}


