package com.appcraft.zoompanviewgroup.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.SizeF
import android.view.*
import android.widget.RelativeLayout
import android.widget.Scroller
import java.lang.ref.WeakReference


fun Float.clamped(min: Float, max: Float): Float {
    return Math.max(min, Math.min(this, max))
}

fun ViewGroup.getAllChildren(v: View = this): List<View> {
    return if (v !is ViewGroup) ArrayList<View>(1).apply { add(v) }
    else ArrayList<View>().apply {
        for (i in 0 until v.childCount) {
            addAll(getAllChildren(getChildAt(i)))
        }
    }
}

class ZoomPanViewGroup : RelativeLayout, ScaleGestureDetector.OnScaleGestureListener,
    GestureDetector.OnGestureListener {

    private val clickListener: WeakReference<OnClickListener>? = null
    private val lastClick = PointF()

    private val mScaleDetector = ScaleGestureDetector(context, this)
    private val mScrollDetector = GestureDetector(context, this)
    private val scroller = Scroller(context)

    private var mLeft = 0.0f
    private var mTop = 0.0f
    private var mRight = 0.0f
    private var mBottom = 0.0f

    private var offset = PointF(0.0f, 0.0f)
    private var scale = 1.0f

    private val layoutComputationRect = Rect()

    private val maxOffset: PointF
        get() = PointF(
            xOffset + Math.max(0f, (size.width - width) / 2),
            yOffset + Math.max(0f, (size.height - height) / 2)
        )

    private val minOffset: PointF
        get() = PointF(
            xOffset - Math.max(0f, (size.width - width) / 2),
            yOffset - Math.max(0f, (size.height - height) / 2)
        )

    private val xOffset
        get() = (width / 2) - (size.width / 2) - (mLeft * scale)

    private val yOffset
        get() = (height / 2) - (size.height / 2) - (mTop * scale)

    private val size: SizeF
        get() = SizeF(
            (mRight - mLeft) * scale,
            (mBottom - mTop) * scale
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
    override fun onDown(p0: MotionEvent?) = true
    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        with(motionEventToCoords(p0)) {
            lastClick.x = x
            lastClick.y = y
            clickListener?.get()?.onClick(this)
        }
        redraw()
        return true
    }

    private fun motionEventToCoords(motionEvent: MotionEvent) = PointF(
        motionEvent.x / scale - mLeft - offset.x / scale,
        motionEvent.y / scale - mTop - offset.y / scale
    )

    override fun onScaleEnd(p0: ScaleGestureDetector) {}
    override fun onScaleBegin(p0: ScaleGestureDetector) = true

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
        (scale * detector.scaleFactor).let {
            if (it in MIN_ZOOM..MAX_ZOOM) {
                prepareToScale(detector.scaleFactor, PointF(detector.focusX, detector.focusY))
                scale = it
                redraw()
            }
        }
        return true
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
        debug(canvas)
    }

    private fun debug(canvas: Canvas) {
        val step = height / 60f

        canvas.drawLine((width / 2).toFloat(), 0f, (width / 2).toFloat(), height.toFloat(), Paint().apply {
            color = Color.GREEN
            strokeWidth = 3f
        })
        canvas.drawLine(0f, (height / 2).toFloat(), width.toFloat(), (height / 2).toFloat(), Paint().apply {
            color = Color.GREEN
            strokeWidth = 3f
        })

        canvas.drawText(
            "mLeft: $mLeft, mTop: $mTop, mRight: $mRight, mBottom: $mBottom",
            step,
            step * 2,
            Paint().apply {
                textSize = step
                color = Color.GREEN
            })
        canvas.drawText(
            "MIN_X: ${minOffset.x}, MAX_X: ${maxOffset.x}, MIN_Y: ${minOffset.y} MAX_Y: ${maxOffset.y}",
            step,
            step * 4,
            Paint().apply {
                textSize = step
                color = Color.GREEN
            })
        canvas.drawText("X: ${offset.x}, Y: ${offset.y}", step, step * 6, Paint().apply {
            textSize = step
            color = Color.GREEN
        })
        canvas.drawText("LAST_CLICK_X: ${lastClick.x}, LAST_CLICK_Y: ${lastClick.y}", step, step * 8, Paint().apply {
            textSize = step
            color = Color.GREEN
        })
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        mLeft = l.toFloat()
        mTop = t.toFloat()
        mRight = 0f
        mBottom = 0f
        getAllChildren().forEach { child ->
            if (child.visibility != View.GONE && (child !is ViewGroup)) {
                layoutComputationRect.apply {
                    child.getDrawingRect(this@apply)
                    this@ZoomPanViewGroup.offsetDescendantRectToMyCoords(child, this@apply)
                    mLeft = Math.max(mLeft, this.left.toFloat())
                    mTop = Math.max(mTop, this.top.toFloat())
                    mRight = Math.max(mRight, this.right.toFloat())
                    mBottom = Math.max(mBottom, this.bottom.toFloat())
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            if (!scroller.isFinished) scroller.abortAnimation()
        }
        mScrollDetector.onTouchEvent(event)
        mScaleDetector.onTouchEvent(event)
        return true
    }

    interface OnClickListener {
        fun onClick(point: PointF)
        fun onDoubleTap(point: PointF)
        fun onLongPress(point: PointF)
    }
}
