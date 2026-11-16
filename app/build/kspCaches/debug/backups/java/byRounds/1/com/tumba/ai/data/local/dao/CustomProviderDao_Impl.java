package com.tumba.ai.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.tumba.ai.data.local.entity.CustomProviderEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CustomProviderDao_Impl implements CustomProviderDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CustomProviderEntity> __insertionAdapterOfCustomProviderEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public CustomProviderDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCustomProviderEntity = new EntityInsertionAdapter<CustomProviderEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `custom_providers` (`id`,`name`,`baseUrl`,`modelId`,`supportsImages`,`contextWindow`,`createdAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomProviderEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getBaseUrl());
        statement.bindString(4, entity.getModelId());
        final int _tmp = entity.getSupportsImages() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getContextWindow() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getContextWindow());
        }
        statement.bindLong(7, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM custom_providers WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final CustomProviderEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCustomProviderEntity.insert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CustomProviderEntity>> observeAll() {
    final String _sql = "SELECT * FROM custom_providers ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"custom_providers"}, new Callable<List<CustomProviderEntity>>() {
      @Override
      @NonNull
      public List<CustomProviderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBaseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "baseUrl");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfSupportsImages = CursorUtil.getColumnIndexOrThrow(_cursor, "supportsImages");
          final int _cursorIndexOfContextWindow = CursorUtil.getColumnIndexOrThrow(_cursor, "contextWindow");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CustomProviderEntity> _result = new ArrayList<CustomProviderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomProviderEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBaseUrl;
            _tmpBaseUrl = _cursor.getString(_cursorIndexOfBaseUrl);
            final String _tmpModelId;
            _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            final boolean _tmpSupportsImages;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSupportsImages);
            _tmpSupportsImages = _tmp != 0;
            final Integer _tmpContextWindow;
            if (_cursor.isNull(_cursorIndexOfContextWindow)) {
              _tmpContextWindow = null;
            } else {
              _tmpContextWindow = _cursor.getInt(_cursorIndexOfContextWindow);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CustomProviderEntity(_tmpId,_tmpName,_tmpBaseUrl,_tmpModelId,_tmpSupportsImages,_tmpContextWindow,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAll(final Continuation<? super List<CustomProviderEntity>> $completion) {
    final String _sql = "SELECT * FROM custom_providers ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CustomProviderEntity>>() {
      @Override
      @NonNull
      public List<CustomProviderEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBaseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "baseUrl");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfSupportsImages = CursorUtil.getColumnIndexOrThrow(_cursor, "supportsImages");
          final int _cursorIndexOfContextWindow = CursorUtil.getColumnIndexOrThrow(_cursor, "contextWindow");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CustomProviderEntity> _result = new ArrayList<CustomProviderEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomProviderEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBaseUrl;
            _tmpBaseUrl = _cursor.getString(_cursorIndexOfBaseUrl);
            final String _tmpModelId;
            _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            final boolean _tmpSupportsImages;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSupportsImages);
            _tmpSupportsImages = _tmp != 0;
            final Integer _tmpContextWindow;
            if (_cursor.isNull(_cursorIndexOfContextWindow)) {
              _tmpContextWindow = null;
            } else {
              _tmpContextWindow = _cursor.getInt(_cursorIndexOfContextWindow);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CustomProviderEntity(_tmpId,_tmpName,_tmpBaseUrl,_tmpModelId,_tmpSupportsImages,_tmpContextWindow,_tmpCreatedAt);
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
  public Object getById(final String id,
      final Continuation<? super CustomProviderEntity> $completion) {
    final String _sql = "SELECT * FROM custom_providers WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CustomProviderEntity>() {
      @Override
      @Nullable
      public CustomProviderEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBaseUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "baseUrl");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfSupportsImages = CursorUtil.getColumnIndexOrThrow(_cursor, "supportsImages");
          final int _cursorIndexOfContextWindow = CursorUtil.getColumnIndexOrThrow(_cursor, "contextWindow");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final CustomProviderEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBaseUrl;
            _tmpBaseUrl = _cursor.getString(_cursorIndexOfBaseUrl);
            final String _tmpModelId;
            _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            final boolean _tmpSupportsImages;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSupportsImages);
            _tmpSupportsImages = _tmp != 0;
            final Integer _tmpContextWindow;
            if (_cursor.isNull(_cursorIndexOfContextWindow)) {
              _tmpContextWindow = null;
            } else {
              _tmpContextWindow = _cursor.getInt(_cursorIndexOfContextWindow);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new CustomProviderEntity(_tmpId,_tmpName,_tmpBaseUrl,_tmpModelId,_tmpSupportsImages,_tmpContextWindow,_tmpCreatedAt);
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
