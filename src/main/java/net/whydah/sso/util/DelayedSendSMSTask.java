package net.whydah.sso.util;

import net.whydah.sso.commands.adminapi.user.CommandSendSMSToUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class DelayedSendSMSTask {
    Toolkit toolkit;

    Timer timer;

    String smsGwServiceURL;
    String smsGwServiceAccount;
    String smsGwUsername;
    String smsGwPassword;
    String smsGwQueryParam;
    String cellNo;
    String smsMessage;
    private static final Logger log = LoggerFactory.getLogger(DelayedSendSMSTask.class);

    public DelayedSendSMSTask(long timestamp, String smsGwServiceURL, String smsGwServiceAccount, String smsGwUsername, String smsGwPassword, String smsGwQueryParam, String cellNo, String smsMessage) {
        this.smsGwServiceURL = smsGwServiceURL;
        this.smsGwServiceAccount = smsGwServiceAccount;
        this.smsGwUsername = smsGwUsername;
        this.smsGwPassword = smsGwPassword;
        this.smsGwQueryParam = smsGwQueryParam;
        this.cellNo = cellNo;
        this.smsMessage = smsMessage;

        toolkit = Toolkit.getDefaultToolkit();
        timer = new Timer();
        long milliseconds = timestamp - new Date().getTime();
        log.debug("Task started, waiting {} milliseconds", milliseconds);
        timer.schedule(new RemindTask(), milliseconds);
    }

    class RemindTask extends TimerTask {
        public void run() {
            log.debug("Task completed. cellNo:{}   message:{}   time:{}", cellNo, smsMessage, new Date().toString());
            new CommandSendSMSToUser(smsGwServiceURL, smsGwServiceAccount, smsGwUsername, smsGwPassword, smsGwQueryParam, cellNo, smsMessage).execute();
            timer.cancel();
        }
    }

}