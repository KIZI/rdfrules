package com.github.propi.rdfrules.experiments.benchmark.tasks

import com.github.propi.rdfrules.experiments.benchmark.{DefaultMiningSettings, RdfRulesMiningTask, TaskPostProcessor}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class NumOfThreadsRdfRules[T](val name: String,
                              override val numberOfThreads: Int,
                              override val minHeadCoverage: Double = DefaultMiningSettings.minHeadCoverage,
                              override val allowConstants: Option[ConstantsPosition] = DefaultMiningSettings.allowConstants)
                             (implicit val debugger: Debugger) extends RdfRulesMiningTask[T] {

  self: TaskPostProcessor[Ruleset, T] =>

}