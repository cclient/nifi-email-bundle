package com.github.cclient.nifi.email;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.behavior.SystemResource;
import org.apache.nifi.annotation.behavior.SystemResourceConsideration;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

@SupportsBatching
@Tags({"email", "put", "notify", "smtp"})
@InputRequirement(Requirement.INPUT_REQUIRED)
@CapabilityDescription("Sends an e-mail to configured recipients for each incoming FlowFile")
@SystemResourceConsideration(resource = SystemResource.MEMORY, description = "The entirety of the FlowFile's content (as a String object) "
        + "will be read into memory in case the property to use the flow file content as the email body is set to true.")
public class SendEmail extends AbstractProcessor {

    public static final PropertyDescriptor SMTP_HOSTNAME = new PropertyDescriptor.Builder()
            .name("SMTP Hostname")
            .description("The hostname of the SMTP host")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor SMTP_PORT = new PropertyDescriptor.Builder()
            .name("SMTP Port")
            .description("The Port used for SMTP communications")
            .required(true)
            .defaultValue("25")
            .addValidator(StandardValidators.PORT_VALIDATOR)
            .build();
    public static final PropertyDescriptor SMTP_USERNAME = new PropertyDescriptor.Builder()
            .name("SMTP Username")
            .description("Username for the SMTP account")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .build();
    public static final PropertyDescriptor SMTP_PASSWORD = new PropertyDescriptor.Builder()
            .name("SMTP Password")
            .description("Password for the SMTP account")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .required(false)
            .sensitive(true)
            .build();
    public static final PropertyDescriptor SMTP_AUTH = new PropertyDescriptor.Builder()
            .name("SMTP Auth")
            .description("Flag indicating whether authentication should be used")
            .required(true)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .defaultValue("true")
            .build();
    public static final PropertyDescriptor SMTP_TLS = new PropertyDescriptor.Builder()
            .name("SMTP TLS")
            .description("Flag indicating whether TLS should be enabled")
            .required(true)
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .defaultValue("false")
            .build();
    public static final PropertyDescriptor SMTP_SOCKET_FACTORY = new PropertyDescriptor.Builder()
            .name("SMTP Socket Factory")
            .description("Socket Factory to use for SMTP Connection")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .defaultValue("javax.net.ssl.SSLSocketFactory")
            .build();
    public static final PropertyDescriptor HEADER_XMAILER = new PropertyDescriptor.Builder()
            .name("SMTP X-Mailer Header")
            .description("X-Mailer used in the header of the outgoing email")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .defaultValue("NiFi")
            .build();
    public static final PropertyDescriptor ATTRIBUTE_NAME_REGEX = new PropertyDescriptor.Builder()
            .name("attribute-name-regex")
            .displayName("Attributes to Send as Headers (Regex)")
            .description("A Regular Expression that is matched against all FlowFile attribute names. "
                    + "Any attribute whose name matches the regex will be added to the Email messages as a Header. "
                    + "If not specified, no FlowFile attributes will be added as headers.")
            .addValidator(StandardValidators.REGULAR_EXPRESSION_VALIDATOR)
            .required(false)
            .build();
    public static final PropertyDescriptor CONTENT_TYPE = new PropertyDescriptor.Builder()
            .name("Content Type")
            .description("Mime Type used to interpret the contents of the email, such as text/plain or text/html")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.NONE)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .defaultValue("text/html")
            .build();
    public static final PropertyDescriptor FROM = new PropertyDescriptor.Builder()
            .name("From")
            .description("Specifies the Email address to use as the sender. "
                    + "Comma separated sequence of addresses following RFC822 syntax.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor TO = new PropertyDescriptor.Builder()
            .name("To")
            .description("The recipients to include in the To-Line of the email. "
                    + "Comma separated sequence of addresses following RFC822 syntax.")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor CC = new PropertyDescriptor.Builder()
            .name("CC")
            .description("The recipients to include in the CC-Line of the email. "
                    + "Comma separated sequence of addresses following RFC822 syntax.")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor BCC = new PropertyDescriptor.Builder()
            .name("BCC")
            .description("The recipients to include in the BCC-Line of the email. "
                    + "Comma separated sequence of addresses following RFC822 syntax.")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();
    public static final PropertyDescriptor SUBJECT = new PropertyDescriptor.Builder()
            .name("Subject")
            .description("The email subject")
            .required(true)
            .defaultValue("Message from NiFi")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor MESSAGE = new PropertyDescriptor.Builder()
            .name("Message")
            .description("The body of the email message")
            .required(false)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor GROUP_SIZE = new PropertyDescriptor
            .Builder().name("Group Size")
            .description("How many size of flowfile send in a mail")
            .required(false)
            .addValidator(StandardValidators.POSITIVE_INTEGER_VALIDATOR)
            .defaultValue("1000")
            .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("FlowFiles that are successfully sent will be routed to this relationship")
            .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("FlowFiles that fail to send will be routed to this relationship")
            .build();
    /**
     * Mapping of the mail properties to the NiFi PropertyDescriptors that will be evaluated at runtime
     */
    private static final Map<String, PropertyDescriptor> propertyToContext = new HashMap<>();

    static {
        propertyToContext.put("mail.smtp.host", SMTP_HOSTNAME);
        propertyToContext.put("mail.smtp.port", SMTP_PORT);
        propertyToContext.put("mail.smtp.socketFactory.port", SMTP_PORT);
        propertyToContext.put("mail.smtp.socketFactory.class", SMTP_SOCKET_FACTORY);
        propertyToContext.put("mail.smtp.auth", SMTP_AUTH);
        propertyToContext.put("mail.smtp.starttls.enable", SMTP_TLS);
        propertyToContext.put("mail.smtp.user", SMTP_USERNAME);
        propertyToContext.put("mail.smtp.password", SMTP_PASSWORD);
    }

    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;
    private volatile Pattern attributeNamePattern = null;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(SMTP_HOSTNAME);
        properties.add(SMTP_PORT);
        properties.add(SMTP_USERNAME);
        properties.add(SMTP_PASSWORD);
        properties.add(SMTP_AUTH);
        properties.add(SMTP_TLS);
        properties.add(SMTP_SOCKET_FACTORY);
        properties.add(HEADER_XMAILER);
        properties.add(ATTRIBUTE_NAME_REGEX);
        properties.add(CONTENT_TYPE);
        properties.add(FROM);
        properties.add(TO);
        properties.add(CC);
        properties.add(BCC);
        properties.add(SUBJECT);
        properties.add(MESSAGE);
        properties.add(GROUP_SIZE);
        this.properties = Collections.unmodifiableList(properties);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    protected Collection<ValidationResult> customValidate(final ValidationContext context) {
        final List<ValidationResult> errors = new ArrayList<>(super.customValidate(context));

        final String to = context.getProperty(TO).getValue();
        final String cc = context.getProperty(CC).getValue();
        final String bcc = context.getProperty(BCC).getValue();

        if (to == null && cc == null && bcc == null) {
            errors.add(new ValidationResult.Builder().subject("To, CC, BCC").valid(false).explanation("Must specify at least one To/CC/BCC address").build());
        }

        return errors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        final String attributeNameRegex = context.getProperty(ATTRIBUTE_NAME_REGEX).getValue();
        this.attributeNamePattern = attributeNameRegex == null ? null : Pattern.compile(attributeNameRegex);
    }

    private void setMessageHeader(final String header, final String value, final Message message) throws MessagingException {
        final ComponentLog logger = getLogger();
        try {
            message.setHeader(header, MimeUtility.encodeText(value));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Unable to add header {} with value {} due to encoding exception", new Object[]{header, value});
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        Integer groupSize = context.getProperty(GROUP_SIZE).asInteger();
        List<FlowFile> flowFiles = session.get(groupSize);
        if (flowFiles == null || flowFiles.size() == 0) {
            return;
        }

        final Properties properties = this.getMailPropertiesFromFlowFile(context);

        final Session mailSession = this.createMailSession(properties);

        final Message message = new MimeMessage(mailSession);
        final ComponentLog logger = getLogger();

        try {
            message.addFrom(toInetAddresses(context, FROM));
            message.setRecipients(RecipientType.TO, toInetAddresses(context, TO));
            message.setRecipients(RecipientType.CC, toInetAddresses(context, CC));
            message.setRecipients(RecipientType.BCC, toInetAddresses(context, BCC));

            this.setMessageHeader("X-Mailer", context.getProperty(HEADER_XMAILER).getValue(), message);
            message.setSubject(context.getProperty(SUBJECT).getValue());
            String messageText;
            if (context.getProperty(MESSAGE) != null && context.getProperty(MESSAGE).getValue() != null) {
                messageText = context.getProperty(MESSAGE).getValue();
            } else {
                messageText = getMessage(flowFiles);
            }

            String contentType = context.getProperty(CONTENT_TYPE).getValue();
            message.setContent(messageText, contentType);
            message.setSentDate(new Date());

            send(message);
            session.transfer(flowFiles, REL_SUCCESS);
            logger.info("Sent email as a result of receiving {}", new Object[]{flowFiles});
        } catch (final ProcessException | MessagingException e) {
            context.yield();
            logger.error("Failed to send email for {}: {}; routing to failure", new Object[]{flowFiles, e.getMessage()}, e);
            session.transfer(flowFiles, REL_FAILURE);
        }
    }

    private String getMessage(final List<FlowFile> flowFiles) {
        String messageText = Util.buildEmailHtmlBodyByAttr(flowFiles);
        return messageText;
    }

    /**
     * Based on the input properties, determine whether an authenticate or unauthenticated session should be used. If authenticated, creates a Password Authenticator for use in sending the email.
     *
     * @param properties mail properties
     * @return session
     */
    private Session createMailSession(final Properties properties) {
        String authValue = properties.getProperty("mail.smtp.auth");
        Boolean auth = Boolean.valueOf(authValue);
        /*
         * Conditionally create a password authenticator if the 'auth' parameter is set.
         */
        final Session mailSession = auth ? Session.getInstance(properties, new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                String username = properties.getProperty("mail.smtp.user"), password = properties.getProperty("mail.smtp.password");
                return new PasswordAuthentication(username, password);
            }
        }) : Session.getInstance(properties); // without auth
        return mailSession;
    }

    /**
     * Uses the mapping of javax.mail properties to NiFi PropertyDescriptors to build the required Properties object to be used for sending this email
     *
     * @param context context
     * @return mail properties
     */
    private Properties getMailPropertiesFromFlowFile(final ProcessContext context) {
        final Properties properties = new Properties();
        final ComponentLog logger = this.getLogger();
        for (Entry<String, PropertyDescriptor> entry : propertyToContext.entrySet()) {
            String value = context.getProperty(entry.getValue()).getValue();
            String property = entry.getKey();
            logger.debug("Evaluated Mail Property: {} with Value: {}", new Object[]{property, value});
            // Nullable values are not allowed, so filter out
            if (null != value) {
                properties.setProperty(property, value);
            }
        }
        return properties;
    }

    /**
     * @param context            the current context
     * @param propertyDescriptor the property to evaluate
     * @return an InternetAddress[] parsed from the supplied property
     * @throws AddressException if the property cannot be parsed to a valid InternetAddress[]
     */
    private InternetAddress[] toInetAddresses(final ProcessContext context, PropertyDescriptor propertyDescriptor) throws AddressException {
        InternetAddress[] parse;
        String value = context.getProperty(propertyDescriptor).getValue();
        if (value == null || value.isEmpty()) {
            if (propertyDescriptor.isRequired()) {
                final String exceptionMsg = "Required property '" + propertyDescriptor.getDisplayName() + "' evaluates to an empty string.";
                throw new AddressException(exceptionMsg);
            } else {
                parse = new InternetAddress[0];
            }
        } else {
            try {
                parse = InternetAddress.parse(value);
            } catch (AddressException e) {
                final String exceptionMsg = "Unable to parse a valid address for property '" + propertyDescriptor.getDisplayName() + "' with value '" + value + "'";
                throw new AddressException(exceptionMsg);
            }
        }
        return parse;
    }

    /**
     * Wrapper for static method {@link Transport#send(Message)} to add testability of this class.
     *
     * @param msg the message to send
     * @throws MessagingException on error
     */
    protected void send(final Message msg) throws MessagingException {
        Transport.send(msg);
    }

}
