package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.index.TripleItemIndex

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class RulePattern private(antecedent: IndexedSeq[AtomPattern], consequent: Option[AtomPattern], exact: Boolean) {

  /**
    * Add an atom pattern as a body pattern. It adds the pattern into the left side.
    *
    * @param atom atom pattern
    * @return a new rule pattern with added atom body pattern
    */
  def &:(atom: AtomPattern): RulePattern = this.copy(antecedent = atom +: antecedent)

  /**
    * Create a new rule pattern with changed exact property.
    *
    * @param exact true = the output rule must completely match this pattern, false = the output rule must right-partially match of this rule
    * @return a new rule pattern
    */
  def withExact(exact: Boolean = true): RulePattern = this.copy(exact = exact)

  /**
    * Create mapped rule pattern with mapped triple items to hash numbers
    *
    * @param mapper triple item mapper
    * @return a mapped rule pattern
    */
  def mapped(implicit mapper: TripleItemIndex): RulePattern.Mapped = RulePattern.Mapped(antecedent.map(_.mapped), consequent.map(_.mapped), exact)

}

object RulePattern {

  case class Mapped(antecedent: IndexedSeq[AtomPattern.Mapped], consequent: Option[AtomPattern.Mapped], exact: Boolean)

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern may be exect or partial.
    *
    * @param consequent consequent pattern, None = no pattern for consequent.
    * @param exact      true = the output rule must completely match this pattern, false = the output rule must right-partially match of this rule
    * @return rule pattern
    */
  def apply(consequent: Option[AtomPattern], exact: Boolean): RulePattern = RulePattern(Vector.empty, consequent, exact)

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern may be exect or partial.
    *
    * @param consequent consequent pattern
    * @param exact      true = the output rule must completely match this pattern, false = the output rule must right-partially match of this rule
    * @return rule pattern
    */
  def apply(consequent: AtomPattern, exact: Boolean): RulePattern = apply(Some(consequent), exact)

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern is not exact.
    *
    * @param consequent consequent pattern, None = no pattern for consequent.
    * @return rule pattern
    */
  implicit def apply(consequent: Option[AtomPattern]): RulePattern = apply(consequent, false)

  /**
    * Create a rule pattern with an atom pattern as the consequent.
    * The pattern is not exact.
    *
    * @param consequent consequent pattern
    * @return rule pattern
    */
  implicit def apply(consequent: AtomPattern): RulePattern = apply(consequent, false)

  /**
    * Create a rule pattern with none pattern as the consequent.
    * The pattern is not exact.
    *
    * @return rule pattern
    */
  def apply(): RulePattern = apply(None, false)

}