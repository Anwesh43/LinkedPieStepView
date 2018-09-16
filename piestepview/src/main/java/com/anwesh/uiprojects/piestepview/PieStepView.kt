package com.anwesh.uiprojects.piestepview

/**
 * Created by anweshmishra on 17/09/18.
 */

import android.app.Activity
import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Color

val nodes : Int = 5

fun Canvas.drawPSNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val r : Float = gap / 4
    paint.color = Color.parseColor("#FFA726")
    save()
    translate(w/2, gap + i * gap)
    for (j in 0..1) {
        val sf : Float = 1f - 2 * j
        val sc : Float = Math.min(0.5f, Math.max(0f, scale - j * 0.5f)) * 2
        val sc1 : Float = Math.min(0.5f, sc) * 2
        val sc2 : Float = Math.min(0.5f, Math.max(0.5f, sc - 0.5f)) * 2
        save()
        translate(w/2 * sc2 * sf, 0f)
        rotate(180f * sc2)
        if (sc1 == 0f) {
            drawLine(0f, 0f, r, 0f, paint)
        } else {
            drawArc(RectF(-r, -r, r, r), 180f * j -90f * sc1, 180f * sc1, true, paint)
        }
        restore()
    }
    restore()
}

class PieStepView (ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += 0.1f * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class PSNode(var i : Int, val state : State = State()) {

        private var next : PSNode? = null
        private var prev : PSNode? = null

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = PSNode(i + 1)
                next?.prev = this
            }
        }

        fun update(cb : (Int, Float) -> Unit){
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        init {
            addNeighbor()
        }

        fun getNext(dir : Int, cb : () -> Unit) : PSNode {
            var curr : PSNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawPSNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }
    }


    data class PieStep(var i : Int) {
        private var dir : Int = 1
        private var root : PSNode = PSNode(0)
        private var curr : PSNode = root

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : PieStepView) {
        private val pieStep : PieStep = PieStep(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            pieStep.draw(canvas, paint)
            animator.animate {
                pieStep.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            pieStep.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity) : PieStepView {
            val view : PieStepView = PieStepView(activity)
            activity.setContentView(view)
            return view
        }
    }
}