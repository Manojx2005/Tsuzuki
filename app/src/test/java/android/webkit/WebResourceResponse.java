package android.webkit;

import java.io.InputStream;

/** JVM fixture: the mockable Android constructor does not retain the response stream. */
public class WebResourceResponse {
    private final String mimeType;
    private final String encoding;
    private final InputStream data;

    public WebResourceResponse(String mimeType, String encoding, InputStream data) {
        this.mimeType = mimeType;
        this.encoding = encoding;
        this.data = data;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getEncoding() {
        return encoding;
    }

    public InputStream getData() {
        return data;
    }
}
