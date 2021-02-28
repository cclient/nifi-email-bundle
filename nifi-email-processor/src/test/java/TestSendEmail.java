import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage.RecipientType;

import com.github.cclient.nifi.email.SendEmail;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

public class TestSendEmail {

    SendEmailExtension processor;
    TestRunner runner;

    @Before
    public void setup() {
        processor = new SendEmailExtension();
        runner = TestRunners.newTestRunner(processor);
    }

    @Test
    public void testExceptionWhenSending() {
        // verifies that files are routed to failure when Transport.send() throws a MessagingException
        runner.setProperty(SendEmail.SMTP_HOSTNAME, "host-doesnt-exist123");
        runner.setProperty(SendEmail.FROM, "test@apache.org");
        runner.setProperty(SendEmail.TO, "test@apache.org");
        runner.setProperty(SendEmail.MESSAGE, "Message Body");

        processor.setException(new MessagingException("Forced failure from send()"));

        final Map<String, String> attributes = new HashMap<>();
        runner.enqueue("Some Text".getBytes(), attributes);

        runner.run();

        runner.assertQueueEmpty();
        runner.assertAllFlowFilesTransferred(SendEmail.REL_FAILURE);
        assertEquals("Expected an attempt to send a single message", 1, processor.getMessages().size());
    }

    @Test
    public void testOutgoingMessage() throws Exception {
        // verifies that are set on the outgoing Message correctly
        runner.setProperty(SendEmail.SMTP_HOSTNAME, "smtp-host");
        runner.setProperty(SendEmail.HEADER_XMAILER, "TestingNiFi");
        runner.setProperty(SendEmail.FROM, "test@apache.org");
        runner.setProperty(SendEmail.MESSAGE, "Message Body");
        runner.setProperty(SendEmail.TO, "recipient@apache.org");

        runner.enqueue("Some Text".getBytes());

        runner.run();

        runner.assertQueueEmpty();
        runner.assertAllFlowFilesTransferred(SendEmail.REL_SUCCESS);

        // Verify that the Message was populated correctly
        assertEquals("Expected a single message to be sent", 1, processor.getMessages().size());
        Message message = processor.getMessages().get(0);
        assertEquals("test@apache.org", message.getFrom()[0].toString());
        assertEquals("X-Mailer Header", "TestingNiFi", message.getHeader("X-Mailer")[0]);
        assertEquals("Message Body", message.getContent());
        assertEquals("recipient@apache.org", message.getRecipients(RecipientType.TO)[0].toString());
        assertNull(message.getRecipients(RecipientType.BCC));
        assertNull(message.getRecipients(RecipientType.CC));
    }

    @Test
    public void testInvalidAddress() throws Exception {
        // verifies that unparsable addresses lead to the flow file being routed to failure
        runner.setProperty(SendEmail.SMTP_HOSTNAME, "smtp-host");
        runner.setProperty(SendEmail.HEADER_XMAILER, "TestingNiFi");
        runner.setProperty(SendEmail.FROM, "test@apache.org <invalid");
        runner.setProperty(SendEmail.MESSAGE, "Message Body");
        runner.setProperty(SendEmail.TO, "recipient@apache.org");

        runner.enqueue("Some Text".getBytes());

        runner.run();

        runner.assertQueueEmpty();
        runner.assertAllFlowFilesTransferred(SendEmail.REL_FAILURE);

        assertEquals("Expected no messages to be sent", 0, processor.getMessages().size());
    }

    /**
     * Extension to SendEmail that stubs out the calls to
     * Transport.sendMessage().
     *
     * <p>
     * All sent messages are records in a list available via the
     * {@link #getMessages()} method.</p>
     * <p> Calling
     * {@link #setException(MessagingException)} will cause the supplied exception to be
     * thrown when sendMessage is invoked.
     * </p>
     */
    private static final class SendEmailExtension extends SendEmail {
        private MessagingException e;
        private ArrayList<Message> messages = new ArrayList<>();

        @Override
        protected void send(Message msg) throws MessagingException {
            messages.add(msg);
            if (this.e != null) {
                throw e;
            }
        }

        void setException(final MessagingException e) {
            this.e = e;
        }

        List<Message> getMessages() {
            return messages;
        }
    }

}