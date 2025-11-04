package com.liyaqa.backend.shared.notification.domain

import com.liyaqa.backend.core.domain.base.BaseEntity
import jakarta.persistence.*

/**
 * Reusable notification templates for consistency and maintainability.
 *
 * Design Rationale:
 * - Centralized template management for brand consistency
 * - Variable substitution for personalization
 * - Multi-language support for internationalization
 * - A/B testing support for optimization
 * - Version control for template changes
 *
 * Template Variables:
 * Use {{variableName}} syntax for variable substitution
 * Example: "Hello {{firstName}}, your booking for {{courtName}} is confirmed."
 */
@Entity
@Table(
    name = "notification_templates",
    indexes = [
        Index(name = "idx_template_code", columnList = "template_code,tenant_id", unique = true),
        Index(name = "idx_template_type", columnList = "notification_type,channel"),
        Index(name = "idx_template_active", columnList = "is_active,tenant_id")
    ]
)
class NotificationTemplate(

    @Column(name = "template_code", nullable = false, length = 100)
    var templateCode: String, // Unique identifier like "booking_confirmation_email"

    @Column(name = "name", nullable = false, length = 255)
    var name: String,

    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 100)
    var notificationType: NotificationType,

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    var channel: NotificationChannel,

    @Column(name = "language", length = 10)
    var language: String = "en", // ISO language code

    // === Content ===

    @Column(name = "subject_template", length = 500)
    var subjectTemplate: String? = null,

    @Column(name = "body_template", columnDefinition = "TEXT", nullable = false)
    var bodyTemplate: String,

    @Column(name = "html_template", columnDefinition = "TEXT")
    var htmlTemplate: String? = null,

    // === Template Metadata ===

    @Column(name = "variables", columnDefinition = "TEXT")
    var variables: String? = null, // JSON array of expected variables

    @Column(name = "example_data", columnDefinition = "TEXT")
    var exampleData: String? = null, // JSON object with example values for testing

    // === Configuration ===

    @Column(name = "is_active")
    var isActive: Boolean = true,

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 50)
    var priority: NotificationPriority = NotificationPriority.MEDIUM,

    @Column(name = "max_retries")
    var maxRetries: Int = 3,

    // === A/B Testing ===

    @Column(name = "ab_test_variant", length = 50)
    var abTestVariant: String? = null, // e.g., "A", "B", "control"

    @Column(name = "ab_test_weight")
    var abTestWeight: Double? = null // Distribution weight 0.0-1.0

) : BaseEntity() {

    fun renderSubject(variables: Map<String, Any>): String {
        return subjectTemplate?.let { render(it, variables) } ?: ""
    }

    fun renderBody(variables: Map<String, Any>): String {
        return render(bodyTemplate, variables)
    }

    fun renderHtml(variables: Map<String, Any>): String? {
        return htmlTemplate?.let { render(it, variables) }
    }

    /**
     * Simple variable substitution using {{variableName}} syntax.
     *
     * For production, consider using a proper template engine like:
     * - Mustache
     * - Handlebars
     * - FreeMarker
     */
    private fun render(template: String, variables: Map<String, Any>): String {
        var result = template

        variables.forEach { (key, value) ->
            val placeholder = "{{$key}}"
            result = result.replace(placeholder, value.toString())
        }

        return result
    }
}
