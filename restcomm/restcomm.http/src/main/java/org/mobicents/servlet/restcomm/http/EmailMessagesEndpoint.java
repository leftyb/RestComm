package org.mobicents.servlet.restcomm.http;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.*;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.AuthorizationException;
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.email.EmailRequest;
import org.mobicents.servlet.restcomm.email.EmailResponse;
import org.mobicents.servlet.restcomm.email.Mail;
import org.mobicents.servlet.restcomm.http.converter.EmailMessageConverter;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.email.api.CreateEmailService;
import org.mobicents.servlet.restcomm.email.api.EmailService;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;


/**
 *  @author lefty .liblefty@telestax.com (Lefteris Banos)
 */
public class EmailMessagesEndpoint extends AbstractEndpoint {
    private static Logger logger = Logger.getLogger(EmailMessagesEndpoint.class);
    @Context
    protected ServletContext context;
    protected ActorSystem system;
    protected Configuration configuration;
    protected Configuration confemail;
    protected Gson gson;
    protected AccountsDao accountsDao;
    ActorRef mailerService = null;

    // Send the email.
    protected Mail emailMsg;

    public EmailMessagesEndpoint() {
        super();
    }

    @PostConstruct
    public void init() {
        final DaoManager storage = (DaoManager) context.getAttribute(DaoManager.class.getName());
        configuration = (Configuration) context.getAttribute(Configuration.class.getName());
        confemail=configuration.subset("smtp-service");
        configuration = configuration.subset("runtime-settings");
        accountsDao = storage.getAccountsDao();
        system = (ActorSystem) context.getAttribute(ActorSystem.class.getName());
        super.init(configuration);
        final EmailMessageConverter converter = new EmailMessageConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Mail.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    private void normalize(final MultivaluedMap<String, String> data) throws IllegalArgumentException {
        final String from = data.getFirst("From");
        data.remove("From");
        try {
            data.putSingle("From", validateEmail(from));
        } catch (final InvalidEmailException  exception) {
            throw new IllegalArgumentException(exception);
        }
        final String to = data.getFirst("To");
        data.remove("To");
        try {
            data.putSingle("To", validateEmail(to));
        } catch (final InvalidEmailException exception) {
            throw new IllegalArgumentException(exception);
        }
        final String body = data.getFirst("Body");
        if (body.getBytes().length > 160) {
            data.remove("Body");
            data.putSingle("Body", body.substring(0, 159));
        }

        final String subject = data.getFirst("Subject");
        if (body.getBytes().length > 60) {
            data.remove("Subject");
            data.putSingle("Subject", subject.substring(0, 60));
        }

        final String cc = data.getFirst("CC");
        data.remove("CC");
        try {
            data.putSingle("CC", validateEmails(cc));
        } catch (final InvalidEmailException exception) {
            throw new IllegalArgumentException(exception);
        }

        final String bcc = data.getFirst("CC");
        data.remove("BCC");
        try {
            data.putSingle("BCC", validateEmails(bcc));
        } catch (final InvalidEmailException exception) {
            throw new IllegalArgumentException(exception);
        }

    }

    @SuppressWarnings("unchecked")
    protected Response putEmailMessage(final String accountSid, final MultivaluedMap<String, String> data) {
        try {
            secure(accountsDao.getAccount(accountSid), "RestComm:Create:SmsMessages"); //need to fix for Emails.
            secureLevelControl(accountsDao, accountSid, null);
        } catch (final AuthorizationException exception) {
            return status(UNAUTHORIZED).build();
        }
        try {
            validate(data);
            normalize(data);
        } catch (final RuntimeException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }
        final String sender = data.getFirst("From");
        final String recipient = data.getFirst("To");
        final String body = data.getFirst("Body");
        final String subject = data.getFirst("Subject");
        final String cc = data.getFirst("CC");
        final String bcc = data.getFirst("BCC");

        try {

            // Send the email.
            emailMsg = new Mail(sender, recipient, subject, body ,cc,bcc, DateTime.now(),accountSid);
            if (mailerService == null){
                mailerService = session(confemail);
            }

            final ActorRef observer = observer();
            mailerService.tell(new Observe(observer), observer);
            return ok(gson.toJson(emailMsg), APPLICATION_JSON).build();
        } catch (final Exception exception) {
            return status(INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
        }
    }


    private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
        if (!data.containsKey("From")) {
            throw new NullPointerException("From can not be null.");
        } else if (!data.containsKey("To")) {
            throw new NullPointerException("To can not be null.");
        } else if (!data.containsKey("Body")) {
            throw new NullPointerException("Body can not be null.");
        } else if (!data.containsKey("Subject")) {
            throw new NullPointerException("Body can not be null.");
        }
    }


    public String validateEmail(String email) throws InvalidEmailException {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        if (!m.matches()) {
            String err = "Not a Valid Email Address";
            throw new InvalidEmailException(err);
        }
        return email;
    }

    public String validateEmails(String emails) throws InvalidEmailException {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);

        if (emails.indexOf(',') > 0) {
            String[] emailsArray = emails.split(",");
            for (int i = 0; i < emailsArray.length; i++) {
                java.util.regex.Matcher m = p.matcher(emailsArray[i]);
                if (!m.matches()) {
                    String err = "Not a Valid Email Address:" + emailsArray[i];
                    throw new InvalidEmailException(err);
                }
            }
        }else{
            java.util.regex.Matcher m = p.matcher(emails);
            if (!m.matches()) {
                String err = "Not a Valid Email Address";
                throw new InvalidEmailException(err);
            }
        }
        return emails;
    }

    private ActorRef session(final Configuration configuration) {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public Actor create() throws Exception {
                final CreateEmailService builder = new EmailService();
                builder.CreateEmailSession(configuration);
                return builder.build();
            }
        }));
    }

    private ActorRef observer() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new EmailSessionObserver();
            }
        }));
    }

    private final class EmailSessionObserver extends UntypedActor {
        public EmailSessionObserver() {
            super();
        }

        @Override
        public void onReceive(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if (EmailResponse.class.equals(klass)) {
                final EmailResponse response = (EmailResponse) message;
                if (!response.succeeded()) {
                    logger.error(
                            "There was an error while sending an email :" + response.error(),
                            response.cause());
                }

                final UntypedActorContext context = getContext();
                final ActorRef self = self();
                mailerService.tell(new StopObserving(self), null);
                context.stop(self);
            }else if (Observing.class.equals(klass)){
                mailerService.tell(new EmailRequest(emailMsg), self());
            }
        }
    }

    private static class InvalidEmailException extends Exception {
        public InvalidEmailException() {}

        public InvalidEmailException(String message) {
            super(message);
        }
    }

}



