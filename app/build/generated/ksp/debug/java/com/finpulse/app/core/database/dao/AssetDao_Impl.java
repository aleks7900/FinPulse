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
import com.finpulse.app.core.database.entity.AssetEntity;
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
public final class AssetDao_Impl implements AssetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AssetEntity> __insertionAdapterOfAssetEntity;

  private final EntityDeletionOrUpdateAdapter<AssetEntity> __deletionAdapterOfAssetEntity;

  private final EntityDeletionOrUpdateAdapter<AssetEntity> __updateAdapterOfAssetEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAssetById;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePrice;

  public AssetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAssetEntity = new EntityInsertionAdapter<AssetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `assets` (`id`,`name`,`symbol`,`assetClass`,`quantity`,`purchasePriceMinor`,`currentPriceMinor`,`currencyCode`,`lastUpdated`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AssetEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getSymbol());
        statement.bindString(4, entity.getAssetClass());
        statement.bindDouble(5, entity.getQuantity());
        statement.bindLong(6, entity.getPurchasePriceMinor());
        statement.bindLong(7, entity.getCurrentPriceMinor());
        statement.bindString(8, entity.getCurrencyCode());
        statement.bindLong(9, entity.getLastUpdated());
        if (entity.getNotes() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getNotes());
        }
      }
    };
    this.__deletionAdapterOfAssetEntity = new EntityDeletionOrUpdateAdapter<AssetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `assets` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AssetEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfAssetEntity = new EntityDeletionOrUpdateAdapter<AssetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `assets` SET `id` = ?,`name` = ?,`symbol` = ?,`assetClass` = ?,`quantity` = ?,`purchasePriceMinor` = ?,`currentPriceMinor` = ?,`currencyCode` = ?,`lastUpdated` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AssetEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getSymbol());
        statement.bindString(4, entity.getAssetClass());
        statement.bindDouble(5, entity.getQuantity());
        statement.bindLong(6, entity.getPurchasePriceMinor());
        statement.bindLong(7, entity.getCurrentPriceMinor());
        statement.bindString(8, entity.getCurrencyCode());
        statement.bindLong(9, entity.getLastUpdated());
        if (entity.getNotes() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getNotes());
        }
        statement.bindString(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAssetById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM assets WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdatePrice = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE assets SET currentPriceMinor = ?, lastUpdated = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAsset(final AssetEntity asset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAssetEntity.insert(asset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAsset(final AssetEntity asset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAssetEntity.handle(asset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAsset(final AssetEntity asset, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAssetEntity.handle(asset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAssetById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAssetById.acquire();
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
          __preparedStmtOfDeleteAssetById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePrice(final String id, final long priceMinor, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePrice.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, priceMinor);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, timestamp);
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
          __preparedStmtOfUpdatePrice.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAssetById(final String id, final Continuation<? super AssetEntity> $completion) {
    final String _sql = "SELECT * FROM assets WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AssetEntity>() {
      @Override
      @Nullable
      public AssetEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfAssetClass = CursorUtil.getColumnIndexOrThrow(_cursor, "assetClass");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPurchasePriceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePriceMinor");
          final int _cursorIndexOfCurrentPriceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPriceMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final AssetEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final String _tmpAssetClass;
            _tmpAssetClass = _cursor.getString(_cursorIndexOfAssetClass);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final long _tmpPurchasePriceMinor;
            _tmpPurchasePriceMinor = _cursor.getLong(_cursorIndexOfPurchasePriceMinor);
            final long _tmpCurrentPriceMinor;
            _tmpCurrentPriceMinor = _cursor.getLong(_cursorIndexOfCurrentPriceMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new AssetEntity(_tmpId,_tmpName,_tmpSymbol,_tmpAssetClass,_tmpQuantity,_tmpPurchasePriceMinor,_tmpCurrentPriceMinor,_tmpCurrencyCode,_tmpLastUpdated,_tmpNotes);
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
  public Flow<AssetEntity> getAssetByIdFlow(final String id) {
    final String _sql = "SELECT * FROM assets WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"assets"}, new Callable<AssetEntity>() {
      @Override
      @Nullable
      public AssetEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfAssetClass = CursorUtil.getColumnIndexOrThrow(_cursor, "assetClass");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPurchasePriceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePriceMinor");
          final int _cursorIndexOfCurrentPriceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPriceMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final AssetEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final String _tmpAssetClass;
            _tmpAssetClass = _cursor.getString(_cursorIndexOfAssetClass);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final long _tmpPurchasePriceMinor;
            _tmpPurchasePriceMinor = _cursor.getLong(_cursorIndexOfPurchasePriceMinor);
            final long _tmpCurrentPriceMinor;
            _tmpCurrentPriceMinor = _cursor.getLong(_cursorIndexOfCurrentPriceMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new AssetEntity(_tmpId,_tmpName,_tmpSymbol,_tmpAssetClass,_tmpQuantity,_tmpPurchasePriceMinor,_tmpCurrentPriceMinor,_tmpCurrencyCode,_tmpLastUpdated,_tmpNotes);
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
  public Flow<List<AssetEntity>> getAllAssetsFlow() {
    final String _sql = "SELECT * FROM assets ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"assets"}, new Callable<List<AssetEntity>>() {
      @Override
      @NonNull
      public List<AssetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfAssetClass = CursorUtil.getColumnIndexOrThrow(_cursor, "assetClass");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPurchasePriceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePriceMinor");
          final int _cursorIndexOfCurrentPriceMinor = CursorUtil.getColumnIndexOrThrow(_cursor, "currentPriceMinor");
          final int _cursorIndexOfCurrencyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "currencyCode");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<AssetEntity> _result = new ArrayList<AssetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AssetEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final String _tmpAssetClass;
            _tmpAssetClass = _cursor.getString(_cursorIndexOfAssetClass);
            final double _tmpQuantity;
            _tmpQuantity = _cursor.getDouble(_cursorIndexOfQuantity);
            final long _tmpPurchasePriceMinor;
            _tmpPurchasePriceMinor = _cursor.getLong(_cursorIndexOfPurchasePriceMinor);
            final long _tmpCurrentPriceMinor;
            _tmpCurrentPriceMinor = _cursor.getLong(_cursorIndexOfCurrentPriceMinor);
            final String _tmpCurrencyCode;
            _tmpCurrencyCode = _cursor.getString(_cursorIndexOfCurrencyCode);
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new AssetEntity(_tmpId,_tmpName,_tmpSymbol,_tmpAssetClass,_tmpQuantity,_tmpPurchasePriceMinor,_tmpCurrentPriceMinor,_tmpCurrencyCode,_tmpLastUpdated,_tmpNotes);
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
