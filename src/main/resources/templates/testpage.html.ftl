<!DOCTYPE html>
<html>
	<head>
        <title>SecurityTokenService</title>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
		<meta http-equiv="cache-control" content="no-store, no-cache, must-revalidate">
		<meta http-equiv="Pragma" content="no-store, no-cache">
		<meta http-equiv="Expires" content="0">
        <script src="files/js/jquery.js"></script>
        <link rel="stylesheet" href="files/css/style.css" type="text/css" />
    </head>
<body>
<script src="files/js/logontest.js"></script>

<div class="header">SecurityTokenService</div>

<div style="margin-top: 5px;border-bottom: 1px solid black; margin-bottom: 10px;">
This is the SSO token service for Whydah AS. Any misuse will be prosecuted.<br>
The service description (WADL) is available <a href="application.wadl">here</a><br>
</div>

<b>Application authentication</b><br>
<FORM id="applicationlogonform" action="logon" method="post">
    <label for="applicationcredential">applicationcredential:</label><br>
    <TEXTAREA id="applicationcredential" NAME="applicationcredential" COLS=60 ROWS=7>${applicationcredential?html}</TEXTAREA><br>
    <input name="appsubmit" type="submit" value="Logon application">Url: <span id="applicationlogonformurl">logon</span>
</FORM>
<div id="apptokenid">&nbsp;</div>

<hr>
<b>User authentication</b><br>
<FORM id="userlogonform" method="post" action=".">
    <div id="d1">
        <div id="d2">
            <div id="uauth_user">
                <label for="usercredential">Usercredential:</label><br>
                <TEXTAREA id="usercredential" NAME="usercredential" COLS=55 ROWS=7>${testUserCredential?html}</TEXTAREA>
            </div>
            <div id="uauth_app">
                <label for="apptoken1">Apptoken:</label><br>
                <TEXTAREA id="apptoken1" NAME="apptoken" COLS=80 ROWS=11 placeholder="apptokenxml inn her"></TEXTAREA>
            </div>
        </div>
        <div id="usertokendiv1">
            <label for="usertoken1">UserToken:</label><br>
            <div id="usertoken1">Authenticating...</div>
        </div>
    <div class="submit">
        <input type="submit" value="Logon user"/>Url: <span id="userlogonformurl"></span>
    </div>
</FORM><br/>

<hr>
<b>Get usertoken</b><br>
<FORM id="getusertokenform" method="post" action=".">
    <div id="d21">
        <div id="d22">
            <label for="apptoken2">Apptoken:</label><br>
            <TEXTAREA id="apptoken2" NAME="apptoken" COLS=60 ROWS=12 placeholder="apptokenxml inn her"></TEXTAREA><br>
            <label for="usertokenidparam">Usertokenid:</label><br/>
            <INPUT id="usertokenidparam" TYPE="text" NAME="usertokenid" placeholder="Usertokenid inn her"><br>
        </div>
    <div id="usertokendiv2">
        <label for="usertoken1">UserToken:</label><br>
        <div id="usertoken2">Fetching usertoken...</div>
    </div>
    </div>
    <div class="submit">
        <input type="submit"  value="Fetch">Url: <span id="getusertokenformurl"></span>
    </div>
</FORM>

</BODY>
</html>