package com.liyaqa.backend.facility.booking.service

import com.liyaqa.backend.facility.booking.domain.Booking
import com.liyaqa.backend.internal.shared.config.EmailService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Email notification service for booking-related communications.
 *
 * Handles all customer-facing emails for the booking lifecycle including:
 * - Booking confirmations
 * - Cancellation notifications
 * - Check-in reminders
 * - No-show notifications
 *
 * All emails are sent asynchronously to avoid blocking booking operations.
 */
@Service
class BookingEmailService(
    private val emailService: EmailService,

    @Value("\${liyaqa.app.base-url:http://localhost:8080}")
    private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
        private val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a")
    }

    /**
     * Send booking confirmation email.
     */
    @Async
    fun sendBookingConfirmation(booking: Booking) {
        val memberEmail = booking.member.email
        val memberName = booking.member.getFullName()
        val courtName = booking.court.name
        val dateTime = booking.startTime.format(DATETIME_FORMATTER)
        val duration = booking.getDurationString()
        val price = "${booking.currency} ${booking.finalPrice}"

        val subject = "Booking Confirmed - ${booking.bookingNumber}"

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #28a745; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0;">✓ Booking Confirmed</h1>
                    </div>

                    <div style="padding: 30px; background-color: #f8f9fa;">
                        <p>Hello ${memberName},</p>
                        <p>Your court booking has been confirmed! Here are your booking details:</p>

                        <div style="background-color: white; border-left: 4px solid #28a745; padding: 20px; margin: 20px 0;">
                            <h2 style="margin-top: 0; color: #28a745;">Booking Details</h2>
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Booking Number:</td>
                                    <td style="padding: 8px 0;">${booking.bookingNumber}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Court:</td>
                                    <td style="padding: 8px 0;">${courtName}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Date & Time:</td>
                                    <td style="padding: 8px 0;">${dateTime}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Duration:</td>
                                    <td style="padding: 8px 0;">${duration}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Total Price:</td>
                                    <td style="padding: 8px 0; font-weight: bold; color: #28a745;">${price}</td>
                                </tr>
                                ${if (booking.membership != null) """
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Membership:</td>
                                    <td style="padding: 8px 0;">${booking.membership!!.membershipNumber} (${booking.membership!!.plan.name})</td>
                                </tr>
                                """ else ""}
                                ${if (booking.discountAmount.compareTo(java.math.BigDecimal.ZERO) > 0) """
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Discount:</td>
                                    <td style="padding: 8px 0; color: #28a745;">-${booking.currency} ${booking.discountAmount}</td>
                                </tr>
                                """ else ""}
                            </table>
                        </div>

                        ${if (booking.specialRequests != null) """
                        <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                            <strong>Special Requests:</strong>
                            <p style="margin: 5px 0 0 0;">${booking.specialRequests}</p>
                        </div>
                        """ else ""}

                        <div style="background-color: #d1ecf1; border-left: 4px solid #0c5460; padding: 15px; margin: 20px 0;">
                            <strong>⚠️ Cancellation Policy:</strong>
                            <p style="margin: 5px 0 0 0;">
                                Free cancellation up to ${booking.court.cancellationHours} hours before your booking time.
                            </p>
                        </div>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="${baseUrl}/bookings/${booking.id}"
                               style="display: inline-block; padding: 15px 30px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;">
                                View Booking Details
                            </a>
                        </div>

                        <p style="margin-top: 30px; color: #6c757d; font-size: 14px;">
                            Please arrive 10 minutes before your booking time for check-in.<br>
                            If you need to make any changes, please contact us or manage your booking online.
                        </p>
                    </div>

                    <div style="background-color: #e9ecef; padding: 20px; text-align: center; font-size: 12px; color: #6c757d;">
                        <p>Thank you for choosing ${booking.branch.name}!</p>
                        <p>If you have any questions, please contact us at ${booking.branch.contactEmail ?: booking.facility.contactEmail}</p>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Booking Confirmed

            Hello ${memberName},

            Your court booking has been confirmed!

            Booking Details:
            - Booking Number: ${booking.bookingNumber}
            - Court: ${courtName}
            - Date & Time: ${dateTime}
            - Duration: ${duration}
            - Total Price: ${price}
            ${if (booking.membership != null) "- Membership: ${booking.membership!!.membershipNumber}\n" else ""}
            ${if (booking.discountAmount.compareTo(java.math.BigDecimal.ZERO) > 0) "- Discount: -${booking.currency} ${booking.discountAmount}\n" else ""}

            ${if (booking.specialRequests != null) "Special Requests: ${booking.specialRequests}\n\n" else ""}

            Cancellation Policy:
            Free cancellation up to ${booking.court.cancellationHours} hours before your booking time.

            Please arrive 10 minutes before your booking time for check-in.

            View your booking: ${baseUrl}/bookings/${booking.id}

            Thank you for choosing ${booking.branch.name}!
        """.trimIndent()

        try {
            emailService.sendEmail(memberEmail, subject, htmlContent, plainText)
            logger.info("Booking confirmation email sent for booking ${booking.bookingNumber}")
        } catch (e: Exception) {
            logger.error("Failed to send booking confirmation email for ${booking.bookingNumber}", e)
        }
    }

    /**
     * Send booking cancellation email.
     */
    @Async
    fun sendCancellationNotification(booking: Booking) {
        val memberEmail = booking.member.email
        val memberName = booking.member.getFullName()
        val courtName = booking.court.name
        val dateTime = booking.startTime.format(DATETIME_FORMATTER)
        val refundInfo = if (booking.refundAmount != null && booking.refundAmount!!.compareTo(java.math.BigDecimal.ZERO) > 0) {
            "A refund of ${booking.currency} ${booking.refundAmount} will be processed within 5-7 business days."
        } else {
            "No refund is applicable for this cancellation."
        }

        val subject = "Booking Cancelled - ${booking.bookingNumber}"

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #dc3545; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0;">✗ Booking Cancelled</h1>
                    </div>

                    <div style="padding: 30px; background-color: #f8f9fa;">
                        <p>Hello ${memberName},</p>
                        <p>Your booking has been cancelled as requested.</p>

                        <div style="background-color: white; border-left: 4px solid #dc3545; padding: 20px; margin: 20px 0;">
                            <h2 style="margin-top: 0; color: #dc3545;">Cancelled Booking</h2>
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Booking Number:</td>
                                    <td style="padding: 8px 0;">${booking.bookingNumber}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Court:</td>
                                    <td style="padding: 8px 0;">${courtName}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Date & Time:</td>
                                    <td style="padding: 8px 0;">${dateTime}</td>
                                </tr>
                                ${if (booking.cancellationReason != null) """
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Reason:</td>
                                    <td style="padding: 8px 0;">${booking.cancellationReason}</td>
                                </tr>
                                """ else ""}
                            </table>
                        </div>

                        <div style="background-color: #d1ecf1; border-left: 4px solid #0c5460; padding: 15px; margin: 20px 0;">
                            <strong>Refund Information:</strong>
                            <p style="margin: 5px 0 0 0;">${refundInfo}</p>
                        </div>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="${baseUrl}/courts"
                               style="display: inline-block; padding: 15px 30px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;">
                                Make a New Booking
                            </a>
                        </div>

                        <p style="margin-top: 30px; color: #6c757d; font-size: 14px;">
                            We hope to see you again soon! If you have any questions about this cancellation,
                            please don't hesitate to contact us.
                        </p>
                    </div>

                    <div style="background-color: #e9ecef; padding: 20px; text-align: center; font-size: 12px; color: #6c757d;">
                        <p>Thank you for choosing ${booking.branch.name}!</p>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Booking Cancelled

            Hello ${memberName},

            Your booking has been cancelled.

            Cancelled Booking Details:
            - Booking Number: ${booking.bookingNumber}
            - Court: ${courtName}
            - Date & Time: ${dateTime}
            ${if (booking.cancellationReason != null) "- Reason: ${booking.cancellationReason}\n" else ""}

            Refund Information:
            ${refundInfo}

            Make a new booking: ${baseUrl}/courts

            Thank you for choosing ${booking.branch.name}!
        """.trimIndent()

        try {
            emailService.sendEmail(memberEmail, subject, htmlContent, plainText)
            logger.info("Cancellation email sent for booking ${booking.bookingNumber}")
        } catch (e: Exception) {
            logger.error("Failed to send cancellation email for ${booking.bookingNumber}", e)
        }
    }

    /**
     * Send booking reminder (24 hours before).
     */
    @Async
    fun sendBookingReminder(booking: Booking) {
        val memberEmail = booking.member.email
        val memberName = booking.member.getFullName()
        val courtName = booking.court.name
        val date = booking.bookingDate.format(DATE_FORMATTER)
        val time = booking.startTime.format(TIME_FORMATTER)

        val subject = "Reminder: Your Booking Tomorrow at ${time}"

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #ffc107; padding: 20px; text-align: center;">
                        <h1 style="color: #333; margin: 0;">🔔 Booking Reminder</h1>
                    </div>

                    <div style="padding: 30px; background-color: #f8f9fa;">
                        <p>Hello ${memberName},</p>
                        <p>This is a friendly reminder about your upcoming booking tomorrow!</p>

                        <div style="background-color: white; border-left: 4px solid #ffc107; padding: 20px; margin: 20px 0; text-align: center;">
                            <h2 style="margin-top: 0; color: #ffc107;">Tomorrow's Booking</h2>
                            <div style="font-size: 18px; margin: 15px 0;">
                                <strong>${courtName}</strong>
                            </div>
                            <div style="font-size: 16px; color: #6c757d;">
                                ${date}
                            </div>
                            <div style="font-size: 24px; font-weight: bold; color: #333; margin: 10px 0;">
                                ${time}
                            </div>
                            <div style="font-size: 14px; color: #6c757d;">
                                Booking #${booking.bookingNumber}
                            </div>
                        </div>

                        <div style="background-color: #d1ecf1; border-left: 4px solid #0c5460; padding: 15px; margin: 20px 0;">
                            <strong>📍 Arrival Instructions:</strong>
                            <p style="margin: 5px 0 0 0;">
                                Please arrive 10 minutes early for check-in at ${booking.branch.name}.
                            </p>
                        </div>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="${baseUrl}/bookings/${booking.id}"
                               style="display: inline-block; padding: 15px 30px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 5px;">
                                View Booking
                            </a>
                            <a href="${baseUrl}/bookings/${booking.id}/cancel"
                               style="display: inline-block; padding: 15px 30px; background-color: #6c757d; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; margin: 5px;">
                                Cancel Booking
                            </a>
                        </div>

                        <p style="margin-top: 30px; color: #6c757d; font-size: 14px; text-align: center;">
                            Looking forward to seeing you tomorrow!
                        </p>
                    </div>

                    <div style="background-color: #e9ecef; padding: 20px; text-align: center; font-size: 12px; color: #6c757d;">
                        <p>${booking.branch.name}</p>
                        <p>${booking.branch.addressLine1}${if (booking.branch.addressLine2 != null) ", ${booking.branch.addressLine2}" else ""}</p>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Booking Reminder

            Hello ${memberName},

            This is a reminder about your booking tomorrow:

            - Court: ${courtName}
            - Date: ${date}
            - Time: ${time}
            - Booking Number: ${booking.bookingNumber}

            Please arrive 10 minutes early for check-in at ${booking.branch.name}.

            View booking: ${baseUrl}/bookings/${booking.id}
            Cancel booking: ${baseUrl}/bookings/${booking.id}/cancel

            Looking forward to seeing you tomorrow!
        """.trimIndent()

        try {
            emailService.sendEmail(memberEmail, subject, htmlContent, plainText)
            logger.info("Reminder email sent for booking ${booking.bookingNumber}")
        } catch (e: Exception) {
            logger.error("Failed to send reminder email for ${booking.bookingNumber}", e)
        }
    }

    /**
     * Send no-show notification.
     */
    @Async
    fun sendNoShowNotification(booking: Booking) {
        val memberEmail = booking.member.email
        val memberName = booking.member.getFullName()
        val courtName = booking.court.name
        val dateTime = booking.startTime.format(DATETIME_FORMATTER)

        val subject = "Missed Booking - ${booking.bookingNumber}"

        val htmlContent = """
            <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto;">
                    <div style="background-color: #6c757d; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0;">Missed Booking</h1>
                    </div>

                    <div style="padding: 30px; background-color: #f8f9fa;">
                        <p>Hello ${memberName},</p>
                        <p>We noticed you didn't check in for your booking:</p>

                        <div style="background-color: white; border-left: 4px solid #6c757d; padding: 20px; margin: 20px 0;">
                            <table style="width: 100%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Booking Number:</td>
                                    <td style="padding: 8px 0;">${booking.bookingNumber}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Court:</td>
                                    <td style="padding: 8px 0;">${courtName}</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; font-weight: bold;">Date & Time:</td>
                                    <td style="padding: 8px 0;">${dateTime}</td>
                                </tr>
                            </table>
                        </div>

                        <p style="color: #6c757d;">
                            If this was a mistake or you experienced any issues, please contact us.
                            We hope to see you at your next booking!
                        </p>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="${baseUrl}/courts"
                               style="display: inline-block; padding: 15px 30px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; font-weight: bold;">
                                Make a New Booking
                            </a>
                        </div>
                    </div>

                    <div style="background-color: #e9ecef; padding: 20px; text-align: center; font-size: 12px; color: #6c757d;">
                        <p>Contact us: ${booking.branch.contactEmail ?: booking.facility.contactEmail}</p>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val plainText = """
            Missed Booking

            Hello ${memberName},

            We noticed you didn't check in for your booking:

            - Booking Number: ${booking.bookingNumber}
            - Court: ${courtName}
            - Date & Time: ${dateTime}

            If this was a mistake or you experienced any issues, please contact us.
            We hope to see you at your next booking!

            Make a new booking: ${baseUrl}/courts
        """.trimIndent()

        try {
            emailService.sendEmail(memberEmail, subject, htmlContent, plainText)
            logger.info("No-show notification sent for booking ${booking.bookingNumber}")
        } catch (e: Exception) {
            logger.error("Failed to send no-show notification for ${booking.bookingNumber}", e)
        }
    }
}
