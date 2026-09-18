package net.trackme.sso.services;

/**
 * Email address and display name of a notification recipient.
 *
 * @param email recipient email address
 * @param fullName recipient display name
 */
public record EmailRecipient(String email, String fullName) {
}
