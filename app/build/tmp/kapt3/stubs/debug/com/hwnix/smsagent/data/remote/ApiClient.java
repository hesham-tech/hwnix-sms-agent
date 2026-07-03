package com.hwnix.smsagent.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u0007J\u0006\u0010\b\u001a\u00020\tR\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/hwnix/smsagent/data/remote/ApiClient;", "", "()V", "apiService", "Lcom/hwnix/smsagent/data/remote/ApiService;", "getService", "context", "Landroid/content/Context;", "resetClient", "", "app_debug"})
public final class ApiClient {
    @org.jetbrains.annotations.Nullable()
    private static com.hwnix.smsagent.data.remote.ApiService apiService;
    @org.jetbrains.annotations.NotNull()
    public static final com.hwnix.smsagent.data.remote.ApiClient INSTANCE = null;
    
    private ApiClient() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.hwnix.smsagent.data.remote.ApiService getService(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    /**
     * إعادة تعيين العميل (عند تعديل الـ API Base URL).
     */
    public final void resetClient() {
    }
}