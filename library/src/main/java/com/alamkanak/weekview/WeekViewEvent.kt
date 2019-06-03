package com.alamkanak.weekview

import android.content.Context
import android.graphics.Paint
import android.text.TextPaint
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.alamkanak.weekview.Constants.MINUTES_PER_HOUR
import java.util.Calendar

data class WeekViewEvent<T> internal constructor(
    var id: Long = 0L,
    var title: String = "",
    var startTime: Calendar = now(),
    var endTime: Calendar = now(),
    var location: String? = null,
    var isAllDay: Boolean = false,
    var style: Style = Style(),
    var data: T? = null
) : WeekViewDisplayable<T>, Comparable<WeekViewEvent<T>> {

    val isNotAllDay: Boolean
        get() = isAllDay.not()

    internal val isMultiDay: Boolean
        get() = isSameDay(endTime).not()

    internal fun getEffectiveStartMinutes(config: WeekViewConfigWrapper): Int {
        val startHour = startTime.hour - config.minHour
        return startHour * MINUTES_PER_HOUR.toInt() + startTime.minute
    }

    internal fun getEffectiveEndMinutes(config: WeekViewConfigWrapper): Int {
        val endHour = endTime.hour - config.minHour
        return endHour * MINUTES_PER_HOUR.toInt() + endTime.minute
    }

    internal fun isSameDay(other: Calendar): Boolean {
        return startTime.isSameDate(other)
    }

    internal fun isWithin(minHour: Int, maxHour: Int): Boolean {
        return startTime.hour >= minHour && endTime.hour <= maxHour
    }

    internal fun getTextPaint(
        context: Context,
        config: WeekViewConfigWrapper
    ): TextPaint {
        val textPaint = if (isAllDay) {
            config.allDayEventTextPaint
        } else {
            config.eventTextPaint
        }

        textPaint.color = when (val resource = style.textColorResource) {
            is ColorResource.ResourceId -> ContextCompat.getColor(context, resource.colorResId)
            is ColorResource.Value -> resource.color
            null -> config.eventTextPaint.color
        }

        // textPaint.color = if (style.textColor != 0) style.textColor else config.eventTextPaint.color

        if (style.isTextStrikeThrough) {
            textPaint.flags = textPaint.flags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        return textPaint
    }

    internal fun collidesWith(other: WeekViewEvent<T>): Boolean {
        if (isAllDay != other.isAllDay) {
            return false
        }

        if (startTime.isEqual(other.startTime) && endTime.isEqual(other.endTime)) {
            // Complete overlap
            return true
        }

        // Resolve collisions by shortening the preceding event by 1 ms
        if (endTime.isEqual(other.startTime)) {
            endTime = endTime.minusMillis(1)
            return false
        } else if (startTime.isEqual(other.endTime)) {
            other.endTime = other.endTime.minusMillis(1)
        }

        return !startTime.isAfter(other.endTime) && !endTime.isBefore(other.startTime)
    }

    internal fun startsOnEarlierDay(originalEvent: WeekViewEvent<T>): Boolean {
        return startTime.isNotEqual(originalEvent.startTime)
    }

    internal fun endsOnLaterDay(originalEvent: WeekViewEvent<T>): Boolean {
        return endTime.isNotEqual(originalEvent.endTime)
    }

    override fun compareTo(other: WeekViewEvent<T>): Int {
        var comparator = startTime.compareTo(other.startTime)
        if (comparator == 0) {
            comparator = endTime.compareTo(other.endTime)
        }
        return comparator
    }

    override fun toWeekViewEvent(): WeekViewEvent<T> = this

    internal sealed class ColorResource {
        data class Value(val color: Int) : ColorResource()
        data class ResourceId(val colorResId: Int) : ColorResource()
    }

    class Style {

        internal var backgroundColorResource: ColorResource? = null
        internal var textColorResource: ColorResource? = null

        internal var isTextStrikeThrough: Boolean = false
            private set

        internal var borderWidth: Int = 0
            private set

        internal var borderColorResource: ColorResource? = null

        internal val hasBorder: Boolean
            get() = borderWidth > 0

        internal fun getBackgroundColorOrDefault(config: WeekViewConfigWrapper): ColorResource {
            return backgroundColorResource ?: ColorResource.Value(config.defaultEventColor)
        }

        class Builder {

            private val style = Style()

            fun setBackgroundColor(@ColorInt color: Int): Builder {
                style.backgroundColorResource = ColorResource.Value(color)
                return this
            }

            fun setBackgroundColorResource(@ColorRes colorResId: Int): Builder {
                style.backgroundColorResource = ColorResource.ResourceId(colorResId)
                return this
            }

            fun setTextColor(@ColorInt color: Int): Builder {
                style.textColorResource = ColorResource.Value(color)
                return this
            }

            fun setTextColorResource(@ColorRes colorResId: Int): Builder {
                style.textColorResource = ColorResource.ResourceId(colorResId)
                return this
            }

            fun setTextStrikeThrough(strikeThrough: Boolean): Builder {
                style.isTextStrikeThrough = strikeThrough
                return this
            }

            fun setBorderWidth(width: Int): Builder {
                style.borderWidth = width
                return this
            }

            fun setBorderColor(@ColorInt color: Int): Builder {
                style.borderColorResource = ColorResource.Value(color)
                return this
            }

            fun setBorderColorResource(@ColorRes colorResId: Int): Builder {
                style.borderColorResource = ColorResource.ResourceId(colorResId)
                return this
            }

            fun build(): Style {
                return style
            }

        }

    }

    class Builder<T> {

        private val event = WeekViewEvent<T>()

        fun setId(id: Long): Builder<T> {
            event.id = id
            return this
        }

        fun setTitle(title: String): Builder<T> {
            event.title = title
            return this
        }

        fun setStartTime(startTime: Calendar): Builder<T> {
            event.startTime = startTime
            return this
        }

        fun setEndTime(endTime: Calendar): Builder<T> {
            event.endTime = endTime
            return this
        }

        fun setLocation(location: String): Builder<T> {
            event.location = location
            return this
        }

        fun setStyle(style: Style): Builder<T> {
            event.style = style
            return this
        }

        fun setAllDay(isAllDay: Boolean): Builder<T> {
            event.isAllDay = isAllDay
            return this
        }

        fun setData(data: T): Builder<T> {
            event.data = data
            return this
        }

        fun build(): WeekViewEvent<T> {
            return event
        }

    }

}
