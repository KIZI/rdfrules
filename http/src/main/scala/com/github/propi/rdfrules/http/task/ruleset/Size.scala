package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class Size extends Task[Ruleset, Int] {
  val companion: TaskDefinition = Size

  def execute(input: Ruleset): Int = input.size
}

object Size extends TaskDefinition {
  val name: String = "RulesetSize"
}