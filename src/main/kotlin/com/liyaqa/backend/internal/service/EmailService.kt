package com.liyaqa.backend.internal.service

import com.liyaqa.backend.internal.domain.employee.Permission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Email service for transactional notifications.
 *
 * This service handles all email communications for our internal team.
 * Emails are sent asynchronously to avoid blocking request threads.
 * In production, this integrates with SMTP providers like SendGrid or AWS SES.
 *
 * Design considerations:
 * - HTML templates with plain text fallbacks for accessibility
 * - Comprehensive logging for debugging email delivery issues
 * - Graceful error handling to prevent failures from blocking operations
 * - Security-focused notifications for high-risk actions
 */
@Service
class EmailService(

    private val mailSender: JavaMailSender,

    @Value("\${liyaqa.email.from:noreply@liyaqa.com}")
    private val fromAddress: String,

    @Value("\${liyaqa.email.security-team:security@liyaqa.com}")
    private val securityTeamEmail: String,

    @Value("\${liyaqa.app.base-url:http://localhost:8080}")
    private val baseUrl: String,

    @Value("\${liyaqa.email.enabled:false}")
    private val emailEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a z")
            .withZone(ZoneId.systemDefault())
    }

    fun sendWelcomeEmail(email: String, name: String, temporaryPassword: String) {
        val subject = "Welcome to Liyaqa - Your Account is Ready"
        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Welcome to Liyaqa, $name!</h2>
                    <p>Your internal team account has been created. You can now access the Liyaqa control plane.</p>

                    <div style="background-color: #f8f9fa; border-left: 4px solid #007bff; padding: 15px; margin: 20px 0;">
                        <p><strong>Your temporary password:</strong></p>
                        <code style="background-color: #e9ecef; padding: 5px 10px; border-radius: 3px; font-size: 14px;">$temporaryPassword</code>
                    </div>

                    <p><strong>⚠️ Important:</strong> You will be required to change this password on your first login.</p>

                    <p>
                        <a href="$baseUrl/auth/login"
                           style="display: inline-block; padding: 10px 20px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px;">
                            Login to Your Account
                        </a>
                    </p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        If you did not expect this email, please contact your HR department immediately.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Welcome to Liyaqa, $name!

            Your internal team account has been created.

            Your temporary password: $temporaryPassword

            IMPORTANT: You will be required to change this password on your first login.

            Login at: $baseUrl/auth/login

            If you did not expect this email, please contact your HR department immediately.
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.info("Welcome email sent to $email")
    }

    fun sendPermissionChangeNotification(
        email: String,
        name: String,
        added: Set<Permission>,
        removed: Set<Permission>
    ) {
        val subject = "Your Permissions Have Been Updated"

        val addedList = if (added.isNotEmpty()) {
            "<ul>" + added.joinToString("") { "<li style=\"color: #28a745;\">+ ${it.name}</li>" } + "</ul>"
        } else ""

        val removedList = if (removed.isNotEmpty()) {
            "<ul>" + removed.joinToString("") { "<li style=\"color: #dc3545;\">- ${it.name}</li>" } + "</ul>"
        } else ""

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Permission Update Notification</h2>
                    <p>Hello $name,</p>
                    <p>Your account permissions have been modified by an administrator.</p>

                    ${if (added.isNotEmpty()) "<h3>Permissions Added:</h3>$addedList" else ""}
                    ${if (removed.isNotEmpty()) "<h3>Permissions Removed:</h3>$removedList" else ""}

                    <p style="margin-top: 20px;">
                        These changes are effective immediately. If you have questions about this change,
                        please contact your manager or HR department.
                    </p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        This is an automated notification. If you believe this change was made in error,
                        please contact security@liyaqa.com immediately.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Permission Update Notification

            Hello $name,

            Your account permissions have been modified by an administrator.

            ${if (added.isNotEmpty()) "Permissions Added:\n" + added.joinToString("\n") { "  + ${it.name}" } else ""}
            ${if (removed.isNotEmpty()) "Permissions Removed:\n" + removed.joinToString("\n") { "  - ${it.name}" } else ""}

            These changes are effective immediately.

            If you believe this change was made in error, please contact security@liyaqa.com immediately.
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.info("Permission change notification sent to $email (added: ${added.size}, removed: ${removed.size})")
    }

    fun sendPasswordChangeConfirmation(email: String, name: String) {
        val subject = "Password Changed Successfully"
        val timestamp = DATE_FORMATTER.format(Instant.now())

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Password Changed Successfully</h2>
                    <p>Hello $name,</p>
                    <p>Your password was successfully changed on <strong>$timestamp</strong>.</p>

                    <div style="background-color: #d4edda; border-left: 4px solid #28a745; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>✓ Your account is secure</strong></p>
                        <p style="margin: 5px 0 0 0; font-size: 14px;">All existing sessions have been terminated. You'll need to log in again.</p>
                    </div>

                    <p><strong>⚠️ Didn't make this change?</strong></p>
                    <p>If you did not change your password, your account may be compromised.
                       Please contact the security team immediately at <a href="mailto:$securityTeamEmail">$securityTeamEmail</a>.</p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        This is an automated security notification.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Password Changed Successfully

            Hello $name,

            Your password was successfully changed on $timestamp.

            Your account is secure. All existing sessions have been terminated.

            DIDN'T MAKE THIS CHANGE?
            If you did not change your password, please contact the security team immediately at $securityTeamEmail.
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.info("Password change confirmation sent to $email")
    }

    fun sendEmployeeTerminationNotification(email: String, name: String, terminatedBy: String) {
        val subject = "Account Access Terminated"
        val timestamp = DATE_FORMATTER.format(Instant.now())

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Account Access Terminated</h2>
                    <p>Hello $name,</p>
                    <p>Your Liyaqa account access has been terminated as of <strong>$timestamp</strong>.</p>

                    <div style="background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Your access has been revoked</strong></p>
                        <p style="margin: 5px 0 0 0; font-size: 14px;">You can no longer access the Liyaqa control plane or any internal systems.</p>
                    </div>

                    <p>For questions regarding your account status, please contact HR or your manager.</p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        Terminated by: $terminatedBy<br>
                        Date: $timestamp
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Account Access Terminated

            Hello $name,

            Your Liyaqa account access has been terminated as of $timestamp.

            You can no longer access the Liyaqa control plane or any internal systems.

            For questions regarding your account status, please contact HR or your manager.

            Terminated by: $terminatedBy
            Date: $timestamp
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.info("Termination notification sent to $email (terminated by: $terminatedBy)")
    }

    fun sendLoginNotification(email: String, name: String, ipAddress: String, timestamp: Instant) {
        val subject = "New Login to Your Liyaqa Account"
        val formattedTime = DATE_FORMATTER.format(timestamp)

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">New Login Detected</h2>
                    <p>Hello $name,</p>
                    <p>A new login to your high-privilege Liyaqa account was detected.</p>

                    <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Login Details:</strong></p>
                        <ul style="margin: 10px 0; padding-left: 20px;">
                            <li>Time: $formattedTime</li>
                            <li>IP Address: $ipAddress</li>
                        </ul>
                    </div>

                    <p><strong>Was this you?</strong></p>
                    <p>If you recognize this activity, no action is needed. This is just a security notification
                       because your account has elevated privileges.</p>

                    <p><strong>⚠️ Suspicious activity?</strong></p>
                    <p>If you did not log in at this time, please contact the security team immediately at
                       <a href="mailto:$securityTeamEmail">$securityTeamEmail</a> and change your password.</p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        This notification is sent for all logins to high-privilege accounts as part of our security policy.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            New Login Detected

            Hello $name,

            A new login to your high-privilege Liyaqa account was detected.

            Login Details:
            - Time: $formattedTime
            - IP Address: $ipAddress

            WAS THIS YOU?
            If you recognize this activity, no action is needed.

            SUSPICIOUS ACTIVITY?
            If you did not log in at this time, please contact the security team immediately at $securityTeamEmail and change your password.
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.info("Login notification sent to $email (IP: $ipAddress)")
    }

    fun sendAccountLockNotification(email: String, name: String, unlockTime: Instant?) {
        val subject = "Your Account Has Been Locked"
        val unlockTimeFormatted = unlockTime?.let { DATE_FORMATTER.format(it) } ?: "Contact support"

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #dc3545;">Account Locked</h2>
                    <p>Hello $name,</p>
                    <p>Your Liyaqa account has been temporarily locked due to multiple failed login attempts.</p>

                    <div style="background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>🔒 Account Status: LOCKED</strong></p>
                        <p style="margin: 10px 0 0 0;">
                            ${if (unlockTime != null)
                                "Your account will automatically unlock at <strong>$unlockTimeFormatted</strong>"
                            else
                                "Please contact support to unlock your account"}
                        </p>
                    </div>

                    <p><strong>What to do next:</strong></p>
                    <ul>
                        <li>Wait for the automatic unlock time</li>
                        <li>Or contact support immediately if you need access sooner</li>
                        <li>Review your password and ensure it's secure</li>
                    </ul>

                    <p><strong>⚠️ Didn't attempt to log in?</strong></p>
                    <p>If you did not attempt to log in, someone may be trying to access your account.
                       Please contact the security team at <a href="mailto:$securityTeamEmail">$securityTeamEmail</a> immediately.</p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        This is an automated security measure to protect your account.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Account Locked

            Hello $name,

            Your Liyaqa account has been temporarily locked due to multiple failed login attempts.

            Account Status: LOCKED
            ${if (unlockTime != null) "Unlock time: $unlockTimeFormatted" else "Contact support to unlock"}

            What to do next:
            - Wait for the automatic unlock time
            - Or contact support immediately if you need access sooner
            - Review your password and ensure it's secure

            DIDN'T ATTEMPT TO LOG IN?
            If you did not attempt to log in, please contact the security team at $securityTeamEmail immediately.
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.warn("Account lock notification sent to $email (unlock: ${unlockTime ?: "manual"})")
    }

    fun sendSecurityWarning(email: String, message: String) {
        val subject = "Security Warning: Unusual Activity Detected"
        val timestamp = DATE_FORMATTER.format(Instant.now())

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #dc3545;">⚠️ Security Warning</h2>
                    <p>An unusual activity has been detected on your Liyaqa account.</p>

                    <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Alert Details:</strong></p>
                        <p style="margin: 10px 0 0 0;">$message</p>
                        <p style="margin: 10px 0 0 0; font-size: 12px; color: #6c757d;">Time: $timestamp</p>
                    </div>

                    <p><strong>Recommended Actions:</strong></p>
                    <ul>
                        <li>Review your recent account activity</li>
                        <li>Change your password if you suspect unauthorized access</li>
                        <li>Contact the security team if you need assistance</li>
                    </ul>

                    <p>If you believe your account has been compromised, please contact
                       <a href="mailto:$securityTeamEmail">$securityTeamEmail</a> immediately.</p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        This is an automated security notification.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            SECURITY WARNING

            An unusual activity has been detected on your Liyaqa account.

            Alert Details:
            $message
            Time: $timestamp

            Recommended Actions:
            - Review your recent account activity
            - Change your password if you suspect unauthorized access
            - Contact the security team if you need assistance

            If you believe your account has been compromised, please contact $securityTeamEmail immediately.
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.warn("Security warning sent to $email: $message")
    }

    fun sendSecurityAlert(subject: String, message: String) {
        val timestamp = DATE_FORMATTER.format(Instant.now())

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #dc3545;">🚨 SECURITY ALERT</h2>
                    <h3 style="color: #2c3e50;">$subject</h3>

                    <div style="background-color: #f8d7da; border-left: 4px solid #dc3545; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>Alert Details:</strong></p>
                        <pre style="margin: 10px 0 0 0; white-space: pre-wrap; font-family: monospace; font-size: 13px;">$message</pre>
                        <p style="margin: 10px 0 0 0; font-size: 12px; color: #721c24;">Timestamp: $timestamp</p>
                    </div>

                    <p><strong>IMMEDIATE ACTION REQUIRED</strong></p>
                    <p>This alert requires immediate investigation by the security team.</p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        This is an automated security alert from the Liyaqa control plane.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            SECURITY ALERT

            $subject

            Alert Details:
            $message

            Timestamp: $timestamp

            IMMEDIATE ACTION REQUIRED
            This alert requires immediate investigation by the security team.
        """.trimIndent()

        sendEmail(securityTeamEmail, "🚨 SECURITY ALERT: $subject", htmlContent, plainText)
        logger.error("SECURITY ALERT sent to $securityTeamEmail - $subject: $message")
    }

    fun sendPasswordResetEmail(email: String, name: String, resetToken: String) {
        val subject = "Reset Your Password"
        val resetLink = "$baseUrl/auth/reset-password?token=$resetToken"
        val expiryMinutes = 60 // 1 hour

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Password Reset Request</h2>
                    <p>Hello $name,</p>
                    <p>We received a request to reset your password. Click the button below to create a new password:</p>

                    <p style="margin: 30px 0;">
                        <a href="$resetLink"
                           style="display: inline-block; padding: 12px 30px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;">
                            Reset Password
                        </a>
                    </p>

                    <p>Or copy and paste this link into your browser:</p>
                    <p style="background-color: #f8f9fa; padding: 10px; border-radius: 3px; word-break: break-all; font-size: 12px;">
                        $resetLink
                    </p>

                    <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                        <p style="margin: 0;"><strong>⏰ This link expires in $expiryMinutes minutes</strong></p>
                        <p style="margin: 5px 0 0 0; font-size: 14px;">For security reasons, you can only use this link once.</p>
                    </div>

                    <p><strong>Didn't request this?</strong></p>
                    <p>If you didn't request a password reset, you can safely ignore this email.
                       Your password will remain unchanged.</p>

                    <p style="color: #6c757d; font-size: 12px; margin-top: 30px;">
                        This is an automated message. Please do not reply to this email.
                    </p>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Password Reset Request

            Hello $name,

            We received a request to reset your password.

            Reset your password by visiting this link:
            $resetLink

            This link expires in $expiryMinutes minutes and can only be used once.

            DIDN'T REQUEST THIS?
            If you didn't request a password reset, you can safely ignore this email.
        """.trimIndent()

        sendEmail(email, subject, htmlContent, plainText)
        logger.info("Password reset email sent to $email")
    }

    /**
     * Core email sending logic with error handling.
     *
     * This method handles the actual SMTP communication and provides
     * graceful degradation if email sending fails. We log errors but
     * don't throw exceptions to prevent email issues from breaking
     * critical business operations.
     */
    private fun sendEmail(to: String, subject: String, htmlContent: String, plainText: String) {
        if (!emailEnabled) {
            logger.info("Email disabled - would send to $to: $subject")
            return
        }

        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromAddress)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(plainText, htmlContent)

            mailSender.send(message)
            logger.debug("Email sent successfully to $to")
        } catch (ex: Exception) {
            logger.error("Failed to send email to $to: ${ex.message}", ex)
            // Don't throw - we don't want email failures to break business operations
        }
    }
}