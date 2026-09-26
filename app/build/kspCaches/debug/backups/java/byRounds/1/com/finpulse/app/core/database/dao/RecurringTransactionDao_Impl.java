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
import com.finpulse.app.core.database.entity.RecurringTransactionEntity;
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
public final class RecurringTransactionDao_Impl implements RecurringTransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RecurringTransactionEntity> __insertionAdapterOfRecurringTransactionEntity;

  private final EntityDeletionOrUpdateAdapter<RecurringTransactionEntity> __deletionAdapterOfRecurringTransactionEntity;

  private final EntityDeletionOrUpdateAdapter<RecurringTransactionEntity> __updateAdapterOfRecurringTransactionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteRecurringById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateProcessedDate;

  public RecurringTransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRecurringTransactionEntity = new EntityInsertionAdapter<RecurringTransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `recurring_transactions` (`id`,`title`,`amountMinor`,`currencyCode`,`accountId`,`categoryId`,`frequency`,`nextDueDate`,`lastProcessedDate`,`isActive`,`isSubscription`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecurringTransactionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getAmountMinor());
        statement.bindString(4, entity.getCurrencyCode());
        statement.bindString(5, entity.getAccountId());
        statement.bindString(6, entity.getCategoryId());
        statement.bindString(7, entity.getFrequency());
        statement.bindLong(8, entity.getNextDueDate());
        if (entity.getLastProcessedDate() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getLastProcessedDate());
        }
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(10, _tmp);
        final int _tmp_1 = entity.isSubscription() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        if (entity.getNotes() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getNotes());
        }
      }
    };
    this.__deletionAdapterOfRecurringTransactionEntity = new EntityDeletionOrUpdateAdapter<RecurringTransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `recurring_transactions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecurringTransactionEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfRecurringTransactionEntity = new EntityDeletionOrUpdateAdapter<RecurringTransactionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `recurring_transactions` SET `id` = ?,`title` = ?,`amountMinor` = ?,`currencyCode` = ?,`accountId` = ?,`categoryId` = ?,`frequency` = ?,`nextDueDate` = ?,`lastProcessedDate` = ?,`isActive` = ?,`isSubscription` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecurringTransactionEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getAmountMinor());
        statement.bindString(4, entity.getCurrencyCode());
        statement.bindString(5, entity.getAccountId());
        statement.bindString(6, entity.getCategoryId());
        statement.bindString(7, entity.getFrequency());
        statement.bindLong(8, entity.getNextDueDate());
        if (entity.getLastProcessedDate() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getLastProcessedDate());
        }
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(10, _tmp);
        final int _tmp_1 = entity.isSubscription() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        if (entity.getNotes() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getNotes());
        }
        statement.bindString(13, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteRecurringById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM recurring_transactions WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateProcessedDate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE recurring_transactions SET nextDueDate = ?, lastProcessedDate = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertRecurring(final RecurringTransactionEntity recurring,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRecurringTransactionEntity.insert(recurring);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRecurringList(final List<RecurringTransactionEntity> list,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRecurringTransactionEntity.insert(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecurring(final RecurringTransactionEntity recurring,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRecurringTransactionEntity.handle(recurring);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRecurring(final RecurringTransactionEntity recurring,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRecurringTransactionEntity.handle(recurring);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecurringById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteRecurringById.acquire();
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
          __preparedStmtOfDeleteRecurringById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProcessedDate(final String id, final long nextDueDate,
      final long lastProcessedDate, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateProcessedDate.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, nextDueDate);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, lastProcessedDate);
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
          __preparedStmtOfUpdateProcessedDate.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecurringById(final String id,
      final Continuation<? super RecurringTransactionEntity> $completion) {
    final String _sql = "SELECT * FROM recurring_transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RecurringTransactionEntity>() {
      @Override
      @Nullable
      public RecurringTransactionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfNextDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextDueDate");
          final int _cursorIndexOfLastProcessedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastProcessedDate");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfIsSubscription = CursorUtil.getColumnIndexOrThrow(_cursor, "isSubscription");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final RecurringTransactionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final long _tmpNextDueDate;
            _tmpNextDueDate = _cursor.getLong(_cursorIndexOfNextDueDate);
            final Long _tmpLastProcessedDate;
            if (_cursor.isNull(_cursorIndexOfLastProcessedDate)) {
              _tmpLastProcessedDate = null;
            } else {
              _tmpLastProcessedDate = _cursor.getLong(_cursorIndexOfLastProcessedDate);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final boolean _tmpIsSubscription;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSubscription);
            _tmpIsSubscription = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new RecurringTransactionEntity(_tmpId,_tmpTitle,_tmpAmountMinor,_tmpCurrencyCode,_tmpAccountId,_tmpCategoryId,_tmpFrequency,_tmpNextDueDate,_tmpLastProcessedDate,_tmpIsActive,_tmpIsSubscription,_tmpNotes);
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
  public Flow<RecurringTransactionEntity> getRecurringByIdFlow(final String id) {
    final String _sql = "SELECT * FROM recurring_transactions WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recurring_transactions"}, new Callable<RecurringTransactionEntity>() {
      @Override
      @Nullable
      public RecurringTransactionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfNextDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextDueDate");
          final int _cursorIndexOfLastProcessedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastProcessedDate");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfIsSubscription = CursorUtil.getColumnIndexOrThrow(_cursor, "isSubscription");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final RecurringTransactionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final long _tmpNextDueDate;
            _tmpNextDueDate = _cursor.getLong(_cursorIndexOfNextDueDate);
            final Long _tmpLastProcessedDate;
            if (_cursor.isNull(_cursorIndexOfLastProcessedDate)) {
              _tmpLastProcessedDate = null;
            } else {
              _tmpLastProcessedDate = _cursor.getLong(_cursorIndexOfLastProcessedDate);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final boolean _tmpIsSubscription;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSubscription);
            _tmpIsSubscription = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new RecurringTransactionEntity(_tmpId,_tmpTitle,_tmpAmountMinor,_tmpCurrencyCode,_tmpAccountId,_tmpCategoryId,_tmpFrequency,_tmpNextDueDate,_tmpLastProcessedDate,_tmpIsActive,_tmpIsSubscription,_tmpNotes);
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
  public Flow<List<RecurringTransactionEntity>> getAllRecurringFlow() {
    final String _sql = "SELECT * FROM recurring_transactions ORDER BY nextDueDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recurring_transactions"}, new Callable<List<RecurringTransactionEntity>>() {
      @Override
      @NonNull
      public List<RecurringTransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfNextDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextDueDate");
          final int _cursorIndexOfLastProcessedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastProcessedDate");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfIsSubscription = CursorUtil.getColumnIndexOrThrow(_cursor, "isSubscription");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<RecurringTransactionEntity> _result = new ArrayList<RecurringTransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecurringTransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final long _tmpNextDueDate;
            _tmpNextDueDate = _cursor.getLong(_cursorIndexOfNextDueDate);
            final Long _tmpLastProcessedDate;
            if (_cursor.isNull(_cursorIndexOfLastProcessedDate)) {
              _tmpLastProcessedDate = null;
            } else {
              _tmpLastProcessedDate = _cursor.getLong(_cursorIndexOfLastProcessedDate);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final boolean _tmpIsSubscription;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSubscription);
            _tmpIsSubscription = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new RecurringTransactionEntity(_tmpId,_tmpTitle,_tmpAmountMinor,_tmpCurrencyCode,_tmpAccountId,_tmpCategoryId,_tmpFrequency,_tmpNextDueDate,_tmpLastProcessedDate,_tmpIsActive,_tmpIsSubscription,_tmpNotes);
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
  public Flow<List<RecurringTransactionEntity>> getActiveRecurringFlow() {
    final String _sql = "SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY nextDueDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recurring_transactions"}, new Callable<List<RecurringTransactionEntity>>() {
      @Override
      @NonNull
      public List<RecurringTransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfNextDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextDueDate");
          final int _cursorIndexOfLastProcessedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastProcessedDate");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfIsSubscription = CursorUtil.getColumnIndexOrThrow(_cursor, "isSubscription");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<RecurringTransactionEntity> _result = new ArrayList<RecurringTransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecurringTransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final long _tmpNextDueDate;
            _tmpNextDueDate = _cursor.getLong(_cursorIndexOfNextDueDate);
            final Long _tmpLastProcessedDate;
            if (_cursor.isNull(_cursorIndexOfLastProcessedDate)) {
              _tmpLastProcessedDate = null;
            } else {
              _tmpLastProcessedDate = _cursor.getLong(_cursorIndexOfLastProcessedDate);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final boolean _tmpIsSubscription;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSubscription);
            _tmpIsSubscription = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new RecurringTransactionEntity(_tmpId,_tmpTitle,_tmpAmountMinor,_tmpCurrencyCode,_tmpAccountId,_tmpCategoryId,_tmpFrequency,_tmpNextDueDate,_tmpLastProcessedDate,_tmpIsActive,_tmpIsSubscription,_tmpNotes);
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
  public Flow<List<RecurringTransactionEntity>> getActiveSubscriptionsFlow() {
    final String _sql = "SELECT * FROM recurring_transactions WHERE isActive = 1 AND isSubscription = 1 ORDER BY nextDueDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recurring_transactions"}, new Callable<List<RecurringTransactionEntity>>() {
      @Override
      @NonNull
      public List<RecurringTransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfNextDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextDueDate");
          final int _cursorIndexOfLastProcessedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastProcessedDate");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfIsSubscription = CursorUtil.getColumnIndexOrThrow(_cursor, "isSubscription");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<RecurringTransactionEntity> _result = new ArrayList<RecurringTransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecurringTransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final long _tmpNextDueDate;
            _tmpNextDueDate = _cursor.getLong(_cursorIndexOfNextDueDate);
            final Long _tmpLastProcessedDate;
            if (_cursor.isNull(_cursorIndexOfLastProcessedDate)) {
              _tmpLastProcessedDate = null;
            } else {
              _tmpLastProcessedDate = _cursor.getLong(_cursorIndexOfLastProcessedDate);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final boolean _tmpIsSubscription;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSubscription);
            _tmpIsSubscription = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new RecurringTransactionEntity(_tmpId,_tmpTitle,_tmpAmountMinor,_tmpCurrencyCode,_tmpAccountId,_tmpCategoryId,_tmpFrequency,_tmpNextDueDate,_tmpLastProcessedDate,_tmpIsActive,_tmpIsSubscription,_tmpNotes);
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
  public Object getDueRecurring(final long timestamp,
      final Continuation<? super List<RecurringTransactionEntity>> $completion) {
    final String _sql = "SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextDueDate <= ? ORDER BY nextDueDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, timestamp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RecurringTransactionEntity>>() {
      @Override
      @NonNull
      public List<RecurringTransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAmountMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "amountMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfNextDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "nextDueDate");
          final int _cursorIndexOfLastProcessedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastProcessedDate");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfIsSubscription = CursorUtil.getColumnIndexOrThrow(_cursor, "isSubscription");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<RecurringTransactionEntity> _result = new ArrayList<RecurringTransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecurringTransactionEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpAmountMinor;
            _tmpAmountMinor = _cursor.getLong(_cursorIndexOfAmountMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final String _tmpCategoryId;
            _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            final String _tmpFrequency;
            _tmpFrequency = _cursor.getString(_cursorIndexOfFrequency);
            final long _tmpNextDueDate;
            _tmpNextDueDate = _cursor.getLong(_cursorIndexOfNextDueDate);
            final Long _tmpLastProcessedDate;
            if (_cursor.isNull(_cursorIndexOfLastProcessedDate)) {
              _tmpLastProcessedDate = null;
            } else {
              _tmpLastProcessedDate = _cursor.getLong(_cursorIndexOfLastProcessedDate);
            }
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final boolean _tmpIsSubscription;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsSubscription);
            _tmpIsSubscription = _tmp_1 != 0;
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new RecurringTransactionEntity(_tmpId,_tmpTitle,_tmpAmountMinor,_tmpCurrencyCode,_tmpAccountId,_tmpCategoryId,_tmpFrequency,_tmpNextDueDate,_tmpLastProcessedDate,_tmpIsActive,_tmpIsSubscription,_tmpNotes);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
