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
import com.tumba.ai.data.local.entity.LocalModelEntity;
import java.lang.Class;
import java.lang.Exception;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LocalModelDao_Impl implements LocalModelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalModelEntity> __insertionAdapterOfLocalModelEntity;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public LocalModelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalModelEntity = new EntityInsertionAdapter<LocalModelEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `local_models` (`id`,`displayName`,`fileName`,`path`,`sizeBytes`,`importedAt`,`quantization`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalModelEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getDisplayName());
        statement.bindString(3, entity.getFileName());
        statement.bindString(4, entity.getPath());
        statement.bindLong(5, entity.getSizeBytes());
        statement.bindLong(6, entity.getImportedAt());
        if (entity.getQuantization() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getQuantization());
        }
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM local_models WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final LocalModelEntity entity,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLocalModelEntity.insert(entity);
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
  public Flow<List<LocalModelEntity>> observeAll() {
    final String _sql = "SELECT * FROM local_models ORDER BY importedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"local_models"}, new Callable<List<LocalModelEntity>>() {
      @Override
      @NonNull
      public List<LocalModelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
          final int _cursorIndexOfImportedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "importedAt");
          final int _cursorIndexOfQuantization = CursorUtil.getColumnIndexOrThrow(_cursor, "quantization");
          final List<LocalModelEntity> _result = new ArrayList<LocalModelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalModelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpSizeBytes;
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
            final long _tmpImportedAt;
            _tmpImportedAt = _cursor.getLong(_cursorIndexOfImportedAt);
            final String _tmpQuantization;
            if (_cursor.isNull(_cursorIndexOfQuantization)) {
              _tmpQuantization = null;
            } else {
              _tmpQuantization = _cursor.getString(_cursorIndexOfQuantization);
            }
            _item = new LocalModelEntity(_tmpId,_tmpDisplayName,_tmpFileName,_tmpPath,_tmpSizeBytes,_tmpImportedAt,_tmpQuantization);
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
  public Object getAll(final Continuation<? super List<LocalModelEntity>> $completion) {
    final String _sql = "SELECT * FROM local_models ORDER BY importedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalModelEntity>>() {
      @Override
      @NonNull
      public List<LocalModelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
          final int _cursorIndexOfImportedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "importedAt");
          final int _cursorIndexOfQuantization = CursorUtil.getColumnIndexOrThrow(_cursor, "quantization");
          final List<LocalModelEntity> _result = new ArrayList<LocalModelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalModelEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpSizeBytes;
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
            final long _tmpImportedAt;
            _tmpImportedAt = _cursor.getLong(_cursorIndexOfImportedAt);
            final String _tmpQuantization;
            if (_cursor.isNull(_cursorIndexOfQuantization)) {
              _tmpQuantization = null;
            } else {
              _tmpQuantization = _cursor.getString(_cursorIndexOfQuantization);
            }
            _item = new LocalModelEntity(_tmpId,_tmpDisplayName,_tmpFileName,_tmpPath,_tmpSizeBytes,_tmpImportedAt,_tmpQuantization);
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
  public Object getById(final String id, final Continuation<? super LocalModelEntity> $completion) {
    final String _sql = "SELECT * FROM local_models WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalModelEntity>() {
      @Override
      @Nullable
      public LocalModelEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfSizeBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "sizeBytes");
          final int _cursorIndexOfImportedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "importedAt");
          final int _cursorIndexOfQuantization = CursorUtil.getColumnIndexOrThrow(_cursor, "quantization");
          final LocalModelEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpSizeBytes;
            _tmpSizeBytes = _cursor.getLong(_cursorIndexOfSizeBytes);
            final long _tmpImportedAt;
            _tmpImportedAt = _cursor.getLong(_cursorIndexOfImportedAt);
            final String _tmpQuantization;
            if (_cursor.isNull(_cursorIndexOfQuantization)) {
              _tmpQuantization = null;
            } else {
              _tmpQuantization = _cursor.getString(_cursorIndexOfQuantization);
            }
            _result = new LocalModelEntity(_tmpId,_tmpDisplayName,_tmpFileName,_tmpPath,_tmpSizeBytes,_tmpImportedAt,_tmpQuantization);
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

  @Override
  public Object totalSize(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COALESCE(SUM(sizeBytes), 0) FROM local_models";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
