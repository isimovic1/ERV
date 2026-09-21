package hr.algebra.workforce.service;

import java.util.List;

public final class AttachmentPolicy {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    public static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("application/pdf", "image/jpeg", "image/png");

    private AttachmentPolicy() {
    }

    public static boolean isAllowedType(String contentType) {
        return contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
    }

    public static String describeAllowedTypes() {
        return "PDF, JPEG ili PNG";
    }
}
