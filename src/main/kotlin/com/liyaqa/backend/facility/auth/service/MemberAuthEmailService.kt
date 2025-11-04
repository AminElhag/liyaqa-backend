package com.liyaqa.backend.facility.auth.service

import com.liyaqa.backend.facility.membership.domain.Member
import com.liyaqa.backend.internal.shared.config.EmailService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Email service for member authentication flows.
 *
 * Sends emails for:
 * - Email verification
 * - Password reset
 * - Welcome message
 * - Security notifications
 */
@Service
class MemberAuthEmailService(
    private val emailService: EmailService,
    @Value("\${liyaqa.app.base-url}")
    private val baseUrl: String,
    @Value("\${liyaqa.email.from}")
    private val fromEmail: String
) {

    /**
     * Send email verification email.
     */
    @Async
    fun sendVerificationEmail(member: Member, token: String) {
        val verificationUrl = "$baseUrl/member/verify-email?token=$token"

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background-color: #4CAF50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }
                    .content {
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 24px;
                        background-color: #4CAF50;
                        color: white;
                        text-decoration: none;
                        border-radius: 4px;
                        margin: 20px 0;
                    }
                    .footer {
                        text-align: center;
                        padding: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to ${member.facility.name}!</h1>
                    </div>
                    <div class="content">
                        <h2>Hi ${member.firstName},</h2>
                        <p>Thank you for creating an account with us. To complete your registration, please verify your email address.</p>

                        <p style="text-align: center;">
                            <a href="$verificationUrl" class="button">Verify Email Address</a>
                        </p>

                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #666;">$verificationUrl</p>

                        <p>This link will expire in 24 hours.</p>

                        <p>If you didn't create this account, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>${member.facility.name}</p>
                        <p>${member.branch.addressLine1}${if (member.branch.addressLine2 != null) ", ${member.branch.addressLine2}" else ""}</p>
                        <p>${member.branch.city}, ${member.branch.postalCode}</p>
                        <p>${member.branch.contactEmail} | ${member.branch.contactPhone ?: ""}</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val plainText = """
            Welcome to ${member.facility.name}!

            Hi ${member.firstName},

            Thank you for creating an account with us. To complete your registration, please verify your email address by clicking the link below:

            $verificationUrl

            This link will expire in 24 hours.

            If you didn't create this account, please ignore this email.

            ${member.facility.name}
            ${member.branch.addressLine1}${if (member.branch.addressLine2 != null) ", ${member.branch.addressLine2}" else ""}
            ${member.branch.city}, ${member.branch.postalCode}
        """.trimIndent()

        emailService.sendEmail(
            to = member.email,
            subject = "Verify your email address - ${member.facility.name}",
            htmlContent = html,
            plainText = plainText
        )
    }

    /**
     * Send password reset email.
     */
    @Async
    fun sendPasswordResetEmail(member: Member, token: String) {
        val resetUrl = "$baseUrl/member/reset-password?token=$token"

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background-color: #FF9800;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }
                    .content {
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 24px;
                        background-color: #FF9800;
                        color: white;
                        text-decoration: none;
                        border-radius: 4px;
                        margin: 20px 0;
                    }
                    .warning {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 12px;
                        margin: 20px 0;
                    }
                    .footer {
                        text-align: center;
                        padding: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <h2>Hi ${member.firstName},</h2>
                        <p>We received a request to reset your password for your account at ${member.facility.name}.</p>

                        <p style="text-align: center;">
                            <a href="$resetUrl" class="button">Reset Password</a>
                        </p>

                        <p>Or copy and paste this link into your browser:</p>
                        <p style="word-break: break-all; color: #666;">$resetUrl</p>

                        <div class="warning">
                            <strong>Important:</strong> This link will expire in 1 hour.
                        </div>

                        <p>If you didn't request a password reset, please ignore this email or contact us if you have concerns about your account security.</p>
                    </div>
                    <div class="footer">
                        <p>${member.facility.name}</p>
                        <p>${member.branch.contactEmail}</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val plainText = """
            Password Reset Request

            Hi ${member.firstName},

            We received a request to reset your password for your account at ${member.facility.name}.

            To reset your password, click the link below:

            $resetUrl

            This link will expire in 1 hour.

            If you didn't request a password reset, please ignore this email or contact us if you have concerns about your account security.

            ${member.facility.name}
            ${member.branch.contactEmail}
        """.trimIndent()

        emailService.sendEmail(
            to = member.email,
            subject = "Reset your password - ${member.facility.name}",
            htmlContent = html,
            plainText = plainText
        )
    }

    /**
     * Send welcome email after successful registration and verification.
     */
    @Async
    fun sendWelcomeEmail(member: Member) {
        val loginUrl = "$baseUrl/member/login"

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background-color: #2196F3;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }
                    .content {
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 24px;
                        background-color: #2196F3;
                        color: white;
                        text-decoration: none;
                        border-radius: 4px;
                        margin: 20px 0;
                    }
                    .feature {
                        padding: 12px;
                        margin: 10px 0;
                        background-color: white;
                        border-left: 4px solid #2196F3;
                    }
                    .footer {
                        text-align: center;
                        padding: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to ${member.facility.name}!</h1>
                    </div>
                    <div class="content">
                        <h2>Hi ${member.firstName},</h2>
                        <p>Your email has been verified and your account is now active!</p>

                        <h3>What you can do now:</h3>

                        <div class="feature">
                            <strong>📅 Book Courts</strong><br>
                            Reserve your favorite courts online, anytime.
                        </div>

                        <div class="feature">
                            <strong>💳 Manage Memberships</strong><br>
                            View and manage your membership plans and benefits.
                        </div>

                        <div class="feature">
                            <strong>📊 Track Bookings</strong><br>
                            View your booking history and upcoming reservations.
                        </div>

                        <p style="text-align: center;">
                            <a href="$loginUrl" class="button">Login to Your Account</a>
                        </p>

                        <p>If you have any questions, feel free to contact us at ${member.branch.contactEmail} or call us at ${member.branch.contactPhone ?: "the facility"}.</p>
                    </div>
                    <div class="footer">
                        <p>${member.facility.name}</p>
                        <p>${member.branch.addressLine1}${if (member.branch.addressLine2 != null) ", ${member.branch.addressLine2}" else ""}</p>
                        <p>${member.branch.city}, ${member.branch.postalCode}</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val plainText = """
            Welcome to ${member.facility.name}!

            Hi ${member.firstName},

            Your email has been verified and your account is now active!

            What you can do now:
            - Book Courts: Reserve your favorite courts online, anytime
            - Manage Memberships: View and manage your membership plans and benefits
            - Track Bookings: View your booking history and upcoming reservations

            Login to your account: $loginUrl

            If you have any questions, feel free to contact us at ${member.branch.contactEmail}

            ${member.facility.name}
            ${member.branch.addressLine1}${if (member.branch.addressLine2 != null) ", ${member.branch.addressLine2}" else ""}
            ${member.branch.city}, ${member.branch.postalCode}
        """.trimIndent()

        emailService.sendEmail(
            to = member.email,
            subject = "Welcome to ${member.facility.name}!",
            htmlContent = html,
            plainText = plainText
        )
    }

    /**
     * Send account locked notification.
     */
    @Async
    fun sendAccountLockedEmail(member: Member) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                    }
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background-color: #f44336;
                        color: white;
                        padding: 20px;
                        text-align: center;
                    }
                    .content {
                        padding: 20px;
                        background-color: #f9f9f9;
                    }
                    .warning {
                        background-color: #ffebee;
                        border-left: 4px solid #f44336;
                        padding: 12px;
                        margin: 20px 0;
                    }
                    .footer {
                        text-align: center;
                        padding: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Security Alert</h1>
                    </div>
                    <div class="content">
                        <h2>Hi ${member.firstName},</h2>

                        <div class="warning">
                            <strong>Your account has been temporarily locked</strong> due to multiple failed login attempts.
                        </div>

                        <p>This is a security measure to protect your account. Your account will be automatically unlocked in 30 minutes.</p>

                        <p>If this wasn't you, please contact us immediately at ${member.branch.contactEmail}</p>
                    </div>
                    <div class="footer">
                        <p>${member.facility.name}</p>
                        <p>${member.branch.contactEmail}</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val plainText = """
            Security Alert

            Hi ${member.firstName},

            Your account has been temporarily locked due to multiple failed login attempts.

            This is a security measure to protect your account. Your account will be automatically unlocked in 30 minutes.

            If this wasn't you, please contact us immediately at ${member.branch.contactEmail}

            ${member.facility.name}
            ${member.branch.contactEmail}
        """.trimIndent()

        emailService.sendEmail(
            to = member.email,
            subject = "Security Alert - Account Locked",
            htmlContent = html,
            plainText = plainText
        )
    }
}
