package com.hwnix.smsagent.data.remote;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\t\n\u0002\b\u000f\bf\u0018\u00002\u00020\u0001J(\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\nJ2\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\f\u001a\u00020\r2\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\nJ\u001e\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0011\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u0012J\u001e\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0011\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u0012J\u001e\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0015J\u001e\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0015J(\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u001e\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\u0015J(\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\bJ(\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\bJ(\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\b\u00a8\u0006\u001c"}, d2 = {"Lcom/hwnix/smsagent/data/remote/ApiService;", "", "batchSyncSms", "Lretrofit2/Response;", "Lcom/google/gson/JsonObject;", "idempotencyKey", "", "body", "(Ljava/lang/String;Lcom/google/gson/JsonObject;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "checkAppUpdate", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "executeCommand", "commandId", "", "(JLjava/lang/String;Lcom/google/gson/JsonObject;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getDeviceConfig", "getDeviceLines", "deviceId", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getPendingCommands", "login", "(Lcom/google/gson/JsonObject;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refreshToken", "registerDevice", "sendHeartbeat", "syncLines", "syncSmsStatus", "uploadIncomingSms", "app_release"})
public abstract interface ApiService {
    
    @retrofit2.http.GET(value = "v1/agent/public/app-update/check")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object checkAppUpdate(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/auth/login")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object login(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/auth/refresh")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object refreshToken(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/device/register")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object registerDevice(@retrofit2.http.Header(value = "Idempotency-Key")
    @org.jetbrains.annotations.NotNull()
    java.lang.String idempotencyKey, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/device/sync-lines")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncLines(@retrofit2.http.Header(value = "Idempotency-Key")
    @org.jetbrains.annotations.NotNull()
    java.lang.String idempotencyKey, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/device/heartbeat")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object sendHeartbeat(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.GET(value = "v1/agent/device/config")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDeviceConfig(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.GET(value = "v1/agent/device/lines")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getDeviceLines(@retrofit2.http.Query(value = "device_id")
    long deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.GET(value = "v1/agent/commands/pending")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPendingCommands(@retrofit2.http.Query(value = "device_id")
    long deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/commands/{id}/execute")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object executeCommand(@retrofit2.http.Path(value = "id")
    long commandId, @retrofit2.http.Header(value = "Idempotency-Key")
    @org.jetbrains.annotations.NotNull()
    java.lang.String idempotencyKey, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/sms/incoming")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object uploadIncomingSms(@retrofit2.http.Header(value = "Idempotency-Key")
    @org.jetbrains.annotations.NotNull()
    java.lang.String idempotencyKey, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/sms/sync-status")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncSmsStatus(@retrofit2.http.Header(value = "Idempotency-Key")
    @org.jetbrains.annotations.NotNull()
    java.lang.String idempotencyKey, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
    
    @retrofit2.http.POST(value = "v1/agent/sms/batch-sync")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object batchSyncSms(@retrofit2.http.Header(value = "Idempotency-Key")
    @org.jetbrains.annotations.NotNull()
    java.lang.String idempotencyKey, @retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.google.gson.JsonObject body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.google.gson.JsonObject>> $completion);
}