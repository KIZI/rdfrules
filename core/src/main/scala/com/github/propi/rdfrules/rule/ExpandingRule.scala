package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleIndex
import com.github.propi.rdfrules.utils.{MutableRanges, TypedKeyMap}

import scala.ref.SoftReference

/**
  * Created by Vaclav Zeman on 31. 7. 2017.
  */
sealed trait ExpandingRule extends Rule {
  def maxVariable: Atom.Variable

  def supportedRanges: Option[MutableRanges]

  final def measures: TypedKeyMap.Immutable[Measure] = throw new UnsupportedOperationException

  final def headCoverage: Double = support.toDouble / headSize

  def headTriples(injectiveMapping: Boolean)(implicit thi: TripleIndex[Int]): Iterator[(Int, Int)] = (head.subject, head.`object`) match {
    case (_: Atom.Variable, _: Atom.Variable) =>
      thi.predicates(head.predicate).subjects.pairIterator.flatMap {
        case (s, oi) => oi.iterator.filter(o => !injectiveMapping || s != o).map(s -> _)
      }
    case (Atom.Constant(s), _: Atom.Variable) => thi.predicates(head.predicate).subjects(s).iterator.filter(o => !injectiveMapping || s != o).map(s -> _)
    case (_: Atom.Variable, Atom.Constant(o)) => thi.predicates(head.predicate).objects(o).iterator.filter(s => !injectiveMapping || s != o).map(_ -> o)
    case (Atom.Constant(s), Atom.Constant(o)) => if (injectiveMapping && s == o) Iterator.empty else Iterator(s -> o)
  }
}

object ExpandingRule {

  import Atom.variableOrdering.mkOrderingOps

  /*sealed trait DanglingVariables {
    def others: List[Atom.Variable]

    def danglings: List[Atom.Variable]

    final def all: Iterator[Atom.Variable] = danglings.iterator ++ others.iterator
  }

  case class OneDangling(dangling: Atom.Variable, others: List[Atom.Variable]) extends DanglingVariables {
    def danglings: List[Atom.Variable] = List(dangling)
  }

  case class TwoDanglings(dangling1: Atom.Variable, dangling2: Atom.Variable, others: List[Atom.Variable]) extends DanglingVariables {
    def danglings: List[Atom.Variable] = List(dangling1, dangling2)
  }*/

  sealed trait ClosedRule extends ExpandingRule {
    override def equals(other: Any): Boolean = other match {
      case _: ClosedRule => super.equals(other)
      case _ => false
    }

    //TODO check whether it is faster than Rule hashCode
    //override def hashCode(): Int = body.hashCode() * 31 + head.hashCode()

    val variables: List[Atom.Variable]

    lazy val maxVariable: Atom.Variable = variables.max
  }

  object ClosedRule {
    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, variables: List[Atom.Variable]): ClosedRule = new BasicClosedRule(body, head, support, headSize, variables)

    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, variables: List[Atom.Variable], supportedRanges: MutableRanges): ClosedRule = new CachedClosedRule(body, head, support, headSize, variables, SoftReference(supportedRanges))
  }

  private class BasicClosedRule(val body: IndexedSeq[Atom],
                                val head: Atom,
                                val support: Int,
                                val headSize: Int,
                                val variables: List[Atom.Variable]
                               ) extends ClosedRule {
    def supportedRanges: Option[MutableRanges] = None
  }

  private class CachedClosedRule(val body: IndexedSeq[Atom],
                                 val head: Atom,
                                 val support: Int,
                                 val headSize: Int,
                                 val variables: List[Atom.Variable],
                                 _supportedRanges: SoftReference[MutableRanges]
                                ) extends ClosedRule {
    def supportedRanges: Option[MutableRanges] = _supportedRanges.get
  }

  sealed trait DanglingRule extends ExpandingRule {
    override def equals(other: Any): Boolean = other match {
      case _: DanglingRule => super.equals(other)
      case _ => false
    }

    def danglings: List[Atom.Variable]

    def others: List[Atom.Variable]

    final def all: Iterator[Atom.Variable] = danglings.iterator ++ others.iterator

    lazy val maxVariable: Atom.Variable = if (others.isEmpty) danglings.head else danglings.head.max(others.head)
  }

  object DanglingRule {
    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, danglings: List[Atom.Variable], others: List[Atom.Variable]): DanglingRule = new BasicDanglingRule(body, head, support, headSize, danglings, others)

    def apply(body: IndexedSeq[Atom], head: Atom, support: Int, headSize: Int, danglings: List[Atom.Variable], others: List[Atom.Variable], supportedRanges: MutableRanges): DanglingRule = new CachedDanglingRule(body, head, support, headSize, danglings, others, SoftReference(supportedRanges))
  }

  private class BasicDanglingRule(val body: IndexedSeq[Atom],
                                  val head: Atom,
                                  val support: Int,
                                  val headSize: Int,
                                  val danglings: List[Atom.Variable],
                                  val others: List[Atom.Variable]) extends DanglingRule {
    def supportedRanges: Option[MutableRanges] = None
  }

  private class CachedDanglingRule(val body: IndexedSeq[Atom],
                                   val head: Atom,
                                   val support: Int,
                                   val headSize: Int,
                                   val danglings: List[Atom.Variable],
                                   val others: List[Atom.Variable],
                                   _supportedRanges: SoftReference[MutableRanges]
                                  ) extends DanglingRule {
    def supportedRanges: Option[MutableRanges] = _supportedRanges.get
  }

}