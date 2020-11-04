package com.github.propi.rdfrules.http.task.ruleset

import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource, RulesetWriter}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportRules(path: String, format: Option[RulesetSource]) extends Task[Ruleset, Unit] with Task.Prevalidate {
  val companion: TaskDefinition = ExportRules

  def validate(): Option[ValidationException] = if (!Workspace.filePathIsWritable(path)) {
    Some(ValidationException("DirectoryIsNotWritable", "The directory for placing the file is not writable."))
  } else {
    None
  }

  def execute(input: Ruleset): Unit = format match {
    case Some(x) => input.export(Workspace.path(path))(x)
    case None => input.export(Workspace.path(path))
  }
}

object ExportRules extends TaskDefinition {
  val name: String = "ExportRules"
}