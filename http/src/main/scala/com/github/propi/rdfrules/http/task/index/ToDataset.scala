package com.github.propi.rdfrules.http.task.index

import com.github.propi.rdfrules.data.Dataset
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.index.IndexPart
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 10. 8. 2018.
  */
class ToDataset(implicit debugger: Debugger) extends Task[IndexPart, Dataset] {
  val companion: TaskDefinition = ToDataset

  def execute(input: IndexPart): Dataset = input.toDataset.withDebugger
}

object ToDataset extends TaskDefinition {
  val name: String = "IndexToDataset"
}