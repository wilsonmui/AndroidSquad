package edu.ucsb.cs48.lookup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * Created by esuarez on 2/4/18.
 */

public class SignUpPageActivity extends AppCompatActivity implements View.OnClickListener {

    //==============================================================================================
    // Declare Variables
    //==============================================================================================
    private EditText editTextEmail, editTextPassword, editTextName, editTextPhone;
    private ProgressBar progressBar;
    private TextView textViewSignIn;
    private FirebaseAuth mAuth;
    private static final int SIGN_IN_REQUEST = 0;
    private Button buttonSignUp;
    private CallbackManager callbackManager;
    private String g_username = "";

    private DatabaseReference db;
    private StorageReference storageRef;

    FirebaseUser user;

    private String fbLink;

    private static final String NAME = "public_profile", EMAIL = "email";
    private TextView info;

    GoogleSignInClient mGoogleSignInClient;

    private static final int CAMERA_REQUEST = 1888;
    private ImageView user_profile_photo;
    //==============================================================================================
    // On Create Setup
    //==============================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Check if User is Authenticated
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currUser = mAuth.getCurrentUser();
        updateUI(currUser);

        setContentView(R.layout.sign_up_page);

        initListeners();

        db = FirebaseDatabase.getInstance().getReference();

        //setup Google sign-in options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("499747879832-vaojkbm5j81thueo3m2k6miqhrn57rt6.apps.googleusercontent.com")
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.sign_in_button).setOnClickListener(this);

        // Facebook sign up
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) findViewById(R.id.fb_sign_up_button); //TODO: why this happen ??!!??!
        info = (TextView)findViewById(R.id.info);
        loginButton.setReadPermissions(Arrays.asList(NAME, EMAIL));

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                try {
                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                            new GraphRequest.GraphJSONObjectCallback() {
                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                    try {
                                        String id = object.getString("id");
                                        Log.d(TAG, "FB id: " + id);
                                        setFBLink(id);
                                        Log.d(TAG, "FB link successful");
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                        Log.d(TAG, "FB link unsuccessful");
                                    }
                                }
                            });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields","link");
                    request.setParameters(parameters);
                    request.executeAsync();
                }
                catch (Exception e) {
                    Log.d("FACEBOOK ERROR", "cancelled");
                }
                setResult(RESULT_OK);
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
//                finish();
            }

            @Override
            public void onCancel() {
                setResult(RESULT_CANCELED);
                Log.d(TAG, "facebook:onCancel");
                finish();
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d(TAG, "facebook:onError", exception);
            }
        });


    }

    //==============================================================================================
    // On Start setup
    //==============================================================================================
    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);

    }

    //==============================================================================================
    // Action Listeners
    //==============================================================================================
    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.buttonSignUp:
                registerUser();
                break;
            case R.id.textViewSignIn:
                finish();
                startActivity(new Intent(this, SignInPageActivity.class));
                break;
            case R.id.sign_in_button:
                signIn();
                break;
        }
    }

    //==============================================================================================
    // Helper Functions
    //==============================================================================================
    private void updateUI(FirebaseUser currentUser) {

        if (currentUser != null) {
            finish();
            startActivity(new Intent(this, HomePageActivity.class));
        } else {
            Log.d(TAG, "current user is null");
        }

    }

    private void initListeners() {

        editTextName = (EditText) findViewById(R.id.editTextName);
        editTextEmail = (EditText)findViewById(R.id.editTextEmail);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        editTextPhone = (EditText)findViewById(R.id.editTextPhone);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        buttonSignUp = (Button) findViewById(R.id.buttonSignUp);
        textViewSignIn = (TextView) findViewById(R.id.textViewSignIn);
        user_profile_photo =(ImageView) findViewById(R.id.user_profile_photo);
        Button photoButton = (Button) findViewById(R.id.set_photo_button);

        photoButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        buttonSignUp.setOnClickListener(this);
        textViewSignIn.setOnClickListener(this);
    }

    private void registerUser() {

        // Sanitize Inputs
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        final String phone = editTextPhone.getText().toString().trim();

        if(name.isEmpty()) {
            editTextName.setError("Name is required");
            editTextName.requestFocus();
            return;
        }

        if(email.isEmpty()) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            editTextPhone.setError("Phone Number is required");
            editTextPhone.requestFocus();
            return;
        }

        if (phone.length() < 10) {
            editTextPhone.setError("Please enter a valid phone number");
            editTextPhone.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if(password.length() < 6) {
            editTextPassword.setError("Minimum length of password should be 6");
            editTextPassword.requestFocus();
            return;
        }


        // Show Progress bar
        progressBar.setVisibility(View.VISIBLE);

        // If all fields pass all checks proceed to creation
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {

            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressBar.setVisibility(View.GONE);

                if (task.isSuccessful()) {

                    // Variable Set up
                    String name = editTextName.getText().toString().trim();
                    String email = editTextEmail.getText().toString().trim();
                    String phone = editTextPhone.getText().toString().trim();
                    FirebaseUser currUser = mAuth.getCurrentUser();
                    String uid = currUser.getUid();
                    Uri photoUrl = currUser.getPhotoUrl();

                    // Save User Data to DataBase
                    saveUserData(name, email, phone, uid, photoUrl);

                    // Sign in success, update UI with the signed in User's Information
                    updateUI(currUser);

                } else {

                    // If sign in fails, display a message to the user.
                    Toast.makeText(SignUpPageActivity.this, "Authentication failed." + task.getException(),
                            Toast.LENGTH_SHORT).show();
                    updateUI(null);
                }
            }
        });
    }

    private void saveUserData(String name, String email, String phone, String userId, Uri photoUrl) {
        User user = new User(name, email, phone, userId, photoUrl);
        db.child("users").child(userId).setValue(user);
    }

    private void setFBLink(String fbID) {
        fbLink = "https://facebook.com/" + fbID;
    }

    private String getFBLink() {
        return fbLink;
    }

    private void saveFBUserLink(String link, String userId) {
        Map<String,String> userFBData = new HashMap<String,String>();
        userFBData.put("facebook", link);
        db.child("users").child(userId).child("facebook").setValue(link);
    }

    // sign-in for Google
    // note sign out: FirebaseAuth.getInstance().signOut();
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, SIGN_IN_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == SIGN_IN_REQUEST) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }

        if (requestCode == CAMERA_REQUEST) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            user_profile_photo.setImageBitmap(photo);

        }
    }


    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
            // Signed in successfully, show authenticated UI
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "Google sign in failed", e);
        }
    }

    //exchange Google ID token for Firebase credential
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct){
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            user = mAuth.getCurrentUser();

                            saveUserData(user.getDisplayName(), user.getEmail(),user.getPhoneNumber(), user.getUid(), user.getPhotoUrl());
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            //Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            user = mAuth.getCurrentUser();
                            String name = user.getDisplayName();
                            String email = user.getEmail();
                            String phone = user.getPhoneNumber();
                            String uid = user.getUid();
                            Uri photoUrl = user.getPhotoUrl();
                            String link = getFBLink();
                            saveUserData(name, email, phone, uid, photoUrl);
                            saveFBUserLink(link, uid);
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(SignUpPageActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                    }
                });
    }


    public void uploadImageFileToFirebaseStorage(String userID, Uri imageFilePathUri) {

        // Checking whether FilePathUri Is empty or not.
        if (imageFilePathUri != null) {

            // Setting progressDialog Title.
//            progressDialog.setTitle("Image is Uploading...");

            // Showing progressDialog.
//            progressDialog.show();

            // Creating second StorageReference.
            StorageReference storageRef2 = storageRef.child(userID + "_" + System.currentTimeMillis() + "." + GetFileExtension(imageFilePathUri));

            // Adding addOnSuccessListener to second StorageReference.
            storageRef2.putFile(imageFilePathUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            // Getting image name from EditText and store into string variable.
//                            String TempImageName = ImageName.getText().toString().trim();

                            // Hiding the progressDialog after done uploading.
//                            progressDialog.dismiss();

                            // Showing toast message after done uploading.
                            Toast.makeText(getApplicationContext(), "Image Uploaded Successfully ", Toast.LENGTH_LONG).show();
//                            @SuppressWarnings("VisibleForTests")
//                            ImageUploadInfo imageUploadInfo = new ImageUploadInfo(userID + "_profile_pic", taskSnapshot.getDownloadUrl().toString());

//                            // Getting image upload ID.
//                            String ImageUploadId = databaseRef.push().getKey();
//
//                            // Adding image upload id s child element into databaseReference.
//                            databaseRef.child(ImageUploadId).setValue(imageUploadInfo);

=                            Log.d(TAG, "pic to upload: " + taskSnapshot.getDownloadUrl().toString());
                            userProfilePicURL = taskSnapshot.getDownloadUrl().toString();

                            // put the changes to the database
                            updateDatabase();
                        }
                    })
                    // If something goes wrong .
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {

                            // Hiding the progressDialog.
//                            progressDialog.dismiss();

                            // Showing exception erro message.
                            Toast.makeText(SignUpPageActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })

                    // On progress change upload time.
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                            // Setting progressDialog Title.
//                            progressDialog.setTitle("Image is Uploading...");

                        }

                    })

        }

    }

    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }

}
