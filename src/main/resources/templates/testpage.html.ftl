<!DOCTYPE html>
<html>
	<head>
        <title>SecurityTokenService testpage</title>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
		<meta http-equiv="cache-control" content="no-store, no-cache, must-revalidate">
		<meta http-equiv="Pragma" content="no-store, no-cache">
		<meta http-equiv="Expires" content="0">
        <script src="files/js/jquery.js"></script>
        <link rel="stylesheet" href="files/css/style.css" type="text/css" />
        <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css">
    </head>
<body>

<script src="files/js/logontest.js"></script>

<h1>SecurityTokenService</h1>

<p>
This is the testpage for Security Token Service. Any misuse will be prosecuted.<br>
The service description (WADL) is available <a href="application.wadl">here</a><br><br>
<strong>Usage:</strong>1. Autenticate application, 2. Authenticate user, 3. Fetch usertoken<br>
You may update the application credential or test user credential before pushing the button.
</p>

<h3>1. Application authentication</h3><br>
<div class="row">
    <div class="col-sm-8">
        <FORM id="applicationlogonform" action="logon" method="post" role="form" class="form-horizontal">
            <div class="form-group">
                <label for="applicationcredential" class="col-sm-2">Applicationcredential:</label>
                <div class="col-sm-10">
                   <TEXTAREA id="applicationcredential" NAME="applicationcredential" COLS=80 ROWS=7>${applicationcredential?html}</TEXTAREA>
               </div>
            </div>
            <div class="form-group">
                <label for="applicationlogonformurl" class="col-sm-2">Url:</label> 
                <div class="col-sm-10">
                    <span id="applicationlogonformurl">logon</span>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-10 col-sm-offset-2">
                    <input name="appsubmit" type="submit" value="1. Press here to logon application" class="btn btn-primary"> 
                </div>
            </div>
        </FORM>
    </div>
    <div class="col-sm-4">
        <div id="apptokenid">&nbsp;</div>
    </div>
</div>
<hr>
<h3>2. User authentication</h3>
<div class="row">
    <div class="col-sm-8">
        <FORM id="userlogonform" method="post" action="." role="form" class="form-horizontal">
            <div class="form-group">
                <label for="usercredential" class="col-sm-2">Usercredential:</label>
                <div class="col-sm-10">
                        <TEXTAREA id="usercredential" NAME="usercredential" COLS=80 ROWS=7>${testUserCredential?html}</TEXTAREA>
                </div>
            </div>
            <div class="form-group">
                <label for="apptoken1" class="col-sm-2">Apptoken:</label>
                <div class="col-sm-10">
                    <TEXTAREA id="apptoken1" NAME="apptoken" COLS=80 ROWS=11 placeholder="apptokenxml inn her"></TEXTAREA>
                </div>
            </div>
            <div class="form-group">
                <label for="userlogonformurl" class="col-sm-2">Url:</label> 
                <div class="col-sm-10">
                    <span id="userlogonformurl">logon</span>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-10 col-sm-offset-2">
                        <input type="submit" value="2. Press here to logon user"/  class="btn btn-primary">
                </div>
            </div>
        </FORM>
    </div>
    <div class="col-sm-4">
        <div id="usertokendiv1">
            <label for="usertoken1">UserToken:</label><br>
            <div id="usertoken1">Authenticating...</div>
        </div>        
    </div>
</div>

<hr>
<h3>3. Get usertoken</h3>
<div class="row">
    <div class="col-sm-8">
        <FORM id="getusertokenform" method="post" action="." role="form" class="form-horizontal">

            <div class="form-group">
                <label for="apptoken2"  class="col-sm-2">Apptoken:</label>
                <div class="col-sm-10">
                    <TEXTAREA id="apptoken2" NAME="apptoken" COLS=80 ROWS=11 placeholder="apptokenxml inn her"></TEXTAREA>
                </div>
            </div>
            <div class="form-group">
                <label for="usertokenidparam" class="col-sm-2">Usertokenid:</label>
                <div class="col-sm-10">
                    <INPUT id="usertokenidparam" TYPE="text" NAME="usertokenid" placeholder="Usertokenid inn her">
                </div>
            </div>
            <div class="form-group">
                <label for="applicationlogonformurl" class="col-sm-2">Url:</label> 
                <div class="col-sm-10">
                    <span id="getusertokenformurl">logon</span>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-10 col-sm-offset-2">
                    <input type="submit" value="3. Fetch user token" class="btn btn-primary">
                </div>
            </div>
        </FORM>
    </div>
    <div class="col-sm-4">
        <div id="usertokendiv2">
                <label for="usertoken1">UserToken:</label><br>
                <div id="usertoken2">Fetching usertoken...</div>
            </div>
    </div>
</div>

</BODY>
</html>