/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.graphics;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.v4.content.res.FontResourcesParserCompat.FontFamilyFilesResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.FontFileResourceEntry;
import android.support.v4.provider.FontsContractCompat.FontInfo;
import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;


/**
 * Implementation of the Typeface compat methods for API 24 and above.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(24)
class TypefaceCompatApi24Impl implements TypefaceCompat.TypefaceCompatImpl {
    private static final String TAG = "TypefaceCompatApi24Impl";

    private static final String FONT_FAMILY_CLASS = "android.graphics.FontFamily";
    private static final String ADD_FONT_WEIGHT_STYLE_METHOD = "addFontWeightStyle";
    private static final String CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD =
            "createFromFamiliesWithDefault";
    private static final Class sFontFamily;
    private static final Constructor sFontFamilyCtor;
    private static final Method sAddFontWeightStyle;
    private static final Method sCreateFromFamiliesWithDefault;

    static {
        Class fontFamilyClass;
        Constructor fontFamilyCtor;
        Method addFontMethod;
        Method createFromFamiliesWithDefaultMethod;
        try {
            fontFamilyClass = Class.forName(FONT_FAMILY_CLASS);
            fontFamilyCtor = fontFamilyClass.getConstructor();
            addFontMethod = fontFamilyClass.getMethod(ADD_FONT_WEIGHT_STYLE_METHOD,
                    ByteBuffer.class, Integer.TYPE, List.class, Integer.TYPE, Boolean.TYPE);
            Object familyArray = Array.newInstance(fontFamilyClass, 1);
            createFromFamiliesWithDefaultMethod =
                    Typeface.class.getMethod(CREATE_FROM_FAMILIES_WITH_DEFAULT_METHOD,
                          familyArray.getClass());
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Log.e(TAG, e.getClass().getName(), e);
            fontFamilyClass = null;
            fontFamilyCtor = null;
            addFontMethod = null;
            createFromFamiliesWithDefaultMethod = null;
        }
        sFontFamilyCtor = fontFamilyCtor;
        sFontFamily = fontFamilyClass;
        sAddFontWeightStyle = addFontMethod;
        sCreateFromFamiliesWithDefault = createFromFamiliesWithDefaultMethod;
    }

    /**
     * Returns true if API24 implementation is usable.
     */
    public static boolean isUsable() {
        return sAddFontWeightStyle != null;
    }

    private static Object newFamily() {
        try {
            return sFontFamilyCtor.newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean addFontWeightStyle(Object family, ByteBuffer buffer, int ttcIndex,
            int weight, boolean style) {
        try {
            final Boolean result = (Boolean) sAddFontWeightStyle.invoke(
                    family, buffer, ttcIndex, null /* variation axis */, weight, style);
            return result.booleanValue();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static Typeface createFromFamiliesWithDefault(Object family) {
        try {
            Object familyArray = Array.newInstance(sFontFamily, 1);
            Array.set(familyArray, 0, family);
            return (Typeface) sCreateFromFamiliesWithDefault.invoke(
                    null /* static method */, familyArray);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Typeface createTypeface(Context context, @NonNull FontInfo[] fonts,
            Map<Uri, ByteBuffer> uriBuffer) {
        Object family = newFamily();
        for (final FontInfo font : fonts) {
            if (!addFontWeightStyle(family, uriBuffer.get(font.getUri()), font.getTtcIndex(),
                    font.getWeight(), font.isItalic())) {
                return null;
            }
        }
        return createFromFamiliesWithDefault(family);
    }

    @Override
    public Typeface createFromFontFamilyFilesResourceEntry(Context context,
            FontFamilyFilesResourceEntry entry, Resources resources, int style) {
        Object family = newFamily();
        for (final FontFileResourceEntry e : entry.getEntries()) {
            final ByteBuffer buffer =
                    TypefaceCompatUtil.copyToDirectBuffer(context, resources, e.getResourceId());
            // TODO: support ttc index.
            if (!addFontWeightStyle(family, buffer, 0, e.getWeight(), e.isItalic())) {
                return null;
            }
        }
        return createFromFamiliesWithDefault(family);
    }
}
