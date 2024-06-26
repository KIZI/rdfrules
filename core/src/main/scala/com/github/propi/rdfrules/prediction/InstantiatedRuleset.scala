package com.github.propi.rdfrules.prediction

import com.github.propi.rdfrules.data.ops.{Cacheable, Debugable, Transformable}
import com.github.propi.rdfrules.index.{IndexPart, TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.PatternMatcher.Aliases
import com.github.propi.rdfrules.rule.RulePatternMatcher._
import com.github.propi.rdfrules.rule.{InstantiatedRule, PatternMatcher, ResolvedInstantiatedRule, Rule, RulePattern}
import com.github.propi.rdfrules.serialization.RuleSerialization._
import com.github.propi.rdfrules.utils.ForEach
import com.github.propi.rdfrules.utils.serialization.{Deserializer, SerializationSize, Serializer}

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class InstantiatedRuleset private(val rules: ForEach[InstantiatedRule], val index: IndexPart)
  extends Transformable[InstantiatedRule, InstantiatedRuleset]
    with Cacheable[InstantiatedRule, InstantiatedRuleset]
    with Debugable[InstantiatedRule, InstantiatedRuleset] {

  self =>

  protected def coll: ForEach[InstantiatedRule] = rules

  protected def transform(col: ForEach[InstantiatedRule]): InstantiatedRuleset = new InstantiatedRuleset(col, index)

  protected def cachedTransform(col: ForEach[InstantiatedRule]): InstantiatedRuleset = new InstantiatedRuleset(col, index)

  protected val serializer: Serializer[InstantiatedRule] = Serializer.by[InstantiatedRule, ResolvedInstantiatedRule](ResolvedInstantiatedRule(_)(index.tripleItemMap))
  protected val deserializer: Deserializer[InstantiatedRule] = Deserializer.by[ResolvedInstantiatedRule, InstantiatedRule](_.toInstantiatedRule(index.tripleItemMap))
  protected val serializationSize: SerializationSize[InstantiatedRule] = SerializationSize.by[InstantiatedRule, ResolvedInstantiatedRule]
  protected val dataLoadingText: String = "Instantiated ruleset loading"

  def withIndex(index: IndexPart): InstantiatedRuleset = new InstantiatedRuleset(rules, index)

  def filter(pattern: RulePattern, patterns: RulePattern*): InstantiatedRuleset = transform((f: InstantiatedRule => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    implicit val thi: TripleIndex[Int] = index.tripleMap
    val rulePatternMatcher = implicitly[PatternMatcher[Rule, RulePattern.Mapped]]
    val mappedPatterns = (pattern +: patterns).map(_.withOrderless().mapped)
    rules.filter(rule => mappedPatterns.exists(rulePattern => rulePatternMatcher.matchPattern(rule.toRule, rulePattern)(Aliases.empty).isDefined)).foreach(f)
  })

  def filterResolved(f: ResolvedInstantiatedRule => Boolean): InstantiatedRuleset = transform((f2: InstantiatedRule => Unit) => {
    implicit val mapper: TripleItemIndex = index.tripleItemMap
    rules.filter(x => f(x)).foreach(f2)
  })

  def resolvedRules: ForEach[ResolvedInstantiatedRule] = new ForEach[ResolvedInstantiatedRule] {
    def foreach(f: ResolvedInstantiatedRule => Unit): Unit = {
      implicit val mapper: TripleItemIndex = index.tripleItemMap
      rules.foreach(x => f(x))
    }

    override def knownSize: Int = rules.knownSize
  }

  def foreach(f: ResolvedInstantiatedRule => Unit): Unit = resolvedRules.foreach(f)

  def +(ruleset: InstantiatedRuleset): InstantiatedRuleset = transform(rules.concat(ruleset.rules))

  def headResolved: ResolvedInstantiatedRule = resolvedRules.head

  def headResolvedOption: Option[ResolvedInstantiatedRule] = resolvedRules.headOption

  def findResolved(f: ResolvedInstantiatedRule => Boolean): Option[ResolvedInstantiatedRule] = resolvedRules.find(f)

}

object InstantiatedRuleset {

  def apply(index: IndexPart, rules: ForEach[InstantiatedRule]): InstantiatedRuleset = new InstantiatedRuleset(rules, index)

}
