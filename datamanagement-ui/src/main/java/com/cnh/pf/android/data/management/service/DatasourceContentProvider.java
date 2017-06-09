/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.cnh.pf.android.data.management.service;

import java.util.Arrays;
import java.util.HashSet;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import com.cnh.android.util.RestartUtils;
import com.cnh.pf.data.management.service.DatasourceContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kedzie
 */
public class DatasourceContentProvider  extends ContentProvider {

   private static final Logger logger = LoggerFactory.getLogger(DatasourceContentProvider.class);
   /*
    * Defines a handle to the database helper object. The MainDatabaseHelper class is defined
    * in a following snippet.
    */
   private MainDatabaseHelper mOpenHelper;

   // Defines the database name
   private static final String DBNAME = "datasource";

   private static final int DATASOURCES = 10;
   private static final int DATASOURCE_ID = 20;
   private static final int FOLDERS = 30;
   private static final int FOLDER_ID = 40;

   private static final UriMatcher sURIMatcher = new UriMatcher(
      UriMatcher.NO_MATCH);
   static {
      sURIMatcher.addURI(DatasourceContract.AUTHORITY, DatasourceContract.Datasource.BASE_PATH, DATASOURCES);
      sURIMatcher.addURI(DatasourceContract.AUTHORITY, DatasourceContract.Datasource.BASE_PATH + "/#", DATASOURCE_ID);
      sURIMatcher.addURI(DatasourceContract.AUTHORITY, DatasourceContract.Folder.BASE_PATH, FOLDERS);
      sURIMatcher.addURI(DatasourceContract.AUTHORITY, DatasourceContract.Folder.BASE_PATH + "/#", FOLDER_ID);
   }


   public boolean onCreate() {
      mOpenHelper = new MainDatabaseHelper(getContext());
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();
      if (RestartUtils.isFirstAppRunSinceBoot(getContext(), this.getClass().getSimpleName())) {
         db.delete(DatasourceContract.Datasource.TABLE, null, null);
         db.delete(DatasourceContract.Folder.TABLE, null, null);
         logger.debug("Cleared data on first run");
      }
      db.close();
      return true;
   }

   @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
      // Uisng SQLiteQueryBuilder instead of query() method
      SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

      // check if the caller has requested a column which does not exists
      checkColumns(projection);

      int uriType = sURIMatcher.match(uri);
      switch (uriType) {
      case DATASOURCES:
         queryBuilder.setTables(DatasourceContract.Datasource.TABLE);
         break;
      case DATASOURCE_ID:
         queryBuilder.setTables(DatasourceContract.Datasource.TABLE);
         queryBuilder.appendWhere(DatasourceContract.Datasource.COLUMN_ID + "="
            + uri.getLastPathSegment());
         break;
      case FOLDERS:
         queryBuilder.setTables(DatasourceContract.Folder.TABLE);
         break;
      case FOLDER_ID:
         queryBuilder.setTables(DatasourceContract.Folder.TABLE);
         queryBuilder.appendWhere(DatasourceContract.Folder.COLUMN_ID + "="
            + uri.getLastPathSegment());
         break;
      default:
         throw new IllegalArgumentException("Unknown URI: " + uri);
      }

      SQLiteDatabase db = mOpenHelper.getReadableDatabase();
      Cursor cursor = queryBuilder.query(db, projection, selection,
         selectionArgs, null, null, sortOrder);
      // make sure that potential listeners are getting notified
      cursor.setNotificationUri(getContext().getContentResolver(), uri);

      return cursor;
   }

   @Override public String getType(Uri uri) {
      switch(sURIMatcher.match(uri)) {
      case DATASOURCES:
         return DatasourceContract.Datasource.CONTENT_TYPE;
      case DATASOURCE_ID:
         return DatasourceContract.Datasource.CONTENT_ITEM_TYPE;
      case FOLDERS:
         return DatasourceContract.Folder.CONTENT_TYPE;
      case FOLDER_ID:
         return DatasourceContract.Folder.CONTENT_ITEM_TYPE;
      }
      return null;
   }

   @Override public Uri insert(Uri uri, ContentValues values) {
      int uriType = sURIMatcher.match(uri);
      SQLiteDatabase sqlDB = mOpenHelper.getWritableDatabase();
      sqlDB.setForeignKeyConstraintsEnabled(true);
      long id = 0;
      switch (uriType) {
      case DATASOURCES:
         id = sqlDB.insertWithOnConflict(DatasourceContract.Datasource.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
         getContext().getContentResolver().notifyChange(uri, null);
         return ContentUris.withAppendedId(DatasourceContract.Datasource.CONTENT_URI, id);
      case FOLDERS:
         id = sqlDB.insert(DatasourceContract.Folder.TABLE, null, values);
         getContext().getContentResolver().notifyChange(uri, null);
         return ContentUris.withAppendedId(DatasourceContract.Folder.CONTENT_URI, id);
      default:
         throw new IllegalArgumentException("Unknown URI: " + uri);
      }
   }

   @Override
   public int delete(Uri uri, String selection, String[] selectionArgs) {
      int uriType = sURIMatcher.match(uri);
      SQLiteDatabase sqlDB = mOpenHelper.getWritableDatabase();
      sqlDB.setForeignKeyConstraintsEnabled(true);
      int rowsDeleted = 0;
      String id = null;
      switch (uriType) {
      case DATASOURCES:
         rowsDeleted = sqlDB.delete(DatasourceContract.Datasource.TABLE, selection,
            selectionArgs);
         break;
      case DATASOURCE_ID:
         id = uri.getLastPathSegment();
         if (TextUtils.isEmpty(selection)) {
            rowsDeleted = sqlDB.delete(
               DatasourceContract.Datasource.TABLE,
               DatasourceContract.Datasource.COLUMN_ID + "=" + id,
               null);
         } else {
            rowsDeleted = sqlDB.delete(
               DatasourceContract.Datasource.TABLE,
               DatasourceContract.Datasource.COLUMN_ID + "=" + id
                  + " and " + selection,
               selectionArgs);
         }
         break;
      case FOLDERS:
         rowsDeleted = sqlDB.delete(DatasourceContract.Folder.TABLE, selection,
            selectionArgs);
         break;
      case FOLDER_ID:
         id = uri.getLastPathSegment();
         if (TextUtils.isEmpty(selection)) {
            rowsDeleted = sqlDB.delete(
               DatasourceContract.Folder.TABLE,
               DatasourceContract.Folder.COLUMN_ID + "=" + id,
               null);
         } else {
            rowsDeleted = sqlDB.delete(
               DatasourceContract.Folder.TABLE,
               DatasourceContract.Folder.COLUMN_ID + "=" + id
                  + " and " + selection,
               selectionArgs);
         }
         break;
      default:
         throw new IllegalArgumentException("Unknown URI: " + uri);
      }
      getContext().getContentResolver().notifyChange(uri, null);
      return rowsDeleted;
   }

   @Override
   public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {

      int uriType = sURIMatcher.match(uri);
      SQLiteDatabase sqlDB = mOpenHelper.getWritableDatabase();
      sqlDB.setForeignKeyConstraintsEnabled(true);
      int rowsUpdated = 0;
      String id = null;
      switch (uriType) {
      case DATASOURCES:
         rowsUpdated = sqlDB.update(DatasourceContract.Datasource.TABLE,
            values,
            selection,
            selectionArgs);
         break;
      case DATASOURCE_ID:
         id = uri.getLastPathSegment();
         if (TextUtils.isEmpty(selection)) {
            rowsUpdated = sqlDB.update(DatasourceContract.Datasource.TABLE,
               values,
               DatasourceContract.Datasource.COLUMN_ID + "=" + id,
               null);
         } else {
            rowsUpdated = sqlDB.update(DatasourceContract.Datasource.TABLE,
               values,
               DatasourceContract.Datasource.COLUMN_ID + "=" + id
                  + " and "
                  + selection,
               selectionArgs);
         }
         break;
      case FOLDERS:
         rowsUpdated = sqlDB.update(DatasourceContract.Folder.TABLE,
            values,
            selection,
            selectionArgs);
         break;
      case FOLDER_ID:
         id = uri.getLastPathSegment();
         if (TextUtils.isEmpty(selection)) {
            rowsUpdated = sqlDB.update(DatasourceContract.Folder.TABLE,
               values,
               DatasourceContract.Folder.COLUMN_ID + "=" + id,
               null);
         } else {
            rowsUpdated = sqlDB.update(DatasourceContract.Folder.TABLE,
               values,
               DatasourceContract.Folder.COLUMN_ID + "=" + id
                  + " and "
                  + selection,
               selectionArgs);
         }
         break;
      default:
         throw new IllegalArgumentException("Unknown URI: " + uri);
      }
      getContext().getContentResolver().notifyChange(uri, null);
      return rowsUpdated;
   }

   private void checkColumns(String[] projection) {
      String[] available = { DatasourceContract.Datasource.COLUMN_NAME,
         DatasourceContract.Folder.COLUMN_DATASOURCE, DatasourceContract.Folder.COLUMN_VALID,
         DatasourceContract.Folder.COLUMN_FOLDER,
         DatasourceContract.Folder.COLUMN_ID };
      if (projection != null) {
         HashSet<String> requestedColumns = new HashSet<String>(
            Arrays.asList(projection));
         HashSet<String> availableColumns = new HashSet<String>(
            Arrays.asList(available));
         // check if all columns which are requested are available
         if (!availableColumns.containsAll(requestedColumns)) {
            throw new IllegalArgumentException(
               "Unknown columns in projection");
         }
      }
   }

   private static final String SQL_CREATE_DATASOURCE = "CREATE TABLE " +
      DatasourceContract.Datasource.TABLE+                       // Table's name
      " (" +                           // The columns in the table
      DatasourceContract.Folder.COLUMN_ID  + " INTEGER PRIMARY KEY, " +
      DatasourceContract.Datasource.COLUMN_NAME + " TEXT UNIQUE NOT NULL)";

   private static final String SQL_CREATE_FOLDER = "CREATE TABLE " +
      DatasourceContract.Folder.TABLE +                       // Table's name
      " (" +                           // The columns in the table
      DatasourceContract.Folder.COLUMN_ID + " INTEGER PRIMARY KEY, " +
      DatasourceContract.Folder.COLUMN_FOLDER + " TEXT NOT NULL, " +
      DatasourceContract.Folder.COLUMN_VALID + " INTEGER, " +
      DatasourceContract.Folder.COLUMN_DATASOURCE + " TEXT NOT NULL, " +
      "FOREIGN KEY(" +  DatasourceContract.Folder.COLUMN_DATASOURCE + ") REFERENCES "
      + DatasourceContract.Datasource.TABLE + "(" + DatasourceContract.Datasource.COLUMN_NAME + ")" + " ON DELETE CASCADE)";

   /**
    * Helper class that actually creates and manages the provider's underlying data repository.
    */
   protected static final class MainDatabaseHelper extends SQLiteOpenHelper {

      /*
       * Instantiates an open helper for the provider's SQLite data repository
       * Do not do database creation and upgrade here.
       */
      MainDatabaseHelper(Context context) {
         super(context, DBNAME, null, 1);
      }

      /*
       * Creates the data repository. This is called when the provider attempts to open the
       * repository and SQLite reports that it doesn't exist.
       */
      public void onCreate(SQLiteDatabase db) {
         db.execSQL(SQL_CREATE_DATASOURCE);
         db.execSQL(SQL_CREATE_FOLDER);
      }

      @Override public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
         sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DatasourceContract.Datasource.TABLE);
         sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DatasourceContract.Folder.TABLE);
         onCreate(sqLiteDatabase);
      }
   }

}
