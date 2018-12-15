package com.appcraft.zoompanviewgroup.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.SizeF
import android.view.*
import android.widget.RelativeLayout
import android.widget.Scroller


fun Float.clamped(min: Float, max: Float): Float {
    return Math.max(min, Math.min(this, max))
}

fun ViewGroup.getAllChildren(v: View = this): List<View> {
    if (v !is ViewGroup) {
        val viewArrayList = ArrayList<View>()
        viewArrayList.add(v)
        return viewArrayList
    }
    val result = ArrayList<View>()
    for (i in 0 until v.childCount) {
        val child = v.getChildAt(i)
        result.addAll(getAllChildren(child))
    }
    return result
}

class ZoomPanViewGroup : RelativeLayout, ScaleGestureDetector.OnScaleGestureListener,
    GestureDetector.OnGestureListener {

    private val mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private val mScrollDetector: GestureDetector = GestureDetector(context, this)
    private val scroller: Scroller = Scroller(context)

    private var mLeft = 0.0f
    private var mTop = 0.0f
    private var mRight = 0.0f
    private var mBottom = 0.0f

    private var offset = PointF(0.0f, 0.0f)
    private var scale = 1.0f

    private val layoutComputationRect = Rect()

    private val maxOffset: PointF
        get() = PointF(-mLeft * scale, -mTop * scale)

    private val minOffset: PointF
        get() = PointF(
            Math.min(0.0f, (width - size.width)),
            Math.min(0.0f, (height - size.height))
        )

    private val size: SizeF
        get() = SizeF(
            mRight * scale,
            mBottom * scale
        )

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attr: AttributeSet?) : super(context, attr)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 10f
    }

    init {
        setLayerType(View.LAYER_TYPE_NONE, null)
    }

    override fun onShowPress(p0: MotionEvent?) {}
    override fun onLongPress(p0: MotionEvent?) {}
    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onScaleEnd(p0: ScaleGestureDetector) {}
    override fun onScaleBegin(p0: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        scroller.fling(
            (offset.x).toInt(), (offset.y).toInt(),
            (velocityX).toInt(), (velocityY).toInt(),
            minOffset.x.toInt(), maxOffset.x.toInt(),
            minOffset.y.toInt(), maxOffset.y.toInt()
        )
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            offset.x = scroller.currX.toFloat()
            offset.y = scroller.currY.toFloat()
            redraw()
        }
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, deltaX: Float, deltaY: Float): Boolean {
        offset.x -= deltaX
        offset.y -= deltaY
        redraw()
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        scale *= detector.scaleFactor
        return if (scale in MIN_ZOOM..MAX_ZOOM) {
            prepareToScale(detector.scaleFactor, PointF(detector.focusX, detector.focusY))
            redraw()
            true
        } else {
            scale = scale.clamped(MIN_ZOOM, MAX_ZOOM)
            false
        }
    }

    private fun prepareToScale(scaleBy: Float, focus: PointF) {
        val widthGrowth = size.width * (scaleBy - 1)
        val heightGrowth = size.height * (scaleBy - 1)

        val canvasFocus = PointF(focus.x - offset.x, focus.y - offset.y)
        val xFocusRelation = canvasFocus.x / size.width
        val yFocusRelation = canvasFocus.y / size.height

        offset.x -= widthGrowth * xFocusRelation
        offset.y -= heightGrowth * yFocusRelation
    }

    private fun clampOffset() {
        offset.x = offset.x.clamped(minOffset.x, maxOffset.x)
        offset.y = offset.y.clamped(minOffset.y, maxOffset.y)
    }

    private fun redraw() {
        clampOffset()
        postInvalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(offset.x, offset.y)
        canvas.scale(scale, scale)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        mLeft = l.toFloat()
        mTop = t.toFloat()
        mRight = 0f
        mBottom = 0f
        getAllChildren().forEach { child ->
            if (child.visibility != View.GONE) {
                layoutComputationRect.apply {
                    child.getDrawingRect(this@apply)
                    this@ZoomPanViewGroup.offsetDescendantRectToMyCoords(child, this)
                    mLeft = Math.min(mLeft, this.left.toFloat())
                    mTop = Math.min(mTop, this.top.toFloat())
                    mRight = Math.max(mRight, this.right.toFloat())
                    mBottom = Math.max(mBottom, this.bottom.toFloat())
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        getAllChildren().forEach { child ->
            if (child.visibility != View.GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            if (!scroller.isFinished) scroller.abortAnimation()
        }
        mScrollDetector.onTouchEvent(event)
        mScaleDetector.onTouchEvent(event)
        return true
    }
}
