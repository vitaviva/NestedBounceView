package com.vitaviva.nestedbounceview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;


public class NestedBounceView extends FrameLayout implements NestedScrollingParent, NestedScrollingChild {
    public NestedBounceView(Context context) {
        super(context);
    }

    NestedScrollingParentHelper mNestedScrollingParentHelper;

    public NestedBounceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NestedBounceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (childView == null) childView = getChildAt(0);
        //移动过程中,recyclerView的数据更新会造成onLayout，导致画面异常
        if (isMoving) {
            return;
        }
        super.onLayout(changed,
                left, top, right, bottom);
    }

    private boolean isMoving = false;
    private float xDistance, yDistance, lastX, lastY;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDistance = yDistance = 0f;
                lastX = ev.getX();
                lastY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float curX = ev.getX();
                final float curY = ev.getY();
                xDistance += curX - lastX;
                yDistance += curY - lastY;
                lastX = curX;
                lastY = curY;

                if (Math.abs(xDistance) > Math.abs(yDistance)) {
                    return false;
                }
                return shouldIntercept((int) yDistance);

        }

        return super.onInterceptTouchEvent(ev);
    }

    private boolean shouldIntercept(int dy) {
        View v = getChildAt(0);
        return v != null && !ViewCompat.canScrollVertically(v, -dy);
    }


    private static final int S_SIZE = 4;//表示 拖动的距离为屏幕的高度的1/4
    private View childView;
    private float nowY;
    private Rect normal = new Rect();

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (childView == null) {
            return super.onTouchEvent(ev);
        } else {
            commOnTouchEvent(ev);
        }
        return true;
    }

    private void commOnTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                nowY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                if (!normal.isEmpty()) {
                    moveRelease();
                } else {
                    getChildAt(0).onTouchEvent(ev);
                }
                isMoving = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final float preY = nowY;
                nowY = ev.getY();
                int deltaY = (int) (preY - nowY);
                if (isNeedMove(deltaY)) {
                    moveOn(deltaY);
                } else {
                    getChildAt(0).onTouchEvent(ev);
                }
                break;
            default:
                break;
        }
    }

    // 是否需要移动布局
    public boolean isNeedMove(float direction) {
        View v = getChildAt(0);
        if (ViewCompat.canScrollVertically(v, -1) ||
                ViewCompat.canScrollVertically(v, -1)) {
            //优先处理子view的scroll事件，此时父view不能拦截事件
            return false;
        }
        if (v.getTop() <= 0 && direction > 0) {
            //子view顶端与父view对齐后继续向下scroll，此时父view不能拦截事件
            return false;
        }
        if (v.getTop() >= 0) {
            return true;
        }
        return false;
    }

    private void moveRelease() {
        if (!normal.isEmpty()) {
            int start = childView.getTop();
            int end = normal.top;
            ObjectAnimator.ofFloat(childView, "translationY", start, end).setDuration(100).start();
            // 设置回到正常的布局位置
            childView.layout(normal.left, normal.top, normal.right, normal.bottom);
            normal.setEmpty();
        }
    }

    private void moveOn(float dy) {
        isMoving = true;
        if (normal.isEmpty()) {
            // 保存正常的布局位置
            normal.set(childView.getLeft(), childView.getTop(),
                    childView.getRight(), childView.getBottom());
            return;
        }
        dy /= S_SIZE;
        int yy = (int) (childView.getTop() - dy);

        if (yy < 0) { //防止子view向上移动过度
            childView.layout(childView.getLeft(), 0, childView.getRight(),
                    childView.getMeasuredHeight());
        } else {
            childView.layout(childView.getLeft(), yy, childView.getRight(),
                    (int) (childView.getBottom() - dy));
        }

    }

    ///////////////////////////////// NestedScrollingParent ////////////////////////
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        boolean rtn = (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        Log.d("NestedBounceView", "onStartNestedScroll return:" + rtn);
        return rtn;
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);

        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            moveRelease();
            mTotalUnconsumed = 0;
        }
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        mTotalUnconsumed = 0;
    }

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        if (dyUnconsumed < 0) {
            mTotalUnconsumed += Math.abs(dyUnconsumed);
            moveOn(dyUnconsumed);
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        Log.d("NestedBounceView", "onNestedPreScroll dy:" + dy);
        Log.d("NestedBounceView", "onNestedPreScroll mTotalUnconsumed:" + mTotalUnconsumed);
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;

            }
            moveOn(dy);
        }

    }
}
