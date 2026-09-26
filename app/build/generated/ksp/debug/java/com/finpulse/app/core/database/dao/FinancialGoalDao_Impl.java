package com.finpulse.app.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.finpulse.app.core.database.entity.FinancialGoalEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class FinancialGoalDao_Impl implements FinancialGoalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FinancialGoalEntity> __insertionAdapterOfFinancialGoalEntity;

  private final EntityDeletionOrUpdateAdapter<FinancialGoalEntity> __deletionAdapterOfFinancialGoalEntity;

  private final EntityDeletionOrUpdateAdapter<FinancialGoalEntity> __updateAdapterOfFinancialGoalEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteGoalById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateProgress;

  public FinancialGoalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFinancialGoalEntity = new EntityInsertionAdapter<FinancialGoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `financial_goals` (`id`,`title`,`targetAmountMinor`,`currentAmountMinor`,`currencyCode`,`targetDate`,`linkedAccountId`,`icon`,`colorHex`,`isCompleted`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FinancialGoalEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getTargetAmountMinor());
        statement.bindLong(4, entity.getCurrentAmountMinor());
        statement.bindString(5, entity.getCurrencyCode());
        statement.bindLong(6, entity.getTargetDate());
        if (entity.getLinkedAccountId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getLinkedAccountId());
        }
        statement.bindString(8, entity.getIcon());
        statement.bindLong(9, entity.getColorHex());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfFinancialGoalEntity = new EntityDeletionOrUpdateAdapter<FinancialGoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `financial_goals` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FinancialGoalEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfFinancialGoalEntity = new EntityDeletionOrUpdateAdapter<FinancialGoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `financial_goals` SET `id` = ?,`title` = ?,`targetAmountMinor` = ?,`currentAmountMinor` = ?,`currencyCode` = ?,`targetDate` = ?,`linkedAccountId` = ?,`icon` = ?,`colorHex` = ?,`isCompleted` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FinancialGoalEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getTargetAmountMinor());
        statement.bindLong(4, entity.getCurrentAmountMinor());
        statement.bindString(5, entity.getCurrencyCode());
        statement.bindLong(6, entity.getTargetDate());
        if (entity.getLinkedAccountId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getLinkedAccountId());
        }
        statement.bindString(8, entity.getIcon());
        statement.bindLong(9, entity.getColorHex());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.getCreatedAt());
        statement.bindString(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteGoalById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM financial_goals WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateProgress = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE financial_goals SET currentAmountMinor = ?, isCompleted = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertGoal(final FinancialGoalEntity goal,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFinancialGoalEntity.insert(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteGoal(final FinancialGoalEntity goal,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfFinancialGoalEntity.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateGoal(final FinancialGoalEntity goal,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfFinancialGoalEntity.handle(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteGoalById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteGoalById.acquire();
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
          __preparedStmtOfDeleteGoalById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProgress(final String id, final long currentAmountMinor,
      final boolean isCompleted, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateProgress.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, currentAmountMinor);
        _argIndex = 2;
        final int _tmp = isCompleted ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 3;
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
          __preparedStmtOfUpdateProgress.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getGoalById(final String id,
      final Continuation<? super FinancialGoalEntity> $completion) {
    final String _sql = "SELECT * FROM financial_goals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<FinancialGoalEntity>() {
      @Override
      @Nullable
      public FinancialGoalEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTargetAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAmountMinor");
          final int _cursorIndexOfCurrentAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "currentAmountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfLinkedAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedAccountId");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final FinancialGoalEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpTargetAmountMinor;
            _tmpTargetAmountMinor = _cursor.getLong(_cursorIndexOfTargetAmountMinor);
            final long _tmpCurrentAmountMinor;
            _tmpCurrentAmountMinor = _cursor.getLong(_cursorIndexOfCurrentAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final long _tmpTargetDate;
            _tmpTargetDate = _cursor.getLong(_cursorIndexOfTargetDate);
            final String _tmpLinkedAccountId;
            if (_cursor.isNull(_cursorIndexOfLinkedAccountId)) {
              _tmpLinkedAccountId = null;
            } else {
              _tmpLinkedAccountId = _cursor.getString(_cursorIndexOfLinkedAccountId);
            }
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final long _tmpColorHex;
            _tmpColorHex = _cursor.getLong(_cursorIndexOfColorHex);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new FinancialGoalEntity(_tmpId,_tmpTitle,_tmpTargetAmountMinor,_tmpCurrentAmountMinor,_tmpCurrencyCode,_tmpTargetDate,_tmpLinkedAccountId,_tmpIcon,_tmpColorHex,_tmpIsCompleted,_tmpCreatedAt);
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
  public Flow<FinancialGoalEntity> getGoalByIdFlow(final String id) {
    final String _sql = "SELECT * FROM financial_goals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"financial_goals"}, new Callable<FinancialGoalEntity>() {
      @Override
      @Nullable
      public FinancialGoalEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTargetAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAmountMinor");
          final int _cursorIndexOfCurrentAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "currentAmountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfLinkedAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedAccountId");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final FinancialGoalEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpTargetAmountMinor;
            _tmpTargetAmountMinor = _cursor.getLong(_cursorIndexOfTargetAmountMinor);
            final long _tmpCurrentAmountMinor;
            _tmpCurrentAmountMinor = _cursor.getLong(_cursorIndexOfCurrentAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final long _tmpTargetDate;
            _tmpTargetDate = _cursor.getLong(_cursorIndexOfTargetDate);
            final String _tmpLinkedAccountId;
            if (_cursor.isNull(_cursorIndexOfLinkedAccountId)) {
              _tmpLinkedAccountId = null;
            } else {
              _tmpLinkedAccountId = _cursor.getString(_cursorIndexOfLinkedAccountId);
            }
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final long _tmpColorHex;
            _tmpColorHex = _cursor.getLong(_cursorIndexOfColorHex);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new FinancialGoalEntity(_tmpId,_tmpTitle,_tmpTargetAmountMinor,_tmpCurrentAmountMinor,_tmpCurrencyCode,_tmpTargetDate,_tmpLinkedAccountId,_tmpIcon,_tmpColorHex,_tmpIsCompleted,_tmpCreatedAt);
          } else {
            _result = null;
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
  public Flow<List<FinancialGoalEntity>> getAllGoalsFlow() {
    final String _sql = "SELECT * FROM financial_goals ORDER BY isCompleted ASC, targetDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"financial_goals"}, new Callable<List<FinancialGoalEntity>>() {
      @Override
      @NonNull
      public List<FinancialGoalEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTargetAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "targetAmountMinor");
          final int _cursorIndexOfCurrentAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "currentAmountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfLinkedAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedAccountId");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<FinancialGoalEntity> _result = new ArrayList<FinancialGoalEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FinancialGoalEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpTargetAmountMinor;
            _tmpTargetAmountMinor = _cursor.getLong(_cursorIndexOfTargetAmountMinor);
            final long _tmpCurrentAmountMinor;
            _tmpCurrentAmountMinor = _cursor.getLong(_cursorIndexOfCurrentAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final long _tmpTargetDate;
            _tmpTargetDate = _cursor.getLong(_cursorIndexOfTargetDate);
            final String _tmpLinkedAccountId;
            if (_cursor.isNull(_cursorIndexOfLinkedAccountId)) {
              _tmpLinkedAccountId = null;
            } else {
              _tmpLinkedAccountId = _cursor.getString(_cursorIndexOfLinkedAccountId);
            }
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final long _tmpColorHex;
            _tmpColorHex = _cursor.getLong(_cursorIndexOfColorHex);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new FinancialGoalEntity(_tmpId,_tmpTitle,_tmpTargetAmountMinor,_tmpCurrentAmountMinor,_tmpCurrencyCode,_tmpTargetDate,_tmpLinkedAccountId,_tmpIcon,_tmpColorHex,_tmpIsCompleted,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
