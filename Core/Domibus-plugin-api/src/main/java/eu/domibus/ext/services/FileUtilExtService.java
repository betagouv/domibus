package eu.domibus.ext.services;

/**
 * File Utils service which contains methods like sanitization of the filename for payloads
 *
 * @since 4.1.4
 * @author Catalin Enache
 */
public interface FileUtilExtService {

    /**
     * Sanitizes file name by removing any references (prefixes) to full or relative path
     * @param fileName file name to be sanitized
     * @return sanitized value of the file name, null if sanitization fails
     */
    String sanitizeFileName(String fileName);

    /**
     * Translates a string into application/x-www-form-urlencoded format using a specific encoding scheme.
     *
     * @param s String to be translated
     * @return the translated String.
     */
    String urlEncode(String s);
}
