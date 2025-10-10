package com.microsoft.xbox.telemetry.utc.model;


public class UTCNames {

    public static class Client {

        public static class Errors {
            public static final String Failure = "Client Error Type - Failure";
            public static final String MSACancel = "Client Error Type - MSA canceled";
            public static final String SignedOut = "Client Error Type - Signed out";
            public static final String UINeeded = "Client Error Type - UI Needed";
            public static final String UserCancel = "Client Error Type - User canceled";
        }
    }

    public static class PageAction {

        public static class ChangeRelationship {
            public static final String Action = "Change Relationship - Action";
            public static final String Done = "Change Relationship - Done";
        }

        public static class DeepLink {
            public static final String FriendSuggestions = "DeepLink - Friend Suggestions";
            public static final String SendToStore = "DeepLink - Store Redirect";
            public static final String TitleAchievements = "DeepLink - GameHub Achievements";
            public static final String TitleHub = "DeepLink - GameHub";
            public static final String UserProfile = "DeepLink - User Profile";
            public static final String UserSettings = "DeepLink - User Settings";
        }

        public static class Errors {
            public static final String Close = "Errors - Close error screen";
            public static final String GoToBanned = "Errors - View enforcement site";
            public static final String GoToXboxCom = "Errors - View Xbox.com";
            public static final String Offline = "Errors - Offline";
            public static final String Retry = "Errors - Try again";
            public static final String RetryAccountCreation = "Errors - Retry Account Creation";
            public static final String RetrySignIn = "Errors - Retry Signin";
            public static final String RightButton = "Errors - Ok";
            public static final String SignOut = "Errors - Signout";
        }

        public static class FriendFinder {
            public static final String Close = "Friend Finder - Close";
            public static final String ContactsSignUp = "Friend Finder Contacts - Sign Up";
            public static final String ContactsSuggestions = "Friend Finder Contacts - Suggestions";
            public static final String Done = "Friend Finder - Done";
            public static final String FacebookSignup = "Friend Finder - Facebook Signup";
            public static final String FacebookSuggestions = "Friend Finder - Facebook Suggestions";
            public static final String GamertagSearch = "Friend Finder - Gamertag Search";
            public static final String GamertagSearchSubmit = "Friend Finder - Gamertag Search Submit";
            public static final String GamertagSearchSuccess = "Friend Finder - Gamertag Search Success";
        }

        public static class FriendFinderContacts {
            public static final String AddFriends = "Friend Finder Contacts - Add Friends";
            public static final String CallMe = "Friend Finder Contacts - Call Me";
            public static final String ChangeRegion = "Friend Finder Contacts - Change Region";
            public static final String Close = "Friend Finder Contacts - Close";
            public static final String LinkSuccess = "Friend Finder Contacts - Link Success";
            public static final String Next = "Friend Finder Contacts - Next";
            public static final String ResendCode = "Friend Finder Contacts - Resend Code";
            public static final String SendInvite = "Friend Finder Contacts - Send Invite";
            public static final String Skip = "Friend Finder Contacts - Skip";
            public static final String Unfriend = "Friend Finder Contacts - Unfriend";
        }

        public static class FriendFinderFacebook {
            public static final String AddFacebookFriend = "Friend Finder Facebook - Add Facebook Friend";
            public static final String AddFriendCancel = "Friend Finder Facebook - Add Facebook Friend Cancel";
            public static final String FriendFinderBack = "Friend Finder Facebook - Friend Finder Back";
            public static final String Login = "Friend Finder Facebook - Login";
            public static final String LoginCancel = "Friend Finder Facebook - Login Cancel";
            public static final String OptInNext = "Friend Finder Facebook - Opt In Next";
            public static final String ShareCancel = "Friend Finder Facebook - Upsell Cancel";
            public static final String ShareSuccess = "Friend Finder Facebook - Upsell Success";
            public static final String Unfriend = "Friend Finder Facebook - Unfriend";
        }

        public static class MSA {
            public static final String Cancel = "MSA - Cancel";
            public static final String Start = "MSA - Start";
            public static final String Success = "MSA - Success";
        }

        public static class PeopleHub {
            public static final String Block = "People Hub - Block";
            public static final String BlockOK = "People Hub - Block OK";
            public static final String Mute = "People Hub - Mute";
            public static final String Report = "People Hub - Report";
            public static final String ReportOK = "People Hub - Report OK";
            public static final String Unblock = "People Hub - Unblock";
            public static final String ViewXboxApp = "People Hub - View in Xbox App";
            public static final String ViewXboxAppOK = "People Hub - View in Xbox App OK";
        }

        public static class Signin {
            public static final String AccountSuccess = "Signin - Account acquired";
            public static final String Signin = "Signin - Sign in";
            public static final String SigninStart = "Signin - Sign in Start";
            public static final String TicketSuccess = "Signin - Ticket acquired";
        }

        public static class Signout {
            public static final String Signout = "Signout - User signed out";
        }

        public static class Signup {
            public static final String ChangeUser = "Signup - Signin with different user";
            public static final String ClaimGamerTag = "Signup - Claim gamertag";
            public static final String ClearGamerTagText = "Signup - Clear gamertag text";
            public static final String SearchGamertag = "Signup - Search for gamertag";
            public static final String SelectSuggestedGamerTag = "Signup - Select suggested gamertag";
        }

        public static class UserCancel {
            public static final String Cancel = "UserCancel - User canceled";
        }

        public static class Welcome {
            public static final String ChangeUser = "Welcome - Change user";
            public static final String Done = "Welcome - Done";
        }

        public static class XboxAccount {
            public static final String Start = "XboxAccount - Start";
            public static final String Success = "XboxAccount - Success";
        }
    }

    public static class PageView {

        public static class ChangeRelationship {
            public static final String ChangeRelationshipView = "Change Relationship - Change Relationship View";
        }

        public static class Errors {
            public static final String Banned = "Banned error view";
            public static final String Create = "Create error view";
            public static final String Generic = "Generic error view";
            public static final String Offline = "Offline error view";
        }

        public static class FriendFinder {
            public static final String FriendFinderView = "Friend Finder - View";
        }

        public static class FriendFinderContacts {
            public static final String AddPhoneView = "Friend Finder Contacts - Add Phone View";
            public static final String ChangeRegionView = "Friend Finder Contacts - Change Region View";
            public static final String ErrorView = "Friend Finder Contacts - Error View";
            public static final String FindFriendsView = "Friend Finder Contacts - Find Friends View";
            public static final String InviteFriendsView = "Friend Finder Contacts - Invite Friends View";
            public static final String OptInView = "Friend Finder Contacts - Opt In View";
            public static final String VerifyPhoneView = "Friend Finder Contacts - Verify Phone View";
        }

        public static class FriendFinderFacebook {
            public static final String FindFriendsView = "Friend Finder Facebook - Find Friends View";
            public static final String LinkAccountView = "Friend Finder Facebook -  Link View";
            public static final String OptInView = "Friend Finder Facebook -  Opt In View";
            public static final String ShareView = "Friend Finder Facebook - Share View";
        }

        public static class PeopleHub {
            public static final String PeopleHubMeView = "People Hub - ME View";
            public static final String PeopleHubYouView = "People Hub - You View";
            public static final String ReportView = "People Hub Report view";
        }

        public static class Signin {
            public static final String View = "Sign in view";
        }

        public static class Signup {
            public static final String View = "Sign up view";
        }

        public static class Welcome {
            public static final String View = "Welcome view";
        }
    }

    public static class Service {

        public static class Errors {
            public static final String GamerPicChoiceList = "Service Error - Gamerpic Choice List";
            public static final String GamerPicUpdate = "Service Error - Gamerpic Update";
            public static final String LoadBitmap = "Service Error - Load bitmap";
            public static final String LoadPrivacySettings = "Service Error - Load privacy settings";
            public static final String LoadProfile = "Service Error - Load Profile";
            public static final String LoadSuggestions = "Service Error - Load suggestions";
            public static final String LoadXTOKEN = "Service Error - Load XTOKEN";
            public static final String ReserveGamerTag = "Service Error - Reserve gamertag";
            public static final String SetPrivacySettings = "Service Error - Set privacy settings";
        }
    }
}
