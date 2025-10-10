package com.microsoft.xbox.toolkit;

import org.jetbrains.annotations.NotNull;


public enum AsyncActionStatus {
    SUCCESS,
    FAIL,
    NO_CHANGE,
    NO_OP_SUCCESS,
    NO_OP_FAIL;

    private static AsyncActionStatus[][] MERGE_MATRIX = null;

    static {
        MERGE_MATRIX = new AsyncActionStatus[][]{new AsyncActionStatus[]{SUCCESS, FAIL, SUCCESS, SUCCESS, FAIL}, new AsyncActionStatus[]{FAIL, FAIL, FAIL, FAIL, FAIL}, new AsyncActionStatus[]{SUCCESS, FAIL, NO_CHANGE, NO_OP_SUCCESS, NO_OP_FAIL}, new AsyncActionStatus[]{SUCCESS, FAIL, NO_OP_SUCCESS, NO_OP_SUCCESS, NO_OP_FAIL}, new AsyncActionStatus[]{FAIL, FAIL, NO_OP_FAIL, NO_OP_FAIL, NO_OP_FAIL}};
    }

    public static boolean getIsFail(AsyncActionStatus status) {
        return status == FAIL || status == NO_OP_FAIL;
    }

    public static AsyncActionStatus merge(AsyncActionStatus x, @NotNull AsyncActionStatus... y) {
        AsyncActionStatus left = x;
        for (AsyncActionStatus right : y) {
            left = MERGE_MATRIX[left.ordinal()][right.ordinal()];
        }
        return left;
    }
}