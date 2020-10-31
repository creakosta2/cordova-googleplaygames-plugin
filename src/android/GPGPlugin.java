package com.ludei.googleplaygames.cordova;

import android.content.Intent;
import android.util.Log;

import com.ludei.googleplaygames.GPGService;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;


public class GPGPlugin extends CordovaPlugin implements GPGService.SessionCallback, GPGService.WillStartActivityCallback {


    protected GPGService _service;
    protected CallbackContext _sessionListener;

    @Override
    protected void pluginInitialize() {
        _service = new GPGService(this.cordova.getActivity());
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        try
        {
            Method method = this.getClass().getDeclaredMethod(action, CordovaArgs.class, CallbackContext.class);
            method.invoke(this, args, callbackContext);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d("df", "activity Result calling ");
        this._service.handleActivityResult(requestCode, resultCode, intent);
    }



    @SuppressWarnings("unused")
    public void setListener(CordovaArgs args, CallbackContext ctx) throws JSONException {
        _sessionListener = ctx;
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        ctx.sendPluginResult(result);
    }

    @SuppressWarnings("unused")
    public void init(CordovaArgs args, CallbackContext ctx) throws JSONException {

        JSONObject params = args.optJSONObject(0);
        String[] scopes = null;
        if (params != null) {
            JSONArray array = params.optJSONArray("scopes");
            if (array != null) {
                scopes = new String[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    scopes[i] = array.getString(i);
                }
            }
        }

         Log.d("df", " Own LOOGGGINGG  df");
        _service.setSessionListener(this);
        _service.setWillStartActivityListener(this);
        _service.setExecutor(cordova.getThreadPool());
        _service.init(scopes);
        ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
    }

    @SuppressWarnings("unused")
    public void login(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        JSONObject obj = args.optJSONObject(0);
        String[] scopes = null;
        if (obj != null) {
            JSONArray array = obj.optJSONArray("scope");
            if (array != null) {
                scopes = new String[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    scopes[i] = array.getString(i);
                }
            }
        }


        _service.login(scopes, new GPGService.SessionCallback() {
            @Override
            public void onComplete(GPGService.Session session, GPGService.Error error) {
                ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, toSessionData(session, error)));
            }
        });
    }

    @SuppressWarnings("unused")
    public void disconnect(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        _service.logout(new GPGService.CompletionCallback() {
            @Override
            public void onComplete(GPGService.Error error) {

                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
                }

            }
        });

    }

    @SuppressWarnings("unused")
    public void showLeaderboard(CordovaArgs args, final CallbackContext ctx) throws JSONException {


        JSONObject json = new JSONObject(args.getString(0));
        String leaderboardId = json.getString("name"); //args.optString(0);

//        if(args.isNull(0)){
//            Log.d("df", " my values is NULL leader los ");
//        }else {
//             //args.get(0);
//            Log.d("df", " my valueswwkej288djjj   NULL leader los ");
//            System.out.println(args.get(0));
//           Object var =  args.opt(0);
//              Log.d("df", " aa leadgetGetdata = " + args.get(0));
//          //  for ( Object var : args)
//                System.out.println(var);
//        }

        //   showLeaderboard:function(i,n){e.exec(this.serviceName,"showLeaderboard",[n],function(){i&&i()},function(e){i&&i(e)})},

         // my add's
        Log.d("df", "my LeaderIDBoard == " + leaderboardId);
       //  leaderboardId = "CgkIr9zokPQeEAIQAQ"; // CgkIr9zokPQeEAIQAQ
         //Log.d("df", "my LeaderIDBoard == " + leaderboardId);
        _service.showLeaderboard(leaderboardId, new GPGService.CompletionCallback() {
            @Override
            public void onComplete(GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void showAchievements(CordovaArgs args, final CallbackContext ctx) throws JSONException {
            Log.d("df", "show my Achievement loos 111");
        _service.showAchievements(new GPGService.CompletionCallback() {
            @Override
            public void onComplete(GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    String str = null;
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void loadAchievements(CordovaArgs args, final CallbackContext ctx) throws JSONException {

          Log.d("df", "los LOAD ACHIEVEMENT ");
        _service.loadAchievements(new GPGService.AchievementsCallback() {
            @Override
            public void onComplete(ArrayList<GPGService.GPGAchievement> achievements, GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                }
                else {
                    JSONArray array = new JSONArray();
                    if (achievements != null) {
                        for (GPGService.GPGAchievement ach: achievements) {
                            array.put(new JSONObject(ach.toMap()));
                        }
                    }
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, array));
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void submitAchievement(CordovaArgs args, final CallbackContext ctx) throws JSONException {

          Log.d("df", " SubmitAchievement !DSSS ");
        //String achievement = args.getString(0);
        //boolean show = args.optBoolean(1);
        JSONObject data = args.getJSONObject(0);
        boolean show = data.getBoolean("sho");
        String achievement = data.getString("achive");

        _service.unlockAchievement(achievement, show, new GPGService.CompletionCallback() {
            @Override
            public void onComplete(GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
                }
            }
        });
    }


    @SuppressWarnings("unused")
    public void loadScore(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        String leaderboardId = args.getString(0);

        Log.d("df", "leaderBoard == " + leaderboardId);

        _service.loadScore(leaderboardId, new GPGService.LoadScoreCallback() {
            @Override
            public void onComplete(long score, GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, score));
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void submitScore(CordovaArgs args, final CallbackContext ctx) throws JSONException {

      //  long score = args.getLong(0);
       // String leaderboardId = args.getString(1);
         // scores: 100, leaderId:

        JSONObject data = args.getJSONObject(0);
        long score = data.getLong("scores");
        String leaderboardId = data.getString("leaderId");
      //  Log.d("df",  "my SS ==  " + args.getJSONObject(0));
       // Log.d("df", "snap oSSbject desk = " + data.getString("desc"));


        Log.d("df", " SUBBBBBMIITT OK " + score + " and == " + leaderboardId);

        _service.submitScore(score, leaderboardId, new GPGService.CompletionCallback() {
            @Override
            public void onComplete(GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void addScore(CordovaArgs args, final CallbackContext ctx) throws JSONException {

//        long score = args.getLong(0);
//        String leaderboardId = args.getString(1);
        JSONObject data = args.getJSONObject(0);
        long score = data.getLong("scores");
        String leaderboardId = data.getString("leaderId");

        _service.addScore(score, leaderboardId, new GPGService.CompletionCallback() {
            @Override
            public void onComplete(GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
                }
            }
        });
    }


    @SuppressWarnings("unused")
    public void loadPlayer(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        String playerId = args.optString(0);
         Log.d("df", " Loading my Player !!");

        _service.loadPlayer(playerId, new GPGService.LoadPlayerCallback() {
            @Override
            public void onComplete(GPGService.GPGPlayer player, GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                }
                else if (player != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, new JSONObject(player.toMap())));
                }
                else {

                    error = new GPGService.Error("Player not found", 0);
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                }
            }
        });
    }

    protected void notifySavedGameCallback(JSONObject data, GPGService.Error error, CallbackContext ctx) {
        Log.d("df", " we are calling this 01");
        ArrayList<PluginResult> args = new ArrayList<PluginResult>();
        if (data != null) {
             Log.d("s"," in data backs ");
            args.add(new PluginResult(PluginResult.Status.OK, data)); // was data.toMap()
        } else {
            args.add(new PluginResult(PluginResult.Status.OK, (String) null)); //null value
        }

        if (error != null) {
            args.add(new PluginResult(PluginResult.Status.OK, new JSONObject(error.toMap())));
        }
        Log.d("df", " we are calling this 2");
        ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, args));

      }

    @SuppressWarnings("unused")
    public void loadSavedGame(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        String identifier = args.getString(0);

        _service.loadSavedGame(identifier, new GPGService.SavedGameCallback() {
            @Override
            public void onComplete(JSONObject data, GPGService.Error error) {
                notifySavedGameCallback(data, error, ctx);
            }

//            @Override
//            public void onComplete(GPGService.GameSnapshot data, GPGService.Error error) {
//                notifySavedGameCallback(data, error, ctx);
//            }
        });
    }

    @SuppressWarnings("unused")
    public void writeSavedGame(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        JSONObject data = args.getJSONObject(0);
          Log.d("df",  "my snapshot ==  " + args.getJSONObject(0));
           Log.d("df", "snap object desk = " + data.getString("desc"));
        GPGService.GameSnapshot snapshot = GPGService.GameSnapshot.fromJSONObject(data);


         // final String NameSnapshot
         final String aName  = "aLosSnaptwo";

          // was myNewSnapshot and correct work
       // _service.writeSavedGame(snapshot, new GPGService.CompletionCallback() {
          _service.writeSavedGame(data, aName, new GPGService.CompletionCallback() {
            @Override
            public void onComplete(GPGService.Error error) {
                if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                } else {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, (String) null));
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public void showSavedGames(CordovaArgs args, final CallbackContext ctx) throws JSONException {
        Log.d("df", "show saved games");

        _service.showSavedGames((data, error) -> notifySavedGameCallback(data, error, ctx));
    }


    @SuppressWarnings("unused")
    public void submitEvent(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        String eventId = args.getString(0);
       // String eventAll = args.getString(0);
         //String findName = eventAll.

        JSONObject json = new JSONObject(args.getString(0));
        Log.d("dsf","my json name == "+ json.getString("name"));
        int LosInt = json.getInt("value");
        Log.d("df", "my json value == "+ LosInt);
        //Read more: https://www.java67.com/2016/10/3-ways-to-convert-string-to-json-object-in-java.html#ixzz6bs1mCBis
             //JSONObject al = args.getString(0);
             //JSONObject ef = JSONObject(args.getString(0))
          eventId = json.getString("name");
        int increment = json.getInt("value");
       // String eventId = findName;
        //int increment = args.optInt(1);
        if (increment == 0) {
            increment  = 1;
        }
         Log.d("df", " my EVENT LLOOS == "+  eventId + " df " + increment );
        // my Event 
        _service.submitEvent(eventId, increment);
        ctx.success();
    }

    @SuppressWarnings("unused")
    public void loadPlayerStats(CordovaArgs args, final CallbackContext ctx) throws JSONException {

          Log.d("df", " loadPlayerStats loadPlayerStats loadPlayerStats !! ");

        _service.loadPlayerStats(new GPGService.RequestCallback() {
            @Override
            public void onComplete(JSONObject responseJSON, GPGService.Error error) {
                if (responseJSON != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, responseJSON));
                } else if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, new JSONObject(error.toMap())));
                }
                // should never happen
                else {
                    error = new GPGService.Error("Player Stats could not be accessed, no specific error code", 1);
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                }
            }
        });
    }


    @SuppressWarnings("unused") // was requestFriends   ShowFriends
    public void ShowFriends(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        Log.d("df", " ShowFriends in java !!! ");

        _service.RequrstFriends(new GPGService.RequestCallback() {
            @Override
            public void onComplete(JSONObject responseJSON, GPGService.Error error) {
                if (responseJSON != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, responseJSON));
                } else if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, new JSONObject(error.toMap())));
                }
                // should never happen
                else {
                    error = new GPGService.Error("Player Stats could not be accessed, no specific error code", 1);
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                }
            }
        });
    }


    @SuppressWarnings("unused")
    public void recordVideo(CordovaArgs args, final CallbackContext ctx) throws JSONException {

        Log.d("df", " RecordVideo STARTS !!!  ");

        _service.recordVideoL(new GPGService.RequestCallback() {
            @Override
            public void onComplete(JSONObject responseJSON, GPGService.Error error) {
                if (responseJSON != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.OK, responseJSON));
                } else if (error != null) {
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, new JSONObject(error.toMap())));
                }
                // should never happen
                else {
                    error = new GPGService.Error("Player Stats could not be accessed, no specific error code", 1);
                    ctx.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, new JSONObject(error.toMap())));
                }
            }
        });
    }



    //Session Listener
    @Override
    public void onComplete(GPGService.Session session, GPGService.Error error)
    {
        if (_sessionListener != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, this.toSessionData(session, error));
            result.setKeepCallback(true);
            _sessionListener.sendPluginResult(result);
        }
    }

    @Override
    public void onWillStartActivity() {
        this.cordova.setActivityResultCallback(this);
    }



    //utilities
    private JSONObject toSessionData(GPGService.Session session, GPGService.Error error) {
        JSONObject object = new JSONObject();
        try {
            if (session != null) {
                object.put("session", new JSONObject(session.toMap()));
            }
            if (error != null) {
                object.put("error", new JSONObject(error.toMap()));
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
        }
        return object;
    }


}
