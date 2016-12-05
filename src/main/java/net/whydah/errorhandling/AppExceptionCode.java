package net.whydah.errorhandling;

import javax.ws.rs.core.Response.Status;




public class AppExceptionCode {

	//USER EXCEPTIONS
	public static AppException USER_AUTHENTICATION_FAILED_6000 = new AppException(Status.FORBIDDEN, 6000, "Authentication failed", "Authentication failed", "");
	public static AppException USER_VALIDATE_FAILED_6001 = new AppException(Status.UNAUTHORIZED, 6001, "UserToken is invalid", "validateUserTokenXML failed", "");
	public static AppException USER_INVALID_USERTOKENID_6002 = new AppException(Status.NOT_ACCEPTABLE, 6002, "Attempt to access with non acceptable usertokenid", "Attempt to access with non acceptable usertokenid", "");
	public static AppException USER_USERTICKET_NOTFOUND_6003 = new AppException(Status.GONE, 6003, "Attempt to resolve non-existing userticket", "Attempt to resolve non-existing userticket", "");
			
	//APPLICATION EXCEPTIONS
	public static AppException APP_ILLEGAL_7000 = new AppException(Status.FORBIDDEN, 7000, "Illegal Application.", "Application is invalid", "");
	
	//MISC
	public static AppException MISC_MISSING_PARAMS_9998 = new AppException(Status.BAD_REQUEST, 9998, "Missing required parameters","Missing required parameters",""); 
	
	
	
	
}
