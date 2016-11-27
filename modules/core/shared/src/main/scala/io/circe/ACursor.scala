package io.circe

import cats.Applicative
import cats.kernel.Eq
import scala.annotation.tailrec

/**
 * A zipper that represents a position in a JSON document and supports navigation and modification.
 *
 * The `focus` represents the current position of the cursor; it may be updated with `withFocus` or
 * changed using navigation methods like `left` and `right`.
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 * @groupname Access Access and navigation
 * @groupprio Access 1
 * @groupname Modification Modification
 * @groupprio Modification 2
 * @groupname ArrayAccess Array access
 * @groupprio ArrayAccess 3
 * @groupname ObjectAccess Object access
 * @groupprio ObjectAccess 4
 * @groupname ArrayNavigation Array navigation
 * @groupprio ArrayNavigation 5
 * @groupname ObjectNavigation Object navigation
 * @groupprio ObjectNavigation 6
 * @groupname ArrayModification Array modification
 * @groupprio ArrayModification 7
 * @groupname ObjectModification Object modification
 * @groupprio ObjectModification 8
 * @groupname Decoding Decoding
 * @groupprio Decoding 9
 *
 * @author Travis Brown
 */
sealed abstract class ACursor(
  private val lastCursor: HCursor,
  private val lastOp: CursorOp
) extends Serializable {
  /**
   * The current location in the document.
   *
   * @group Access
   */
  def focus: Option[Json]

  /**
   * The operations that have been performed so far.
   *
   * @group Decoding
   */
  final def history: List[CursorOp] = {
    var next = this
    val builder = List.newBuilder[CursorOp]

    while (next.ne(null)) {
      if (next.lastOp.ne(null)) {
        builder += next.lastOp
      }
      next = next.lastCursor
    }

    builder.result()
  }

  /**
   * Indicate whether this cursor represents the result of a successful
   * operation.
   *
   * @group Decoding
   */
  def succeeded: Boolean

  /**
   * Indicate whether this cursor represents the result of an unsuccessful
   * operation.
   *
   * @group Decoding
   */
  final def failed: Boolean = !succeeded

  /**
   * Return the cursor as an [[HCursor]] if it was successful.
   *
   * @group Decoding
   */
  def success: Option[HCursor]

  /**
   * Return to the root of the document.
   *
   * @group Access
   */
  def top: Option[Json]

  /**
   * Modify the focus using the given function.
   *
   * @group Modification
   */
  def withFocus(f: Json => Json): ACursor

  /**
   * Modify the focus in a context using the given function.
   *
   * @group Modification
   */
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor]

  /**
   * Replace the focus.
   *
   * @group Modification
   */
  final def set(j: Json): ACursor = withFocus(_ => j)

  /**
   * If the focus is a JSON array, return the elements to the left.
   *
   * @group ArrayAccess
   */
  def lefts: Option[List[Json]]

  /**
   * If the focus is a JSON array, return the elements to the right.
   *
   * @group ArrayAccess
   */
  def rights: Option[List[Json]]

  /**
   * If the focus is a JSON object, return its field names in a set.
   *
   * @group ObjectAccess
   */
  def fieldSet: Option[Set[String]]

  /**
   * If the focus is a JSON object, return its field names in their original order.
   *
   * @group ObjectAccess
   */
  def fields: Option[List[String]]

  /**
   * Delete the focus and move to its parent.
   *
   * @group Modification
   */
  def delete: ACursor

  /**
   * Move the focus to the parent.
   *
   * @group Access
   */
  def up: ACursor

  /**
   * If the focus is an element in a JSON array, move to the left.
   *
   * @group ArrayNavigation
   */
  def left: ACursor

  /**
   * If the focus is an element in a JSON array, move to the right.
   *
   * @group ArrayNavigation
   */
  def right: ACursor

  /**
   * If the focus is an element in a JSON array, move to the first element.
   *
   * @group ArrayNavigation
   */
  def first: ACursor

  /**
   * If the focus is an element in a JSON array, move to the last element.
   *
   * @group ArrayNavigation
   */
  def last: ACursor

  /**
   * If the focus is an element in JSON array, move to the left the given number of times.
   *
   * A negative value will move the cursor right.
   *
   * @group ArrayNavigation
   */
  def leftN(n: Int): ACursor

  /**
   * If the focus is an element in JSON array, move to the right the given number of times.
   *
   * A negative value will move the cursor left.
   *
   * @group ArrayNavigation
   */
  def rightN(n: Int): ACursor

  /**
   * If the focus is an element in a JSON array, move to the left until the given predicate matches
   * the new focus.
   *
   * @group ArrayNavigation
   */
  def leftAt(p: Json => Boolean): ACursor

  /**
   * If the focus is an element in a JSON array, move to the right until the given predicate matches
   * the new focus.
   *
   * @group ArrayNavigation
   */
  def rightAt(p: Json => Boolean): ACursor

  /**
   * If the focus is an element in a JSON array, find the first element at or to its right that
   * matches the given predicate.
   *
   * @group ArrayNavigation
   */
  def find(p: Json => Boolean): ACursor

  /**
   * If the focus is a JSON array, move to its first element.
   *
   * @group ArrayNavigation
   */
  def downArray: ACursor

  /**
   * If the focus is a JSON array, move to the first element that satisfies the given predicate.
   *
   * @group ArrayNavigation
   */
  def downAt(p: Json => Boolean): ACursor

  /**
   * If the focus is a JSON array, move to the element at the given index.
   *
   * @group ArrayNavigation
   */
  def downN(n: Int): ACursor

  /**
   * If the focus is a value in a JSON object, move to a sibling with the given key.
   *
   * @group ObjectNavigation
   */
  def field(k: String): ACursor

  /**
   * If the focus is a JSON object, move to the value of the given key.
   *
   * @group ObjectNavigation
   */
  def downField(k: String): ACursor

  /**
   * Delete the focus and move to the left in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoLeft: ACursor

  /**
   * Delete the focus and move to the right in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoRight: ACursor

  /**
   * Delete the focus and move to the first element in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoFirst: ACursor

  /**
   * Delete the focus and move to the last element in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoLast: ACursor

  /**
   * Delete all values to the left of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteLefts: ACursor

  /**
   * Delete all values to the right of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteRights: ACursor

  /**
   * Replace all values to the left of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def setLefts(x: List[Json]): ACursor

  /**
   * Replace all values to the right of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def setRights(x: List[Json]): ACursor

  /**
   * Delete the focus and move to the sibling with the given key in a JSON object.
   *
   * @group ObjectModification
   */
  def deleteGoField(k: String): ACursor

  /**
   * Attempt to decode the focus as an `A`.
   *
   * @group Decoding
   */
  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d.tryDecode(this)

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   *
   * @group Decoding
   */
  final def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = downField(k).as[A]

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   * If the field `k` is missing, then use the `fallback` instead.
   *
   * @group Decoding
   */
  final def getOrElse[A](k: String)(fallback: => A)(implicit d: Decoder[A]): Decoder.Result[A] =
    get[Option[A]](k) match {
      case Right(Some(a)) => Right(a)
      case Right(None) => Right(fallback)
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

  /**
   * Replay an operation against this cursor.
   *
   * @group Utilities
   */
  final def replayOne(op: CursorOp): ACursor = op match {
    case CursorOp.MoveLeft => left
    case CursorOp.MoveRight => right
    case CursorOp.MoveFirst => first
    case CursorOp.MoveLast => last
    case CursorOp.MoveUp => up
    case CursorOp.LeftN(n) => leftN(n)
    case CursorOp.RightN(n) => rightN(n)
    case CursorOp.LeftAt(p) => leftAt(p)
    case CursorOp.RightAt(p) => rightAt(p)
    case CursorOp.Find(p) => find(p)
    case CursorOp.Field(k) => field(k)
    case CursorOp.DownField(k) => downField(k)
    case CursorOp.DownArray => downArray
    case CursorOp.DownAt(p) => downAt(p)
    case CursorOp.DownN(n) => downN(n)
    case CursorOp.DeleteGoParent => delete
    case CursorOp.DeleteGoLeft => deleteGoLeft
    case CursorOp.DeleteGoRight => deleteGoRight
    case CursorOp.DeleteGoFirst => deleteGoFirst
    case CursorOp.DeleteGoLast => deleteGoLast
    case CursorOp.DeleteGoField(k) => deleteGoField(k)
    case CursorOp.DeleteLefts => deleteLefts
    case CursorOp.DeleteRights => deleteRights
    case CursorOp.SetLefts(js) => setLefts(js)
    case CursorOp.SetRights(js) => setRights(js)
  }

  /**
   * Replay history (a list of operations in reverse "chronological" order) against this cursor.
   *
   * @group Utilities
   */
  final def replay(history: List[CursorOp]): ACursor = history.foldRight(this)((op, c) => c.replayOne(op))
}

final object ACursor {
  implicit val eqACursor: Eq[ACursor] = Eq.instance {
    case (a: HCursor, b: HCursor) => HCursor.eqHCursor.eqv(a, b)
    case (a: FailedCursor, b: FailedCursor) => FailedCursor.eqFailedCursor.eqv(a, b)
    case _ => false
  }
}

final class FailedCursor(val incorrectFocus: Boolean)(
  lastCursor: HCursor,
  lastOp: CursorOp
) extends ACursor(lastCursor, lastOp) {
  def succeeded: Boolean = false
  def success: Option[HCursor] = None

  def focus: Option[Json] = None
  def top: Option[Json] = None

  def withFocus(f: Json => Json): ACursor = this
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] = F.pure(this)

  def fieldSet: Option[Set[String]] = None
  def fields: Option[List[String]] = None
  def lefts: Option[List[Json]] = None
  def rights: Option[List[Json]] = None

  def downArray: ACursor = this
  def downAt(p: Json => Boolean): ACursor = this
  def downField(k: String): ACursor = this
  def downN(n: Int): ACursor = this
  def find(p: Json => Boolean): ACursor = this
  def leftAt(p: Json => Boolean): ACursor = this
  def leftN(n: Int): ACursor = this
  def rightAt(p: Json => Boolean): ACursor = this
  def rightN(n: Int): ACursor = this
  def up: ACursor = this

  def left: ACursor = this
  def right: ACursor = this
  def first: ACursor = this
  def last: ACursor = this

  def delete: ACursor = this
  def deleteGoLeft: ACursor = this
  def deleteGoRight: ACursor = this
  def deleteGoFirst: ACursor = this
  def deleteGoLast: ACursor = this
  def deleteLefts: ACursor = this
  def deleteRights: ACursor = this

  def setLefts(x: List[Json]): ACursor = this
  def setRights(x: List[Json]): ACursor = this

  def field(k: String): ACursor = this
  def deleteGoField(q: String): ACursor = this
}

final object FailedCursor {
  implicit val eqFailedCursor: Eq[FailedCursor] =
    cats.instances.list.catsKernelStdEqForList[CursorOp].on[FailedCursor](_.history)
}

sealed abstract class HCursor(lastCursor: HCursor, lastOp: CursorOp) extends ACursor(lastCursor, lastOp) {
  def value: Json

  protected def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor
  protected def addOp(cursor: HCursor, op: CursorOp): HCursor

  final def withFocus(f: Json => Json): ACursor = replace(f(value), this, null)
  final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
    F.map(f(value))(replace(_, this, null))

  final def succeeded: Boolean = true
  final def success: Option[HCursor] = Some(this)

  final def focus: Option[Json] = Some(value)

  final def fieldSet: Option[Set[String]] = value match {
    case Json.JObject(o) => Some(o.fieldSet)
    case _ => None
  }

  final def fields: Option[List[String]] = value match {
    case Json.JObject(o) => Some(o.fields)
    case _ => None
  }

  final def leftN(n: Int): ACursor = if (n < 0) rightN(-n) else {
    @tailrec
    def go(i: Int, c: ACursor): ACursor = if (i == 0) c else go(i - 1, c.left)

    go(n, this)
  }

  final def rightN(n: Int): ACursor = if (n < 0) leftN(-n) else {
    @tailrec
    def go(i: Int, c: ACursor): ACursor = if (i == 0) c else go(i - 1, c.right)

    go(n, this)
  }

  final def leftAt(p: Json => Boolean): ACursor = {
    @tailrec
    def go(c: ACursor): ACursor = c match {
      case success: HCursor => if (p(success.value)) success else go(success.left)
      case other => other
    }

    go(left)
  }

  final def rightAt(p: Json => Boolean): ACursor = right.find(p)

  final def find(p: Json => Boolean): ACursor = {
    @annotation.tailrec
    def go(c: ACursor): ACursor = c match {
      case success: HCursor => if (p(success.value)) success else go(success.right)
      case other => other
    }

    go(this)
  }

  final def downAt(p: Json => Boolean): ACursor = downArray.find(p)

  final def downN(n: Int): ACursor = downArray.rightN(n)

  /**
   * Create a new cursor that has failed on the given operation.
   *
   * @group Utilities
   */
  protected[this] final def fail(op: CursorOp): ACursor =
    new FailedCursor((op.requiresObject && !value.isObject) || (op.requiresArray && !value.isArray))(this, op)
}

final object HCursor {
  def fromJson(value: Json): HCursor = new ValueCursor(value)(null, null)

  private[this] val eqJsonVector: Eq[Vector[Json]] = cats.instances.vector.catsKernelStdEqForVector[Json]

  implicit val eqHCursor: Eq[HCursor] = new Eq[HCursor] {
    def eqv(a: HCursor, b: HCursor): Boolean =
      Json.eqJson.eqv(a.value, b.value) &&
      CursorOp.eqCursorOpList.eqv(a.history, b.history) && (
        (a, b) match {
          case (_: ValueCursor, _: ValueCursor) => true
          case (aa: ArrayCursor, ba: ArrayCursor) =>
            aa.changed == ba.changed &&
            eqv(aa.parent, ba.parent) &&
            eqJsonVector.eqv(aa.values, ba.values)
          case (ao: ObjectCursor, bo: ObjectCursor) =>
            ao.changed == bo.changed &&
            eqv(ao.parent, bo.parent) &&
            ao.key == bo.key &&
            JsonObject.eqJsonObject.eqv(ao.obj, bo.obj)
          case _ => false
        }
      )
  }

  private[this] sealed abstract class BaseHCursor(
    lastCursor: HCursor,
    lastOp: CursorOp
  ) extends HCursor(lastCursor, lastOp) {
    final def top: Option[Json] = {
      var current: HCursor = this

      while (!current.isInstanceOf[ValueCursor]) {
        current = current.up.asInstanceOf[HCursor]
      }

      Some(current.asInstanceOf[ValueCursor].value)
    }

    final def downArray: ACursor = value match {
      case Json.JArray(values) if !values.isEmpty =>
        new ArrayCursor(values, 0, this, false)(this, CursorOp.DownArray)
      case _ => fail(CursorOp.DownArray)
    }

    final def downField(k: String): ACursor = value match {
      case Json.JObject(o) =>
        val m = o.toMap

        if (!m.contains(k)) fail(CursorOp.DownField(k)) else {
          new ObjectCursor(o, k, this, false)(this, CursorOp.DownField(k))
        }
      case _ => fail(CursorOp.DownField(k))
    }
  }

  private[this] final class ValueCursor(val value: Json)(
    lastCursor: HCursor,
    lastOp: CursorOp
  ) extends BaseHCursor(lastCursor, lastOp) {
    def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor = new ValueCursor(newValue)(cursor, op)
    def addOp(cursor: HCursor, op: CursorOp): HCursor = new ValueCursor(value)(cursor, op)

    def lefts: Option[List[Json]] = None
    def rights: Option[List[Json]] = None

    def up: ACursor = fail(CursorOp.MoveUp)
    def delete: ACursor = fail(CursorOp.DeleteGoParent)

    def left: ACursor = fail(CursorOp.MoveLeft)
    def right: ACursor = fail(CursorOp.MoveRight)
    def first: ACursor = fail(CursorOp.MoveFirst)
    def last: ACursor = fail(CursorOp.MoveLast)

    def deleteGoLeft: ACursor = fail(CursorOp.DeleteGoLeft)
    def deleteGoRight: ACursor = fail(CursorOp.DeleteGoRight)
    def deleteGoFirst: ACursor = fail(CursorOp.DeleteGoFirst)
    def deleteGoLast: ACursor = fail(CursorOp.DeleteGoLast)
    def deleteLefts: ACursor = fail(CursorOp.DeleteLefts)
    def deleteRights: ACursor = fail(CursorOp.DeleteRights)

    def setLefts(x: List[Json]): ACursor = fail(CursorOp.SetLefts(x))
    def setRights(x: List[Json]): ACursor = fail(CursorOp.SetRights(x))

    def field(k: String): ACursor = fail(CursorOp.Field(k))
    def deleteGoField(k: String): ACursor = fail(CursorOp.DeleteGoField(k))
  }

  private[this] final class ArrayCursor(
    val values: Vector[Json],
    val index: Int,
    val parent: HCursor,
    val changed: Boolean
  )(
    lastCursor: HCursor,
    lastOp: CursorOp
  ) extends BaseHCursor(lastCursor, lastOp) {
    def value: Json = values(index)

    private[this] def valuesExcept: Vector[Json] = values.take(index) ++ values.drop(index + 1)

    def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor =
      new ArrayCursor(values.updated(index, newValue), index, parent, true)(cursor, op)

    def addOp(cursor: HCursor, op: CursorOp): HCursor =
      new ArrayCursor(values, index, parent, true)(cursor, op)

    def up: ACursor =
      if (!changed) parent.addOp(this, CursorOp.MoveUp) else
        parent.replace(Json.fromValues(values), this, CursorOp.MoveUp)
    def delete: ACursor = parent.replace(Json.fromValues(valuesExcept), this, CursorOp.DeleteGoParent)

    def lefts: Option[List[Json]] = Some(values.take(index).reverse.toList)
    def rights: Option[List[Json]] = Some(values.drop(index + 1).toList)

    def left: ACursor = if (index == 0) fail(CursorOp.MoveLeft) else {
      new ArrayCursor(values, index - 1, parent, changed)(this, CursorOp.MoveLeft)
    }

    def right: ACursor = if (index == values.size - 1) fail(CursorOp.MoveRight) else {
      new ArrayCursor(values, index + 1, parent, changed)(this, CursorOp.MoveRight)
    }

    def first: ACursor = new ArrayCursor(values, 0, parent, changed)(this, CursorOp.MoveFirst)
    def last: ACursor = new ArrayCursor(values, values.size - 1, parent, changed)(this, CursorOp.MoveLast)

    def deleteGoLeft: ACursor = if (index == 0) fail(CursorOp.DeleteGoLeft) else {
      new ArrayCursor(valuesExcept, index - 1, parent, true)(this, CursorOp.DeleteGoLeft)
    }

    def deleteGoRight: ACursor = if (index == values.size - 1) fail(CursorOp.DeleteGoRight) else {
      new ArrayCursor(valuesExcept, index, parent, true)(this, CursorOp.DeleteGoRight)
    }

    def deleteGoFirst: ACursor = if (values.size == 1) fail(CursorOp.DeleteGoFirst) else {
      new ArrayCursor(valuesExcept, 0, parent, true)(this, CursorOp.DeleteGoFirst)
    }

    def deleteGoLast: ACursor = if (values.size == 1) fail(CursorOp.DeleteGoLast) else {
      new ArrayCursor(valuesExcept, values.size - 2, parent, true)(this, CursorOp.DeleteGoLast)
    }

    def deleteLefts: ACursor = new ArrayCursor(values.drop(index), 0, parent, index != 0)(this, CursorOp.DeleteLefts)
    def deleteRights: ACursor =
      new ArrayCursor(values.take(index + 1), index, parent, index < values.size)(this, CursorOp.DeleteRights)

    def setLefts(js: List[Json]): ACursor =
      new ArrayCursor(js.reverse.toVector ++ values.drop(index), js.size, parent, true)(this, CursorOp.SetLefts(js))

    def setRights(js: List[Json]): ACursor =
      new ArrayCursor(values.take(index + 1) ++ js, index, parent, true)(this, CursorOp.SetRights(js))

    def field(k: String): ACursor = fail(CursorOp.Field(k))
    def deleteGoField(k: String): ACursor = fail(CursorOp.DeleteGoField(k))
  }

  private[this] final class ObjectCursor(
    val obj: JsonObject,
    val key: String,
    val parent: HCursor,
    val changed: Boolean
  )(
    lastCursor: HCursor,
    lastOp: CursorOp
  ) extends BaseHCursor(lastCursor, lastOp) {
    def value: Json = obj.toMap(key)

    def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor =
      new ObjectCursor(obj.add(key, newValue), key, parent, true)(cursor, op)

    def addOp(cursor: HCursor, op: CursorOp): HCursor = new ObjectCursor(obj, key, parent, true)(cursor, op)

    def lefts: Option[List[Json]] = None
    def rights: Option[List[Json]] = None

    def up: ACursor =
      if (!changed) parent.addOp(this, CursorOp.MoveUp) else
        parent.replace(Json.fromJsonObject(obj), this, CursorOp.MoveUp)

    def delete: ACursor = parent.replace(Json.fromJsonObject(obj.remove(key)), this, CursorOp.DeleteGoParent)

    def field(k: String): ACursor = {
      val m = obj.toMap

      if (m.contains(k)) new ObjectCursor(obj, k, parent, changed)(this, CursorOp.Field(k))
        else fail(CursorOp.Field(k))
    }

    def deleteGoField(k: String): ACursor = {
      val m = obj.toMap

      if (m.contains(k)) new ObjectCursor(obj.remove(key), k, parent, true)(this, CursorOp.DeleteGoField(k))
        else fail(CursorOp.DeleteGoField(k))
    }

    def left: ACursor = fail(CursorOp.MoveLeft)
    def right: ACursor = fail(CursorOp.MoveRight)
    def first: ACursor = fail(CursorOp.MoveFirst)
    def last: ACursor = fail(CursorOp.MoveLast)

    def deleteGoLeft: ACursor = fail(CursorOp.DeleteGoLeft)
    def deleteGoRight: ACursor = fail(CursorOp.DeleteGoRight)
    def deleteGoFirst: ACursor = fail(CursorOp.DeleteGoFirst)
    def deleteGoLast: ACursor = fail(CursorOp.DeleteGoLast)
    def deleteLefts: ACursor = fail(CursorOp.DeleteLefts)
    def deleteRights: ACursor = fail(CursorOp.DeleteRights)

    def setLefts(x: List[Json]): ACursor = fail(CursorOp.SetLefts(x))
    def setRights(x: List[Json]): ACursor = fail(CursorOp.SetRights(x))
  }
}
