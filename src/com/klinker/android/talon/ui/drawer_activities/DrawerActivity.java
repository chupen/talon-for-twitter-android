package com.klinker.android.talon.ui.drawer_activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.MainDrawerArrayAdapter;
import com.klinker.android.talon.listeners.MainDrawerClickListener;
import com.klinker.android.talon.manipulations.BlurTransform;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.manipulations.MySuggestionsProvider;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.settings.SettingsPagerActivity;
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.ComposeActivity;
import com.klinker.android.talon.ui.ComposeDMActivity;
import com.klinker.android.talon.ui.LoginActivity;
import com.klinker.android.talon.ui.UserProfileActivity;
import com.squareup.picasso.Picasso;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;
import java.util.Arrays;

import de.keyboardsurfer.android.widget.crouton.Crouton;

/**
 * Created by luke on 11/28/13.
 */
public abstract class DrawerActivity extends Activity {

    public AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;

    public ActionBar actionBar;

    public static ViewPager mViewPager;

    public DrawerLayout mDrawerLayout;
    public LinearLayout mDrawer;
    public ListView drawerList;
    public ActionBarDrawerToggle mDrawerToggle;

    public AsyncListView listView;

    public boolean logoutVisible = false;

    public void setUpDrawer(int number, final String actName) {

        MainDrawerArrayAdapter.current = number;

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.drawerIcon});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = (LinearLayout) findViewById(R.id.left_drawer);

        TextView name = (TextView) mDrawer.findViewById(R.id.name);
        TextView screenName = (TextView) mDrawer.findViewById(R.id.screen_name);
        NetworkedCacheableImageView backgroundPic = (NetworkedCacheableImageView) mDrawer.findViewById(R.id.background_image);
        NetworkedCacheableImageView profilePic = (NetworkedCacheableImageView) mDrawer.findViewById(R.id.profile_pic);
        final ImageButton showMoreDrawer = (ImageButton) mDrawer.findViewById(R.id.options);
        final LinearLayout logoutLayout = (LinearLayout) mDrawer.findViewById(R.id.logoutLayout);
        final Button logoutDrawer = (Button) mDrawer.findViewById(R.id.logoutButton);
        drawerList = (ListView) mDrawer.findViewById(R.id.drawer_list);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                resource,  /* nav drawer icon to replace 'Up' caret */
                R.string.app_name,  /* "open drawer" description */
                R.string.app_name  /* "close drawer" description */
        ) {

            public void onDrawerClosed(View view) {
                if (logoutVisible) {
                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    logoutDrawer.setVisibility(View.GONE);
                    drawerList.setVisibility(View.VISIBLE);

                    logoutVisible = false;
                }

                if (MainDrawerArrayAdapter.current > 2) {
                    actionBar.setTitle(actName);
                } else {
                    int position = mViewPager.getCurrentItem();

                    switch (position) {
                        case 0:
                            actionBar.setTitle(getResources().getString(R.string.timeline));
                            break;
                        case 1:
                            actionBar.setTitle(getResources().getString(R.string.mentions));
                            break;
                        case 2:
                            actionBar.setTitle(getResources().getString(R.string.direct_messages));
                            break;
                    }
                }
            }

            public void onDrawerOpened(View drawerView) {
                actionBar.setTitle(getResources().getString(R.string.app_name));
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        showMoreDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(logoutDrawer.getVisibility() == View.GONE) {
                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    logoutDrawer.setVisibility(View.VISIBLE);
                    drawerList.setVisibility(View.GONE);

                    logoutVisible = true;
                } else {
                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    logoutDrawer.setVisibility(View.GONE);
                    drawerList.setVisibility(View.VISIBLE);

                    logoutVisible = false;
                }
            }
        });

        logoutDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutFromTwitter();
            }
        });

        final String sName = settings.myName;
        final String sScreenName = settings.myScreenName;
        final String backgroundUrl = settings.myBackgroundUrl;
        final String profilePicUrl = settings.myProfilePicUrl;

        backgroundPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.closeDrawer(Gravity.START);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent viewProfile = new Intent(context, UserProfileActivity.class);
                        viewProfile.putExtra("name", sName);
                        viewProfile.putExtra("screenname", sScreenName);
                        viewProfile.putExtra("proPic", profilePicUrl);
                        viewProfile.putExtra("tweetid", 0);
                        viewProfile.putExtra("retweet", false);

                        context.startActivity(viewProfile);
                    }
                }, 400);

            }
        });

        Log.v("twitter_drawer", profilePicUrl);

        name.setText(sName);
        screenName.setText("@" + sScreenName);

        // Keeping picasso right now because of the transforms...
        // Don't know how to do them yet with the manual caching
        try {
            Picasso.with(context)
                    .load(backgroundUrl)
                    .transform(new BlurTransform(context))
                    .into(backgroundPic);
            //backgroundPic.loadImage(backgroundUrl, false, null, NetworkedCacheableImageView.BLUR);
        } catch (Exception e) {
            // empty path for some reason
        }

        try {
            Picasso.with(context)
                    .load(profilePicUrl)
                    .transform(new CircleTransform())
                    .into(profilePic);
            //backgroundPic.loadImage(profilePicUrl, false, null, NetworkedCacheableImageView.CIRCLE);
        } catch (Exception e) {
            // empty path again
        }

        MainDrawerArrayAdapter adapter = new MainDrawerArrayAdapter(context, new ArrayList<String>(Arrays.asList(MainDrawerArrayAdapter.getItems(context))));
        drawerList.setAdapter(adapter);

        drawerList.setOnItemClickListener(new MainDrawerClickListener(context, mDrawerLayout, mViewPager));

    }

    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack);
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void logoutFromTwitter() {
        // Clear the shared preferences
        SharedPreferences.Editor e = sharedPrefs.edit();
        e.remove("authentication_token");
        e.remove("authentication_token_secret");
        e.remove("is_logged_in");
        e.commit();

        HomeDataSource homeSources = new HomeDataSource(context);
        homeSources.open();
        homeSources.deleteAllTweets();
        homeSources.close();

        MentionsDataSource mentionsSources = new MentionsDataSource(context);
        mentionsSources.open();
        mentionsSources.deleteAllTweets();
        mentionsSources.close();

        DMDataSource dmSource = new DMDataSource(context);
        dmSource.open();
        dmSource.deleteAllTweets();
        dmSource.close();

        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
        suggestions.clearHistory();

        Intent login = new Intent(context, LoginActivity.class);
        startActivity(login);
    }

    @Override
    protected void onDestroy() {
        Crouton.cancelAllCroutons();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // cancels the notifications when the app is opened
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        // for testing
        /*RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
        Intent popup = new Intent(context, MainActivityPopup.class);
        popup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("from_notification", true);
        PendingIntent popupPending =
                PendingIntent.getActivity(
                        this,
                        0,
                        popup,
                        0
                );
        remoteView.setOnClickPendingIntent(R.id.popup_button, popupPending);
        remoteView.setTextViewText(R.id.content, "test");

        remoteView.setImageViewResource(R.id.icon, R.drawable.timeline_dark);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_action_accept_dark)
                        .setContent(remoteView);
        //.setContentTitle(getResources().getString(R.string.app_name))
        //.setContentText(numberNew + " new tweets");

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("from_notification", true);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        0
                );

        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(4, mBuilder.build());*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public static final int SETTINGS_RESULT = 101;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_compose:
                Intent compose = new Intent(context, ComposeActivity.class);
                startActivity(compose);
                return true;

            case R.id.menu_direct_message:
                Intent dm = new Intent(context, ComposeDMActivity.class);
                startActivity(dm);
                return true;

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsPagerActivity.class);
                startActivityForResult(settings, SETTINGS_RESULT);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*@Override
    public void onBackPressed() {
        finish();
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        recreate();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }
}
