package com.codepath.travel.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codepath.travel.R;
import com.codepath.travel.models.ParseModelConstants;
import com.codepath.travel.models.Trip;
import com.codepath.travel.models.User;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class HomeActivity extends AppCompatActivity implements PlaceSelectionListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int LOGIN_REQUEST = 0;
    private static final int CREATE_STORY_REQUEST = 1;

    // intent arguments
    public static final String CREATED_TRIP_ID = "trip_id";

    @BindView(R.id.drawer_layout) DrawerLayout mDrawer;
    @BindView(R.id.toolbar)  Toolbar toolbar;
    @BindView(R.id.nvView) NavigationView nvDrawer;
    @BindView(R.id.btStartNewTrip) Button btNewTrip;
    @BindView(R.id.etDestination) TextView etDestination;
    @BindView(R.id.lvMyTrips) ListView lvMyTrips;

    // Views in Navigatin view
    private ImageView ivProfileImage;
    private TextView tvProfileName;
    private ArrayList<Trip> mTrips;
    private ArrayAdapter<Trip> mTripsAdapter;


    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        // Facebook integration
        FacebookSdk.sdkInitialize(getApplicationContext());
        FacebookSdk.setIsDebugEnabled(true);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        AppEventsLogger.activateApp(getApplication());

        setupViews();
        setupDrawerContent(nvDrawer);

        // User login
        if (ParseUser.getCurrentUser() != null) { // start with existing user
            startWithCurrentUser();
        } else {
            launchLoginActivity();
        }

        //Search autocomplete fragment initialization
        setupSearchAutoComplete();
        setUpClickListeners();
    }

    private void setupViews() {
        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        nvDrawer = (NavigationView) findViewById(R.id.nvView);
        View nvHeader = nvDrawer.getHeaderView(0);
        this.ivProfileImage = (ImageView) nvHeader.findViewById(R.id.ivProfilePic);
        this.ivProfileImage.setImageResource(0);

        this.tvProfileName = (TextView) nvHeader.findViewById(R.id.tvName);

        this.mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = setupDrawerToggle();
        mDrawer.addDrawerListener(drawerToggle);

        mTrips = new ArrayList<>();
        mTripsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mTrips);
        lvMyTrips.setAdapter(mTripsAdapter);
        refreshMyTrips();
    }

    private void refreshMyTrips() {
        ParseQuery<Trip> myTripsQuery = ParseQuery.getQuery("Trip");
        mTrips.clear();
        myTripsQuery.whereEqualTo(ParseModelConstants.USER_KEY, ParseUser.getCurrentUser());
        myTripsQuery.findInBackground((List<Trip> myTrips, ParseException e) -> {
            if (e == null) {
                mTrips.addAll(myTrips);
                mTripsAdapter.notifyDataSetChanged();
                Toast.makeText(HomeActivity.this, "Got my trips", Toast.LENGTH_LONG);
            } else {
                Log.d("Fetch Trips error", e.toString());
            }
        });
    }

    private void setUpClickListeners() {
        btNewTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createTrip = new Intent(HomeActivity.this, CreateStoryActivity.class);
                createTrip.putExtra(
                    CreateStoryActivity.DESTINATION_ARGS,
                    etDestination.getText().toString()
                );
                etDestination.setText("");
                startActivityForResult(createTrip, CREATE_STORY_REQUEST);
            }
        });
    }

    // Get the userId from the cached currentUser object
    private void startWithCurrentUser() {
        User user = (User) ParseUser.getCurrentUser();
        this.tvProfileName.setText(user.getUsername());
        Glide.with(this).load(user.getProfilePicUrl())
                .placeholder(R.drawable.com_facebook_profile_picture_blank_portrait)
                .fitCenter()
                .bitmapTransform(new CropCircleTransformation(this))
                .into(this.ivProfileImage);
    }

    private void newFBAccountSetup(final User user) {
        GraphRequest request = GraphRequest.newMeRequest(
                AccessToken.getCurrentAccessToken(),
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        try {
                            user.setFbUid(object.getInt("id"));
                            user.setUsername(object.getString("name"));
                            if (object.has("email")) {
                                user.setEmail(object.getString("email"));
                            }
                            if (object.has("picture")) {
                                user.setProfilePicUrl(object.getJSONObject("picture").getJSONObject("data").getString("url"));
                            }
                            if (object.has("cover")) {
                                user.setCoverPicUrl(object.getJSONObject("cover").getString("source"));
                            }
                            user.saveInBackground();
                            startWithCurrentUser();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email,picture,cover");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void launchLoginActivity() {
        ParseLoginBuilder builder = new ParseLoginBuilder(HomeActivity.this);
        startActivityForResult(builder.build(), LOGIN_REQUEST);
    }

    private void logout() {
        Log.d(TAG, String.format("Logging out for user: %s",
                ParseUser.getCurrentUser().getUsername()));
        ParseUser.logOut();
        launchLoginActivity();
    }

    private void deleteAccount() {
        Log.d(TAG, String.format("Deleting account for user: %s",
                ParseUser.getCurrentUser().getUsername()));
        ParseUser.getCurrentUser().deleteEventually();
        ParseUser.logOut();
        launchLoginActivity();
    }

    // Initialize the navigation drawer listener
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    selectDrawerItem(menuItem);
                    return true;
                });
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.drawer_open,  R.string.drawer_close);
    }

    private void setupSearchAutoComplete() {
        // Retrieve the PlaceAutocompleteFragment
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Register a listener to receive callbacks when a place has been selected or an error has occured
        autocompleteFragment.setOnPlaceSelectedListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == LOGIN_REQUEST) {
            ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
            User user = (User) ParseUser.getCurrentUser();
            // update user fields if this is a new facebook login
            if (user.isNew() && ParseFacebookUtils.isLinked(user)) {
                newFBAccountSetup(user);
            }
            startWithCurrentUser();
        } else if (resultCode == RESULT_OK && requestCode == CREATE_STORY_REQUEST) {
            refreshMyTrips();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Callback invoked when a place has been selected from the PlaceAutocompleteFragment.
    @Override
    public void onPlaceSelected(Place place) {
//        Log.i(TAG, "Place Selected: " + place.getName());
//
//        //Set place image and place details
//        Glide.with(this).load("http://www.english-heritage.org.uk/content/properties/stonehenge/things-to-do/stonehenge-in-day")
//                .into(this.ivPlace);
//        tvPlaceName.setText(place.getName());
//        tvPlaceAddress.setText(place.getAddress());
//
//        CharSequence attributions = place.getAttributions();
//        if (!TextUtils.isEmpty(attributions)) {
//            tvPlaceAttribution.setText(Html.fromHtml(attributions.toString()));
//        } else {
//            tvPlaceAttribution.setText("");
//        }
    }

    //Callback invoked when PlaceAutocompleteFragment encounters an error.
    @Override
    public void onError(Status status) {
        Log.e(TAG, "onError: Status = " + status.toString());

        Toast.makeText(this, "Place selection failed: " + status.getStatusMessage(),
                Toast.LENGTH_SHORT).show();
    }

    public void onSearchedPlace(View view) {
        Intent intent = new Intent(this, CreateStoryActivity.class);
        startActivity(intent);
    }

    // Assign action to perform for different navigation drawer item
    public void selectDrawerItem(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.nav_logout:
                    logout();
                break;
            case R.id.nav_delete_account:
                    deleteAccount();
                break;
            default: break;
        }

        menuItem.setChecked(true);
        setTitle(menuItem.getTitle());
        mDrawer.closeDrawers();
    }

}
