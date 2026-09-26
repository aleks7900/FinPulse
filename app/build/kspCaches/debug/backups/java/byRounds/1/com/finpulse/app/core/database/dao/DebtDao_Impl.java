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
import com.finpulse.app.core.database.entity.DebtEntity;
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
public final class DebtDao_Impl implements DebtDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DebtEntity> __insertionAdapterOfDebtEntity;

  private final EntityDeletionOrUpdateAdapter<DebtEntity> __deletionAdapterOfDebtEntity;

  private final EntityDeletionOrUpdateAdapter<DebtEntity> __updateAdapterOfDebtEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteDebtById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateBalance;

  public DebtDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDebtEntity = new EntityInsertionAdapter<DebtEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `debts` (`id`,`name`,`type`,`totalPrincipalMinor`,`remainingBalanceMinor`,`currencyCode`,`interestRatePercent`,`minimumPaymentMinor`,`nextPaymentDate`,`linkedAccountId`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DebtEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getType());
        statement.bindLong(4, entity.getTotalPrincipalMinor());
        statement.bindLong(5, entity.getRemainingBalanceMinor());
        statement.bindString(6, entity.getCurrencyCode());
        statement.bindDouble(7, entity.getInterestRatePercent());
        statement.bindLong(8, entity.getMinimumPaymentMinor());
        statement.bindLong(9, entity.getNextPaymentDate());
        if (entity.getLinkedAccountId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getLinkedAccountId());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getNotes());
        }
      }
    };
    this.__deletionAdapterOfDebtEntity = new EntityDeletionOrUpdateAdapter<DebtEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `debts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DebtEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfDebtEntity = new EntityDeletionOrUpdateAdapter<DebtEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `debts` SET `id` = ?,`name` = ?,`type` = ?,`totalPrincipalMinor` = ?,`remainingBalanceMinor` = ?,`currencyCode` = ?,`interestRatePercent` = ?,`minimumPaymentMinor` = ?,`nextPaymentDate` = ?,`linkedAccountId` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DebtEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getType());
        statement.bindLong(4, entity.getTotalPrincipalMinor());
        statement.bindLong(5, entity.getRemainingBalanceMinor());
        statement.bindString(6, entity.getCurrencyCode());
        statement.bindDouble(7, entity.getInterestRatePercent());
        statement.bindLong(8, entity.getMinimumPaymentMinor());
        statement.bindLong(9, entity.getNextPaymentDate());
        if (entity.getLinkedAccountId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getLinkedAccountId());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getNotes());
        }
        statement.bindString(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteDebtById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM debts WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateBalance = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE debts SET remainingBalanceMinor = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertDebt(final DebtEntity debt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDebtEntity.insert(debt);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDebt(final DebtEntity debt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDebtEntity.handle(debt);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDebt(final DebtEntity debt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDebtEntity.handle(debt);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDebtById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteDebtById.acquire();
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
          __preparedStmtOfDeleteDebtById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBalance(final String id, final long remainingMinor,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateBalance.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, remainingMinor);
        _argIndex = 2;
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
          __preparedStmtOfUpdateBalance.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getDebtById(final String id, final Continuation<? super DebtEntity> $completion) {
    final String _sql = "SELECT * FROM debts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DebtEntity>() {
      @Override
      @Nullable
      public DebtEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTotalPrincipalMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPrincipalMinor");
          final int _cursorIndexOfRemainingBalanceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingBalanceMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfInterestRatePercent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRatePercent");
          final int _cursorIndexOfMinimumPaymentMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "minimumPaymentMinor");
          final int _cursorIndexOfNextPaymentDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextPaymentDate");
          final int _cursorIndexOfLinkedAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedAccountId");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final DebtEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTotalPrincipalMinor;
            _tmpTotalPrincipalMinor = _cursor.getLong(_cursorIndexOfTotalPrincipalMinor);
            final long _tmpRemainingBalanceMinor;
            _tmpRemainingBalanceMinor = _cursor.getLong(_cursorIndexOfRemainingBalanceMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final double _tmpInterestRatePercent;
            _tmpInterestRatePercent = _cursor.getDouble(_cursorIndexOfInterestRatePercent);
            final long _tmpMinimumPaymentMinor;
            _tmpMinimumPaymentMinor = _cursor.getLong(_cursorIndexOfMinimumPaymentMinor);
            final long _tmpNextPaymentDate;
            _tmpNextPaymentDate = _cursor.getLong(_cursorIndexOfNextPaymentDate);
            final String _tmpLinkedAccountId;
            if (_cursor.isNull(_cursorIndexOfLinkedAccountId)) {
              _tmpLinkedAccountId = null;
            } else {
              _tmpLinkedAccountId = _cursor.getString(_cursorIndexOfLinkedAccountId);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new DebtEntity(_tmpId,_tmpName,_tmpType,_tmpTotalPrincipalMinor,_tmpRemainingBalanceMinor,_tmpCurrencyCode,_tmpInterestRatePercent,_tmpMinimumPaymentMinor,_tmpNextPaymentDate,_tmpLinkedAccountId,_tmpNotes);
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
  public Flow<DebtEntity> getDebtByIdFlow(final String id) {
    final String _sql = "SELECT * FROM debts WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"debts"}, new Callable<DebtEntity>() {
      @Override
      @Nullable
      public DebtEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTotalPrincipalMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPrincipalMinor");
          final int _cursorIndexOfRemainingBalanceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingBalanceMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfInterestRatePercent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRatePercent");
          final int _cursorIndexOfMinimumPaymentMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "minimumPaymentMinor");
          final int _cursorIndexOfNextPaymentDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextPaymentDate");
          final int _cursorIndexOfLinkedAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedAccountId");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final DebtEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTotalPrincipalMinor;
            _tmpTotalPrincipalMinor = _cursor.getLong(_cursorIndexOfTotalPrincipalMinor);
            final long _tmpRemainingBalanceMinor;
            _tmpRemainingBalanceMinor = _cursor.getLong(_cursorIndexOfRemainingBalanceMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final double _tmpInterestRatePercent;
            _tmpInterestRatePercent = _cursor.getDouble(_cursorIndexOfInterestRatePercent);
            final long _tmpMinimumPaymentMinor;
            _tmpMinimumPaymentMinor = _cursor.getLong(_cursorIndexOfMinimumPaymentMinor);
            final long _tmpNextPaymentDate;
            _tmpNextPaymentDate = _cursor.getLong(_cursorIndexOfNextPaymentDate);
            final String _tmpLinkedAccountId;
            if (_cursor.isNull(_cursorIndexOfLinkedAccountId)) {
              _tmpLinkedAccountId = null;
            } else {
              _tmpLinkedAccountId = _cursor.getString(_cursorIndexOfLinkedAccountId);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new DebtEntity(_tmpId,_tmpName,_tmpType,_tmpTotalPrincipalMinor,_tmpRemainingBalanceMinor,_tmpCurrencyCode,_tmpInterestRatePercent,_tmpMinimumPaymentMinor,_tmpNextPaymentDate,_tmpLinkedAccountId,_tmpNotes);
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
  public Flow<List<DebtEntity>> getAllDebtsFlow() {
    final String _sql = "SELECT * FROM debts ORDER BY remainingBalanceMinor DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"debts"}, new Callable<List<DebtEntity>>() {
      @Override
      @NonNull
      public List<DebtEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTotalPrincipalMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPrincipalMinor");
          final int _cursorIndexOfRemainingBalanceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingBalanceMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfInterestRatePercent = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRatePercent");
          final int _cursorIndexOfMinimumPaymentMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "minimumPaymentMinor");
          final int _cursorIndexOfNextPaymentDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextPaymentDate");
          final int _cursorIndexOfLinkedAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedAccountId");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<DebtEntity> _result = new ArrayList<DebtEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DebtEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final long _tmpTotalPrincipalMinor;
            _tmpTotalPrincipalMinor = _cursor.getLong(_cursorIndexOfTotalPrincipalMinor);
            final long _tmpRemainingBalanceMinor;
            _tmpRemainingBalanceMinor = _cursor.getLong(_cursorIndexOfRemainingBalanceMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final double _tmpInterestRatePercent;
            _tmpInterestRatePercent = _cursor.getDouble(_cursorIndexOfInterestRatePercent);
            final long _tmpMinimumPaymentMinor;
            _tmpMinimumPaymentMinor = _cursor.getLong(_cursorIndexOfMinimumPaymentMinor);
            final long _tmpNextPaymentDate;
            _tmpNextPaymentDate = _cursor.getLong(_cursorIndexOfNextPaymentDate);
            final String _tmpLinkedAccountId;
            if (_cursor.isNull(_cursorIndexOfLinkedAccountId)) {
              _tmpLinkedAccountId = null;
            } else {
              _tmpLinkedAccountId = _cursor.getString(_cursorIndexOfLinkedAccountId);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new DebtEntity(_tmpId,_tmpName,_tmpType,_tmpTotalPrincipalMinor,_tmpRemainingBalanceMinor,_tmpCurrencyCode,_tmpInterestRatePercent,_tmpMinimumPaymentMinor,_tmpNextPaymentDate,_tmpLinkedAccountId,_tmpNotes);
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
