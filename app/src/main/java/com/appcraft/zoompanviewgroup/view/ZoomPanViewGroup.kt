package com.appcraft.zoompanviewgroup.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.RelativeLayout
import android.widget.Scroller
import java.util.concurrent.atomic.AtomicBoolean


class ZoomPanViewGroup : RelativeLayout, ScaleGestureDetector.OnScaleGestureListener,
    GestureDetector.OnGestureListener {
    private val mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, this)
    private val mScrollDetector: GestureDetector = GestureDetector(context, this)
    private val scroller: Scroller = Scroller(context)

    private var contentWidth = 0f
    private var contentHeight = 0f
    private var mLeft = 0
    private var mTop = 0
    private var mRight = 0
    private var mBottom = 0

    private var xPos = 0f
    private var yPos = 0f

    private var lastFocusX: Float = 0f
    private var lastFocusY: Float = 0f

    private var lastFocusDeltaX: Float = 0f
    private var lastFocusDeltaY: Float = 0f

    private var mScale = 1f

    private val isScale = AtomicBoolean(false)

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attr: AttributeSet?) : super(context, attr)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        setLayerType(View.LAYER_TYPE_NONE, null)
    }

    private fun minX(): Int = Math.min(0, (width - contentWidth * mScale).toInt()).toInt()

    private fun maxX(): Int = 0

    private fun minY(): Int = Math.min(0, (height - contentHeight * mScale).toInt()).toInt()

    private fun maxY(): Int = 0

    override fun onShowPress(p0: MotionEvent?) {

    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {

    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        scroller.fling(
            (xPos).toInt(), (yPos).toInt(),
            (velocityX).toInt(), (velocityY).toInt(),
            (minX()), (maxX()),
            (minY()), (maxY())
        )
        return true
    }

    // Scroll part >>>
    // Scroll by finger
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        xPos -= distanceX
        yPos -= distanceY
        if (xPos < minX()) xPos = minX().toFloat()
        if (xPos > maxX()) xPos = maxX().toFloat()
        if (yPos < minY()) yPos = minY().toFloat()
        if (yPos > maxY()) yPos = maxY().toFloat()
        if (isScale.get()) return true
        postInvalidate()
        return true
    }

    // Scroll by scroller
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            xPos = scroller.currX.toFloat()
            yPos = scroller.currY.toFloat()
            postInvalidate()
        }
    }
    // Scroll part <<<

    // Scale part >>>
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        mScale *= detector.scaleFactor
        lastFocusDeltaX = detector.focusX - lastFocusX
        lastFocusDeltaY = detector.focusY - lastFocusY
        lastFocusX = detector.focusX
        lastFocusY = detector.focusY
        postInvalidate()
        return true
    }

    override fun onScaleBegin(p0: ScaleGestureDetector): Boolean {
        isScale.set(true)
        lastFocusX = p0.focusX
        lastFocusY = p0.focusY
        lastFocusDeltaX = 0f
        lastFocusDeltaY = 0F
        return true
    }

    override fun onScaleEnd(p0: ScaleGestureDetector) {
        isScale.set(false)
    }
    // Scale part <<<

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val childCount = childCount
        contentWidth = 0f
        contentHeight = 0f
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                mLeft = Math.min(mLeft, l)
                mTop = Math.min(mTop, t)
                mRight = Math.max(mRight, l + child.measuredWidth)
                mBottom = Math.max(mBottom, t + child.measuredHeight)
                contentWidth = Math.max(contentWidth, child.measuredWidth.toFloat())
                contentHeight = Math.max(contentHeight, child.measuredHeight.toFloat())
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(xPos, yPos)
        val fX = lastFocusX - xPos
        val fY = lastFocusY - yPos
//        canvas.translate(-fX, -fY)
        canvas.scale(mScale, mScale)
//        canvas.drawCircle((fX), (fY), 300 / mScale, Paint().apply {
//            style = Paint.Style.FILL
//            strokeWidth = 30f
//            color = Color.rgb(0, 100, 0)
//        })
//        canvas.translate(lastFocusDeltaX + fX, lastFocusDeltaY + fY)
        super.dispatchDraw(canvas)
        canvas.restore()
        debug(canvas)
    }

    private fun debug(canvas: Canvas) {
        canvas.drawLine((width / 2).toFloat(), 0f, (width / 2).toFloat(), height.toFloat(), Paint().apply {
            color = Color.GREEN
            strokeWidth = 1f
        })
        canvas.drawLine(0f, (height / 2).toFloat(), width.toFloat(), (height / 2).toFloat(), Paint().apply {
            color = Color.GREEN
            strokeWidth = 1f
        })

        val step = height / 60f
        canvas.drawCircle((lastFocusX), (lastFocusY), step, Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.GREEN
        })
        canvas.drawText(mScale.toString(), step, step * 2, Paint().apply {
            textSize = step
            color = Color.YELLOW
        })
        canvas.drawText("width: $contentWidth, height: $contentHeight", step, step * 4, Paint().apply {
            textSize = step
            color = Color.WHITE
        })
        canvas.drawText(
            "real width: ${contentWidth * mScale}, real height: ${contentHeight * mScale}",
            step,
            step * 6,
            Paint().apply {
                textSize = step
                color = Color.WHITE
            })
        canvas.drawText(
            "minX: ${minX()}, maxX: ${maxX()} minY: ${minY()}, maxY: ${maxY()}",
            step,
            step * 8,
            Paint().apply {
                textSize = step
                color = Color.RED
            })
        canvas.drawText("xPos: $xPos, yPos: $yPos", step, step * 10, Paint().apply {
            textSize = step
            color = Color.WHITE
        })
        canvas.drawText(
            "lastFocusX: $lastFocusX ($lastFocusDeltaX), lastFocusY: $lastFocusY ($lastFocusDeltaY)",
            step,
            step * 12,
            Paint().apply {
                textSize = step
                color = Color.RED
            })
        canvas.drawText(
            "mLeft: $mLeft, mTop: $mTop, mRight: $mRight, mBottom: $mBottom",
            step,
            step * 14,
            Paint().apply {
                textSize = step
                color = Color.GREEN
            })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
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
