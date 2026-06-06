package com.ecommerce.ecommercebackend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A historical customer review captured from the TGDD crawl
 * ({@code customer_reviews}). This is a read-only snapshot of the source site's
 * reviews — distinct from any in-app review feature planned for later phases.
 * Embedded as an element collection of {@link Product}.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawledReview {

    @JsonProperty("reviewer_name")
    @Column(name = "reviewer_name", length = 150)
    private String reviewerName;

    /** Raw publish date string from the source, e.g. "5/26/2026 9:53:42 PM". */
    @JsonProperty("publish_date")
    @Column(name = "publish_date", length = 100)
    private String publishDate;

    @JsonProperty("review_content")
    @Column(name = "review_content", columnDefinition = "TEXT")
    private String reviewContent;

    @JsonProperty("rating_stars")
    @Column(name = "rating_stars")
    private Double ratingStars;
}
