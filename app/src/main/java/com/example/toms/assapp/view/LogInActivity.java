package com.example.toms.assapp.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.toms.assapp.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LogInActivity extends AppCompatActivity {

    public static final int KEY_UIF = 102;

    private CallbackManager callbackManager;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        callbackManager = CallbackManager.Factory.create();
        mAuth = FirebaseAuth.getInstance();

        final LoginButton loginButton = findViewById(R.id.login_button_facebook);
        loginButton.setReadPermissions("email", "public_profile");

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case KEY_UIF:
                    FirebaseUser user = mAuth.getCurrentUser();
                    //Volver a la pantalla
                    Intent info = MainActivity.respuestaLogin(user.getDisplayName());
                    setResult(Activity.RESULT_OK,info);
                    finish();
                    break;
            }
        }else {
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        //Progess dialog
        final ProgressDialog prog= new ProgressDialog(LogInActivity.this);
        prog.setTitle("Por favor espere");
        prog.setMessage("Estamos cargando su imagen");
        prog.setCancelable(false);
        prog.setIndeterminate(true);
        prog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        prog.show();
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            final FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);

                            String email = user.getEmail();
                            String phone = user.getPhoneNumber();
                            String dataBaseName;
                            if (email != null) {
                                String mail = email.substring(0, email.indexOf("."));
                                dataBaseName = mail;
                            }else {
                                dataBaseName = phone;
                            }

                            DatabaseReference mReference = FirebaseDatabase.getInstance().getReference();
                            DatabaseReference idProfile = mReference.child(dataBaseName).child(getResources().getString(R.string.uif_reference_child));
                            idProfile.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()){
                                        Intent info = MainActivity.respuestaLogin(user.getDisplayName());
                                        prog.dismiss();
                                        setResult(Activity.RESULT_OK,info);
                                        finish();
                                    }else {
                                        Intent intent = new Intent( LogInActivity.this,UifDataActivity.class);
                                        Bundle bundle = new Bundle();
                                        bundle.putString(UifDataActivity.KEY_EMAIL,user.getEmail());
                                        bundle.putString(UifDataActivity.KEY_PHONE,user.getPhoneNumber());
                                        bundle.putString(UifDataActivity.KEY_FULL_NAME,user.getDisplayName());
                                        intent.putExtras(bundle);
                                        prog.dismiss();
                                        startActivityForResult(intent, KEY_UIF);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    prog.dismiss();
                                }
                            });

                        } else {
                            // If sign in fails, display a message to the user.
                            updateUI(null);
                            prog.dismiss();
                        }
                        prog.dismiss();
                        // ...
                    }
                });
    }


    public void updateUI(FirebaseUser user){
        if (user != null) {
            String name = user.getDisplayName();
            Uri uri = user.getPhotoUrl();
//            Profile profile = Profile.getCurrentProfile();
//            if (profile != null) {
//                Uri uri2 = profile.getProfilePictureUri(500, 500);
//            collapsingToolbarLayout.setTitle(name);
//            Glide.with(this).load(uri).into(imageView);
//            }
        }
    }
}
