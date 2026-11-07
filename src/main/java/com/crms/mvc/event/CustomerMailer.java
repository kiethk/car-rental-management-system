package com.crms.mvc.event;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

/**
 * Renders a Thymeleaf mail template and sends it to a customer. Mail is optional in this project: callers check
 * {@link #isConfigured()} and simply skip when Gmail is not set up.
 */
@Component
public class CustomerMailer {
    private static final Locale VIETNAMESE = Locale.of("vi", "VN");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ITemplateEngine templateEngine;
    private final String username;
    private final String password;
    private final String fromAddress;
    private final String baseUrl;

    public CustomerMailer(ObjectProvider<JavaMailSender> mailSender,
                          ITemplateEngine templateEngine,
                          @Value("${spring.mail.username:}") String username,
                          @Value("${spring.mail.password:}") String password,
                          @Value("${crms.mail.from:}") String fromAddress,
                          @Value("${crms.base-url:http://localhost:8081}") String baseUrl) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.username = username;
        this.password = password;
        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return mailSender.getIfAvailable() != null && username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    /** Absolute link into the app for buttons in emails, e.g. url("/bookings/42"). */
    public String url(String path) {
        return baseUrl + path;
    }

    public void send(String to, String subject, String template, Map<String, Object> variables) throws Exception {
        JavaMailSender sender = mailSender.getObject();
        Context context = new Context(VIETNAMESE);
        variables.forEach(context::setVariable);
        String body = templateEngine.process(template, context);

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress == null || fromAddress.isBlank() ? "DriveNow <" + username + ">" : fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        sender.send(message);
    }
}
