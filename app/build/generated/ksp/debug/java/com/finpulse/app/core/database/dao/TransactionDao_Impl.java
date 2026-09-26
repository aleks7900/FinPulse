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
import com.finpulse.app.core.database.entity.TransactionEntity;
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
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TransactionEntity> __insertionAdapterOfTransactionEntity;

  private final EntityDeletionOrUpdateAdapter<TransactionEntity> __deletionAdapterOfTransactionEntity;

  private final EntityDeletionOrUpdateAdapter<TransactionEntity> __updateAdapterOfTransactionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTransactionById;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransactionEntity = new EntityInsertionAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`amountMinor`,`currencyCode`,`type`,`sourceAccountId`,`destinationAccountId`,`categoryId`,`merchant`,`timestamp`,`description`,`tags`,`notes`,`recurringRuleId`,`isExcludedFromBudget`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getAmountMinor());
        statement.bindString(3, entity.getCurrencyCode());
        statement.bindString(4, entity.getType());
        statement.bindString(5, entity.getSourceAccountId());
        if (entity.getDestinationAccountId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDestinationAccountId());
        }
        statement.bindString(7, entity.getCategoryId());
        if (entity.getMerchant() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getMerchant());
        }
        statement.bindLong(9, entity.getTimestamp());
        statement.bindString(10, entity.getDescription());
        statement.bindString(11, entity.getTags());
        if (entity.getNotes() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getNotes());
        }
        if (entity.getRecurringRuleId() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getRecurringRuleId());
        }
        final int _tmp = entity.isExcludedFromBudget() ? 1 : 0;
        statement.bindLong(14, _tmp);
        statement.bindLong(15, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfTransactionEntity = new EntityDeletionOrUpdateAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `transactions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfTransactionEntity = new EntityDeletionOrUpdateAdapter<TransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `transactions` SET `id` = ?,`amountMinor` = ?,`currencyCode` = ?,`type` = ?,`sourceAccountId` = ?,`destinationAccountId` = ?,`categoryId` = ?,`merchant` = ?,`timestamp` = ?,`description` = ?,`tags` = ?,`notes` = ?,`recurringRuleId` = ?,`isExcludedFromBudget` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TransactionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getAmountMinor());
        statement.bindString(3, entity.getCurrencyCode());
        statement.bindString(4, entity.getType());
        statement.bindString(5, entity.getSourceAccountId());
        if (entity.getDestinationAccountId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getDestinationAccountId());
        }
        statement.bindString(7, entity.getCategoryId());
        if (entity.getMerchant() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getMerchant());
        }
        statement.bindLong(9, entity.getTimestamp());
        statement.bindString(10, entity.getDescription());
        statement.bindString(11, entity.getTags());
        if (entity.getNotes() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getNotes());
        }
        if (entity.getRecurringRuleId() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getRecurringRuleId());
        }
        final int _tmp = entity.isExcludedFromBudget() ? 1 : 0;
        statement.bindLong(14, _tmp);
        statement.bindLong(15, entity.getCreatedAt());
        statement.bindString(16, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteTransactionById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionEntity.insert(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTransactions(final List<TransactionEntity> transactions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransactionEntity.insert(transactions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTransactionEntity.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTransaction(final TransactionEntity transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTransactionEntity.handle(transaction);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTransactionById(final String id,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTransactionById.acquire();
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
          __preparedStmtOfDeleteTransactionById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getTransactionById(final String id,
      final Continuation<? super TransactionEntity> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TransactionEntity>() {
      @Override
      @Nullable
      public TransactionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final TransactionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<TransactionEntity> getTransactionByIdFlow(final String id) {
    final String _sql = "SELECT * FROM transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<TransactionEntity>() {
      @Override
      @Nullable
      public TransactionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final TransactionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<List<TransactionEntity>> getAllTransactionsFlow() {
    final String _sql = "SELECT * FROM transactions ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<List<TransactionEntity>> getRecentTransactionsFlow(final int limit) {
    final String _sql = "SELECT * FROM transactions ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<List<TransactionEntity>> getTransactionsByAccountFlow(final String accountId) {
    final String _sql = "SELECT * FROM transactions WHERE sourceAccountId = ? OR destinationAccountId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    _argIndex = 2;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<List<TransactionEntity>> getTransactionsByCategoryFlow(final String categoryId) {
    final String _sql = "SELECT * FROM transactions WHERE categoryId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<List<TransactionEntity>> getTransactionsByDateRangeFlow(final long startDate,
      final long endDate) {
    final String _sql = "SELECT * FROM transactions WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Object getTransactionsByDateRange(final long startDate, final long endDate,
      final Continuation<? super List<TransactionEntity>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE timestamp >= ? AND timestamp <= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<List<TransactionEntity>> searchTransactionsFlow(final String query) {
    final String _sql = "\n"
            + "        SELECT * FROM transactions \n"
            + "        WHERE (description LIKE '%' || ? || '%' \n"
            + "           OR merchant LIKE '%' || ? || '%' \n"
            + "           OR notes LIKE '%' || ? || '%' \n"
            + "           OR tags LIKE '%' || ? || '%')\n"
            + "        ORDER BY timestamp DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    _argIndex = 3;
    _statement.bindString(_argIndex, query);
    _argIndex = 4;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<TransactionEntity>>() {
      @Override
      @NonNull
      public List<TransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSourceAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceAccountId");
          final int _cursorIndexOfDestinationAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "destinationAccountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfRecurringRuleId = CursorUtil.getColumnIndexOrThrow(_cursor, "recurringRuleId");
          final int _cursorIndexOfIsExcludedFromBudget = CursorUtil.getColumnIndexOrThrow(_cursor, "isExcludedFromBudget");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TransactionEntity> _result = new ArrayList<TransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSourceAccountId;
            _tmpSourceAccountId = _cursor.getString(_cursorIndexOfSourceAccountId);
            final String _tmpDestinationAccountId;
            if (_cursor.isNull(_cursorIndexOfDestinationAccountId)) {
              _tmpDestinationAccountId = null;
            } else {
              _tmpDestinationAccountId = _cursor.getString(_cursorIndexOfDestinationAccountId);
            }
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpMerchant;
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _tmpMerchant = null;
            } else {
              _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpRecurringRuleId;
            if (_cursor.isNull(_cursorIndexOfRecurringRuleId)) {
              _tmpRecurringRuleId = null;
            } else {
              _tmpRecurringRuleId = _cursor.getString(_cursorIndexOfRecurringRuleId);
            }
            final boolean _tmpIsExcludedFromBudget;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsExcludedFromBudget);
            _tmpIsExcludedFromBudget = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new TransactionEntity(_tmpId,_tmpAmountMinor,_tmpCurrencyCode,_tmpType,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpCategoryId,_tmpMerchant,_tmpTimestamp,_tmpDescription,_tmpTags,_tmpNotes,_tmpRecurringRuleId,_tmpIsExcludedFromBudget,_tmpCreatedAt);
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
  public Flow<Long> getSumByTypeAndDateRangeFlow(final String type, final long startDate,
      final long endDate) {
    final String _sql = "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE type = ? AND timestamp >= ? AND timestamp <= ? AND isExcludedFromBudget = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Long>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Long> getExpenseSumByCategoryAndDateRangeFlow(final String categoryId,
      final long startDate, final long endDate) {
    final String _sql = "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions WHERE categoryId = ? AND type = 'EXPENSE' AND timestamp >= ? AND timestamp <= ? AND isExcludedFromBudget = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<Long>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<String>> getFrequentCategoryIdsFlow(final String type, final int limit) {
    final String _sql = "\n"
            + "        SELECT categoryId FROM transactions \n"
            + "        WHERE type = ? \n"
            + "        GROUP BY categoryId \n"
            + "        ORDER BY COUNT(*) DESC, MAX(timestamp) DESC \n"
            + "        LIMIT ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Flow<List<String>> getFrequentMerchantsFlow(final int limit) {
    final String _sql = "\n"
            + "        SELECT merchant FROM transactions \n"
            + "        WHERE merchant IS NOT NULL AND merchant != '' \n"
            + "        GROUP BY merchant \n"
            + "        ORDER BY COUNT(*) DESC, MAX(timestamp) DESC \n"
            + "        LIMIT ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Object getSuggestedCategoryForMerchant(final String merchant,
      final Continuation<? super String> $completion) {
    final String _sql = "\n"
            + "        SELECT categoryId FROM transactions \n"
            + "        WHERE merchant = ? \n"
            + "        GROUP BY categoryId \n"
            + "        ORDER BY COUNT(*) DESC, MAX(timestamp) DESC \n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, merchant);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getString(0);
            }
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
