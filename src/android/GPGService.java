package com.ludei.googleplaygames;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.icu.text.LocaleDisplayNames;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
//import android.support.annotation.NonNull;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.FriendsResolutionRequiredException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerBuffer;
import com.google.android.gms.games.Players;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.achievement.Achievements;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.Leaderboards;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotContents;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.android.gms.games.stats.PlayerStats;
import com.google.android.gms.games.stats.Stats;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.plyerplugin.android.aacom.R;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executor;

import static com.google.android.gms.games.Games.getPlayerStatsClient;

public class GPGService  {


    private static final int PAGE_SIZE = 101 ;
    private static final Object SHOW_SHARING_FRIENDS_CONSENT = 102;

    public class Session {

        public String accessToken;
        public String playerId;
        public String playerAlias;
        public ArrayList<String> scopes;
        public long expirationDate;


        public Session(String token, ArrayList<String> scopes, Player p, long expirationDate)
        {
            this.accessToken = token;
            this.scopes = scopes;
            if (p != null) {
                this.playerId = p.getPlayerId();
                this.playerAlias = p.getDisplayName();
            }
            this.expirationDate = expirationDate;
        }

        public HashMap<String, Object> toMap()
        {
            HashMap<String,Object> dic = new HashMap<String, Object>();
            dic.put("access_token", accessToken != null ? accessToken : "");
            String scope = GPUtils.scopeArrayToString(scopes);
            dic.put("state", scope);
            dic.put("playerId", playerId != null ? playerId : "");
            dic.put("playerAlias", playerAlias != null ? playerAlias : "");
            dic.put("expirationDate", expirationDate);
            return dic;
        }
    }
    public static class Error {
        public String message;
        public int code;

        public Error(String msg, int code) {
            this.message = msg;
            this.code = code;
        }

        public HashMap<String, Object> toMap()
        {
            HashMap<String,Object> dic = new HashMap<String, Object>();
            dic.put("code", code);
            dic.put("message", message);
            return dic;
        }
    }

    public class GPGPlayer {
        public String playerId;
        public String playerAlias;
        public long lastPlayed;

        public GPGPlayer(Player player) {
            if (player != null) {
                playerId = player.getPlayerId();
                playerAlias = player.getDisplayName();
                lastPlayed = player.getLastPlayedWithTimestamp();
            }
        }

        public HashMap<String, Object> toMap()
        {
            HashMap<String,Object> dic = new HashMap<String, Object>();
            dic.put("playerId", playerId != null ? playerId : "");
            dic.put("playerAlias", playerAlias != null ? playerAlias : "");
            dic.put("lastPlayed", lastPlayed);
            return dic;
        }
    }

    public class GPGAchievement {
        public String title;
        public String description;
        public String identifier;
        public boolean unlocked;

        GPGAchievement(Achievement ach) {
            if (ach != null) {
                this.title = ach.getName();
                this.description = ach.getDescription();
                this.identifier = ach.getAchievementId();
                unlocked = ach.getState() == Achievement.STATE_UNLOCKED;
            }
        }

        public HashMap<String, Object> toMap()
        {
            HashMap<String,Object> dic = new HashMap<String, Object>();
            dic.put("title", title != null ? title : "");
            dic.put("description", description != null ? description : "");
            dic.put("identifier", identifier != null ? identifier : "");
            dic.put("unlocked", unlocked);
            return dic;
        }
    }

    public static class GameSnapshot {
        public String identifier;
        public String title;
        public String description;
        public byte[] bytes;

        public HashMap<String, Object> toMap()
        {
            HashMap<String,Object> dic = new HashMap<String, Object>();
            dic.put("identifier", identifier != null ? identifier : "");
            dic.put("description", description != null ? description : "");
            try {
                if (bytes != null) {
                    dic.put("data", new String(bytes, "UTF-8"));
                }
            }catch (Exception ex) {
                dic.put("data", new String(bytes));
            }
            //dic.put("data", data); TODO
            return dic;
        }

        public static GameSnapshot fromJSONObject(JSONObject obj) {
            GameSnapshot data = new GameSnapshot();
            data.identifier = obj.optString("identifier");
            data.description = obj.optString("description");
            String strData = obj.optString("data");
            if (strData != null && strData.length() > 0) {
                try {
                    data.bytes = strData.getBytes(StandardCharsets.UTF_8);
                }
                catch (Exception ex) {
                    data.bytes = strData.getBytes();
                }
            }
            //data.data = obj.optString("data"); TODO
            return data;
        }

        public static GameSnapshot fromMetadata(SnapshotMetadata metadada, byte[] bytes) {
            GameSnapshot data = new GameSnapshot();
            data.identifier = metadada.getUniqueName();
            data.description = metadada.getDescription();
            data.bytes = bytes;
            return data;
        }
    }

    public static class ShareData {
        public String url;
        public String message;
    }

    public interface SessionCallback {
        void onComplete(Session session, Error error);
    }

    public interface CompletionCallback {
        void onComplete(Error error);
    }


    public interface LoadPlayerCallback {
        void onComplete(GPGPlayer player, Error error);
    }

    public interface LoadScoreCallback {
        void onComplete(long score, Error error);
    }
 // JSONObject      GameSnapshot
    public interface SavedGameCallback {
        void onComplete(JSONObject data, Error error);
    }

    public interface AchievementsCallback {
        void onComplete(ArrayList<GPGAchievement> achievements, Error error);
    }


    public interface RequestCallback {
        void onComplete(JSONObject responseJSON, Error error);
    }

    public interface WillStartActivityCallback {
        void onWillStartActivity();
    }


    private static final int GP_DIALOG_REQUEST_CODE = 0x000000000001112;
    private static final int RESOLUTION_REQUEST_CODE = 0x00000000001113;
    private static final int GP_ERROR_DIALOG_REQUEST_CODE = 0x00000000001114;
    private static final int GP_SAVED_GAMES_REQUEST_CODE = 0x000000000001117;
    private static final int ACHIVEMENT_SHOW = 101;
    private static final int SHOW_LEADErBOARD = 102; // showLeaderboard
    private static final int RC_VIDEO_OVERLAY = 9011;
    private static final String GP_SIGNED_IN_PREFERENCE = "gp_signedIn";

    private static final int RC_SIGN_IN = 10;

    private static GPGService sharedInstance = null;

    //static values used to communicate with Ludei's Google Play Multiplayer Service
    public static Runnable onConnectedCallback;
    public static Invitation multiplayerInvitation;
    public static GoogleSignInClient getGoogleAPIClient() {
        return sharedInstance != null? sharedInstance.mGoogleSignInClient : null;
    }
    public static GPGService currentInstance() {
        return sharedInstance;
    }

    protected Activity activity;
  //  protected GoogleApiClient client;

    GoogleSignInClient mGoogleSignInClient;
    GoogleSignInAccount signedInAccount;
    private SnapshotsClient mSnapshotsClient = null;

    protected static final String[] defaultScopes = new String[]{Scopes.GAMES, Scopes.PLUS_LOGIN};
    protected ArrayList<String> scopes = new ArrayList<String>();
    protected CompletionCallback intentCallback;
    protected SavedGameCallback intentSavedGameCallback;
    protected ArrayList<SessionCallback> loginCallbacks = new ArrayList<SessionCallback>();
    protected String authToken;
    protected SessionCallback sessionListener;
    protected WillStartActivityCallback willStartListener;
    protected boolean trySilentAuthentication = true;
    protected Executor executor;
    protected Player me;
    protected Runnable errorDialogCallback;
    protected Runnable requestPermission = null;
    protected ImageManager imageManager = null;
    private EventsClient mEventsClient;
    private String mCurrentSaveName = "snapshotTemp";

    public GPGService(Activity activity)
    {
        sharedInstance = this;
        this.activity = activity;
        this.trySilentAuthentication = this.activity.getPreferences(Activity.MODE_PRIVATE).getBoolean(GP_SIGNED_IN_PREFERENCE, true);
    }

    public void init() {
        this.init(null);
    }
    public void init(String[] extraScopes)
    {
        for (String scope : defaultScopes) {
            this.scopes.add(scope);
        }
        if (extraScopes != null) {
            for (String scope : extraScopes) {
                String value = GPUtils.mapScope(scope);
                if (!this.scopes.contains(value)) {
                    this.scopes.add(value);
                }
            }
        }
       // if (this.isAvailable()) {
         Log.d("df", "Own Logey3773737733  ");
            this.createClient();
            if (this.trySilentAuthentication) {
                Log.d("df", " try silentium !  ");
              signInSilently();
            }
      //  }
    }

    public boolean isAvailable() {
        return  GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this.activity) == ConnectionResult.SUCCESS;
    }

    public void destroy()
    {
        this.activity = null;
        sharedInstance = null;
    }

    public void setRequestPermission(Runnable task) {
        requestPermission = task;
    }

    public void setSessionListener(SessionCallback listener)
    {
        this.sessionListener = listener;
    }

    public void setWillStartActivityListener(WillStartActivityCallback listener)
    {
        this.willStartListener = listener;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public boolean handleActivityResult(final int requestCode, final int resultCode, final Intent intent) {

           boolean managed = false;
            Log.d("df", "Los Handle Activity ");


        if (requestCode == RC_SIGN_IN) { // was resultCode

               Log.d("df", "Los RC in handle active ");
                if (signedInAccount == null) {  // was signedIn not account
                    Log.d("df", "Los RC in handle active 0023002 ");
                    Task<GoogleSignInAccount> task =
                            GoogleSignIn.getSignedInAccountFromIntent(intent);
                 Log.d("df", " handle Active Result aaaa  002309290 ");
                    try {
                         signedInAccount = task.getResult(ApiException.class);
                        Log.d("df", "Los RC in  PRE OnConnect ");
                        onConnected(signedInAccount);

                    } catch (ApiException apiException) {
                        String message = apiException.getMessage();
                        if (message == null || message.isEmpty()) {
                            //message = getString(R.string.signin_other_error);
                            Log.d("df", "error my LOSSS SIGN == " + message);
                        }
                    }
                    this.createClient();
                }
                // mGoogleSignInClient.connect();

        }else if (requestCode == ACHIVEMENT_SHOW){

        }else if (requestCode == SHOW_LEADErBOARD){

        } else if (requestCode == GP_SAVED_GAMES_REQUEST_CODE) {
            if (intent != null) {
                if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    SnapshotMetadata snapshotMetadata =
                            intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
                    mCurrentSaveName = snapshotMetadata.getUniqueName();
                    Log.d("df", "Snapshot name ! first " + mCurrentSaveName);
                    // Load the game data from the Snapshot
                    // ...

                      loadingSnapshot(snapshotMetadata);


//                    SnapshotMetadata meta = snapshot.getMetadata();
//                    SnapshotContents contents = snapshot.getSnapshotContents();
//                    if (meta != null && contents != null) {
//                        GameSnapshot data = GameSnapshot.fromMetadata(meta, contents.readFully());
//                        return data;
//                    }

                    //



                } else if (intent.hasExtra(SnapshotsClient.EXTRA_SNAPSHOT_NEW)) {
                    // Create a new snapshot named with a unique string
                    String unique = new BigInteger(281, new Random()).toString(13);
                    mCurrentSaveName = "snapshotTemp-" + unique;
                    Log.d("df", "Snapshot name ! " + mCurrentSaveName);

                    // Create the new snapshot
                    // ...
                }
            }

        } else if (requestCode == RC_VIDEO_OVERLAY) {
            Log.d("dsf", " My request Code is null ");
        }

//        if ((requestCode == RESOLUTION_REQUEST_CODE || requestCode == GP_DIALOG_REQUEST_CODE) &&
//                resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
//
//            //User signed out from the achievements/leaderboards settings in the upper right corner
//            this.logout(null);
//            if (intentCallback != null) {
//                intentCallback.onComplete(new Error("User signed out",  GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED));
//                intentCallback = null;
//            }
//            managed = true;
//
//        }
//        else if (requestCode == RESOLUTION_REQUEST_CODE) {
//
//
//        }
//        else if (requestCode == GP_DIALOG_REQUEST_CODE) {
//
//            if (intentCallback != null) {
//                Error error = resultCode != Activity.RESULT_OK ? null : new Error("resultCode: " + resultCode, resultCode);
//                intentCallback.onComplete(error);
//                intentCallback = null;
//            }
//            managed = true;
//
//        }
//        else if (requestCode == GP_ERROR_DIALOG_REQUEST_CODE) {
//            if (errorDialogCallback != null) {
//                errorDialogCallback.run();
//            }
//        }
//        else if (requestCode == GP_SAVED_GAMES_REQUEST_CODE && intentSavedGameCallback != null) {
//            if (resultCode == Activity.RESULT_CANCELED) {
//                intentSavedGameCallback.onComplete(null, null);
//            }
//            else if (resultCode != Activity.RESULT_OK) {
//                intentSavedGameCallback.onComplete(null, new Error("resultCode: " + resultCode, resultCode));
//            }
//            if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_METADATA)) {
//                // Load a snapshot.
//                SnapshotMetadata snapshotMetadata = (SnapshotMetadata)intent.getParcelableExtra(Snapshots.EXTRA_SNAPSHOT_METADATA);
//                GameSnapshot snapshot = GameSnapshot.fromMetadata(snapshotMetadata, null);
//                intentSavedGameCallback.onComplete(snapshot, null);
//            }
//            else if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_NEW)) {
//                intentSavedGameCallback.onComplete(new GameSnapshot(), null);
//            }
//            intentSavedGameCallback = null;
//        }else if (resultCode == RC_SIGN_IN)
//        {
//            if (resultCode == Activity.RESULT_OK) {
//                if (mGoogleSignInClient == null) {
//
//                    Task<GoogleSignInAccount> task =
//                            GoogleSignIn.getSignedInAccountFromIntent(intent);
//
//                    try {
//                        GoogleSignInAccount account = task.getResult(ApiException.class);
//                        onConnected(account);
//                    } catch (ApiException apiException) {
//                        String message = apiException.getMessage();
//                        if (message == null || message.isEmpty()) {
//                            //message = getString(R.string.signin_other_error);
//                        }
//                    }
//
//                    this.createClient();
//                }
//                // mGoogleSignInClient.connect();
//            }
//            else {
//                String errorString = resultCode == Activity.RESULT_CANCELED ? null : GPUtils.activityResponseCodeToString(resultCode);
//                processSessionChange(null, resultCode, errorString);
//            }
//            managed = true;
//        }
        return managed;
    }



//    void   SnapshotMetadata snapshotMetadata =
//            intent.getParcelableExtra(SnapshotsClient.EXTRA_SNAPSHOT_METADATA);
//    mCurrentSaveName = snapshotMetadata.getUniqueName();
//                    Log.d("df", "Snapshot name ! first " + mCurrentSaveName);
//    // Load the game data from the Snapshot
//    // ...


    private void readdAllData(byte[] data) throws IOException, JSONException {

         // public SaveGame(byte[] data) {
         ///   if (data == null) return; // default progress
            //loadFromJson(new String(data));
         // Error
            Error error = new Error("error to return data", 2);
            String a = new String(data);
            JSONObject onf = new JSONObject((a));
            String formatlosString = onf.getString("desc");
        Log.d("df", " my FINALLLL == " + formatlosString);
        intentSavedGameCallback.onComplete(onf, error);
        //}
    }

    void loadingSnapshot(SnapshotMetadata snapshotMetadata)
    {

          waitForCloseAndOpen(snapshotMetadata).addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
              @Override
              public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> result) {

                  Snapshot snapLoading = result.getData();
                      if(snapLoading == null ){
                    Log.d("df", " Conflicted snapshot loading ! \n");
                      }else{
                          //read my snapshot
                            Log.d("df", " ok in snap my okkkk !!  ");


                          SnapshotContents contents = snapLoading.getSnapshotContents();
                          if (snapshotMetadata != null && contents != null) {

                          }
                              //GameSnapshot data = GameSnapshot.fromMetadata(snapshotMetadata, contents.readFully());
                          try {
                              readdAllData(contents.readFully());
                          } catch (IOException | JSONException e) {
                              e.printStackTrace();
                          }


                      }

              }
          });

    }


    private Task<SnapshotsClient.DataOrConflict<Snapshot>> waitForCloseAndOpen(final SnapshotMetadata snapshotMetadata){

        final boolean useMetadata = snapshotMetadata != null && snapshotMetadata.getUniqueName() != null;
        if (useMetadata) {
            Log.i("TAG", "Opening snapshot using metadata: " + snapshotMetadata);
        } else {
            Log.i("TAG", "Opening snapshot using currentSaveName: " + mCurrentSaveName);
        }

        final String filename = useMetadata ? snapshotMetadata.getUniqueName() : mCurrentSaveName;

        Task<SnapshotsClient.DataOrConflict<Snapshot>> openTask = useMetadata ? mSnapshotsClient.open(snapshotMetadata).addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
            @Override
            public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
            // Snapshot snap =  Objects.requireNonNull(task.getResult()).getData();
               Log.d("df", " SNAPREADING this 0001  ");
            }
        }) : mSnapshotsClient.open(mCurrentSaveName, true, 1).addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
            @Override
            public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
                Log.d("df", " SNAPREADING this 0002  ");
            }
        });

            return openTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                  Log.d("df", " onFailure to get snapshot data loading ");
                }
            });

    }



    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d("TAG", "onConnected(): connected to Google APIs");

        signedInAccount = googleSignInAccount;

        mEventsClient = Games.getEventsClient(this.activity, signedInAccount);

        mSnapshotsClient = Games.getSnapshotsClient(this.activity,  signedInAccount);

        Games.getPlayersClient(this.activity, googleSignInAccount)
                .getCurrentPlayer()
                .addOnSuccessListener(
                        new OnSuccessListener<Player>() {
                            @Override
                            public void onSuccess(Player player) {
                               // mDisplayName = player.getDisplayName();
                               // mPlayerId = player.getPlayerId();

                                me = player;

                           Log.d("df ", " my names == " + player.getDisplayName());
                                Log.d("df", " my id == "+ player.getPlayerId());
                               // mEventsClient.load(true);
                                //setViewVisibility();
                            }
                        }
                )
                .addOnFailureListener(createFailureListener("There was a problem getting the player!"));

        Log.d("TAG", "onConnected(): Connection successful");

        // Retrieve the TurnBasedMatch from the connectionHint
        GamesClient gamesClient = Games.getGamesClient(this.activity, googleSignInAccount);
        gamesClient.getActivationHint()
                .addOnSuccessListener(new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle hint) {
                        if (hint != null) {
                          //  TurnBasedMatch match = hint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);

                          //  if (match != null) {
                                //updateMatch(match);
                           // }
                        }
                    }
                })
                .addOnFailureListener(createFailureListener(
                        "There was a problem getting the activation hint!"));
    }


    private OnFailureListener createFailureListener(final String string) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                handleException(e, string);
            }
        };
    }


    private void handleException(Exception exception, String details) {
        int status = 0;

        if (exception instanceof TurnBasedMultiplayerClient.MatchOutOfDateApiException) {
            TurnBasedMultiplayerClient.MatchOutOfDateApiException matchOutOfDateApiException =
                    (TurnBasedMultiplayerClient.MatchOutOfDateApiException) exception;

            new AlertDialog.Builder(this.activity)
                    .setMessage("Match was out of date, updating with latest match data...")
                    .setNeutralButton(android.R.string.ok, null)
                    .show();

            TurnBasedMatch match = matchOutOfDateApiException.getMatch();
            //updateMatch(match);

            return;
        }

        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            status = apiException.getStatusCode();
        }

//        if (!checkStatusCode(status)) {
//            return;
//        }

//        String message = getString(R.string.status_exception_error, details, status, exception);
//
//        new AlertDialog.Builder(this.activity)
//                .setMessage(message)
//                .setNeutralButton(android.R.string.ok, null)
//                .show();
    }

    private void createClient()
    {
         Log.d("df", "create Client 000 1 ");
        if (mGoogleSignInClient != null) {
          //  client.unregisterConnectionCallbacks(this);
          //  client.unregisterConnectionFailedListener(this);
            Log.d("df", "create Client 000 2 ");
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            activity.startActivityForResult(signInIntent, RC_SIGN_IN);

        }

          Log.d("df", "Los CreateClient 555666 ");
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestScopes(Drive.SCOPE_APPFOLDER)
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
        Log.d("df", "Los CreateClient 8588399393 ");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        Log.d("df", " create own CLIENT !");
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    private void processSessionChange(String authToken, int errorCode, String errorMessage)
    {
        this.authToken = authToken;



        Session session = authToken != null ? new Session(authToken, scopes, getMe(), 0) : null;
        Error error = errorMessage != null ? new Error(errorMessage, errorCode) : null;

        if (this.sessionListener != null) {
            this.sessionListener.onComplete(session, error);
        }


        ArrayList<SessionCallback> callbacks;
        synchronized (loginCallbacks) {
            callbacks = new ArrayList<SessionCallback>(loginCallbacks);
            loginCallbacks.clear();
        }

        for (SessionCallback cb: callbacks) {
            cb.onComplete(session, error);
        }
    }


    private void signInSilently() {

        GoogleSignInOptions signInOption =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestScopes(Drive.SCOPE_APPFOLDER)
                        .build();

        GoogleSignInClient signInClient = GoogleSignIn.getClient(activity, signInOption);
        signInClient.silentSignIn().addOnCompleteListener(activity,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            onConnected(task.getResult());
                            Log.d("sdf", " onComplete LOS  !! \n");

                           // signedInAccount = task.getSignInAccount();
                           // me = Games.Players.getCurrentPlayer(client);
                           // String token = me.getPlayerId();
                           // return token;


//                            mGoogleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
//
//                            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//                            activity.startActivityForResult(signInIntent, RC_SIGN_IN);


                        } else {
                            // Player will need to sign-in explicitly using via UI
                            Log.d("sdf", " not complete 23424 LOS  !! \n");
                            String sd = Objects.requireNonNull(task.getException()).getMessage();
                            Log.d("df",  " my message tasking == "+ sd);
                        }
                    }
                });
    }


    public boolean isLoggedIn()
    {
       // return mGoogleSignInClient != null && mGoogleSignInClient.isConnected() && authToken != null;
     return mGoogleSignInClient != null;
    }

    private Player getMe() {
        if (me != null) {
            return me;
        }
        if (isLoggedIn()) {
            try {
                //me = Games.Players.getCurrentPlayer(mGoogleSignInClient);

                Games.getPlayersClient(this.activity, signedInAccount)
                        .getCurrentPlayer()
                        .addOnSuccessListener(
                                new OnSuccessListener<Player>() {
                                    @Override
                                    public void onSuccess(Player player) {
                                       // mDisplayName = player.getDisplayName();
                                      //  mPlayerId = player.getPlayerId();

                                       // setViewVisibility();
                                    }
                                }
                        )
                        .addOnFailureListener(createFailureListener("There was a problem getting the player!"));



            } catch(IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public GPGService.Session getSession() {

        if (authToken != null && mGoogleSignInClient != null) {
            return new GPGService.Session(authToken, scopes, getMe(), 0);
        }
        return null;
    }

    public void login(final String[] userScopes, final SessionCallback callback)
    {

        final int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this.activity);
        if (errorCode != ConnectionResult.SUCCESS) {

            if (GoogleApiAvailability.getInstance().isUserResolvableError(errorCode)) {
                errorDialogCallback = new Runnable() {
                    @Override
                    public void run() {
                        if (isAvailable()) {
                            login(userScopes, callback);
                        }
                        else if (callback != null) {
                            callback.onComplete(null, null);
                        }
                    }
                };
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(activity, errorCode, GP_ERROR_DIALOG_REQUEST_CODE);
                        dialog.setCancelable(true);
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                errorDialogCallback = null;
                                if (callback != null) {
                                    callback.onComplete(null, null);
                                }
                            }
                        });
                        dialog.show();
                    }
                });

            }
            else if (callback != null) {
                callback.onComplete(null, new Error(GPUtils.errorCodeToString(errorCode), errorCode));
            }
            return;
        }

        if (this.isLoggedIn()) {
            if (callback != null) {
                callback.onComplete(this.getSession(), null);
            }
            return;
        }

        synchronized (loginCallbacks) {
            this.loginCallbacks.add(callback);
        }

//        if (this.mGoogleSignInClient != null && mGoogleSignInClient.isConnecting()) {
//            return; //already connecting
//        }

        //check if a user wants a scope that it not already set in the GameClient

        /*boolean recreateClient = false;
        if (userScopes != null) {
            for (String scope: userScopes) {
                String value = GPUtils.mapScope(scope);
                if (!this.scopes.contains(value)) {
                    this.scopes.add(value);
                    recreateClient = true;
                }

            }
        }*/

        if (mGoogleSignInClient == null) { // || recreateClient) {
            this.createClient();
        }

        //client.connect();
    }

    public void logout(CompletionCallback callback) {

           mGoogleSignInClient.signOut();
    }

    public void showLeaderboard(String leaderboard, CompletionCallback callback)
    {
//        if (intentCallback != null) {
//            if (callback != null) {
//                callback.onComplete(new Error("Intent already running", 0));
//            }
//            return;
//        }
         Log.d("df", "show leaderBorer in This 01  my leaderB " + leaderboard);
        try {
            intentCallback = callback;
           // notifyWillStart();
            Log.d("df", "show leaderBorer in This 02 ");
            if (leaderboard == null || leaderboard.length() == 0) {
              //  activity.startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(client), GP_DIALOG_REQUEST_CODE);
                Log.d("df", "show leaderBorer in This 03 ");
                assert leaderboard != null;
                Games.getLeaderboardsClient(this.activity, signedInAccount) // GoogleSignIn.getLastSignedInAccount(this)
                        .getLeaderboardIntent(leaderboard)
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                Log.d("df", "show leaderBorer in This success 01  ");
                               activity.startActivityForResult(intent, SHOW_LEADErBOARD);
                            }
                        });


            } else {
                Log.d("df", "show leaderBorer in EMPTY ");
                assert leaderboard != null;
                Games.getLeaderboardsClient(this.activity, signedInAccount) // GoogleSignIn.getLastSignedInAccount(this)
                        .getLeaderboardIntent(leaderboard)
                        .addOnSuccessListener(new OnSuccessListener<Intent>() {
                            @Override
                            public void onSuccess(Intent intent) {
                                Log.d("df", "show leaderBorer in This success 02 ");
                                activity.startActivityForResult(intent, SHOW_LEADErBOARD);
                            }
                        });

               // activity.startActivityForResult(Games.Leaderboards.getLeaderboardIntent(client, leaderboard), GP_DIALOG_REQUEST_CODE);
            }
        }
        catch (Exception ex) {
            if (callback != null) {
                callback.onComplete(new Error(ex.toString(), 0));
            }
        }

    }


    public void loadAchievements(final AchievementsCallback callback)
    {
//        if (!isLoggedIn()) {
//            if (callback != null) {
//                callback.onComplete(null, new Error("User is not logged into Google Play Game Services", 0));
//            }
//            return;
//        }

        try {

          //  Games.getAchievementsClient(this.activity, signedInAccount)
           //         .unlock(getString(R.string.my_achievement_id));


            Games.getAchievementsClient(this.activity, signedInAccount).load(true).addOnSuccessListener(
                    new OnSuccessListener<AnnotatedData<AchievementBuffer>>() {
                        @Override
                        public void onSuccess(AnnotatedData<AchievementBuffer> achievementBufferAnnotatedData) {

                            Log.d("df", " we are thising ");
                            AchievementBuffer buffer = achievementBufferAnnotatedData.get();
                            Iterator<Achievement> it = buffer.iterator();

                            ArrayList<GPGAchievement> data = new ArrayList<GPGAchievement>();

                            while (it.hasNext()) {
                                data.add(new GPGAchievement(it.next()));
                            }
                            buffer.close();
                            callback.onComplete(data, null);
                        }
                    }
            );

//            Games.Achievements.load(client, false).setResultCallback(new ResultCallback<Achievements.LoadAchievementsResult>() {
//                @Override
//                public void onResult(Achievements.LoadAchievementsResult result) {
//                    if (callback == null) {
//                        return;
//                    }
//                    if (result.getStatus().isSuccess()) {
//                        AchievementBuffer buffer = result.getAchievements();
//                        Iterator<Achievement> it = buffer.iterator();
//
//                        ArrayList<GPGAchievement> data = new ArrayList<GPGAchievement>();
//
//                        while (it.hasNext()) {
//                            data.add(new GPGAchievement(it.next()));
//                        }
//                        buffer.close();
//                        callback.onComplete(data, null);
//                    } else {
//                        int code = result.getStatus().getStatusCode();
//                        callback.onComplete(null, new Error("Code: " + code, code));
//                    }
//                }
//            });
//        }
//        catch (Exception ex) {
//            if (callback != null) {
//                callback.onComplete(null, new Error(ex.toString(), 0));
//            }
        } catch (Exception e)
        {

        }


    }

    public void showAchievements(CompletionCallback callback)
    {
         Log.d("df"," show Achievement 01222");
//        if (intentCallback != null) {
//            if (callback != null) {
//                callback.onComplete(new Error("Intent already running", 0));
//            }
//            return;
//        }

        try {
            intentCallback = callback;
            notifyWillStart();
            Log.d("df"," show Achievement 02323322");
            // GoogleSignIn.getLastSignedInAccount(this)    signedInAccount
             Games.getAchievementsClient(this.activity, signedInAccount)
                     .getAchievementsIntent()
                     .addOnSuccessListener(new OnSuccessListener<Intent>() {
                @Override
                public void onSuccess(Intent intent) {
                    Log.d("df"," show Achievement success ");
                    activity.startActivityForResult(intent, ACHIVEMENT_SHOW);
                }
            }).addOnFailureListener(new OnFailureListener() {
                 @Override
                 public void onFailure(@NonNull Exception e) {
                     Log.d(" df", "Achievement FAILURE to " + e.getMessage());
                 }
             });
//           wer.addOnSuccessListener(new OnSuccessListener<Intent>() {
//               @Override
//               public void onSuccess(Intent intent) {
//                   activity.startActivityForResult(intent, GP_DIALOG_REQUEST_CODE);
//               }
//           });
           // activity.startActivityForResult(Games.Achievements.getAchievementsIntent(client), GP_DIALOG_REQUEST_CODE);
        }
        catch (Exception ex) {
             Log.d("df", "Achievement exception !!!" + ex.getMessage()); // !!!GoogleSignInAccount must not be null
            if (callback != null) {

                callback.onComplete(new Error(ex.toString(), 0));
            }
        }
    }

    public void showSavedGames(SavedGameCallback callback) {
        Log.d("df", "in SHHOOOOWW this ");
//        if (intentCallback != null) {
//            if (callback != null) {
//                callback.onComplete(null, new Error("Intent already running", 0));
//            }
//            return;
//        }

        intentSavedGameCallback = callback;
        notifyWillStart();

        int maxNumberOfSavedGamesToShow = 5;
       Task<Intent> savedGamesIntent = Games.getSnapshotsClient(this.activity, signedInAccount).getSelectSnapshotIntent("Saved Games", true, true, maxNumberOfSavedGamesToShow);

         savedGamesIntent.addOnSuccessListener(new OnSuccessListener<Intent>() {
             @Override
             public void onSuccess(Intent intent) {
                 activity.startActivityForResult(intent, GP_SAVED_GAMES_REQUEST_CODE);
             }
         });

        //Intent savedGamesIntent = Games.Snapshots.getSelectSnapshotIntent(this.client, "Saved Games", true, true, maxNumberOfSavedGamesToShow);

    }

    private static final int MAX_SNAPSHOT_RESOLVE_RETRIES = 3;


    private void processFinalSnapshot(final Snapshot snapshot, final GPGService.Error error, final SavedGameCallback callback) {

        if (callback == null) {
            return;
        }

        if (snapshot != null) {
            try {
                AsyncTask<Void, Void, Object> task = new AsyncTask<Void, Void, Object>() {
                    @Override
                    protected Object doInBackground(Void... params) {
                        try
                        {
                            SnapshotMetadata meta = snapshot.getMetadata();
                            SnapshotContents contents = snapshot.getSnapshotContents();
                            if (meta != null && contents != null) {
                                GameSnapshot data = GameSnapshot.fromMetadata(meta, contents.readFully());
                                return data;
                            }
                            else {
                                return null; //no snapshot
                            }

                        }
                        catch (Exception ex)
                        {
                            return new Error(ex.toString(), 0);
                        }
                    }

                    @Override
                    protected void onPostExecute(Object info) {
                        try {
                            if (info == null) {
                                callback.onComplete(null, new Error("Empty snapshot", 0));
                            }
                            else if (info instanceof GameSnapshot) {
                              //  callback.onComplete((GameSnapshot) info, null);
                            }
                            else {
                                callback.onComplete(null, (Error)info);
                            }
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                };

                if (this.executor != null) {
                    task.executeOnExecutor(executor);
                }
                else  {
                    task.execute();
                }

            }
            catch (Exception e) {
                callback.onComplete(null, new Error(e.toString(), 0));
            }
        }
        else {
            callback.onComplete(null, error);
        }


    }

    private void processSnapshotOpenResult(final Snapshots.OpenSnapshotResult result, final SavedGameCallback callback, final int retryCount) {


        int status = result.getStatus().getStatusCode();

        if (status == GamesStatusCodes.STATUS_OK) {
            processFinalSnapshot(result.getSnapshot(), null, callback);
        }
        else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {

            if (retryCount > MAX_SNAPSHOT_RESOLVE_RETRIES) {
                // Failed, log error and show Toast to the user
                processFinalSnapshot(result.getSnapshot(), new Error("Could not resolve snapshot conflicts", 1), callback);
                return;
            }
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Snapshot snapshot = result.getSnapshot();
                    Snapshot conflictSnapshot = result.getConflictingSnapshot();
                    byte[] data = null;
                    byte[] conflictData = null;
                    try {
                        data = snapshot.getSnapshotContents().readFully();
                        conflictData = snapshot.getSnapshotContents().readFully();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    Snapshot resolvedSnapshot = snapshot;

                    if (data != null && conflictData != null && data.length != conflictData.length) {
                        // Resolve between conflicts by selecting the snapshot with more data
                        if (conflictData.length > data.length) {
                            resolvedSnapshot = conflictSnapshot;
                        }
                    }
                    else if (snapshot.getMetadata().getLastModifiedTimestamp() < conflictSnapshot.getMetadata().getLastModifiedTimestamp()) {
                        // Resolve between conflicts by selecting the newest of the conflicting snapshots.
                        resolvedSnapshot = conflictSnapshot;
                    }


                   // Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(client, result.getConflictId(), resolvedSnapshot).await();
                    // Recursively attempt again
                  //  processSnapshotOpenResult(resolveResult, callback, retryCount + 1);
                }
            });
            thread.start();

        }
        else {
            processFinalSnapshot(result.getSnapshot(), new Error("Status: " + status, status), callback);
        }
    }




//   public void loadSavedGame (final String snapshotName, final SavedGameCallback callback)
//   {
//        Task<byte[]> dfke = loadSavedGameDatas(snapshotName, callback);
//
//        dfke.addOnSuccessListener(new OnSuccessListener<byte[]>() {
//            @Override
//            public void onSuccess(byte[] bytes) {
//                   Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(client, result.getConflictId(), resolvedSnapshot).await();
//                //                            // Recursively attempt again
//                //                            processSnapshotOpenResult(resolveResult, callback, retryCount + 1);
//
//
//            }
//        });
//   }




    public void loadSavedGame(final String snapshotName, final SavedGameCallback callback) {


        // Get the SnapshotsClient from the signed in account.
        SnapshotsClient snapshotsClient =
                Games.getSnapshotsClient(this.activity, GoogleSignIn.getLastSignedInAccount(this.activity));

        // In the case of a conflict, the most recently modified version of this snapshot will be used.
        int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

        // Open the saved game using its name.
         snapshotsClient.open(snapshotName, true, conflictResolutionPolicy)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("TAG", "Error while opening Snapshot.", e);
                        callback.onComplete(null, new Error(e.getLocalizedMessage(), 0));
                    }
                }).continueWith(new Continuation<SnapshotsClient.DataOrConflict<Snapshot>, byte[]>() {
                    @Override
                    public byte[] then(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) throws Exception {
                        Snapshot snapshot = task.getResult().getData();



                        // Opening the snapshot was a success and any conflicts have been resolved.
                        // Extract the raw data from the snapshot.
                        //   processSnapshotOpenResult(snapshot, callback, 0);

                        assert snapshot != null;

                        // processSnapshotOpenResult

                        //  processSnapshotOpenResult(result, callback, 0);

                        //  if (retryCount > MAX_SNAPSHOT_RESOLVE_RETRIES) {
                        // Failed, log error and show Toast to the user
                        processFinalSnapshot(snapshot, new Error("Could not resolve snapshot conflicts", 1), callback);
                        // return;
                        //}

                        //return snapshot.getSnapshotContents().readFully();

                        return null;
                    }
                }).addOnCompleteListener(new OnCompleteListener<byte[]>() {
                    @Override
                    public void onComplete(@NonNull Task<byte[]> task) {
                        // Dismiss progress dialog and reflect the changes in the UI when complete.
                        // ...
                    }
                });

//        try {
//            PendingResult<Snapshots.OpenSnapshotResult> pendingResult = Games.Snapshots.open(client, snapshotName, false);
//            ResultCallback<Snapshots.OpenSnapshotResult> cb =
//                    new ResultCallback<Snapshots.OpenSnapshotResult>() {
//                        @Override
//                        public void onResult(Snapshots.OpenSnapshotResult result) {
//                            processSnapshotOpenResult(result, callback, 0);
//                        }
//                    };
//            pendingResult.setResultCallback(cb);
//        }
//        catch (Exception ex) {
//            callback.onComplete(null, new Error(ex.getLocalizedMessage(), 0));
//        }
    }


    Task<SnapshotsClient.DataOrConflict<Snapshot>> getSnap(String ident){

        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this.activity, GoogleSignIn.getLastSignedInAccount(this.activity));

        // In the case of a conflict, the most recently modified version of this snapshot will be used.
        int conflictResolutionPolicy = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED;

//        final Snapshot[] oweSnap = new Snapshot[1];
//        // Open the saved game using its name.
//        snapshotsClient.open(ident, true, conflictResolutionPolicy).addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
//            @Override
//            public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> snapshotDataOrConflict) {
//                Snapshot snapshot = Objects.requireNonNull(snapshotDataOrConflict.getConflict()).getSnapshot(); //  .getResult().getData();
//
//
//                oweSnap[0] =  snapshot;
//            }
//        });

        return snapshotsClient.open(ident, true, conflictResolutionPolicy);

        //return oweSnap[0];

    }


    // was    public void  writeSavedGame(final GameSnapshot snapshotData, final CompletionCallback callback)
    public void  writeSavedGame(final JSONObject snapshotData, final String NameSnapshot, final CompletionCallback callback) {
       // final String snapshotName = snapshotData.identifier;
       // final boolean createIfMissing = true;

        final Error potentialError = new Error("", 0);

          Log.d("df", " snap loading this " + NameSnapshot);


        mSnapshotsClient.open(NameSnapshot, true).addOnSuccessListener(new OnSuccessListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
                     @Override
                     public void onSuccess(SnapshotsClient.DataOrConflict<Snapshot> snapshotDataOrConflict) {
                         if(snapshotDataOrConflict.isConflict())
                        {
                          Log.d("as", "Snap is Conflict !! ");
                             }else{
                        Log.d("df", " SNAP is  NOTTT CONFLICT ");
                             Snapshot mySnapA = snapshotDataOrConflict.getData();

                             assert mySnapA != null;
                             Log.d("df", " My datas in writing first ! ");
                             // exists snapshotData.toString().getBytes("utf-8");
                             mySnapA.getSnapshotContents().writeBytes(snapshotData.toString().getBytes());
                            // mySnapA.getSnapshotContents().writeBytes(snapshotData.bytes)

                             Log.d("df", " My datas in writing seconds ! ");
                             Date date = new Date();
                             //This method returns the time in millis
                             long timeMilli = date.getTime();
                           SnapshotMetadataChange metaLos = new SnapshotMetadataChange.Builder()
                                 //.setCoverImage()
                                   .setDescription(" first my snap loading los ")
                                .setPlayedTimeMillis(timeMilli)
                              .build();

//                              assert mySnapA != null;
                         mSnapshotsClient.commitAndClose(mySnapA, metaLos);
                             }
                       }


               }
        ).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(" df", " failure snapshot this !!! "  + e.getMessage());
            }
        });








//        Snapshot snapshot = (Snapshot) getSnap(snapshotData.identifier);
//        snapshot.getSnapshotContents().writeBytes(snapshotData.bytes);
//        // Set the data payload for the snapshot
//       // snapshot.getSnapshotContents().writeBytes(snapshotData.bytes);
//        // Create the change operation
//        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
//                //  .setCoverImage(coverImage)
//                .setDescription(snapshotData.description != null ? snapshotData.description : "")
//                .build();
//
//        SnapshotsClient snapshotsClient = Games.getSnapshotsClient(this.activity, signedInAccount);
//
//        snapshotsClient.commitAndClose(snapshot, metadataChange).addOnSuccessListener(new OnSuccessListener<SnapshotMetadata>() {
//            @Override
//            public void onSuccess(SnapshotMetadata snapshotMetadata) {
//
//                // callback
//                Log.d("df", "ok my SNAPSHOT writing !! ");
//                callback.onComplete(null);
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.d("df", "fuck write my snap ");
//                potentialError.message = "Could not open Snapshot for update.";
//                callback.onComplete(potentialError);
//            }
//        });
    }


    public void loadAvatar(Player player, String destFile, CompletionCallback callback) {
        if (player == null) {
            callback.onComplete(new Error("Player not found", 0));
            return;
        }

        if (!player.hasHiResImage()) {
            callback.onComplete(new Error("Player has not avatar", 0));
            return;
        }

        loadAvatar(player.getHiResImageUri(), destFile, callback);
    }


    private ArrayList<ImageManager.OnImageLoadedListener> holdedImageListeners = new ArrayList<ImageManager.OnImageLoadedListener>();
    public void loadAvatar(final Uri uri, final String destFile, final CompletionCallback callback) {

        if (uri == null) {
            callback.onComplete(new Error("Player has not avatar", 0));
            return;
        }

        if (Looper.myLooper() != Looper.getMainLooper()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadAvatar(uri, destFile, callback);
                }
            });
            return;
        }

        try {
            if (this.imageManager == null) {
                this.imageManager = ImageManager.create(activity);
            }



            ImageManager.OnImageLoadedListener listener = new ImageManager.OnImageLoadedListener() {
                @Override
                public void onImageLoaded(Uri uri, Drawable drawable, boolean b) {
                    if (drawable == null) {
                        callback.onComplete(new Error("Player has not avatar", 0));
                        return;
                    }

                    try {
                        int w = drawable.getIntrinsicWidth();
                        int h = drawable.getIntrinsicHeight();
                        Bitmap  bitmap = Bitmap.createBitmap( w, h, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        drawable.setBounds(0, 0, w, h);
                        drawable.draw(canvas);
                        FileOutputStream fos = new FileOutputStream(new File(destFile));
                        final BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 8);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        bos.flush();
                        bos.close();
                        fos.close();
                        callback.onComplete(null);
                    }
                    catch (Exception ex) {
                        callback.onComplete(new Error(ex.toString(), 0));
                    }
                    holdedImageListeners.remove(this);
                }
            };
            //Google Documentation: Note that you should hold a reference to the listener provided until the callback is complete.
            // For this reason, the use of anonymous implementations is discouraged."
            //I didn't do it the first time and some callbacks never reached...
            holdedImageListeners.add(listener);
            imageManager.loadImage(listener, uri);
        }
        catch (Exception ex) {
            callback.onComplete(new Error(ex.toString(), 0));
        }
    }

    public void loadAvatar(final String playerID, final String destFile, final CompletionCallback callback)
    {
        try {
            if (!isLoggedIn()) {

                if (callback != null) {
                    callback.onComplete(new Error("User is not logged into Google Play Game Services", 0));
                }
                return;
            }

            if (playerID == null || playerID.length() == 0) {
                loadAvatar(getMe(), destFile, callback);
            }
            else  {
                Task<AnnotatedData<Player>> res =  Games.getPlayersClient(this.activity, signedInAccount).loadPlayer(playerID);
                res.addOnSuccessListener(new OnSuccessListener<AnnotatedData<Player>>() {
                    @Override
                    public void onSuccess(AnnotatedData<Player> playerAnnotatedData) {
                        loadAvatar(playerAnnotatedData.get(), destFile, callback);
                    }
                });


                //                Games.Players.loadPlayer(client, playerID).setResultCallback(new ResultCallback<Players.LoadPlayersResult>() {
//                    @Override
//                    public void onResult(@NonNull Players.LoadPlayersResult loadPlayersResult) {
//                        if (loadPlayersResult.getStatus().isSuccess()) {
//                            loadAvatar(loadPlayersResult.getPlayers().get(0), destFile, callback);
//                        }
//                        else if (callback != null ) {
//                            callback.onComplete(new Error("Player not found", 0));
//                        }
//                    }
//                });
            }


        }
        catch (Exception ex) {
            if (callback != null) {
                callback.onComplete(new Error(ex.toString(), 0));
            }
        }

    }

    public void unlockAchievement(String achievementID, boolean showNotification, final CompletionCallback callback)
    {
        try {
            if (!isLoggedIn()) {

                if (callback != null) {
                    callback.onComplete(new Error("User is not logged into Google Play Game Services", 0));
                }
                return;
            }

            if (callback != null) {
                Task<Void> result = Games.getAchievementsClient(this.activity, signedInAccount).unlockImmediate(achievementID).addOnSuccessListener(new OnSuccessListener<Void>() {
                       @Override
                       public void onSuccess(Void aVoid) {

                       }
                   });
//                result.setResultCallback(new ResultCallback() {
//                    @Override
//                    public void onResult(Result result) {
//                        int statusCode = result.getStatus().getStatusCode();
//                        final Error error = statusCode == GamesStatusCodes.STATUS_OK ? null : new Error(GamesStatusCodes.getStatusString(statusCode), statusCode);
//                        callback.onComplete(error);
//                    }
//                });
            }
            else {
                //Games.Achievements.unlock(client, achievementID);

                Games.getAchievementsClient(this.activity, signedInAccount).unlock(achievementID);

            }
        }
        catch (Exception ex) {
            if (callback != null) {
                callback.onComplete(new Error(ex.toString(), 0));
            }
        }

    }

    public void loadPlayer(final String playerId, final LoadPlayerCallback callback) {
        try {
            if (!isLoggedIn()) {
                callback.onComplete(null, new Error("User is not logged into Google Play Game Services", 0));
                return;
            }

            if (playerId == null || playerId.length() == 0) {
                callback.onComplete(new GPGPlayer(getMe()), null);
                return;
            }

            Task<AnnotatedData<Player>> plae = Games.getPlayersClient(this.activity, signedInAccount).loadPlayer(playerId);

            plae.addOnSuccessListener(new OnSuccessListener<AnnotatedData<Player>>() {
                @Override
                public void onSuccess(AnnotatedData<Player> playerAnnotatedData) {
                     new GPGPlayer(playerAnnotatedData.get());
                    callback.onComplete(new GPGPlayer(playerAnnotatedData.get()), null);
                 //   callback.onComplete(new GPGPlayer(loadPlayersResult.getPlayers().get(0)), null);
                }
                 //  callback.onComplete(null, new Error("Error fetching user score", 0));
            });


//            Games.Players.loadPlayer(client, playerId).setResultCallback(new ResultCallback<Players.LoadPlayersResult>() {
//                @Override
//                public void onResult(@NonNull Players.LoadPlayersResult loadPlayersResult) {
//                    if (GamesStatusCodes.STATUS_OK == loadPlayersResult.getStatus().getStatusCode() && loadPlayersResult.getPlayers().getCount() > 0) {
//                        callback.onComplete(new GPGPlayer(loadPlayersResult.getPlayers().get(0)), null);
//                        return;
//                    }
//                    callback.onComplete(null, new Error("Error fetching user score", 0));
//                }
//            });
        }
        catch (Exception ex) {
            callback.onComplete(null, new Error(ex.getLocalizedMessage(), 0));
        }
    }

    public void loadScore(final String leaderboardID, final LoadScoreCallback callback) {

        try {
            if (!isLoggedIn()) {
                callback.onComplete(0, new Error("User is not logged into Google Play Game Services", 0));
                return;
            }

            Games.getLeaderboardsClient(this.activity, signedInAccount).loadCurrentPlayerLeaderboardScore(leaderboardID, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).addOnSuccessListener(
                    new OnSuccessListener<AnnotatedData<LeaderboardScore>>() {
                        @Override
                        public void onSuccess(AnnotatedData<LeaderboardScore> leaderboardScoreAnnotatedData) {
                          long score = 0;
                           Log.d("df", "LoadScore");
                           score = Objects.requireNonNull(leaderboardScoreAnnotatedData.get()).getRawScore();
                            callback.onComplete(score, null);
                        }
                    }
            );

//            Games.Leaderboards.loadCurrentPlayerLeaderboardScore(client, leaderboardID, LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC).setResultCallback(new ResultCallback<Leaderboards.LoadPlayerScoreResult>() {
//                @Override
//                public void onResult(final Leaderboards.LoadPlayerScoreResult scoreResult) {
//                    if (scoreResult != null) {
//                        if (GamesStatusCodes.STATUS_OK == scoreResult.getStatus().getStatusCode()) {
//                            long score = 0;
//                            if (scoreResult.getScore() != null) {
//                                score = scoreResult.getScore().getRawScore();
//                            }
//                            callback.onComplete(score, null);
//                        }
//                    }
//                    else {
//                        callback.onComplete(0, new Error("Error fetching user score", 0));
//                    }
//                }
//            });
        }
        catch (Exception ex) {
            callback.onComplete(0, new Error(ex.getLocalizedMessage(), 0));
        }
    }

    public void addScore(final long scoreToAdd, final String leaderboardID, final CompletionCallback callback) {

         Log.d("df"," addScore ! ll");
        loadScore(leaderboardID, new LoadScoreCallback() {
            @Override
            public void onComplete(long score, Error error) {
                if (error != null) {
                    if (callback != null) {
                        callback.onComplete(error);
                        return;
                    }
                }
                submitScore(score + scoreToAdd, leaderboardID, callback);
            }
        });

    }

    public void submitScore(final long score, final String leaderboardID, final CompletionCallback callback) {
        try {
            if (!isLoggedIn()) {
                if (callback != null) {
                    callback.onComplete(new Error("User is not logged into Google Play Game Services", 0));
                }
                return;
            }

            Games.getLeaderboardsClient(this.activity, signedInAccount).submitScore(leaderboardID, score);

           // Games.Leaderboards.submitScore(client, leaderboardID, score);
            if (callback != null) {
                callback.onComplete(null);
            }
        }
        catch (Exception ex) {
            if (callback != null) {
                callback.onComplete(new Error(ex.getLocalizedMessage(), 0));
            }
        }
    }

    public void shareMessage(ShareData data, CompletionCallback callback)
    {
        if (callback != null) {
            callback.onComplete(new Error("TODO", 0));
        }
    }

    public void submitEvent(String eventId, int increment) {
      //  Games.Events.increment(mGoogleSignInClient, eventId, increment);
        mEventsClient.increment(eventId, increment);
    }


    public void RequrstFriends(final RequestCallback callback){
        // TODO: Player buffer call's

//        Games.getPlayersClient(this.activity, signedInAccount)
//                .loadFriends(PAGE_SIZE, /* forceReload= */ false)
//                .addOnSuccessListener(new OnSuccessListener<AnnotatedData<PlayerBuffer>>() {
//                            @Override
//                            public void onSuccess(AnnotatedData<PlayerBuffer>  data) {
//                                PlayerBuffer playerBuffer = data.get();
//                                // ...
//                                   Log.d("df", " PLAYER BUFFER ! \n");
//                                assert playerBuffer != null;
//                                Log.d("df", " real number to games friends == "+ playerBuffer.getCount());
//                              }
//                            }) // was ;
//
//                .addOnFailureListener(
//                                    exception -> {
//
//                                if (exception instanceof FriendsResolutionRequiredException) {
//
//                                    PendingIntent pendingIntent =
//                                            ((FriendsResolutionRequiredException) task.getException()).getResolution();
//
//                                      // was parentActivity
//                                    this.activity.startIntentSenderForResult(
//                                            pendingIntent.getIntentSender(),
//                                            /* requestCode */ SHOW_SHARING_FRIENDS_CONSENT,
//                                            /* fillInIntent */ null,
//                                            /* flagsMask */ 0,
//                                            /* flagsValues */ 0,
//                                            /* extraFlags */ 0,
//                                            /* options */ null);
//                                }
//                            });

    }


    public void recordVideoL(final RequestCallback callback){

                    Games.getVideosClient(this.activity, signedInAccount)
                    .getCaptureOverlayIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                         //   startActivityForResult(intent, RC_VIDEO_OVERLAY);
                            activity.startActivityForResult(intent, RC_VIDEO_OVERLAY);
                        }
                    });
    }

    public void loadPlayerStats(final RequestCallback callback) {

        Task<AnnotatedData<PlayerStats>> fe =  Games.getPlayerStatsClient(this.activity, signedInAccount).loadPlayerStats(false).addOnSuccessListener(new OnSuccessListener<AnnotatedData<PlayerStats>>() {
             @Override
             public void onSuccess(AnnotatedData<PlayerStats> playerStatsAnnotatedData) {

                 PlayerStats stats = playerStatsAnnotatedData.get(); //  getPlayerStats();
                 JSONObject data = new JSONObject();
                 if (stats != null) {
                     try {
                          Log.d("df", " in loadPlayerStats ");
                         data.put("averageSessionLength", stats.getAverageSessionLength());
                        // data.put("churnProbability", stats.getChurnProbability());
                         data.put("daysSinceLastPlayed", stats.getDaysSinceLastPlayed());
                         //data.put("highSpenderProbability", stats.getHighSpenderProbability());
                         data.put("numberOfPurchases", stats.getNumberOfPurchases());
                         data.put("numberOfSessions", stats.getNumberOfSessions());
                         data.put("sessionPercentile", stats.getSessionPercentile());
                         data.put("spendPercentile", stats.getSpendPercentile());
                         //data.put("spendProbability", stats.getSpendProbability());
                         //data.put("totalSpendNext28Days", stats.getTotalSpendNext28Days());
                     }
                     catch (JSONException e) {
                         e.printStackTrace();
                     }
                     callback.onComplete(data, new Error("Player stats fetched successfully",  GamesStatusCodes.STATUS_OK));
                 } else {
                     callback.onComplete(null, new Error("getPlayerStats returned 'null'",  GamesStatusCodes.STATUS_INTERNAL_ERROR));
                 }

             }
         });



        // load Events my

//        Games.getEventsClient(this.activity, signedInAccount)
//                .load(true)
//                .addOnCompleteListener(new OnCompleteListener<AnnotatedData<EventBuffer>>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AnnotatedData<EventBuffer>> task) {
//                        if (task.isSuccessful()) {
//                            // Process all the events.
//                            for (Event event : task.getResult().get()) {
//                                Log.d("df", "loaded event " + event.getName());
//                                Log.d("dsf", " getEventID = " + event.getEventId());
//                                Log.d("TAG", " getValue = " + event.getValue());
//                                // event.getPlayer()
//                                Log.d("TAG", "get formatted Value = " + event.getFormattedValue());
//                                //event.getEventId()
//                            }
//                        } else {
//                            // Handle Error
//                            Exception exception = task.getException();
//                            int statusCode = CommonStatusCodes.DEVELOPER_ERROR;
//                            if (exception instanceof ApiException) {
//                                ApiException apiException = (ApiException) exception;
//                                statusCode = apiException.getStatusCode();
//                            }
//                            //  showError(statusCode);
//                            Log.d("df", " status los code == " + statusCode);
//                        }
//                    }
//                });





    }

    private void notifyWillStart()
    {
        if (this.willStartListener != null) {
            this.willStartListener.onWillStartActivity();
        }
    }

}
