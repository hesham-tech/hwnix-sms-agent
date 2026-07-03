package com.hwnix.smsagent.data.local;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0002\b\u0014\u0018\u0000 $2\u00020\u0001:\u0001$B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\t\u001a\u00020\nJ\u0006\u0010\u000b\u001a\u00020\nJ\b\u0010\f\u001a\u0004\u0018\u00010\u0006J\u0006\u0010\r\u001a\u00020\u0006J\u0006\u0010\u000e\u001a\u00020\u000fJ\u0006\u0010\u0010\u001a\u00020\u0011J\u0006\u0010\u0012\u001a\u00020\u0006J\u0006\u0010\u0013\u001a\u00020\u0006J\u0006\u0010\u0014\u001a\u00020\u000fJ\u0006\u0010\u0015\u001a\u00020\u000fJ\u000e\u0010\u0016\u001a\u00020\n2\u0006\u0010\u0017\u001a\u00020\u0006J\u000e\u0010\u0018\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00020\u0006J\u000e\u0010\u001a\u001a\u00020\n2\u0006\u0010\u001b\u001a\u00020\u000fJ\u000e\u0010\u001c\u001a\u00020\n2\u0006\u0010\u001d\u001a\u00020\u0011J\u000e\u0010\u001e\u001a\u00020\n2\u0006\u0010\u001f\u001a\u00020\u0006J\u000e\u0010 \u001a\u00020\n2\u0006\u0010!\u001a\u00020\u000fJ\u000e\u0010\"\u001a\u00020\n2\u0006\u0010#\u001a\u00020\u000fR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006%"}, d2 = {"Lcom/hwnix/smsagent/data/local/SessionManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "masterKeyAlias", "", "sharedPreferences", "Landroid/content/SharedPreferences;", "clearAuthToken", "", "clearSession", "getAuthToken", "getBaseUrl", "getConfigVersion", "", "getDeviceId", "", "getDeviceUuid", "getLoggingLevel", "getMaxRetry", "getPollingInterval", "saveAuthToken", "token", "saveBaseUrl", "url", "saveConfigVersion", "version", "saveDeviceId", "id", "saveLoggingLevel", "level", "saveMaxRetry", "count", "savePollingInterval", "seconds", "Companion", "app_debug"})
public final class SessionManager {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String masterKeyAlias = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences sharedPreferences = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_BASE_URL = "base_url";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_AUTH_TOKEN = "auth_token";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_DEVICE_ID = "device_id";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_DEVICE_UUID = "device_uuid";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_CONFIG_VERSION = "config_version";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_POLLING_INTERVAL = "polling_interval";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_LOGGING_LEVEL = "logging_level";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_MAX_RETRY = "max_retry";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String DEFAULT_BASE_URL = "https://api-teste.hwnix.com/api/";
    @org.jetbrains.annotations.NotNull()
    public static final com.hwnix.smsagent.data.local.SessionManager.Companion Companion = null;
    
    public SessionManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getBaseUrl() {
        return null;
    }
    
    public final void saveBaseUrl(@org.jetbrains.annotations.NotNull()
    java.lang.String url) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAuthToken() {
        return null;
    }
    
    public final void saveAuthToken(@org.jetbrains.annotations.NotNull()
    java.lang.String token) {
    }
    
    public final void clearAuthToken() {
    }
    
    public final long getDeviceId() {
        return 0L;
    }
    
    public final void saveDeviceId(long id) {
    }
    
    /**
     * الحصول على أو إنشاء UUID فريد للجهاز الحالي.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDeviceUuid() {
        return null;
    }
    
    public final int getConfigVersion() {
        return 0;
    }
    
    public final void saveConfigVersion(int version) {
    }
    
    public final int getPollingInterval() {
        return 0;
    }
    
    public final void savePollingInterval(int seconds) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLoggingLevel() {
        return null;
    }
    
    public final void saveLoggingLevel(@org.jetbrains.annotations.NotNull()
    java.lang.String level) {
    }
    
    public final int getMaxRetry() {
        return 0;
    }
    
    public final void saveMaxRetry(int count) {
    }
    
    public final void clearSession() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\t\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/hwnix/smsagent/data/local/SessionManager$Companion;", "", "()V", "DEFAULT_BASE_URL", "", "KEY_AUTH_TOKEN", "KEY_BASE_URL", "KEY_CONFIG_VERSION", "KEY_DEVICE_ID", "KEY_DEVICE_UUID", "KEY_LOGGING_LEVEL", "KEY_MAX_RETRY", "KEY_POLLING_INTERVAL", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}