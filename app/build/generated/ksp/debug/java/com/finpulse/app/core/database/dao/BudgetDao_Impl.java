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
import com.finpulse.app.core.database.entity.BudgetEntity;
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
public final class BudgetDao_Impl implements BudgetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BudgetEntity> __insertionAdapterOfBudgetEntity;

  private final EntityDeletionOrUpdateAdapter<BudgetEntity> __deletionAdapterOfBudgetEntity;

  private final EntityDeletionOrUpdateAdapter<BudgetEntity> __updateAdapterOfBudgetEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBudgetById;

  public BudgetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBudgetEntity = new EntityInsertionAdapter<BudgetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `budgets` (`id`,`categoryId`,`name`,`limitAmountMinor`,`currencyCode`,`periodType`,`startDate`,`endDate`,`notifyAt70`,`notifyAt90`,`notifyAt100`,`isArchived`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BudgetEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCategoryId());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getLimitAmountMinor());
        statement.bindString(5, entity.getCurrencyCode());
        statement.bindString(6, entity.getPeriodType());
        statement.bindLong(7, entity.getStartDate());
        statement.bindLong(8, entity.getEndDate());
        final int _tmp = entity.getNotifyAt70() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.getNotifyAt90() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.getNotifyAt100() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        final int _tmp_3 = entity.isArchived() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
      }
    };
    this.__deletionAdapterOfBudgetEntity = new EntityDeletionOrUpdateAdapter<BudgetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `budgets` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BudgetEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfBudgetEntity = new EntityDeletionOrUpdateAdapter<BudgetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `budgets` SET `id` = ?,`categoryId` = ?,`name` = ?,`limitAmountMinor` = ?,`currencyCode` = ?,`periodType` = ?,`startDate` = ?,`endDate` = ?,`notifyAt70` = ?,`notifyAt90` = ?,`notifyAt100` = ?,`isArchived` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BudgetEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getCategoryId());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getLimitAmountMinor());
        statement.bindString(5, entity.getCurrencyCode());
        statement.bindString(6, entity.getPeriodType());
        statement.bindLong(7, entity.getStartDate());
        statement.bindLong(8, entity.getEndDate());
        final int _tmp = entity.getNotifyAt70() ? 1 : 0;
        statement.bindLong(9, _tmp);
        final int _tmp_1 = entity.getNotifyAt90() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        final int _tmp_2 = entity.getNotifyAt100() ? 1 : 0;
        statement.bindLong(11, _tmp_2);
        final int _tmp_3 = entity.isArchived() ? 1 : 0;
        statement.bindLong(12, _tmp_3);
        statement.bindString(13, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteBudgetById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM budgets WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertBudget(final BudgetEntity budget,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBudgetEntity.insert(budget);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertBudgets(final List<BudgetEntity> budgets,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBudgetEntity.insert(budgets);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBudget(final BudgetEntity budget,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfBudgetEntity.handle(budget);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBudget(final BudgetEntity budget,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBudgetEntity.handle(budget);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBudgetById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBudgetById.acquire();
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
          __preparedStmtOfDeleteBudgetById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getBudgetById(final String id,
      final Continuation<? super BudgetEntity> $completion) {
    final String _sql = "SELECT * FROM budgets WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BudgetEntity>() {
      @Override
      @Nullable
      public BudgetEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLimitAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "limitAmountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfPeriodType = CursorUtil.getColumnIndexOrThrow(_cursor, "periodType");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfNotifyAt70 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt70");
          final int _cursorIndexOfNotifyAt90 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt90");
          final int _cursorIndexOfNotifyAt100 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt100");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final BudgetEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpLimitAmountMinor;
            _tmpLimitAmountMinor = _cursor.getLong(_cursorIndexOfLimitAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpPeriodType;
            _tmpPeriodType = _cursor.getString(_cursorIndexOfPeriodType);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpEndDate;
            _tmpEndDate = _cursor.getLong(_cursorIndexOfEndDate);
            final boolean _tmpNotifyAt70;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotifyAt70);
            _tmpNotifyAt70 = _tmp != 0;
            final boolean _tmpNotifyAt90;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyAt90);
            _tmpNotifyAt90 = _tmp_1 != 0;
            final boolean _tmpNotifyAt100;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNotifyAt100);
            _tmpNotifyAt100 = _tmp_2 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_3 != 0;
            _result = new BudgetEntity(_tmpId,_tmpCategoryId,_tmpName,_tmpLimitAmountMinor,_tmpCurrencyCode,_tmpPeriodType,_tmpStartDate,_tmpEndDate,_tmpNotifyAt70,_tmpNotifyAt90,_tmpNotifyAt100,_tmpIsArchived);
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
  public Flow<BudgetEntity> getBudgetByIdFlow(final String id) {
    final String _sql = "SELECT * FROM budgets WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"budgets"}, new Callable<BudgetEntity>() {
      @Override
      @Nullable
      public BudgetEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLimitAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "limitAmountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfPeriodType = CursorUtil.getColumnIndexOrThrow(_cursor, "periodType");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfNotifyAt70 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt70");
          final int _cursorIndexOfNotifyAt90 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt90");
          final int _cursorIndexOfNotifyAt100 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt100");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final BudgetEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpLimitAmountMinor;
            _tmpLimitAmountMinor = _cursor.getLong(_cursorIndexOfLimitAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpPeriodType;
            _tmpPeriodType = _cursor.getString(_cursorIndexOfPeriodType);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpEndDate;
            _tmpEndDate = _cursor.getLong(_cursorIndexOfEndDate);
            final boolean _tmpNotifyAt70;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotifyAt70);
            _tmpNotifyAt70 = _tmp != 0;
            final boolean _tmpNotifyAt90;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyAt90);
            _tmpNotifyAt90 = _tmp_1 != 0;
            final boolean _tmpNotifyAt100;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNotifyAt100);
            _tmpNotifyAt100 = _tmp_2 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_3 != 0;
            _result = new BudgetEntity(_tmpId,_tmpCategoryId,_tmpName,_tmpLimitAmountMinor,_tmpCurrencyCode,_tmpPeriodType,_tmpStartDate,_tmpEndDate,_tmpNotifyAt70,_tmpNotifyAt90,_tmpNotifyAt100,_tmpIsArchived);
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
  public Flow<List<BudgetEntity>> getAllActiveBudgetsFlow() {
    final String _sql = "SELECT * FROM budgets WHERE isArchived = 0 ORDER BY startDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"budgets"}, new Callable<List<BudgetEntity>>() {
      @Override
      @NonNull
      public List<BudgetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLimitAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "limitAmountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfPeriodType = CursorUtil.getColumnIndexOrThrow(_cursor, "periodType");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfNotifyAt70 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt70");
          final int _cursorIndexOfNotifyAt90 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt90");
          final int _cursorIndexOfNotifyAt100 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt100");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final List<BudgetEntity> _result = new ArrayList<BudgetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BudgetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpLimitAmountMinor;
            _tmpLimitAmountMinor = _cursor.getLong(_cursorIndexOfLimitAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpPeriodType;
            _tmpPeriodType = _cursor.getString(_cursorIndexOfPeriodType);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpEndDate;
            _tmpEndDate = _cursor.getLong(_cursorIndexOfEndDate);
            final boolean _tmpNotifyAt70;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotifyAt70);
            _tmpNotifyAt70 = _tmp != 0;
            final boolean _tmpNotifyAt90;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyAt90);
            _tmpNotifyAt90 = _tmp_1 != 0;
            final boolean _tmpNotifyAt100;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNotifyAt100);
            _tmpNotifyAt100 = _tmp_2 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_3 != 0;
            _item = new BudgetEntity(_tmpId,_tmpCategoryId,_tmpName,_tmpLimitAmountMinor,_tmpCurrencyCode,_tmpPeriodType,_tmpStartDate,_tmpEndDate,_tmpNotifyAt70,_tmpNotifyAt90,_tmpNotifyAt100,_tmpIsArchived);
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
  public Flow<List<BudgetEntity>> getBudgetsByCategoryFlow(final String categoryId) {
    final String _sql = "SELECT * FROM budgets WHERE categoryId = ? AND isArchived = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"budgets"}, new Callable<List<BudgetEntity>>() {
      @Override
      @NonNull
      public List<BudgetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLimitAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "limitAmountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfPeriodType = CursorUtil.getColumnIndexOrThrow(_cursor, "periodType");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfEndDate = CursorUtil.getColumnIndexOrThrow(_cursor, "endDate");
          final int _cursorIndexOfNotifyAt70 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt70");
          final int _cursorIndexOfNotifyAt90 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt90");
          final int _cursorIndexOfNotifyAt100 = CursorUtil.getColumnIndexOrThrow(_cursor, "notifyAt100");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final List<BudgetEntity> _result = new ArrayList<BudgetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BudgetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpLimitAmountMinor;
            _tmpLimitAmountMinor = _cursor.getLong(_cursorIndexOfLimitAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpPeriodType;
            _tmpPeriodType = _cursor.getString(_cursorIndexOfPeriodType);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpEndDate;
            _tmpEndDate = _cursor.getLong(_cursorIndexOfEndDate);
            final boolean _tmpNotifyAt70;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfNotifyAt70);
            _tmpNotifyAt70 = _tmp != 0;
            final boolean _tmpNotifyAt90;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifyAt90);
            _tmpNotifyAt90 = _tmp_1 != 0;
            final boolean _tmpNotifyAt100;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfNotifyAt100);
            _tmpNotifyAt100 = _tmp_2 != 0;
            final boolean _tmpIsArchived;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp_3 != 0;
            _item = new BudgetEntity(_tmpId,_tmpCategoryId,_tmpName,_tmpLimitAmountMinor,_tmpCurrencyCode,_tmpPeriodType,_tmpStartDate,_tmpEndDate,_tmpNotifyAt70,_tmpNotifyAt90,_tmpNotifyAt100,_tmpIsArchived);
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
