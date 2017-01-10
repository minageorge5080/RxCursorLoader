/*
 * Copyright (C) 2016 Yaroslav Mytkalyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doctoror.rxcursorloader;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.Cursor;
import android.os.Looper;
import android.os.Parcel;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import java.util.concurrent.CountDownLatch;

import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public final class RxCursorLoaderInstrumentedTest {

    @Test
    public void testQuery() throws Exception {
        final RxCursorLoader.Query query = new RxCursorLoader.Query.Builder()
                .setContentUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
                .setProjection(new String[]{MediaStore.Audio.Media._ID})
                .create();

        final CountDownLatch cdl = new CountDownLatch(1);
        final Observable<Cursor> o = RxCursorLoader.create(
                InstrumentationRegistry.getTargetContext().getContentResolver(), query);
        final Subscription s = o.subscribe(cursor -> {
            testValidCursor(cursor);
            cursor.close();
            cdl.countDown();
        });
        cdl.await();
        s.unsubscribe();
    }

    private void testValidCursor(@Nullable final Cursor c) {
        // Well it can be null if your MediaStore is fucked up, but we do not consider this option
        assertNotNull(c);

        // Test if can query after 1 sec (not closed while onNext())
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Test if can read
        if (c.moveToFirst()) {
            c.getLong(0);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testNoUriBuilder() throws Exception {
        //noinspection ConstantConditions
        RxCursorLoader.create(InstrumentationRegistry.getTargetContext()
                .getContentResolver(), new RxCursorLoader.Query.Builder().create());
    }

    @Test(expected = NullPointerException.class)
    public void testNullContentObserver() throws Exception {
        //noinspection ConstantConditions
        RxCursorLoader.create(null, new RxCursorLoader.Query.Builder()
                .setContentUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).create());
    }

    @Test(expected = NullPointerException.class)
    public void testNullParams() throws Exception {
        //noinspection ConstantConditions
        RxCursorLoader.create(InstrumentationRegistry.getTargetContext()
                .getContentResolver(), null);
    }

    @Test
    public void testQueryParcelable() throws Exception {
        final RxCursorLoader.Query query = new RxCursorLoader.Query.Builder()
                .setContentUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
                .setProjection(new String[]{MediaStore.Audio.Media._ID})
                .setSortOrder(MediaStore.Audio.Artists.ARTIST)
                .setSelection(MediaStore.Audio.Artists.ARTIST + "=?")
                .setSelectionArgs(new String[]{"Oh Long Johnson"})
                .create();

        final Parcel parcel = Parcel.obtain();
        query.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        final RxCursorLoader.Query fromParcel = RxCursorLoader.Query.CREATOR
                .createFromParcel(parcel);
        assertEquals(query, fromParcel);
    }

    @Test
    public void testSchedulers() throws Exception {
        final RxCursorLoader.Query query = new RxCursorLoader.Query.Builder()
                .setContentUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
                .setProjection(new String[]{MediaStore.Audio.Media._ID})
                .create();

        final CountDownLatch cdl = new CountDownLatch(1);
        RxCursorLoader.create(InstrumentationRegistry.getTargetContext()
                .getContentResolver(), query)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe(c -> {
                    assertNotEquals(Looper.getMainLooper(), Looper.myLooper());
                    testValidCursor(c);
                    c.close();
                    cdl.countDown();
                });
        cdl.await();
    }

    @Test
    public void testAsSingle() throws Exception {
        final RxCursorLoader.Query query = new RxCursorLoader.Query.Builder()
                .setContentUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
                .setProjection(new String[]{MediaStore.Audio.Media._ID})
                .create();

        final CountDownLatch cdl = new CountDownLatch(1);
        RxCursorLoader.single(InstrumentationRegistry.getTargetContext()
                .getContentResolver(), query).subscribe(c -> {
            testValidCursor(c);
            c.close();
            cdl.countDown();
        });
        cdl.await();
    }

    @Test
    public void testTake() throws Exception {
        final RxCursorLoader.Query query = new RxCursorLoader.Query.Builder()
                .setContentUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
                .setProjection(new String[]{MediaStore.Audio.Media._ID})
                .create();

        final CountDownLatch cdl = new CountDownLatch(1);
        RxCursorLoader.create(InstrumentationRegistry.getTargetContext()
                .getContentResolver(), query).take(1).subscribe(c -> {
            testValidCursor(c);
            c.close();
            cdl.countDown();
        });
        cdl.await();
    }

    @Test
    public void testAsObservableWithMap() throws Exception {
        final RxCursorLoader.Query query = new RxCursorLoader.Query.Builder()
                .setContentUri(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
                .setProjection(new String[]{MediaStore.Audio.Media._ID})
                .create();

        final CountDownLatch cdl = new CountDownLatch(1);
        RxCursorLoader.create(InstrumentationRegistry.getTargetContext()
                .getContentResolver(), query)
                .map(c -> new Object())
                .subscribe(o -> {
            cdl.countDown();
        });
        cdl.await();
    }
}
