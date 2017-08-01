package com.irfankhoirul.recipe.data.source.local.db;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.irfankhoirul.recipe.data.pojo.Ingredient;
import com.irfankhoirul.recipe.data.pojo.Recipe;
import com.irfankhoirul.recipe.data.pojo.Step;
import com.irfankhoirul.recipe.data.source.local.db.dao.IngredientDao;
import com.irfankhoirul.recipe.data.source.local.db.dao.RecipeDao;
import com.irfankhoirul.recipe.data.source.local.db.dao.StepDao;

import java.util.ArrayList;

import static com.irfankhoirul.recipe.data.source.local.db.RecipeContract.RecipeEntry.AUTHORITY;
import static com.irfankhoirul.recipe.data.source.local.db.RecipeContract.RecipeEntry.CODE_RECIPE_DIRECTORY;
import static com.irfankhoirul.recipe.data.source.local.db.RecipeContract.RecipeEntry.CODE_RECIPE_ITEM;
import static com.irfankhoirul.recipe.data.source.local.db.RecipeContract.RecipeEntry.TABLE_NAME;

/**
 * Created by Irfan Khoirul on 7/25/2017.
 */

public class RecipeContentProvider extends ContentProvider {
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(AUTHORITY, TABLE_NAME, CODE_RECIPE_DIRECTORY);
        MATCHER.addURI(AUTHORITY, TABLE_NAME + "/#", CODE_RECIPE_ITEM);
    }

    private RecipeDao recipeDao;
    private IngredientDao ingredientDao;
    private StepDao stepDao;
    private Context context;

    @Override
    public boolean onCreate() {
        context = getContext();
        if (context == null) {
            return false;
        }
        recipeDao = RecipeDatabase.getInstance(context).recipeDao();
        ingredientDao = RecipeDatabase.getInstance(context).ingredientDao();
        stepDao = RecipeDatabase.getInstance(context).stepDao();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final int code = MATCHER.match(uri);
        if (code == CODE_RECIPE_DIRECTORY || code == CODE_RECIPE_ITEM) {
            final Cursor cursor;
            if (code == CODE_RECIPE_DIRECTORY) {
                cursor = recipeDao.selectAllWithChildElements();
            } else {
                cursor = recipeDao.selectByIdWithChildElements(ContentUris.parseId(uri));
            }
            cursor.setNotificationUri(context.getContentResolver(), uri);
            return cursor;
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (MATCHER.match(uri)) {
            case CODE_RECIPE_DIRECTORY:
                return "vnd.android.cursor.dir/" + AUTHORITY + "." + TABLE_NAME;
            case CODE_RECIPE_ITEM:
                return "vnd.android.cursor.item/" + AUTHORITY + "." + TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        switch (MATCHER.match(uri)) {
            case CODE_RECIPE_DIRECTORY:
                Recipe recipe = Recipe.fromContentValues(values);
                final long recipeId = recipeDao.insert(recipe);
                if (recipeId > 0) {
                    // Insert ingredients
                    bulkInsertIngredients(recipe, recipeId);

                    // Insert steps
                    bulkInsertSteps(recipe, recipeId);
                }
                context.getContentResolver().notifyChange(uri, null);
                if (recipeId > 0) {
                    return ContentUris.withAppendedId(uri, recipeId);
                } else {
                    throw new SQLiteException("Failed to insert row into URI: " + uri);
                }
            case CODE_RECIPE_ITEM:
                throw new IllegalArgumentException("Invalid URI, cannot insert with ID: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private void bulkInsertSteps(Recipe recipe, long recipeId) {
        final Step[] steps = new Step[recipe.getSteps().size()];
        for (int i = 0; i < recipe.getSteps().size(); i++) {
            recipe.getSteps().get(i).setRecipeId(recipeId);
            steps[i] = recipe.getSteps().get(i);
        }
        long stepIds[] = stepDao.insertAll(steps);
        for (int i = 0; i < stepIds.length; i++) {
            if (stepIds[i] != 0) {
                Log.v("StepInsert", "Success: " + i);
            } else {
                Log.v("StepInsert", "Failed");
            }
        }
    }

    private void bulkInsertIngredients(Recipe recipe, long recipeId) {
        final Ingredient[] ingredients = new Ingredient[recipe.getIngredients().size()];
        for (int i = 0; i < recipe.getIngredients().size(); i++) {
            recipe.getIngredients().get(i).setRecipeId(recipeId);
            ingredients[i] = recipe.getIngredients().get(i);
        }
        long ingredientIds[] = ingredientDao.insertAll(ingredients);
        for (int i = 0; i < ingredientIds.length; i++) {
            if (ingredientIds[i] != 0) {
                Log.v("IngredientInsert", "Success: " + i);
            } else {
                Log.v("IngredientInsert", "Failed");
            }
        }
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] valuesArray) {
        switch (MATCHER.match(uri)) {
            case CODE_RECIPE_DIRECTORY:
                final Context context = getContext();
                if (context == null) {
                    return 0;
                }
                final Recipe[] recipes = new Recipe[valuesArray.length];
                for (int i = 0; i < valuesArray.length; i++) {
                    recipes[i] = Recipe.fromContentValues(valuesArray[i]);
                }
                long recipeIds[] = recipeDao.insertAll(recipes);
                for (int i = 0; i < recipeIds.length; i++) {
                    // Insert ingredients
                    bulkInsertIngredients(recipes[i], recipeIds[i]);

                    // Insert steps
                    bulkInsertSteps(recipes[i], recipeIds[i]);
                }
            case CODE_RECIPE_ITEM:
                throw new IllegalArgumentException("Invalid URI, cannot insert with ID: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        switch (MATCHER.match(uri)) {
            case CODE_RECIPE_DIRECTORY:
                throw new IllegalArgumentException("Invalid URI, cannot update without ID" + uri);
            case CODE_RECIPE_ITEM:
                final int count = recipeDao.deleteById(ContentUris.parseId(uri));
                context.getContentResolver().notifyChange(uri, null);
                return count;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        switch (MATCHER.match(uri)) {
            case CODE_RECIPE_DIRECTORY:
                throw new IllegalArgumentException("Not implemented yet" + uri);
            case CODE_RECIPE_ITEM:
                throw new IllegalArgumentException("Not implemented yet" + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        final Context context = getContext();
        if (context == null) {
            return new ContentProviderResult[0];
        }
        final RecipeDatabase database = RecipeDatabase.getInstance(context);
        database.beginTransaction();
        try {
            final ContentProviderResult[] result = super.applyBatch(operations);
            database.setTransactionSuccessful();
            return result;
        } finally {
            database.endTransaction();
        }
    }


}
