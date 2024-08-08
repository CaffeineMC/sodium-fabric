/*
MIT License

Copyright (c) 2016 lodborg

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

Modified for inclusion in Sodium by douira.
Copied from https://github.com/lodborg/interval-tree/tree/v1.0.0.
*/
package net.caffeinemc.mods.sodium.client.util.interval_tree;

import java.util.Comparator;

/**
 * A representation of a generic interval. The interval can be open or closed (the start
 * and end points may be inclusive or exclusive), as well as bounded and unbounded (it can
 * extend to positive or negative infinity).
 * <p>
 * The class doesn't assume that the intervals are numeric, instead it is generalized to
 * represent a contiguous subset of elements, where contiguity is defined with respect to
 * an arbitrary total order function. These elements can be for example {@link java.util.Date}s
 * or basically any type, the elements of which can be compared to one another. Since the
 * class requires its generic variable to implement the {@link Comparable} interface, all
 * comparisons in the internals of the {@code Interval} class are done via the interface method.
 * <p>
 * When subclassing the {@code Interval} class, note that the start and end points of the
 * interval <strong>can</strong> be {@code null}. A {@code null} start point represents
 * the negative infinity and a {@code null} end point represents positive infinity. This
 * fact needs to be kept in mind in particular when overwriting methods with default
 * implementations in the {@code Interval} class, such as {@link #contains(Comparable)},
 * {@link #isLeftOf(Comparable)}, and in particular {@link #equals(Object)} and
 * {@link #hashCode()}.
 *
 * @param <T> The type that represents a single point from the domain of definition of the
 *            interval.
 */
public abstract class Interval<T extends Comparable<? super T>> {
    private T start, end;
    private boolean isStartInclusive, isEndInclusive;

    /**
     * An enum representing all possible types of bounded intervals.
     */
    public enum Bounded {
        /**
         * An interval, in which both start and end point are exclusive.
         */
        OPEN,

        /**
         * An interval, in which both start and end point are inclusive.
         */
        CLOSED,

        /**
         * An interval, in which the start is exclusive and the end is inclusive.
         */
        CLOSED_RIGHT,

        /**
         * An interval, in which the start is inclusive and the end is exclusive.
         */
        CLOSED_LEFT
    }

    public enum Unbounded {
        /**
         * An interval extending to positive infinity and having an exclusive start
         * point as a lower bound. For example, (5, +inf)
         */
        OPEN_LEFT,

        /**
         * An interval extending to positive infinity and having an inclusive start
         * point as a lower bound. For example, [5, +inf)
         */
        CLOSED_LEFT,

        /**
         * An interval extending to negative infinity and having an exclusive end
         * point as an upper bound. For example, (-inf, 5)
         */
        OPEN_RIGHT,

        /**
         * An interval extending to negative infinity and having an inclusive end
         * point as an upper bound. For example, (-inf, 5]
         */
        CLOSED_RIGHT
    }

    /**
     * Instantiates a new interval representing all points in the domain of definition,
     * i.e. this will instantiate the interval (-inf, +inf).
     */
    public Interval() {
        this.isStartInclusive = true;
        this.isEndInclusive = true;
    }

    /**
     * Instantiates a new bounded interval.
     *
     * @param start The start point of the interval
     * @param end   The end point of the interval.
     * @param type  Description of whether the interval is open/closed at one or both
     *              of its ends. See {@link Bounded the documentation of the Bounded enum}
     *              for more information on the different possibilities.
     */
    public Interval(T start, T end, Bounded type) {
        this.start = start;
        this.end = end;
        if (type == null)
            type = Bounded.CLOSED;
        switch (type) {
            case OPEN:
                break;
            case CLOSED:
                this.isStartInclusive = true;
                this.isEndInclusive = true;
                break;
            case CLOSED_RIGHT:
                this.isEndInclusive = true;
                break;
            default:
                this.isStartInclusive = true;
                break;
        }
    }

    /**
     * Instantiates a new unbounded interval - an interval that extends to positive or
     * negative infinity. The interval will be bounded by either the start point
     * or the end point and unbounded in the other point.
     *
     * @param value The bounding value for either the start or the end point.
     * @param type  Describes, if the interval extends to positive or negative infinity,
     *              as well as if it is open or closed at the bounding point. See {@link Unbounded
     *              the Unbounded enum} for description of the different possibilities.
     */
    public Interval(T value, Unbounded type) {
        if (type == null)
            type = Unbounded.CLOSED_RIGHT;
        switch (type) {
            case OPEN_LEFT:
                this.start = value;
                this.isStartInclusive = false;
                this.isEndInclusive = true;
                break;
            case CLOSED_LEFT:
                this.start = value;
                this.isStartInclusive = true;
                this.isEndInclusive = true;
                break;
            case OPEN_RIGHT:
                this.end = value;
                this.isStartInclusive = true;
                this.isEndInclusive = false;
                break;
            default:
                this.end = value;
                this.isStartInclusive = true;
                this.isEndInclusive = true;
                break;
        }
    }

    /**
     * Checks if the current interval contains no points.
     *
     * <p>In particular, if the end point is less than the start point, then the interval is
     * considered to be empty. There are, however other instances, in which an interval is empty.
     * For example, in the class {IntegerInterval}, an open interval, whose start and end
     * points differ by one, for example the interval (4, 5), is empty, because it contains no integers
     * in it. The same interval, however, will <strong>not</strong> be considered empty in the
     * {@link DoubleInterval} class, because there are Double numbers within this interval.
     * </p>
     *
     * @return {@code true}, if the current interval is empty or {@code false} otherwise.
     */
    public boolean isEmpty() {
        if (this.start == null || this.end == null)
            return false;
        int compare = this.start.compareTo(this.end);
        if (compare > 0)
            return true;
        return compare == 0 && (!this.isEndInclusive || !this.isStartInclusive);
    }

    /**
     * Used to create new instances of a specific {@code Interval} subclass.
     * <p>
     * The {@code Interval} class aims to avoid reflexion. On several occasions, however, the class
     * needs to create new instances of the {@code Interval} class. To be able to guarantee that they
     * will have the desired runtime type, the {@link #create()} method of a specific reference object
     * is called.
     * </p>
     * <p>
     * Generally, the only thing you need to do in your implementation of this abstract method is
     * to call the default constructor of your subclass and return the new interval.
     * </p>
     *
     * @return A new instance of the particular {@code Interval} class.
     */
    @SuppressWarnings("JavadocDeclaration")
    protected abstract Interval<T> create();

    /**
     * Returns the center of the current interval. If the center of the interval exists, but can't
     * be determined, return any point inside the interval. This method will be used only to
     * instantiate the midpoint of a new {@link TreeNode} inside a {@link IntervalTree}, which is why
     * it is not necessary to return exactly the center of the interval, but it will help the
     * {@link IntervalTree} perform slightly better.
     *
     * @return The center point of the current interval, if it exists or {@code null} otherwise. If the
     * center exists but can't be determined correctly, return any point inside the interval.
     */
    public abstract T getMidpoint();

    /**
     * Creates a new instance of the particular {@code Interval} subclass.
     *
     * @param start            The start point of the interval
     * @param isStartInclusive {@code true}, if the start is inclusive or false otherwise
     * @param end              The end point of the interval
     * @param isEndInclusive   {@code true}, if the end is inclusive or false otherwise
     * @return The newly created interval.
     */
    protected Interval<T> create(T start, boolean isStartInclusive, T end, boolean isEndInclusive) {
        Interval<T> interval = create();
        interval.start = start;
        interval.isStartInclusive = isStartInclusive;
        interval.end = end;
        interval.isEndInclusive = isEndInclusive;
        return interval;
    }

    /**
     * Returns the start point of the interval.
     */
    public T getStart() {
        return this.start;
    }

    /**
     * Returns the end point of the interval.
     */
    public T getEnd() {
        return this.end;
    }

    /**
     * Returns {@code true}, if the start point is a part of the interval, or false otherwise.
     */
    public boolean isStartInclusive() {
        return this.isStartInclusive;
    }

    /**
     * Returns {@code true}, if the end point is a part of the interval, or false otherwise.
     */
    public boolean isEndInclusive() {
        return this.isEndInclusive;
    }

    /**
     * Determines if the current interval contains a query point.
     *
     * @param query The point.
     * @return {@code true}, if the current interval contains the {@code query} point or false otherwise.
     */
    public boolean contains(T query) {
        if (isEmpty() || query == null) {
            return false;
        }

        int startCompare = this.start == null ? 1 : query.compareTo(this.start);
        int endCompare = this.end == null ? -1 : query.compareTo(this.end);
        if (startCompare > 0 && endCompare < 0) {
            return true;
        }
        return (startCompare == 0 && this.isStartInclusive) || (endCompare == 0 && this.isEndInclusive);
    }

    /**
     * Returns an interval, representing the intersection of two intervals. More formally, for every
     * point {@code x} in the returned interval, {@code x} will belong in both the current interval
     * and the {@code other} interval.
     *
     * @param other The other interval
     * @return The intersection of the current interval wih the {@code other} interval.
     */
    public Interval<T> getIntersection(Interval<T> other) {
        if (other == null || isEmpty() || other.isEmpty())
            return null;
        // Make sure that the one with the smaller starting point gets intersected with the other.
        // If necessary, swap the intervals
        if ((other.start == null && this.start != null) || (this.start != null && this.start.compareTo(other.start) > 0))
            return other.getIntersection(this);
        if (this.end != null && other.start != null && (this.end.compareTo(other.start) < 0 || (this.end.compareTo(other.start) == 0 && (!this.isEndInclusive || !other.isStartInclusive))))
            return null;

        T newStart, newEnd;
        boolean isNewStartInclusive, isNewEndInclusive;

        // If other.start is null, this means my start is also null, because we made sure
        // that the caller object hast the smaller start point => the new start is null
        if (other.start == null) {
            newStart = null;
            isNewStartInclusive = true;
        } else {
            newStart = other.start;
            if (this.start != null && other.start.compareTo(this.start) == 0)
                isNewStartInclusive = other.isStartInclusive && this.isStartInclusive;
            else
                isNewStartInclusive = other.isStartInclusive;
        }

        if (this.end == null) {
            newEnd = other.end;
            isNewEndInclusive = other.isEndInclusive;
        } else if (other.end == null) {
            newEnd = this.end;
            isNewEndInclusive = this.isEndInclusive;
        } else {
            int compare = this.end.compareTo(other.end);
            if (compare == 0) {
                newEnd = this.end;
                isNewEndInclusive = this.isEndInclusive && other.isEndInclusive;
            } else if (compare < 0) {
                newEnd = this.end;
                isNewEndInclusive = this.isEndInclusive;
            } else {
                newEnd = other.end;
                isNewEndInclusive = other.isEndInclusive;
            }
        }
        Interval<T> intersection = create(newStart, isNewStartInclusive, newEnd, isNewEndInclusive);
        return intersection.isEmpty() ? null : intersection;
    }

    /**
     * Checks if the current interval intersects another interval. More formally, this method
     * returns {@code true} if there is at least one point the current interval, that also
     * belongs to the {@code query} interval.
     *
     * @param query The interval being checked for intersection with the current interval.
     * @return {@code true}, if the two intervals intersect or {@code false} otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean intersects(Interval<T> query) {
        if (query == null)
            return false;
        Interval<T> intersection = getIntersection(query);
        return intersection != null;
    }

    /**
     * This method checks, if this current interval is entirely to the right of a point. More formally,
     * the method will return {@code true}, if for every point {@code x} from the current interval the inequality
     * {@code x} &gt; {@code point} holds. If the parameter {@code inclusive} is set to {@code false}, this
     * method will return {@code true} also if the start point of the interval is equal to the reference
     * {@code point}.
     *
     * @param point     The reference point
     * @param inclusive {@code false} if the reference {@code point} is allowed to be the start point
     *                  of the current interval.
     * @return {@code true}, if the current interval is entirely to the right of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isRightOf(T point, boolean inclusive) {
        if (point == null || this.start == null)
            return false;
        int compare = point.compareTo(this.start);
        if (compare != 0)
            return compare < 0;
        return !isStartInclusive() || !inclusive;
    }

    /**
     * This method checks, if this current interval is entirely to the right of a point. More formally,
     * the method will return true, if for every point {@code x} from the current interval the inequality
     * {@code x} &gt; {@code point} holds. This formal definition implies in particular that if the start point
     * of the current interval is equal to the reference {@code point} and the end point is open, the method
     * will return {@code true}.
     *
     * @param point The reference point
     * @return {@code true}, if the current interval is entirely to the right of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isRightOf(T point) {
        return isRightOf(point, true);
    }

    /**
     * This method checks, if this current interval is entirely to the right of another interval
     * with no common points. More formally, the method will return true, if for every point {@code x}
     * from the current interval and for every point {@code y} from the {@code other} interval the
     * inequality {@code x} &gt; {@code y} holds. This formal definition implies in particular that if the start point
     * of the current interval is equal to the end point of the {@code other} interval, the method
     * will return {@code false} only if both points are inclusive and {@code true} in all other cases.
     *
     * @param other The reference interval
     * @return {@code true}, if the current interval is entirely to the right of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isRightOf(Interval<T> other) {
        if (other == null || other.isEmpty())
            return false;
        return isRightOf(other.end, other.isEndInclusive());
    }

    /**
     * This method checks, if this current interval is entirely to the left of a point. More formally,
     * the method will return {@code true}, if for every point {@code x} from the current interval the inequality
     * {@code x} &lt; {@code point} holds. If the parameter {@code inclusive} is set to {@code false}, this
     * method will return {@code true} also if the end point of the interval is equal to the reference
     * {@code point}.
     *
     * @param point     The reference point
     * @param inclusive {@code false} if the reference {@code point} is allowed to be the end point
     *                  of the current interval.
     * @return {@code true}, if the current interval is entirely to the left of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isLeftOf(T point, boolean inclusive) {
        if (point == null || this.end == null)
            return false;
        int compare = point.compareTo(this.end);
        if (compare != 0)
            return compare > 0;
        return !isEndInclusive() || !inclusive;
    }

    /**
     * This method checks, if this current interval is entirely to the left of a point. More formally,
     * the method will return true, if for every point {@code x} from the current interval the inequality
     * {@code x} &lt; {@code point} holds. This formal definition implies in particular that if the end point
     * of the current interval is equal to the reference {@code point} and the end point is open, the method
     * will return {@code true}.
     *
     * @param point The reference point
     * @return {@code true}, if the current interval is entirely to the left of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isLeftOf(T point) {
        return isLeftOf(point, true);
    }

    /**
     * This method checks, if this current interval is entirely to the left of another interval
     * with no common points. More formally, the method will return true, if for every point {@code x}
     * from the current interval and for every point {@code y} from the {@code other} interval the
     * inequality {@code x} &lt; {@code y} holds. This formal definition implies in particular that if the end point
     * of the current interval is equal to the start point of the {@code other} interval, the method
     * will return {@code false} only if both points are inclusive and {@code true} in all other cases.
     *
     * @param other The reference interval
     * @return {@code true}, if the current interval is entirely to the left of the {@code other}
     * interval, or {@code false} instead.
     */
    public boolean isLeftOf(Interval<T> other) {
        if (other == null || other.isEmpty())
            return false;
        return isLeftOf(other.start, other.isStartInclusive());
    }

    /**
     * A {@link Comparator} that only considers the start points of the intervals. It can not and must
     * not be used as a standalone {@link Comparator}. It only serves to create a more readable and
     * modular code.
     */
    private int compareStarts(Interval<T> other) {
        if (this.start == null && other.start == null)
            return 0;
        if (this.start == null)
            return -1;
        if (other.start == null)
            return 1;
        int compare = this.start.compareTo(other.start);
        if (compare != 0)
            return compare;
        if (this.isStartInclusive ^ other.isStartInclusive)
            return this.isStartInclusive ? -1 : 1;
        return 0;
    }

    /**
     * A {@link Comparator} that only considers the end points of the intervals. It can not and must
     * not be used as a standalone {@link Comparator}. It only serves to create a more readable and
     * modular code.
     */
    private int compareEnds(Interval<T> other) {
        if (this.end == null && other.end == null)
            return 0;
        if (this.end == null)
            return 1;
        if (other.end == null)
            return -1;
        int compare = this.end.compareTo(other.end);
        if (compare != 0)
            return compare;
        if (this.isEndInclusive ^ other.isEndInclusive)
            return this.isEndInclusive ? 1 : -1;
        return 0;
    }

    /**
     * A comparator that can be used as a parameter for sorting functions. The start comparator sorts the intervals
     * in <em>ascending</em> order by placing the intervals with a smaller start point before intervals with greater
     * start points. This corresponds to a line sweep from left to right.
     * <p>
     * Intervals with start point null (negative infinity) are considered smaller than all other intervals.
     * If two intervals have the same start point, the closed start point is considered smaller than the open one.
     * For example, [0, 2) is considered smaller than (0, 2).
     * </p>
     * <p>
     * To ensure that this comparator can also be used in sets it considers the end points of the intervals, if the
     * start points are the same. Otherwise the set will not be able to handle two different intervals, sharing
     * the same starting point, and omit one of the intervals.
     * </p>
     * <p>
     * Since this is a static method of a generic class, it involves unchecked calls to class methods. It is left to
     * ths user to ensure that she compares intervals from the same class, otherwise an exception might be thrown.
     * </p>
     */
    public static final Comparator<Interval> sweepLeftToRight = (a, b) -> {
        int compare = a.compareStarts(b);
        if (compare != 0)
            return compare;
        compare = a.compareEnds(b);
        if (compare != 0)
            return compare;
        return a.compareSpecialization(b);
    };

    /**
     * A comparator that can be used as a parameter for sorting functions. The end comparator sorts the intervals
     * in <em>descending</em> order by placing the intervals with a greater end point before intervals with smaller
     * end points. This corresponds to a line sweep from right to left.
     * <p>
     * Intervals with end point null (positive infinity) are placed before all other intervals. If two intervals
     * have the same end point, the closed end point is placed before the open one. For example,  [0, 10) is placed
     * after (0, 10].
     * </p>
     * <p>
     * To ensure that this comparator can also be used in sets it considers the start points of the intervals, if the
     * end points are the same. Otherwise the set will not be able to handle two different intervals, sharing
     * the same end point, and omit one of the intervals.
     * </p>
     * <p>
     * Since this is a static method of a generic class, it involves unchecked calls to class methods. It is left to
     * ths user to ensure that she compares intervals from the same class, otherwise an exception might be thrown.
     * </p>
     */
    public static final Comparator<Interval> sweepRightToLeft = (a, b) -> {
        int compare = b.compareEnds(a);
        if (compare != 0)
            return compare;
        compare = b.compareStarts(a);
        if (compare != 0)
            return compare;
        return a.compareSpecialization(b);
    };

    /**
     * A method that should be overwritten by subclasses of {@code Interval}, if they have properties
     * that characterize the objects of the class and are used to identify them. It is used to create
     * a total order between distinct objects, that would otherwise be considered equal, if only
     * the start and end points were considered. If you don't have any such special properties, you
     * may leave the default implementation of this method.
     * <p>
     * This method functions as a traditional {@link Comparator}, bit can not and should not be used
     * on its own, nor should it be implemented as a full standalone comparator. Instead, it is always
     * used in conjunction with one of the two base {@link Comparator}s in the {@code Interval} class -
     * {@link #sweepLeftToRight} and {@link #sweepRightToLeft}. This method will only be executed if
     * the main comparator returns 0, i.e. if it considers the intervals to be equal. At that moment,
     * the start and end points would already have been compared to one another, which is why this method
     * should <strong>disregard the start and end points</strong> completely and only consider the
     * special properties defined in the particular subclass.
     * </p>
     * <p>
     * It is vital to overwrite this method, if you have any properties in your subclass, that identify
     * the interval, such as for example user IDs, student IDs or room numbers. The two base comparators
     * are used within the underlying {@link java.util.TreeSet}s, which may discard two distinct interval
     * objects, that have the same start and end points.
     * </p>
     *
     * @param other The object that is being compared to this interval
     * @return <ul>
     * <li>-1, if this object is less than the {@code other},</li>
     * <li>0, if the two objects are equal,</li>
     * <li>1, if this object is greater than the {@code other}.</li>
     * </ul>
     */
    protected int compareSpecialization(Interval<T> other) {
        return 0;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = this.start == null ? 0 : this.start.hashCode();
        result = prime * result + (this.end == null ? 0 : this.end.hashCode());
        result = prime * result + (this.isStartInclusive ? 1 : 0);
        result = prime * result + (this.isEndInclusive ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Interval))
            return false;
        Interval<T> other = (Interval<T>) obj;
        if (this.start == null ^ other.start == null)
            return false;
        if (this.end == null ^ other.end == null)
            return false;
        if (this.isEndInclusive ^ other.isEndInclusive)
            return false;
        if (this.isStartInclusive ^ other.isStartInclusive)
            return false;
        if (this.start != null && !this.start.equals(other.start))
            return false;
        return this.end == null || this.end.equals(other.end);
    }
}
