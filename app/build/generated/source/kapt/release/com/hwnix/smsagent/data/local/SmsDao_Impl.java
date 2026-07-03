package com.hwnix.smsagent.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SmsDao_Impl implements SmsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SmsEntity> __insertionAdapterOfSmsEntity;

  private final EntityDeletionOrUpdateAdapter<SmsEntity> __updateAdapterOfSmsEntity;

  private final SharedSQLiteStatement __preparedStmtOfCleanOldLogs;

  public SmsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSmsEntity = new EntityInsertionAdapter<SmsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sms_cache` (`id`,`phoneNumber`,`messageBody`,`direction`,`status`,`messageRef`,`subscriptionId`,`sentAt`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SmsEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getPhoneNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPhoneNumber());
        }
        if (entity.getMessageBody() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getMessageBody());
        }
        if (entity.getDirection() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDirection());
        }
        if (entity.getStatus() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getStatus());
        }
        if (entity.getMessageRef() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getMessageRef());
        }
        if (entity.getSubscriptionId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getSubscriptionId());
        }
        statement.bindLong(8, entity.getSentAt());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__updateAdapterOfSmsEntity = new EntityDeletionOrUpdateAdapter<SmsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sms_cache` SET `id` = ?,`phoneNumber` = ?,`messageBody` = ?,`direction` = ?,`status` = ?,`messageRef` = ?,`subscriptionId` = ?,`sentAt` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SmsEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getPhoneNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPhoneNumber());
        }
        if (entity.getMessageBody() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getMessageBody());
        }
        if (entity.getDirection() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDirection());
        }
        if (entity.getStatus() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getStatus());
        }
        if (entity.getMessageRef() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getMessageRef());
        }
        if (entity.getSubscriptionId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getSubscriptionId());
        }
        statement.bindLong(8, entity.getSentAt());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfCleanOldLogs = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sms_cache WHERE status = 'uploaded' AND createdAt < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SmsEntity sms, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSmsEntity.insertAndReturnId(sms);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final SmsEntity sms, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSmsEntity.handle(sms);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object cleanOldLogs(final long timestamp, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCleanOldLogs.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfCleanOldLogs.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getPendingUploads(final Continuation<? super List<SmsEntity>> $completion) {
    final String _sql = "SELECT * FROM sms_cache WHERE status = 'pending_upload' ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SmsEntity>>() {
      @Override
      @NonNull
      public List<SmsEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfMessageBody = CursorUtil.getColumnIndexOrThrow(_cursor, "messageBody");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfMessageRef = CursorUtil.getColumnIndexOrThrow(_cursor, "messageRef");
          final int _cursorIndexOfSubscriptionId = CursorUtil.getColumnIndexOrThrow(_cursor, "subscriptionId");
          final int _cursorIndexOfSentAt = CursorUtil.getColumnIndexOrThrow(_cursor, "sentAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SmsEntity> _result = new ArrayList<SmsEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SmsEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final String _tmpMessageBody;
            if (_cursor.isNull(_cursorIndexOfMessageBody)) {
              _tmpMessageBody = null;
            } else {
              _tmpMessageBody = _cursor.getString(_cursorIndexOfMessageBody);
            }
            final String _tmpDirection;
            if (_cursor.isNull(_cursorIndexOfDirection)) {
              _tmpDirection = null;
            } else {
              _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            final String _tmpMessageRef;
            if (_cursor.isNull(_cursorIndexOfMessageRef)) {
              _tmpMessageRef = null;
            } else {
              _tmpMessageRef = _cursor.getString(_cursorIndexOfMessageRef);
            }
            final String _tmpSubscriptionId;
            if (_cursor.isNull(_cursorIndexOfSubscriptionId)) {
              _tmpSubscriptionId = null;
            } else {
              _tmpSubscriptionId = _cursor.getString(_cursorIndexOfSubscriptionId);
            }
            final long _tmpSentAt;
            _tmpSentAt = _cursor.getLong(_cursorIndexOfSentAt);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SmsEntity(_tmpId,_tmpPhoneNumber,_tmpMessageBody,_tmpDirection,_tmpStatus,_tmpMessageRef,_tmpSubscriptionId,_tmpSentAt,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object exists(final String messageRef, final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT * FROM sms_cache WHERE messageRef = ? AND direction = 'incoming')";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (messageRef == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, messageRef);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp == null ? null : _tmp != 0;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
