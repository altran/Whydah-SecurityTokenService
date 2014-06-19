$(document).ready(function(){
    $("#applicationlogonform").submit(submitAppLogon);
    $("#userlogonform").submit(submitUserLogon);
    $("#getusertokenform").submit(submitGetUsertoken);
   });

/*
 * Logger på applikasjonen.
 */
function submitAppLogon(event) {
        event.preventDefault();
        var $form = $( this );
        var applicationcredential = $form.find( 'textarea[name="applicationcredential"]' ).val();
        var url = $form.attr('action');

        $.post( url, { applicationcredential: applicationcredential } , function(apptokenXMLObjectResponse) {
            var apptokenXMLString = apptokenXMLObjectResponse.xml ? apptokenXMLObjectResponse.xml : (new XMLSerializer()).serializeToString(apptokenXMLObjectResponse);
            $('textarea#apptoken1').val(apptokenXMLString);
            $('textarea#apptoken2').val(apptokenXMLString);
            var apptokenid = $(apptokenXMLObjectResponse).contents().find("applicationtokenID").text();
            $("#apptokenid").html("Application tokenID: " + apptokenid);
            $("#userlogonform").attr("action", "iam/" + apptokenid + "/usertoken");
            $("#userlogonformurl").html("iam/" + apptokenid + "/usertoken");
            $("#getusertokenform").attr("action", "iam/" + apptokenid + "/getusertokenbytokenid");
            $("#getusertokenformurl").html("iam/" + apptokenid + "/getusertokenbytokenid");
        }, "xml");
}

/*
 * Logger på bruker.
 */
function submitUserLogon(event) {
        event.preventDefault();
        var $form = $( this );
        var apptoken = $form.find( 'textarea[name="apptoken"]' ).val();
        var usercredential = $form.find( 'textarea[name="usercredential"]' ).val();
        var url = $form.attr('action');

        $("#usertokendiv1").fadeIn('slow', function() {});
        $.post( url, { apptoken: apptoken,  usercredential: usercredential} , function(usertokenXMLObjectResponse) {
            var usertokenID = $(usertokenXMLObjectResponse).find("token").attr("id");
            var usertokenXML = usertokenXMLObjectResponse.xml ? usertokenXMLObjectResponse.xml : (new XMLSerializer()).serializeToString(usertokenXMLObjectResponse);
            $("#usertokenidparam").val(usertokenID);
            $("#usertoken1").text(usertokenXML);
        }, "xml");
}

/*
 * Henter usertoken basert på usertokenid.
 */
function submitGetUsertoken(event) {
        event.preventDefault();
        var $form = $(this);
        var apptoken = $form.find( 'textarea[name="apptoken"]' ).val();
        var usertokenid = $form.find( 'input[name="usertokenid"]' ).val();
        var url = $form.attr('action');

        $.post( url, { apptoken: apptoken,  usertokenid: usertokenid} , function(usertokenXMLObjectResponse) {
            var usertokenXML = usertokenXMLObjectResponse.xml ? usertokenXMLObjectResponse.xml : (new XMLSerializer()).serializeToString(usertokenXMLObjectResponse);
            $("#usertoken2").text(usertokenXML);
            $("#usertokendiv2").fadeIn('slow', function() {});
        }, "xml");
}
