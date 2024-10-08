package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 23. 9. 2017.
  */
case class AtomPattern(subject: AtomItemPattern = Any, predicate: AtomItemPattern = Any, `object`: AtomItemPattern = Any, graph: AtomItemPattern = Any) {
  def mapped(implicit mapper: TripleItemIndex): AtomPattern.Mapped = AtomPattern.Mapped(subject.mapped, predicate.mapped, `object`.mapped, graph.mapped)
}

object AtomPattern {

  case class Mapped(subject: AtomItemPattern.Mapped, predicate: AtomItemPattern.Mapped, `object`: AtomItemPattern.Mapped, graph: AtomItemPattern.Mapped)

  sealed trait AtomItemPattern {
    def mapped(implicit mapper: TripleItemIndex): AtomItemPattern.Mapped
  }

  object AtomItemPattern {

    sealed trait Mapped

    object Mapped {

      case class OneOf(col: Seq[Mapped.Constant]) extends Mapped

      case class NoneOf(col: Seq[Mapped.Constant]) extends Mapped

      case class Constant(constant: Atom.Constant) extends Mapped

    }

    case object Any extends AtomItemPattern with Mapped {
      def mapped(implicit mapper: TripleItemIndex): Mapped = this
    }

    case class OneOf(col: Seq[AtomItemPattern.Constant]) extends AtomItemPattern {
      def mapped(implicit mapper: TripleItemIndex): Mapped.OneOf = Mapped.OneOf(col.map(_.mapped))
    }

    object OneOf {
      def apply(atomItemPattern: AtomItemPattern.Constant, atomItemPatterns: AtomItemPattern.Constant*): OneOf = new OneOf(atomItemPattern +: atomItemPatterns)
    }

    case class NoneOf(col: Seq[AtomItemPattern.Constant]) extends AtomItemPattern {
      def mapped(implicit mapper: TripleItemIndex): Mapped.NoneOf = Mapped.NoneOf(col.map(_.mapped))
    }

    object NoneOf {
      def apply(atomItemPattern: AtomItemPattern.Constant, atomItemPatterns: AtomItemPattern.Constant*): NoneOf = new NoneOf(atomItemPattern +: atomItemPatterns)
    }

    case object AnyVariable extends AtomItemPattern with Mapped {
      def mapped(implicit mapper: TripleItemIndex): Mapped = this
    }

    case object AnyConstant extends AtomItemPattern with Mapped {
      def mapped(implicit mapper: TripleItemIndex): Mapped = this
    }

    case class Variable(variable: Atom.Variable) extends AtomItemPattern with Mapped {
      def mapped(implicit mapper: TripleItemIndex): Mapped = this
    }

    case class Constant(constant: TripleItem) extends AtomItemPattern {
      def mapped(implicit mapper: TripleItemIndex): Mapped.Constant = Mapped.Constant(Atom.Constant(mapper.getIndex(constant)))
    }

    implicit def apply(tripleItem: TripleItem): AtomItemPattern = Constant(tripleItem)

    implicit def apply(variable: Char): AtomItemPattern = Variable(variable)

  }

  trait PimpedOptionalConsequent {
    protected val consequent: Option[AtomPattern]

    def =>:(antecedent: AtomPattern): RulePattern = antecedent &: RulePattern(consequent)
  }

}