package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.{Stringifier, TypedKeyMap}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 17. 4. 2018.
  */
case class ResolvedRule private(body: IndexedSeq[ResolvedAtom], head: ResolvedAtom)(val measures: TypedKeyMap.Immutable[Measure]) extends ResolvedRuleContent {
  def toRule(implicit tripleItemIndex: TripleItemIndex): FinalRule = Rule(
    head.toAtom,
    body.map(_.toAtom),
    measures
  )

  def toRuleOpt(implicit tripleItemIndex: TripleItemIndex): Option[FinalRule] = for (h <- head.toAtomOpt) yield {
    Rule(h, body.flatMap(_.toAtomOpt), measures)
  }

  override def toString: String = Stringifier(this)
}

object ResolvedRule {

  /*def simple(resolvedRule: ResolvedRule)(implicit mapper: TripleItemIndex): (FinalRule, collection.Map[Int, TripleItem]) = {
    var i = 0
    val map = collection.mutable.Map.empty[TripleItem, Int]

    @scala.annotation.tailrec
    def newIndex: Int = {
      i += 1
      mapper.getTripleItemOpt(i) match {
        case Some(_) => newIndex
        case None => i
      }
    }

    def tripleItem(x: TripleItem): Int = mapper.getIndexOpt(x) match {
      case Some(x) => x
      case None => map.getOrElseUpdate(x, newIndex)
    }

    def atomItem(x: ResolvedItem): rule.Atom.Item = x match {
      case ResolvedItem.Variable(x) => x
      case ResolvedItem.Constant(x) => rule.Atom.Constant(tripleItem(x))
    }

    def atom(x: ResolvedAtom): rule.Atom = rule.Atom(atomItem(x.subject), tripleItem(x.predicate), atomItem(x.`object`))

    Rule(
      atom(resolvedRule.head),
      resolvedRule.body.map(atom)
    )(resolvedRule.measures) -> map.map(_.swap)
  }*/

  def apply(body: IndexedSeq[ResolvedAtom], head: ResolvedAtom, measures: Measure*): ResolvedRule = ResolvedRule(body, head)(TypedKeyMap(measures))

  def parse(head: (String, String, String), body: (String, String, String)*): ResolvedRule = apply(
    body.iterator.map(x => ResolvedAtom.parse(x._1, x._2, x._3)).toIndexedSeq,
    ResolvedAtom.parse(head._1, head._2, head._3)
  )

  implicit def apply(rule: FinalRule)(implicit mapper: TripleItemIndex): ResolvedRule = ResolvedRule(
    rule.body.map(ResolvedAtom.apply),
    rule.head
  )(rule.measures)

  implicit val resolvedRuleStringifier: Stringifier[ResolvedRule] = (v: ResolvedRule) => v.body.map(x => Stringifier(x)).mkString(" ^ ") +
    " -> " +
    Stringifier(v.head) + " | " +
    v.measures.iterator.toList.sortBy(_.companion).iterator.map(x => Stringifier(x)).mkString(", ")

  implicit def resolvedRuleOrdering(implicit measuresOrdering: Ordering[TypedKeyMap.Immutable[Measure]]): Ordering[ResolvedRule] = Ordering.by[ResolvedRule, TypedKeyMap.Immutable[Measure]](_.measures)

  implicit val resolvedRuleJsonFormat: RootJsonFormat[ResolvedRule] = new RootJsonFormat[ResolvedRule] {
    def write(obj: ResolvedRule): JsValue = JsObject(
      "head" -> obj.head.toJson,
      "body" -> JsArray(obj.body.iterator.map(_.toJson).toVector),
      "measures" -> JsArray(obj.measures.iterator.map(_.toJson).toVector)
    )

    def read(json: JsValue): ResolvedRule = {
      val fields = json.asJsObject.fields
      ResolvedRule(fields("body").convertTo[IndexedSeq[ResolvedAtom]], fields("head").convertTo[ResolvedAtom])(TypedKeyMap(fields("measures").convertTo[Seq[Measure]]))
    }
  }

}