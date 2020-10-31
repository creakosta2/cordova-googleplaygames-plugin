cordova.define("cocoon-plugin-social-android-googleplaygames.GooglePlayGames", function(require, exports, module) {
!function()
{
 window.cordova&&"undefined"!=typeof require&&require("cocoon-plugin-social-common.Social");
  var e=window.Cocoon;
  e.define("Cocoon.Social",function(i)
  {function n(i){return new e.Social.User(i.playerId,i.playerAlias,"")}
  function t(i){var n=new e.Social.Achievement(i.identifier,i.title,i.description,"",0);
  return n.gpAchievementData=i,n}return i.GooglePlayGamesExtension=function(){return this.session=null,this.serviceName="LDGooglePlayGamesPlugin",this.onSessionChanged=new e.Signal,this.on=this.onSessionChanged.expose(),this.defaultScopes=["https://www.googleapis.com/auth/games","https://www.googleapis.com/auth/plus.login"],this},i.GooglePlayGamesExtension.prototype={settings:{},socialService:null,initialized:!1,auth:null,client:null,
  init:function(i,n){if(!i||"object"!=typeof i)throw"Invalid params los argument";this.settings=i,this.settings.hasOwnProperty("showAchievementNotifications")||(this.settings.showAchievementNotifications=!0),this.initialized=!0;var t=this;e.exec(this.serviceName,"setListener",[],function(e){t.session=e.session,t.onSessionChanged.emit("sessionChanged",null,[e])}),e.exec(this.serviceName,"init",[t.settings],function(e){n&&n(e)})},
  getSocialInterface:function(){if(!this.initialized)throw"You must call init() before getting the Social Interface";return this.socialService||(this.socialService=new e.Social.SocialServiceGooglePlayGames(this)),this.socialService}
  ,getMultiplayerInterface:function(){return e.Multiplayer.GooglePlayGames},login:function(i,n){var t=this;e.exec(this.serviceName,"login",[i],function(e){t.session=e.session,n&&n(e.session,e.error)},function(e){t.session=e.session,n&&n(e.session,e.error)})},isLoggedIn:function(){return!(!this.session||!this.session.playerId)},getSession:function(){return this.session},disconnect:function(i){e.exec(this.serviceName,"disconnect",[],i,i)},
  submitEvent:function(i,n){e.exec(this.serviceName,"submitEvent",[i,n],null,null)},
  loadSavedGame:function(i,n){e.exec(this.serviceName,"loadSavedGame",[i],n,n)},
  recordVideo:function(i,n){e.exec(this.serviceName, "recordVideo",[i],n,n)},
  ShowFriends:function(i,n){e.exec(this.serviceName, "ShowFriends",[i],n,n)},
  writeSavedGame:function(i,n){e.exec(this.serviceName,"writeSavedGame",[i],n,n)},
  showSavedGames:function(i){e.exec(this.serviceName,"showSavedGames",[],i,i)},
  loadAchievements:function(i){e.exec(this.serviceName,"loadAchievements",[],function(e){i(e||[],null)},function(e){i(null,e)})},
  submitAchievement:function(i,n,t){e.exec(this.serviceName,"submitAchievement",[i,!!n],function(){t()},function(e){t(e)})},
  loadScore:function(i,n){e.exec(this.serviceName,"loadScore",[i],function(e){n(e,null)},function(e){n(0,e)})},
  submitScore:function(i,n,t){e.exec(this.serviceName,"submitScore",[i,n],function(){t()},function(e){t(e)})},
  addScore:function(i,n,t){e.exec(this.serviceName,"addScore",[i,n],function(){t()},function(e){t(e)})},
  showAchievements:function(i){
  console.log("show in plugins AAA 01" + i);
  e.exec(this.serviceName,"showAchievements",[],function(){i&&i()},function(e){i&&i(e)})},
  showLos:function(i, n){
   console.log("show in Los 312" + i);
  },
  showLeaderboard:function(i,n){e.exec(this.serviceName,"showLeaderboard",[n],function(){i&&i()},function(e){i&&i(e)})},
  loadPlayer:function(i,n){e.exec(this.serviceName,"loadPlayer",[i||""],function(e){n(e,null)},function(e){n(null,e)})},
  loadPlayerStats:function(i){e.exec(this.serviceName,"loadPlayerStats",[],function(e){i(e,null)},function(e){i(null,e)})}},i.GooglePlayGames=new i.GooglePlayGamesExtension,i.SocialServiceGooglePlayGames=function(i){e.Social.SocialServiceGooglePlayGames.superclass.constructor.call(this),this.gapi=i;var n=this;return this.gapi.on("sessionChanged",function(e){n.onLoginStatusChanged.emit("loginStatusChanged",null,[n.gapi.isLoggedIn(),e.error])}),this},i.SocialServiceGooglePlayGames.prototype={isLoggedIn:function(){return this.gapi.isLoggedIn()},login:function(e){var i=this;this.gapi.login({scope:this.gapi.settings.scopes},function(n,t){e&&e(i.isLoggedIn(),t)})},
  logout:function(e){this.gapi.disconnect(e)},getLoggedInUser:function(){return this.gapi.session?n(this.gapi.session):null},requestUser:function(e,i){this.gapi.loadPlayer(i||"",function(i,t){t?e(null,t):e(n(i),null)})},
  requestUserImage:function(e,i,n){e(!1,{message:"Not implemented"})},
  requestFriends:function(e,i){e(!1,{message:"Not implemented"})},
  publishMessage:function(e,i){i&&i({message:"Not implemented"})},
  publishMessageWithDialog:function(e,i){i&&i({message:"Not implemented"})},
  requestScore:function(i,n){n=n||{};var t=(n.userID||"me",n.leaderboardID||this.gapi.settings.defaultLeaderboard);if(!t)throw"leaderboardID not provided in the params. You can also set the default leaderboard in the init method";this.gapi.loadScore(t,function(n,s){if(s)i(null,s);else{var o=new e.Social.Score;o.score=n||0,o.leaderboardID=t,i(o)}})},submitScore:function(e,i,n){n=n||{};var t=n.leaderboardID||this.gapi.settings.defaultLeaderboard;if(!t)throw"leaderboardID not provided in the params. You can also set the default leaderboard in the init method";this.gapi.submitScore(e,t,i)},showLeaderboard:function(e,i){i=i||{};var n=i.leaderboardID||"";this.gapi.showLeaderboard(e,n)},prepareAchievements:function(e,i){if(!this._cachedAchievements||e){var n=this;this.gapi.loadAchievements(function(e,s){if(e&&!s){for(var o=[],a=0;a<e.length;a++)o.push(t(e[a]));n.setCachedAchievements(o),i(o,null)}else i([],response?response.error:null)})}else i(this._cachedAchievements,null)},requestAllAchievements:function(e){this.gapi.loadAchievements(function(i,n){if(i&&!n){for(var s=[],o=0;o<i.length;o++)s.push(t(i[o]));e(s,null)}else e([],n)})},requestAchievements:function(e,i){this.gapi.loadAchievements(function(i,n){if(i&&!n){for(var s=[],o=0;o<i.length;o++)i[o].unlocked&&s.push(t(i[o]));
  e(s,null)}else e([],n)})},
  submitAchievement:function(e,i){if(null===e||"undefined"==typeof e)throw"No achievementID specified";var n=this.translateAchievementID(e);this.gapi.submitAchievement(n,this.gapi.settings.showAchievementNotifications,i)},resetAchievements:function(e){e&&e({message:"Not implemented"})},
  showAchievements:function(e){this.gapi.showAchievements(e)}},e.extend(e.Social.SocialServiceGooglePlayGames,e.Social.SocialGamingService),i})}();
});
