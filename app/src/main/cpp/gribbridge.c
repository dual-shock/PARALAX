#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "eccodes.h"

static int ensure_capacity(float** arr, int* cap, int need) {
    if (need <= *cap) return 1;
    int new_cap = (*cap == 0) ? 256 : (*cap * 2);
    while (new_cap < need) new_cap *= 2;

    float* p = (float*)realloc(*arr, sizeof(float) * (size_t)new_cap);
    if (p == NULL) return 0;

    *arr = p;
    *cap = new_cap;
    return 1;
}

JNIEXPORT jfloatArray JNICALL
Java_com_dual_1shock_paralax_data_grib_NativeGribBridge_decodeNearestFromFile(
        JNIEnv* env, jobject thiz, jstring jFilePath, jdouble lat, jdouble lon
) {
    const char* filePath = (*env)->GetStringUTFChars(env, jFilePath, NULL);
    if (!filePath) return NULL;
    FILE* f = fopen(filePath, "rb");
    if (!f) {
        (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
        return NULL;
    }

    float* out = NULL;
    int cap = 0;
    int n = 0;
    int err = 0;

    while (1) {
        codes_handle* h = codes_handle_new_from_file(NULL, f, PRODUCT_GRIB, &err);
        if (!h) break;

        long edition = 0;
        if (codes_get_long(h, "edition", &edition) != 0 || edition != 2) {
            codes_handle_delete(h);
            continue;
        }

        long discipline = -1, category = -1, num = -1, level = -1;
        if (codes_get_long(h, "discipline", &discipline) != 0) discipline = 0;
        if (codes_get_long(h, "parameterCategory", &category) != 0) category = 0;
        if (codes_get_long(h, "parameterNumber", &num) != 0) num = -1;
        if (codes_get_long(h, "level", &level) != 0) level = -1;

        char levelType[64] = {0};
        size_t ltLen = sizeof(levelType);
        if (codes_get_string(h, "typeOfLevel", levelType, &ltLen) != 0) {
            codes_handle_delete(h);
            continue;
        }

        if (strcmp(levelType, "isobaricInhPa") != 0) {
            codes_handle_delete(h);
            continue;
        }

        // 1. Create nearest object
        codes_nearest* nearest = codes_grib_nearest_new(h, &err);
        if (!nearest) {
            codes_handle_delete(h);
            continue;
        }

        double lats[4], lons[4], vals[4], dists[4];
        int idx[4];
        size_t size = 4;

        // 2. Find the 4 nearest points
        if (codes_grib_nearest_find(nearest, h, lat, lon, 0, lats, lons, vals, dists, idx, &size) != 0 || size == 0) {
            codes_grib_nearest_delete(nearest);
            codes_handle_delete(h);
            continue;
        }

        int best = 0;
        for (int i = 1; i < (int)size; i++) {
            if (dists[i] < dists[best]) best = i;
        }

        if (!ensure_capacity(&out, &cap, n + 4)) {
            codes_grib_nearest_delete(nearest);
            codes_handle_delete(h);
            fclose(f);
            (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
            free(out);
            return NULL;
        }

        out[n++] = (float)category;
        out[n++] = (float)num;
        out[n++] = (float)level;
        out[n++] = (float)vals[best];

        // 3. Delete nearest object and handle
        codes_grib_nearest_delete(nearest);
        codes_handle_delete(h);
    }

    fclose(f);
    (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);

    jfloatArray result = (*env)->NewFloatArray(env, n);
    if (result && n > 0) {
        (*env)->SetFloatArrayRegion(env, result, 0, n, out);
    }
    free(out);
    return result;
}

JNIEXPORT jfloatArray JNICALL
Java_com_dual_1shock_paralax_data_grib_NativeGribBridge_decodeRegionFromFile(
        JNIEnv* env, jobject thiz, jstring jFilePath,
        jdouble centerLat, jdouble centerLon, jint patchRadius
) {
    const char* filePath = (*env)->GetStringUTFChars(env, jFilePath, NULL);
    if (!filePath) return NULL;

    FILE* f = fopen(filePath, "rb");
    if (!f) {
        (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
        return NULL;
    }

    int err = 0;
    int side = 2 * patchRadius + 1;
    int nTargets = side * side;
    double dlat = 0, dlon = 0;

    /* ---- first pass: find grid spacing via ecCodes nearest-point lookup ---- */
    while (1) {
        codes_handle* h = codes_handle_new_from_file(NULL, f, PRODUCT_GRIB, &err);
        if (!h) break;

        long edition = 0;
        if (codes_get_long(h, "edition", &edition) != 0 || edition != 2) {
            codes_handle_delete(h);
            continue;
        }
        char levelType[64] = {0};
        size_t ltLen = sizeof(levelType);
        if (codes_get_string(h, "typeOfLevel", levelType, &ltLen) != 0 ||
            strcmp(levelType, "isobaricInhPa") != 0) {
            codes_handle_delete(h);
            continue;
        }

        codes_nearest* nearest = codes_grib_nearest_new(h, &err);
        if (nearest) {
            double lats[4], lons[4], vals[4], dists[4];
            int idx[4];
            size_t size = 4;
            if (codes_grib_nearest_find(nearest, h, centerLat, centerLon, 0,
                    lats, lons, vals, dists, idx, &size) == 0 && size == 4) {
                double minLat = lats[0], maxLat = lats[0];
                double minLon = lons[0], maxLon = lons[0];
                for (int i = 1; i < 4; i++) {
                    if (lats[i] < minLat) minLat = lats[i];
                    if (lats[i] > maxLat) maxLat = lats[i];
                    if (lons[i] < minLon) minLon = lons[i];
                    if (lons[i] > maxLon) maxLon = lons[i];
                }
                dlat = maxLat - minLat;
                dlon = maxLon - minLon;
            }
            codes_grib_nearest_delete(nearest);
        }
        codes_handle_delete(h);
        break;
    }

    if (dlat <= 0 || dlon <= 0) {
        fclose(f);
        (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
        return NULL;
    }

    /* ---- build target lat/lon grid using the discovered spacing ---- */
    double* targetLats = (double*)malloc((size_t)nTargets * sizeof(double));
    double* targetLons = (double*)malloc((size_t)nTargets * sizeof(double));
    if (!targetLats || !targetLons) {
        free(targetLats); free(targetLons);
        fclose(f);
        (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
        return NULL;
    }

    for (int dr = -patchRadius; dr <= patchRadius; dr++) {
        for (int dc = -patchRadius; dc <= patchRadius; dc++) {
            int ti = (dr + patchRadius) * side + (dc + patchRadius);
            targetLats[ti] = centerLat + dr * dlat;
            targetLons[ti] = centerLon + dc * dlon;
        }
    }

    /* ---- second pass: query each target point per message via ecCodes ---- */
    rewind(f);

    float* out = NULL;
    int cap = 0, n = 0;

    while (1) {
        codes_handle* h = codes_handle_new_from_file(NULL, f, PRODUCT_GRIB, &err);
        if (!h) break;

        long edition = 0;
        if (codes_get_long(h, "edition", &edition) != 0 || edition != 2) {
            codes_handle_delete(h);
            continue;
        }

        char levelType[64] = {0};
        size_t ltLen = sizeof(levelType);
        if (codes_get_string(h, "typeOfLevel", levelType, &ltLen) != 0 ||
            strcmp(levelType, "isobaricInhPa") != 0) {
            codes_handle_delete(h);
            continue;
        }

        long category = -1, num = -1, level = -1;
        codes_get_long(h, "parameterCategory", &category);
        codes_get_long(h, "parameterNumber", &num);
        codes_get_long(h, "level", &level);

        codes_nearest* nearest = codes_grib_nearest_new(h, &err);
        if (!nearest) {
            codes_handle_delete(h);
            continue;
        }

        for (int ti = 0; ti < nTargets; ti++) {
            double lats[4], lons[4], vals[4], dists[4];
            int idx[4];
            size_t size = 4;

            /* CODES_NEAREST_SAME_GRID reuses cached grid structure after first call */
            unsigned long flags = (ti == 0) ? 0 : CODES_NEAREST_SAME_GRID;

            if (codes_grib_nearest_find(nearest, h, targetLats[ti], targetLons[ti],
                    flags, lats, lons, vals, dists, idx, &size) != 0 || size == 0) {
                continue;
            }

            int best = 0;
            for (int i = 1; i < (int)size; i++) {
                if (dists[i] < dists[best]) best = i;
            }

            if (!ensure_capacity(&out, &cap, n + 6)) {
                codes_grib_nearest_delete(nearest);
                codes_handle_delete(h);
                free(targetLats); free(targetLons);
                fclose(f);
                (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
                free(out);
                return NULL;
            }

            out[n++] = (float)lats[best];
            out[n++] = (float)lons[best];
            out[n++] = (float)category;
            out[n++] = (float)num;
            out[n++] = (float)level;
            out[n++] = (float)vals[best];
        }

        codes_grib_nearest_delete(nearest);
        codes_handle_delete(h);
    }

    fclose(f);
    (*env)->ReleaseStringUTFChars(env, jFilePath, filePath);
    free(targetLats);
    free(targetLons);

    jfloatArray regionResult = (*env)->NewFloatArray(env, n);
    if (regionResult && n > 0) {
        (*env)->SetFloatArrayRegion(env, regionResult, 0, n, out);
    }
    free(out);
    return regionResult;
}