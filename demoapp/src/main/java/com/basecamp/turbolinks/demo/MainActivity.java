package com.basecamp.turbolinks.demo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.basecamp.turbolinks.TurbolinksAdapter;
import com.basecamp.turbolinks.TurbolinksSession;
import com.basecamp.turbolinks.TurbolinksView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.iid.FirebaseInstanceId;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;

// Interface for custom JS functions
class JellySwitch {
    private MainActivity mainActivity;
    private static String lastMenu = "";

    public JellySwitch() { }

    public void updateActivity(MainActivity _mainActivity) {
        mainActivity = _mainActivity;
    }

    @JavascriptInterface
    public String toString() { return "Hello, I am a Jellyswitch object."; }

    @JavascriptInterface
    public void postMenu(String menuJson) {
        Log.d("Menu-Rec", menuJson);
        if (lastMenu != menuJson) {
            lastMenu = menuJson;
            mainActivity.createMenu(menuJson);
        }
    }
}

public class MainActivity extends AppCompatActivity implements TurbolinksAdapter {
    public static final String INTENT_URL = "intentUrl";
    public static final  String CHANNEL_ID = "Announcements";

    private static JellySwitch js = new JellySwitch();
    private static String firebaseToken = "";

    private String BASE_URL = "";
    private TurbolinksView turbolinksView;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private LinkedList<String> navHistory = new LinkedList<>();

    private String currntLocation = "";

    // -----------------------------------------------------------------------
    // Life Cycle
    // -----------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BASE_URL = getResources().getString(R.string.base_url);
        createNotificationChannel();

        // Find elements used class wide
        turbolinksView = findViewById(R.id.turbolinks_view);
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Set up title bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //// Set up TurboLinks ////

        // For this demo app, we force debug logging on. You will only want to do
        // this for debug builds of your app (it is off by default)
        // TurbolinksSession.getDefault(this).setDebugLoggingEnabled(true);

        // Create callback for updating the agent after the token is created
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(this, instanceIdResult -> {
            firebaseToken = instanceIdResult.getToken();
            Log.d("Push", firebaseToken);
            this.updateAgent();
        });

        this.updateAgent();

        // Get location from intent
        currntLocation = locationString(getIntent().getStringExtra(INTENT_URL));

        // Go to page in turbolinks
        TurbolinksSession.getDefault(this)
                .activity(this)
                .adapter(this)
                .view(turbolinksView)
                .visit(currntLocation);

        // Add interface
        TurbolinksSession.getDefault(this)
            .addJavascriptInterface(js, "JellyswitchAndroid");

        // The interface is not re-created in the webview, so update the reference to this instance
        js.updateActivity(this);

        Log.d("Lifecylcle", "Create App");
    }

    private void updateAgent() {
        TurbolinksSession.getDefault(this).getWebView().getSettings().setUserAgentString(userAgentStr());
        Log.d("UserAgent", TurbolinksSession.getDefault(this).getWebView().getSettings().getUserAgentString());
    }

    private String userAgentStr() {
        String oldUserAgent = TurbolinksSession.getDefault(this).getWebView().getSettings().getUserAgentString();
        final String baseAgent = " Jellyswitch/Android/1.2";
        final String token = firebaseToken == "" ? "" : " token: " + firebaseToken;

        if (!oldUserAgent.contains(baseAgent) && !oldUserAgent.contains("token:")) {
            return oldUserAgent + baseAgent + token;
        } else if (!oldUserAgent.contains("token:")) {
            return oldUserAgent + token;
        } else {
            return oldUserAgent;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getStringExtra(INTENT_URL) != null) {
            this.appNavigation(intent.getStringExtra(INTENT_URL));
        }
        Log.d("Lifecycle", "New Intent");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        TurbolinksSession.getDefault(this)
                .activity(this)
                .adapter(this)
                .restoreWithCachedSnapshot(true)
                .view(turbolinksView)
                .visit(currntLocation);
        Log.d("Life", "Restart App");
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            finish();
            overridePendingTransition(0, 0);
        }
    }

    // -----------------------------------------------------------------------
    // TurbolinksAdapter interface
    // -----------------------------------------------------------------------

    @Override
    public void onPageFinished() {
    }

    @Override
    public void onReceivedError(int errorCode) {
        handleError(errorCode);
    }

    @Override
    public void pageInvalidated() {

    }

    @Override
    public void requestFailedWithStatusCode(int statusCode) {
        handleError(statusCode);
    }

    @Override
    public void visitCompleted() {
    }

    // The starting point for any href clicked inside a Turbolinks enabled site. In a simple case
    // you can just open another activity, or in more complex cases, this would be a good spot for
    // routing logic to take you to the right place within your app.
    @Override
    public void visitProposedToLocationWithAction(String location, String action) {
        this.appNavigation(location);
    }

    // Simply forwards to an error page, but you could alternatively show your own native screen
    // or do whatever other kind of error handling you want.
    private void handleError(int code) {
        if (code == 404) {
            TurbolinksSession.getDefault(this)
                    .activity(this)
                    .adapter(this)
                    .restoreWithCachedSnapshot(false)
                    .view(turbolinksView)
                    .visit(BASE_URL);
        }
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    public void createMenu(String mjson) {
       runOnUiThread(() -> {
           try {
               JSONArray menuList = new JSONArray(mjson);
               Menu menu = navigationView.getMenu();
               menu.clear();

               for (int i = 0; i < menuList.length(); i++) {
                   JSONObject menuItem = menuList.getJSONObject(i);
                   String title = menuItem.getString("title");
                   String path = menuItem.getString("path");
                   MenuItem mi = menu.add(title);
                   mi.setShowAsAction(0);
                   mi.setOnMenuItemClickListener(m -> {
                       this.onMenuItemClick(path);
                       return true;
                   });
               }
           } catch(Exception e) {
               Log.d("Menu-Error", e.getMessage());
           }
       });
    }

    public void onMenuClick(View v) {
        // Scroll to top
        RecyclerView recyclerView = (RecyclerView) navigationView.getChildAt(0);
        LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
        layoutManager.scrollToPositionWithOffset(0, 0);

        drawer.openDrawer(GravityCompat.START);
    }

    public void onMenuItemClick(String location) {
        this.appNavigation(location);
        drawer.closeDrawer(GravityCompat.START);
    }

    private void appNavigation(String location) {
        Log.d("Nav", locationString(location));

        if (location.startsWith("http") && !location.contains(BASE_URL)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(location));
            this.startActivity(intent);
        } else if (location.contains("/doors/") && location.endsWith("open")) {
            // Dont launch new intent, user should not be revisiting this location
            TurbolinksSession.getDefault(this)
                    .activity(this)
                    .adapter(this)
                    .view(turbolinksView)
                    .visit(locationString(location));
        } else if (locationString(location).equals(currntLocation)) {
            // Do nothing, dont need to create extra pages to go back to. Optional, we could re-load the page
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra(INTENT_URL, location);
            this.startActivity(intent);
        }
    }

    // Converts different location string options to the same
    private String locationString(String baseLocation) {
        if (baseLocation == null) {
            return BASE_URL;
        } else if (baseLocation.startsWith("http")) {
            return baseLocation;
        } else if (baseLocation.startsWith("/")) {
            return BASE_URL + baseLocation;
        } else {
            return BASE_URL + "/" + baseLocation;
        }
    }

    // -----------------------------------------------------------------------
    // Notification
    // -----------------------------------------------------------------------

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Announcements", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for announcements from the admin");
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
